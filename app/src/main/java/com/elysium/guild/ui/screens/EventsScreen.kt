package com.elysium.guild.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.elysium.guild.ui.components.*
import com.elysium.guild.ui.theme.ElysiumGuildTheme
import com.elysium.guild.viewmodel.EventsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    navController: NavController,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTime = viewModel.currentTime.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    
    LaunchedEffect(Unit) {
        viewModel.refreshEvents(isInitial = true)
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        DynamicElysiumBackground {
            PullToRefreshBox(
                state = pullToRefreshState,
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshEvents() },
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = uiState.isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Guild Events",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when {
                        uiState.isLoading && uiState.events.isEmpty() -> {
                            LoadingIndicator()
                        }
                        
                        uiState.error != null && uiState.events.isEmpty() -> {
                            val errorMessage = uiState.error ?: "Unknown error"
                            ErrorMessage(
                                message = errorMessage,
                                onRetry = { viewModel.refreshEvents() }
                            )
                        }
                        
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.events,
                                    key = { it.id }
                                ) { event ->
                                    EventCard(
                                        event = event,
                                        currentTime = currentTime,
                                        onReminderClick = { viewModel.toggleReminder(event) }
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

@Preview
@Composable
fun EventsScreenPreview() {
    ElysiumGuildTheme {
        // EventsScreen(navController = NavController())
    }
}
