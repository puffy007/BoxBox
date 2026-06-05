package com.boxbox.app.ui.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boxbox.app.data.model.*
import com.boxbox.app.ui.*
import com.boxbox.app.ui.theme.*
import com.boxbox.app.viewmodel.StandingsViewModel

@Composable
fun StandingsScreen(vm: StandingsViewModel = viewModel()) {
    val driverState by vm.driverStandings.collectAsState()
    val constructorState by vm.constructorStandings.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(F1Black)
    ) {
        BoxBoxTopBar(title = "STANDINGS")

        // Segment control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(F1DarkGray, RoundedCornerShape(10.dp))
                .padding(3.dp)
        ) {
            listOf("Drivers", "Constructors").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selectedTab == index) F1Red else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickableNoRipple { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (selectedTab == index) Color.White else F1LightGray,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> when (val state = driverState) {
                is UiState.Loading -> LoadingScreen()
                is UiState.Error -> ErrorScreen(state.message) { vm.loadStandings() }
                is UiState.Success -> DriverStandingsList(state.data)
            }
            1 -> when (val state = constructorState) {
                is UiState.Loading -> LoadingScreen()
                is UiState.Error -> ErrorScreen(state.message) { vm.loadStandings() }
                is UiState.Success -> ConstructorStandingsList(state.data)
            }
        }
    }
}

@Composable
fun DriverStandingsList(standings: List<DriverStanding>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(standings) { _, standing ->
            DriverStandingRow(standing)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun DriverStandingRow(standing: DriverStanding) {
    val teamName = standing.Constructors.firstOrNull()?.name ?: ""
    val isFirst = standing.position == "1"

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = F1DarkGray,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                standing.position,
                color = if (isFirst) F1Red else F1LightGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp)
            )
            TeamColorBar(teamName)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${standing.Driver.givenName} ${standing.Driver.familyName}",
                    color = F1White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(teamName, color = F1LightGray, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    standing.points,
                    color = if (isFirst) F1White else F1LightGray,
                    fontSize = 15.sp,
                    fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal
                )
                Text("pts", color = F1LightGray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun ConstructorStandingsList(standings: List<ConstructorStanding>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(standings) { _, standing ->
            ConstructorStandingRow(standing)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun ConstructorStandingRow(standing: ConstructorStanding) {
    val isFirst = standing.position == "1"
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = F1DarkGray,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                standing.position,
                color = if (isFirst) F1Red else F1LightGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp)
            )
            TeamColorBar(standing.Constructor.name)
            Spacer(Modifier.width(10.dp))
            Text(
                standing.Constructor.name,
                color = F1White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    standing.points,
                    color = if (isFirst) F1White else F1LightGray,
                    fontSize = 15.sp,
                    fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal
                )
                Text("pts", color = F1LightGray, fontSize = 10.sp)
            }
        }
    }
}

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
        indication = null,
        onClick = onClick
    ))
