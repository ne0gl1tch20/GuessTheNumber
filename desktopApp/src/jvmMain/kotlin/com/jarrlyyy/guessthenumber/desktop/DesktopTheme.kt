package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal object DesktopThemePalette {
    val dark = darkColorScheme(
        primary = Color(0xFFCE93D8),
        secondary = Color(0xFF80DEEA),
        tertiary = Color(0xFF69F0AE),
        background = Color(0xFF0D0B1E),
        surface = Color(0xFF191632),
        surfaceVariant = Color(0xFF25214D),
        onBackground = Color(0xFFE6E1F9),
        onSurface = Color(0xFFE6E1F9)
    )

    val light = lightColorScheme(
        primary = Color(0xFF7B1FA2),
        secondary = Color(0xFF006874),
        tertiary = Color(0xFF007A3D),
        background = Color(0xFFFAF8FF),
        surface = Color(0xFFF1EEFF),
        surfaceVariant = Color(0xFFE5DEFF),
        onBackground = Color(0xFF1D1B20),
        onSurface = Color(0xFF1D1B20)
    )
}

internal fun desktopNavigationContainerColor(
    selected: Boolean,
    colorScheme: ColorScheme
): Color = if (selected) colorScheme.primary else colorScheme.surfaceVariant

internal fun desktopNavigationContentColor(
    selected: Boolean,
    colorScheme: ColorScheme
): Color = if (selected) colorScheme.onPrimary else colorScheme.onSurface

/**
 * Reads the Windows app-theme preference from the current user's personalization registry.
 * A null result means Windows is unavailable or the preference could not be read.
 */
internal fun parseWindowsThemePreference(
    output: String,
    valueName: String = "AppsUseLightTheme"
): Boolean? {
    val pattern = Regex(
        """(?im)^\s*${Regex.escape(valueName)}\s+REG_DWORD\s+0x([0-9a-f]+)\s*$"""
    )
    val value = pattern.find(output)?.groupValues?.getOrNull(1)?.toIntOrNull(16) ?: return null
    return value == 0
}

private fun readWindowsDarkThemePreference(): Boolean? {
    if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return null

    val registryPath = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"
    return listOf("AppsUseLightTheme", "SystemUsesLightTheme").firstNotNullOfOrNull { valueName ->
        val output = runCatching {
            val process = ProcessBuilder("reg", "query", registryPath, "/v", valueName)
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return@runCatching null
            }
            process.inputStream.bufferedReader().use { it.readText() }
        }.getOrNull()
        output?.let { parseWindowsThemePreference(it, valueName) }
    }
}

/** Follows the Windows app-theme preference while the desktop window is open. */
@Composable
fun DesktopTheme(content: @Composable () -> Unit) {
    val fallbackIsDark = isSystemInDarkTheme()
    val isDark by produceState(initialValue = fallbackIsDark) {
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            while (true) {
                val windowsPreference = withContext(Dispatchers.IO) {
                    readWindowsDarkThemePreference()
                }
                if (windowsPreference != null) value = windowsPreference
                delay(2_000)
            }
        }
    }
    MaterialTheme(
        colorScheme = if (isDark) DesktopThemePalette.dark else DesktopThemePalette.light,
        content = content
    )
}
