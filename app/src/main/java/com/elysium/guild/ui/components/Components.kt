package com.elysium.guild.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elysium.guild.models.*
import com.elysium.guild.ui.theme.*
import com.elysium.guild.utils.Constants
import com.elysium.guild.utils.UIUtils
import kotlinx.datetime.*
import java.util.Locale

@Composable
fun EventCard(
    event: GuildEvent,
    currentTime: State<Instant>,
    onReminderClick: (GuildEvent) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    val targetColor = remember(event.startTime, currentTime.value, isDark) {
        val diffMs = try { (Instant.parse(event.startTime) - currentTime.value).inWholeMilliseconds } catch(e: Exception) { null }
        UIUtils.getStatusColor(null, diffMs, isDark)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "EventColorAnimation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.2f else 0.15f),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.05f else 0.02f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f) 
                else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isDark) 4.dp else 2.dp,
        shadowElevation = if (isDark) 0.dp else 3.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                animatedColor.copy(alpha = if (isDark) 0.12f else 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = animatedColor.copy(alpha = if (isDark) 0.2f else 0.1f)
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
                
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )

                // Shared Status Badge Behavior
                val countdown = remember(event.startTime, currentTime.value) {
                    calculateCountdown(event.startTime, currentTime.value)
                }

                if (countdown.isNotEmpty()) {
                    Surface(
                        color = animatedColor.copy(alpha = if (isDark) 0.4f else 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.End),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp, 
                            color = animatedColor.copy(alpha = if (isDark) 0.3f else 0.4f)
                        )
                    ) {
                        Text(
                            text = countdown,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (!isDark) animatedColor.copy(alpha = 0.9f) 
                                    else animatedColor
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
