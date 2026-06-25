package com.boxbox.app.ui.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.boxbox.app.data.model.Driver
import com.boxbox.app.data.model.UiState
import com.boxbox.app.ui.*
import com.boxbox.app.ui.theme.*
import com.boxbox.app.viewmodel.TeamDetailData
import com.boxbox.app.viewmodel.TeamDetailViewModel

@Composable
fun TeamDetailScreen(
    constructorId: String,
    onBack: () -> Unit,
    onDriverClick: (String) -> Unit = {},
    vm: TeamDetailViewModel = viewModel()
) {
    LaunchedEffect(constructorId) { vm.load(constructorId) }
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        when (val s = state) {
            is UiState.Loading -> {
                DetailBackBar(onBack, AppColors.surface)
                LoadingScreen()
            }
            is UiState.Error -> {
                DetailBackBar(onBack, AppColors.surface)
                ErrorScreen(s.message) { vm.load(constructorId) }
            }
            is UiState.Success -> TeamDetailContent(s.data, onBack, onDriverClick)
        }
    }
}

@Composable
fun TeamDetailContent(data: TeamDetailData, onBack: () -> Unit, onDriverClick: (String) -> Unit) {
    val teamColor = getTeamColor(data.standing.Constructor.name)
    val teamName = data.standing.Constructor.name
    val driverNames = data.drivers.map { it.driver.full_name }
        .ifEmpty { listOf(data.standing.Constructor.name) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            // ---- Hero header: team color gradient, car photo, big team name,
            // driver names row, nationality flag chip ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                lightenColor(teamColor, 0.18f),
                                teamColor,
                                darkenColor(teamColor, 0.45f)
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 28.dp, top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val carPhotoUrl = resolveTeamCarPhoto(teamName)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(110.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.18f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            if (carPhotoUrl != null) {
                                AsyncImage(
                                    model = carPhotoUrl,
                                    contentDescription = "$teamName car",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("🏎️", fontSize = 64.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            teamName.uppercase(),
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            driverNames.forEachIndexed { index, name ->
                                Text(
                                    name,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (index != driverNames.lastIndex) {
                                    Text(
                                        "  |  ",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Nationality flag, same real-image style as the driver screen
                        val countryInfo = countryInfoFor(data.standing.Constructor.nationality)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            countryInfo?.let { info ->
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = flagUrlFor(info.isoCode),
                                        contentDescription = info.countryName,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    countryInfo?.countryName ?: data.standing.Constructor.nationality,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // ---- Stat sheet, same style as the driver detail screen ----
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                StatRow("Season Position", "P${data.standing.position}", "Season Points", data.standing.points)
            }
        }

        item {
            Text(
                "DRIVER LINEUP".uppercase(),
                color = AppColors.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (data.drivers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Driver lineup unavailable right now",
                        color = AppColors.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(data.drivers) { lineupDriver ->
                TeamDriverRow(lineupDriver.driver, teamColor) { onDriverClick(lineupDriver.driverId) }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Text(
                "Position, points, and wins come from Jolpica's championship standings. Driver photos, numbers, and codes come from OpenF1's latest session.",
                color = AppColors.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun TeamDriverRow(driver: Driver, teamColor: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppColors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(teamColor.copy(alpha = 0.15f))
                    .border(2.dp, teamColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (driver.headshot_url.isNotEmpty()) {
                    AsyncImage(
                        model = driver.headshot_url,
                        contentDescription = driver.full_name,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        driver.name_acronym.ifEmpty { "?" },
                        color = teamColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(driver.full_name, color = AppColors.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = teamColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            "#${driver.driver_number}",
                            color = teamColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(driver.name_acronym, color = AppColors.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppColors.outline)
        }
    }
}

/** Blends a color toward white by the given amount (0f-1f), for a subtle lighter shade. */
fun lightenColor(color: Color, amount: Float): Color {
    return Color(
        red = color.red + (1f - color.red) * amount,
        green = color.green + (1f - color.green) * amount,
        blue = color.blue + (1f - color.blue) * amount,
        alpha = color.alpha
    )
}

/** Blends a color toward black by the given amount (0f-1f), for a subtle darker shade. */
fun darkenColor(color: Color, amount: Float): Color {
    return Color(
        red = color.red * (1f - amount),
        green = color.green * (1f - amount),
        blue = color.blue * (1f - amount),
        alpha = color.alpha
    )
}
