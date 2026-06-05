package com.boxbox.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// F1 Brand Colors
val F1Red = Color(0xFFE10600)
val F1DarkRed = Color(0xFF9B0000)
val F1Black = Color(0xFF0F0F0F)
val F1DarkGray = Color(0xFF1A1A1A)
val F1MidGray = Color(0xFF2A2A2A)
val F1LightGray = Color(0xFF555555)
val F1White = Color(0xFFE8E8E8)
val F1Purple = Color(0xFFC44BFF) // fastest lap
val F1Yellow = Color(0xFFEF9F27) // yellow flag / medium tyre
val F1Green = Color(0xFF4CAF50) // green flag
val F1Orange = Color(0xFFFF9800) // safety car

// Team Colors
val RedBullColor = Color(0xFF3671C6)
val FerrariColor = Color(0xFFE8002D)
val McLarenColor = Color(0xFFFF8000)
val MercedesColor = Color(0xFF27F4D2)
val AstonMartinColor = Color(0xFF229971)
val AlpineColor = Color(0xFF0093CC)
val WilliamsColor = Color(0xFF64C4FF)
val RBColor = Color(0xFF6692FF)
val SauberColor = Color(0xFF52E252)
val HaasColor = Color(0xFFB6BABD)

private val DarkColorScheme = darkColorScheme(
    primary = F1Red,
    onPrimary = Color.White,
    primaryContainer = F1DarkRed,
    secondary = F1Yellow,
    background = F1Black,
    surface = F1DarkGray,
    surfaceVariant = F1MidGray,
    onBackground = F1White,
    onSurface = F1White,
    onSurfaceVariant = F1LightGray,
    error = F1Red,
    outline = F1MidGray
)

@Composable
fun BoxBoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
