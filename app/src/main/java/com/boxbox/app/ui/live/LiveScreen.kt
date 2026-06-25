package com.boxbox.app.ui.live

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boxbox.app.data.model.*
import com.boxbox.app.ui.*
import com.boxbox.app.ui.theme.*
import com.boxbox.app.viewmodel.LiveViewModel

@Composable
fun LiveScreen(vm: LiveViewModel = viewModel()) {
    val positions by vm.positions.collectAsState()
    val raceControl by vm.raceControl.collectAsState()
    val stints by vm.stints.collectAsState()
    val intervals by vm.intervals.collectAsState()
    val drivers by vm.drivers.collectAsState()
    val laps by vm.laps.collectAsState()
    val isLive by vm.isLive.collectAsState()
    val currentSession by vm.currentSession.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Track", "Timing", "Race Control")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        LiveTopBar(
            subtitle = currentSession?.let { "${it.circuit_short_name} — ${it.session_name}" } ?: "Loading...",
            lap = if (!isLive) "REPLAY" else ""
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AppColors.background,
            contentColor = AppColors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AppColors.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontSize = 12.sp,
                            color = if (selectedTab == index) AppColors.primary else AppColors.onSurfaceVariant
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> TrackMapTab(positions, drivers, stints)
            1 -> TimingTab(positions, drivers, laps, stints, intervals)
            2 -> RaceControlTab(raceControl)
        }
    }
}

@Composable
fun TrackMapTab(
    positions: List<Position>,
    drivers: List<Driver>,
    stints: List<Stint>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AppColors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Box(Modifier.fillMaxSize().padding(12.dp)) {
                TrackCanvas(positions, drivers)
            }
        }

        Spacer(Modifier.height(12.dp))

        SectionLabel("Positions")
        positions.take(5).forEach { pos ->
            val driver = drivers.find { it.driver_number == pos.driver_number }
            val stint = stints.filter { it.driver_number == pos.driver_number }.maxByOrNull { it.stint_number }
            QuickTimingRow(pos, driver, stint)
        }
    }
}

@Composable
fun TrackCanvas(positions: List<Position>, drivers: List<Driver>) {
    val trackColor = AppColors.surfaceVariant
    val trackSurfaceColor = AppColors.surfaceVariant
    val accentColor = AppColors.primary
    val sectorColor = F1Yellow

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val trackPath = Path().apply {
            moveTo(w * 0.1f, h * 0.8f)
            lineTo(w * 0.1f, h * 0.5f)
            quadraticBezierTo(w * 0.1f, h * 0.3f, w * 0.2f, h * 0.25f)
            lineTo(w * 0.28f, h * 0.22f)
            quadraticBezierTo(w * 0.36f, h * 0.2f, w * 0.38f, h * 0.13f)
            lineTo(w * 0.42f, h * 0.07f)
            quadraticBezierTo(w * 0.46f, h * 0.02f, w * 0.54f, h * 0.02f)
            lineTo(w * 0.68f, h * 0.04f)
            quadraticBezierTo(w * 0.78f, h * 0.06f, w * 0.81f, h * 0.13f)
            lineTo(w * 0.83f, h * 0.2f)
            quadraticBezierTo(w * 0.85f, h * 0.27f, w * 0.83f, h * 0.38f)
            lineTo(w * 0.8f, h * 0.48f)
            quadraticBezierTo(w * 0.77f, h * 0.55f, w * 0.71f, h * 0.57f)
            lineTo(w * 0.63f, h * 0.59f)
            quadraticBezierTo(w * 0.57f, h * 0.61f, w * 0.54f, h * 0.67f)
            lineTo(w * 0.5f, h * 0.73f)
            quadraticBezierTo(w * 0.46f, h * 0.8f, w * 0.38f, h * 0.8f)
            lineTo(w * 0.26f, h * 0.78f)
            quadraticBezierTo(w * 0.14f, h * 0.77f, w * 0.1f, h * 0.8f)
            close()
        }

        drawPath(trackPath, color = trackColor, style = Stroke(width = 22f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(trackPath, color = trackSurfaceColor.copy(alpha = 0.6f), style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawLine(
            color = accentColor,
            start = Offset(w * 0.1f, h * 0.8f),
            end = Offset(w * 0.1f, h * 0.72f),
            strokeWidth = 4f
        )

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (sectorColor.alpha * 255).toInt(),
                    (sectorColor.red * 255).toInt(),
                    (sectorColor.green * 255).toInt(),
                    (sectorColor.blue * 255).toInt()
                )
                textSize = 28f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            drawText("S1", w * 0.04f, h * 0.45f, paint)
            drawText("S2", w * 0.55f, h * 0.05f, paint)
            drawText("S3", w * 0.78f, h * 0.92f, paint)
        }

        val trackPoints = generateTrackPoints(w, h, 20)
        positions.take(20).forEachIndexed { index, pos ->
            val driver = drivers.find { it.driver_number == pos.driver_number }
            val teamColor = driver?.team_colour?.let {
                try { Color(android.graphics.Color.parseColor("#$it")) } catch (e: Exception) { Color(0xFF888888) }
            } ?: Color(0xFF888888)

            val point = trackPoints.getOrElse(index) { Offset(w * 0.5f, h * 0.5f) }

            drawCircle(color = teamColor.copy(alpha = 0.3f), radius = 14f, center = point)
            drawCircle(color = teamColor, radius = 9f, center = point)
            drawCircle(color = Color.Black, radius = 9f, center = point, style = Stroke(width = 1.5f))

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 18f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(
                    driver?.name_acronym ?: pos.driver_number.toString(),
                    point.x,
                    point.y - 14f,
                    paint
                )
            }
        }
    }
}

