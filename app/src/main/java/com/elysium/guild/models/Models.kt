package com.elysium.guild.models

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.google.gson.JsonElement

// Boss Timer Models
@Immutable
data class BossTimer(
    val bossName: String,
    val bossPoints: Int,
    val type: String, // "timer" or "schedule"
    val killedBy: String? = null,
    val lastKillTime: String? = null, // ISO datetime
    val nextSpawnTime: String? = null, // ISO datetime
    val interval: Int? = null, // in hours for timer-based bosses
    val timeRemaining: Long? = null, // milliseconds until spawn
    val status: String, // "ready", "soon", "overdue"
    val killCount: Int = 0,
    val isPredicted: Boolean = false,
    val rotation: RotationInfo? = null,
    val imageUrl: String? = null
)

@Immutable
data class RotationInfo(
    val isRotating: Boolean,
    val currentIndex: Int? = null,
    val currentGuild: String? = null,
    val isOurTurn: Boolean? = null,
    val guilds: List<String>? = null,
    val nextGuild: String? = null
)

// Leaderboard Models
@Stable
interface LeaderboardEntry {
    val rank: Int
    val username: String
    val memberId: String
}

// Leaderboard Models - Attendance Type
@Immutable
data class AttendanceLeaderboardEntry(
    override val rank: Int,
    override val username: String,
    override val memberId: String,
    val totalKills: Int,
    val pointsEarned: Int,
    val attendanceRate: Int, // Integer in API
    val currentStreak: Int
) : LeaderboardEntry

// Leaderboard Models - Points Type  
@Immutable
data class PointsLeaderboardEntry(
    override val rank: Int,
    override val username: String,
    override val memberId: String,
    val pointsAvailable: Int,
    val pointsEarned: Int,
    val pointsSpent: Int,
    val consumptionRate: Int // Integer in API
) : LeaderboardEntry

enum class LeaderboardType {
    ATTENDANCE,
    POINTS
}

// Event Models
@Immutable
data class GuildEvent(
    val id: String,
    val name: String,
    val type: EventType,
    val startTime: String, // ISO datetime
    val endTime: String?, // ISO datetime
    val description: String,
    val reminderSet: Boolean
)

enum class EventType {
    WORLD_BOSS,
    GUILD_DUNGEON,
    ARENA_BATTLE,
    GUILD_BOSS,
    GVG,
    SPECIAL_EVENT
}

// Member Profile Models
@Immutable
data class MemberProfile(
    val memberId: String,
    val name: String,
    val avatarUrl: String?,
    val rank: String,
    val totalPoints: Int,
    val availablePoints: Int,
    val attendanceRate: Double,
    val currentStreak: Int,
    val weeklyRank: Int,
    val role: String,
    val recentActivities: List<ActivityEntry>
)

@Immutable
data class ActivityEntry(
    val id: String,
    val type: ActivityType,
    val description: String,
    val timestamp: String, // ISO datetime
    val points: Int?
)

enum class ActivityType {
    BOSS_KILL,
    ATTENDANCE_VERIFIED,
    AUCTION_WON,
    POINTS_EARNED,
    POINTS_SPENT
}

// API Response Models
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val error: String?
)

data class BossTimerResponse(
    val success: Boolean,
    val count: Int,
    val bosses: List<BossTimer>,
    val timestamp: String
)

data class LeaderboardResponse(
    val success: Boolean,
    val type: String, // "attendance" or "points"
    val period: String,
    val count: Int,
    val total: Int,
    val data: JsonElement, // Different structure for attendance vs points
    val timestamp: String
)

data class EventsResponse(
    val events: List<GuildEvent>
)

data class MemberProfileResponse(
    val profile: MemberProfile
)

// Database Models
@Entity(tableName = "boss_timers")
data class BossTimerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val alias: String, // JSON string
    val points: Int,
    val nextSpawn: String?,
    val lastKilled: String?,
    val status: String,
    val interval: Int?,
    val imageUrl: String?
)

@Entity(tableName = "leaderboard")
data class LeaderboardEntryEntity(
    @PrimaryKey val memberId: String,
    val memberName: String,
    val avatarUrl: String?,
    val totalPoints: Int,
    val availablePoints: Int,
    val attendanceRate: Double,
    val currentStreak: Int,
    val weeklyRank: Int,
    val role: String,
    val type: String, // attendance or points
    val lastUpdated: String
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val startTime: String,
    val endTime: String?,
    val description: String,
    val reminderSet: Boolean
)

@Entity(tableName = "member_profile")
data class MemberProfileEntity(
    @PrimaryKey val memberId: String,
    val name: String,
    val avatarUrl: String?,
    val rank: String,
    val totalPoints: Int,
    val availablePoints: Int,
    val attendanceRate: Double,
    val currentStreak: Int,
    val weeklyRank: Int,
    val role: String,
    val lastUpdated: String
)
