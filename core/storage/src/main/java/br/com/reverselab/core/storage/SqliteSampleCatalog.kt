package br.com.reverselab.core.storage

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import br.com.reverselab.core.domain.ImportBatchId
import br.com.reverselab.core.domain.SampleAssociationConfidence
import br.com.reverselab.core.domain.SampleCatalog
import br.com.reverselab.core.domain.SampleGroupId
import br.com.reverselab.core.domain.SampleId
import br.com.reverselab.core.domain.SampleImportStatus
import br.com.reverselab.core.domain.SampleRecord
import br.com.reverselab.core.domain.SampleRole

class SqliteSampleCatalog(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
), SampleCatalog {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE samples (
                id TEXT PRIMARY KEY NOT NULL,
                import_batch_id TEXT NOT NULL,
                display_name TEXT NOT NULL,
                source_uri TEXT NOT NULL,
                internal_relative_path TEXT,
                format_code TEXT NOT NULL,
                mime_type TEXT,
                sha256 TEXT,
                size_bytes INTEGER NOT NULL,
                imported_at INTEGER NOT NULL,
                status TEXT NOT NULL,
                role TEXT NOT NULL,
                association_group_id TEXT NOT NULL,
                association_confidence TEXT NOT NULL,
                duplicate_of_sample_id TEXT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX index_samples_batch ON samples(import_batch_id)")
        db.execSQL("CREATE INDEX index_samples_sha256 ON samples(sha256)")
        db.execSQL("CREATE INDEX index_samples_group ON samples(association_group_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Migração do catálogo de amostras não definida: $oldVersion -> $newVersion")
    }

    @Synchronized
    override fun insert(record: SampleRecord) {
        writableDatabase.insertOrThrow(TABLE_SAMPLES, null, record.toContentValues())
    }

    @Synchronized
    override fun listAll(): List<SampleRecord> = querySamples(null, null)

    @Synchronized
    override fun findByBatch(batchId: ImportBatchId): List<SampleRecord> =
        querySamples("import_batch_id = ?", arrayOf(batchId.value))

    @Synchronized
    override fun findBySha256(sha256: String): List<SampleRecord> {
        require(sha256.matches(Regex("^[a-fA-F0-9]{64}$")))
        return querySamples("sha256 = ?", arrayOf(sha256))
    }

    @Synchronized
    override fun updateImportOutcome(
        id: SampleId,
        status: SampleImportStatus,
        internalRelativePath: String?,
        sha256: String?,
        sizeBytes: Long,
        duplicateOfSampleId: SampleId?,
    ) {
        require(sizeBytes >= 0)
        require(sha256 == null || sha256.matches(Regex("^[a-fA-F0-9]{64}$")))
        val changed = writableDatabase.update(
            TABLE_SAMPLES,
            ContentValues().apply {
                put("status", status.name)
                put("internal_relative_path", internalRelativePath)
                put("sha256", sha256)
                put("size_bytes", sizeBytes)
                put("duplicate_of_sample_id", duplicateOfSampleId?.value)
            },
            "id = ?",
            arrayOf(id.value),
        )
        check(changed == 1) { "Amostra não encontrada para atualização de importação: ${id.value}" }
    }

    @Synchronized
    override fun updateAssociation(
        id: SampleId,
        groupId: SampleGroupId,
        confidence: SampleAssociationConfidence,
    ) {
        val changed = writableDatabase.update(
            TABLE_SAMPLES,
            ContentValues().apply {
                put("association_group_id", groupId.value)
                put("association_confidence", confidence.name)
            },
            "id = ?",
            arrayOf(id.value),
        )
        check(changed == 1) { "Amostra não encontrada para atualização de associação: ${id.value}" }
    }

    private fun querySamples(selection: String?, selectionArgs: Array<String>?): List<SampleRecord> =
        readableDatabase.query(
            TABLE_SAMPLES, null, selection, selectionArgs, null, null, "imported_at DESC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toSampleRecord())
            }
        }

    private fun SampleRecord.toContentValues() = ContentValues().apply {
        put("id", id.value)
        put("import_batch_id", importBatchId.value)
        put("display_name", displayName)
        put("source_uri", sourceUri)
        put("internal_relative_path", internalRelativePath)
        put("format_code", formatCode)
        put("mime_type", mimeType)
        put("sha256", sha256)
        put("size_bytes", sizeBytes)
        put("imported_at", importedAtEpochMillis)
        put("status", status.name)
        put("role", role.name)
        put("association_group_id", associationGroupId.value)
        put("association_confidence", associationConfidence.name)
        put("duplicate_of_sample_id", duplicateOfSampleId?.value)
    }

    private fun Cursor.toSampleRecord() = SampleRecord(
        id = SampleId(getString(getColumnIndexOrThrow("id"))),
        importBatchId = ImportBatchId(getString(getColumnIndexOrThrow("import_batch_id"))),
        displayName = getString(getColumnIndexOrThrow("display_name")),
        sourceUri = getString(getColumnIndexOrThrow("source_uri")),
        internalRelativePath = getNullableString("internal_relative_path"),
        formatCode = getString(getColumnIndexOrThrow("format_code")),
        mimeType = getNullableString("mime_type"),
        sha256 = getNullableString("sha256"),
        sizeBytes = getLong(getColumnIndexOrThrow("size_bytes")),
        importedAtEpochMillis = getLong(getColumnIndexOrThrow("imported_at")),
        status = SampleImportStatus.valueOf(getString(getColumnIndexOrThrow("status"))),
        role = SampleRole.valueOf(getString(getColumnIndexOrThrow("role"))),
        associationGroupId = SampleGroupId(getString(getColumnIndexOrThrow("association_group_id"))),
        associationConfidence = SampleAssociationConfidence.valueOf(
            getString(getColumnIndexOrThrow("association_confidence")),
        ),
        duplicateOfSampleId = getNullableString("duplicate_of_sample_id")?.let(::SampleId),
    )

    private fun Cursor.getNullableString(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private companion object {
        const val DATABASE_NAME = "reverselab_samples.db"
        const val DATABASE_VERSION = 1
        const val TABLE_SAMPLES = "samples"
    }
}
