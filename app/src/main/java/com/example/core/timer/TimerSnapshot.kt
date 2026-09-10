package com.example.core.timer

data class TimerSnapshot(
    val timerId: String = "TIMER-0",
    val sessionUid: String = "",
    val state: TimerState = TimerState.IDLE,
    val timerType: TimerType = TimerType.STUDY_SESSION,
    val subjectId: Long = 0,
    val topicId: Long = 0,
    val subjectName: String = "English Literature",
    val topicName: String = "Romantic Poetry",
    val plannedDurationMillis: Long = 50 * 60 * 1000L,
    val currentTimerDurationMillis: Long = 50 * 60 * 1000L,
    val elapsedProductiveMillis: Long = 0L,
    val elapsedEyeRestMillis: Long = 0L,
    val elapsedWaterBreakMillis: Long = 0L,
    val elapsedOtherBreakMillis: Long = 0L,
    val currentSegmentRemainingMillis: Long = 50 * 60 * 1000L,
    val currentSegmentElapsedMillis: Long = 0L,
    val nextEyeRestCountdownSeconds: Long = 20 * 60L,
    val eyeRestDurationSeconds: Long = 20L,
    val nextWaterCountdownSeconds: Long = 45 * 60L,
    val currentPomodoroCycle: Int = 1,
    val totalPomodoroCycles: Int = 4,
    val interruptionCount: Int = 0,
    val laps: List<LapRecord> = emptyList(),
    val notes: String = "",
    val startTimestampMillis: Long = 0L,
    val debugModeActive: Boolean = false
) {
    val totalBreakMillis: Long
        get() = elapsedEyeRestMillis + elapsedWaterBreakMillis + elapsedOtherBreakMillis

    val totalSessionElapsedMillis: Long
        get() = elapsedProductiveMillis + totalBreakMillis

    val progressFraction: Float
        get() = if (plannedDurationMillis > 0) {
            (elapsedProductiveMillis.toFloat() / plannedDurationMillis.toFloat()).coerceIn(0f, 1f)
        } else 0f

    fun formattedActiveDisplay(): String {
        return when (timerType) {
            TimerType.STOPWATCH -> formatElapsed(currentSegmentElapsedMillis)
            else -> {
                if (state == TimerState.EYE_REST) {
                    formatSecondsRemaining(currentSegmentRemainingMillis / 1000)
                } else if (state == TimerState.WATER_BREAK) {
                    formatSecondsRemaining(currentSegmentRemainingMillis / 1000)
                } else if (state == TimerState.SHORT_BREAK || state == TimerState.LONG_BREAK) {
                    formatSecondsRemaining(currentSegmentRemainingMillis / 1000)
                } else {
                    formatSecondsRemaining(currentSegmentRemainingMillis / 1000)
                }
            }
        }
    }

    fun formattedProductiveStudy(): String {
        val totalSecs = elapsedProductiveMillis / 1000
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        return if (h > 0) {
            String.format("%dh %02dm %02ds", h, m, s)
        } else {
            String.format("%dm %02ds", m, s)
        }
    }

    fun formattedEyeCountdown(): String {
        val m = nextEyeRestCountdownSeconds / 60
        val s = nextEyeRestCountdownSeconds % 60
        return String.format("%02d:%02d", m, s)
    }

    fun formattedWaterCountdown(): String {
        val m = nextWaterCountdownSeconds / 60
        val s = nextWaterCountdownSeconds % 60
        return String.format("%02d:%02d", m, s)
    }

    fun formatPlanned(): String {
        val totalSecs = (plannedDurationMillis / 1000).coerceAtLeast(0)
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    companion object {
        fun formatSecondsRemaining(totalSecs: Long): String {
            val secs = totalSecs.coerceAtLeast(0)
            val h = secs / 3600
            val m = (secs % 3600) / 60
            val s = secs % 60
            return String.format("%02d:%02d:%02d", h, m, s)
        }

        fun formatElapsed(millis: Long): String {
            val totalSecs = millis / 1000
            val h = totalSecs / 3600
            val m = (totalSecs % 3600) / 60
            val s = totalSecs % 60
            return String.format("%02d:%02d:%02d", h, m, s)
        }
    }
}
