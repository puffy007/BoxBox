package com.boxbox.app.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
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

    private val _selectedRace = MutableStateFlow<Race?>(null)
    val selectedRace: StateFlow<Race?> = _selectedRace

    private val _raceControlMessages = MutableStateFlow<UiState<List<RaceControlMessage>>>(UiState.Loading)
    val raceControlMessages: StateFlow<UiState<List<RaceControlMessage>>> = _raceControlMessages

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions

    init { load() }

    fun load() {
        viewModelScope.launch {
            _races.value = UiState.Loading
            try {
                val result = repository.getCurrentSchedule()
                val past = result.filter { race ->
                    try {
                        val d = java.time.LocalDate.parse(race.date)
                        d.isBefore(java.time.LocalDate.now()) || d.isEqual(java.time.LocalDate.now())
                    } catch (e: Exception) { false }
                }.reversed()
                _races.value = UiState.Success(past)
            } catch (e: Exception) {
                _races.value = UiState.Error(e.message ?: "Error loading results")
            }
        }
    }

    fun selectRace(race: Race) {
        _selectedRace.value = race
        loadRaceReplay(race)
    }

    fun clearSelection() {
        _selectedRace.value = null
        _raceControlMessages.value = UiState.Loading
    }

    private fun loadRaceReplay(race: Race) {
        viewModelScope.launch {
            _raceControlMessages.value = UiState.Loading
            _sessions.value = emptyList()
            try {
                val allSessions = repository.getSessions(race.season.toIntOrNull() ?: 2025)
                val raceSessions = allSessions.filter { session ->
                    session.circuit_short_name.contains(
                        race.Circuit.circuitId.replace("_", " "), ignoreCase = true
                    ) || session.location.contains(
                        race.Circuit.Location.locality, ignoreCase = true
                    )
                }
                _sessions.value = raceSessions

                val raceSession = raceSessions.firstOrNull {
                    it.session_type == "Race"
                } ?: raceSessions.lastOrNull()

                if (raceSession != null) {
                    val messages = repository.getRaceControlForSession(raceSession.session_key)
                    _raceControlMessages.value = UiState.Success(
                        messages.sortedByDescending { it.date }
                    )
                } else {
                    _raceControlMessages.value = UiState.Error("No session data found for this race")
                }
            } catch (e: Exception) {
                _raceControlMessages.value = UiState.Error(e.message ?: "Error loading replay")
            }
        }
    }
}

@Composable
fun ResultsScreen(vm: ResultsViewModel = viewModel()) {
    val state by vm.races.collectAsState()
    val selectedRace by vm.selectedRace.collectAsState()

    if (selectedRace != null) {
        RaceDetailScreen(vm = vm, race = selectedRace!!)
    } else {
        ResultsListScreen(state = state, onRaceClick = { vm.selectRace(it) }, onRetry = { vm.load() })
    }
}

