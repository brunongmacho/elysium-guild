package com.elysium.guild.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elysium.guild.models.*
import com.elysium.guild.repository.BossTimersRepository
import com.elysium.guild.utils.NotificationHelper
import com.elysium.guild.utils.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@HiltViewModel
class BossTimersViewModel @Inject constructor(
    private val repository: BossTimersRepository,
    private val preferenceManager: PreferenceManager,
    private val notificationHelper: NotificationHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BossTimersUiState(
        notificationsEnabled = preferenceManager.bossNotificationsEnabled.value
    ))
    val uiState: StateFlow<BossTimersUiState> = _uiState.asStateFlow()

    private val _currentTime = MutableStateFlow(Clock.System.now())
    val currentTime: StateFlow<Instant> = _currentTime.asStateFlow()

    private val _refreshEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val refreshEvents: SharedFlow<Boolean> = _refreshEvents.asSharedFlow()
    
    private val refreshedBossIds = mutableSetOf<String>()
    private var refreshJob: Job? = null

    init {
        refreshTimers(isInitial = true)
        startTicker()
        startAutoRefresh()
    }

    fun isHapticEnabled(): Boolean = preferenceManager.hapticEnabled.value

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                val now = Clock.System.now()
                _currentTime.value = now
                checkProximityRefresh(now)
                delay(1000L)
            }
        }
    }

    private fun checkProximityRefresh(now: Instant) {
        val bosses = _uiState.value.bosses
        if (bosses.isEmpty()) return

        bosses.forEach { boss ->
            val spawnTime = boss.nextSpawnTime?.let { try { Instant.parse(it) } catch(e: Exception) { null } } ?: return@forEach
            val diffMs = spawnTime.toEpochMilliseconds() - now.toEpochMilliseconds()
            val refreshKey = "${boss.bossName}_${boss.nextSpawnTime}"

            when {
                diffMs in 1799000..1801000 && !refreshedBossIds.contains("${refreshKey}_30m") -> {
                    refreshedBossIds.add("${refreshKey}_30m")
                    refreshTimers(isBackground = true)
                }
                diffMs in -2000..0 && !refreshedBossIds.contains("${refreshKey}_spawned") -> {
                    refreshedBossIds.add("${refreshKey}_spawned")
                    refreshTimers(isBackground = true)
                }
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                val hasReadyBoss = _uiState.value.bosses.any { 
                    it.status == "ready" || it.status == "overdue" || (it.timeRemaining ?: 1) <= 0 
                }
                val nextDelay = if (hasReadyBoss) 30_000L else 300_000L
                delay(nextDelay)
                refreshTimers(isBackground = true)
                if (refreshedBossIds.size > 100) refreshedBossIds.clear()
            }
        }
    }
    
    fun refreshTimers(isBackground: Boolean = false, isInitial: Boolean = false) {
        if (refreshJob?.isActive == true && !isBackground) return
        
        refreshJob = viewModelScope.launch {
            if (!isBackground) {
                if (isInitial || _uiState.value.bosses.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                } else {
                    _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
                }
            }
            
            try {
                if (!isBackground && !isInitial) delay(500)
                
                withTimeout(10000) { // 10 seconds timeout
                    val rawBosses = repository.getBossTimers()
                    val bosses = recalculateTimeRemaining(rawBosses)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        bosses = bosses,
                        filteredBosses = applyFilter(
                            bosses,
                            _uiState.value.selectedFilter,
                            _uiState.value.searchQuery,
                            _uiState.value.onlyElysiumTurn
                        )
                    )
                }
                // Emit true if this was a foreground/manual refresh, suggesting a scroll to top
                _refreshEvents.tryEmit(!isBackground)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to refresh timers"
                )
            }
        }
    }

    fun testNotification() {
        notificationHelper.showBossNotification("Test Boss", 10)
    }
    
    fun setFilter(filter: String) {
        val currentBosses = _uiState.value.bosses
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredBosses = applyFilter(
                currentBosses,
                filter,
                _uiState.value.searchQuery,
                _uiState.value.onlyElysiumTurn
            )
        )
    }

    fun onSearchQueryChanged(query: String) {
        val currentBosses = _uiState.value.bosses
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredBosses = applyFilter(
                currentBosses,
                _uiState.value.selectedFilter,
                query,
                _uiState.value.onlyElysiumTurn
            )
        )
    }

    fun toggleElysiumTurnFilter() {
        val next = !_uiState.value.onlyElysiumTurn
        val currentBosses = _uiState.value.bosses
        _uiState.value = _uiState.value.copy(
            onlyElysiumTurn = next,
            filteredBosses = applyFilter(
                currentBosses,
                _uiState.value.selectedFilter,
                _uiState.value.searchQuery,
                next
            )
        )
    }

    private fun recalculateTimeRemaining(bosses: List<BossTimer>): List<BossTimer> {
        val now = Clock.System.now()
        return bosses.map { boss ->
            val nextSpawn = boss.nextSpawnTime?.let {
                try { Instant.parse(it) } catch (e: Exception) { null }
            }
            val newTimeRemaining = if (nextSpawn != null) (nextSpawn - now).inWholeMilliseconds else boss.timeRemaining
            val updatedStatus = if (newTimeRemaining != null) {
                when {
                    newTimeRemaining <= 0 -> "ready"
                    newTimeRemaining <= 30 * 60 * 1000L -> "soon"
                    else -> "tracking"
                }
            } else boss.status
            boss.copy(timeRemaining = newTimeRemaining, status = updatedStatus)
        }
    }
    
    private fun applyFilter(
        bosses: List<BossTimer>,
        filter: String,
        searchQuery: String,
        onlyElysiumTurn: Boolean
    ): List<BossTimer> {
        val thirtyMinutesMs = 30 * 60 * 1000L

        var result = when (filter) {
            "All" -> bosses
            "Ready" -> bosses.filter { it.status == "ready" || it.status == "overdue" || (it.timeRemaining ?: 1) <= 0 }
            "Soon" -> bosses.filter { 
                val isSpawned = it.status == "ready" || it.status == "overdue" || (it.timeRemaining ?: 1) <= 0
                !isSpawned && (it.status == "soon" || (it.timeRemaining != null && it.timeRemaining <= thirtyMinutesMs))
            }
            "Tracking" -> bosses.filter { 
                val isSpawned = it.status == "ready" || it.status == "overdue" || (it.timeRemaining ?: 1) <= 0
                val isSoon = it.status == "soon" || (it.timeRemaining != null && it.timeRemaining <= thirtyMinutesMs)
                !isSpawned && !isSoon
            }
            else -> bosses
        }

        if (onlyElysiumTurn) {
            result = result.filter { it.rotation?.isOurTurn == true }
        }

        return if (searchQuery.isBlank()) result else result.filter { it.bossName.contains(searchQuery, ignoreCase = true) }
    }
}

data class BossTimersUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val bosses: List<BossTimer> = emptyList(),
    val filteredBosses: List<BossTimer> = emptyList(),
    val selectedFilter: String = "All",
    val searchQuery: String = "",
    val onlyElysiumTurn: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val error: String? = null
)
