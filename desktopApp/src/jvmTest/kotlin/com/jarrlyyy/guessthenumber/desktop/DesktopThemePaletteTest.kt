package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopThemePaletteTest {
    @Test
    fun lightSurfaceTextMeetsReadableContrast() {
        assertTrue(contrastRatio(DesktopThemePalette.light.onSurface, DesktopThemePalette.light.surface) >= 4.5)
        assertTrue(contrastRatio(DesktopThemePalette.light.onBackground, DesktopThemePalette.light.background) >= 4.5)
    }

    @Test
    fun darkSurfaceTextMeetsReadableContrast() {
        assertTrue(contrastRatio(DesktopThemePalette.dark.onSurface, DesktopThemePalette.dark.surface) >= 4.5)
        assertTrue(contrastRatio(DesktopThemePalette.dark.onBackground, DesktopThemePalette.dark.background) >= 4.5)
    }

    @Test
    fun bothPalettesProvideDistinctSurfaceAndBackgroundColors() {
        assertTrue(DesktopThemePalette.light.surface != DesktopThemePalette.light.background)
        assertTrue(DesktopThemePalette.dark.surface != DesktopThemePalette.dark.background)
        // Android's ExpressiveDarkColorScheme and ExpressiveLightColorScheme both use PrimaryNeonPurple.
        assertTrue(DesktopThemePalette.light.primary == Color(0xFF9C27B0))
        assertTrue(DesktopThemePalette.dark.primary == Color(0xFF9C27B0))
    }

    @Test
    fun navigationButtonColorsHaveReadableContrastInBothThemes() {
        listOf(DesktopThemePalette.light, DesktopThemePalette.dark).forEach { scheme ->
            assertTrue(
                contrastRatio(
                    desktopNavigationContentColor(selected = true, colorScheme = scheme),
                    desktopNavigationContainerColor(selected = true, colorScheme = scheme)
                ) >= 4.5
            )
            assertTrue(
                contrastRatio(
                    desktopNavigationContentColor(selected = false, colorScheme = scheme),
                    desktopNavigationContainerColor(selected = false, colorScheme = scheme)
                ) >= 4.5
            )
        }
    }

    @Test
    fun parsesWindowsLightAndDarkAppPreferences() {
        assertTrue(
            parseWindowsThemePreference(
                "    AppsUseLightTheme    REG_DWORD    0x0",
                "AppsUseLightTheme"
            ) == true
        )
        assertTrue(
            parseWindowsThemePreference(
                "    AppsUseLightTheme    REG_DWORD    0x1",
                "AppsUseLightTheme"
            ) == false
        )
    }

    @Test
    fun unknownWindowsThemePreferenceFallsBackCleanly() {
        assertTrue(
            parseWindowsThemePreference("AppsUseLightTheme REG_SZ 0x0") == null
        )
        assertTrue(
            parseWindowsThemePreference("SystemUsesLightTheme REG_DWORD 0x0") == null
        )
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val foregroundLuminance = foreground.luminance().toDouble()
        val backgroundLuminance = background.luminance().toDouble()
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
