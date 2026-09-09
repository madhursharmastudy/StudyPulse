package com.example.core.logging

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentLinkedDeque

object AppLogger {
    private const val TAG = "StudyWatch"
    private const val MAX_LOG_SIZE = 5000

    private val logBuffer = ConcurrentLinkedDeque<LogEntry>()
    private val _logsStateFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsStateFlow: StateFlow<List<LogEntry>> = _logsStateFlow.asStateFlow()

    private var logPersister: ((LogEntry) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun setLogPersister(persister: (LogEntry) -> Unit) {
        this.logPersister = persister
    }

    fun d(
        feature: LogFeature,
        event: String,
        details: String? = null,
        timerState: String? = null,
        sessionUid: String? = null,
        timerId: String? = null,
        subjectName: String? = null,
        topicName: String? = null
    ) {
        log(LogLevel.DEBUG, feature, event, details, timerState, sessionUid, timerId, subjectName, topicName, null)
    }

    fun i(
        feature: LogFeature,
        event: String,
        details: String? = null,
        timerState: String? = null,
        sessionUid: String? = null,
        timerId: String? = null,
        subjectName: String? = null,
        topicName: String? = null
    ) {
        log(LogLevel.INFO, feature, event, details, timerState, sessionUid, timerId, subjectName, topicName, null)
    }

    fun w(
        feature: LogFeature,
        event: String,
        details: String? = null,
        timerState: String? = null,
        sessionUid: String? = null,
        timerId: String? = null,
        throwable: Throwable? = null
    ) {
        log(LogLevel.WARNING, feature, event, details, timerState, sessionUid, timerId, null, null, throwable)
    }

    fun e(
        feature: LogFeature,
        event: String,
        details: String? = null,
        timerState: String? = null,
        sessionUid: String? = null,
        timerId: String? = null,
        throwable: Throwable? = null
    ) {
        log(LogLevel.ERROR, feature, event, details, timerState, sessionUid, timerId, null, null, throwable)
    }

    fun c(
        feature: LogFeature,
        event: String,
        details: String? = null,
        timerState: String? = null,
        sessionUid: String? = null,
        timerId: String? = null,
        throwable: Throwable? = null
    ) {
        log(LogLevel.CRITICAL, feature, event, details, timerState, sessionUid, timerId, null, null, throwable)
    }

    private fun log(
        level: LogLevel,
        feature: LogFeature,
        event: String,
        details: String?,
        timerState: String?,
        sessionUid: String?,
        timerId: String?,
        subjectName: String?,
        topicName: String?,
        throwable: Throwable?
    ) {
        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        }
        val entry = LogEntry(
            level = level,
            feature = feature,
            event = event,
            timerState = timerState,
            sessionUid = sessionUid,
            timerId = timerId,
            subjectName = subjectName,
            topicName = topicName,
            details = details,
            exceptionMessage = throwable?.message,
            stackTrace = stackTrace
        )

        // Log to Android logcat
        when (level) {
            LogLevel.DEBUG -> Log.d(TAG, entry.toFormattedString())
            LogLevel.INFO -> Log.i(TAG, entry.toFormattedString())
            LogLevel.WARNING -> Log.w(TAG, entry.toFormattedString(), throwable)
            LogLevel.ERROR -> Log.e(TAG, entry.toFormattedString(), throwable)
            LogLevel.CRITICAL -> Log.e(TAG, "CRITICAL: " + entry.toFormattedString(), throwable)
        }

        // Add to in-memory circular buffer
        logBuffer.addFirst(entry)
        while (logBuffer.size > MAX_LOG_SIZE) {
            logBuffer.pollLast()
        }

        // Publish to UI StateFlow
        _logsStateFlow.value = logBuffer.toList()

        // Background persist to database if persister attached
        logPersister?.let { persister ->
            scope.launch {
                try {
                    persister(entry)
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to persist log to database", t)
                }
            }
        }
    }

    fun clear() {
        logBuffer.clear()
        _logsStateFlow.value = emptyList()
        i(LogFeature.Diagnostics, "Log book cleared by user")
    }

    fun exportToTxt(): String {
        val sb = StringBuilder()
        sb.append("====================================================\n")
        sb.append("STUDY WATCH - DIAGNOSTIC LOG BOOK\n")
        sb.append("Generated at: ${java.util.Date()}\n")
        sb.append("Total records: ${logBuffer.size}\n")
        sb.append("====================================================\n\n")
        logBuffer.reversed().forEach { entry ->
            sb.append(entry.toFormattedString()).append("\n")
        }
        return sb.toString()
    }

    fun exportToJson(): String {
        val sb = StringBuilder()
        sb.append("[\n")
        val items = logBuffer.toList()
        items.forEachIndexed { index, entry ->
            sb.append("  {\n")
            sb.append("    \"timestamp\": ${entry.timestampMillis},\n")
            sb.append("    \"timeFormatted\": \"${entry.formattedDate()}\",\n")
            sb.append("    \"level\": \"${entry.level.name}\",\n")
            sb.append("    \"feature\": \"${entry.feature.name}\",\n")
            sb.append("    \"event\": \"${escapeJson(entry.event)}\",\n")
            sb.append("    \"timerState\": \"${escapeJson(entry.timerState ?: "")}\",\n")
            sb.append("    \"sessionUid\": \"${escapeJson(entry.sessionUid ?: "")}\",\n")
            sb.append("    \"timerId\": \"${escapeJson(entry.timerId ?: "")}\",\n")
            sb.append("    \"subject\": \"${escapeJson(entry.subjectName ?: "")}\",\n")
            sb.append("    \"details\": \"${escapeJson(entry.details ?: "")}\",\n")
            sb.append("    \"exception\": \"${escapeJson(entry.exceptionMessage ?: "")}\"\n")
            sb.append("  }")
            if (index < items.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
