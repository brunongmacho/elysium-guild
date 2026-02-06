package com.elysium.guild.repository

import android.util.Log
import com.elysium.guild.database.EventsDao
import com.elysium.guild.models.*
import com.elysium.guild.network.ElysiumApiService
import kotlinx.coroutines.flow.first
import kotlinx.datetime.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsRepository @Inject constructor(
    private val apiService: ElysiumApiService,
    private val eventsDao: EventsDao
) {
    
    suspend fun getEvents(): List<GuildEvent> {
        return try {
            val response = apiService.getEvents()
            val apiEvents = if (response.isSuccessful && response.body()?.success == true) {
                val events = response.body()?.data?.events ?: emptyList()
                saveEventsToLocal(events)
                events
            } else {
                Log.w("EventsRepository", "API response unsuccessful, trying cache")
                getCachedEvents()
            }

            // If API and cache return no events, provide the guild's standard schedule
            if (apiEvents.isEmpty()) {
                getMockEvents()
            } else {
                apiEvents
            }
        } catch (e: Exception) {
            Log.e("EventsRepository", "Error fetching events, using cache/mock", e)
            val cached = getCachedEvents()
            if (cached.isEmpty()) getMockEvents() else cached
        }
    }

    private suspend fun getCachedEvents(): List<GuildEvent> {
        return try {
            val entities = eventsDao.getAllEvents().first()
            entities.map { it.toDomainModel() }
        } catch (e: Exception) {
            Log.e("EventsRepository", "Failed to fetch events from cache", e)
            emptyList()
        }
    }

    private suspend fun saveEventsToLocal(events: List<GuildEvent>) {
        try {
            val entities = events.map { it.toEntity() }
            eventsDao.clearAll()
            eventsDao.insertAll(entities)
        } catch (e: Exception) {
            Log.e("EventsRepository", "Failed to save events to cache", e)
        }
    }

    private fun getMockEvents(): List<GuildEvent> {
        val now = Clock.System.now()
        val tz = TimeZone.of("Asia/Manila")
        val today = now.toLocalDateTime(tz).date

        return listOf(
            createGuildEvent(
                "1", "World Boss Event (Morning)", EventType.WORLD_BOSS,
                today, 11, 0, 30, "Daily World Boss - 11:00 AM (30m duration)"
            ),
            createGuildEvent(
                "2", "World Boss Event (Evening)", EventType.WORLD_BOSS,
                today, 20, 0, 30, "Daily World Boss - 8:00 PM (30m duration)"
            ),
            createGuildEvent(
                "3", "Individual Arena", EventType.ARENA_BATTLE,
                getNextDayOfWeek(today, listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
                20, 30, 60, "Mon, Wed, Fri Arena Battle - 8:30 PM (60m duration)"
            ),
            createGuildEvent(
                "4", "Guild Boss", EventType.GUILD_BOSS,
                getNextDayOfWeek(today, listOf(DayOfWeek.MONDAY)),
                21, 0, 5, "Monday Guild Boss - 9:00 PM (5m duration)"
            ),
            createGuildEvent(
                "5", "Coop Round Arena", EventType.ARENA_BATTLE,
                getNextDayOfWeek(today, listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)),
                20, 30, 60, "Tue, Thu, Sat Coop Arena - 8:30 PM (60m duration)"
            ),
            createGuildEvent(
                "6", "Guild War Queue Reminder", EventType.SPECIAL_EVENT,
                getNextDayOfWeek(today, listOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)),
                23, 0, 120, "Thu, Fri, Sat GvG Queue - 11:00 PM (120m duration)"
            ),
            createGuildEvent(
                "7", "GvG / Guild War", EventType.GVG,
                getNextDayOfWeek(today, listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
                20, 25, 3, "Fri, Sat, Sun Guild War - 8:25 PM (3m duration)"
            )
        ).sortedBy { it.startTime }
    }

    private fun createGuildEvent(
        id: String, name: String, type: EventType,
        date: LocalDate, hour: Int, minute: Int, durationMinutes: Int, description: String
    ): GuildEvent {
        val tz = TimeZone.of("Asia/Manila")
        val eventDateTime = LocalDateTime(date.year, date.month, date.dayOfMonth, hour, minute)
        var startInstant = eventDateTime.toInstant(tz)
        var endInstant = startInstant.plus(durationMinutes, DateTimeUnit.MINUTE)

        // If the event has already ended today, move to the next occurrence
        if (endInstant < Clock.System.now()) {
            val nextDay = date.plus(1, DateTimeUnit.DAY)
            val nextEventDateTime = LocalDateTime(nextDay.year, nextDay.month, nextDay.dayOfMonth, hour, minute)
            startInstant = nextEventDateTime.toInstant(tz)
            endInstant = startInstant.plus(durationMinutes, DateTimeUnit.MINUTE)
        }

        return GuildEvent(
            id = id,
            name = name,
            type = type,
            startTime = startInstant.toString(),
            endTime = endInstant.toString(),
            description = description,
            reminderSet = false
        )
    }

    private fun getNextDayOfWeek(today: LocalDate, allowedDays: List<DayOfWeek>): LocalDate {
        var current = today
        repeat(7) {
            if (allowedDays.contains(current.dayOfWeek)) {
                return current
            }
            current = current.plus(1, DateTimeUnit.DAY)
        }
        return today
    }
}

// Extension functions for mapping between Domain and Entity models
fun GuildEvent.toEntity(): EventEntity {
    return EventEntity(
        id = this.id,
        name = this.name,
        type = this.type.name,
        startTime = this.startTime,
        endTime = this.endTime,
        description = this.description,
        reminderSet = this.reminderSet
    )
}

fun EventEntity.toDomainModel(): GuildEvent {
    return GuildEvent(
        id = this.id,
        name = this.name,
        type = try { EventType.valueOf(this.type) } catch(e: Exception) { EventType.SPECIAL_EVENT },
        startTime = this.startTime,
        endTime = this.endTime,
        description = this.description,
        reminderSet = this.reminderSet
    )
}
