package br.com.reverselab.core.storage

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import br.com.reverselab.core.domain.ArchiveEntryRecord
import br.com.reverselab.core.domain.ArchiveInspectionId
import br.com.reverselab.core.domain.ArchiveInspectionStatus
import br.com.reverselab.core.domain.ArchiveInventoryRecord
import br.com.reverselab.core.domain.ImportBatchId
import br.com.reverselab.core.domain.SampleAssociationConfidence
import br.com.reverselab.core.domain.SampleCatalog
import br.com.reverselab.core.domain.SampleGroupId
import br.com.reverselab.core.domain.SampleId
import br.com.reverselab.core.domain.SampleImportStatus
import br.com.reverselab.core.domain.SampleRecord
import br.com.reverselab.core.domain.SampleRole

class SqliteSampleCatalog(context: Context) : SQLiteOpenHelper(
    context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION,
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
        createArchiveInventoryTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createArchiveInventoryTable(db)
        if (newVersion > DATABASE_VERSION) error("Versão futura do catálogo de amostras não suportada: $newVersion")
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

    @Synchronized
    override fun saveArchiveInventory(inventory: ArchiveInventoryRecord) {
        writableDatabase.insertOrThrow(TABLE_ARCHIVE_INVENTORIES, null, inventory.toContentValues())
    }

    @Synchronized
    override fun listArchiveInventories(sampleId: SampleId): List<ArchiveInventoryRecord> =
        readableDatabase.query(
            TABLE_ARCHIVE_INVENTORIES, null, "sample_id = ?", arrayOf(sampleId.value),
            null, null, "inspected_at DESC",
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toArchiveInventory()) }
        }

    private fun querySamples(selection: String?, selectionArgs: Array<String>?): List<SampleRecord> =
        readableDatabase.query(TABLE_SAMPLES, null, selection, selectionArgs, null, null, "imported_at DESC").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toSampleRecord()) }
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

    private fun ArchiveInventoryRecord.toContentValues() = ContentValues().apply {
        put("id", id.value)
        put("sample_id", sampleId.value)
        put("inspected_at", inspectedAtEpochMillis)
        put("declared_format", declaredFormat)
        put("detected_container_type", detectedContainerType)
        put("status", status.name)
        put("entry_count", entryCount)
        put("entries_json", entries.toJson())
        put("has_android_manifest", if (hasAndroidManifest) 1 else 0)
        put("dex_entry_count", dexEntryCount)
        put("has_resources_table", if (hasResourcesTable) 1 else 0)
        put("has_native_libraries", if (hasNativeLibraries) 1 else 0)
        put("has_xapk_manifest", if (hasXapkManifest) 1 else 0)
        put("has_apks_toc", if (hasApksToc) 1 else 0)
        put("has_bundle_config", if (hasBundleConfig) 1 else 0)
        put("apk_entry_count", apkEntryCount)
        put("obb_entry_count", obbEntryCount)
        put("suspicious_entry_count", suspiciousEntryCount)
        put("warnings_json", warnings.toJson())
        put("truncated", if (truncated) 1 else 0)
    }

    private fun List<ArchiveEntryRecord>.toJson(): String = JSONArray().apply {
        forEach { entry ->
            put(
                JSONObject()
                    .put("path", entry.path)
                    .put("compressedBytes", entry.compressedBytes)
                    .put("uncompressedBytes", entry.uncompressedBytes)
                    .put("isDirectory", entry.isDirectory)
                    .put("warnings", entry.warnings.toJsonArray()),
            )
        }
    }.toString()

    private fun List<String>.toJson(): String = toJsonArray().toString()
    private fun List<String>.toJsonArray(): JSONArray = JSONArray().apply { forEach { put(it) } }

    private fun Cursor.toArchiveInventory(): ArchiveInventoryRecord {
        val entriesArray = JSONArray(getString(getColumnIndexOrThrow("entries_json")))
        val entries = buildList {
            for (index in 0 until entriesArray.length()) {
                val item = entriesArray.getJSONObject(index)
                add(
                    ArchiveEntryRecord(
                        path = item.getString("path"),
                        compressedBytes = item.getLong("compressedBytes"),
                        uncompressedBytes = item.getLong("uncompressedBytes"),
                        isDirectory = item.getBoolean("isDirectory"),
                        warnings = item.optJSONArray("warnings").toStringList(),
                    ),
                )
            }
        }
        return ArchiveInventoryRecord(
            id = ArchiveInspectionId(getString(getColumnIndexOrThrow("id"))),
            sampleId = SampleId(getString(getColumnIndexOrThrow("sample_id"))),
            inspectedAtEpochMillis = getLong(getColumnIndexOrThrow("inspected_at")),
            declaredFormat = getString(getColumnIndexOrThrow("declared_format")),
            detectedContainerType = getNullableString("detected_container_type"),
            status = ArchiveInspectionStatus.valueOf(getString(getColumnIndexOrThrow("status"))),
            entryCount = getInt(getColumnIndexOrThrow("entry_count")),
            entries = entries,
            hasAndroidManifest = getInt(getColumnIndexOrThrow("has_android_manifest")) != 0,
            dexEntryCount = getInt(getColumnIndexOrThrow("dex_entry_count")),
            hasResourcesTable = getInt(getColumnIndexOrThrow("has_resources_table")) != 0,
            hasNativeLibraries = getInt(getColumnIndexOrThrow("has_native_libraries")) != 0,
            hasXapkManifest = getInt(getColumnIndexOrThrow("has_xapk_manifest")) != 0,
            hasApksToc = getInt(getColumnIndexOrThrow("has_apks_toc")) != 0,
            hasBundleConfig = getInt(getColumnIndexOrThrow("has_bundle_config")) != 0,
            apkEntryCount = getInt(getColumnIndexOrThrow("apk_entry_count")),
            obbEntryCount = getInt(getColumnIndexOrThrow("obb_entry_count")),
            suspiciousEntryCount = getInt(getColumnIndexOrThrow("suspicious_entry_count")),
            warnings = JSONArray(getString(getColumnIndexOrThrow("warnings_json"))).toStringList(),
            truncated = getInt(getColumnIndexOrThrow("truncated")) != 0,
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList { for (index in 0 until length()) add(optString(index)) }
    }

    private fun createArchiveInventoryTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS archive_inventories (
                id TEXT PRIMARY KEY NOT NULL,
                sample_id TEXT NOT NULL,
                inspected_at INTEGER NOT NULL,
                declared_format TEXT NOT NULL,
                detected_container_type TEXT,
                status TEXT NOT NULL,
                entry_count INTEGER NOT NULL,
                entries_json TEXT NOT NULL,
                has_android_manifest INTEGER NOT NULL,
                dex_entry_count INTEGER NOT NULL,
                has_resources_table INTEGER NOT NULL,
                has_native_libraries INTEGER NOT NULL,
                has_xapk_manifest INTEGER NOT NULL,
                has_apks_toc INTEGER NOT NULL,
                has_bundle_config INTEGER NOT NULL,
                apk_entry_count INTEGER NOT NULL,
                obb_entry_count INTEGER NOT NULL,
                suspicious_entry_count INTEGER NOT NULL,
                warnings_json TEXT NOT NULL,
                truncated INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_archive_inventory_sample ON archive_inventories(sample_id, inspected_at DESC)")
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
        associationConfidence = SampleAssociationConfidence.valueOf(getString(getColumnIndexOrThrow("association_confidence"))),
        duplicateOfSampleId = getNullableString("duplicate_of_sample_id")?.let(::SampleId),
    )

    private fun Cursor.getNullableString(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private companion object {
        const val DATABASE_NAME = "reverselab_samples.db"
        const val DATABASE_VERSION = 2
        const val TABLE_SAMPLES = "samples"
        const val TABLE_ARCHIVE_INVENTORIES = "archive_inventories"
    }
}
