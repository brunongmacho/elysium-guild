package com.elysium.guild.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.elysium.guild.models.*
import com.elysium.guild.utils.Constants
import com.elysium.guild.utils.UIUtils
import kotlinx.datetime.Instant

@Composable
fun BossTimerCard(
    boss: BossTimer,
    currentTime: State<Instant>,
    useLocalTimezone: Boolean = false,
    modifier: Modifier = Modifier,
    searchQuery: String = ""
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

    val isElysiumTurn = boss.rotation?.isOurTurn == true
    val isReady = boss.status == Constants.STATUS_READY || boss.status == Constants.STATUS_OVERDUE || (boss.timeRemaining ?: 1L) <= 0L

    ElysiumGlassCard(
        modifier = modifier.padding(vertical = 6.dp),
        statusColor = animatedColor,
        glowColor = if (isElysiumTurn) MaterialTheme.colorScheme.primary else Color.Transparent,
        onClick = { /* Could navigate to detail if needed */ }
    ) {
        Column(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            shareBossStatus(context, boss, currentTime.value)
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BossAvatar(boss = boss, statusColor = animatedColor, isUrgent = isReady)

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = highlightSearchText(boss.bossName, searchQuery, MaterialTheme.colorScheme.primary),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    shadow = if (searchQuery.isNotEmpty()) Shadow(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        blurRadius = 8f
                                    ) else null
                                ),
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
                        RotationStatus(boss.rotation!!)
                    }

                    SpawnTimeText(boss, useLocalTimezone)
                }
            }

            DynamicProgressBar(boss, currentTime, animatedColor)
        }
    }
}

@Composable
fun highlightSearchText(text: String, query: String, highlightColor: Color): AnnotatedString {
    if (query.isEmpty()) return AnnotatedString(text)
    
    val queries = query.split(" ").filter { it.isNotEmpty() }
    
    return buildAnnotatedString {
        var currentText = text
        var lastIndex = 0
        
        val matches = mutableListOf<IntRange>()
        queries.forEach { q ->
            var start = 0
            while (true) {
                val index = text.indexOf(q, start, ignoreCase = true)
                if (index == -1) break
                matches.add(index until index + q.length)
                start = index + q.length
            }
        }
        
        // Sort and merge overlapping ranges
        val sortedMatches = matches.sortedBy { it.first }
        val mergedMatches = mutableListOf<IntRange>()
        if (sortedMatches.isNotEmpty()) {
            var currentRange = sortedMatches[0]
            for (i in 1 until sortedMatches.size) {
                val nextRange = sortedMatches[i]
                if (nextRange.first <= currentRange.last) {
                    currentRange = currentRange.first until maxOf(currentRange.last, nextRange.last)
                } else {
                    mergedMatches.add(currentRange)
                    currentRange = nextRange
                }
            }
            mergedMatches.add(currentRange)
        }
        
        var currentIndex = 0
        mergedMatches.forEach { range ->
            append(text.substring(currentIndex, range.first))
            withStyle(style = SpanStyle(
                color = highlightColor, 
                fontWeight = FontWeight.Black,
                background = highlightColor.copy(alpha = 0.1f)
            )) {
                append(text.substring(range.first, range.last))
            }
            currentIndex = range.last
        }
        append(text.substring(currentIndex))
    }
}

@Composable
private fun StatusBadge(
    boss: BossTimer,
    currentTime: State<Instant>,
    statusColor: Color
) {
    val isReady = boss.status == Constants.STATUS_READY || boss.status == Constants.STATUS_OVERDUE || (boss.timeRemaining ?: 1L) <= 0L
    val isSoon = !isReady && (boss.status == Constants.STATUS_SOON || (boss.timeRemaining != null && boss.timeRemaining <= Constants.SPAWNING_SOON_THRESHOLD_MS))
    val isDark = isSystemInDarkTheme()

    if (isReady) {
        val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
        val badgeScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "BadgeScale"
        )

        Surface(
            color = Constants.COLOR_READY.copy(alpha = if (isDark) 0.8f else 1f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.scale(badgeScale)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = Constants.LABEL_READY,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    } else {
        val countdownText = remember(boss.nextSpawnTime, currentTime.value) {
            boss.nextSpawnTime?.let { calculateBossCountdown(it, currentTime.value) } ?: ""
        }
        
        val countdownColor = UIUtils.getCountdownColor(boss.timeRemaining, isDark)

        if (countdownText.isNotEmpty()) {
            Surface(
                color = countdownColor.copy(alpha = if (isDark) 0.2f else 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp, 
                    color = countdownColor.copy(alpha = if (isDark) 0.4f else 0.5f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isSoon) Icons.Default.Warning else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = countdownColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = countdownText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = countdownColor
                    )
                }
            }
        }
    }
}

@Composable
private fun RotationStatus(rotation: RotationInfo) {
    val isDark = isSystemInDarkTheme()
    val isElysium = rotation.isOurTurn == true

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
    val isReady = boss.status == Constants.STATUS_READY || boss.status == Constants.STATUS_OVERDUE || timeRemaining <= 0L
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
                .padding(bottom = 16.dp)
                .clip(CircleShape),
            color = animatedColor,
            trackColor = animatedColor.copy(alpha = if (isDark) 0.1f else 0.05f)
        )
    }
}

@Composable
fun BossAvatar(
    boss: BossTimer,
    statusColor: Color,
    size: Int = 60,
    isUrgent: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val avatarColor = remember(boss.bossName) {
        val hash = boss.bossName.hashCode()
        val colors = listOf(
            Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFFF59E0B),
            Color(0xFF10B981), Color(0xFF8B5CF6), Color(0xFF06B6D4)
        )
        colors[Math.abs(hash) % colors.size]
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size((size + 12).dp)
            .then(if (isUrgent) Modifier.scale(pulseScale) else Modifier)
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

        val pixelSize = with(density) { size.dp.roundToPx() }

        if (imageSource != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageSource)
                    .size(pixelSize)
                    .crossfade(true)
                    .build(),
                contentDescription = boss.bossName,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                },
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
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.4f else 1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // Avatar Shimmer
                Box(modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), CircleShape))
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            // Title Shimmer
                            Box(modifier = Modifier
                                .size(140.dp, 20.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(8.dp))
                            // Points Shimmer
                            Box(modifier = Modifier
                                .size(60.dp, 12.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                        }
                        
                        // Badge Shimmer
                        Box(modifier = Modifier
                            .size(70.dp, 24.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), RoundedCornerShape(12.dp)))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Time Shimmer
                    Box(modifier = Modifier
                        .size(180.dp, 10.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                }
            }
            
            // Progress Bar Shimmer (Sometimes visible)
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.5f), CircleShape))
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
