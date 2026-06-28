package com.boxbox.app.ui.results

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.boxbox.app.data.model.*
import com.boxbox.app.data.repository.BoxBoxRepository
import com.boxbox.app.data.repository.getRaceResultsTable
import com.boxbox.app.data.repository.getRaceTopThree
import com.boxbox.app.data.repository.normalizeForMatch
import com.boxbox.app.ui.*
import com.boxbox.app.ui.standings.driverHighResPhotos
import com.boxbox.app.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class RaceWithTrack(
    val race: Race,
    val meeting: Meeting?,
    val isPast: Boolean
)

data class TopThreeEntry(
    val position: String,
    val driverCode: String,
    val driverFamilyName: String,
    val gapOrTime: String,
    val points: String = ""
)

/**
 * Maps a Jolpica circuitId to words we'd expect in OpenF1's meeting.location or
 * meeting.circuit_short_name for that same track, for circuits where the city name
 * Jolpica gives (e.g. "Abu Dhabi") doesn't appear anywhere in OpenF1's naming for that
 * meeting (which uses "Yas Marina" - the circuit name, not the city). A small explicit
 * list covering known mismatches, since track/city naming conventions differ
 * unpredictably from one circuit to another.
 */
private val circuitIdFallbackHints: Map<String, List<String>> = mapOf(
    "yas_marina" to listOf("yas", "yasmarina", "abudhabi"),
    "albert_park" to listOf("albertpark", "melbourne"),
    "marina_bay" to listOf("marinabay", "singapore"),
    "americas" to listOf("austin", "americas", "cota"),
    "rodriguez" to listOf("mexico", "rodriguez"),
    "interlagos" to listOf("interlagos", "saopaulo"),
    "vegas" to listOf("lasvegas", "vegas"),
    "losail" to listOf("losail", "lusail", "qatar"),
    "red_bull_ring" to listOf("redbullring", "spielberg"),
    "villeneuve" to listOf("villeneuve", "montreal"),
    "catalunya" to listOf("catalunya", "barcelona"),
    "madring" to listOf("madring", "madrid")
)

/**
 * Finds the OpenF1 meeting for a given Jolpica race. Tries the existing city-name match
 * first (works for most circuits), then falls back to circuitId-based hints for circuits
 * where OpenF1's location/circuit_short_name uses the track name rather than the city
 * name Jolpica provides - this is what was leaving some circuits with no meeting match,
 * and therefore no circuit_image, even though OpenF1 actually has the data.
 */
private fun findMeetingForRace(race: Race, meetings: List<Meeting>): Meeting? {
    val cityMatch = meetings.firstOrNull { m ->
        normalizeForMatch(m.location).contains(normalizeForMatch(race.Circuit.Location.locality)) ||
                normalizeForMatch(race.Circuit.Location.locality).contains(normalizeForMatch(m.location))
    }
    if (cityMatch != null) return cityMatch

    val hints = circuitIdFallbackHints[race.Circuit.circuitId] ?: return null
    return meetings.firstOrNull { m ->
        val normalizedLocation = normalizeForMatch(m.location)
        val normalizedCircuitShortName = normalizeForMatch(m.circuit_short_name)
        hints.any { hint ->
            normalizedLocation.contains(hint) || normalizedCircuitShortName.contains(hint)
        }
    }
}

/**
 * OpenF1's circuit_image PNGs aren't visually consistent: most use a track line that's
 * clearly lighter than its surrounding border, which reads fine on our dark cards.
 * Monaco and Hungary (and possibly others) ship with darker/lower-contrast tones instead -
 * fine on a light background, but hard to see on our dark cards. We only touch these
 * specific circuits, leaving every other circuit's image completely untouched.
 */
private val whitenCircuitIds = setOf("monaco", "hungaroring")

/**
 * Forces every visible (non-transparent) pixel of the image to pure white, regardless
 * of its original colour - using BlendMode.SrcIn, which replaces the colour of every
 * opaque/semi-opaque pixel with the filter colour while leaving fully transparent pixels
 * untouched. This only depends on the image's alpha channel (its shape/silhouette), not
 * on its original RGB values, so it can't be thrown off by a dark, low-contrast, or
 * oddly-tinted source colour the way a ColorMatrix multiply/invert can. The whole track
 * shape - thick border and thin line alike - becomes solid white, matching how the
 * other circuits' assets already look on our dark cards.
 */
private val whitenColorFilter: ColorFilter = ColorFilter.tint(Color.White, BlendMode.SrcIn)

/**
 * Returns the whiten filter only when both (a) this circuit is known to need it and
 * (b) we're in dark mode. In light mode these assets already render correctly as-is -
 * this filter must never touch light mode, only compensate for the dark-card case.
 */
