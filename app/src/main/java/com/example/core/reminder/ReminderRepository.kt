package com.example.core.reminder

import android.content.Context
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Keeps the user's reminders on the phone and keeps their alarms in line with them.
 * Stored as a small JSON list (no database change needed).
 */
class ReminderRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("reminder_store", Context.MODE_PRIVATE)

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    init {
        _reminders.value = load()
    }

    // ----- Defaults used when the user does not choose N and M -----

    fun defaultMinutes(): Int =
        prefs.getInt(KEY_DEFAULT_MINUTES, ReminderPlanner.DEFAULT_REPEAT_MINUTES)

    fun defaultTimes(): Int =
        prefs.getInt(KEY_DEFAULT_TIMES, ReminderPlanner.DEFAULT_REPEAT_TIMES)

    fun setDefaults(minutes: Int, times: Int) {
        prefs.edit()
            .putInt(KEY_DEFAULT_MINUTES, minutes.coerceIn(ReminderPlanner.MIN_REPEAT_MINUTES, ReminderPlanner.MAX_REPEAT_MINUTES))
            .putInt(KEY_DEFAULT_TIMES, times.coerceIn(ReminderPlanner.MIN_REPEAT_TIMES, ReminderPlanner.MAX_REPEAT_TIMES))
            .apply()
    }

    // ----- Reading -----

    fun get(id: Int): Reminder? = _reminders.value.firstOrNull { it.id == id }

    // ----- Changing -----

    @Synchronized
    fun add(
        text: String,
        triggerAtMillis: Long,
        repeatDaily: Boolean,
        repeatEveryMinutes: Int,
        repeatTimes: Int,
        untilDone: Boolean
    ): Reminder {
        val id = prefs.getInt(KEY_NEXT_ID, 1)
        prefs.edit().putInt(KEY_NEXT_ID, id + 1).apply()
        val created = ReminderPlanner.onSaved(
            Reminder(
                id = id,
                text = text.trim(),
                triggerAtMillis = triggerAtMillis,
                repeatDaily = repeatDaily,
                repeatEveryMinutes = repeatEveryMinutes,
                repeatTimes = repeatTimes,
                untilDone = untilDone
            ),
            System.currentTimeMillis()
        )
        store(_reminders.value + created)
        ReminderScheduler.schedule(appContext, created)
        return created
    }

    /** Saves an edited reminder and starts it again from its (new) start time. */
    @Synchronized
    fun update(edited: Reminder) {
        val saved = ReminderPlanner.onSaved(edited, System.currentTimeMillis())
        replace(saved)
    }

    @Synchronized
    fun setEnabled(id: Int, enabled: Boolean) {
        val current = get(id) ?: return
        if (current.completed) return
        val updated = if (enabled) {
            ReminderPlanner.onSaved(current.copy(enabled = true), System.currentTimeMillis())
        } else {
            current.copy(enabled = false)
        }
        replace(updated)
    }

    @Synchronized
    fun delete(id: Int) {
        ReminderScheduler.cancel(appContext, id)
        store(_reminders.value.filterNot { it.id == id })
    }

    /** An alert was just shown: plan the next repeat (or finish). */
    @Synchronized
    fun onAlertShown(id: Int) {
        val current = get(id) ?: return
        replace(ReminderPlanner.onAlertShown(current, System.currentTimeMillis()))
    }

    @Synchronized
    fun markDone(id: Int) {
        val current = get(id) ?: return
        replace(ReminderPlanner.onDone(current, System.currentTimeMillis()))
    }

    @Synchronized
    fun snooze(id: Int) {
        val current = get(id) ?: return
        replace(ReminderPlanner.onSnooze(current, System.currentTimeMillis()))
    }

    /** Puts every active reminder back on the phone clock (app start, phone restart, app update). */
    @Synchronized
    fun rescheduleAll() {
        _reminders.value.forEach { ReminderScheduler.schedule(appContext, it) }
    }

    // ----- Storage -----

    private fun replace(updated: Reminder) {
        store(_reminders.value.map { if (it.id == updated.id) updated else it })
        ReminderScheduler.schedule(appContext, updated)
    }

    private fun store(list: List<Reminder>) {
        _reminders.value = list
        try {
            val array = JSONArray()
            list.forEach { array.put(toJson(it)) }
            prefs.edit().putString(KEY_LIST, array.toString()).apply()
        } catch (t: Throwable) {
            AppLogger.e(LogFeature.NotificationManager, "Failed to save reminders: ${t.message}")
        }
    }

    private fun load(): List<Reminder> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
        } catch (t: Throwable) {
            AppLogger.e(LogFeature.NotificationManager, "Failed to read reminders: ${t.message}")
            emptyList()
        }
    }

    private fun toJson(r: Reminder): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("text", r.text)
        put("triggerAt", r.triggerAtMillis)
        put("daily", r.repeatDaily)
        put("every", r.repeatEveryMinutes)
        put("times", r.repeatTimes)
        put("untilDone", r.untilDone)
        put("enabled", r.enabled)
        put("fired", r.firedCount)
        put("nextAlert", r.nextAlertAtMillis)
        put("completed", r.completed)
    }

    private fun fromJson(o: JSONObject): Reminder = Reminder(
        id = o.getInt("id"),
        text = o.getString("text"),
        triggerAtMillis = o.getLong("triggerAt"),
        repeatDaily = o.optBoolean("daily", false),
        repeatEveryMinutes = o.optInt("every", ReminderPlanner.DEFAULT_REPEAT_MINUTES),
        repeatTimes = o.optInt("times", ReminderPlanner.DEFAULT_REPEAT_TIMES),
        untilDone = o.optBoolean("untilDone", false),
        enabled = o.optBoolean("enabled", true),
        firedCount = o.optInt("fired", 0),
        nextAlertAtMillis = o.optLong("nextAlert", 0L),
        completed = o.optBoolean("completed", false)
    )

    private companion object {
        const val KEY_LIST = "reminders_json"
        const val KEY_NEXT_ID = "next_id"
        const val KEY_DEFAULT_MINUTES = "default_minutes"
        const val KEY_DEFAULT_TIMES = "default_times"
    }
}
