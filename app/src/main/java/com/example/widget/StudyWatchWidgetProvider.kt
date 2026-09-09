package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.core.timer.TimerEngine
import com.example.core.timer.TimerSnapshot
import com.example.core.timer.TimerState

class StudyWatchWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val snapshot = TimerEngine.snapshot.value
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, snapshot)
        }
    }

    companion object {
        fun updateWidget(context: Context, snapshot: TimerSnapshot) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, StudyWatchWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId, snapshot)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            snapshot: TimerSnapshot
        ) {
            val views = RemoteViews(context.packageName, R.layout.study_watch_widget_layout)

            val openAppIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            views.setTextViewText(R.id.widget_status, snapshot.state.name)

            val display = if (snapshot.state == TimerState.IDLE) {
                "00:00"
            } else {
                snapshot.formattedActiveDisplay()
            }
            views.setTextViewText(R.id.widget_timer_display, display)

            val subjectTopicText = if (snapshot.state == TimerState.IDLE) {
                "Tap to start study session"
            } else {
                "${snapshot.subjectName} • ${snapshot.topicName}"
            }
            views.setTextViewText(R.id.widget_subject_topic, subjectTopicText)

            views.setTextViewText(
                R.id.widget_eye_countdown,
                "👁 Eye: ${snapshot.formattedEyeCountdown()}"
            )
            views.setTextViewText(
                R.id.widget_water_countdown,
                "💧 Water: ${snapshot.formattedWaterCountdown()}"
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
