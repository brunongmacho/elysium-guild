package com.elysium.guild.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import com.elysium.guild.ui.theme.*
import com.elysium.guild.utils.Constants
import com.elysium.guild.utils.UIUtils
import kotlinx.datetime.Instant

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BossTimerCard(
    boss: BossTimer,
    currentTime: State<Instant>,
    useLocalTimezone: Boolean = false,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    
    val isElysiumTurn = boss.rotation?.isOurTurn == true
    val isSoon = boss.status == Constants.STATUS_SOON || (boss.timeRemaining != null && boss.timeRemaining <= Constants.SPAWNING_SOON_THRESHOLD_MS)
    val isReady = boss.status == Constants.STATUS_READY || boss.status == Constants.STATUS_OVERDUE || (boss.timeRemaining ?: 1L) <= 0L
    val isTracking = boss.status == Constants.STATUS_TRACKING

    val baseStatusColor = remember(boss.status, boss.timeRemaining, isElysiumTurn, isDark) {
        when {
            isReady ->
                if (isDark) StatusReadyGlow else Color(0xFF00796B) // Active: Teal/Green
            isElysiumTurn && isSoon ->
                if (isDark) ElysiumAmethyst else ElysiumAmethystDark // Guild Turn + Soon: Amethyst Magenta
            isElysiumTurn ->
                if (isDark) ElysiumPurpleLight else ElysiumPurple // Guild Turn: Purple
            isSoon ->
                if (isDark) Color(0xFFFFCC00) else Color(0xFFF57C00) // Soon: Yellow/Orange
            else -> 
                if (isDark) Color(0xFF888888) else Color(0xFF5D4037) // Tracking: Muted
        }
    }
    
    val animatedColor by animateColorAsState(
        targetValue = baseStatusColor,
        animationSpec = tween(durationMillis = 500),
        label = "CardColorAnimation"
    )

    ElysiumGlassCard(
        modifier = modifier.padding(vertical = 6.dp),
        statusColor = animatedColor,
        glowColor = when {
            isReady || isElysiumTurn || isSoon || isTracking -> animatedColor
            else -> Color.Transparent
        },
        showLegendaryEffect = isElysiumTurn,
        onClick = { /* Detail navigation can go here */ }
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
                with(sharedTransitionScope) {
                    BossAvatar(
                        boss = boss, 
                        statusColor = animatedColor, 
                        isUrgent = isReady || (isElysiumTurn && isSoon),
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState(key = "boss-image-${boss.bossName}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    )
                }

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
                                    shadow = Shadow(
                                        color = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Transparent,
                                        blurRadius = 4f
                                    )
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isReady || isElysiumTurn) animatedColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${boss.bossPoints} BP",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isElysiumTurn) animatedColor else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            StatusBadge(boss = boss, currentTime = currentTime, statusColor = animatedColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (boss.rotation?.isRotating == true) {
                        RotationStatus(boss.rotation!!, animatedColor)
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
        var currentIndex = 0
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
        
        var lastAppendedIndex = 0
        mergedMatches.forEach { range ->
            append(text.substring(lastAppendedIndex, range.first))
            withStyle(style = SpanStyle(
                color = highlightColor, 
                fontWeight = FontWeight.Black,
                background = highlightColor.copy(alpha = 0.15f)
            )) {
                append(text.substring(range.first, range.last))
            }
            lastAppendedIndex = range.last
        }
        append(text.substring(lastAppendedIndex))
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
    val isElysiumTurn = boss.rotation?.isOurTurn == true
    val isDark = isSystemInDarkTheme()

    if (isReady) {
        val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
        val badgeScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "BadgeScale"
        )

        Surface(
            color = statusColor,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = if (isDark) 8.dp else 2.dp,
            modifier = Modifier.scale(badgeScale)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = if (isDark) Color.Black else Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ACTIVE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.Black else Color.White
                )
            }
        }
    } else {
        val countdownText = remember(boss.nextSpawnTime, currentTime.value) {
            boss.nextSpawnTime?.let { calculateBossCountdown(it, currentTime.value) } ?: ""
        }
        
        if (countdownText.isNotEmpty() || isElysiumTurn) {
            Surface(
                color = statusColor.copy(alpha = if (isDark) 0.15f else 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp, 
                    color = statusColor.copy(alpha = if (isDark) 0.5f else 0.8f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = when {
                            isElysiumTurn -> Icons.Default.Shield
                            isSoon -> Icons.Default.Timer
                            else -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isElysiumTurn && countdownText.isEmpty()) "GUILD TURN" else countdownText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun RotationStatus(rotation: RotationInfo, highlightColor: Color) {
    val isDark = isSystemInDarkTheme()
    val isElysium = rotation.isOurTurn == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isElysium) 
                    highlightColor.copy(alpha = if (isDark) 0.1f else 0.15f)
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), 
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (isElysium) highlightColor.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        rotation.currentGuild?.let { current ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isElysium) highlightColor else Color.Gray,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isElysium) "Current: $current (GUILD TURN)" else "Current: $current",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isElysium) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isElysium) FontWeight.ExtraBold else FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun SpawnTimeText(boss: BossTimer, useLocalTimezone: Boolean) {
    boss.nextSpawnTime?.let { spawnTime ->
        val formattedTime = remember(spawnTime, boss.status, boss.type, useLocalTimezone) {
            val prefix = if (boss.type == "schedule") "Scheduled:" else "Spawn Window:"
            "$prefix ${UIUtils.formatEventTime(spawnTime, useLocalTimezone)}"
        }
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
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
    
    if (!isReady && timeRemaining <= threshold) {
        val progress = remember(timeRemaining, currentTime.value) {
            val currentDiff = boss.nextSpawnTime?.let { 
                try { (Instant.parse(it) - currentTime.value).inWholeMilliseconds } catch(e: Exception) { timeRemaining }
            } ?: timeRemaining
            (1f - (currentDiff.toFloat() / threshold.toFloat())).coerceIn(0f, 1f)
        }

        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
            label = "ProgressBarAnimation"
        )

        // Requirement 17: Progress Gauge with Glow
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color.Black.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, animatedColor.copy(alpha = 0.3f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(animatedColor.copy(alpha = 0.7f), animatedColor)
                            ),
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun BossAvatar(
    boss: BossTimer,
    statusColor: Color,
    modifier: Modifier = Modifier,
    size: Int = 64,
    isUrgent: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size((size + 16).dp)
            .then(if (isUrgent) Modifier.scale(pulseScale) else Modifier)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(statusColor, statusColor.copy(alpha = 0.2f), statusColor)
                )
            )
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
                error = { BossInitialAvatar(boss.bossName, statusColor, size) }
            )
        } else {
            BossInitialAvatar(boss.bossName, statusColor, size)
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
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                            Box(modifier = Modifier
                                .size(140.dp, 20.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier
                                .size(60.dp, 12.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                        }
                        Box(modifier = Modifier
                            .size(70.dp, 24.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), RoundedCornerShape(12.dp)))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier
                        .size(180.dp, 10.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                }
            }
        }
    }
}

private fun shareBossStatus(context: Context, boss: BossTimer, now: Instant) {
    val countdown = boss.nextSpawnTime?.let { calculateBossCountdown(it, now) } ?: ""
    val isReady = boss.status == Constants.STATUS_READY || boss.status == Constants.STATUS_OVERDUE

    val message = if (isReady) {
        "⚔️ [ELYSIUM] BOSS ACTIVE: ${boss.bossName.uppercase()}! To arms! ⚔️"
    } else {
        "⏳ [ELYSIUM] BOSS INBOUND: ${boss.bossName.uppercase()} in $countdown! ⏳"
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
