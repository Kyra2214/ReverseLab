package br.com.reverselab.core.storage

import android.content.ContentResolver
import android.net.Uri
import br.com.reverselab.core.domain.ExplicitUserConfirmation
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Exports the local analysis database to a new user-selected SAF document. */
class SafBackupExporter(
    private val contentResolver: ContentResolver,
    private val analysisRunStore: SqliteAnalysisRunStore,
) {
    fun export(request: BackupExportRequest): BackupExportReceipt {
        require(request.initiatedByUser) { "A exportação precisa ser iniciada pelo usuário." }
        require(request.createdAtEpochMillis > 0)
        val action = if (request.destinationIsNewDocument) {
            StorageAction.EXPORT_COPY
        } else {
            StorageAction.OVERWRITE
        }
        val gateRequest = StorageOperationRequest(
            requestId = request.requestId,
            action = action,
            targetIds = setOf(request.destination.toString()),
            initiatedByUser = request.initiatedByUser,
            scopeDescription = "Exportação do backup local do ReverseLab",
        )
        val decision = StorageGovernance.evaluate(gateRequest, request.overwriteConfirmation)
        check(decision == StorageDecision.ALLOW) {
            "O destino exige confirmação explícita de sobrescrita ou não está autorizado."
        }

        val rawOutput = contentResolver.openOutputStream(request.destination, "w")
            ?: throw IOException("Não foi possível abrir o destino escolhido para backup.")

        rawOutput.use { output ->
            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                val manifest = JSONObject()
                    .put("format", "ReverseLabBackup")
                    .put("formatVersion", FORMAT_VERSION)
                    .put("createdAtEpochMillis", request.createdAtEpochMillis)
                    .put("database", DATABASE_ENTRY)
                    .put("databaseSchemaVersion", DATABASE_SCHEMA_VERSION)
                    .put(
                        "includedTables",
                        JSONArray().put("analysis_runs").put("analysis_run_events"),
                    )
                    .put("excludesOriginalSamples", true)
                    .put("excludesCredentials", true)

                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                zip.putNextEntry(ZipEntry(DATABASE_ENTRY))
                analysisRunStore.writeSnapshotTo(zip)
                zip.closeEntry()
                zip.finish()
            }
        }

        return BackupExportReceipt(
            destination = request.destination,
            createdAtEpochMillis = request.createdAtEpochMillis,
            formatVersion = FORMAT_VERSION,
        )
    }

    private companion object {
        const val FORMAT_VERSION = 1
        const val DATABASE_SCHEMA_VERSION = 1
        const val MANIFEST_ENTRY = "manifest.json"
        const val DATABASE_ENTRY = "analysis-state.db"
    }
}

data class BackupExportRequest(
    val requestId: String,
    val destination: Uri,
    val createdAtEpochMillis: Long,
    val initiatedByUser: Boolean,
    val destinationIsNewDocument: Boolean,
    val overwriteConfirmation: ExplicitUserConfirmation? = null,
) {
    init {
        require(requestId.isNotBlank())
        require(destination.scheme == "content") { "O destino precisa ser um URI SAF." }
    }
}

data class BackupExportReceipt(
    val destination: Uri,
    val createdAtEpochMillis: Long,
    val formatVersion: Int,
)