private fun circuitImageColorFilterFor(circuitId: String, isDarkTheme: Boolean): ColorFilter? {
    return if (isDarkTheme && circuitId in whitenCircuitIds) whitenColorFilter else null
}

class ResultsViewModel(
    private val repository: BoxBoxRepository = BoxBoxRepository()
) : ViewModel() {

    private val _races = MutableStateFlow<UiState<List<RaceWithTrack>>>(UiState.Loading)
    val races: StateFlow<UiState<List<RaceWithTrack>>> = _races

    private val _selectedRace = MutableStateFlow<RaceWithTrack?>(null)
    val selectedRace: StateFlow<RaceWithTrack?> = _selectedRace

    private val _topThreeByRound = MutableStateFlow<Map<String, List<TopThreeEntry>>>(emptyMap())
    val topThreeByRound: StateFlow<Map<String, List<TopThreeEntry>>> = _topThreeByRound

    private val _detailResultsTable = MutableStateFlow<UiState<List<TopThreeEntry>>>(UiState.Loading)
    val detailResultsTable: StateFlow<UiState<List<TopThreeEntry>>> = _detailResultsTable

    init { load() }

    fun load() {
        viewModelScope.launch {
            _races.value = UiState.Loading
            try {
                val schedule = repository.getCurrentSchedule()
                val meetings = runCatching {
                    val year = schedule.firstOrNull()?.season?.toIntOrNull() ?: LocalDate.now().year
                    repository.getMeetings(year)
                }.getOrNull().orEmpty()

                val today = LocalDate.now()
                val combined = schedule.map { race ->
                    val isPast = runCatching { LocalDate.parse(race.date).isBefore(today) }.getOrDefault(false)
                    val meeting = findMeetingForRace(race, meetings)
                    RaceWithTrack(race, meeting, isPast)
                }
                _races.value = UiState.Success(combined)

                loadTopThreeForPastRaces(combined.filter { it.isPast })
            } catch (e: Exception) {
                _races.value = UiState.Error(e.message ?: "Error loading results")
            }
        }
    }

    private fun loadTopThreeForPastRaces(pastRaces: List<RaceWithTrack>) {
        viewModelScope.launch {
            val result = mutableMapOf<String, List<TopThreeEntry>>()
            pastRaces.chunked(5).forEach { batch ->
                val entries = batch.map { rwt ->
                    async {
                        rwt.race.round to runCatching {
                            repository.getRaceTopThree(rwt.race.season, rwt.race.round)
                        }.getOrNull().orEmpty()
                    }
                }
                entries.forEach { deferred ->
                    val (round, topThree) = deferred.await()
                    if (topThree.isNotEmpty()) result[round] = topThree
                }
                _topThreeByRound.value = result.toMap()
            }
        }
    }

    fun selectRace(raceWithTrack: RaceWithTrack) {
        _selectedRace.value = raceWithTrack
        if (raceWithTrack.isPast) {
            loadDetailResultsTable(raceWithTrack.race.season, raceWithTrack.race.round)
        }
    }

    fun clearSelection() {
        _selectedRace.value = null
        _detailResultsTable.value = UiState.Loading
    }

    private fun loadDetailResultsTable(season: String, round: String) {
        viewModelScope.launch {
            _detailResultsTable.value = UiState.Loading
            try {
                val table = repository.getRaceResultsTable(season, round, limit = 22)
                _detailResultsTable.value = if (table.isNotEmpty()) {
                    UiState.Success(table)
                } else {
                    UiState.Error("Results not available yet")
                }
            } catch (e: Exception) {
                _detailResultsTable.value = UiState.Error(e.message ?: "Error loading results")
            }
        }
    }
}

@Composable
fun ResultsScreen(vm: ResultsViewModel = viewModel()) {
    val state by vm.races.collectAsState()
    val selectedRace by vm.selectedRace.collectAsState()
    val topThreeByRound by vm.topThreeByRound.collectAsState()
    val detailResultsTable by vm.detailResultsTable.collectAsState()

    if (selectedRace != null) {
        RaceDetailScreen(
            raceWithTrack = selectedRace!!,
            resultsTable = detailResultsTable,
            onBack = { vm.clearSelection() }
        )
    } else {
        ResultsListScreen(
            state = state,
            topThreeByRound = topThreeByRound,
            onRaceClick = { vm.selectRace(it) },
            onRetry = { vm.load() }
        )
    }
}

