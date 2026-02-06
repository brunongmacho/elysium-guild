package com.elysium.guild.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.elysium.guild.ui.components.*
import com.elysium.guild.viewmodel.LeaderboardViewModel
import com.elysium.guild.models.LeaderboardType
import com.elysium.guild.viewmodel.LeaderboardPeriod
import com.elysium.guild.viewmodel.PointsFilter
import com.elysium.guild.utils.Constants

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LeaderboardScreen(
    navController: NavController,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Adjust padding and spacing based on screen width
    val contentPadding = if (screenWidth < 360.dp) 8.dp else 16.dp
    val headerSpacing = if (screenWidth < 360.dp) 8.dp else 16.dp

    LaunchedEffect(Unit) {
        viewModel.refreshLeaderboard()
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        DynamicElysiumBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = contentPadding)
            ) {
                Spacer(modifier = Modifier.height(headerSpacing))
                
                // Centered Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Constants.TITLE_LEADERBOARD,
                        style = if (screenWidth < 360.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = Constants.SUBTITLE_LEADERBOARD,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        fontSize = if (screenWidth < 360.dp) 10.sp else 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(headerSpacing))

                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search members...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = if (screenWidth < 360.dp) 12.sp else 14.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = if (screenWidth < 360.dp) 12.sp else 14.sp)
                )

                Spacer(modifier = Modifier.height(headerSpacing))
                
                // Type Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElysiumFilterChip(
                        modifier = Modifier.weight(1f),
                        selected = uiState.leaderboardType == LeaderboardType.ATTENDANCE,
                        onClick = { viewModel.setLeaderboardType(LeaderboardType.ATTENDANCE) },
                        label = "Attendance"
                    )

                    ElysiumFilterChip(
                        modifier = Modifier.weight(1f),
                        selected = uiState.leaderboardType == LeaderboardType.POINTS,
                        onClick = { viewModel.setLeaderboardType(LeaderboardType.POINTS) },
                        label = "Points"
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Sub-Filter Selection
                val subFilterScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(subFilterScrollState),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (uiState.leaderboardType == LeaderboardType.ATTENDANCE) {
                        LeaderboardPeriod.entries.forEach { period ->
                            ElysiumFilterChip(
                                selected = uiState.selectedPeriod == period,
                                onClick = { viewModel.setPeriod(period) },
                                label = period.label
                            )
                        }
                    } else {
                        PointsFilter.entries.forEach { filter ->
                            ElysiumFilterChip(
                                selected = uiState.selectedPointsFilter == filter,
                                onClick = { viewModel.setPointsFilter(filter) },
                                label = filter.label
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState.isLoading -> {
                            LoadingIndicator()
                        }

                        uiState.error != null -> {
                            val errorMessage = uiState.error ?: "Unknown error"
                            ErrorMessage(
                                message = errorMessage,
                                onRetry = { viewModel.refreshLeaderboard() }
                            )
                        }

                        uiState.filteredLeaderboard.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (uiState.searchQuery.isEmpty()) "No data available" else "No members found matching \"${uiState.searchQuery}\"",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        else -> {
                            val sortedList = uiState.sortedLeaderboard
                            if (uiState.searchQuery.isEmpty()) {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    item {
                                        LeaderboardPodium(
                                            topThree = sortedList.take(3),
                                            leaderboardType = uiState.leaderboardType,
                                            pointsFilter = uiState.selectedPointsFilter
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }

                                    items(sortedList.drop(3)) { member ->
                                        LeaderboardMemberCard(
                                            member = member,
                                            rank = sortedList.indexOf(member) + 1,
                                            type = uiState.leaderboardType,
                                            pointsFilter = uiState.selectedPointsFilter
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    items(uiState.filteredLeaderboard) { member ->
                                        LeaderboardMemberCard(
                                            member = member,
                                            rank = sortedList.indexOf(member) + 1,
                                            type = uiState.leaderboardType,
                                            pointsFilter = uiState.selectedPointsFilter
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
