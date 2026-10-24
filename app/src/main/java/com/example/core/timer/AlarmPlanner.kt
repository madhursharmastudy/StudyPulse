package com.example.core.timer

/** The four kinds of background alarms the app can have scheduled at once. */
enum class AlarmSlot { EYE, WATER, SESSION_END, BREAK_END }

data class PlannedAlarm(
    val slot: AlarmSlot,
    val triggerAtMillis: Long,
    val breakType: BreakType? = null
)

/**
 * Decides which phone-clock alarms should exist for the current timer state.
 * Pure logic (no Android classes) so it can be unit tested.
 */
object AlarmPlanner {

    /** Alarms fire slightly after the due time so the timer always sees the event as due. */
    const val SAFETY_MARGIN_MILLIS = 1500L

    fun plan(
        snapshot: TimerSnapshot,
        nowWallMillis: Long,
        pauseBreaksUntilMillis: Long,
        notificationsEnabled: Boolean,
        quietMode: Boolean
    ): List<PlannedAlarm> {
        if (quietMode || !notificationsEnabled) return emptyList()

        return when (snapshot.state) {
            TimerState.STUDYING -> planStudying(snapshot, nowWallMillis, pauseBreaksUntilMillis)
            TimerState.WATER_BREAK -> planBreakEnd(snapshot, nowWallMillis, BreakType.WATER_BREAK)
            TimerState.SHORT_BREAK -> planBreakEnd(snapshot, nowWallMillis, BreakType.SHORT_BREAK)
            TimerState.LONG_BREAK -> planBreakEnd(snapshot, nowWallMillis, BreakType.LONG_BREAK)
            else -> emptyList()
        }
    }

    private fun planStudying(
        snapshot: TimerSnapshot,
        now: Long,
        pauseBreaksUntilMillis: Long
    ): List<PlannedAlarm> {
        val alarms = mutableListOf<PlannedAlarm>()

        val hasEnd = snapshot.timerType != TimerType.STOPWATCH &&
            snapshot.plannedDurationMillis > 0 &&
            snapshot.currentSegmentRemainingMillis > 0
        val sessionEnd: Long = if (hasEnd) {
            now + snapshot.currentSegmentRemainingMillis + SAFETY_MARGIN_MILLIS
        } else {
            Long.MAX_VALUE
        }
        if (hasEnd) {
            alarms.add(PlannedAlarm(AlarmSlot.SESSION_END, sessionEnd))
        }

        fun beforeEnd(time: Long): Boolean = time < sessionEnd

        if (snapshot.eyeCareEnabled) {
            val eyeAt = now + snapshot.nextEyeRestCountdownSeconds * 1000L + SAFETY_MARGIN_MILLIS
            if (beforeEnd(eyeAt)) {
                alarms.add(PlannedAlarm(AlarmSlot.EYE, eyeAt, BreakType.EYE_REST))
            }
        }

        if (snapshot.waterReminderEnabled || snapshot.stretchBreakEnabled) {
            var waterAt = now + snapshot.nextWaterCountdownSeconds * 1000L + SAFETY_MARGIN_MILLIS
            if (pauseBreaksUntilMillis + SAFETY_MARGIN_MILLIS > waterAt) {
                waterAt = pauseBreaksUntilMillis + SAFETY_MARGIN_MILLIS
            }
            if (beforeEnd(waterAt)) {
                alarms.add(PlannedAlarm(AlarmSlot.WATER, waterAt, BreakType.WATER_BREAK))
            }
        }

        return alarms
    }

    private fun planBreakEnd(
        snapshot: TimerSnapshot,
        now: Long,
        breakType: BreakType
    ): List<PlannedAlarm> {
        if (snapshot.currentSegmentRemainingMillis <= 0) return emptyList()
        val endAt = now + snapshot.currentSegmentRemainingMillis + SAFETY_MARGIN_MILLIS
        return listOf(PlannedAlarm(AlarmSlot.BREAK_END, endAt, breakType))
    }
}
