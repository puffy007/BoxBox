package com.boxbox.app.ui.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.boxbox.app.data.model.DriverSeasonSummary
import com.boxbox.app.data.model.UiState
import com.boxbox.app.data.repository.normalizeForMatch
import com.boxbox.app.ui.*
import com.boxbox.app.ui.theme.*
import com.boxbox.app.viewmodel.DriverDetailData
import com.boxbox.app.viewmodel.DriverDetailViewModel

@Composable
fun DriverDetailScreen(
    driverId: String,
    onBack: () -> Unit,
    vm: DriverDetailViewModel = viewModel()
) {
    LaunchedEffect(driverId) { vm.load(driverId) }
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
                ErrorScreen(s.message) { vm.load(driverId) }
            }
            is UiState.Success -> DriverDetailContent(s.data, onBack)
        }
    }
}

@Composable
fun DetailBackBar(onBack: () -> Unit, bg: Color) {
    Surface(color = bg, shadowElevation = 2.dp, modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppColors.onBackground)
            }
        }
    }
}

@Composable
fun DriverDetailContent(data: DriverDetailData, onBack: () -> Unit) {
    val teamName = data.standing.Constructors.firstOrNull()?.name ?: ""
    val teamColor = getTeamColor(teamName)
    val driver = data.standing.Driver
    val openF1 = data.openF1Driver

    val surnameKey = normalizeForMatch(driver.familyName)
    val photoUrl = driverHighResPhotos[surnameKey]
        ?: driverHighResPhotos.entries.firstOrNull { (key, _) ->
            surnameKey.contains(key) || key.contains(surnameKey)
        }?.value
        ?: openF1?.headshot_url?.takeIf { it.isNotEmpty() }

    val carNumber = openF1?.driver_number?.takeIf { it != 0 }?.toString()
        ?: data.standing.Driver.permanentNumber.ifEmpty { "—" }

    // statusBarsPadding/navigationBarsPadding are applied to the OUTER Column, BEFORE
    // verticalScroll() in the modifier chain. Modifiers are applied outer-to-inner left
    // to right, so placing them before verticalScroll() makes them outer/scroll-independent
    // space reserved at the true top/bottom of the screen - it never moves no matter what
    // content has scrolled into view. Putting them after verticalScroll() (the previous
    // bug) or only on the first item's Box made them scroll away with the content instead
    // of protecting the actual screen edges.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ---- Hero header: team-colored gradient, giant number watermark, photo, name ----
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
            Text(
                carNumber,
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 210.sp,
                    fontWeight = FontWeight.Bold,
                    drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-10).dp)
            )
            Text(
                carNumber,
                color = Color.White.copy(alpha = 0.18f),
                fontSize = 210.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-10).dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .fillMaxHeight(0.85f)
                            .align(Alignment.Center)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.18f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    if (!photoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = driver.familyName,
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .fillMaxHeight()
                                .align(Alignment.Center)
                                .graphicsLayer(alpha = 0.99f)
                                .drawWithCache {
                                    val fadeBrush = Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black,
                                            0.6f to Color.Black,
                                            1.0f to Color.Transparent
                                        ),
                                        startY = 0f,
                                        endY = size.height
                                    )
                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
                                    }
                                },
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                driver.code.ifEmpty { driver.familyName.take(3).uppercase() },
                                color = Color.White,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        driver.givenName,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        driver.familyName.uppercase(),
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val countryInfo = countryInfoFor(driver.nationality)
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
                        Text(
                            countryInfo?.countryName ?: driver.nationality.ifEmpty { "—" },
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "  |  ",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                        Text(
                            teamName,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "  |  ",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                        Text(
                            carNumber,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // ---- Stats content directly below hero, no tabs ----
        StatisticsTab(data, teamColor, carNumber)
    }
}

@Composable
fun StatisticsTab(data: DriverDetailData, teamColor: Color, carNumber: String) {
    val driver = data.standing.Driver
    val teamName = data.standing.Constructors.firstOrNull()?.name ?: ""
    val summary = data.seasonSummary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Two-column detailed breakdown, in the official app's stat-sheet style:
        // muted gray label above, big bold number below, divider lines between groups.
        // Every row pairs two real values - no row is left with an empty second column.
        StatRow("Season Position", "P${data.standing.position}", "Season Points", data.standing.points)
        Divider(color = AppColors.outline, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

        if (summary != null) {
            StatRow("Wins", data.standing.wins, "Races Entered", summary.racesEntered.toString())
            Spacer(Modifier.height(18.dp))
            StatRow("Podiums", summary.podiums.toString(), "Win Rate", winRate(summary))
            Divider(color = AppColors.outline, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))
            StatRow(
                "Code",
                data.openF1Driver?.name_acronym?.ifEmpty { null } ?: driver.code.ifEmpty { "—" },
                "",
                ""
            )
        } else {
            // Season summary requires an extra API call per driver and may still be
            // loading, or could fail (offline, rate limit) - show a lightweight
            // placeholder rather than blocking the rest of the screen on it.
            StatRow("Wins", data.standing.wins, "", "")
            Text(
                "Loading season results…",
                color = AppColors.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Divider(color = AppColors.outline, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))
            StatRow(
                "Code",
                data.openF1Driver?.name_acronym?.ifEmpty { null } ?: driver.code.ifEmpty { "—" },
                "",
                ""
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun winRate(summary: DriverSeasonSummary): String {
    if (summary.racesEntered == 0) return "—"
    val pct = (summary.wins.toDouble() / summary.racesEntered.toDouble()) * 100
    return "${pct.toInt()}%"
}

@Composable
fun StatRow(label1: String, value1: String, label2: String, value2: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label1, color = AppColors.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text(value1, color = AppColors.onBackground, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            if (label2.isNotEmpty()) {
                Text(label2, color = AppColors.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(value2, color = AppColors.onBackground, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DetailStatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = AppColors.surface, modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label.uppercase(), color = AppColors.onSurfaceVariant, fontSize = 9.sp, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun DetailInfoRow(label: String, value: String, isLast: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AppColors.onSurfaceVariant, fontSize = 13.sp)
        Text(value, color = AppColors.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    if (!isLast) Divider(color = AppColors.outline, thickness = 0.5.dp)
}
