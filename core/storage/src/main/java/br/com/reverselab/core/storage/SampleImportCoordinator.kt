package br.com.reverselab.core.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import br.com.reverselab.core.domain.ImportBatchId
import br.com.reverselab.core.domain.SampleAssociationConfidence
import br.com.reverselab.core.domain.SampleCatalog
import br.com.reverselab.core.domain.SampleGroupId
import br.com.reverselab.core.domain.SampleId
import br.com.reverselab.core.domain.SampleImportStatus
import br.com.reverselab.core.domain.SampleRecord
import br.com.reverselab.core.domain.SampleRole
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

class SampleImportCoordinator(
    context: Context,
    private val contentResolver: ContentResolver,
    private val catalog: SampleCatalog,
) {
    private val appContext = context.applicationContext

    @Synchronized
    fun importBatch(
        batchId: ImportBatchId,
        documents: List<ImportDocumentRequest>,
        importedAtEpochMillis: Long,
    ): List<SampleImportResult> {
        require(documents.isNotEmpty())
        require(importedAtEpochMillis > 0)

        val results = documents.map { document ->
            importOne(batchId, document, importedAtEpochMillis)
        }
        val resolved = PackageAssociationResolver.resolve(results.map { it.record })
        resolved.forEach { record ->
            catalog.updateAssociation(record.id, record.associationGroupId, record.associationConfidence)
        }
        val resolvedById = resolved.associateBy { it.id }
        return results.map { result -> result.copy(record = resolvedById.getValue(result.record.id)) }
    }

    private fun importOne(
        batchId: ImportBatchId,
        document: ImportDocumentRequest,
        importedAtEpochMillis: Long,
    ): SampleImportResult {
        val sampleId = SampleId(UUID.randomUUID().toString())
        val safeName = sanitizeFileName(document.displayName)
        val relativePath = "samples/${sampleId.value}/$safeName"
        val target = File(appContext.filesDir, relativePath)
        val role = determineRole(document.formatCode, document.displayName)
        val initialRecord = SampleRecord(
            id = sampleId,
            importBatchId = batchId,
            displayName = document.displayName,
            sourceUri = document.uri.toString(),
            internalRelativePath = relativePath,
            formatCode = document.formatCode,
            mimeType = document.mimeType,
            sha256 = null,
            sizeBytes = 0,
            importedAtEpochMillis = importedAtEpochMillis,
            status = SampleImportStatus.PENDING,
            role = role,
            associationGroupId = SampleGroupId("batch:${batchId.value}"),
            associationConfidence = SampleAssociationConfidence.IMPORT_BATCH_ONLY,
        )
        catalog.insert(initialRecord)

        var bytesCopied = 0L
        var status = SampleImportStatus.FAILED
        var errorMessage: String? = null
        var contentHash: String? = null
        var storedPath: String? = null
        val digest = MessageDigest.getInstance("SHA-256")

        try {
            val input = contentResolver.openInputStream(document.uri)
                ?: throw IllegalStateException("O provedor não abriu o documento selecionado.")
            input.use { source ->
                target.parentFile?.let { parent ->
                    if (!parent.exists() && !parent.mkdirs()) {
                        throw IllegalStateException("Não foi possível criar a pasta privada da amostra.")
                    }
                }
                check(target.createNewFile()) { "O destino interno já existe; nenhuma sobrescrita foi realizada." }
                FileOutputStream(target, false).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_SIZE)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        bytesCopied += count
                    }
                    output.fd.sync()
                }
            }
            status = SampleImportStatus.IMPORTED
            contentHash = digest.digest().toHexString()
            storedPath = relativePath
        } catch (exception: Exception) {
            errorMessage = exception.message ?: "Falha desconhecida durante a importação."
            status = if (target.isFile && target.length() > 0) {
                SampleImportStatus.PARTIAL
            } else {
                SampleImportStatus.FAILED
            }
            if (bytesCopied > 0) contentHash = digest.digest().toHexString()
            if (target.exists()) storedPath = relativePath
        }

        val duplicate = if (status == SampleImportStatus.IMPORTED && contentHash != null) {
            catalog.findBySha256(contentHash).firstOrNull { it.status == SampleImportStatus.IMPORTED }?.id
        } else {
            null
        }
        catalog.updateImportOutcome(
            id = sampleId,
            status = status,
            internalRelativePath = storedPath,
            sha256 = contentHash,
            sizeBytes = bytesCopied,
            duplicateOfSampleId = duplicate,
        )

        return SampleImportResult(
            record = initialRecord.copy(
                internalRelativePath = storedPath,
                sha256 = contentHash,
                sizeBytes = bytesCopied,
                status = status,
                duplicateOfSampleId = duplicate,
            ),
            errorMessage = errorMessage,
        )
    }

    private fun determineRole(formatCode: String, displayName: String): SampleRole {
        val normalizedFormat = formatCode.uppercase()
        val normalizedName = displayName.substringAfterLast('/').lowercase()
        return when (normalizedFormat) {
            "APK" -> if (normalizedName.startsWith("split_") || normalizedName.startsWith("config.")) {
                SampleRole.SPLIT_APK_CANDIDATE
            } else {
                SampleRole.BASE_APK_CANDIDATE
            }
            "OBB" -> SampleRole.OBB_CANDIDATE
            "XAPK", "APKS", "AAB" -> SampleRole.PACKAGE_CONTAINER
            else -> SampleRole.OTHER
        }
    }

    private fun sanitizeFileName(name: String): String {
        val sanitized = name.substringAfterLast('/').substringAfterLast('\\')
            .replace(UNSAFE_FILE_NAME, "_")
            .trim('.', ' ')
        return sanitized.ifBlank { "sample.bin" }
    }

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xFF)
    }

    private companion object {
        const val COPY_BUFFER_SIZE = 64 * 1024
        val UNSAFE_FILE_NAME = Regex("[^A-Za-z0-9._-]")
    }
}

