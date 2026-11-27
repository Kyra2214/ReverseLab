package br.com.reverselab.core.storage

import android.util.Log
import br.com.reverselab.core.domain.DiagnosticEvent
import br.com.reverselab.core.domain.DiagnosticLevel
import br.com.reverselab.core.domain.DiagnosticLogger
import org.json.JSONObject

class LogcatDiagnosticLogger : DiagnosticLogger {
    override fun write(event: DiagnosticEvent) {
        val safeAttributes = JSONObject()
        event.attributes.forEach { (key, value) ->
            safeAttributes.put(key, SensitiveDataRedactor.redact(key, value))
        }
        val payload = JSONObject()
            .put("event", event.name)
            .put("correlationId", event.correlationId)
            .put("occurredAt", event.occurredAtEpochMillis)
            .put("attributes", safeAttributes)
            .toString()

        when (event.level) {
            DiagnosticLevel.DEBUG -> Log.d(TAG, payload)
            DiagnosticLevel.INFO -> Log.i(TAG, payload)
            DiagnosticLevel.WARNING -> Log.w(TAG, payload)
            DiagnosticLevel.ERROR -> Log.e(TAG, payload)
        }
    }

    private companion object { const val TAG = "ReverseLab" }
}

object SensitiveDataRedactor {
    private val sensitiveKey = Regex(
        ".*(authorization|token|password|secret|api[_-]?key|credential|cookie|session|private[_-]?key).*",
        RegexOption.IGNORE_CASE,
    )
    private val bearerValue = Regex("(?i)(bearer\\s+)[A-Za-z0-9._~+/-]+=*")
    private val querySecret = Regex("(?i)(access_token|api_key|token|password|secret)=([^&\\s]+)")

    fun redact(key: String, value: String): String {
        if (sensitiveKey.matches(key)) return "[REDACTED]"
        return value
            .replace(bearerValue, "\\$1[REDACTED]")
            .replace(querySecret, "\\$1=[REDACTED]")
    }
}
