package br.com.reverselab.core.storage

import android.content.Context
import br.com.reverselab.core.domain.ArchiveInventoryRecord
import br.com.reverselab.core.domain.ImportBatchId
import br.com.reverselab.core.domain.SampleAssociationReviewRecord
import br.com.reverselab.core.domain.SampleId
import br.com.reverselab.core.domain.SampleImportStatus
import br.com.reverselab.core.domain.SampleRecord
import java.io.File

class ImportHistoryRepository(
    context: Context,
    private val catalog: br.com.reverselab.core.domain.SampleCatalog,
) {
    private val samplesDirectory = File(context.applicationContext.filesDir, SAMPLES_DIRECTORY)

    fun loadSnapshot(): ImportHistorySnapshot {
        val records = catalog.listAll()
        val batches = records.groupBy { it.importBatchId }
            .map { (batchId, samples) -> batchSummary(batchId, samples) }
            .sortedByDescending { it.importedAtEpochMillis }

        var fileCount = 0
        var bytesOnDisk = 0L
        if (samplesDirectory.exists()) {
            samplesDirectory.walkTopDown().filter { it.isFile }.forEach { file ->
                fileCount++
                bytesOnDisk += file.length()
            }
        }

        val usage = StorageUsageSnapshot(
            bytesOnDisk = bytesOnDisk,
            fileCount = fileCount,
            cataloguedSamples = records.size,
            importedSamples = records.count { it.status == SampleImportStatus.IMPORTED },
            partialSamples = records.count { it.status == SampleImportStatus.PARTIAL },
            failedSamples = records.count { it.status == SampleImportStatus.FAILED },
            pendingSamples = records.count { it.status == SampleImportStatus.PENDING },
            duplicateCandidates = records.count { it.duplicateOfSampleId != null },
        )
        val archiveInventories = records.mapNotNull { sample ->
            catalog.listArchiveInventories(sample.id).maxByOrNull { it.inspectedAtEpochMillis }
        }.associateBy { it.sampleId }
        return ImportHistorySnapshot(batches, usage, archiveInventories)
    }

    private fun batchSummary(batchId: ImportBatchId, samples: List<SampleRecord>) = ImportBatchSummary(
        batchId = batchId,
        importedAtEpochMillis = samples.maxOfOrNull { it.importedAtEpochMillis } ?: 0L,
        totalSamples = samples.size,
        importedSamples = samples.count { it.status == SampleImportStatus.IMPORTED },
        partialSamples = samples.count { it.status == SampleImportStatus.PARTIAL },
        failedSamples = samples.count { it.status == SampleImportStatus.FAILED },
        pendingSamples = samples.count { it.status == SampleImportStatus.PENDING },
        totalBytes = samples.sumOf { it.sizeBytes },
        duplicateCandidates = samples.count { it.duplicateOfSampleId != null },
        formats = samples.map { it.formatCode }.distinct().sorted(),
        samples = samples,
        associationReviews = catalog.listAssociationReviews(batchId),
    )

    private companion object { const val SAMPLES_DIRECTORY = "samples" }
}

data class ImportHistorySnapshot(
    val batches: List<ImportBatchSummary>,
    val storage: StorageUsageSnapshot,
    val archiveInventories: Map<SampleId, ArchiveInventoryRecord>,
)

data class ImportBatchSummary(
    val batchId: ImportBatchId,
    val importedAtEpochMillis: Long,
    val totalSamples: Int,
    val importedSamples: Int,
    val partialSamples: Int,
    val failedSamples: Int,
    val pendingSamples: Int,
    val totalBytes: Long,
    val duplicateCandidates: Int,
    val formats: List<String>,
    val samples: List<SampleRecord>,
    val associationReviews: List<SampleAssociationReviewRecord>,
)

data class StorageUsageSnapshot(
    val bytesOnDisk: Long,
    val fileCount: Int,
    val cataloguedSamples: Int,
    val importedSamples: Int,
    val partialSamples: Int,
    val failedSamples: Int,
    val pendingSamples: Int,
    val duplicateCandidates: Int,
)
