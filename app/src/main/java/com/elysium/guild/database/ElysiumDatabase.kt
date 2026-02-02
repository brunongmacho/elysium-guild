package com.elysium.guild.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elysium.guild.models.*

@Database(
    entities = [
        BossTimerEntity::class,
        LeaderboardEntryEntity::class,
        EventEntity::class,
        MemberProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ElysiumDatabase : RoomDatabase() {
    
    abstract fun bossTimerDao(): BossTimerDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun eventsDao(): EventsDao
    abstract fun memberProfileDao(): MemberProfileDao
}

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns if needed in future versions
    }
}