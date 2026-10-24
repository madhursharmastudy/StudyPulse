package com.example.core.timer

import android.content.Context

/**
 * Keeps a small copy of the running timer on the phone, so the timer can be
 * rebuilt if Android closes the app in the background.
 */
class TimerStateStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("timer_state_store", Context.MODE_PRIVATE)

    fun save(json: String) {
        prefs.edit().putString(KEY_STATE, json).apply()
    }

    fun load(): String? = prefs.getString(KEY_STATE, null)

    fun clear() {
        prefs.edit().remove(KEY_STATE).apply()
    }

    private companion object {
        const val KEY_STATE = "state_json"
    }
}
