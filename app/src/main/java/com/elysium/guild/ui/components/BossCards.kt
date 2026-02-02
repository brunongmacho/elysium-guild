package com.elysium.guild.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.elysium.guild.models.*
import com.elysium.guild.utils.Constants
import kotlinx.datetime.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BossTimerCard(
    boss: BossTimer,
    currentTime: Instant
) {
    val context = LocalContext.current
    val isSpawned = boss.status == "ready" || boss.status == "overdue" || (boss.timeRemaining ?: 1) <= 0
    
    val targetColor = remember(boss.nextSpawnTime, currentTime) {
        getBossStatusColor(boss, currentTime)
    }
    
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "CardColorAnimation"
    )
    
    val countdown = remember(boss.nextSpawnTime, currentTime) {
        boss.nextSpawnTime?.let { calculateBossCountdown(it, currentTime) } ?: ""
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
        Box(modifier = Modifier.fillMaxWidth()) {
            // Seamless background gradient that covers the entire card area
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                animatedColor.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Boss Image with Status Ring
                    BossAvatar(boss = boss, statusColor = animatedColor)

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = boss.bossName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${boss.bossPoints} Points",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Share Button (Call to Arms)
                                IconButton(
                                    onClick = { 
                                        shareBossStatus(context, boss, countdown)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                if (!isSpawned && countdown.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = countdown,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else if (isSpawned) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(12.dp),
                                        shadowElevation = 4.dp
                                    ) {
                                        Text(
                                            text = "READY",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (boss.rotation?.isRotating == true) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                boss.rotation.currentGuild?.let { current ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).background(if (boss.rotation.isOurTurn == true) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Current: $current",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (boss.rotation.isOurTurn == true) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (boss.rotation.isOurTurn == true) 
                                                FontWeight.Bold 
                                            else 
                                                FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        boss.nextSpawnTime?.let { spawnTime ->
                            val formattedTime = remember(spawnTime, boss.status, boss.type) {
                                formatSpawnTime(spawnTime, boss.status, boss.type)
                            }
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Visual Progress Bar for "Soon" bosses
                if (!isSpawned && boss.timeRemaining != null) {
                    val progress = remember(boss.timeRemaining) {
                        val threshold = Constants.SPAWNING_SOON_THRESHOLD_MINUTES * 60 * 1000L
                        if (boss.timeRemaining <= threshold) {
                            1f - (boss.timeRemaining.toFloat() / threshold.toFloat())
                        } else 0f
                    }
                    
                    if (progress > 0f) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress.coerceIn(0f, 1f),
                            animationSpec = tween(durationMillis = 1000),
                            label = "ProgressBarAnimation"
                        )
                        
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .padding(horizontal = 16.dp)
                                .clip(CircleShape),
                            color = animatedColor,
                            trackColor = animatedColor.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BossAvatar(
    boss: BossTimer,
    statusColor: Color,
    size: Int = 60
) {
    val context = LocalContext.current
    val avatarColor = remember(boss.bossName) {
        val hash = boss.bossName.hashCode()
        val colors = listOf(
            Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFFF59E0B), 
            Color(0xFF10B981), Color(0xFF8B5CF6), Color(0xFF06B6D4)
        )
        colors[Math.abs(hash) % colors.size]
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size((size + 12).dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = statusColor.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, statusColor.copy(alpha = 0.5f))
        ) {}

        val imageSource = remember(boss.bossName, boss.imageUrl) {
            val resourceName = boss.bossName.lowercase()
                .replace(Regex("[^a-z0-9]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
            
            val resId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
            
            if (resId != 0) resId
            else if (!boss.imageUrl.isNullOrEmpty()) {
                if (boss.imageUrl.startsWith("http")) boss.imageUrl
                else "${Constants.BASE_URL.removeSuffix("/")}/${boss.imageUrl.removePrefix("/")}"
            } else {
                null
            }
        }

        if (imageSource != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageSource)
                    .crossfade(true)
                    .build(),
                contentDescription = boss.bossName,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                loading = { CircularProgressIndicator(modifier = Modifier.padding(16.dp), strokeWidth = 2.dp) },
                error = { BossInitialAvatar(boss.bossName, avatarColor, size) }
            )
        } else {
            BossInitialAvatar(boss.bossName, avatarColor, size)
        }
    }
}

@Composable
fun BossInitialAvatar(name: String, backgroundColor: Color, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = backgroundColor
        )
    }
}

@Composable
fun BossCarouselCard(
    boss: BossTimer,
    currentTime: Instant
) {
    val isSpawned = boss.status == "ready" || boss.status == "overdue" || (boss.timeRemaining ?: 1) <= 0
    val targetColor = getBossStatusColor(boss, currentTime)
    val countdown = boss.nextSpawnTime?.let { calculateBossCountdown(it, currentTime) } ?: ""

    Surface(
        modifier = Modifier
            .width(260.dp)
            .height(100.dp)
            .padding(end = 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = targetColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, targetColor.copy(alpha = 0.3f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BossAvatar(boss = boss, statusColor = targetColor, size = 48)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = boss.bossName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                if (isSpawned) {
                    Text(
                        text = "READY",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF10B981)
                    )
                } else {
                    Text(
                        text = countdown,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = targetColor
                    )
                }
            }
        }
    }
}

@Composable
fun BossTimerShimmerItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = alpha),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp).background(Color.White.copy(alpha = 0.1f), CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Box(modifier = Modifier.size(120.dp, 20.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.size(80.dp, 12.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
            }
        }
    }
}

private fun shareBossStatus(context: Context, boss: BossTimer, countdown: String) {
    val message = if (boss.status == "ready" || boss.status == "overdue") {
        "[ELYSIUM] BOSS READY: ${boss.bossName.uppercase()} is spawning now! Get to the relay! ⚔️"
    } else {
        "[ELYSIUM] BOSS REMINDER: ${boss.bossName.uppercase()} spawning in $countdown! Prepare for the kill! ⚔️"
    }
    
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Boss CTA", message)
    clipboard.setPrimaryClip(clip)
    
    Toast.makeText(context, "Call to Arms copied to clipboard!", Toast.LENGTH_SHORT).show()
}

private fun getBossStatusColor(boss: BossTimer, now: Instant): Color {
    val eventInstant = boss.nextSpawnTime?.let { try { Instant.parse(it) } catch (e: Exception) { null } } ?: return Color(0xFF6B7280)
    val duration = eventInstant - now
    val minutesRemaining = duration.inWholeMinutes

    return when {
        duration.isNegative() -> Color(0xFF10B981) // Green
        minutesRemaining <= 30 -> Color(0xFFF59E0B) // Yellow
        else -> Color(0xFFEF4444) // Red
    }
}

private fun formatSpawnTime(spawnTime: String, status: String, type: String): String {
    return try {
        val instant = Instant.parse(spawnTime)
        val javaInstant = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
        val zonedDateTime = javaInstant.atZone(java.time.ZoneId.of("Asia/Manila"))
        val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH)
        val formattedDate = zonedDateTime.format(formatter)
        
        val prefix = if (type == "schedule") "Scheduled:" else "Spawns:"
        "$prefix $formattedDate"
    } catch (e: Exception) {
        "Time TBD"
    }
}

private fun calculateBossCountdown(spawnTime: String, now: Instant): String {
    return try {
        val eventInstant = Instant.parse(spawnTime)
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
