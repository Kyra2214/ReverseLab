package br.com.reverselab.core.storage

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import br.com.reverselab.core.domain.AnalysisEngine
import br.com.reverselab.core.domain.AnalysisRun
import br.com.reverselab.core.domain.AnalysisRunId
import br.com.reverselab.core.domain.AnalysisRunStore
import br.com.reverselab.core.domain.AnalysisScope
import br.com.reverselab.core.domain.EvidenceId
import br.com.reverselab.core.domain.ProcessingState
import br.com.reverselab.core.domain.ProjectId
import br.com.reverselab.core.domain.SampleId
import br.com.reverselab.core.domain.canTransitionTo
import org.json.JSONArray

class SqliteAnalysisRunStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
), AnalysisRunStore {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE analysis_runs (
                id TEXT PRIMARY KEY NOT NULL,
                project_id TEXT NOT NULL,
                module_path TEXT NOT NULL,
                sample_ids TEXT NOT NULL,
                evidence_ids TEXT NOT NULL,
                includes_cross_module INTEGER NOT NULL,
                engine TEXT NOT NULL,
                provider TEXT,
                model TEXT,
                prompt_version TEXT,
                state TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                input_tokens INTEGER,
                output_tokens INTEGER,
                cost_micros INTEGER,
                cost_currency TEXT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX index_analysis_runs_project ON analysis_runs(project_id)")
        db.execSQL("CREATE INDEX index_analysis_runs_state ON analysis_runs(state)")
        db.execSQL(
            """
            CREATE TABLE analysis_run_events (
                event_id INTEGER PRIMARY KEY AUTOINCREMENT,
                run_id TEXT NOT NULL,
                from_state TEXT NOT NULL,
                to_state TEXT NOT NULL,
                occurred_at INTEGER NOT NULL,
                FOREIGN KEY(run_id) REFERENCES analysis_runs(id)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX index_analysis_run_events_run ON analysis_run_events(run_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future schema changes must use additive, data-preserving migrations.
        error("Migração SQLite não definida: $oldVersion -> $newVersion")
    }

    override fun insert(run: AnalysisRun) {
        writableDatabase.insertOrThrow(TABLE_RUNS, null, run.toContentValues())
    }

    override fun findById(id: AnalysisRunId): AnalysisRun? = readableDatabase.query(
        TABLE_RUNS, null, "id = ?", arrayOf(id.value), null, null, null, "1",
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toAnalysisRun() else null }

    override fun findByProject(projectId: ProjectId): List<AnalysisRun> =
        readableDatabase.query(
            TABLE_RUNS, null, "project_id = ?", arrayOf(projectId.value),
            null, null, "created_at DESC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toAnalysisRun())
            }
        }

    override fun transitionState(
        id: AnalysisRunId,
        expected: ProcessingState,
        next: ProcessingState,
        occurredAtEpochMillis: Long,
    ): Boolean {
        require(occurredAtEpochMillis > 0)
        require(expected.canTransitionTo(next)) { "Transição de estado não permitida: $expected -> $next" }

        val db = writableDatabase
        db.beginTransaction()
        try {
            val current = db.query(
                TABLE_RUNS, arrayOf("state"), "id = ?", arrayOf(id.value),
                null, null, null, "1",
            ).use { cursor ->
                if (cursor.moveToFirst()) ProcessingState.valueOf(cursor.getString(0)) else null
            }
            if (current != expected) return false

            val changed = db.update(
                TABLE_RUNS,
                ContentValues().apply { put("state", next.name) },
                "id = ? AND state = ?",
                arrayOf(id.value, expected.name),
            )
            if (changed != 1) return false

            db.insertOrThrow(
                TABLE_EVENTS,
                null,
                ContentValues().apply {
                    put("run_id", id.value)
                    put("from_state", expected.name)
                    put("to_state", next.name)
                    put("occurred_at", occurredAtEpochMillis)
                },
            )
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    private fun AnalysisRun.toContentValues() = ContentValues().apply {
        put("id", id.value)
        put("project_id", projectId.value)
        put("module_path", scope.modulePath)
        put("sample_ids", JSONArray().apply { scope.sampleIds.forEach { put(it.value) } }.toString())
        put("evidence_ids", JSONArray().apply { scope.evidenceIds.forEach { put(it.value) } }.toString())
        put("includes_cross_module", if (scope.includesCrossModuleData) 1 else 0)
        put("engine", engine.name)
        put("provider", provider)
        put("model", model)
        put("prompt_version", promptVersion)
        put("state", state.name)
        put("created_at", createdAtEpochMillis)
        put("input_tokens", inputTokens)
        put("output_tokens", outputTokens)
        put("cost_micros", costMicros)
        put("cost_currency", costCurrency)
    }

    private fun Cursor.toAnalysisRun(): AnalysisRun {
        val sampleIds = JSONArray(getString(getColumnIndexOrThrow("sample_ids")))
        val evidenceIds = JSONArray(getString(getColumnIndexOrThrow("evidence_ids")))
        return AnalysisRun(
            id = AnalysisRunId(getString(getColumnIndexOrThrow("id"))),
            projectId = ProjectId(getString(getColumnIndexOrThrow("project_id"))),
            scope = AnalysisScope(
                modulePath = getString(getColumnIndexOrThrow("module_path")),
                sampleIds = buildSet {
                    for (index in 0 until sampleIds.length()) add(SampleId(sampleIds.getString(index)))
                },
                evidenceIds = buildSet {
                    for (index in 0 until evidenceIds.length()) add(EvidenceId(evidenceIds.getString(index)))
                },
                includesCrossModuleData = getInt(getColumnIndexOrThrow("includes_cross_module")) == 1,
            ),
            engine = AnalysisEngine.valueOf(getString(getColumnIndexOrThrow("engine"))),
            provider = getNullableString("provider"),
            model = getNullableString("model"),
            promptVersion = getNullableString("prompt_version"),
            state = ProcessingState.valueOf(getString(getColumnIndexOrThrow("state"))),
            createdAtEpochMillis = getLong(getColumnIndexOrThrow("created_at")),
            inputTokens = getNullableLong("input_tokens"),
            outputTokens = getNullableLong("output_tokens"),
            costMicros = getNullableLong("cost_micros"),
            costCurrency = getNullableString("cost_currency"),
        )
    }

    private fun Cursor.getNullableString(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private fun Cursor.getNullableLong(column: String): Long? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getLong(index)
    }

    private companion object {
        const val DATABASE_NAME = "reverselab_state.db"
        const val DATABASE_VERSION = 1
        const val TABLE_RUNS = "analysis_runs"
        const val TABLE_EVENTS = "analysis_run_events"
    }
}
