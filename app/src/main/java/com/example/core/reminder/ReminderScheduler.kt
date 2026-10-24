package com.example.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import com.example.notifications.ReminderReceiver

/** Puts reminder alerts on the phone clock so they arrive even when the app is closed. */
object ReminderScheduler {

    private const val REQUEST_BASE = 20_000

    private fun fireIntent(context: Context, id: Int): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, id)
        }

    fun schedule(context: Context, reminder: Reminder) {
        if (!ReminderPlanner.isActive(reminder)) {
            cancel(context, reminder.id)
            return
        }
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_BASE + reminder.id,
                fireIntent(context, reminder.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerAt = ReminderPlanner.alarmTime(reminder, System.currentTimeMillis())
            val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            if (canUseExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            AppLogger.i(
                LogFeature.NotificationManager,
                "Reminder ${reminder.id} scheduled (${if (canUseExact) "exact" else "inexact"}) at $triggerAt"
            )
        } catch (e: Exception) {
            AppLogger.e(LogFeature.NotificationManager, "Failed to schedule reminder ${reminder.id}: ${e.message}")
        }
    }

    fun cancel(context: Context, id: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_BASE + id,
                fireIntent(context, id),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            AppLogger.e(LogFeature.NotificationManager, "Failed to cancel reminder $id: ${e.message}")
        }
    }
}
