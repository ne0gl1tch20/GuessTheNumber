package com.jarrlyyy.guessthenumber.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ExpressiveDarkColorScheme = darkColorScheme(
    primary = PrimaryNeonPurple,
    onPrimary = OnPrimaryNeon,
    primaryContainer = PrimaryContainerNeon,
    onPrimaryContainer = OnPrimaryContainerNeon,
    secondary = SecondaryElectricCyan,
    onSecondary = OnSecondaryElectric,
    secondaryContainer = SecondaryContainerElectric,
    tertiary = TertiaryEmerald,
    onTertiary = OnTertiaryEmerald,
    tertiaryContainer = TertiaryContainerEmerald,
    background = ExpressiveBackgroundDark,
    surface = ExpressiveSurfaceDark,
    surfaceVariant = ExpressiveSurfaceVariantDark,
    onBackground = Color(0xFFE6E1F9),
    onSurface = Color(0xFFE6E1F9)
)

private val ExpressiveLightColorScheme = lightColorScheme(
    primary = PrimaryNeonPurple,
    onPrimary = OnPrimaryNeon,
    primaryContainer = PrimaryContainerNeon,
    onPrimaryContainer = OnPrimaryContainerNeon,
    secondary = SecondaryElectricCyan,
    onSecondary = OnSecondaryElectric,
    secondaryContainer = SecondaryContainerElectric,
    tertiary = TertiaryEmerald,
    onTertiary = OnTertiaryEmerald,
    tertiaryContainer = TertiaryContainerEmerald,
    background = ExpressiveBackgroundLight,
    surface = ExpressiveSurfaceLight,
    surfaceVariant = ExpressiveSurfaceVariantLight,
    onBackground = Color(0xFF1D1B20),
    onSurface = Color(0xFF1D1B20)
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = PrimaryNeonPurple,
    onPrimary = OnPrimaryNeon,
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF121212),
    onBackground = Color.White,
    onSurface = Color.White
)

// Google Material 3 Expressive Shapes (Oversized rounded corners and pill shapes)
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(24.dp),
    medium = RoundedCornerShape(32.dp),
    large = RoundedCornerShape(44.dp),
    extraLarge = RoundedCornerShape(56.dp)
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
        "Dark" -> ExpressiveDarkColorScheme
        "Light" -> ExpressiveLightColorScheme
        else -> {
            if (systemDark) ExpressiveDarkColorScheme else ExpressiveLightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ExpressiveShapes,
        content = content
    )
}

