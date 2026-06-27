package com.boxbox.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.boxbox.app.data.model.*
import com.boxbox.app.data.repository.BoxBoxRepository
import com.boxbox.app.data.repository.getRaceTopThree
import com.boxbox.app.data.repository.normalizeForMatch
import com.boxbox.app.notifications.RaceNotificationScheduler
import com.boxbox.app.ui.*
import com.boxbox.app.ui.results.TopThreeEntry
import com.boxbox.app.ui.results.formatRaceDateShort
import com.boxbox.app.ui.standings.driverHighResPhotos
import com.boxbox.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Lightweight home-screen summary data, fetched once and shown as a quick snapshot. */
data class HomeSummary(
    val nextRace: Race?,
    val lastRace: Race?,
    val lastRaceTopThree: List<TopThreeEntry>,
    val topDrivers: List<DriverStanding>,
    val topConstructors: List<ConstructorStanding>
)

class HomeSummaryViewModel(
    private val repository: BoxBoxRepository = BoxBoxRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeSummary>>(UiState.Loading)
    val state: StateFlow<UiState<HomeSummary>> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val schedule = repository.getCurrentSchedule()
                val today = LocalDate.now()

                val nextRace = schedule.firstOrNull { race ->
                    runCatching { LocalDate.parse(race.date).isAfter(today.minusDays(1)) }.getOrDefault(false)
                }
                val lastRace = schedule.lastOrNull { race ->
                    runCatching { LocalDate.parse(race.date).isBefore(today) }.getOrDefault(false)
                }

                val lastRaceTopThree = lastRace?.let { race ->
                    runCatching { repository.getRaceTopThree(race.season, race.round) }.getOrNull().orEmpty()
                }.orEmpty()

                val topDrivers = runCatching { repository.getDriverStandings() }.getOrNull().orEmpty().take(3)
                val topConstructors = runCatching { repository.getConstructorStandings() }.getOrNull().orEmpty().take(3)

                _state.value = UiState.Success(
                    HomeSummary(nextRace, lastRace, lastRaceTopThree, topDrivers, topConstructors)
                )
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Error loading home summary")
            }
        }
    }
}

