package com.elysium.guild.repository

import com.elysium.guild.models.*
import com.elysium.guild.network.ElysiumApiService
import kotlinx.datetime.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsRepository @Inject constructor(
    private val apiService: ElysiumApiService
) {
    
    suspend fun getEvents(): List<GuildEvent> {
        return try {
            val response = apiService.getEvents()
            val apiEvents = if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.events ?: emptyList()
            } else {
                emptyList()
            }

            // If API returns no events, provide the guild's standard schedule
            if (apiEvents.isEmpty()) {
                getMockEvents()
            } else {
                apiEvents
            }
        } catch (e: Exception) {
            getMockEvents()
        }
    }

    private fun getMockEvents(): List<GuildEvent> {
        val now = Clock.System.now()
        val tz = TimeZone.of("Asia/Manila")
        val today = now.toLocalDateTime(tz).date

        return listOf(
            createGuildEvent(
                "1", "World Boss Event (Morning)", EventType.WORLD_BOSS,
                today, 11, 0, "Daily World Boss - 11:00 AM (30m duration)"
            ),
            createGuildEvent(
                "2", "World Boss Event (Evening)", EventType.WORLD_BOSS,
                today, 20, 0, "Daily World Boss - 8:00 PM (30m duration)"
            ),
            createGuildEvent(
                "3", "Individual Arena", EventType.ARENA_BATTLE,
                getNextDayOfWeek(today, listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
                20, 30, "Mon, Wed, Fri Arena Battle - 8:30 PM (60m duration)"
            ),
            createGuildEvent(
                "4", "Guild Boss", EventType.GUILD_BOSS,
                getNextDayOfWeek(today, listOf(DayOfWeek.MONDAY)),
                21, 0, "Monday Guild Boss - 9:00 PM (5m duration)"
            ),
            createGuildEvent(
                "5", "Coop Round Arena", EventType.ARENA_BATTLE,
                getNextDayOfWeek(today, listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)),
                20, 30, "Tue, Thu, Sat Coop Arena - 8:30 PM (60m duration)"
            ),
            createGuildEvent(
                "6", "Guild War Queue Reminder", EventType.SPECIAL_EVENT,
                getNextDayOfWeek(today, listOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)),
                23, 0, "Thu, Fri, Sat GvG Queue - 11:00 PM (120m duration)"
            ),
            createGuildEvent(
                "7", "GvG / Guild War", EventType.GVG,
                getNextDayOfWeek(today, listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
                20, 25, "Fri, Sat, Sun Guild War - 8:25 PM (3m duration)"
            )
        ).sortedBy { it.startTime }
    }

    private fun createGuildEvent(
        id: String, name: String, type: EventType,
        date: LocalDate, hour: Int, minute: Int, description: String
    ): GuildEvent {
        val tz = TimeZone.of("Asia/Manila")
        var eventDateTime = LocalDateTime(date.year, date.month, date.dayOfMonth, hour, minute)
        var instant = eventDateTime.toInstant(tz)

        // If the event today has already passed, move to the next occurrence
        if (instant < Clock.System.now()) {
            // For daily events, just add a day. For others, it's handled by getNextDayOfWeek usually,
            // but we add a safety check here.
            val nextDay = date.plus(1, DateTimeUnit.DAY)
            eventDateTime = LocalDateTime(nextDay.year, nextDay.month, nextDay.dayOfMonth, hour, minute)
            instant = eventDateTime.toInstant(tz)
        }

        return GuildEvent(
            id = id,
            name = name,
            type = type,
            startTime = instant.toString(),
            endTime = null,
            description = description,
            reminderSet = false
        )
    }

    private fun getNextDayOfWeek(today: LocalDate, allowedDays: List<DayOfWeek>): LocalDate {
        var current = today
        // Check if today is an allowed day and if we have a special case,
        // but for simplicity in mock, we just find the nearest upcoming allowed day including today
        repeat(7) {
            if (allowedDays.contains(current.dayOfWeek)) {
                return current
            }
            current = current.plus(1, DateTimeUnit.DAY)
        }
        return today
    }
}
