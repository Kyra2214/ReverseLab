package br.com.reverselab.core.domain

@JvmInline
value class ArchiveInspectionId(val value: String) {
    init { require(value.isNotBlank()) }
}

enum class ArchiveInspectionStatus { INSPECTED, TRUNCATED, NOT_ARCHIVE, INVALID_ARCHIVE, FAILED }

data class ArchiveEntryRecord(
    val path: String,
    val compressedBytes: Long,
    val uncompressedBytes: Long,
    val isDirectory: Boolean,
    val warnings: List<String>,
) {
    init {
        require(path.isNotBlank())
        require(compressedBytes >= -1)
        require(uncompressedBytes >= -1)
    }
}

data class ArchiveInventoryRecord(
    val id: ArchiveInspectionId,
    val sampleId: SampleId,
    val inspectedAtEpochMillis: Long,
    val declaredFormat: String,
    val detectedContainerType: String?,
    val status: ArchiveInspectionStatus,
    val entryCount: Int,
    val entries: List<ArchiveEntryRecord>,
    val hasAndroidManifest: Boolean,
    val dexEntryCount: Int,
    val hasResourcesTable: Boolean,
    val hasNativeLibraries: Boolean,
    val hasXapkManifest: Boolean,
    val hasApksToc: Boolean,
    val hasBundleConfig: Boolean,
    val apkEntryCount: Int,
    val obbEntryCount: Int,
    val suspiciousEntryCount: Int,
    val warnings: List<String>,
    val truncated: Boolean,
    val manifestPackageName: String? = null,
    val manifestSplitName: String? = null,
    val manifestConfigForSplit: String? = null,
    val manifestUsesSplits: List<String> = emptyList(),
    val manifestParseStatus: String? = null,
) {
    init {
        require(inspectedAtEpochMillis > 0)
        require(declaredFormat.isNotBlank())
        require(entryCount >= 0)
        require(dexEntryCount >= 0)
        require(apkEntryCount >= 0)
        require(obbEntryCount >= 0)
        require(suspiciousEntryCount >= 0)
    }
}