fun generateTrackPoints(w: Float, h: Float, count: Int): List<Offset> {
    val points = listOf(
        Offset(w * 0.1f, h * 0.65f), Offset(w * 0.1f, h * 0.5f), Offset(w * 0.12f, h * 0.38f),
        Offset(w * 0.18f, h * 0.28f), Offset(w * 0.26f, h * 0.23f), Offset(w * 0.34f, h * 0.2f),
        Offset(w * 0.38f, h * 0.13f), Offset(w * 0.42f, h * 0.07f), Offset(w * 0.5f, h * 0.03f),
        Offset(w * 0.62f, h * 0.04f), Offset(w * 0.72f, h * 0.07f), Offset(w * 0.8f, h * 0.13f),
        Offset(w * 0.83f, h * 0.22f), Offset(w * 0.82f, h * 0.35f), Offset(w * 0.78f, h * 0.5f),
        Offset(w * 0.7f, h * 0.57f), Offset(w * 0.6f, h * 0.6f), Offset(w * 0.52f, h * 0.68f),
        Offset(w * 0.38f, h * 0.79f), Offset(w * 0.22f, h * 0.78f)
    )
    return points.take(count)
}

@Composable
fun QuickTimingRow(pos: Position, driver: Driver?, stint: Stint?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${pos.position}",
            color = if (pos.position == 1) AppColors.primary else AppColors.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        TeamColorBar(driver?.team_name ?: "")
        Spacer(Modifier.width(8.dp))
        Text(
            driver?.name_acronym ?: pos.driver_number.toString(),
            color = AppColors.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp)
        )
        stint?.let {
            TyreIndicator(it.compound)
            Spacer(Modifier.width(4.dp))
            Text("L${it.lap_start}", color = AppColors.onSurfaceVariant, fontSize = 11.sp)
        }
    }
    Divider(color = AppColors.outline, thickness = 0.5.dp)
}

