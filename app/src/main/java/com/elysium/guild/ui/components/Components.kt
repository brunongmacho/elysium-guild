package com.elysium.guild.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elysium.guild.models.*
import com.elysium.guild.ui.theme.*
import kotlinx.datetime.*
import java.util.Locale

@Composable
fun EventCard(
    event: GuildEvent,
    currentTime: Instant,
    onReminderClick: (GuildEvent) -> Unit
) {
    val countdown = remember(event.startTime, currentTime) {
        calculateCountdown(event.startTime, currentTime)
    }
    
    val statusColor = remember(event.startTime, currentTime) {
        getEventStatusColor(event.startTime, currentTime)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Event Icon and Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = statusColor.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = getEventIcon(event.type),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        
                        Column {
                            Text(
                                text = event.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val formattedTime = remember(event.startTime) {
                                formatEventTime(event.startTime)
                            }
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                // Event Description
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )

                // Countdown
                if (countdown.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.End),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = countdown,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    filters: List<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) },
                selected = selectedFilter == filter
            )
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry
        ) {
            Text("Retry")
        }
    }
}

private fun getEventStatusColor(startTime: String, now: Instant): Color {
    return try {
        val eventInstant = Instant.parse(startTime)
        val duration = eventInstant - now
        val minutesRemaining = duration.inWholeMinutes

        when {
            duration.isNegative() -> Color(0xFF10B981) // Green
            minutesRemaining <= 30 -> Color(0xFFF59E0B) // Yellow
            else -> Color(0xFFEF4444) // Red
        }
    } catch (e: Exception) {
        Color(0xFFEF4444)
    }
}

private fun getEventIcon(eventType: EventType): String {
    return when (eventType) {
        EventType.WORLD_BOSS -> "🐉"
        EventType.GUILD_DUNGEON -> "🏰"
        EventType.ARENA_BATTLE -> "⚔️"
        EventType.GUILD_BOSS -> "👹"
        EventType.GVG -> "⚔️"
        EventType.SPECIAL_EVENT -> "🎯"
    }
}

private fun formatEventTime(timeString: String): String {
    return try {
        val instant = Instant.parse(timeString)
        val localDateTime = instant.toLocalDateTime(TimeZone.of("Asia/Manila"))
        val day = localDateTime.dayOfMonth
        val month = localDateTime.month.name.substring(0, 3).lowercase().replaceFirstChar { it.uppercase() }
        val year = localDateTime.year
        val hour = if (localDateTime.hour % 12 == 0) 12 else localDateTime.hour % 12
        val minute = String.format("%02d", localDateTime.minute)
        val amPm = if (localDateTime.hour < 12) "AM" else "PM"

        "$month $day, $year $hour:$minute $amPm"
    } catch (e: Exception) {
        "Time TBD"
    }
}

private fun calculateCountdown(startTime: String, now: Instant): String {
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
