package br.com.reverselab.core.storage

enum class StorageAction {
    READ, CREATE_DERIVED, EXPORT_COPY, RELEASE_ACCESS,
    DELETE, OVERWRITE, CLEANUP, PHYSICAL_DEDUPLICATION,
}

data class StorageOperationRequest(
    val requestId: String,
    val action: StorageAction,
    val targetIds: Set<String>,
    val initiatedByUser: Boolean,
    val scopeDescription: String,
) {
    init {
        require(requestId.isNotBlank())
        require(targetIds.isNotEmpty() && targetIds.none(String::isBlank))
        require(scopeDescription.isNotBlank())
    }
}

data class ExplicitUserConfirmation(
    val requestId: String,
    val action: StorageAction,
    val confirmedTargetIds: Set<String>,
    val confirmedAtEpochMillis: Long,
) {
    init {
        require(requestId.isNotBlank())
        require(confirmedTargetIds.isNotEmpty() && confirmedTargetIds.none(String::isBlank))
        require(confirmedAtEpochMillis > 0)
    }
}

enum class StorageDecision { ALLOW, REQUIRE_EXPLICIT_CONFIRMATION, DENY }

/** Central policy gate. It does not perform file operations itself. */
object StorageGovernance {
    private val destructiveActions = setOf(
        StorageAction.DELETE,
        StorageAction.OVERWRITE,
        StorageAction.CLEANUP,
        StorageAction.PHYSICAL_DEDUPLICATION,
    )

    fun evaluate(
        request: StorageOperationRequest,
        confirmation: ExplicitUserConfirmation? = null,
    ): StorageDecision {
        if (request.action !in destructiveActions) {
            return if (request.action == StorageAction.RELEASE_ACCESS && !request.initiatedByUser) {
                StorageDecision.DENY
            } else {
                StorageDecision.ALLOW
            }
        }

        if (!request.initiatedByUser) return StorageDecision.DENY
        if (confirmation == null) return StorageDecision.REQUIRE_EXPLICIT_CONFIRMATION

        val matchesExactScope = confirmation.requestId == request.requestId &&
            confirmation.action == request.action &&
            confirmation.confirmedTargetIds == request.targetIds

        return if (matchesExactScope && confirmation.confirmedAtEpochMillis > 0) {
            StorageDecision.ALLOW
        } else {
            StorageDecision.REQUIRE_EXPLICIT_CONFIRMATION
        }
    }
}
