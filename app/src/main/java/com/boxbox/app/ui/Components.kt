package com.boxbox.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boxbox.app.ui.theme.*

@Composable
fun BoxBoxTopBar(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    Surface(color = F1Red, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) { actions() }
        }
    }
}

@Composable
fun LiveTopBar(subtitle: String, lap: String = "") {
    Surface(color = F1Black, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot()
                Spacer(Modifier.width(8.dp))
                Text(subtitle, color = F1White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            if (lap.isNotEmpty()) {
                Text(lap, color = F1LightGray, fontSize = 12.sp)
            } else {
                LiveBadge()
            }
        }
    }
}

@Composable
fun LiveDot() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(F1Red)
    )
}

@Composable
fun LiveBadge() {
    Surface(
        color = F1Red,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "LIVE",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun TeamColorBar(teamName: String, modifier: Modifier = Modifier) {
    val color = getTeamColor(teamName)
    Box(
        modifier = modifier
            .width(4.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

@Composable
fun TyreDot(compound: String) {
    val color = when (compound.uppercase()) {
        "SOFT" -> F1Red
        "MEDIUM" -> F1Yellow
        "HARD" -> F1White
        "INTERMEDIATE" -> F1Green
        "WET" -> Color(0xFF0090FF)
        else -> F1LightGray
    }

    // Moderniji i čišći način crtanja gume pomoću Modifiers
    Box(
        modifier = Modifier
            .size(12.dp)
            .border(
                width = 1.dp,
                color = color,
                shape = CircleShape
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun TyreIndicator(compound: String) {
    val color = when (compound.uppercase()) {
        "SOFT" -> F1Red
        "MEDIUM" -> F1Yellow
        "HARD" -> F1White
        "INTERMEDIATE" -> F1Green
        "WET" -> Color(0xFF0090FF)
        else -> F1LightGray
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = F1LightGray,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = F1Red)
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = F1White)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = F1Red)
            ) { Text("Retry") }
        }
    }
}

fun getTeamColor(teamName: String): Color {
    return when {
        teamName.contains("Red Bull", ignoreCase = true) -> RedBullColor
        teamName.contains("Ferrari", ignoreCase = true) -> FerrariColor
        teamName.contains("McLaren", ignoreCase = true) -> McLarenColor
        teamName.contains("Mercedes", ignoreCase = true) -> MercedesColor
        teamName.contains("Aston", ignoreCase = true) -> AstonMartinColor
        teamName.contains("Alpine", ignoreCase = true) -> AlpineColor
        teamName.contains("Williams", ignoreCase = true) -> WilliamsColor
        teamName.contains("RB", ignoreCase = true) || teamName.contains("Toro", ignoreCase = true) -> RBColor
        teamName.contains("Sauber", ignoreCase = true) || teamName.contains("Alfa", ignoreCase = true) -> SauberColor
        teamName.contains("Haas", ignoreCase = true) -> HaasColor
        else -> Color(0xFF888888)
    }
}

fun formatLapTime(seconds: Double?): String {
    if (seconds == null) return "--:--.---"
    val mins = (seconds / 60).toInt()
    val secs = seconds % 60
    return "%d:%06.3f".format(mins, secs)
}
