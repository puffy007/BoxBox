package com.boxbox.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxbox.app.data.model.*
import com.boxbox.app.data.repository.BoxBoxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class HomeViewModel(
    private val repository: BoxBoxRepository = BoxBoxRepository()
) : ViewModel() {

    private val _schedule = MutableStateFlow<UiState<List<Race>>>(UiState.Loading)
    val schedule: StateFlow<UiState<List<Race>>> = _schedule

    private val _nextRace = MutableStateFlow<Race?>(null)
    val nextRace: StateFlow<Race?> = _nextRace

    init {
        loadSchedule()
    }

    fun loadSchedule() {
        viewModelScope.launch {
            _schedule.value = UiState.Loading
            try {
                val races = repository.getCurrentSchedule()
                _schedule.value = UiState.Success(races)
                findNextRace(races)
            } catch (e: Exception) {
                _schedule.value = UiState.Error(e.message ?: "Failed to load schedule")
            }
        }
    }

    private fun findNextRace(races: List<Race>) {
        val now = ZonedDateTime.now()
        val next = races.firstOrNull { race ->
            try {
                val dateStr = "${race.date}T${race.time ?: "00:00:00Z"}"
                val raceTime = ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
                raceTime.isAfter(now)
            } catch (e: Exception) { false }
        }
        _nextRace.value = next
    }
}
