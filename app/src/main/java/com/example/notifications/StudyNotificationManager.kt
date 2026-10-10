package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import com.example.core.timer.BreakType
import com.example.core.timer.TimerEngine
import com.example.core.timer.TimerSnapshot
import com.example.core.timer.TimerState

/**
 * StudyNotificationManager (Part 4C)
 * Manages heads-up notifications, system alert tones, haptic feedback,
 * and AlarmManager exact background alarms for study sessions and breaks.
 */
class StudyNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

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

            // NEW channel ID for alerts so user sound/vibration overrides are applied freshly
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_V2_ID,
                "Study & Break Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority heads-up alerts and tones for study breaks and session completions"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(alertsChannel)
            AppLogger.i(LogFeature.NotificationManager, "Notification channels initialized (Alerts V2)")
        }
    }

    /**
     * Plays short system alert tone and triggers haptic vibration based on current settings.
     */
    fun playAlertToneAndVibrate() {
        if (TimerEngine.isQuietMode || !TimerEngine.notificationsEnabled) return

        // 1. System tone
        if (TimerEngine.soundEnabled) {
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
            } catch (_: Exception) {
                // Fallback to ToneGenerator if RingtoneManager is unavailable
                try {
                    val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                } catch (_: Exception) {}
            }
        }

        // 2. Haptic vibration
        if (TimerEngine.vibrationEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    @Suppress("DEPRECATION")
                    v?.vibrate(250)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Notification & tone at START of any break (Eye, Water, Stretch, Short, Long, Lunch).
     */
    fun showBreakStartAlert(breakType: BreakType) {
        if (TimerEngine.isQuietMode || !TimerEngine.notificationsEnabled) return
        playAlertToneAndVibrate()

        if (!hasNotificationPermission()) return

        val (title, body, iconRes) = when (breakType) {
            BreakType.EYE_REST -> Triple(
                "👁 Eye Rest (20-20-20 Rule)",
                "20 minutes study reached! Look 20 feet away for 20 seconds.",
                R.drawable.ic_notification_eye
            )
            BreakType.WATER_BREAK -> Triple(
                "💧 Hydration & Stretch Break",
                "Time for water and gentle ergonomic posture stretches.",
                R.drawable.ic_notification_water
            )
            BreakType.SHORT_BREAK -> Triple(
                "☕ Short Break",
                "Take 5 minutes to stretch, breathe, and refresh.",
                R.drawable.ic_notification_studypulse
            )
            BreakType.LONG_BREAK -> Triple(
                "🌿 Long Rest Break",
                "Great work! Step away from your desk for a well-deserved rest.",
                R.drawable.ic_notification_studypulse
            )
            BreakType.CUSTOM_BREAK -> Triple(
                "🍱 Planned Lunch / Rest Break",
                "Take your planned rest. Timer will await your return.",
                R.drawable.ic_notification_studypulse
            )
        }

        val openAppIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context, 10, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_V2_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (!TimerEngine.soundEnabled) {
            builder.setSilent(true)
        }

        notificationManager.notify(NOTIFICATION_BREAK_ID, builder.build())
        AppLogger.i(LogFeature.NotificationManager, "Break start alert posted: ${breakType.displayName}")
    }

    /**
     * Notification & tone at END of any break. Automatically dismisses break start notification.
     */
    fun showBreakEndAlert(breakType: BreakType) {
        // Break notification disappears automatically when that break ends
        cancelBreakAlerts()

        if (TimerEngine.isQuietMode || !TimerEngine.notificationsEnabled) return
        playAlertToneAndVibrate()

        if (!hasNotificationPermission()) return

        val title = when (breakType) {
            BreakType.EYE_REST -> "👁 Eye Rest Finished"
            BreakType.WATER_BREAK -> "💪 Break Complete"
            else -> "✨ Break Ended"
        }
        val body = "Ready to resume your study block! Let's get focused."

        val openAppIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context, 11, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_V2_ID)
            .setSmallIcon(R.drawable.ic_notification_studypulse)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(7000L) // Auto-cleans after 7 seconds
            .setContentIntent(contentPendingIntent)

        if (!TimerEngine.soundEnabled) {
            builder.setSilent(true)
        }

        notificationManager.notify(NOTIFICATION_BREAK_END_ID, builder.build())
        AppLogger.i(LogFeature.NotificationManager, "Break end alert posted: ${breakType.displayName}")
    }

    /**
     * Notification & tone when study session starts.
     */
    fun showSessionStartedTone() {
        if (TimerEngine.isQuietMode || !TimerEngine.notificationsEnabled) return
        playAlertToneAndVibrate()
    }

    /**
     * Notification & tone when study session finishes.
     */
    fun showSessionCompletedNotification(snapshot: TimerSnapshot) {
        if (TimerEngine.isQuietMode || !TimerEngine.notificationsEnabled) return
        playAlertToneAndVibrate()

        if (!hasNotificationPermission()) return

        val openAppIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context, 12, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_V2_ID)
            .setSmallIcon(R.drawable.ic_notification_studypulse)
            .setContentTitle("🎉 Study Session Completed!")
            .setContentText("Finished planned session on ${snapshot.subjectName} (${snapshot.formattedProductiveStudy()})")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (!TimerEngine.soundEnabled) {
            builder.setSilent(true)
        }

        notificationManager.notify(NOTIFICATION_COMPLETED_ID, builder.build())
        AppLogger.i(LogFeature.NotificationManager, "Session completed notification posted")
    }

    /**
     * Schedules an exact alarm so alerts reach user when screen is off or another app is open.
     */
    fun scheduleExactAlarm(triggerAtMillis: Long, alertType: String, breakType: BreakType? = null) {
        if (TimerEngine.isQuietMode || !TimerEngine.notificationsEnabled) return

        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_ALERT
                putExtra(AlarmReceiver.EXTRA_ALERT_TYPE, alertType)
                breakType?.let { putExtra(AlarmReceiver.EXTRA_BREAK_NAME, it.name) }
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alertType.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            AppLogger.i(LogFeature.NotificationManager, "Scheduled exact alarm: $alertType at $triggerAtMillis")
        } catch (e: Exception) {
            AppLogger.e(LogFeature.NotificationManager, "Failed to schedule exact alarm: ${e.message}")
        }
    }

    /**
     * Cancels any active break notification.
     */
    fun cancelBreakAlerts() {
        notificationManager.cancel(NOTIFICATION_BREAK_ID)
        notificationManager.cancel(NOTIFICATION_EYE_ID)
        notificationManager.cancel(NOTIFICATION_WATER_ID)
    }

    /**
     * Cancels all alerts and notifications (used on Quiet Mode or Reset).
     */
    fun cancelAllAlerts() {
        notificationManager.cancelAll()
        AppLogger.i(LogFeature.NotificationManager, "All notifications and alert popups cancelled")
    }

    fun showEyeRestNotification(snapshot: TimerSnapshot) {
        showBreakStartAlert(BreakType.EYE_REST)
    }

    fun showWaterReminderNotification(snapshot: TimerSnapshot) {
        showBreakStartAlert(BreakType.WATER_BREAK)
    }

    fun updateRunningTimerNotification(snapshot: TimerSnapshot) {
        // Part 4C requirement: No always-on notification while a session just runs.
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
        const val CHANNEL_ALERTS_V2_ID = "study_alerts_channel_v2"
        const val NOTIFICATION_TIMER_ID = 1001
        const val NOTIFICATION_EYE_ID = 1002
        const val NOTIFICATION_WATER_ID = 1003
        const val NOTIFICATION_COMPLETED_ID = 1004
        const val NOTIFICATION_BREAK_ID = 1005
        const val NOTIFICATION_BREAK_END_ID = 1006
    }
}
