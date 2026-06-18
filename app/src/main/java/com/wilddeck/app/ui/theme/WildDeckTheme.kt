package com.wilddeck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6547),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC5EBD6),
    onPrimaryContainer = Color(0xFF083824),
    secondary = Color(0xFF8A4F24),
    secondaryContainer = Color(0xFFFFDCC5),
    background = Color(0xFFF6F2E8),
    surface = Color(0xFFFFFBF2),
    surfaceVariant = Color(0xFFE5E0D5),
    onSurface = Color(0xFF20221E),
    outline = Color(0xFF74786F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD2AD),
    primaryContainer = Color(0xFF0B4E33),
    secondary = Color(0xFFFFB783),
    background = Color(0xFF111410),
    surface = Color(0xFF191D18),
    surfaceVariant = Color(0xFF3E443D)
)

@Composable
fun WildDeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
