package com.elysium.guild.utils

object Constants {
    
    // API Configuration
    const val BASE_URL = "https://initial-michelina-1elysium-87b4172a.koyeb.app/"
    const val API_TIMEOUT = 30L // seconds
    
    // Database Configuration
    const val DATABASE_NAME = "elysium_guild_database"
    const val DATABASE_VERSION = 1
    
    // Notification Channels
    const val BOSS_SPAWN_CHANNEL_ID = "boss_spawn_channel"
    const val EVENT_REMINDER_CHANNEL_ID = "event_reminder_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Elysium Guild Notifications"
    
    // Shared Preferences
    const val PREFS_NAME = "elysium_prefs"
    const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val KEY_BOSS_SPAWN_ALERTS = "boss_spawn_alerts"
    const val KEY_EVENT_REMINDERS = "event_reminders"
    const val KEY_NOTIFICATION_SOUND = "notification_sound"
    const val KEY_NOTIFICATION_VIBRATION = "notification_vibration"
    const val KEY_SYNC_INTERVAL = "sync_interval"
    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_BOSS_NOTIFICATION_OFFSET = "boss_notification_offset"
    
    // Theme Modes
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
    
    // Sync Intervals (in minutes)
    val SYNC_INTERVALS = listOf(15, 30, 60, 120)
    const val DEFAULT_SYNC_INTERVAL = 30
    
    // Time Constants
    const val SPAWNING_SOON_THRESHOLD_MINUTES = 30
    const val OVERDUE_THRESHOLD_MINUTES = -60
    
    // Boss Status Colors
    const val BOSS_READY_COLOR = "#10B981"
    const val BOSS_SPAWNING_SOON_COLOR = "#F59E0B"
    const val BOSS_OVERDUE_COLOR = "#EF4444"
    const val BOSS_NORMAL_COLOR = "#6B7280"
    
    // UI Constants
    const val AVATAR_PLACEHOLDER_URL = "https://via.placeholder.com/48"
    const val BOSS_IMAGE_PLACEHOLDER_URL = "https://via.placeholder.com/60"
}