data class ImportDocumentRequest(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val formatCode: String,
)

data class SampleImportResult(
    val record: SampleRecord,
    val errorMessage: String?,
)

private object PackageAssociationResolver {
    fun resolve(records: List<SampleRecord>): List<SampleRecord> = records
        .groupBy { it.importBatchId }
        .values
        .flatMap(::resolveBatch)

    private fun resolveBatch(records: List<SampleRecord>): List<SampleRecord> {
        val baseApks = records.filter { it.role == SampleRole.BASE_APK_CANDIDATE }
        val attachableRoles = setOf(
            SampleRole.BASE_APK_CANDIDATE,
            SampleRole.SPLIT_APK_CANDIDATE,
            SampleRole.OBB_CANDIDATE,
        )
        return if (baseApks.size == 1) {
            val groupId = SampleGroupId("sample:${baseApks.single().id.value}")
            records.map { record ->
                if (record.role in attachableRoles) {
                    record.copy(
                        associationGroupId = groupId,
                        associationConfidence = SampleAssociationConfidence.HEURISTIC_SINGLE_BASE_IN_BATCH,
                    )
                } else {
                    record.copy(
                        associationGroupId = SampleGroupId("sample:${record.id.value}"),
                        associationConfidence = SampleAssociationConfidence.IMPORT_BATCH_ONLY,
                    )
                }
            }
        } else {
            records.map { record ->
                when (record.role) {
                    SampleRole.BASE_APK_CANDIDATE, SampleRole.PACKAGE_CONTAINER, SampleRole.OTHER -> record.copy(
                        associationGroupId = SampleGroupId("sample:${record.id.value}"),
                        associationConfidence = SampleAssociationConfidence.IMPORT_BATCH_ONLY,
                    )
                    SampleRole.SPLIT_APK_CANDIDATE, SampleRole.OBB_CANDIDATE -> record.copy(
                        associationGroupId = SampleGroupId("batch:${record.importBatchId.value}"),
                        associationConfidence = if (baseApks.size > 1) {
                            SampleAssociationConfidence.AMBIGUOUS_BATCH
                        } else {
                            SampleAssociationConfidence.IMPORT_BATCH_ONLY
                        },
                    )
                }
            }
        }
    }
}