@Composable
fun ResultsListScreen(
    state: UiState<List<Race>>,
    onRaceClick: (Race) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        BoxBoxTopBar(title = "RESULTS")
        when (state) {
            is UiState.Loading -> LoadingScreen()
            is UiState.Error -> ErrorScreen(state.message, onRetry)
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No past races yet", color = AppColors.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.data) { race ->
                            ResultRaceCard(race = race, onClick = { onRaceClick(race) })
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultRaceCard(race: Race, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppColors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Round ${race.round}",
                    color = AppColors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    race.raceName,
                    color = AppColors.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${race.Circuit.Location.locality}, ${race.Circuit.Location.country}",
                    color = AppColors.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    race.date,
                    color = AppColors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppColors.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        "▶ REPLAY",
                        color = AppColors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AppColors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun RaceDetailScreen(vm: ResultsViewModel, race: Race) {
    val messagesState by vm.raceControlMessages.collectAsState()
    val sessions by vm.sessions.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Race Control", "Sessions")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        Surface(color = AppColors.primary, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.clearSelection() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppColors.onPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        race.raceName,
                        color = AppColors.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${race.Circuit.Location.locality} · ${race.date}",
                        color = AppColors.onPrimary.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = AppColors.onPrimary.copy(alpha = 0.2f)
                ) {
                    Text(
                        "▶ REPLAY",
                        color = AppColors.onPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AppColors.background,
            contentColor = AppColors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AppColors.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontSize = 12.sp,
                            color = if (selectedTab == index) AppColors.primary else AppColors.onSurfaceVariant
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> RaceControlReplayTab(messagesState)
            1 -> SessionsTab(sessions)
        }
    }
}

@Composable
fun RaceControlReplayTab(state: UiState<List<RaceControlMessage>>) {
    when (state) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Error -> Box(
            Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️", fontSize = 32.sp)
                Spacer(Modifier.height(8.dp))
                Text(state.message, color = AppColors.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "OpenF1 has data from 2023 onwards",
                    color = AppColors.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No race control messages found", color = AppColors.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AppColors.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("▶", color = AppColors.primary, fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Race Replay",
                                        color = AppColors.onBackground,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${state.data.size} race control events",
                                        color = AppColors.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    items(state.data) { msg ->
                        ReplayRaceControlCard(msg)
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun ReplayRaceControlCard(msg: RaceControlMessage) {
    val borderColor = when (msg.flag) {
        "GREEN" -> F1Green
        "YELLOW", "DOUBLE_YELLOW" -> F1Yellow
        "RED" -> Color(0xFFFF4444)
        "SAFETY_CAR" -> F1Orange
        "VIRTUAL_SAFETY_CAR" -> F1Yellow
        "CHEQUERED" -> AppColors.onBackground
        else -> when {
            msg.message.contains("PENALTY", ignoreCase = true) -> F1Purple
            msg.message.contains("SAFETY CAR", ignoreCase = true) -> F1Orange
            else -> AppColors.onSurfaceVariant
        }
    }

    val emoji = when (msg.flag) {
        "GREEN" -> "🟢"
        "YELLOW", "DOUBLE_YELLOW" -> "🟡"
        "RED" -> "🔴"
        "SAFETY_CAR" -> "🚗"
        "VIRTUAL_SAFETY_CAR" -> "🟡"
        "CHEQUERED" -> "🏁"
        else -> when {
            msg.message.contains("PENALTY", ignoreCase = true) -> "⚖️"
            msg.message.contains("DRS", ignoreCase = true) -> "↗️"
            else -> "📻"
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AppColors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(0.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 52.dp)
                    .background(
                        color = borderColor,
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    )
            )
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 18.sp, modifier = Modifier.width(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        msg.message,
                        color = AppColors.onBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        buildString {
                            msg.lap_number?.let { append("Lap $it · ") }
                            append(msg.date.take(19).replace("T", " "))
                        },
                        color = AppColors.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SessionsTab(sessions: List<Session>) {
    if (sessions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📭", fontSize = 32.sp)
                Spacer(Modifier.height(8.dp))
                Text("No session data available", color = AppColors.onSurfaceVariant)
                Text("OpenF1 covers 2023+", color = AppColors.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sessions) { session ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AppColors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val emoji = when (session.session_type) {
                        "Race" -> "🏁"
                        "Qualifying" -> "⏱️"
                        "Practice" -> "🔧"
                        "Sprint" -> "⚡"
                        else -> "📋"
                    }
                    Text(emoji, fontSize = 20.sp, modifier = Modifier.width(32.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            session.session_name,
                            color = AppColors.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            session.date_start.take(16).replace("T", " "),
                            color = AppColors.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    if (session.session_type == "Race") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AppColors.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "RACE",
                                color = AppColors.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

private fun Modifier.tabIndicatorOffset(tabPosition: androidx.compose.material3.TabPosition): Modifier = this
