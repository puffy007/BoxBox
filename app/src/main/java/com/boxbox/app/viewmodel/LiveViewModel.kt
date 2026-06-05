package com.boxbox.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxbox.app.data.model.*
import com.boxbox.app.data.repository.BoxBoxRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LiveViewModel(
    private val repository: BoxBoxRepository = BoxBoxRepository()
) : ViewModel() {

    private val _positions = MutableStateFlow<List<Position>>(emptyList())
    val positions: StateFlow<List<Position>> = _positions

    private val _raceControl = MutableStateFlow<List<RaceControlMessage>>(emptyList())
    val raceControl: StateFlow<List<RaceControlMessage>> = _raceControl

    private val _stints = MutableStateFlow<List<Stint>>(emptyList())
    val stints: StateFlow<List<Stint>> = _stints

    private val _intervals = MutableStateFlow<List<Interval>>(emptyList())
    val intervals: StateFlow<List<Interval>> = _intervals

    private val _drivers = MutableStateFlow<List<Driver>>(emptyList())
    val drivers: StateFlow<List<Driver>> = _drivers

    private val _laps = MutableStateFlow<List<Lap>>(emptyList())
    val laps: StateFlow<List<Lap>> = _laps

    private val _isLive = MutableStateFlow(false)
    val isLive: StateFlow<Boolean> = _isLive

    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var pollingJob: Job? = null

    init {
        checkLiveSession()
    }

    private fun checkLiveSession() {
        viewModelScope.launch {
            try {
                val sessions = repository.getSessions(2025)
                val now = System.currentTimeMillis()

                val liveSession = sessions.firstOrNull { session ->
                    try {
                        val start = java.time.ZonedDateTime.parse(session.date_start).toInstant().toEpochMilli()
                        val end = java.time.ZonedDateTime.parse(session.date_end).toInstant().toEpochMilli()
                        now in start..end
                    } catch (e: Exception) { false }
                }

                _isLive.value = liveSession != null
                _currentSession.value = liveSession

                if (liveSession != null) {
                    loadDrivers()
                    startPolling()
                } else {
                    val lastSession = sessions.lastOrNull { session ->
                        try {
                            val end = java.time.ZonedDateTime.parse(session.date_end).toInstant().toEpochMilli()
                            end < now && session.session_type == "Race"
                        } catch (e: Exception) { false }
                    }
                    lastSession?.let {
                        _currentSession.value = it
                        loadReplay(it.session_key)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun loadDrivers() {
        viewModelScope.launch {
            try {
                _drivers.value = repository.getDrivers("latest")
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchLiveData()
                delay(3000L)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    private suspend fun fetchLiveData() {
        // Use coroutineScope to allow using async correctly
        coroutineScope {
            try {
                val positionsDeferred = async { repository.getLivePositions() }
                val raceControlDeferred = async { repository.getLiveRaceControl() }
                val stintsDeferred = async { repository.getLiveStints() }
                val intervalsDeferred = async { repository.getLiveIntervals() }
                val lapsDeferred = async { repository.getLiveLaps() }

                // Wait for data and process
                val allPositions = positionsDeferred.await()
                val latestPositions = allPositions
                    .groupBy { it.driver_number } // Changed from driver_number
                    .mapValues { (_, posList) -> posList.maxByOrNull { it.date } }
                    .values.filterNotNull()
                    .sortedBy { it.position }
                _positions.value = latestPositions

                _raceControl.value = raceControlDeferred.await()
                    .sortedByDescending { it.date }

                _stints.value = stintsDeferred.await()
                _intervals.value = intervalsDeferred.await()

                val allLaps = lapsDeferred.await()
                val latestLaps = allLaps
                    .groupBy { it.driver_number } // Changed from driver_number
                    .mapValues { (_, lapList) -> lapList.maxByOrNull { it.lap_number } } // Changed from lap_number
                    .values.filterNotNull()
                _laps.value = latestLaps
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun loadReplay(sessionKey: Int) {
        viewModelScope.launch {
            try {
                val rc = repository.getRaceControlForSession(sessionKey)
                _raceControl.value = rc.sortedByDescending { it.date }
                val pos = repository.getPositionsForSession(sessionKey)
                val latestPositions = pos
                    .groupBy { it.driver_number } // Changed from driver_number
                    .mapValues { (_, posList) -> posList.maxByOrNull { it.date } }
                    .values.filterNotNull()
                    .sortedBy { it.position }
                _positions.value = latestPositions
                _drivers.value = repository.getDrivers(sessionKey.toString())
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}