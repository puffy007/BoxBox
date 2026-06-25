package com.boxbox.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boxbox.app.data.model.Race
import com.boxbox.app.data.model.UiState
import com.boxbox.app.notifications.RaceNotificationScheduler
import com.boxbox.app.ui.*
import com.boxbox.app.ui.theme.*
import com.boxbox.app.viewmodel.HomeViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val scheduleState by vm.schedule.collectAsState()
    val nextRace by vm.nextRace.collectAsState()

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
        when (val state = scheduleState) {
            is UiState.Loading -> LoadingScreen()
            is UiState.Error -> ErrorScreen(state.message) { vm.loadSchedule() }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    nextRace?.let { race ->
                        item { CountdownCard(race) }
                    }
                    item { SectionLabel("2025 Calendar") }
                    items(state.data) { race ->
                        RaceCard(race = race, isNext = race == nextRace)
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
    val countdown = remember(race) { computeCountdown(race) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppColors.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Next Race", color = AppColors.onPrimary.copy(alpha = 0.75f), fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(countdown, color = AppColors.onPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "${race.raceName} · ${race.Circuit.Location.locality}",
                color = AppColors.onPrimary.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                race.FirstPractice?.let { SessionChip("FP1", it.date) }
                race.Qualifying?.let { SessionChip("QUALI", it.date) }
                SessionChip("RACE", race.date)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { RaceNotificationScheduler.scheduleRaceNotification(context, race) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.onPrimary.copy(alpha = 0.2f),
                    contentColor = AppColors.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Notify me 30 min before", fontSize = 13.sp)
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

@Composable
fun RaceCard(race: Race, isNext: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppColors.surface,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Round ${race.round}",
                    color = if (isNext) AppColors.primary else AppColors.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(race.raceName, color = AppColors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${formatDate(race.date)} · ${race.Circuit.Location.locality}",
                    color = AppColors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            if (isNext) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppColors.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        "NEXT",
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

fun computeCountdown(race: Race): String {
    return try {
        val dateStr = "${race.date}T${race.time ?: "00:00:00Z"}"
        val raceTime = ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
        val now = ZonedDateTime.now()
        if (raceTime.isBefore(now)) return "Race passed"
        val days = ChronoUnit.DAYS.between(now, raceTime)
        val hours = ChronoUnit.HOURS.between(now, raceTime) % 24
        val mins = ChronoUnit.MINUTES.between(now, raceTime) % 60
        "%02dd %02dh %02dm".format(days, hours, mins)
    } catch (e: Exception) { "--d --h --m" }
}

fun formatDate(date: String): String {
    return try {
        val d = java.time.LocalDate.parse(date)
        d.format(DateTimeFormatter.ofPattern("d MMM"))
    } catch (e: Exception) { date }
}

fun formatSessionDate(date: String): String {
    return try {
        val d = java.time.LocalDate.parse(date)
        d.format(DateTimeFormatter.ofPattern("EEE d"))
    } catch (e: Exception) { date }
}
