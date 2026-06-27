package com.boxbox.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ---- Team accent colors (these are now the PRIMARY app color) ----
// Covers the full 2026 grid. Some teams are kept under both their old and new names
// since Jolpica returns whichever name was current for the requested season:
//   - Racing Bulls: "RB" (2024-2025) -> "Racing Bulls" (2026+)
//   - Kick Sauber -> rebranded to "Audi" for 2026
//   - "Cadillac" is the new 2026 entrant (Perez, Bottas)
val teamAccentColors = mapOf(
    "Red Bull Racing" to Color(0xFF3671C6),
    "Ferrari" to Color(0xFFE10600),
    "McLaren" to Color(0xFFFF8000),
    "Mercedes" to Color(0xFF27F4D2),
    "Aston Martin" to Color(0xFF229971),
    "Alpine" to Color(0xFF0093CC),
    "Williams" to Color(0xFF64C4FF),
    "Racing Bulls" to Color(0xFF6692FF),
    "RB" to Color(0xFF6692FF),
    "Kick Sauber" to Color(0xFF52E252),
    "Audi" to Color(0xFFD40000),
    "Haas" to Color(0xFFB6BABD),
    "Cadillac" to Color(0xFF8C8C8C)
)

val defaultAccent = Color(0xFFE10600) // F1 red default

fun resolveTeamAccent(teamName: String): Color {
    if (teamName.isBlank()) return defaultAccent
    // Exact match first (case-insensitive) to avoid accidental substring collisions
    // between unrelated team names - e.g. a loose contains() check can fail to connect
    // "Racing Bulls" with its own entry depending on key ordering/substrings.
    teamAccentColors.forEach { (key, color) ->
        if (key.equals(teamName, ignoreCase = true)) return color
    }
    // Fall back to a looser containment check for partial/legacy naming variants.
    teamAccentColors.forEach { (key, color) ->
        if (teamName.contains(key, ignoreCase = true) ||
            key.contains(teamName, ignoreCase = true)) {
            return color
        }
    }
    return defaultAccent
}

// ---- Global theme state (in-memory, survives navigation, resets on process death) ----
object ThemeState {
    var favouriteTeam by mutableStateOf("")
    var isDarkMode by mutableStateOf(true)

    val accentColor: Color
        get() = resolveTeamAccent(favouriteTeam)
}

// Fixed dark/light neutrals (independent of team)
// Dark palette matches the official F1 app's signature deep navy look.
private val DarkNeutrals = NeutralPalette(
    background = Color(0xFF0A0E1A),
    surface = Color(0xFF15182A),
    surfaceVariant = Color(0xFF1F2438),
    onBackground = Color(0xFFEDEEF2),
    onSurfaceVariant = Color(0xFF8A8FA3),
    outline = Color(0xFF252A40)
)

// Light palette uses a warm, soft off-white/cream tone (closer to the official F1 app's
// light mode) instead of plain white or a cold gray, with enough contrast between
// background/surface/surfaceVariant that cards and sections are visually distinguishable.
private val LightNeutrals = NeutralPalette(
    background = Color(0xFFF2EFEA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8E3DC),
    onBackground = Color(0xFF1C1B19),
    onSurfaceVariant = Color(0xFF6B6862),
    outline = Color(0xFFDDD8D0)
)

data class NeutralPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val outline: Color
)

@Composable
fun BoxBoxTheme(content: @Composable () -> Unit) {
    val accent = ThemeState.accentColor
    val neutrals = if (ThemeState.isDarkMode) DarkNeutrals else LightNeutrals

    // Pick readable "on" color for the accent (white text usually, dark for very light accents like Williams blue is fine with white too)
    val onAccent = if (accent.luminance() > 0.6f) Color.Black else Color.White

    val colorScheme = if (ThemeState.isDarkMode) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.25f),
            onPrimaryContainer = accent,
            secondary = accent,
            background = neutrals.background,
            surface = neutrals.surface,
            surfaceVariant = neutrals.surfaceVariant,
            onBackground = neutrals.onBackground,
            onSurface = neutrals.onBackground,
            onSurfaceVariant = neutrals.onSurfaceVariant,
            error = Color(0xFFFF5252),
            outline = neutrals.outline
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.22f),
            onPrimaryContainer = accent,
            secondary = accent,
            background = neutrals.background,
            surface = neutrals.surface,
            surfaceVariant = neutrals.surfaceVariant,
            onBackground = neutrals.onBackground,
            onSurface = neutrals.onBackground,
            onSurfaceVariant = neutrals.onSurfaceVariant,
            error = Color(0xFFD32F2F),
            outline = neutrals.outline
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// ---- Backward-compatible accessors used throughout the app ----
object AppColors {
    val primary: Color @Composable get() = MaterialTheme.colorScheme.primary
    val background: Color @Composable get() = MaterialTheme.colorScheme.background
    val surface: Color @Composable get() = MaterialTheme.colorScheme.surface
    val surfaceVariant: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val onBackground: Color @Composable get() = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val outline: Color @Composable get() = MaterialTheme.colorScheme.outline
    val onPrimary: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
}

// Status colors stay fixed regardless of team/theme (universal meaning: yellow flag is always yellow)
val F1Purple = Color(0xFFC44BFF)
val F1Yellow = Color(0xFFEF9F27)
val F1Green = Color(0xFF4CAF50)
val F1Orange = Color(0xFFFF9800)
