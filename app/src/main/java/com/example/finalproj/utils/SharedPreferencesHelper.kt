package com.example.finalproj.utils

import android.content.Context

object SharedPreferencesHelper {

    private const val PREF_NAME = "app_prefs"

    private const val KEY_LOGGED_IN = "is_logged_in"
    private const val KEY_THEME = "theme"
    private const val KEY_NOTIFY_DAYS = "notify_days"

    // ---------- LOGIN ----------
    fun setLoggedIn(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LOGGED_IN, false)
    }

    // ---------- THEME ----------
    fun saveTheme(context: Context, theme: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun getTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, "light") ?: "light"
    }

    // ---------- NOTIFICATION DAYS ----------
    fun saveNotifyDays(context: Context, days: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_NOTIFY_DAYS, days).apply()
    }

    fun getNotifyDays(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NOTIFY_DAYS, 7)
    }
}
