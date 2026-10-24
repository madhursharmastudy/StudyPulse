package com.example

import com.example.core.timer.AlarmPlanner
import com.example.core.timer.AlarmSlot
import com.example.core.timer.BreakType
import com.example.core.timer.TimerSnapshot
import com.example.core.timer.TimerState
import com.example.core.timer.TimerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPlannerTest {

    private val now = 1_000_000L
    private val margin = AlarmPlanner.SAFETY_MARGIN_MILLIS

    private fun studying(
        remainingMinutes: Long = 100,
        eyeMinutes: Long = 10,
        waterMinutes: Long = 40,
        type: TimerType = TimerType.COUNTDOWN
    ) = TimerSnapshot(
        state = TimerState.STUDYING,
        timerType = type,
        plannedDurationMillis = 100 * 60_000L,
        currentSegmentRemainingMillis = remainingMinutes * 60_000L,
        nextEyeRestCountdownSeconds = eyeMinutes * 60L,
        nextWaterCountdownSeconds = waterMinutes * 60L,
        eyeCareEnabled = true,
        waterReminderEnabled = true,
        stretchBreakEnabled = true
    )

    private fun plan(
        snapshot: TimerSnapshot,
        pauseUntil: Long = 0L,
        notifications: Boolean = true,
        quiet: Boolean = false
    ) = AlarmPlanner.plan(snapshot, now, pauseUntil, notifications, quiet)

    @Test
    fun studying_plansEyeWaterAndSessionEnd() {
        val alarms = plan(studying()).associateBy { it.slot }
        assertEquals(now + 10 * 60_000L + margin, alarms[AlarmSlot.EYE]?.triggerAtMillis)
        assertEquals(now + 40 * 60_000L + margin, alarms[AlarmSlot.WATER]?.triggerAtMillis)
        assertEquals(now + 100 * 60_000L + margin, alarms[AlarmSlot.SESSION_END]?.triggerAtMillis)
        assertEquals(BreakType.EYE_REST, alarms[AlarmSlot.EYE]?.breakType)
        assertEquals(BreakType.WATER_BREAK, alarms[AlarmSlot.WATER]?.breakType)
    }

    @Test
    fun studying_dropsAlarmsThatWouldFireAfterSessionEnds() {
        val alarms = plan(studying(remainingMinutes = 30, waterMinutes = 40)).associateBy { it.slot }
        assertNotNull(alarms[AlarmSlot.EYE])
        assertNotNull(alarms[AlarmSlot.SESSION_END])
        assertNull(alarms[AlarmSlot.WATER])
    }

    @Test
    fun stopwatch_hasNoSessionEndAlarm() {
        val alarms = plan(studying(type = TimerType.STOPWATCH).copy(plannedDurationMillis = 0L)).associateBy { it.slot }
        assertNull(alarms[AlarmSlot.SESSION_END])
        assertNotNull(alarms[AlarmSlot.EYE])
    }

    @Test
    fun eyeCareDisabled_hasNoEyeAlarm() {
        val alarms = plan(studying().copy(eyeCareEnabled = false)).associateBy { it.slot }
        assertNull(alarms[AlarmSlot.EYE])
        assertNotNull(alarms[AlarmSlot.WATER])
    }

    @Test
    fun officePause_pushesWaterAlarmLater() {
        val pauseUntil = now + 60 * 60_000L
        val alarms = plan(studying(), pauseUntil = pauseUntil).associateBy { it.slot }
        assertEquals(pauseUntil + margin, alarms[AlarmSlot.WATER]?.triggerAtMillis)
    }

    @Test
    fun paused_hasNoAlarms() {
        assertTrue(plan(studying().copy(state = TimerState.PAUSED)).isEmpty())
    }

    @Test
    fun quietMode_hasNoAlarms() {
        assertTrue(plan(studying(), quiet = true).isEmpty())
    }

    @Test
    fun notificationsOff_hasNoAlarms() {
        assertTrue(plan(studying(), notifications = false).isEmpty())
    }

    @Test
    fun waterBreak_plansBreakEndAlarm() {
        val snapshot = TimerSnapshot(
            state = TimerState.WATER_BREAK,
            currentSegmentRemainingMillis = 5 * 60_000L
        )
        val alarms = plan(snapshot)
        assertEquals(1, alarms.size)
        assertEquals(AlarmSlot.BREAK_END, alarms[0].slot)
        assertEquals(BreakType.WATER_BREAK, alarms[0].breakType)
        assertEquals(now + 5 * 60_000L + margin, alarms[0].triggerAtMillis)
    }

    @Test
    fun eyeRest_hasNoAlarm() {
        val snapshot = TimerSnapshot(
            state = TimerState.EYE_REST,
            currentSegmentRemainingMillis = 20_000L
        )
        assertTrue(plan(snapshot).isEmpty())
    }
}
