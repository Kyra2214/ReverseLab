package br.com.reverselab.core.domain

@JvmInline
value class ImportBatchId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class SampleGroupId(val value: String) {
    init { require(value.isNotBlank()) }
}

enum class SampleImportStatus { PENDING, IMPORTED, PARTIAL, FAILED }
enum class SampleRole { BASE_APK_CANDIDATE, SPLIT_APK_CANDIDATE, OBB_CANDIDATE, PACKAGE_CONTAINER, OTHER }
enum class SampleAssociationConfidence {
    IMPORT_BATCH_ONLY, HEURISTIC_SINGLE_BASE_IN_BATCH, AMBIGUOUS_BATCH, USER_CONFIRMED, MANIFEST_VERIFIED,
}

data class SampleRecord(
    val id: SampleId,
    val importBatchId: ImportBatchId,
    val displayName: String,
    val sourceUri: String,
    val internalRelativePath: String?,
    val formatCode: String,
    val mimeType: String?,
    val sha256: String?,
    val sizeBytes: Long,
    val importedAtEpochMillis: Long,
    val status: SampleImportStatus,
    val role: SampleRole,
    val associationGroupId: SampleGroupId,
    val associationConfidence: SampleAssociationConfidence,
    val duplicateOfSampleId: SampleId? = null,
) {
    init {
        require(displayName.isNotBlank())
        require(sourceUri.isNotBlank())
        require(formatCode.isNotBlank())
        require(sizeBytes >= 0)
        require(importedAtEpochMillis > 0)
        require(sha256 == null || SHA256_PATTERN.matches(sha256))
        require(internalRelativePath == null || (
            internalRelativePath.isNotBlank() &&
                !internalRelativePath.startsWith('/') &&
                internalRelativePath.replace('\\', '/').split('/').none { it == ".." }
            ))
    }

    private companion object { val SHA256_PATTERN = Regex("^[a-fA-F0-9]{64}$") }
}

/** Persistent sample catalog contract intentionally has no deletion operation. */
interface SampleCatalog {
    fun insert(record: SampleRecord)
    fun listAll(): List<SampleRecord>
    fun findByBatch(batchId: ImportBatchId): List<SampleRecord>
    fun findBySha256(sha256: String): List<SampleRecord>
    fun updateImportOutcome(
        id: SampleId,
        status: SampleImportStatus,
        internalRelativePath: String?,
        sha256: String?,
        sizeBytes: Long,
        duplicateOfSampleId: SampleId?,
    )
    fun updateAssociation(
        id: SampleId,
        groupId: SampleGroupId,
        confidence: SampleAssociationConfidence,
    )
    fun saveArchiveInventory(inventory: ArchiveInventoryRecord)
    fun listArchiveInventories(sampleId: SampleId): List<ArchiveInventoryRecord>
}
