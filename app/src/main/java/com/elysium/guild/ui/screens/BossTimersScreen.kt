package com.elysium.guild.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.elysium.guild.ui.components.*
import com.elysium.guild.utils.Constants
import com.elysium.guild.viewmodel.BossTimersViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BossTimersScreen(
    navController: NavController,
    viewModel: BossTimersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTime = viewModel.currentTime.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val pullRefreshState = rememberPullToRefreshState()
    
    // Track previous boss IDs and statuses to detect changes
    var previousBossStatuses by remember { mutableStateOf(mapOf<String, String>()) }

    LaunchedEffect(uiState.bosses) {
        val currentStatuses = uiState.bosses.associate { it.bossName to it.status }
        
        // If we have previous data, check if any boss changed from ready/soon to tracking
        if (previousBossStatuses.isNotEmpty()) {
            val hasChangedToTracking = uiState.bosses.any { boss ->
                val prevStatus = previousBossStatuses[boss.bossName]
                (prevStatus == Constants.STATUS_READY || prevStatus == Constants.STATUS_SOON) && boss.status == Constants.STATUS_TRACKING
            }
            
            if (hasChangedToTracking) {
                listState.animateScrollToItem(0)
            }
        }
        previousBossStatuses = currentStatuses
    }
    
    // Haptic Feedback Logic
    LaunchedEffect(Unit) {
        viewModel.refreshEvents.collectLatest { shouldScrollToTop ->
            if (viewModel.isHapticEnabled()) {
                triggerHapticFeedback(context)
            }
            if (shouldScrollToTop) {
                listState.animateScrollToItem(0)
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        DynamicElysiumBackground {
            PullToRefreshBox(
                state = pullRefreshState,
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshTimers() },
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullRefreshState,
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
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Centered Header using Centralized Constants
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Constants.TITLE_BOSS_TIMERS,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = Constants.SUBTITLE_BOSS_TIMERS,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        // Keep debug button reachable but out of centered titles
                        IconButton(
                            onClick = { viewModel.testNotification() },
                            modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { 
                            viewModel.onSearchQueryChanged(it)
                            coroutineScope.launch { listState.animateScrollToItem(0) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search bosses...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 14.sp) },
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
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Filter Chips with Counts
                    val filters = listOf("All", "Ready", "Soon", "Tracking")
                    val filterCounts = remember(uiState.bosses) {
                        mapOf(
                            "All" to uiState.bosses.size,
                            "Ready" to uiState.bosses.count { it.status == Constants.STATUS_READY || it.status == Constants.STATUS_OVERDUE || (it.timeRemaining ?: 1) <= 0 },
                            "Soon" to uiState.bosses.count { 
                                val isReady = it.status == Constants.STATUS_READY || it.status == Constants.STATUS_OVERDUE || (it.timeRemaining ?: 1) <= 0
                                !isReady && (it.status == Constants.STATUS_SOON || (it.timeRemaining != null && it.timeRemaining <= Constants.SPAWNING_SOON_THRESHOLD_MS))
                            },
                            "Tracking" to uiState.bosses.count {
                                 val isReady = it.status == Constants.STATUS_READY || it.status == Constants.STATUS_OVERDUE || (it.timeRemaining ?: 1) <= 0
                                 val isSoon = !isReady && (it.status == Constants.STATUS_SOON || (it.timeRemaining != null && it.timeRemaining <= Constants.SPAWNING_SOON_THRESHOLD_MS))
                                 !isReady && !isSoon
                            }
                        )
                    }

                    FilterChipsWithCounts(
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = { filter ->
                            viewModel.setFilter(filter)
                            coroutineScope.launch { listState.animateScrollToItem(0) }
                        },
                        filters = filters,
                        counts = filterCounts
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Boss List
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.isLoading && uiState.bosses.isEmpty()) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(5) { BossTimerShimmerItem() }
                            }
                        } else if (uiState.error != null && uiState.bosses.isEmpty()) {
                            ErrorMessage(message = uiState.error!!, onRetry = { viewModel.refreshTimers() })
                        } else if (uiState.filteredBosses.isEmpty()) {
                            EmptyBossState(query = uiState.searchQuery)
                        } else {
                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.filteredBosses,
                                    key = { it.bossName }
                                ) { boss ->
                                    BossTimerCard(boss = boss, currentTime = currentTime)
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
fun FilterChipsWithCounts(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    filters: List<String>,
    counts: Map<String, Int>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (filter in filters) {
            ElysiumFilterChip(
                modifier = Modifier.weight(1f),
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = filter,
                count = counts[filter]
            )
        }
    }
}

@Composable
fun EmptyBossState(query: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (query.isEmpty()) "No bosses tracked" else "No results for \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Try adjusting your filters or search query",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

private fun triggerHapticFeedback(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(50)
    }
}
