package com.elysium.guild.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    useLocalTimezone: Boolean = false,
    onReminderClick: (GuildEvent) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    val targetColor = remember(event.startTime, event.endTime, currentTime.value, isDark) {
        UIUtils.getEventStatusColor(event.startTime, event.endTime, currentTime.value, isDark)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "EventColorAnimation"
    )

    ElysiumGlassCard(
        statusColor = animatedColor
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
                                text = UIUtils.getEventIcon(event.type),
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
                        val formattedTime = remember(event.startTime, useLocalTimezone) {
                            UIUtils.formatEventTime(event.startTime, useLocalTimezone)
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

            val countdown = remember(event.startTime, event.endTime, currentTime.value) {
                UIUtils.calculateEventCountdown(event.startTime, event.endTime, currentTime.value)
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

@Composable
fun ElysiumFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    selectedColor: Color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else Constants.COLOR_TRACKING_LIGHT
) {
    val isDark = isSystemInDarkTheme()
    
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedColor.copy(alpha = if (isDark) 0.25f else 0.15f) 
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        border = BorderStroke(
            width = 1.dp,
            brush = if (selected) {
                Brush.linearGradient(
                    colors = listOf(
                        selectedColor.copy(alpha = 0.8f),
                        selectedColor.copy(alpha = 0.2f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                )
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false
            )
            if (count != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    color = if (selected)
                        selectedColor.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    filters: List<String>
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            ElysiumFilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = filter
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
