package com.elysium.guild.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elysium.guild.models.*
import com.elysium.guild.utils.Constants
import com.elysium.guild.viewmodel.PointsFilter

@Composable
fun LeaderboardPodium(
    topThree: List<LeaderboardEntry>,
    leaderboardType: LeaderboardType,
    pointsFilter: PointsFilter = PointsFilter.EARNED
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place
        if (topThree.size >= 2) {
            PodiumItem(
                member = topThree[1],
                rank = 2,
                height = 200.dp,
                baseColor = Constants.COLOR_SILVER,
                leaderboardType = leaderboardType,
                pointsFilter = pointsFilter,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // 1st Place
        if (topThree.isNotEmpty()) {
            PodiumItem(
                member = topThree[0],
                rank = 1,
                height = 240.dp,
                baseColor = Constants.COLOR_GOLD,
                leaderboardType = leaderboardType,
                pointsFilter = pointsFilter,
                modifier = Modifier.weight(1.2f)
            )
        }

        // 3rd Place
        if (topThree.size >= 3) {
            PodiumItem(
                member = topThree[2],
                rank = 3,
                height = 180.dp,
                baseColor = Constants.COLOR_BRONZE,
                leaderboardType = leaderboardType,
                pointsFilter = pointsFilter,
                modifier = Modifier.weight(1f)
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
    height: Dp,
    baseColor: Color,
    leaderboardType: LeaderboardType,
    pointsFilter: PointsFilter,
    modifier: Modifier = Modifier
) {
    ElysiumGlassCard(
        modifier = modifier.height(height),
        statusColor = baseColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp).fillMaxWidth()
        ) {
            // Rank Circle
            Surface(
                shape = CircleShape,
                color = baseColor.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp).border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
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

            // Name inside podium
            Text(
                text = member.username,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Stat
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

            // Secondary Stats
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
    pointsFilter: PointsFilter = PointsFilter.EARNED
) {
    ElysiumGlassCard {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
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
                Text(
                    text = member.username,
                    style = MaterialTheme.typography.bodyLarge,
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
            
            // Percentage/Main Stat Circle
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
