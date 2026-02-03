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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.elysium.guild.ui.components.*
import com.elysium.guild.viewmodel.LeaderboardViewModel
import com.elysium.guild.models.LeaderboardType
import com.elysium.guild.viewmodel.LeaderboardPeriod
import com.elysium.guild.viewmodel.PointsFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    navController: NavController,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.refreshLeaderboard()
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search members...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Type Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setLeaderboardType(LeaderboardType.ATTENDANCE) },
                    label = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Attendance", fontSize = 12.sp, maxLines = 1) } },
                    selected = uiState.leaderboardType == LeaderboardType.ATTENDANCE
                )

                FilterChip(
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setLeaderboardType(LeaderboardType.POINTS) },
                    label = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Points", fontSize = 12.sp, maxLines = 1) } },
                    selected = uiState.leaderboardType == LeaderboardType.POINTS
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Sub-Filter Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.leaderboardType == LeaderboardType.ATTENDANCE) {
                    LeaderboardPeriod.entries.forEach { period ->
                        FilterChip(
                            onClick = { viewModel.setPeriod(period) },
                            label = { Text(period.label, fontSize = 11.sp) },
                            selected = uiState.selectedPeriod == period
                        )
                    }
                } else {
                    PointsFilter.entries.forEach { filter ->
                        FilterChip(
                            onClick = { viewModel.setPointsFilter(filter) },
                            label = { Text(filter.label, fontSize = 11.sp) },
                            selected = uiState.selectedPointsFilter == filter
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                else -> {
                    if (uiState.searchQuery.isEmpty()) {
                        LeaderboardPodium(
                            topThree = uiState.filteredLeaderboard.take(3),
                            leaderboardType = uiState.leaderboardType,
                            pointsFilter = uiState.selectedPointsFilter
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(uiState.filteredLeaderboard.drop(3)) { member ->
                                LeaderboardMemberCard(
                                    member = member,
                                    rank = uiState.filteredLeaderboard.indexOf(member) + 1,
                                    type = uiState.leaderboardType,
                                    pointsFilter = uiState.selectedPointsFilter
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(uiState.filteredLeaderboard) { member ->
                                LeaderboardMemberCard(
                                    member = member,
                                    rank = uiState.filteredLeaderboard.indexOf(member) + 1,
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