@Composable
fun HomeScreen(vm: HomeSummaryViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        BoxBoxTopBar(title = "BOXBOX") {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = AppColors.onPrimary)
            }
        }
        when (val s = state) {
            is UiState.Loading -> LoadingScreen()
            is UiState.Error -> ErrorScreen(s.message) { vm.load() }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    s.data.nextRace?.let { race ->
                        item { CountdownCard(race) }
                    }

                    s.data.lastRace?.let { race ->
                        item {
                            SectionLabel("Last Result")
                            Spacer(Modifier.height(4.dp))
                            LastResultCard(race, s.data.lastRaceTopThree)
                        }
                    }

                    if (s.data.topDrivers.isNotEmpty()) {
                        item {
                            PillSectionHeader("DRIVERS' STANDINGS")
                            Spacer(Modifier.height(10.dp))
                            TopStandingsCard(s.data.topDrivers)
                        }
                    }

                    if (s.data.topConstructors.isNotEmpty()) {
                        item {
                            PillSectionHeader("TEAMS' STANDINGS")
                            Spacer(Modifier.height(10.dp))
                            TopConstructorsCard(s.data.topConstructors)
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun CountdownCard(race: Race) {
    val context = LocalContext.current
    val countdownParts = remember(race) { computeCountdownParts(race) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            lightenColorFor(AppColors.primary, 0.12f),
                            AppColors.primary,
                            darkenColorFor(AppColors.primary, 0.18f)
                        )
                    )
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "NEXT RACE",
                color = AppColors.onPrimary.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(12.dp))

            if (countdownParts != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CountdownUnit(countdownParts.days, "DAYS")
                    CountdownUnit(countdownParts.hours, "HRS")
                    CountdownUnit(countdownParts.minutes, "MIN")
                }
            } else {
                Text(
                    "Race weekend underway",
                    color = AppColors.onPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                race.raceName,
                color = AppColors.onPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                race.Circuit.Location.locality,
                color = AppColors.onPrimary.copy(alpha = 0.8f),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                race.FirstPractice?.let { SessionChip("FP1", it.date) }
                race.Qualifying?.let { SessionChip("QUALI", it.date) }
                SessionChip("RACE", race.date)
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { RaceNotificationScheduler.scheduleRaceNotification(context, race) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.95f),
                    contentColor = AppColors.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Notify me 30 min before", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SessionChip(label: String, date: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = AppColors.onPrimary.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(
            formatSessionDate(date),
            color = AppColors.onPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Quick podium summary for the most recently completed race. */
@Composable
fun LastResultCard(race: Race, topThree: List<TopThreeEntry>) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppColors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(race.raceName, color = AppColors.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${race.Circuit.Location.locality} · ${formatRaceDateShort(race.date)}",
                        color = AppColors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            if (topThree.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    topThree.forEach { entry ->
                        HomePodiumChip(entry, modifier = Modifier.weight(1f))
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text("Results unavailable", color = AppColors.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HomePodiumChip(entry: TopThreeEntry, modifier: Modifier = Modifier) {
    val photoUrl = driverHighResPhotos[normalizeForMatch(entry.driverFamilyName)]

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
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
                Text(entry.position, color = AppColors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(3.dp))
                Text(entry.driverCode, color = AppColors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Top 3 drivers championship snapshot. */
@Composable
fun TopStandingsCard(topDrivers: List<DriverStanding>) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppColors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            topDrivers.forEachIndexed { index, standing ->
                val teamName = standing.Constructors.firstOrNull()?.name ?: ""
                val teamColor = getTeamColor(teamName)
                val photoUrl = driverHighResPhotos[normalizeForMatch(standing.Driver.familyName)]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        standing.position,
                        color = AppColors.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp)
                    )
                    val photoBgAlpha = if (ThemeState.isDarkMode) 0.15f else 0.35f
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(teamColor.copy(alpha = photoBgAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl != null) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = standing.Driver.familyName,
                                modifier = Modifier.size(36.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter
                            )
                        } else {
                            Text(
                                standing.Driver.code.ifEmpty { standing.Driver.familyName.take(2).uppercase() },
                                color = teamColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${standing.Driver.givenName.take(1)}. ${standing.Driver.familyName}",
                            color = AppColors.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(teamName, color = AppColors.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Text(
                        standing.points,
                        color = AppColors.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (index != topDrivers.lastIndex) {
                    Divider(color = AppColors.outline, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

/** Pill-style section header with an arrow, matching the official F1 app's "DRIVERS' STANDINGS →" style. */
@Composable
fun PillSectionHeader(title: String, onClick: () -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AppColors.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                color = AppColors.onBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppColors.onBackground,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Top 3 constructors championship snapshot, same visual language as TopStandingsCard. */
@Composable
fun TopConstructorsCard(topConstructors: List<ConstructorStanding>) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppColors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            topConstructors.forEachIndexed { index, standing ->
                val teamColor = getTeamColor(standing.Constructor.name)
                val logoUrl = com.boxbox.app.ui.standings.resolveTeamLogo(standing.Constructor.name)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        standing.position,
                        color = AppColors.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp)
                    )
                    val logoBgAlpha = if (ThemeState.isDarkMode) 0.35f else 0.55f
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(teamColor.copy(alpha = logoBgAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoUrl != null) {
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = "${standing.Constructor.name} logo",
                                modifier = Modifier.size(20.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        standing.Constructor.name,
                        color = AppColors.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        standing.points,
                        color = AppColors.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (index != topConstructors.lastIndex) {
                    Divider(color = AppColors.outline, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

data class CountdownParts(val days: String, val hours: String, val minutes: String)

fun computeCountdownParts(race: Race): CountdownParts? {
    return try {
        val dateStr = "${race.date}T${race.time ?: "00:00:00Z"}"
        val raceTime = ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
        val now = ZonedDateTime.now()
        if (raceTime.isBefore(now)) return null
        val days = ChronoUnit.DAYS.between(now, raceTime)
        val hours = ChronoUnit.HOURS.between(now, raceTime) % 24
        val mins = ChronoUnit.MINUTES.between(now, raceTime) % 60
        CountdownParts("%02d".format(days), "%02d".format(hours), "%02d".format(mins))
    } catch (e: Exception) { null }
}

/** One countdown segment, e.g. "00" over "DAYS", in its own rounded chip. */
@Composable
fun CountdownUnit(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                value,
                color = AppColors.onPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = AppColors.onPrimary.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

/** Blends a color toward white - local helper so HomeScreen doesn't need a cross-package import. */
fun lightenColorFor(color: Color, amount: Float): Color {
    return Color(
        red = color.red + (1f - color.red) * amount,
        green = color.green + (1f - color.green) * amount,
        blue = color.blue + (1f - color.blue) * amount,
        alpha = color.alpha
    )
}

/** Blends a color toward black - local helper so HomeScreen doesn't need a cross-package import. */
fun darkenColorFor(color: Color, amount: Float): Color {
    return Color(
        red = color.red * (1f - amount),
        green = color.green * (1f - amount),
        blue = color.blue * (1f - amount),
        alpha = color.alpha
    )
}

fun formatSessionDate(date: String): String {
    return try {
        val d = LocalDate.parse(date)
        d.format(DateTimeFormatter.ofPattern("EEE d"))
    } catch (e: Exception) { date }
}
