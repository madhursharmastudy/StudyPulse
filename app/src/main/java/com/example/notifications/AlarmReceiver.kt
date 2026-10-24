package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.StudyWatchApplication
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import com.example.core.timer.TimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AlarmReceiver: wakes the app when a phone-clock alarm fires (eye break, water break,
 * break end or session end) while the app is in the background or the screen is off.
 *
 * The app class has already rebuilt the running timer before this runs. Here the timer
 * catches up, and it posts the same notifications it would have posted if the app
 * had been open. A stale alarm (timer already paused or stopped) does nothing.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_ALARM_ALERT) return

        val slot = intent.getStringExtra(EXTRA_SLOT) ?: "UNKNOWN"
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                AppLogger.i(LogFeature.NotificationManager, "Alarm received: $slot")
                TimerEngine.catchUp()

                // If the alarm fired a little early, plan the next alarm from the current state
                val app = context.applicationContext as? StudyWatchApplication
                app?.notificationManager?.rescheduleAlarms(TimerEngine.snapshot.value)

                // Keep the process alive briefly so the notification can be posted
                delay(2500)
            } catch (t: Throwable) {
                AppLogger.e(LogFeature.NotificationManager, "Alarm handling failed: ${t.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ALARM_ALERT = "com.example.action.ALARM_ALERT"
        const val EXTRA_SLOT = "extra_alarm_slot"
    }
}
