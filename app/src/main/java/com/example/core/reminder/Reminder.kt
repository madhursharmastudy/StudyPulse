package com.example.core.reminder

/**
 * A reminder written by the user.
 *
 * triggerAtMillis = when the current occurrence starts (first alert).
 * nextAlertAtMillis = when the next alert is due (0 when nothing is waiting).
 * firedCount = how many alerts were already shown for the current occurrence.
 */
data class Reminder(
    val id: Int,
    val text: String,
    val triggerAtMillis: Long,
    val repeatDaily: Boolean,
    val repeatEveryMinutes: Int,
    val repeatTimes: Int,
    val untilDone: Boolean,
    val enabled: Boolean = true,
    val firedCount: Int = 0,
    val nextAlertAtMillis: Long = triggerAtMillis,
    val completed: Boolean = false
)
