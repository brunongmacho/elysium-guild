package com.elysium.guild.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elysium.guild.models.*
import com.elysium.guild.repository.EventsRepository
import com.elysium.guild.utils.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventsRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EventsUiState(
        notificationsEnabled = preferenceManager.eventNotificationsEnabled.value
    ))
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    private val _currentTime = MutableStateFlow(Clock.System.now())
    val currentTime: StateFlow<Instant> = _currentTime.asStateFlow()
    
    private var refreshJob: Job? = null
    
    init {
        refreshEvents(isInitial = true)
        startTicker()
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = Clock.System.now()
                delay(1000L)
            }
        }
    }
    
    fun refreshEvents(isInitial: Boolean = false) {
        if (refreshJob?.isActive == true) return
        
        refreshJob = viewModelScope.launch {
            try {
                if (isInitial || _uiState.value.events.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                } else {
                    _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
                }

                // Add a small delay for better UX on pull-to-refresh
                if (!isInitial) delay(500)

                val events = repository.getEvents()
                val sortedEvents = sortEvents(events, Clock.System.now())

                _uiState.value = _uiState.value.copy(
                    events = sortedEvents,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to refresh events"
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
    }

    private fun sortEvents(events: List<GuildEvent>, now: Instant): List<GuildEvent> {
        return events.sortedWith(compareByDescending<GuildEvent> {
            // Priority 1: Events that are currently running (between start and end time)
            val start = Instant.parse(it.startTime)
            val end = it.endTime?.let { e -> Instant.parse(e) }
            end != null && now >= start && now < end
        }.thenBy {
            // Priority 2: Upcoming events sorted by start time
            Instant.parse(it.startTime)
        })
    }

    fun toggleNotifications() {
        val newValue = !_uiState.value.notificationsEnabled
        preferenceManager.setEventNotificationsEnabled(newValue)
        _uiState.value = _uiState.value.copy(notificationsEnabled = newValue)
    }
    
    fun toggleReminder(event: GuildEvent) {
        // Individual reminder toggle logic
    }
}

data class EventsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val events: List<GuildEvent> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val error: String? = null
)
