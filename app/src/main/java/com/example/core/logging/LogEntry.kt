package com.example.core.logging

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestampMillis: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val feature: LogFeature,
    val event: String,
    val timerState: String? = null,
    val sessionUid: String? = null,
    val timerId: String? = null,
    val subjectName: String? = null,
    val topicName: String? = null,
    val details: String? = null,
    val exceptionMessage: String? = null,
    val stackTrace: String? = null
) {
    fun formattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestampMillis))
    }

    fun formattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestampMillis))
    }

    fun toFormattedString(): String {
        val sb = StringBuilder()
        sb.append("[${formattedTime()}] ")
        sb.append("[${level.name}] ")
        sb.append("[${feature.name}] ")
        sb.append(event)
        if (!timerState.isNullOrEmpty()) sb.append(" | State: $timerState")
        if (!sessionUid.isNullOrEmpty()) sb.append(" | Session: $sessionUid")
        if (!timerId.isNullOrEmpty()) sb.append(" | Timer: $timerId")
        if (!subjectName.isNullOrEmpty()) sb.append(" | Subject: $subjectName")
        if (!details.isNullOrEmpty()) sb.append(" | $details")
        if (!exceptionMessage.isNullOrEmpty()) sb.append("\nException: $exceptionMessage")
        if (!stackTrace.isNullOrEmpty()) sb.append("\nStack: $stackTrace")
        return sb.toString()
    }
}
