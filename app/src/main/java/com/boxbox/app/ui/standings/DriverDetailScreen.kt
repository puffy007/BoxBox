package com.boxbox.app.ui.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.boxbox.app.data.model.UiState
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
    Surface(color = bg, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
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
    val photoUrl = openF1?.headshot_url?.takeIf { it.isNotEmpty() }
    val carNumber = openF1?.driver_number?.takeIf { it != 0 }?.toString()
        ?: data.standing.Driver.permanentNumber.ifEmpty { "—" }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- Hero header: team-colored gradient, giant number watermark, photo, name ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(teamColor, teamColor.copy(alpha = 0.75f))
                    )
                )
        ) {
            // Giant car-number watermark behind the photo
            Text(
                carNumber,
                color = Color.White.copy(alpha = 0.16f),
                fontSize = 220.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 8.dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                // Top bar: back only
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

                // Driver photo, full width, fills the hero area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (!photoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = driver.familyName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
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

                // Name block: script-style first name, bold surname
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

                    // Country | Team | Number row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            driver.nationality.ifEmpty { "—" },
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DetailStatCard("Position", "P${data.standing.position}", teamColor, Modifier.weight(1f))
            DetailStatCard("Points", data.standing.points, teamColor, Modifier.weight(1f))
            DetailStatCard("Wins", data.standing.wins, teamColor, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        Surface(shape = RoundedCornerShape(14.dp), color = AppColors.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailInfoRow("Car number", "#$carNumber")
                DetailInfoRow("Nationality", driver.nationality)
                DetailInfoRow("Team", teamName)
                DetailInfoRow(
                    "Code",
                    data.openF1Driver?.name_acronym?.ifEmpty { null } ?: driver.code.ifEmpty { "—" },
                    isLast = true
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Live data (number, code, photo) comes from OpenF1's latest session. Standings and points come from Jolpica.",
            color = AppColors.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
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
