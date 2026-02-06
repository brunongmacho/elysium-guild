package com.elysium.guild.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.elysium.guild.models.*
import com.elysium.guild.utils.Constants
import com.elysium.guild.utils.UIUtils
import kotlinx.datetime.Instant

@Composable
fun BossTimerCard(
    boss: BossTimer,
    currentTime: State<Instant>,
    useLocalTimezone: Boolean = false
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    
    val targetColor = remember(boss.status, boss.timeRemaining, isDark) {
        UIUtils.getStatusColor(boss.status, boss.timeRemaining, isDark)
    }
    
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "CardColorAnimation"
    )

    ElysiumGlassCard(
        statusColor = animatedColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        IconButton(
                            onClick = { shareBossStatus(context, boss, currentTime.value) },
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

                        StatusBadge(boss = boss, currentTime = currentTime, statusColor = animatedColor)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (boss.rotation?.isRotating == true) {
                    RotationStatus(boss.rotation)
                }

                SpawnTimeText(boss, useLocalTimezone)
            }
        }

        DynamicProgressBar(boss, currentTime, animatedColor)
    }
}

@Composable
private fun StatusBadge(
    boss: BossTimer,
    currentTime: State<Instant>,
    statusColor: Color
) {
    val isReady = boss.status == Constants.STATUS_READY || boss.status == Constants.STATUS_OVERDUE || (boss.timeRemaining ?: 1) <= 0
    val isSoon = !isReady && (boss.status == Constants.STATUS_SOON || (boss.timeRemaining != null && boss.timeRemaining <= Constants.SPAWNING_SOON_THRESHOLD_MS))
    val isDark = isSystemInDarkTheme()

    if (isReady) {
        Surface(
            color = Constants.COLOR_READY.copy(alpha = if (isDark) 0.8f else 1f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp
        ) {
            Text(
                text = Constants.LABEL_READY,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    } else {
        val countdownText = remember(boss.nextSpawnTime, currentTime.value) {
            boss.nextSpawnTime?.let { calculateBossCountdown(it, currentTime.value) } ?: ""
        }
        
        if (countdownText.isNotEmpty()) {
            Surface(
                color = statusColor.copy(alpha = if (isDark) 0.4f else 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp, 
                    color = statusColor.copy(alpha = if (isDark) 0.3f else 0.4f)
                )
            ) {
                Text(
                    text = countdownText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (!isDark) statusColor.copy(alpha = 0.9f) 
                            else statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RotationStatus(rotation: RotationInfo) {
    val isDark = isSystemInDarkTheme()
    val isElysium = rotation.currentGuild?.uppercase() == "ELYSIUM"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isElysium) 
                    MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.15f else 0.1f)
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.05f else 0.03f), 
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isElysium) 1.dp else 0.dp,
                color = if (isElysium) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        rotation.currentGuild?.let { current ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isElysium) MaterialTheme.colorScheme.primary else Color.Gray,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isElysium) "Current: $current (OUR TURN)" else "Current: $current",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isElysium)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isElysium)
                        FontWeight.ExtraBold
                    else
                        FontWeight.Medium,
                    letterSpacing = if (isElysium) 0.5.sp else 0.sp
                )
            }
        }
    }
}

@Composable
private fun SpawnTimeText(boss: BossTimer, useLocalTimezone: Boolean) {
    boss.nextSpawnTime?.let { spawnTime ->
        val formattedTime = remember(spawnTime, boss.status, boss.type, useLocalTimezone) {
            val prefix = if (boss.type == "schedule") "Scheduled:" else "Spawns:"
            "$prefix ${UIUtils.formatEventTime(spawnTime, useLocalTimezone)}"
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

@Composable
private fun DynamicProgressBar(
    boss: BossTimer,
    currentTime: State<Instant>,
    animatedColor: Color
) {
    val threshold = Constants.SPAWNING_SOON_THRESHOLD_MS
    val timeRemaining = boss.timeRemaining ?: return
    val isReady = boss.status == Constants.STATUS_READY || boss.status == Constants.STATUS_OVERDUE || timeRemaining <= 0
    val isDark = isSystemInDarkTheme()
    
    if (!isReady && timeRemaining <= threshold) {
        val progress = remember(timeRemaining, currentTime.value) {
            val currentDiff = boss.nextSpawnTime?.let { 
                try { (Instant.parse(it) - currentTime.value).inWholeMilliseconds } catch(e: Exception) { timeRemaining }
            } ?: timeRemaining
            (1f - (currentDiff.toFloat() / threshold.toFloat())).coerceIn(0f, 1f)
        }

        val animatedProgress by animateFloatAsState(
            targetValue = progress,
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
            trackColor = animatedColor.copy(alpha = if (isDark) 0.1f else 0.05f)
        )
        Spacer(modifier = Modifier.height(16.dp))
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
fun BossTimerShimmerItem() {
    val isDark = isSystemInDarkTheme()
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
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) alpha else alpha * 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Box(modifier = Modifier.size(120.dp, 20.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.size(80.dp, 12.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
            }
        }
    }
}

private fun shareBossStatus(context: Context, boss: BossTimer, now: Instant) {
    val countdown = boss.nextSpawnTime?.let { calculateBossCountdown(it, now) } ?: ""
    val isReady = boss.status == Constants.STATUS_READY || boss.status == Constants.STATUS_OVERDUE

    val message = if (isReady) {
        "[ELYSIUM] BOSS READY: ${boss.bossName.uppercase()} is spawning now! Get to the relay! ⚔️"
    } else {
        "[ELYSIUM] BOSS REMINDER: ${boss.bossName.uppercase()} spawning in $countdown! Prepare for the kill! ⚔️"
    }
    
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Boss CTA", message)
    clipboard.setPrimaryClip(clip)
    
    Toast.makeText(context, "Call to Arms copied to clipboard!", Toast.LENGTH_SHORT).show()
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
