package br.com.reverselab.core.analysis

import br.com.reverselab.core.domain.ArchiveInventoryRecord
import br.com.reverselab.core.domain.SampleImportStatus
import br.com.reverselab.core.domain.SampleRecord
import br.com.reverselab.core.domain.SampleRole

/**
 * Correlates virtual APK members inside XAPK/APKS with APK samples from the same import batch.
 * It never creates sample records or changes the container's association identity.
 */
class NestedPackageAssociationAnalyzer {
    fun assess(
        batchSamples: List<SampleRecord>,
        inventories: Map<br.com.reverselab.core.domain.SampleId, ArchiveInventoryRecord>,
    ): List<NestedPackageAssociationAssessment> {
        val imported = batchSamples.filter { it.status == SampleImportStatus.IMPORTED }
        val externalApks = imported.filter {
            it.role == SampleRole.BASE_APK_CANDIDATE || it.role == SampleRole.SPLIT_APK_CANDIDATE
        }
        return imported
            .filter { it.role == SampleRole.PACKAGE_CONTAINER && it.formatCode.uppercase() in CONTAINER_FORMATS }
            .flatMap { container ->
                val nested = inventories[container.id]?.nestedApkManifests.orEmpty()
                if (nested.isEmpty()) return@flatMap emptyList()
                val parsed = nested.filter { it.parseStatus == "PARSED" && !it.packageName.isNullOrBlank() }
                val unparsed = nested.filterNot { it.parseStatus == "PARSED" && !it.packageName.isNullOrBlank() }
                val assessments = parsed.groupBy { it.packageName!! }.map { (packageName, members) ->
                    val matchingExternal = externalApks.filter { sample ->
                        inventories[sample.id]?.let { it.manifestParseStatus == "PARSED" && it.manifestPackageName == packageName } == true
                    }
                    val externalBases = matchingExternal.filter { sample ->
                        inventories[sample.id]?.manifestSplitName.isNullOrBlank()
                    }
                    val externalSplits = matchingExternal.filter { sample ->
                        !inventories[sample.id]?.manifestSplitName.isNullOrBlank()
                    }
                    val nestedBases = members.filter { it.splitName.isNullOrBlank() }
                    val nestedSplits = members.filter { !it.splitName.isNullOrBlank() }
                    val splitNames = (externalSplits.mapNotNull { inventories[it.id]?.manifestSplitName } +
                        nestedSplits.mapNotNull { it.splitName })
                    val duplicateSplits = splitNames.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
                    val suppliedNames = splitNames.toSet()
                    val missingDependencies = (externalSplits.flatMap { inventories[it.id]?.manifestUsesSplits.orEmpty() } +
                        nestedSplits.flatMap { it.usesSplits }).filter { it !in suppliedNames }.distinct()
                    val missingConfigTargets = (externalSplits.mapNotNull { inventories[it.id]?.manifestConfigForSplit } +
                        nestedSplits.mapNotNull { it.configForSplit })
                        .filter { it.isNotBlank() && it != "base" && it !in suppliedNames }.distinct()

                    val reasons = mutableListOf<String>()
                    val status = when {
                        externalBases.size > 1 -> {
                            reasons += "Mais de um APK base externo com o mesmo package name foi encontrado no lote."
                            NestedPackageAssociationStatus.AMBIGUOUS
                        }
                        externalBases.size == 1 && nestedBases.isNotEmpty() -> {
                            reasons += "O contêiner e o lote externo declaram um APK base para o mesmo package; a duplicidade exige revisão."
                            NestedPackageAssociationStatus.AMBIGUOUS
                        }
                        externalBases.size == 1 -> {
                            reasons += "Package name coincide com um APK base externo do mesmo lote; a associação é virtual e não duplica a amostra."
                            NestedPackageAssociationStatus.MATCHED_EXTERNAL_BASE
                        }
                        nestedBases.size == 1 -> {
                            reasons += "O próprio contêiner contém um APK base para este package; nenhum APK externo foi necessário para a associação."
                            NestedPackageAssociationStatus.SELF_CONTAINED
                        }
                        nestedBases.size > 1 -> {
                            reasons += "O contêiner declara múltiplos APKs base para o mesmo package."
                            NestedPackageAssociationStatus.AMBIGUOUS
                        }
                        externalApks.any { inventories[it.id]?.manifestParseStatus == "PARSED" } -> {
                            reasons += "Nenhum APK base externo com package name coincidente foi encontrado no lote."
                            NestedPackageAssociationStatus.EXTERNAL_BASE_NOT_FOUND
                        }
                        else -> {
                            reasons += "Há apenas splits internos e não foi possível localizar um APK base correspondente no lote."
                            NestedPackageAssociationStatus.EXTERNAL_BASE_NOT_FOUND
                        }
                    }
                    if (duplicateSplits.isNotEmpty()) {
                        reasons += "splitName duplicado entre membros internos/externos: ${duplicateSplits.joinToString()}."
                    }
                    if (missingDependencies.isNotEmpty()) {
                        reasons += "Dependências uses-split sem membro correspondente: ${missingDependencies.joinToString()}."
                    }
                    if (missingConfigTargets.isNotEmpty()) {
                        reasons += "configForSplit aponta para módulos não encontrados: ${missingConfigTargets.joinToString()}."
                    }
                    if (nestedSplits.isEmpty() && nestedBases.isEmpty()) {
                        reasons += "Nenhum papel base/split pôde ser identificado nos APKs internos."
                    }
                    NestedPackageAssociationAssessment(
                        containerSampleId = container.id.value,
                        containerName = container.displayName,
                        packageName = packageName,
                        status = if (duplicateSplits.isNotEmpty() || missingDependencies.isNotEmpty() || missingConfigTargets.isNotEmpty()) {
                            NestedPackageAssociationStatus.STRUCTURE_REQUIRES_REVIEW
                        } else status,
                        nestedEntryPaths = members.map { it.entryPath },
                        externalSampleIds = matchingExternal.map { it.id.value },
                        reasons = reasons,
                    )
                }
                val parseFailures = if (unparsed.isEmpty()) emptyList() else listOf(
                    NestedPackageAssociationAssessment(
                        containerSampleId = container.id.value,
                        containerName = container.displayName,
                        packageName = null,
                        status = NestedPackageAssociationStatus.IDENTITY_UNVERIFIED,
                        nestedEntryPaths = unparsed.map { it.entryPath },
                        externalSampleIds = emptyList(),
                        reasons = unparsed.map { "${it.entryPath}: ${it.parseStatus}" },
                    )
                )
                assessments + parseFailures
            }
    }

    private companion object {
        val CONTAINER_FORMATS = setOf("XAPK", "APKS")
    }
}

enum class NestedPackageAssociationStatus {
    MATCHED_EXTERNAL_BASE,
    SELF_CONTAINED,
    EXTERNAL_BASE_NOT_FOUND,
    AMBIGUOUS,
    STRUCTURE_REQUIRES_REVIEW,
    IDENTITY_UNVERIFIED,
}

data class NestedPackageAssociationAssessment(
    val containerSampleId: String,
    val containerName: String,
    val packageName: String?,
    val status: NestedPackageAssociationStatus,
    val nestedEntryPaths: List<String>,
    val externalSampleIds: List<String>,
    val reasons: List<String>,
)
