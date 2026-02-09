package com.elysium.guild.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elysium.guild.models.GuildEvent
import com.elysium.guild.utils.UIUtils
import kotlinx.datetime.Instant

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ElysiumEventCard(
    event: GuildEvent,
    currentTime: State<Instant>,
    useLocalTimezone: Boolean = false,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val isDark = isSystemInDarkTheme()
    val now = currentTime.value
    
    val targetColor = remember(event.startTime, event.endTime, now, isDark) {
        UIUtils.getEventStatusColor(event.startTime, event.endTime, now, isDark)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "EventColorAnimation"
    )

    val start = try { Instant.parse(event.startTime) } catch (e: Exception) { now }
    val end = event.endTime?.let { try { Instant.parse(it) } catch(e: Exception) { null } }
    val isLive = end?.let { now >= start && now < it } ?: (now >= start && (now - start).inWholeMinutes < 60)

    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    ElysiumGlassCard(
        modifier = modifier.then(if (isLive) Modifier.scale(pulseScale) else Modifier),
        statusColor = animatedColor,
        glowColor = if (isLive) animatedColor else Color.Transparent,
        onClick = { /* Navigation or detail can go here */ }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Event Icon Wrapper
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = animatedColor.copy(alpha = if (isDark) 0.15f else 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, animatedColor.copy(alpha = 0.5f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = UIUtils.getEventIcon(event.type),
                                fontSize = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isLive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            EventLiveBadge(animatedColor)
                        }
                    }
                    
                    Text(
                        text = UIUtils.formatEventTime(event.startTime, useLocalTimezone),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            // Requirement 17: Progress Gauge for events
            if (isLive && end != null) {
                val total = (end - start).inWholeMilliseconds
                val elapsed = (now - start).inWholeMilliseconds
                if (total > 0) {
                    val progress = (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    EventProgressBar(progress, animatedColor)
                }
            }

            // Countdown Badge
            val countdown = UIUtils.calculateEventCountdown(event.startTime, event.endTime, now)
            if (countdown.isNotEmpty()) {
                Surface(
                    color = animatedColor.copy(alpha = if (isDark) 0.1f else 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End),
                    border = androidx.compose.foundation.BorderStroke(1.dp, animatedColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = countdown,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = animatedColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EventLiveBadge(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "Live")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "Alpha"
    )
    Surface(
        color = color.copy(alpha = alpha),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "LIVE",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            fontSize = 8.sp
        )
    }
}

@Composable
fun EventProgressBar(progress: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(Color.Black.copy(alpha = 0.1f), CircleShape)
            .border(0.5.dp, color.copy(alpha = 0.2f), CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)),
                    CircleShape
                )
        )
    }
}
