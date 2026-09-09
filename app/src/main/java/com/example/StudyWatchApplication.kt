package com.example

import android.app.Application
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import com.example.core.timer.TimerEngine
import com.example.data.db.AppDatabase
import com.example.data.repository.LogRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.StudyWatchRepository
import com.example.notifications.StudyNotificationManager
import com.example.widget.StudyWatchWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyWatchApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: StudyWatchRepository
        private set
    lateinit var logRepository: LogRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var notificationManager: StudyNotificationManager
        private set

    private val appScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this

        AppLogger.i(LogFeature.Diagnostics, "StudyPulse application starting...")

        database = AppDatabase.getDatabase(this)
        repository = StudyWatchRepository(database.studyWatchDao())
        logRepository = LogRepository(database.logDao())
        settingsRepository = SettingsRepository(this)
        notificationManager = StudyNotificationManager(this)

        // Bind TimerEngine events to database and notifications
        setupTimerEngineCallbacks()

        AppLogger.i(LogFeature.Diagnostics, "StudyPulse initialized successfully")
    }

    private fun setupTimerEngineCallbacks() {
        TimerEngine.onSessionCompleted = { snapshot ->
            appScope.launch {
                try {
                    repository.recordCompletedSession(snapshot)
                    notificationManager.showSessionCompletedNotification(snapshot)
                    StudyWatchWidgetProvider.updateWidget(this@StudyWatchApplication, snapshot)
                } catch (t: Throwable) {
                    AppLogger.e(LogFeature.Database, "Failed to record completed session", null, null, null, null, t)
                }
            }
        }

        TimerEngine.onEyeRestTriggered = { snapshot ->
            appScope.launch {
                try {
                    repository.recordEyeRestEvent(
                        sessionUid = snapshot.sessionUid,
                        durationSecs = snapshot.eyeRestDurationSeconds,
                        completed = true
                    )
                    notificationManager.showEyeRestNotification(snapshot)
                    StudyWatchWidgetProvider.updateWidget(this@StudyWatchApplication, snapshot)
                } catch (t: Throwable) {
                    AppLogger.e(LogFeature.EyeCare, "Failed to handle eye rest callback", null, null, null, null, t)
                }
            }
        }

        TimerEngine.onWaterBreakTriggered = { snapshot ->
            appScope.launch {
                try {
                    notificationManager.showWaterReminderNotification(snapshot)
                    StudyWatchWidgetProvider.updateWidget(this@StudyWatchApplication, snapshot)
                } catch (t: Throwable) {
                    AppLogger.e(LogFeature.Hydration, "Failed to handle water break callback", null, null, null, null, t)
                }
            }
        }

        TimerEngine.onStateChangedCallback = { snapshot ->
            appScope.launch {
                try {
                    notificationManager.updateRunningTimerNotification(snapshot)
                    StudyWatchWidgetProvider.updateWidget(this@StudyWatchApplication, snapshot)
                } catch (t: Throwable) {
                    AppLogger.e(LogFeature.TimerEngine, "Failed on state changed callback", null, null, null, null, t)
                }
            }
        }
    }

    companion object {
        lateinit var instance: StudyWatchApplication
            private set
    }
}
