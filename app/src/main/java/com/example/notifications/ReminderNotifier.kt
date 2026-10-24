package com.example.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import com.example.core.reminder.Reminder

/** Shows the heads-up notification for a user reminder, with Done and Snooze buttons. */
object ReminderNotifier {

    private const val NOTIFICATION_BASE = 30_000
    private const val PENDING_BASE = 40_000

    fun show(context: Context, reminder: Reminder, alertNumber: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            AppLogger.e(LogFeature.NotificationManager, "Reminder ${reminder.id} not shown: notification permission missing")
            return
        }

        val title = if (!reminder.untilDone && reminder.repeatTimes > 1) {
            "⏰ Reminder ($alertNumber/${reminder.repeatTimes})"
        } else {
            "⏰ Reminder"
        }

        val openApp = PendingIntent.getActivity(
            context,
            PENDING_BASE + reminder.id * 3,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, StudyNotificationManager.CHANNEL_ALERTS_V2_ID)
            .setSmallIcon(R.drawable.ic_notification_studypulse)
            .setContentTitle(title)
            .setContentText(reminder.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(!reminder.untilDone)
            .setOngoing(reminder.untilDone)
            .addAction(0, "Done", actionIntent(context, reminder.id, ReminderReceiver.ACTION_DONE, 1))
            .addAction(0, "Snooze ${reminder.repeatEveryMinutes} min", actionIntent(context, reminder.id, ReminderReceiver.ACTION_SNOOZE, 2))

        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_BASE + reminder.id, builder.build())
        } catch (e: Exception) {
            AppLogger.e(LogFeature.NotificationManager, "Failed to show reminder ${reminder.id}: ${e.message}")
        }
    }

    fun cancel(context: Context, id: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_BASE + id)
    }

    private fun actionIntent(context: Context, id: Int, action: String, offset: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            PENDING_BASE + id * 3 + offset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
