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

    private val _notificationSound = MutableStateFlow(prefs.getString(Constants.KEY_NOTIFICATION_SOUND, "terran_launch") ?: "terran_launch")
    val notificationSound: StateFlow<String> = _notificationSound.asStateFlow()

    private val _bossNotificationsEnabled = MutableStateFlow(prefs.getBoolean(Constants.KEY_BOSS_SPAWN_ALERTS, true))
    val bossNotificationsEnabled: StateFlow<Boolean> = _bossNotificationsEnabled.asStateFlow()

    private val _eventNotificationsEnabled = MutableStateFlow(prefs.getBoolean(Constants.KEY_EVENT_REMINDERS, true))
    val eventNotificationsEnabled: StateFlow<Boolean> = _eventNotificationsEnabled.asStateFlow()

    private val _floatingBubbleEnabled = MutableStateFlow(prefs.getBoolean(Constants.KEY_FLOATING_BUBBLE_ENABLED, false))
    val floatingBubbleEnabled: StateFlow<Boolean> = _floatingBubbleEnabled.asStateFlow()

    var bossNotificationOffset: Int
        get() = prefs.getInt(Constants.KEY_BOSS_NOTIFICATION_OFFSET, 10)
        set(value) = prefs.edit().putInt(Constants.KEY_BOSS_NOTIFICATION_OFFSET, value).apply()

    fun setBossNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.KEY_BOSS_SPAWN_ALERTS, enabled).apply()
        _bossNotificationsEnabled.value = enabled
    }

    fun setEventNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.KEY_EVENT_REMINDERS, enabled).apply()
        _eventNotificationsEnabled.value = enabled
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback_enabled", enabled).apply()
        _hapticEnabled.value = enabled
    }

    fun setNotificationSound(sound: String) {
        prefs.edit().putString(Constants.KEY_NOTIFICATION_SOUND, sound).apply()
        _notificationSound.value = sound
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(Constants.KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setFloatingBubbleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.KEY_FLOATING_BUBBLE_ENABLED, enabled).apply()
        _floatingBubbleEnabled.value = enabled
    }
}
