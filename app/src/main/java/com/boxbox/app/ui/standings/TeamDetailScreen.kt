package com.boxbox.app.ui.standings

import androidx.compose.foundation.background
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
    onDriverClick: (Driver) -> Unit = {},
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
fun TeamDetailContent(data: TeamDetailData, onBack: () -> Unit, onDriverClick: (Driver) -> Unit) {
    val teamColor = getTeamColor(data.standing.Constructor.name)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Surface(color = teamColor, shadowElevation = 4.dp) {
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
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏎️", fontSize = 28.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            data.standing.Constructor.name.uppercase(),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            data.standing.Constructor.nationality,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailStatCard("Position", "P${data.standing.position}", teamColor, Modifier.weight(1f))
                DetailStatCard("Points", data.standing.points, teamColor, Modifier.weight(1f))
                DetailStatCard("Wins", data.standing.wins, teamColor, Modifier.weight(1f))
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
            items(data.drivers) { driver ->
                TeamDriverRow(driver, teamColor) { onDriverClick(driver) }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun TeamDriverRow(driver: Driver, teamColor: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppColors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(teamColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                if (driver.headshot_url.isNotEmpty()) {
                    AsyncImage(
                        model = driver.headshot_url,
                        contentDescription = driver.full_name,
                        modifier = Modifier.size(48.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        driver.name_acronym.ifEmpty { "?" },
                        color = teamColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(driver.full_name, color = AppColors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("#${driver.driver_number} · ${driver.name_acronym}", color = AppColors.onSurfaceVariant, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppColors.outline)
        }
    }
}
