package com.boxbox.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxbox.app.data.model.*
import com.boxbox.app.data.repository.BoxBoxRepository
import com.boxbox.app.data.repository.findDriverByName
import com.boxbox.app.data.repository.getDriverSeasonSummary
import com.boxbox.app.data.repository.normalizeForMatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DriverDetailData(
    val standing: DriverStanding,
    val openF1Driver: Driver?,
    val seasonSummary: DriverSeasonSummary? = null
)

class DriverDetailViewModel(
    private val repository: BoxBoxRepository = BoxBoxRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<DriverDetailData>>(UiState.Loading)
    val state: StateFlow<UiState<DriverDetailData>> = _state

    fun load(driverId: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val standings = repository.getDriverStandings()
                val standing = standings.firstOrNull { it.Driver.driverId == driverId }
                if (standing == null) {
                    _state.value = UiState.Error("Driver not found")
                    return@launch
                }
                val openF1Driver = repository.findDriverByName(
                    standing.Driver.familyName, standing.Driver.givenName
                )
                // Show the core data immediately, then fill in season summary once it
                // resolves - races/wins/podiums require an extra API call per driver,
                // so we don't want to block the whole screen on it.
                _state.value = UiState.Success(DriverDetailData(standing, openF1Driver, null))

                val summary = repository.getDriverSeasonSummary(driverId)
                _state.value = UiState.Success(DriverDetailData(standing, openF1Driver, summary))
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Error loading driver")
            }
        }
    }
}

data class TeamDetailData(
    val standing: ConstructorStanding,
    val drivers: List<TeamLineupDriver>
)

data class TeamLineupDriver(
    val driverId: String,
    val driver: Driver
)

class TeamDetailViewModel(
    private val repository: BoxBoxRepository = BoxBoxRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TeamDetailData>>(UiState.Loading)
    val state: StateFlow<UiState<TeamDetailData>> = _state

    fun load(constructorId: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val constructorStandings = repository.getConstructorStandings()
                val standing = constructorStandings.firstOrNull { it.Constructor.constructorId == constructorId }
                if (standing == null) {
                    _state.value = UiState.Error("Team not found")
                    return@launch
                }

                val driverStandings = repository.getDriverStandings()
                val teamKey = normalizeForMatch(standing.Constructor.constructorId)
                val teamNameKey = normalizeForMatch(standing.Constructor.name)

                val teamDriverEntries = driverStandings.filter { ds ->
                    ds.Constructors.any { c ->
                        normalizeForMatch(c.constructorId) == teamKey ||
                                normalizeForMatch(c.name) == teamNameKey
                    }
                }

                val drivers = teamDriverEntries.map { ds ->
                    val openF1Driver = repository.findDriverByName(ds.Driver.familyName, ds.Driver.givenName)
                        ?: Driver(
                            driver_number = ds.Driver.permanentNumber.toIntOrNull() ?: 0,
                            broadcast_name = "${ds.Driver.givenName.take(1)} ${ds.Driver.familyName}".uppercase(),
                            full_name = "${ds.Driver.givenName} ${ds.Driver.familyName}",
                            name_acronym = ds.Driver.code.ifEmpty { ds.Driver.familyName.take(3).uppercase() },
                            team_name = standing.Constructor.name,
                            team_colour = "",
                            country_code = ds.Driver.nationality,
                            headshot_url = "",
                            session_key = 0
                        )
                    TeamLineupDriver(driverId = ds.Driver.driverId, driver = openF1Driver)
                }

                _state.value = UiState.Success(TeamDetailData(standing, drivers))
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Error loading team")
            }
        }
    }
}
