package com.boxbox.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxbox.app.data.model.*
import com.boxbox.app.data.repository.BoxBoxRepository
import com.boxbox.app.data.repository.allRecentDriversBySurname
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StandingsViewModel(
    private val repository: BoxBoxRepository = BoxBoxRepository()
) : ViewModel() {

    private val _driverStandings = MutableStateFlow<UiState<List<DriverStanding>>>(UiState.Loading)
    val driverStandings: StateFlow<UiState<List<DriverStanding>>> = _driverStandings

    private val _constructorStandings = MutableStateFlow<UiState<List<ConstructorStanding>>>(UiState.Loading)
    val constructorStandings: StateFlow<UiState<List<ConstructorStanding>>> = _constructorStandings

    // Surname (accent-normalized) -> OpenF1 Driver, merged across recent sessions
    // and, if needed, the previous season too. See RepositoryExtensions.kt for why
    // this is needed instead of a single "latest" session call.
    private val _driverPhotosByName = MutableStateFlow<Map<String, Driver>>(emptyMap())
    val driverPhotosByName: StateFlow<Map<String, Driver>> = _driverPhotosByName

    // Which tab (0 = Drivers, 1 = Constructors) was selected. Lives here instead of as
    // a local remember{} in the composable so that navigating to a Driver/Team detail
    // screen and pressing back returns to the same tab the user was on, rather than
    // resetting to Drivers every time (the composable is recreated on that round trip,
    // but this ViewModel survives as long as Standings stays in the nav back stack).
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    init {
        loadStandings()
        loadOpenF1DriverPhotos()
    }

    fun loadStandings() {
        viewModelScope.launch {
            _driverStandings.value = UiState.Loading
            _constructorStandings.value = UiState.Loading
            try {
                val drivers = repository.getDriverStandings()
                _driverStandings.value = UiState.Success(drivers)
            } catch (e: Exception) {
                _driverStandings.value = UiState.Error(e.message ?: "Error")
            }
            try {
                val constructors = repository.getConstructorStandings()
                _constructorStandings.value = UiState.Success(constructors)
            } catch (e: Exception) {
                _constructorStandings.value = UiState.Error(e.message ?: "Error")
            }
        }
    }

    private fun loadOpenF1DriverPhotos() {
        viewModelScope.launch {
            _driverPhotosByName.value = repository.allRecentDriversBySurname()
        }
    }
}
