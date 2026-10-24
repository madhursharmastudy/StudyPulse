package com.example.core.reminder

import java.util.Calendar

/**
 * Pure rules for when a reminder alerts and repeats. No Android classes here,
 * so every rule can be unit tested.
 *
 * Alerts per occurrence: the first alert at the chosen time, then one more every
 * N minutes. Without "until Done" the total number of alerts is M (the first one counts).
 * With "until Done" it keeps alerting every N minutes until the user presses Done.
 */
object ReminderPlanner {

    const val DEFAULT_REPEAT_MINUTES = 5
    const val DEFAULT_REPEAT_TIMES = 3
    const val MIN_REPEAT_MINUTES = 1
    const val MAX_REPEAT_MINUTES = 240
    const val MIN_REPEAT_TIMES = 1
    const val MAX_REPEAT_TIMES = 50

    private const val MINUTE_MILLIS = 60_000L
    private const val MIN_DELAY_MILLIS = 2_000L

    fun isActive(r: Reminder): Boolean = r.enabled && !r.completed && r.nextAlertAtMillis > 0L

    /** The moment the phone alarm should really be set (never in the past). */
    fun alarmTime(r: Reminder, nowMillis: Long): Long =
        maxOf(r.nextAlertAtMillis, nowMillis + MIN_DELAY_MILLIS)

    /** First day-occurrence after [nowMillis], keeping the same clock time every day. */
    fun nextOccurrenceAfter(triggerAtMillis: Long, nowMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = triggerAtMillis
        var guard = 0
        while (cal.timeInMillis <= nowMillis && guard < 4000) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            guard++
        }
        return cal.timeInMillis
    }

    /** Use when a reminder is created or edited. */
    fun onSaved(r: Reminder, nowMillis: Long): Reminder {
        var trigger = r.triggerAtMillis
        if (r.repeatDaily && trigger <= nowMillis) {
            trigger = nextOccurrenceAfter(trigger, nowMillis)
        }
        return r.copy(
            triggerAtMillis = trigger,
            nextAlertAtMillis = trigger,
            firedCount = 0,
            completed = false
        )
    }

    /** Use right after an alert was shown to the user. */
    fun onAlertShown(r: Reminder, nowMillis: Long): Reminder {
        val shown = r.firedCount + 1
        val alertAgain = r.untilDone || shown < r.repeatTimes
        return if (alertAgain) {
            r.copy(
                firedCount = shown,
                nextAlertAtMillis = nowMillis + r.repeatEveryMinutes * MINUTE_MILLIS
            )
        } else {
            finishOccurrence(r.copy(firedCount = shown), nowMillis)
        }
    }

    /** The user pressed Done. */
    fun onDone(r: Reminder, nowMillis: Long): Reminder = finishOccurrence(r, nowMillis)

    /** The user pressed Snooze: alert again after N minutes, without counting it. */
    fun onSnooze(r: Reminder, nowMillis: Long): Reminder =
        r.copy(nextAlertAtMillis = nowMillis + r.repeatEveryMinutes * MINUTE_MILLIS)

    private fun finishOccurrence(r: Reminder, nowMillis: Long): Reminder {
        return if (r.repeatDaily) {
            val next = nextOccurrenceAfter(r.triggerAtMillis, nowMillis)
            r.copy(triggerAtMillis = next, nextAlertAtMillis = next, firedCount = 0)
        } else {
            r.copy(completed = true, nextAlertAtMillis = 0L, firedCount = 0)
        }
    }
}
