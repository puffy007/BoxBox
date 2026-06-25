package com.boxbox.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ---- Team accent colors (same as before, but these are now the PRIMARY app color) ----
val teamAccentColors = mapOf(
    "Red Bull Racing" to Color(0xFF3671C6),
    "Ferrari" to Color(0xFFE10600),
    "McLaren" to Color(0xFFFF8000),
    "Mercedes" to Color(0xFF27F4D2),
    "Aston Martin" to Color(0xFF229971),
    "Alpine" to Color(0xFF0093CC),
    "Williams" to Color(0xFF64C4FF),
    "RB" to Color(0xFF6692FF),
    "Kick Sauber" to Color(0xFF52E252),
    "Haas" to Color(0xFFB6BABD)
)

val defaultAccent = Color(0xFFE10600) // F1 red default

fun resolveTeamAccent(teamName: String): Color {
    if (teamName.isBlank()) return defaultAccent
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
private val DarkNeutrals = NeutralPalette(
    background = Color(0xFF0F0F0F),
    surface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFF2A2A2A),
    onBackground = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF888888),
    outline = Color(0xFF2A2A2A)
)

private val LightNeutrals = NeutralPalette(
    background = Color(0xFFF7F7F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEDF0),
    onBackground = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF666666),
    outline = Color(0xFFE0E0E0)
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
            primaryContainer = accent.copy(alpha = 0.15f),
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
// Instead of hardcoded F1Red / F1Black etc, screens should now read from MaterialTheme.colorScheme
// But to avoid rewriting every screen at once, expose these as Composable-safe current-theme reads:

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
