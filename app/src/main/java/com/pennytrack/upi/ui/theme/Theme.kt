package com.pennytrack.upi.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFFF7F7F2)
val AppSurface = Color(0xFFFFFFFF)
val AppInk = Color(0xFF242421)
val AppMuted = Color(0xFF6B6B61)
val AppAccent = Color(0xFF1D7A68)
val AppAmber = Color(0xFFC4821A)
val AppRed = Color(0xFFB2463A)

private val PennyScheme: ColorScheme = lightColorScheme(
    primary = AppAccent,
    onPrimary = Color.White,
    secondary = AppAmber,
    onSecondary = AppInk,
    error = AppRed,
    background = AppBackground,
    onBackground = AppInk,
    surface = AppSurface,
    onSurface = AppInk,
    surfaceVariant = Color(0xFFE8E8DF),
    onSurfaceVariant = AppMuted,
    outline = Color(0xFFD6D6CA)
)

@Composable
fun PennyTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PennyScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
