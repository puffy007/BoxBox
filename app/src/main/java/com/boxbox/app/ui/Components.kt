package com.boxbox.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border // Corrected import
import androidx.compose.foundation.layout.*
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
    Surface(color = AppColors.primary, shadowElevation = 4.dp) {
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
                color = AppColors.onPrimary,
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
    Surface(color = AppColors.surface, shadowElevation = 4.dp) {
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
                Text(subtitle, color = AppColors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            if (lap.isNotEmpty()) {
                Text(lap, color = AppColors.onSurfaceVariant, fontSize = 12.sp)
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
            .background(AppColors.primary)
    )
}

@Composable
fun LiveBadge() {
    Surface(
        color = AppColors.primary,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "LIVE",
            color = AppColors.onPrimary,
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
fun TyreIndicator(compound: String) {
    val color = when (compound.uppercase()) {
        "SOFT" -> Color(0xFFE10600)
        "MEDIUM" -> F1Yellow
        "HARD" -> Color(0xFFE8E8E8)
        "INTERMEDIATE" -> F1Green
        "WET" -> Color(0xFF0090FF)
        else -> Color(0xFF888888)
    }
    Box(
        modifier = Modifier
            .size(14.dp)
            .border(1.5.dp, color, CircleShape) // Fixed syntax error here
            .padding(3.dp),
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
fun TyreDot(compound: String) = TyreIndicator(compound)

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = AppColors.onSurfaceVariant,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppColors.primary)
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = AppColors.onBackground)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary)
            ) { Text("Retry", color = AppColors.onPrimary) }
        }
    }
}

fun getTeamColor(teamName: String): Color = resolveTeamAccent(teamName)

fun formatLapTime(seconds: Double?): String {
    if (seconds == null) return "--:--.---"
    val mins = (seconds / 60).toInt()
    val secs = seconds % 60
    return "%d:%06.3f".format(mins, secs)
}