@Composable
fun TimingTab(
    positions: List<Position>,
    drivers: List<Driver>,
    laps: List<Lap>,
    stints: List<Stint>,
    intervals: List<Interval>
) {
    val fastestLap = laps.minByOrNull { it.lap_duration ?: Double.MAX_VALUE }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(positions) { pos ->
            val driver = drivers.find { it.driver_number == pos.driver_number }
            val lap = laps.find { it.driver_number == pos.driver_number }
            val stint = stints.filter { it.driver_number == pos.driver_number }.maxByOrNull { it.stint_number }
            val interval = intervals.find { it.driver_number == pos.driver_number }
            val isFastest = lap?.lap_number != null && lap.lap_number == fastestLap?.lap_number
                    && lap.driver_number == fastestLap?.driver_number

            TimingRow(pos, driver, lap, stint, interval, isFastest)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun TimingRow(
    pos: Position,
    driver: Driver?,
    lap: Lap?,
    stint: Stint?,
    interval: Interval?,
    isFastest: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AppColors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${pos.position}",
                color = if (pos.position == 1) AppColors.primary else AppColors.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(22.dp)
            )
            TeamColorBar(driver?.team_name ?: "")
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        driver?.name_acronym ?: pos.driver_number.toString(),
                        color = AppColors.onBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    stint?.let {
                        Spacer(Modifier.width(6.dp))
                        TyreIndicator(it.compound)
                    }
                }
                Text(
                    interval?.gap_to_leader?.let { if (it == 0.0) "Leader" else "+${"%.3f".format(it)}s" } ?: "",
                    color = AppColors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Text(
                formatLapTime(lap?.lap_duration),
                color = if (isFastest) F1Purple else AppColors.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = if (isFastest) FontWeight.Bold else FontWeight.Normal,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
fun RaceControlTab(messages: List<RaceControlMessage>) {
    if (messages.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No race control messages yet", color = AppColors.onSurfaceVariant)
        }
        return
    }

    val latestAlert = messages.firstOrNull {
        it.flag in listOf("SAFETY_CAR", "RED", "VIRTUAL_SAFETY_CAR")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        latestAlert?.let {
            item { AlertBanner(it) }
        }
        item { SectionLabel("Messages") }
        items(messages) { msg ->
            RaceControlCard(msg)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun AlertBanner(msg: RaceControlMessage) {
    val bgColor = when (msg.flag) {
        "RED" -> Color(0xFF3A0000)
        "SAFETY_CAR" -> Color(0xFF2A1400)
        "VIRTUAL_SAFETY_CAR" -> Color(0xFF2A2000)
        else -> AppColors.surface
    }
    val textColor = when (msg.flag) {
        "RED" -> Color(0xFFFF4444)
        "SAFETY_CAR" -> F1Orange
        "VIRTUAL_SAFETY_CAR" -> F1Yellow
        else -> AppColors.onBackground
    }
    val emoji = when (msg.flag) {
        "RED" -> "🔴"
        "SAFETY_CAR" -> "🚗"
        "VIRTUAL_SAFETY_CAR" -> "🟡"
        else -> "⚠️"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(msg.message, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                msg.lap_number?.let {
                    Text("Lap $it", color = textColor.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun RaceControlCard(msg: RaceControlMessage) {
    val borderColor = when (msg.flag) {
        "GREEN" -> F1Green
        "YELLOW", "DOUBLE_YELLOW" -> F1Yellow
        "RED" -> Color(0xFFFF4444)
        "SAFETY_CAR" -> F1Orange
        "VIRTUAL_SAFETY_CAR" -> F1Yellow
        "CHEQUERED" -> AppColors.onBackground
        else -> when {
            msg.category == "Drs" -> Color(0xFF00AAFF)
            msg.category == "Flag" -> F1Yellow
            msg.message.contains("PENALTY", ignoreCase = true) -> F1Purple
            else -> AppColors.onSurfaceVariant
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AppColors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(0.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .background(
                        color = borderColor,
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    )
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(msg.message, color = AppColors.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        msg.lap_number?.let { append("Lap $it · ") }
                        append(msg.date.take(19).replace("T", " "))
                    },
                    color = AppColors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun Modifier.tabIndicatorOffset(tabPosition: androidx.compose.material3.TabPosition): Modifier = this
