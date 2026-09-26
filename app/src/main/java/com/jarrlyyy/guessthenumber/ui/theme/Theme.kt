package com.jarrlyyy.guessthenumber.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = SecondaryPurple,
    tertiary = TertiaryPink,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = OnPrimary
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = SecondaryPurple,
    tertiary = TertiaryPink,
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF121212),
    onPrimary = OnPrimary,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    secondary = SecondaryPurple,
    tertiary = TertiaryPink
)

@Composable
fun GuessTheNumberTheme(
    themeMode: String = "System",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "Dark", "AMOLED" -> true
        "Light" -> false
        else -> systemDark
    }

    val colorScheme = when (themeMode) {
        "AMOLED" -> AmoledDarkColorScheme
        "Dark" -> DarkColorScheme
        "Light" -> LightColorScheme
        else -> if (systemDark) DarkColorScheme else LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
