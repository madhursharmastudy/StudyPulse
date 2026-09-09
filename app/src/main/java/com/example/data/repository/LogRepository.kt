package com.example.data.repository

import com.example.core.logging.AppLogger
import com.example.core.logging.LogEntry
import com.example.core.logging.LogFeature
import com.example.core.logging.LogLevel
import com.example.data.db.AppLogEntity
import com.example.data.db.LogDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LogRepository(private val logDao: LogDao) {

    init {
        // Wire AppLogger to persist logs in Room
        AppLogger.setLogPersister { entry ->
            // Asynchronously insert
            kotlinx.coroutines.runBlocking {
                try {
                    logDao.insertLog(
                        AppLogEntity(
                            timestamp = entry.timestampMillis,
                            level = entry.level.name,
                            feature = entry.feature.name,
                            event = entry.event,
                            timerState = entry.timerState,
                            sessionUid = entry.sessionUid,
                            subjectName = entry.subjectName,
                            topicName = entry.topicName,
                            details = entry.details,
                            exceptionDetails = entry.exceptionMessage ?: entry.stackTrace
                        )
                    )
                } catch (_: Throwable) {}
            }
        }
    }

    val liveLogs: Flow<List<LogEntry>> = AppLogger.logsStateFlow

    val databaseLogs: Flow<List<LogEntry>> = logDao.getRecentLogs().map { entities ->
        entities.map { it.toLogEntry() }
    }

    suspend fun clearLogs() {
        AppLogger.clear()
        logDao.clearLogs()
    }

    fun exportToTxt(): String = AppLogger.exportToTxt()
    fun exportToJson(): String = AppLogger.exportToJson()

    private fun AppLogEntity.toLogEntry(): LogEntry {
        return LogEntry(
            id = this.id,
            timestampMillis = this.timestamp,
            level = runCatching { LogLevel.valueOf(this.level) }.getOrDefault(LogLevel.INFO),
            feature = runCatching { LogFeature.valueOf(this.feature) }.getOrDefault(LogFeature.Diagnostics),
            event = this.event,
            timerState = this.timerState,
            sessionUid = this.sessionUid,
            subjectName = this.subjectName,
            topicName = this.topicName,
            details = this.details,
            exceptionMessage = this.exceptionDetails
        )
    }
}
