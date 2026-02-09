package com.elysium.guild.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.airbnb.lottie.compose.*
import com.elysium.guild.R
import com.elysium.guild.ui.components.*
import com.elysium.guild.utils.Constants
import com.elysium.guild.utils.PreferenceManager
import com.elysium.guild.viewmodel.BossTimersViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BossTimersScreen(
    navController: NavController,
    viewModel: BossTimersViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTime = viewModel.currentTime.collectAsState()
    val useLocalTimezone by preferenceManager.useLocalTimezone.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val pullRefreshState = rememberPullToRefreshState()
    
    val scrollOffset = remember { derivedStateOf { listState.firstVisibleItemScrollOffset.toFloat() + listState.firstVisibleItemIndex * 500f } }

    var previousBossStatuses by remember { mutableStateOf(mapOf<String, String>()) }

    LaunchedEffect(uiState.bosses) {
        val currentStatuses = uiState.bosses.associate { it.bossName to it.status }
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

    LaunchedEffect(uiState.isRefreshing) {
        if (uiState.isRefreshing && viewModel.isHapticEnabled()) {
            triggerHapticFeedback(context, duration = 15)
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        DynamicElysiumBackground(scrollOffset = scrollOffset.value) {
            PullToRefreshBox(
                state = pullRefreshState,
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshTimers() },
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_orb))
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (uiState.error != null && uiState.bosses.isNotEmpty()) {
                        OfflineBanner()
                        Spacer(modifier = Modifier.height(8.dp))
                    }

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ElysiumGlassSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { 
                                viewModel.onSearchQueryChanged(it)
                                coroutineScope.launch { listState.animateScrollToItem(0) }
                            },
                            onClear = { viewModel.onSearchQueryChanged("") },
                            placeholder = "Search bosses...",
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { 
                                viewModel.toggleElysiumTurnFilter()
                                coroutineScope.launch { listState.animateScrollToItem(0) }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = if (uiState.onlyElysiumTurn) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                                    else 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Elysium Turn Filter",
                                tint = if (uiState.onlyElysiumTurn) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val filters = listOf("All", "Ready", "Soon", "Tracking")
                    val displayBossesForCounts = if (uiState.onlyElysiumTurn) {
                        uiState.bosses.filter { it.rotation?.isOurTurn == true }
                    } else {
                        uiState.bosses
                    }

                    val filterCounts = remember(displayBossesForCounts) {
                        mapOf(
                            "All" to displayBossesForCounts.size,
                            "Ready" to displayBossesForCounts.count { it.status == Constants.STATUS_READY || it.status == Constants.STATUS_OVERDUE || (it.timeRemaining ?: 1L) <= 0L },
                            "Soon" to displayBossesForCounts.count {
                                val isReady = it.status == Constants.STATUS_READY || it.status == Constants.STATUS_OVERDUE || (it.timeRemaining ?: 1L) <= 0L
                                !isReady && (it.status == Constants.STATUS_SOON || (it.timeRemaining != null && it.timeRemaining <= Constants.SPAWNING_SOON_THRESHOLD_MS))
                            },
                            "Tracking" to displayBossesForCounts.count {
                                 val isReady = it.status == Constants.STATUS_READY || it.status == Constants.STATUS_OVERDUE || (it.timeRemaining ?: 1L) <= 0L
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
                    
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.isLoading && uiState.bosses.isEmpty()) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(5) { BossTimerShimmerItem() }
                            }
                        } else if (uiState.error != null && uiState.bosses.isEmpty()) {
                            ErrorMessage(message = uiState.error!!, onRetry = { viewModel.refreshTimers() })
                        } else if (uiState.filteredBosses.isEmpty()) {
                            EmptyBossState(
                                query = uiState.searchQuery,
                                isElysiumFilterActive = uiState.onlyElysiumTurn,
                                onClearFilters = {
                                    viewModel.onSearchQueryChanged("")
                                    viewModel.setFilter("All")
                                    if (uiState.onlyElysiumTurn) viewModel.toggleElysiumTurnFilter()
                                }
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items = uiState.filteredBosses,
                                    key = { _, boss -> boss.bossName }
                                ) { index, boss ->
                                    BossTimerCard(
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(300, delayMillis = (index % 10) * 50),
                                            fadeOutSpec = tween(300),
                                            placementSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ),
                                        boss = boss,
                                        currentTime = currentTime,
                                        useLocalTimezone = useLocalTimezone,
                                        searchQuery = uiState.searchQuery
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
fun EmptyBossState(
    query: String,
    isElysiumFilterActive: Boolean = false,
    onClearFilters: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty_search))
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(150.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when {
                query.isNotEmpty() -> "No results for \"$query\""
                isElysiumFilterActive -> "No bosses for Elysium's turn"
                else -> "No bosses tracked"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Try adjusting your filters or search query",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onClearFilters) {
            Text("Clear all filters", fontWeight = FontWeight.Bold)
        }
    }
}

private fun triggerHapticFeedback(context: Context, duration: Long = 50) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(duration)
    }
}
