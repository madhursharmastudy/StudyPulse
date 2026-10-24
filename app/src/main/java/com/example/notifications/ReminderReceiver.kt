package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.StudyWatchApplication
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import com.example.core.reminder.ReminderPlanner

/**
 * Handles three things for user reminders: an alert is due (FIRE), the user pressed
 * Done on the notification, or the user pressed Snooze.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val id = intent?.getIntExtra(EXTRA_REMINDER_ID, -1) ?: -1
        if (id < 0) return
        val repository = (context.applicationContext as? StudyWatchApplication)?.reminderRepository ?: return

        when (intent?.action) {
            ACTION_FIRE -> {
                val reminder = repository.get(id) ?: return
                if (!ReminderPlanner.isActive(reminder)) return
                // Ignore a stale alarm that is not due yet; make sure the right one is set
                if (reminder.nextAlertAtMillis > System.currentTimeMillis() + STALE_ALARM_MILLIS) {
                    repository.rescheduleAll()
                    return
                }
                AppLogger.i(LogFeature.NotificationManager, "Reminder $id alert ${reminder.firedCount + 1}")
                ReminderNotifier.show(context, reminder, reminder.firedCount + 1)
                repository.onAlertShown(id)
            }
            ACTION_DONE -> {
                ReminderNotifier.cancel(context, id)
                repository.markDone(id)
            }
            ACTION_SNOOZE -> {
                ReminderNotifier.cancel(context, id)
                repository.snooze(id)
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.example.action.REMINDER_FIRE"
        const val ACTION_DONE = "com.example.action.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.example.action.REMINDER_SNOOZE"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        private const val STALE_ALARM_MILLIS = 60_000L
    }
}
