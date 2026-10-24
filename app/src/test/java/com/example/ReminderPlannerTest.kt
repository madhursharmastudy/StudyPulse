package com.example

import com.example.core.reminder.Reminder
import com.example.core.reminder.ReminderPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlannerTest {

    private val now = 1_700_000_000_000L
    private val minute = 60_000L
    private val day = 24 * 60 * minute

    private fun reminder(
        daily: Boolean = false,
        every: Int = 5,
        times: Int = 3,
        untilDone: Boolean = false,
        trigger: Long = now + 10 * minute
    ) = Reminder(
        id = 1,
        text = "Take medicine",
        triggerAtMillis = trigger,
        repeatDaily = daily,
        repeatEveryMinutes = every,
        repeatTimes = times,
        untilDone = untilDone,
        nextAlertAtMillis = trigger
    )

    @Test
    fun repeatsEveryNMinutesUntilMAlertsThenCompletes() {
        var r = reminder(every = 5, times = 3)
        r = ReminderPlanner.onAlertShown(r, now)
        assertEquals(1, r.firedCount)
        assertEquals(now + 5 * minute, r.nextAlertAtMillis)
        assertFalse(r.completed)

        r = ReminderPlanner.onAlertShown(r, now + 5 * minute)
        assertEquals(2, r.firedCount)
        assertEquals(now + 10 * minute, r.nextAlertAtMillis)

        r = ReminderPlanner.onAlertShown(r, now + 10 * minute)
        assertTrue(r.completed)
        assertEquals(0L, r.nextAlertAtMillis)
        assertFalse(ReminderPlanner.isActive(r))
    }

    @Test
    fun timesOneMeansASingleAlert() {
        val r = ReminderPlanner.onAlertShown(reminder(times = 1), now)
        assertTrue(r.completed)
    }

    @Test
    fun untilDoneKeepsRepeatingPastM() {
        var r = reminder(every = 2, times = 3, untilDone = true)
        repeat(10) { i ->
            r = ReminderPlanner.onAlertShown(r, now + i * 2 * minute)
            assertFalse(r.completed)
            assertTrue(r.nextAlertAtMillis > 0L)
        }
        assertEquals(10, r.firedCount)
    }

    @Test
    fun doneStopsAOneTimeReminder() {
        val r = ReminderPlanner.onDone(reminder(untilDone = true), now)
        assertTrue(r.completed)
        assertFalse(ReminderPlanner.isActive(r))
    }

    @Test
    fun doneMovesADailyReminderToTomorrow() {
        val trigger = now + 10 * minute
        var r = reminder(daily = true, trigger = trigger)
        r = ReminderPlanner.onAlertShown(r, trigger)
        r = ReminderPlanner.onDone(r, trigger + minute)
        assertFalse(r.completed)
        assertEquals(0, r.firedCount)
        assertTrue(r.nextAlertAtMillis > trigger + minute)
        assertTrue(r.nextAlertAtMillis - trigger in (day - 2 * 60 * minute)..(day + 2 * 60 * minute))
        assertEquals(r.triggerAtMillis, r.nextAlertAtMillis)
    }

    @Test
    fun dailyReminderRollsToNextDayAfterLastAlert() {
        val trigger = now + 10 * minute
        var r = reminder(daily = true, every = 5, times = 2, trigger = trigger)
        r = ReminderPlanner.onAlertShown(r, trigger)
        r = ReminderPlanner.onAlertShown(r, trigger + 5 * minute)
        assertFalse(r.completed)
        assertEquals(0, r.firedCount)
        assertTrue(r.nextAlertAtMillis > trigger + 5 * minute)
    }

    @Test
    fun snoozeAlertsAgainAfterNMinutesWithoutCounting() {
        var r = reminder(every = 7, times = 3)
        r = ReminderPlanner.onAlertShown(r, now)
        val before = r.firedCount
        r = ReminderPlanner.onSnooze(r, now + minute)
        assertEquals(before, r.firedCount)
        assertEquals(now + minute + 7 * minute, r.nextAlertAtMillis)
    }

    @Test
    fun savingADailyReminderWithAPastTimeStartsTomorrow() {
        val past = now - 3 * 60 * minute
        val r = ReminderPlanner.onSaved(reminder(daily = true, trigger = past), now)
        assertTrue(r.triggerAtMillis > now)
        assertEquals(r.triggerAtMillis, r.nextAlertAtMillis)
    }

    @Test
    fun savingOneTimeReminderKeepsItsTimeAndResetsProgress() {
        val trigger = now + 3 * 60 * minute
        val r = ReminderPlanner.onSaved(reminder(trigger = trigger).copy(firedCount = 2, completed = true), now)
        assertEquals(trigger, r.nextAlertAtMillis)
        assertEquals(0, r.firedCount)
        assertFalse(r.completed)
    }

    @Test
    fun alarmTimeIsNeverInThePast() {
        val missed = reminder(trigger = now - 30 * minute)
        assertTrue(ReminderPlanner.alarmTime(missed, now) > now)
        val later = reminder(trigger = now + 30 * minute)
        assertEquals(now + 30 * minute, ReminderPlanner.alarmTime(later, now))
    }

    @Test
    fun disabledOrCompletedReminderIsNotActive() {
        assertFalse(ReminderPlanner.isActive(reminder().copy(enabled = false)))
        assertFalse(ReminderPlanner.isActive(reminder().copy(completed = true)))
        assertTrue(ReminderPlanner.isActive(reminder()))
    }
}
