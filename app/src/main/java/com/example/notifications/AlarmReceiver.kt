package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import com.example.core.timer.BreakType
import com.example.core.timer.TimerEngine

/**
 * AlarmReceiver: Receives exact alarm triggers from AlarmManager.
 * Delivers timely notifications and alert tones even when app is in background or screen is off.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        // If Quiet Mode is active or master Notifications is disabled, suppress all alerts
        if (TimerEngine.isQuietMode || !TimerEngine.notificationsEnabled) {
            AppLogger.i(LogFeature.NotificationManager, "Alarm received but silenced due to Quiet Mode or Notifications disabled")
            return
        }

        val notificationManager = StudyNotificationManager(context.applicationContext)
        val alertType = intent.getStringExtra(EXTRA_ALERT_TYPE) ?: return

        when (alertType) {
            TYPE_SESSION_START -> {
                notificationManager.playAlertToneAndVibrate()
            }
            TYPE_SESSION_END -> {
                notificationManager.showSessionCompletedNotification(TimerEngine.snapshot.value)
            }
            TYPE_BREAK_START -> {
                val breakTypeName = intent.getStringExtra(EXTRA_BREAK_NAME) ?: BreakType.EYE_REST.name
                val breakType = try { BreakType.valueOf(breakTypeName) } catch (_: Exception) { BreakType.EYE_REST }
                notificationManager.showBreakStartAlert(breakType)
            }
            TYPE_BREAK_END -> {
                val breakTypeName = intent.getStringExtra(EXTRA_BREAK_NAME) ?: BreakType.EYE_REST.name
                val breakType = try { BreakType.valueOf(breakTypeName) } catch (_: Exception) { BreakType.EYE_REST }
                notificationManager.showBreakEndAlert(breakType)
            }
        }
    }

    companion object {
        const val ACTION_ALARM_ALERT = "com.example.action.ALARM_ALERT"
        const val EXTRA_ALERT_TYPE = "extra_alert_type"
        const val EXTRA_BREAK_NAME = "extra_break_name"

        const val TYPE_SESSION_START = "TYPE_SESSION_START"
        const val TYPE_SESSION_END = "TYPE_SESSION_END"
        const val TYPE_BREAK_START = "TYPE_BREAK_START"
        const val TYPE_BREAK_END = "TYPE_BREAK_END"
    }
}
