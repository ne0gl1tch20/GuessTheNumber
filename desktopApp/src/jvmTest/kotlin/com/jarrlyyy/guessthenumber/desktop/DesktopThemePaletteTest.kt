package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class DesktopThemePaletteTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun printAiContextReminder() {
            println(
                """
                |GTN WINDOWS PARITY REMINDER:
                |Android is the source of truth. Windows must feel like the same app, not a redesign.
                |Preserve features, visuals, gameplay, localization and save compatibility.
                |Adapt only genuine desktop needs. Do not add unnecessary rewrites or complexity.
                |Theme tests cover theme tokens only; they do NOT prove full UI/runtime parity.
                """.trimMargin()
            )
        }
    }

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
    fun savedAndroidThemeModeAndCustomColorsAreApplied() {
        val custom = desktopColorScheme(
            themeMode = "Light",
            systemIsDark = true,
            primaryHex = "#123456",
            secondaryHex = "#234567",
            tertiaryHex = "#345678"
        )
        assertTrue(custom.primary == Color(0xFF123456))
        assertTrue(custom.secondary == Color(0xFF234567))
        assertTrue(custom.tertiary == Color(0xFF345678))
        assertTrue(custom.background == DesktopThemePalette.light.background)
    }

    @Test
    fun amoledModeUsesBlackBackgroundAndSurface() {
        val amoled = desktopColorScheme(
            themeMode = "AMOLED",
            systemIsDark = false,
            primaryHex = "#7C4DFF",
            secondaryHex = "#00BCD4",
            tertiaryHex = "#00C853"
        )
        assertTrue(amoled.background == Color.Black)
        assertTrue(amoled.surface == Color.Black)
        assertTrue(amoled.surfaceVariant == Color(0xFF121212))
    }

    @Test
    fun invalidCustomColorsFallBackToAndroidDefaults() {
        val scheme = desktopColorScheme(
            themeMode = "Dark",
            systemIsDark = false,
            primaryHex = "not-a-color",
            secondaryHex = "#00BCD4",
            tertiaryHex = "#00C853"
        )
        assertTrue(scheme.primary == Color(0xFF7C4DFF))
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
