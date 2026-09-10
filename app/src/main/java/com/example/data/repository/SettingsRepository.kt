package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import com.example.core.timer.TimerEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "study_watch_settings")

data class AppSettings(
    val use24HourFormat: Boolean = false,
    val showSeconds: Boolean = false,
    val clockStyle: String = "Digital", // Digital, Flip, Minimal, Large
    val appTheme: String = "AMOLED Black", // AMOLED Black, Dark Charcoal, Light
    val defaultStudyDurationMinutes: Int = 50,
    val selectedSubjectId: Long = 1L,
    val selectedTopicId: Long = 1L,
    val selectedSubjectName: String = "English Literature",
    val selectedTopicName: String = "Romantic Poetry",
    val focusModeEnabled: Boolean = false,
    val eyeCareEnabled: Boolean = true,
    val eyeStudyIntervalMinutes: Int = 20,
    val eyeRestDurationSeconds: Int = 20,
    val eyeAutoResume: Boolean = true,
    val waterReminderEnabled: Boolean = true,
    val waterIntervalMinutes: Int = 45,
    val waterBreakDurationSeconds: Int = 300,
    val waterAutoResume: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val dailyTargetHours: Int = 6,
    val keepScreenOn: Boolean = true,
    val debugModeActive: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val APP_THEME = stringPreferencesKey("app_theme")
        val DEFAULT_STUDY_DURATION = intPreferencesKey("default_study_duration")
        val SELECTED_SUBJECT_ID = androidx.datastore.preferences.core.longPreferencesKey("selected_subject_id")
        val SELECTED_TOPIC_ID = androidx.datastore.preferences.core.longPreferencesKey("selected_topic_id")
        val SELECTED_SUBJECT_NAME = stringPreferencesKey("selected_subject_name")
        val SELECTED_TOPIC_NAME = stringPreferencesKey("selected_topic_name")
        val FOCUS_MODE = booleanPreferencesKey("focus_mode")
        val EYE_CARE_ENABLED = booleanPreferencesKey("eye_care_enabled")
        val EYE_STUDY_INTERVAL_MINUTES = intPreferencesKey("eye_study_interval_minutes")
        val EYE_REST_DURATION_SECONDS = intPreferencesKey("eye_rest_duration_seconds")
        val EYE_AUTO_RESUME = booleanPreferencesKey("eye_auto_resume")
        val WATER_REMINDER_ENABLED = booleanPreferencesKey("water_reminder_enabled")
        val WATER_INTERVAL_MINUTES = intPreferencesKey("water_interval_minutes")
        val WATER_BREAK_DURATION_SECONDS = intPreferencesKey("water_break_duration_seconds")
        val WATER_AUTO_RESUME = booleanPreferencesKey("water_auto_resume")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val DAILY_TARGET_HOURS = intPreferencesKey("daily_target_hours")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val debug = prefs[Keys.DEBUG_MODE] ?: false
        val eyeEnabled = prefs[Keys.EYE_CARE_ENABLED] ?: true
        val eyeInterval = prefs[Keys.EYE_STUDY_INTERVAL_MINUTES] ?: 20
        val eyeRest = prefs[Keys.EYE_REST_DURATION_SECONDS] ?: 20
        val eyeAuto = prefs[Keys.EYE_AUTO_RESUME] ?: true

        val waterEnabled = prefs[Keys.WATER_REMINDER_ENABLED] ?: true
        val waterInterval = prefs[Keys.WATER_INTERVAL_MINUTES] ?: 45
        val waterBreak = prefs[Keys.WATER_BREAK_DURATION_SECONDS] ?: 300
        val waterAuto = prefs[Keys.WATER_AUTO_RESUME] ?: true

        // Synchronize with TimerEngine
        TimerEngine.debugModeActive = debug
        if (!debug) {
            TimerEngine.eyeCareEnabled = eyeEnabled
            TimerEngine.eyeStudyIntervalSeconds = eyeInterval * 60L
            TimerEngine.eyeRestDurationSeconds = eyeRest.toLong()
            TimerEngine.eyeAutoResumeEnabled = eyeAuto

            TimerEngine.waterReminderEnabled = waterEnabled
            TimerEngine.waterIntervalSeconds = waterInterval * 60L
            TimerEngine.waterBreakDurationSeconds = waterBreak.toLong()
            TimerEngine.waterAutoResumeEnabled = waterAuto
        }

        AppSettings(
            use24HourFormat = prefs[Keys.USE_24_HOUR] ?: false,
            showSeconds = prefs[Keys.SHOW_SECONDS] ?: false,
            clockStyle = prefs[Keys.CLOCK_STYLE] ?: "Digital",
            appTheme = prefs[Keys.APP_THEME] ?: "AMOLED Black",
            defaultStudyDurationMinutes = prefs[Keys.DEFAULT_STUDY_DURATION] ?: 50,
            selectedSubjectId = prefs[Keys.SELECTED_SUBJECT_ID] ?: 1L,
            selectedTopicId = prefs[Keys.SELECTED_TOPIC_ID] ?: 1L,
            selectedSubjectName = prefs[Keys.SELECTED_SUBJECT_NAME] ?: "English Literature",
            selectedTopicName = prefs[Keys.SELECTED_TOPIC_NAME] ?: "Romantic Poetry",
            focusModeEnabled = prefs[Keys.FOCUS_MODE] ?: false,
            eyeCareEnabled = eyeEnabled,
            eyeStudyIntervalMinutes = eyeInterval,
            eyeRestDurationSeconds = eyeRest,
            eyeAutoResume = eyeAuto,
            waterReminderEnabled = waterEnabled,
            waterIntervalMinutes = waterInterval,
            waterBreakDurationSeconds = waterBreak,
            waterAutoResume = waterAuto,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            dailyTargetHours = prefs[Keys.DAILY_TARGET_HOURS] ?: 6,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            debugModeActive = debug
        )
    }

    suspend fun update24Hour(value: Boolean) = edit { it[Keys.USE_24_HOUR] = value }
    suspend fun updateShowSeconds(value: Boolean) = edit { it[Keys.SHOW_SECONDS] = value }
    suspend fun updateClockStyle(value: String) = edit { it[Keys.CLOCK_STYLE] = value }
    suspend fun updateAppTheme(value: String) = edit { it[Keys.APP_THEME] = value }
    suspend fun updateDefaultStudyDuration(minutes: Int) = edit { it[Keys.DEFAULT_STUDY_DURATION] = minutes }
    suspend fun updateSelectedSubject(id: Long, name: String) = edit {
        it[Keys.SELECTED_SUBJECT_ID] = id
        it[Keys.SELECTED_SUBJECT_NAME] = name
    }
    suspend fun updateSelectedTopic(id: Long, name: String) = edit {
        it[Keys.SELECTED_TOPIC_ID] = id
        it[Keys.SELECTED_TOPIC_NAME] = name
    }
    suspend fun updateFocusMode(enabled: Boolean) = edit { it[Keys.FOCUS_MODE] = enabled }

    suspend fun updateEyeCareEnabled(value: Boolean) = edit { it[Keys.EYE_CARE_ENABLED] = value }
    suspend fun updateEyeStudyInterval(minutes: Int) = edit { it[Keys.EYE_STUDY_INTERVAL_MINUTES] = minutes }
    suspend fun updateEyeRestDuration(seconds: Int) = edit { it[Keys.EYE_REST_DURATION_SECONDS] = seconds }
    suspend fun updateEyeAutoResume(value: Boolean) = edit { it[Keys.EYE_AUTO_RESUME] = value }

    suspend fun updateWaterReminderEnabled(value: Boolean) = edit { it[Keys.WATER_REMINDER_ENABLED] = value }
    suspend fun updateWaterInterval(minutes: Int) = edit { it[Keys.WATER_INTERVAL_MINUTES] = minutes }
    suspend fun updateWaterBreakDuration(seconds: Int) = edit { it[Keys.WATER_BREAK_DURATION_SECONDS] = seconds }
    suspend fun updateWaterAutoResume(value: Boolean) = edit { it[Keys.WATER_AUTO_RESUME] = value }

    suspend fun updateSoundEnabled(value: Boolean) = edit { it[Keys.SOUND_ENABLED] = value }
    suspend fun updateVibrationEnabled(value: Boolean) = edit { it[Keys.VIBRATION_ENABLED] = value }
    suspend fun updateDailyTargetHours(hours: Int) = edit { it[Keys.DAILY_TARGET_HOURS] = hours }
    suspend fun updateKeepScreenOn(value: Boolean) = edit { it[Keys.KEEP_SCREEN_ON] = value }

    suspend fun toggleDebugMode(active: Boolean) {
        edit { it[Keys.DEBUG_MODE] = active }
        TimerEngine.debugModeActive = active
        AppLogger.i(LogFeature.Settings, "Debug test mode updated: $active")
    }

    private suspend fun edit(action: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(action)
    }
}
