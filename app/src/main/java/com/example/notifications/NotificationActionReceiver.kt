package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import com.example.core.timer.TimerEngine

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        AppLogger.i(LogFeature.NotificationManager, "Notification action received: $action")

        when (action) {
            ACTION_PAUSE -> TimerEngine.pause()
            ACTION_RESUME -> TimerEngine.resume()
            ACTION_STOP -> TimerEngine.stop()
            ACTION_SKIP_EYE -> TimerEngine.skipEyeRest()
            ACTION_DRANK_WATER -> TimerEngine.confirmDrankWater()
            ACTION_REMIND_WATER_LATER -> TimerEngine.remindWaterLater()
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.example.studywatch.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.studywatch.ACTION_RESUME"
        const val ACTION_STOP = "com.example.studywatch.ACTION_STOP"
        const val ACTION_SKIP_EYE = "com.example.studywatch.ACTION_SKIP_EYE"
        const val ACTION_DRANK_WATER = "com.example.studywatch.ACTION_DRANK_WATER"
        const val ACTION_REMIND_WATER_LATER = "com.example.studywatch.ACTION_REMIND_WATER_LATER"
    }
}
