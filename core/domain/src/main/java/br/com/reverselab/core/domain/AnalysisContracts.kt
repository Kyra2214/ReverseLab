package br.com.reverselab.core.domain

@JvmInline
value class ProjectId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline
value class SampleId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline
value class EvidenceId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline
value class AnalysisRunId(val value: String) { init { require(value.isNotBlank()) } }

enum class EvidenceClassification { OBSERVED, INTERPRETED, HYPOTHESIS, VALIDATED }
enum class EvidenceKind { FILE, CODE_SYMBOL, RESOURCE, LOG_EVENT, MEASUREMENT, SCREENSHOT, OTHER }
enum class AnalysisEngine { DETERMINISTIC, LOCAL_LLM, EXTERNAL_API }

enum class ProcessingState {
    PENDING, QUEUED, RUNNING, PAUSED, PARTIAL, FAILED_RECOVERABLE,
    CANCEL_REQUESTED, CANCELLED, NEEDS_REVIEW, COMPLETED,
}

fun ProcessingState.canTransitionTo(next: ProcessingState): Boolean = when (this) {
    ProcessingState.PENDING -> next in setOf(ProcessingState.QUEUED, ProcessingState.CANCELLED)
    ProcessingState.QUEUED -> next in setOf(
        ProcessingState.RUNNING, ProcessingState.CANCELLED, ProcessingState.FAILED_RECOVERABLE,
    )
    ProcessingState.RUNNING -> next in setOf(
        ProcessingState.PAUSED, ProcessingState.PARTIAL, ProcessingState.FAILED_RECOVERABLE,
        ProcessingState.CANCEL_REQUESTED, ProcessingState.NEEDS_REVIEW, ProcessingState.COMPLETED,
    )
    ProcessingState.PAUSED -> next in setOf(ProcessingState.QUEUED, ProcessingState.CANCELLED)
    ProcessingState.PARTIAL -> next in setOf(ProcessingState.QUEUED, ProcessingState.NEEDS_REVIEW)
    ProcessingState.FAILED_RECOVERABLE -> next in setOf(ProcessingState.QUEUED, ProcessingState.CANCELLED)
    ProcessingState.CANCEL_REQUESTED -> next in setOf(
        ProcessingState.CANCELLED, ProcessingState.PARTIAL, ProcessingState.FAILED_RECOVERABLE,
    )
    ProcessingState.NEEDS_REVIEW -> next in setOf(ProcessingState.QUEUED, ProcessingState.COMPLETED)
    ProcessingState.CANCELLED, ProcessingState.COMPLETED -> false
}

data class SourceLocator(
    val sampleId: SampleId,
    val relativePath: String,
    val entryPath: String? = null,
    val sha256: String? = null,
    val lineStart: Int? = null,
    val lineEnd: Int? = null,
) {
    init {
        require(relativePath.isNotBlank())
        require(!relativePath.startsWith('/') && !relativePath.startsWith('\\'))
        require(relativePath.replace('\\', '/').split('/').none { it == ".." })
        require(sha256 == null || SHA256_PATTERN.matches(sha256))
        require(lineStart == null || lineStart > 0)
        require(lineEnd == null || lineEnd > 0)
        require(lineStart == null || lineEnd == null || lineEnd >= lineStart)
    }
    private companion object { val SHA256_PATTERN = Regex("^[a-fA-F0-9]{64}$") }
}

data class EvidenceReference(
    val id: EvidenceId,
    val source: SourceLocator,
    val kind: EvidenceKind,
    val classification: EvidenceClassification,
    val description: String,
    val modulePath: String,
    val capturedAtEpochMillis: Long,
) {
    init {
        require(description.isNotBlank())
        require(modulePath.isNotBlank())
        require(capturedAtEpochMillis >= 0)
    }
}

data class AnalysisScope(
    val modulePath: String,
    val sampleIds: Set<SampleId>,
    val evidenceIds: Set<EvidenceId> = emptySet(),
    val includesCrossModuleData: Boolean = false,
) {
    init {
        require(modulePath.isNotBlank())
        require(sampleIds.isNotEmpty() || evidenceIds.isNotEmpty())
    }
}

data class AnalysisRun(
    val id: AnalysisRunId,
    val projectId: ProjectId,
    val scope: AnalysisScope,
    val engine: AnalysisEngine,
    val provider: String? = null,
    val model: String? = null,
    val promptVersion: String? = null,
    val state: ProcessingState = ProcessingState.PENDING,
    val createdAtEpochMillis: Long,
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val costMicros: Long? = null,
    val costCurrency: String? = null,
) {
    init {
        require(createdAtEpochMillis >= 0)
        require(inputTokens == null || inputTokens >= 0)
        require(outputTokens == null || outputTokens >= 0)
        require(costMicros == null || costMicros >= 0)
        require(costCurrency == null || costCurrency.length == 3)
        require(engine != AnalysisEngine.EXTERNAL_API || !provider.isNullOrBlank())
    }
}

data class Finding(
    val id: String,
    val title: String,
    val explanation: String,
    val classification: EvidenceClassification,
    val evidenceIds: Set<EvidenceId>,
) {
    init {
        require(id.isNotBlank())
        require(title.isNotBlank())
        require(explanation.isNotBlank())
        require(evidenceIds.isNotEmpty())
    }
}

enum class ErrorSeverity { INFO, WARNING, ERROR, CRITICAL }
enum class ErrorCode {
    FILE_ACCESS_DENIED, UNSUPPORTED_FORMAT, INVALID_ARCHIVE, INTEGRITY_MISMATCH,
    STORAGE_UNAVAILABLE, PARSE_FAILED, MODEL_UNAVAILABLE, NETWORK_UNAVAILABLE,
    EXTERNAL_PROVIDER_ERROR, INVALID_STATE_TRANSITION, UNKNOWN,
}

data class AppError(
    val code: ErrorCode,
    val severity: ErrorSeverity,
    val retryable: Boolean,
    val userMessage: String,
    val correlationId: String,
    val technicalMessage: String? = null,
) {
    init {
        require(userMessage.isNotBlank())
        require(correlationId.isNotBlank())
    }
}

enum class DiagnosticLevel { DEBUG, INFO, WARNING, ERROR }

data class DiagnosticEvent(
    val name: String,
    val level: DiagnosticLevel,
    val correlationId: String,
    val occurredAtEpochMillis: Long,
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        require(name.isNotBlank())
        require(correlationId.isNotBlank())
        require(occurredAtEpochMillis >= 0)
    }
}

interface DiagnosticLogger {
    fun write(event: DiagnosticEvent)
}

/** Persistence contract intentionally contains no delete operation. */
interface AnalysisRunStore {
    fun insert(run: AnalysisRun)
    fun findById(id: AnalysisRunId): AnalysisRun?
    fun findByProject(projectId: ProjectId): List<AnalysisRun>
    fun transitionState(
        id: AnalysisRunId,
        expected: ProcessingState,
        next: ProcessingState,
        occurredAtEpochMillis: Long,
    ): Boolean
}
