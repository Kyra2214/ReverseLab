package br.com.reverselab.core.analysis

import br.com.reverselab.core.domain.ArchiveInventoryRecord
import br.com.reverselab.core.domain.SampleImportStatus
import br.com.reverselab.core.domain.SampleRecord
import br.com.reverselab.core.domain.SampleRole

/**
 * Conservative structural assessment for associated APK/split/OBB files.
 * Does not decode binary AndroidManifest.xml and never claims package identity or a complete split set.
 */
class PackageSetCompletenessAnalyzer {
    fun assess(
        samples: List<SampleRecord>,
        inventories: Map<br.com.reverselab.core.domain.SampleId, ArchiveInventoryRecord>,
    ): List<PackageSetAssessment> = samples
        .filter { it.role in PACKAGE_ROLES }
        .groupBy { it.associationGroupId }
        .values
        .map { assessGroup(it, inventories) }

    private fun assessGroup(
        group: List<SampleRecord>,
        inventories: Map<br.com.reverselab.core.domain.SampleId, ArchiveInventoryRecord>,
    ): PackageSetAssessment {
        val imported = group.filter { it.status == SampleImportStatus.IMPORTED }
        val bases = imported.filter { it.role == SampleRole.BASE_APK_CANDIDATE }
        val splits = imported.filter { it.role == SampleRole.SPLIT_APK_CANDIDATE }
        val obbs = imported.filter { it.role == SampleRole.OBB_CANDIDATE }
        val reasons = mutableListOf<String>()
        if (splits.isEmpty() && obbs.isEmpty()) return result(group, imported, PackageSetStatus.NOT_A_MULTI_FILE_SET, listOf("Nenhum split APK ou OBB associado foi identificado."))
        if (bases.isEmpty()) return result(group, imported, PackageSetStatus.INCOMPLETE, listOf("Há split/OBB importado, mas nenhum APK base importado com sucesso foi associado."))
        if (bases.size > 1) return result(group, imported, PackageSetStatus.AMBIGUOUS, listOf("Mais de um APK base está no mesmo grupo; a associação precisa ser desambiguada."))
        val apkMembers = bases + splits
        val missingInventory = apkMembers.filter { inventories[it.id] == null }
        if (missingInventory.isNotEmpty()) reasons += "Inventário estrutural ainda não disponível para " + missingInventory.size + " APK(s)."
        val invalidManifest = apkMembers.filter { sample ->
            val inventory = inventories[sample.id]
            inventory != null && (!inventory.hasAndroidManifest || inventory.status.name in setOf("INVALID_ARCHIVE", "FAILED", "NOT_ARCHIVE"))
        }
        if (invalidManifest.isNotEmpty()) {
            reasons += "Manifesto Android não foi localizado ou o contêiner não pôde ser inspecionado em " + invalidManifest.size + " APK(s)."
            return result(group, imported, PackageSetStatus.STRUCTURALLY_INCOMPLETE, reasons)
        }
        if (missingInventory.isNotEmpty()) return result(group, imported, PackageSetStatus.UNKNOWN, reasons)
        reasons += "Manifestos foram localizados estruturalmente, mas package name, splitName e dependências ainda não foram comparados."
        reasons += "A presença de um APK base e de splits não prova que todos os splits obrigatórios foram fornecidos."
        if (obbs.isNotEmpty()) reasons += "OBB é conteúdo de expansão e não comprova nem substitui splits APK."
        return result(group, imported, PackageSetStatus.IDENTITY_UNVERIFIED, reasons)
    }

    private fun result(group: List<SampleRecord>, imported: List<SampleRecord>, status: PackageSetStatus, reasons: List<String>) = PackageSetAssessment(
        groupId = group.first().associationGroupId.value,
        status = status,
        sampleIds = imported.map { it.id.value },
        reasons = reasons,
    )

    private companion object {
        val PACKAGE_ROLES = setOf(SampleRole.BASE_APK_CANDIDATE, SampleRole.SPLIT_APK_CANDIDATE, SampleRole.OBB_CANDIDATE)
    }
}

enum class PackageSetStatus { NOT_A_MULTI_FILE_SET, INCOMPLETE, STRUCTURALLY_INCOMPLETE, AMBIGUOUS, UNKNOWN, IDENTITY_UNVERIFIED }

data class PackageSetAssessment(
    val groupId: String,
    val status: PackageSetStatus,
    val sampleIds: List<String>,
    val reasons: List<String>,
)