@Composable
fun ResultsListScreen(
    state: UiState<List<RaceWithTrack>>,
    topThreeByRound: Map<String, List<TopThreeEntry>>,
    onRaceClick: (RaceWithTrack) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        BoxBoxTopBar(title = "RESULTS")

        when (val s = state) {
            is UiState.Loading -> LoadingScreen()
            is UiState.Error -> ErrorScreen(s.message, onRetry)
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No races found", color = AppColors.onSurfaceVariant)
                    }
                } else {
                    val nextRaceRound = s.data.firstOrNull { !it.isPast }?.race?.round
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(s.data) { raceWithTrack ->
                            if (raceWithTrack.isPast) {
                                PastRaceCard(
                                    raceWithTrack = raceWithTrack,
                                    topThree = topThreeByRound[raceWithTrack.race.round],
                                    onClick = { onRaceClick(raceWithTrack) }
                                )
                            } else {
                                UpcomingRaceCard(
                                    raceWithTrack = raceWithTrack,
                                    isNextRace = raceWithTrack.race.round == nextRaceRound,
                                    onClick = { onRaceClick(raceWithTrack) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingRaceCard(
    raceWithTrack: RaceWithTrack,
    isNextRace: Boolean = false,
    onClick: () -> Unit
) {
    val race = raceWithTrack.race
    val meeting = raceWithTrack.meeting
    val isDarkTheme = isSystemInDarkTheme()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppColors.surface,
        border = if (isNextRace) BorderStroke(1.5.dp, AppColors.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            if (isNextRace) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.primary, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        "NEXT RACE",
                        color = AppColors.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val flagUrl = raceFlagUrlFor(race.Circuit.Location.country)
                            ?: meeting?.country_flag?.takeIf { it.isNotEmpty() }
                        if (flagUrl != null) {
                            AsyncImage(
                                model = flagUrl,
                                contentDescription = race.Circuit.Location.country,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            "ROUND ${race.round}",
                            color = AppColors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        race.Circuit.Location.country,
                        color = AppColors.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        race.raceName.uppercase(),
                        color = AppColors.onSurfaceVariant,
                        fontSize = 11.sp,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        formatRaceDateRange(race.date),
                        color = AppColors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (meeting?.circuit_image?.isNotEmpty() == true) {
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .aspectRatio(4f / 3f),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = meeting.circuit_image,
                            contentDescription = "${race.Circuit.circuitName} layout",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            colorFilter = circuitImageColorFilterFor(race.Circuit.circuitId, isDarkTheme)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PastRaceCard(
    raceWithTrack: RaceWithTrack,
    topThree: List<TopThreeEntry>?,
    onClick: () -> Unit
) {
    val race = raceWithTrack.race
    val meeting = raceWithTrack.meeting

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppColors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val flagUrl = raceFlagUrlFor(race.Circuit.Location.country)
                    ?: meeting?.country_flag?.takeIf { it.isNotEmpty() }
                if (flagUrl != null) {
                    AsyncImage(
                        model = flagUrl,
                        contentDescription = race.Circuit.Location.country,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    "ROUND ${race.round}",
                    color = AppColors.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatRaceDateShort(race.date),
                    color = AppColors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                race.Circuit.Location.country,
                color = AppColors.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                race.raceName,
                color = AppColors.onSurfaceVariant,
                fontSize = 12.sp
            )

            if (!topThree.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    topThree.forEach { entry ->
                        PodiumChip(entry, modifier = Modifier.weight(1f))
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Results unavailable",
                    color = AppColors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun PodiumChip(entry: TopThreeEntry, modifier: Modifier = Modifier) {
    val photoUrl = driverHighResPhotos[normalizeForMatch(entry.driverFamilyName)]

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AppColors.surface),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = entry.driverCode,
                    modifier = Modifier.size(28.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
            } else {
                Text(entry.driverCode.take(1), color = AppColors.onBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(6.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.position,
                    color = AppColors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    entry.driverCode,
                    color = AppColors.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                entry.gapOrTime.ifEmpty { "—" },
                color = AppColors.onSurfaceVariant,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun RaceDetailScreen(
    raceWithTrack: RaceWithTrack,
    resultsTable: UiState<List<TopThreeEntry>>,
    onBack: () -> Unit
) {
    val race = raceWithTrack.race
    val meeting = raceWithTrack.meeting
    var resultsExpanded by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        Surface(
            color = AppColors.background,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.offset(y = (-4).dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppColors.onBackground)
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(
                        "ROUND ${race.round}",
                        color = AppColors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        race.raceName,
                        color = AppColors.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(4.dp)
                            .background(AppColors.primary, RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    ResultDetailInfoRow("Circuit", race.Circuit.circuitName)
                    ResultDetailInfoRow("Location", "${race.Circuit.Location.locality}, ${race.Circuit.Location.country}")
                    ResultDetailInfoRow("Date", formatRaceDateRange(race.date), isLast = true)
                }
            }

            if (meeting?.circuit_image?.isNotEmpty() == true) {
                item {
                    Surface(shape = RoundedCornerShape(14.dp), color = AppColors.surface, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .aspectRatio(4f / 3f),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = meeting.circuit_image,
                                contentDescription = "${race.Circuit.circuitName} layout",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                colorFilter = circuitImageColorFilterFor(race.Circuit.circuitId, isDarkTheme)
                            )
                        }
                    }
                }
            }

            val facts = circuitFactsFor(race.Circuit.circuitId)
            if (facts != null) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Text("Circuit Length", color = AppColors.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${facts.circuitLengthKm}km",
                            color = AppColors.onBackground,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(14.dp))
                        Divider(color = AppColors.outline, thickness = 0.5.dp)
                        Spacer(Modifier.height(14.dp))
                        CircuitFactRow("First Grand Prix", facts.firstGrandPrix, "Number of Laps", facts.numberOfLaps.toString())
                        Spacer(Modifier.height(14.dp))
                        Divider(color = AppColors.outline, thickness = 0.5.dp)
                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fastest Lap", color = AppColors.onSurfaceVariant, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(facts.lapRecordTime, color = AppColors.onBackground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                if (facts.lapRecordDriver != "—") {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${facts.lapRecordDriver} (${facts.lapRecordYear})",
                                        color = AppColors.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Race Distance", color = AppColors.onSurfaceVariant, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("${facts.raceDistanceKm}km", color = AppColors.onBackground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            if (raceWithTrack.isPast) {
                item { SectionLabel("Results") }
                when (resultsTable) {
                    is UiState.Loading -> item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppColors.primary)
                        }
                    }
                    is UiState.Error -> item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(resultsTable.message, color = AppColors.onSurfaceVariant)
                        }
                    }
                    is UiState.Success -> {
                        item { ResultsTableHeader() }
                        val visibleResults = if (resultsExpanded) resultsTable.data else resultsTable.data.take(5)
                        itemsIndexed(visibleResults) { index, entry ->
                            ResultsTableRow(entry)
                            if (index != visibleResults.lastIndex) {
                                Divider(color = AppColors.outline, thickness = 0.5.dp)
                            }
                        }
                        if (resultsTable.data.size > 5) {
                            item {
                                ShowAllToggle(
                                    expanded = resultsExpanded,
                                    onClick = { resultsExpanded = !resultsExpanded }
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("This race hasn't happened yet", color = AppColors.onSurfaceVariant)
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** "Show all" / "Show less" toggle row below the results table, matching the reference. */
@Composable
fun ShowAllToggle(expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (expanded) "Show less" else "Show all",
            color = AppColors.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = AppColors.onBackground,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Header row for the results table - Pos. / Driver / Time / Pts, matching the reference. */
@Composable
fun ResultsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Pos.", color = AppColors.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.width(36.dp))
        Text("Driver", color = AppColors.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("Time", color = AppColors.onSurfaceVariant, fontSize = 12.sp)
        Text(
            "Pts",
            color = AppColors.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

/** A single finisher row in the results table - position, driver photo + code, time/gap, points. */
@Composable
fun ResultsTableRow(entry: TopThreeEntry) {
    val photoUrl = driverHighResPhotos[normalizeForMatch(entry.driverFamilyName)]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            entry.position,
            color = AppColors.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AppColors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = entry.driverCode,
                        modifier = Modifier.size(28.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter
                    )
                } else {
                    Text(entry.driverCode.take(1), color = AppColors.onBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                entry.driverCode,
                color = AppColors.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            entry.gapOrTime.ifEmpty { "—" },
            color = AppColors.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            entry.points.ifEmpty { "0" },
            color = AppColors.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun ResultDetailInfoRow(label: String, value: String, isLast: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AppColors.onSurfaceVariant, fontSize = 13.sp)
        Text(value, color = AppColors.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    if (!isLast) Divider(color = AppColors.outline, thickness = 0.5.dp)
}

/** Two-column stat pair for the circuit facts card, e.g. First Grand Prix / Number of Laps. */
@Composable
fun CircuitFactRow(label1: String, value1: String, label2: String, value2: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label1, color = AppColors.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value1, color = AppColors.onBackground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label2, color = AppColors.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value2, color = AppColors.onBackground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

fun formatRaceDateRange(date: String): String {
    return try {
        val d = LocalDate.parse(date)
        val start = d.minusDays(2)
        if (start.month == d.month) {
            "${start.dayOfMonth} - ${d.format(DateTimeFormatter.ofPattern("dd MMM"))}"
        } else {
            "${start.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${d.format(DateTimeFormatter.ofPattern("dd MMM"))}"
        }
    } catch (e: Exception) { date }
}

fun formatRaceDateShort(date: String): String {
    return try {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd MMM"))
    } catch (e: Exception) { date }
}
