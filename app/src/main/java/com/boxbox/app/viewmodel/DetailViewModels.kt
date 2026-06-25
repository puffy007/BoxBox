package com.boxbox.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxbox.app.data.model.*
import com.boxbox.app.data.repository.BoxBoxRepository
import com.boxbox.app.data.repository.findDriverByName
import com.boxbox.app.data.repository.normalizeForMatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DriverDetailData(
    val standing: DriverStanding,
    val openF1Driver: Driver?
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
                _state.value = UiState.Success(DriverDetailData(standing, openF1Driver))
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Error loading driver")
            }
        }
    }
}

data class TeamDetailData(
    val standing: ConstructorStanding,
    val drivers: List<Driver>
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

                // Find this team's drivers via Jolpica's driver standings, not by trying
                // to string-match OpenF1's team_name (which can lag behind a team's current
                // season name - e.g. "RB" vs "Racing Bulls" - and caused the driver lineup
                // to come back empty for that team). Jolpica's own Constructors list per
                // driver is reliable for "who races for whom this season".
                val driverStandings = repository.getDriverStandings()
                val teamKey = normalizeForMatch(standing.Constructor.constructorId)
                val teamNameKey = normalizeForMatch(standing.Constructor.name)

                val teamDriverEntries = driverStandings.filter { ds ->
                    ds.Constructors.any { c ->
                        normalizeForMatch(c.constructorId) == teamKey ||
                                normalizeForMatch(c.name) == teamNameKey
                    }
                }

                // For each Jolpica driver on this team, resolve an OpenF1 record (for the
                // photo/number/code) by name. If OpenF1 has nothing for a given driver,
                // still show them using Jolpica's name so the lineup is never empty for a
                // team that Jolpica confirms has drivers.
                val drivers = teamDriverEntries.map { ds ->
                    repository.findDriverByName(ds.Driver.familyName, ds.Driver.givenName)
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
                }

                _state.value = UiState.Success(TeamDetailData(standing, drivers))
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Error loading team")
            }
        }
    }
}
