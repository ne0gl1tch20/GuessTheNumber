package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DesktopDarkColors = darkColorScheme(
    primary = Color(0xFFCE93D8),
    secondary = Color(0xFF80DEEA),
    tertiary = Color(0xFF69F0AE),
    background = Color(0xFF0D0B1E),
    surface = Color(0xFF191632),
    surfaceVariant = Color(0xFF25214D),
    onBackground = Color(0xFFE6E1F9),
    onSurface = Color(0xFFE6E1F9)
)

private val DesktopLightColors = lightColorScheme(
    primary = Color(0xFF7B1FA2),
    secondary = Color(0xFF006874),
    tertiary = Color(0xFF007A3D),
    background = Color(0xFFFAF8FF),
    surface = Color(0xFFF1EEFF),
    surfaceVariant = Color(0xFFE5DEFF),
    onBackground = Color(0xFF1D1B20),
    onSurface = Color(0xFF1D1B20)
)

/** Uses the operating system's light/dark preference until desktop theme settings exist. */
@Composable
fun DesktopTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DesktopDarkColors else DesktopLightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
