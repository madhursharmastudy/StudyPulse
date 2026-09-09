package com.example.notifications

import android.app.NotificationChannel
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
import com.example.core.timer.TimerSnapshot
import com.example.core.timer.TimerState

class StudyNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER_ID,
                "Study Timer Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status of active study sessions"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Study Health & Care Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up alerts for 20-20-20 Eye Care and Hydration reminders"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(alertsChannel)
            AppLogger.i(LogFeature.NotificationManager, "Notification channels created")
        }
    }

    fun updateRunningTimerNotification(snapshot: TimerSnapshot) {
        if (!hasNotificationPermission()) return

        if (snapshot.state == TimerState.IDLE || snapshot.state == TimerState.STOPPED || snapshot.state == TimerState.COMPLETED) {
            notificationManager.cancel(NOTIFICATION_TIMER_ID)
            return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "${snapshot.subjectName} • ${snapshot.state.name}"
        val content = "${snapshot.topicName} | Time: ${snapshot.formattedActiveDisplay()}"

        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER_ID)
            .setSmallIcon(R.drawable.ic_notification_studypulse)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentPendingIntent)
            .setOngoing(snapshot.state == TimerState.STUDYING)
            .setOnlyAlertOnce(true)

        // Action buttons
        if (snapshot.state == TimerState.STUDYING) {
            val pauseIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_PAUSE
            }
            val pausePending = PendingIntent.getBroadcast(
                context, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "PAUSE", pausePending)
        } else if (snapshot.state == TimerState.PAUSED) {
            val resumeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_RESUME
            }
            val resumePending = PendingIntent.getBroadcast(
                context, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "RESUME", resumePending)
        }

        val stopIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_STOP
        }
        val stopPending = PendingIntent.getBroadcast(
            context, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPending)

        notificationManager.notify(NOTIFICATION_TIMER_ID, builder.build())
    }

    fun showEyeRestNotification(snapshot: TimerSnapshot) {
        if (!hasNotificationPermission()) return

        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SKIP_EYE
        }
        val skipPending = PendingIntent.getBroadcast(
            context, 4, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context, 5, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_notification_eye)
            .setContentTitle("👁 Eye Rest (20-20-20 Rule)")
            .setContentText("20 minutes reached! Look 20 feet away for 20 seconds.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "SKIP", skipPending)

        notificationManager.notify(NOTIFICATION_ALERT_ID, builder.build())
        AppLogger.i(LogFeature.NotificationManager, "Eye rest notification posted")
    }

    fun showWaterReminderNotification(snapshot: TimerSnapshot) {
        if (!hasNotificationPermission()) return

        val drankIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DRANK_WATER
        }
        val drankPending = PendingIntent.getBroadcast(
            context, 6, drankIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val laterIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_REMIND_WATER_LATER
        }
        val laterPending = PendingIntent.getBroadcast(
            context, 7, laterIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context, 8, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_notification_water)
            .setContentTitle("💧 Hydration Break")
            .setContentText("Time to drink water! Stay refreshed and focused.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "DRANK WATER", drankPending)
            .addAction(android.R.drawable.ic_menu_recent_history, "REMIND LATER", laterPending)

        notificationManager.notify(NOTIFICATION_ALERT_ID, builder.build())
        AppLogger.i(LogFeature.NotificationManager, "Water reminder notification posted")
    }

    fun showSessionCompletedNotification(snapshot: TimerSnapshot) {
        if (!hasNotificationPermission()) return

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_notification_studypulse)
            .setContentTitle("🎉 Study Session Completed!")
            .setContentText("Productive time: ${snapshot.formattedProductiveStudy()} on ${snapshot.subjectName}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ALERT_ID, builder.build())
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        const val CHANNEL_TIMER_ID = "study_timer_channel"
        const val CHANNEL_ALERTS_ID = "study_alerts_channel"
        const val NOTIFICATION_TIMER_ID = 1001
        const val NOTIFICATION_ALERT_ID = 1002
    }
}
