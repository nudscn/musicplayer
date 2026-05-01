package com.example.musicplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Accent,
    secondary = AccentWarm,
    background = Obsidian,
    surface = Panel,
    surfaceVariant = PanelAlt,
    onPrimary = Obsidian,
    onSecondary = Obsidian,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

private val LightColors = lightColorScheme(
    primary = Accent,
    secondary = AccentWarm,
)

@Composable
fun MusicPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
