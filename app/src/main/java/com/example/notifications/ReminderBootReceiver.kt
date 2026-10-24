package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Runs after the phone restarts or the app is updated. Starting the app process is enough:
 * the application class puts reminders and a running timer back on the phone clock.
 */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Nothing else to do here; StudyWatchApplication.onCreate restores everything.
    }
}
