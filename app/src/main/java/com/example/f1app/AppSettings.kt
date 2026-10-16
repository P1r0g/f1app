package com.example.f1app

import android.content.Context

object AppSettings {

    private const val PREFS_NAME = "f1_settings"

    private const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_NOTIFICATIONS = "notifications"

    private fun preferences(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun isDarkTheme(context: Context): Boolean {
        return preferences(context)
            .getBoolean(KEY_DARK_THEME, true)
    }

    fun setDarkTheme(
        context: Context,
        enabled: Boolean
    ) {
        preferences(context)
            .edit()
            .putBoolean(KEY_DARK_THEME, enabled)
            .apply()
    }

    fun getFontScale(context: Context): Float {
        return preferences(context)
            .getFloat(KEY_FONT_SCALE, 1.0f)
    }

    fun setFontScale(
        context: Context,
        scale: Float
    ) {
        preferences(context)
            .edit()
            .putFloat(KEY_FONT_SCALE, scale)
            .apply()
    }

    fun getLanguage(context: Context): String {
        return preferences(context)
            .getString(KEY_LANGUAGE, "Русский")
            ?: "Русский"
    }

    fun setLanguage(
        context: Context,
        language: String
    ) {
        preferences(context)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return preferences(context)
            .getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun setNotificationsEnabled(
        context: Context,
        enabled: Boolean
    ) {
        preferences(context)
            .edit()
            .putBoolean(KEY_NOTIFICATIONS, enabled)
            .apply()
    }

    fun getUserLogin(context: Context): String =
        preferences(context).getString("user_login", "").orEmpty()

    fun getUserName(context: Context): String =
        preferences(context).getString("user_name", "").orEmpty()

    fun saveProfile(context: Context, login: String, name: String) {
        preferences(context).edit()
            .putString("user_login", login.trim())
            .putString("user_name", name.trim())
            .apply()
    }
}
