package com.elysium.guild.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getInt(Constants.KEY_THEME_MODE, Constants.THEME_SYSTEM))
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(prefs.getBoolean("haptic_feedback_enabled", true))
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    var bossNotificationsEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_BOSS_SPAWN_ALERTS, true)
        set(value) = prefs.edit().putBoolean(Constants.KEY_BOSS_SPAWN_ALERTS, value).apply()

    var eventNotificationsEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_EVENT_REMINDERS, true)
        set(value) = prefs.edit().putBoolean(Constants.KEY_EVENT_REMINDERS, value).apply()

    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean("haptic_feedback_enabled", true)
        set(value) {
            prefs.edit().putBoolean("haptic_feedback_enabled", value).apply()
            _hapticEnabled.value = value
        }

    var bossNotificationOffset: Int
        get() = prefs.getInt(Constants.KEY_BOSS_NOTIFICATION_OFFSET, 10)
        set(value) = prefs.edit().putInt(Constants.KEY_BOSS_NOTIFICATION_OFFSET, value).apply()

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(Constants.KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }
}
