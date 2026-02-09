package com.elysium.guild.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elysium.guild.models.*
import com.elysium.guild.utils.Constants
import com.elysium.guild.viewmodel.PointsFilter
import kotlin.random.Random

@Composable
fun LeaderboardPodium(
    topThree: List<LeaderboardEntry>,
    leaderboardType: LeaderboardType,
    pointsFilter: PointsFilter = PointsFilter.EARNED,
    searchQuery: String = ""
) {
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (topThree.size >= 2) {
            PodiumItem(
                member = topThree[1],
                rank = 2,
                targetHeight = 200.dp,
                baseColor = Constants.COLOR_SILVER,
                leaderboardType = leaderboardType,
                pointsFilter = pointsFilter,
                modifier = Modifier.weight(1f),
                searchQuery = searchQuery,
                animate = startAnimation,
                delay = 200
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (topThree.isNotEmpty()) {
            PodiumItem(
                member = topThree[0],
                rank = 1,
                targetHeight = 240.dp,
                baseColor = Constants.COLOR_GOLD,
                leaderboardType = leaderboardType,
                pointsFilter = pointsFilter,
                modifier = Modifier.weight(1.2f),
                searchQuery = searchQuery,
                animate = startAnimation,
                delay = 0
            )
        }

        if (topThree.size >= 3) {
            PodiumItem(
                member = topThree[2],
                rank = 3,
                targetHeight = 180.dp,
                baseColor = Constants.COLOR_BRONZE,
                leaderboardType = leaderboardType,
                pointsFilter = pointsFilter,
                modifier = Modifier.weight(1f),
                searchQuery = searchQuery,
                animate = startAnimation,
                delay = 400
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumItem(
    member: LeaderboardEntry,
    rank: Int,
    targetHeight: Dp,
    baseColor: Color,
    leaderboardType: LeaderboardType,
    pointsFilter: PointsFilter,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    animate: Boolean = false,
    delay: Int = 0
) {
    val height by animateDpAsState(
        targetValue = if (animate) targetHeight else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
            visibilityThreshold = 1.dp
        ),
        label = "PodiumHeight"
    )

    Box(modifier = modifier.height(height)) {
        ElysiumGlassCard(
            modifier = Modifier.fillMaxSize(),
            statusColor = baseColor
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Dynamic Glitter Effect
                PodiumGlitterEffect(
                    color = baseColor,
                    modifier = Modifier.matchParentSize()
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = baseColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(28.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = rank.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val annotatedName = if (searchQuery.isEmpty()) AnnotatedString(member.username) else highlightSearchText(member.username, searchQuery, MaterialTheme.colorScheme.primary)
                    Text(
                        text = annotatedName,
                        style = MaterialTheme.typography.labelLarge.copy(
                            shadow = if (searchQuery.isNotEmpty()) Shadow(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                blurRadius = 8f
                            ) else null
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val (mainStatValue, mainStatLabel) = when (member) {
                        is AttendanceLeaderboardEntry -> "${member.attendanceRate}%" to "RATE"
                        is PointsLeaderboardEntry -> {
                            when (pointsFilter) {
                                PointsFilter.EARNED -> member.pointsEarned.toString() to "EARNED"
                                PointsFilter.SPENT -> member.pointsSpent.toString() to "SPENT"
                                PointsFilter.AVAILABLE -> member.pointsAvailable.toString() to "AVAIL"
                            }
                        }
                        else -> "" to ""
                    }

                    Text(
                        text = mainStatValue,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp
                    )
                    Text(
                        text = mainStatLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 9.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    when (member) {
                        is AttendanceLeaderboardEntry -> {
                            PodiumSecondaryStat("Kills", member.totalKills.toString())
                            PodiumSecondaryStat("Earned", member.pointsEarned.toString())
                        }
                        is PointsLeaderboardEntry -> {
                            when (pointsFilter) {
                                PointsFilter.EARNED -> {
                                    PodiumSecondaryStat("Spent", member.pointsSpent.toString())
                                    PodiumSecondaryStat("Avail", member.pointsAvailable.toString())
                                }
                                PointsFilter.SPENT -> {
                                    PodiumSecondaryStat("Earned", member.pointsEarned.toString())
                                    PodiumSecondaryStat("Avail", member.pointsAvailable.toString())
                                }
                                PointsFilter.AVAILABLE -> {
                                    PodiumSecondaryStat("Earned", member.pointsEarned.toString())
                                    PodiumSecondaryStat("Spent", member.pointsSpent.toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumGlitterEffect(color: Color, modifier: Modifier = Modifier.fillMaxSize()) {
    val infiniteTransition = rememberInfiniteTransition(label = "Glitter")
    
    // Drive a continuous time value - slowed down by increasing the duration
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000000, easing = LinearEasing), // 1000 seconds for 1000 units = 1 unit per second
            repeatMode = RepeatMode.Restart
        ),
        label = "GlitterTime"
    )

    Canvas(modifier = modifier) {
        val count = 12 // Reduced count for a cleaner, more premium feel
        repeat(count) { index ->
            // Significantly slowed down speed factor (0.15f to 0.3f)
            // One cycle now takes approx 3-6 seconds
            val speedFactor = 0.15f + (index % 3) * 0.07f
            val particleTime = (time * speedFactor) + (index.toFloat() / count)
            
            val cycle = particleTime.toInt()
            val phase = particleTime % 1f // 0.0 to 1.0
            
            val indRandom = Random(color.hashCode().toLong() + index + (cycle * 31L))
            
            val x = indRandom.nextFloat() * size.width
            val y = indRandom.nextFloat() * size.height
            val radius = (indRandom.nextFloat() * 1.0.dp.toPx()) + 0.5.dp.toPx()
            
            // Smoother fade using a sine-like curve for a more elegant transition
            val alpha = (Math.sin(phase * Math.PI).toFloat()).coerceIn(0f, 1f)
            
            if (alpha > 0.01f) {
                // Main glitter point - softer alpha
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.35f * indRandom.nextFloat()),
                    center = Offset(x, y),
                    radius = radius
                )
                
                // Secondary soft glow
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.12f),
                    center = Offset(x, y),
                    radius = radius * 2.5f // Larger, softer glow
                )
            }
        }
    }
}

@Composable
private fun PodiumSecondaryStat(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 8.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 9.sp
        )
    }
}

@Composable
fun LeaderboardMemberCard(
    member: LeaderboardEntry,
    rank: Int,
    type: LeaderboardType,
    pointsFilter: PointsFilter = PointsFilter.EARNED,
    searchQuery: String = "",
    modifier: Modifier = Modifier
) {
    ElysiumGlassCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                val annotatedName = if (searchQuery.isEmpty()) AnnotatedString(member.username) else highlightSearchText(member.username, searchQuery, MaterialTheme.colorScheme.primary)
                Text(
                    text = annotatedName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        shadow = if (searchQuery.isNotEmpty()) Shadow(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            blurRadius = 8f
                        ) else null
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val subText = when (member) {
                    is AttendanceLeaderboardEntry -> "Kills: ${member.totalKills} • Earned: ${member.pointsEarned}"
                    is PointsLeaderboardEntry -> {
                        when (pointsFilter) {
                            PointsFilter.EARNED -> "Spent: ${member.pointsSpent} • Available: ${member.pointsAvailable}"
                            PointsFilter.SPENT -> "Earned: ${member.pointsEarned} • Available: ${member.pointsAvailable}"
                            PointsFilter.AVAILABLE -> "Earned: ${member.pointsEarned} • Spent: ${member.pointsSpent}"
                        }
                    }
                    else -> ""
                }
                
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            val mainValue = when (member) {
                is AttendanceLeaderboardEntry -> "${member.attendanceRate}%"
                is PointsLeaderboardEntry -> {
                    when (pointsFilter) {
                        PointsFilter.EARNED -> member.pointsEarned.toString()
                        PointsFilter.SPENT -> member.pointsSpent.toString()
                        PointsFilter.AVAILABLE -> member.pointsAvailable.toString()
                    }
                }
                else -> ""
            }

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Text(
                    text = mainValue,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
