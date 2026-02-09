package com.elysium.guild.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import com.elysium.guild.R
import com.elysium.guild.ui.components.*
import com.elysium.guild.ui.theme.ElysiumGuildTheme
import com.elysium.guild.utils.PreferenceManager
import com.elysium.guild.viewmodel.EventsViewModel
import com.elysium.guild.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    navController: NavController,
    viewModel: EventsViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTime = viewModel.currentTime.collectAsState()
    val useLocalTimezone by preferenceManager.useLocalTimezone.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    val scrollOffset = remember {
        derivedStateOf {
            listState.firstVisibleItemScrollOffset.toFloat() + listState.firstVisibleItemIndex * 500f
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshEvents(isInitial = true)
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        DynamicElysiumBackground(scrollOffset = scrollOffset.value) {
            PullToRefreshBox(
                state = pullToRefreshState,
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshEvents() },
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
                    
                    if (uiState.error != null && uiState.events.isNotEmpty()) {
                        OfflineBanner()
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Constants.TITLE_GUILD_EVENTS,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = Constants.SUBTITLE_GUILD_EVENTS,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when {
                        uiState.isLoading && uiState.events.isEmpty() -> {
                            LoadingIndicator()
                        }
                        
                        uiState.error != null && uiState.events.isEmpty() -> {
                            ErrorMessage(
                                message = uiState.error ?: "Unknown error",
                                onRetry = { viewModel.refreshEvents() }
                            )
                        }
                        
                        else -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (uiState.events.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 21.dp)
                                            .fillMaxHeight()
                                            .width(1.dp)
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                }

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    itemsIndexed(
                                        items = uiState.events,
                                        key = { _, event -> event.id }
                                    ) { index, event ->
                                        EventCard(
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = tween(300, delayMillis = (index % 10) * 50),
                                                fadeOutSpec = tween(300),
                                                placementSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            ),
                                            event = event,
                                            currentTime = currentTime,
                                            useLocalTimezone = useLocalTimezone,
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
}
