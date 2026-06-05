package com.boxbox.app.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boxbox.app.data.model.*
import com.boxbox.app.data.repository.BoxBoxRepository
import com.boxbox.app.ui.*
import com.boxbox.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ResultsViewModel(
    private val repository: BoxBoxRepository = BoxBoxRepository()
) : ViewModel() {
    private val _races = MutableStateFlow<UiState<List<Race>>>(UiState.Loading)
    val races: StateFlow<UiState<List<Race>>> = _races

    init { load() }

    fun load() {
        viewModelScope.launch {
            _races.value = UiState.Loading
            try {
                val result = repository.getCurrentSchedule()
                val past = result.filter { race ->
                    try {
                        val d = java.time.LocalDate.parse(race.date)
                        d.isBefore(java.time.LocalDate.now())
                    } catch (e: Exception) { false }
                }.reversed()
                _races.value = UiState.Success(past)
            } catch (e: Exception) {
                _races.value = UiState.Error(e.message ?: "Error")
            }
        }
    }
}

@Composable
fun ResultsScreen(vm: ResultsViewModel = viewModel()) {
    val state by vm.races.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(F1Black)
    ) {
        BoxBoxTopBar(title = "RESULTS")

        when (val s = state) {
            is UiState.Loading -> LoadingScreen()
            is UiState.Error -> ErrorScreen(s.message) { vm.load() }
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No past races yet", color = F1LightGray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(s.data) { race -> ResultRaceCard(race) }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultRaceCard(race: Race) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = F1DarkGray,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Round ${race.round}",
                        color = F1Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(race.raceName, color = F1White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${race.Circuit.Location.locality}, ${race.Circuit.Location.country}",
                        color = F1LightGray,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = F1MidGray
                ) {
                    Text(
                        race.date,
                        color = F1LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
