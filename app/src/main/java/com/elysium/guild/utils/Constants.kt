package com.elysium.guild.utils

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.elysium.guild.R
import com.elysium.guild.models.EventType
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

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
    const val KEY_VIBRATE_ONLY = "vibrate_only_alerts"
    const val KEY_BOSS_NOTIFICATION_OFFSET = "boss_notification_offset"
    const val KEY_FLOATING_BUBBLE_ENABLED = "floating_bubble_enabled"
    const val KEY_BUBBLE_LAST_X = "bubble_last_x"
    const val KEY_BUBBLE_LAST_Y = "bubble_last_y"
    const val KEY_USE_LOCAL_TIMEZONE = "use_local_timezone"
    const val KEY_IS_FIRST_RUN = "is_first_run"

    // Theme Modes
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
    
    // Notification & Alarm Configuration
    const val DEFAULT_NOTIFICATION_OFFSET_MINUTES = 10
    const val BOSS_NOTIFICATION_CHANNEL_ID_BASE = "boss_event_alerts"
    const val BOSS_NOTIFICATION_CHANNEL_NAME = "Boss & Event Alerts"
    const val BOSS_NOTIFICATION_CHANNEL_DESC = "Notifications for boss spawns and guild events."
    
    // Bubble Service Configuration
    const val BUBBLE_NOTIFICATION_CHANNEL_ID = "bubble_service_channel"
    const val BUBBLE_NOTIFICATION_CHANNEL_NAME = "Floating Bubble Service"
    const val BUBBLE_NOTIFICATION_ID = 1001
    const val BUBBLE_SNAP_ANIMATION_DURATION = 300L
    const val BUBBLE_SPAWNED_GRACE_PERIOD_SECONDS = -600L // 10 minutes after spawn
    const val BUBBLE_VISIBLE_THRESHOLD_SECONDS = 3600L // 1 hour before spawn
    
    const val BUBBLE_INITIAL_Y_DP = 200
    const val BUBBLE_EXPANDED_Y_DP = 50
    const val BUBBLE_HEADER_HEIGHT_DP = 65
    const val BUBBLE_DRAG_THRESHOLD_PX = 10
    const val BUBBLE_TEXT_SIZE_TITLE_SP = 18f
    const val BUBBLE_TEXT_SIZE_SUBTITLE_SP = 12f
    const val BUBBLE_TEXT_SIZE_HEADER_SP = 16f
    const val BUBBLE_TEXT_SIZE_ROW_SP = 14f
    const val BUBBLE_ROW_PADDING_HORIZONTAL_DP = 16
    const val BUBBLE_ROW_PADDING_VERTICAL_DP = 6
    const val BUBBLE_MAX_WIDTH_DP = 300
    const val BUBBLE_MAX_ITEMS = 50 
    
    const val BUBBLE_PORTRAIT_MAX_ITEMS = 10
    const val BUBBLE_LANDSCAPE_MAX_ITEMS = 6
    const val BUBBLE_ROW_ESTIMATED_HEIGHT_DP = 32
    const val BUBBLE_HEADER_ESTIMATED_HEIGHT_DP = 72

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
    val COLOR_READY = Color(0xFF10B981) // Green
    val COLOR_SOON = Color(0xFFF59E0B) // Amber/Gold
    val COLOR_OVERDUE = Color(0xFFEF4444) // Red
    val COLOR_TRACKING = Color(0xFF6366F1) // Indigo/Blue
    val COLOR_TRACKING_LIGHT = Color(0xFF0284C7) // Azure Blue
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

    // Donation Strings
    const val DONATION_TITLE = "Support Elysium Guild"
    const val DONATION_DESC = "Scan the GCASH QR code to support our guild's server and development costs. Any amount is greatly appreciated!"

    // Page Titles & Subtitles
    const val TITLE_BOSS_TIMERS = "Boss Timers"
    const val SUBTITLE_BOSS_TIMERS = "Track spawns & rotations"
    
    const val TITLE_GUILD_EVENTS = "Guild Events"
    const val SUBTITLE_GUILD_EVENTS = "Stay synced with guild activities"
    
    const val TITLE_LEADERBOARD = "Leaderboard"
    const val SUBTITLE_LEADERBOARD = "Track the guild's top performers"
    
    const val TITLE_SETTINGS = "Settings"
    const val SUBTITLE_SETTINGS = "Manage your preferences"

    // Resource Names
    const val RES_QR_DONATION = "qr"
    const val RES_DEFAULT_SOUND = "terran_launch"
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
    
    fun getCountdownColor(timeRemainingMs: Long?, isDark: Boolean): Color {
        if (timeRemainingMs == null) return if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray

        val minutesRemaining = timeRemainingMs / 1000 / 60

        return when {
            minutesRemaining > 30 -> if (isDark) Color.White.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.6f)
            minutesRemaining in 11..30 -> if (isDark) Color.White.copy(alpha = 0.9f) else Color.DarkGray
            minutesRemaining in 0..10 -> Constants.COLOR_SOON // Imminent Gold/Amber
            else -> Constants.COLOR_OVERDUE // Overdue Red
        }
    }

    fun getEventStatusColor(startTime: String, endTime: String?, now: Instant, isDark: Boolean, useLocalTimezone: Boolean = false): Color {
        return try {
            val start = Instant.parse(startTime)
            val end = endTime?.let { Instant.parse(it) }
            val isRunning = end?.let { now >= start && now < it } ?: (now >= start && (now - start).inWholeMinutes < 60)
            
            when {
                isRunning -> Constants.COLOR_READY
                (start - now).inWholeMinutes <= Constants.SPAWNING_SOON_THRESHOLD_MINUTES -> Constants.COLOR_SOON
                else -> if (isDark) Constants.COLOR_TRACKING else Constants.COLOR_TRACKING_LIGHT
            }
        } catch (e: Exception) {
            if (isDark) Constants.COLOR_TRACKING else Constants.COLOR_TRACKING_LIGHT
        }
    }

    fun getBubbleStatusColorRes(diffSeconds: Long): Int {
        return when {
            diffSeconds <= 0 -> R.color.boss_ready
            diffSeconds <= Constants.SPAWNING_SOON_THRESHOLD_MINUTES * 60 -> R.color.boss_soon
            else -> R.color.primary
        }
    }

    fun getEventIcon(eventType: EventType): String {
        return when (eventType) {
            EventType.WORLD_BOSS -> "🐉"
            EventType.GUILD_DUNGEON -> "🏰"
            EventType.ARENA_BATTLE -> "⚔️"
            EventType.GUILD_BOSS -> "👹"
            EventType.GVG -> "⚔️"
            EventType.SPECIAL_EVENT -> "🎯"
        }
    }

    fun formatEventTime(timeString: String, useLocalTimezone: Boolean = false): String {
        return try {
            val instant = Instant.parse(timeString)
            val targetTz = if (useLocalTimezone) TimeZone.currentSystemDefault() else TimeZone.of("Asia/Manila")
            val localDateTime = instant.toLocalDateTime(targetTz)
            val day = localDateTime.dayOfMonth
            val month = localDateTime.month.name.substring(0, 3).lowercase().replaceFirstChar { it.uppercase() }
            val year = localDateTime.year
            val hour = if (localDateTime.hour % 12 == 0) 12 else localDateTime.hour % 12
            val minute = String.format("%02d", localDateTime.minute)
            val amPm = if (localDateTime.hour < 12) "AM" else "PM"
            
            val tzLabel = if (useLocalTimezone) {
                val zoneName = targetTz.id
                if (zoneName.contains("/")) zoneName.split("/").last().replace("_", " ") else zoneName
            } else {
                "PHT"
            }

            "$month $day, $year $hour:$minute $amPm ($tzLabel)"
        } catch (e: Exception) {
            "Time TBD"
        }
    }

    fun calculateCountdown(startTime: String, now: Instant): String {
        return try {
            val eventInstant = Instant.parse(startTime)
            val duration = eventInstant - now

            if (duration.isNegative()) return ""

            duration.toComponents { days, hours, minutes, seconds, _ ->
                when {
                    days > 0 -> "${days}d ${hours}h ${minutes}m"
                    hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
                    else -> "${minutes}m ${seconds}s"
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun calculateEventCountdown(startTime: String, endTime: String?, now: Instant): String {
        return try {
            val start = Instant.parse(startTime)
            val end = endTime?.let { Instant.parse(it) }
            
            if (end != null && now >= start && now < end) {
                val duration = end - now
                duration.toComponents { days, hours, minutes, seconds, _ ->
                    val time = when {
                        days > 0 -> "${days}d ${hours}h ${minutes}m"
                        hours > 0 -> "${hours}h ${minutes}m"
                        else -> "${minutes}m ${seconds}s"
                    }
                    "Ends in: $time"
                }
            } else {
                calculateCountdown(startTime, now)
            }
        } catch (e: Exception) {
            ""
        }
    }
}
