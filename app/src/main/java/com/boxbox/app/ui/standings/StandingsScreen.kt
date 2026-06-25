package com.boxbox.app.ui.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.boxbox.app.data.repository.normalizeForMatch
import com.boxbox.app.data.model.*
import com.boxbox.app.ui.*
import com.boxbox.app.ui.theme.*
import com.boxbox.app.viewmodel.StandingsViewModel

@Composable
fun StandingsScreen(
    vm: StandingsViewModel = viewModel(),
    onDriverClick: (String) -> Unit = {},
    onTeamClick: (String) -> Unit = {}
) {
    val driverState by vm.driverStandings.collectAsState()
    val constructorState by vm.constructorStandings.collectAsState()
    val driverPhotosByName by vm.driverPhotosByName.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        BoxBoxTopBar(title = "STANDINGS")

        // Drivers / Constructors segmented tabs - simple underline style like official app
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surface)
        ) {
            listOf("Drivers", "Constructors").forEachIndexed { index, label ->
                val selected = selectedTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickableNoRipple { selectedTab = index }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        label,
                        color = if (selected) AppColors.onBackground else AppColors.onSurfaceVariant,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (selected) AppColors.primary else Color.Transparent)
                    )
                }
            }
        }

        // Column headers like the official app: Pos. / Driver / Pts.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.background)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                "POS.",
                color = AppColors.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.width(40.dp)
            )
            Text(
                if (selectedTab == 0) "DRIVER" else "TEAM",
                color = AppColors.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "PTS.",
                color = AppColors.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Divider(color = AppColors.outline, thickness = 0.5.dp)

        when (selectedTab) {
            0 -> when (val state = driverState) {
                is UiState.Loading -> LoadingScreen()
                is UiState.Error -> ErrorScreen(state.message) { vm.loadStandings() }
                is UiState.Success -> DriverStandingsList(state.data, driverPhotosByName, onDriverClick)
            }
            1 -> when (val state = constructorState) {
                is UiState.Loading -> LoadingScreen()
                is UiState.Error -> ErrorScreen(state.message) { vm.loadStandings() }
                is UiState.Success -> ConstructorStandingsList(state.data, onTeamClick)
            }
        }
    }
}

@Composable
fun DriverStandingsList(
    standings: List<DriverStanding>,
    driverPhotosByName: Map<String, Driver>,
    onDriverClick: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(standings) { standing ->
            val key = normalizeForMatch(standing.Driver.familyName)
            val photoUrl = driverPhotosByName[key]?.headshot_url?.takeIf { it.isNotEmpty() }
            DriverStandingRow(standing, photoUrl) { onDriverClick(standing.Driver.driverId) }
            Divider(color = AppColors.outline, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun DriverStandingRow(standing: DriverStanding, photoUrl: String?, onClick: () -> Unit) {
    val teamName = standing.Constructors.firstOrNull()?.name ?: ""
    val teamColor = getTeamColor(teamName)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            standing.position,
            color = AppColors.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )

        // Driver headshot, circular, like the official app's portrait thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(teamColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = standing.Driver.familyName,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    standing.Driver.code.ifEmpty { standing.Driver.familyName.take(2).uppercase() },
                    color = teamColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${standing.Driver.givenName.take(1)}. ${standing.Driver.familyName}",
                color = AppColors.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(teamName, color = AppColors.onSurfaceVariant, fontSize = 13.sp)
        }

        Text(
            standing.points,
            color = AppColors.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ConstructorStandingsList(standings: List<ConstructorStanding>, onTeamClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(standings) { standing ->
            ConstructorStandingRow(standing) { onTeamClick(standing.Constructor.constructorId) }
            Divider(color = AppColors.outline, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun ConstructorStandingRow(standing: ConstructorStanding, onClick: () -> Unit) {
    val teamColor = getTeamColor(standing.Constructor.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            standing.position,
            color = AppColors.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )

        Box(
            modifier = Modifier
                .width(5.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(teamColor)
        )

        Spacer(Modifier.width(14.dp))

        Text(
            standing.Constructor.name,
            color = AppColors.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Text(
            standing.points,
            color = AppColors.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}
