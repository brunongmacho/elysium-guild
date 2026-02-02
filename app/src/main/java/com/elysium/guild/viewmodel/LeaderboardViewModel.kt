package com.elysium.guild.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elysium.guild.models.*
import com.elysium.guild.repository.LeaderboardRepository
import com.elysium.guild.repository.BossTimersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LeaderboardPeriod(val value: String?, val label: String) {
    ALL_TIME(null, "All Time"),
    MONTHLY("monthly", "Monthly"),
    WEEKLY("weekly", "Weekly")
}

enum class PointsFilter(val label: String) {
    EARNED("Earned"),
    SPENT("Spent"),
    AVAILABLE("Available")
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: LeaderboardRepository,
    private val bossRepository: BossTimersRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()
    
    init {
        refreshLeaderboard()
        observeBossDataChanges()
    }

    private fun observeBossDataChanges() {
        viewModelScope.launch {
            bossRepository.bossDataChanged.collect {
                refreshLeaderboard()
            }
        }
    }
    
    fun refreshLeaderboard() {
        viewModelScope.launch {
            if (_uiState.value.isLoading || _uiState.value.isRefreshing) return@launch

            val isInitial = _uiState.value.attendanceLeaderboard.isEmpty() && _uiState.value.pointsLeaderboard.isEmpty()
            
            if (isInitial) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            }
            
            try {
                when (_uiState.value.leaderboardType) {
                    LeaderboardType.ATTENDANCE -> {
                        val period = _uiState.value.selectedPeriod.value
                        val leaderboard = repository.getAttendanceLeaderboard(period)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            attendanceLeaderboard = leaderboard
                        )
                    }
                    LeaderboardType.POINTS -> {
                        val leaderboard = repository.getPointsLeaderboard()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            pointsLeaderboard = leaderboard
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to load leaderboard"
                )
            }
        }
    }
    
    fun setLeaderboardType(type: LeaderboardType) {
        if (type != _uiState.value.leaderboardType) {
            _uiState.value = _uiState.value.copy(leaderboardType = type, searchQuery = "")
            refreshLeaderboard()
        }
    }

    fun setPeriod(period: LeaderboardPeriod) {
        if (period != _uiState.value.selectedPeriod) {
            _uiState.value = _uiState.value.copy(
                selectedPeriod = period, 
                searchQuery = ""
            )
            refreshLeaderboard()
        }
    }

    fun setPointsFilter(filter: PointsFilter) {
        _uiState.value = _uiState.value.copy(selectedPointsFilter = filter)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
}

data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val attendanceLeaderboard: List<AttendanceLeaderboardEntry> = emptyList(),
    val pointsLeaderboard: List<PointsLeaderboardEntry> = emptyList(),
    val leaderboardType: LeaderboardType = LeaderboardType.ATTENDANCE,
    val selectedPeriod: LeaderboardPeriod = LeaderboardPeriod.ALL_TIME,
    val selectedPointsFilter: PointsFilter = PointsFilter.EARNED,
    val searchQuery: String = "",
    val error: String? = null
) {
    val filteredLeaderboard: List<LeaderboardEntry>
        get() {
            val list = if (leaderboardType == LeaderboardType.ATTENDANCE) {
                attendanceLeaderboard
            } else {
                when (selectedPointsFilter) {
                    PointsFilter.EARNED -> pointsLeaderboard.sortedByDescending { it.pointsEarned }
                    PointsFilter.SPENT -> pointsLeaderboard.sortedByDescending { it.pointsSpent }
                    PointsFilter.AVAILABLE -> pointsLeaderboard.sortedByDescending { it.pointsAvailable }
                }
            }

            return if (searchQuery.isBlank()) {
                list
            } else {
                list.filter { it.username.contains(searchQuery, ignoreCase = true) }
            }
        }
}
