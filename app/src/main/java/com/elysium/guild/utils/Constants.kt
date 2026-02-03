package com.elysium.guild.utils

import androidx.compose.ui.graphics.Color

object Constants {
    
    // API Configuration
    const val BASE_URL = "https://initial-michelina-1elysium-87b4172a.koyeb.app/"
    const val API_TIMEOUT = 30L
    
    // Database Configuration
    const val DATABASE_NAME = "elysium_guild_database"
    
    // Shared Preferences Keys
    const val PREFS_NAME = "elysium_prefs"
    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_NOTIFICATION_SOUND = "notification_sound"
    const val KEY_BOSS_SPAWN_ALERTS = "boss_spawn_alerts"
    const val KEY_EVENT_REMINDERS = "event_reminders"
    const val KEY_HAPTIC_ENABLED = "haptic_feedback_enabled"
    const val KEY_BOSS_NOTIFICATION_OFFSET = "boss_notification_offset"
    
    // Theme Modes
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
    
    // Notification & Alarm Configuration
    const val DEFAULT_NOTIFICATION_OFFSET_MINUTES = 10
    const val BOSS_NOTIFICATION_CHANNEL_ID_BASE = "boss_event_alerts"
    const val BOSS_NOTIFICATION_CHANNEL_NAME = "Boss & Event Alerts"
    const val BOSS_NOTIFICATION_CHANNEL_DESC = "Notifications for boss spawns and guild events."
    
    // Intent Extras
    const val EXTRA_BOSS_NAME = "extra_boss_name"
    const val EXTRA_MINUTES_REMAINING = "extra_minutes_remaining"
    const val EXTRA_IS_EVENT = "extra_is_event"
    
    // Work Manager
    const val WORK_NAME_PERIODIC = "BossNotificationWorkPeriodic"
    const val WORK_NAME_IMMEDIATE = "BossNotificationWorkImmediate"
    
    // Status Thresholds
    const val SPAWNING_SOON_THRESHOLD_MINUTES = 30
    const val SPAWNING_SOON_THRESHOLD_MS = SPAWNING_SOON_THRESHOLD_MINUTES * 60 * 1000L
    
    // UI Colors - Status
    val COLOR_READY = Color(0xFF10B981)
    val COLOR_SOON = Color(0xFFF59E0B)
    val COLOR_OVERDUE = Color(0xFFEF4444)
    val COLOR_TRACKING = Color(0xFF6366F1)
    val COLOR_TRACKING_LIGHT = Color(0xFF0284C7) // Vibrant Azure Blue
    val COLOR_SUCCESS = Color(0xFF4CAF50)
    
    // UI Colors - Podium
    val COLOR_GOLD = Color(0xFFF59E0B)
    val COLOR_SILVER = Color(0xFF94A3B8)
    val COLOR_BRONZE = Color(0xFFB45309)
    
    // UI Colors - Backgrounds
    val COLOR_BACKGROUND_DEEP = Color(0xFF0B0B1A)
    val COLOR_BLOB_PURPLE = Color(0xFF4A148C)
    val COLOR_BLOB_BLUE = Color(0xFF0D47A1)
    val COLOR_BLOB_ORANGE = Color(0xFFFFA000)
    
    // Labels
    const val LABEL_READY = "READY"
    const val LABEL_SOON = "SOON"
    const val LABEL_TRACKING = "TRACKING"
    
    // Status Strings
    const val STATUS_READY = "ready"
    const val STATUS_SOON = "soon"
    const val STATUS_TRACKING = "tracking"
    const val STATUS_OVERDUE = "overdue"
}

object UIUtils {
    fun getStatusColor(status: String?, timeRemainingMs: Long?, isDark: Boolean): Color {
        val isReady = status == Constants.STATUS_READY || status == Constants.STATUS_OVERDUE || (timeRemainingMs != null && timeRemainingMs <= 0)
        val isSoon = !isReady && (status == Constants.STATUS_SOON || (timeRemainingMs != null && timeRemainingMs <= Constants.SPAWNING_SOON_THRESHOLD_MS))
        
        return when {
            isReady -> Constants.COLOR_READY
            isSoon -> Constants.COLOR_SOON
            else -> if (isDark) Constants.COLOR_TRACKING else Constants.COLOR_TRACKING_LIGHT
        }
    }
}
