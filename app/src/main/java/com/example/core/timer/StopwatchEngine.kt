package com.example.core.timer

import android.os.SystemClock
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class StopwatchSnapshot(
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val laps: List<LapRecord> = emptyList(),
    val subjectId: Long = 0L,
    val subjectName: String = "",
    val topicId: Long = 0L,
    val topicName: String = ""
) {
    fun formattedDisplay(): String {
        val totalSecs = elapsedMillis / 1000
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    val stateText: String
        get() = when {
            isRunning -> "RUNNING"
            isPaused -> "PAUSED"
            else -> "IDLE"
        }
}

object StopwatchEngine {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var tickerJob: Job? = null

    private var accumulatedElapsedMillis: Long = 0L
    private var segmentStartMonotonicMillis: Long = 0L
    private val lapsList = mutableListOf<LapRecord>()

    private var currentSubjectId: Long = 0L
    private var currentSubjectName: String = ""
    private var currentTopicId: Long = 0L
    private var currentTopicName: String = ""

    private val _snapshot = MutableStateFlow(StopwatchSnapshot())
    val snapshot: StateFlow<StopwatchSnapshot> = _snapshot.asStateFlow()

    @Synchronized
    fun start(
        subjectId: Long = currentSubjectId,
        subjectName: String = currentSubjectName,
        topicId: Long = currentTopicId,
        topicName: String = currentTopicName
    ) {
        currentSubjectId = subjectId
        currentSubjectName = subjectName
        currentTopicId = topicId
        currentTopicName = topicName

        segmentStartMonotonicMillis = SystemClock.elapsedRealtime()
        startTicker()
        updateSnapshot(isRunning = true, isPaused = false)
        AppLogger.i(LogFeature.TimerEngine, "Stopwatch started")
    }

    @Synchronized
    fun pause() {
        if (!_snapshot.value.isRunning) return
        accumulatedElapsedMillis += SystemClock.elapsedRealtime() - segmentStartMonotonicMillis
        stopTicker()
        updateSnapshot(isRunning = false, isPaused = true)
        AppLogger.i(LogFeature.TimerEngine, "Stopwatch paused at ${accumulatedElapsedMillis}ms")
    }

    @Synchronized
    fun resume() {
        if (!_snapshot.value.isPaused) return
        segmentStartMonotonicMillis = SystemClock.elapsedRealtime()
        startTicker()
        updateSnapshot(isRunning = true, isPaused = false)
        AppLogger.i(LogFeature.TimerEngine, "Stopwatch resumed")
    }

    @Synchronized
    fun recordLap() {
        val currentTotal = if (_snapshot.value.isRunning) {
            accumulatedElapsedMillis + (SystemClock.elapsedRealtime() - segmentStartMonotonicMillis)
        } else {
            accumulatedElapsedMillis
        }

        val previousLapsTotal = lapsList.sumOf { it.lapDurationMillis }
        val lapDuration = (currentTotal - previousLapsTotal).coerceAtLeast(0L)
        val lapRecord = LapRecord(
            lapNumber = lapsList.size + 1,
            lapDurationMillis = lapDuration,
            totalDurationMillis = currentTotal
        )
        lapsList.add(lapRecord)
        updateSnapshot(isRunning = _snapshot.value.isRunning, isPaused = _snapshot.value.isPaused)
        AppLogger.i(LogFeature.TimerEngine, "Stopwatch lap ${lapRecord.lapNumber} recorded: ${lapDuration}ms")
    }

    @Synchronized
    fun reset() {
        stopTicker()
        accumulatedElapsedMillis = 0L
        segmentStartMonotonicMillis = 0L
        lapsList.clear()
        updateSnapshot(isRunning = false, isPaused = false)
        AppLogger.i(LogFeature.TimerEngine, "Stopwatch reset")
    }

    @Synchronized
    fun stop() {
        stopTicker()
        updateSnapshot(isRunning = false, isPaused = false)
        AppLogger.i(LogFeature.TimerEngine, "Stopwatch stopped")
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(100)
                updateSnapshot(isRunning = true, isPaused = false)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateSnapshot(isRunning: Boolean, isPaused: Boolean) {
        val currentElapsed = if (isRunning) {
            accumulatedElapsedMillis + (SystemClock.elapsedRealtime() - segmentStartMonotonicMillis)
        } else {
            accumulatedElapsedMillis
        }

        _snapshot.value = StopwatchSnapshot(
            elapsedMillis = currentElapsed,
            isRunning = isRunning,
            isPaused = isPaused,
            laps = lapsList.toList(),
            subjectId = currentSubjectId,
            subjectName = currentSubjectName,
            topicId = currentTopicId,
            topicName = currentTopicName
        )
    }
}
