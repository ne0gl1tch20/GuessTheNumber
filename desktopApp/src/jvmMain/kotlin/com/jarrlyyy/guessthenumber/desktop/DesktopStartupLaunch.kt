package com.jarrlyyy.guessthenumber.desktop

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.nio.file.Files
import java.nio.file.Path

/**
 * Windows login-start integration. This is deliberately isolated from common game
 * settings and only registers the packaged app launcher for the current user.
 */
internal object DesktopStartupLaunch {
    private const val RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val VALUE_NAME = "GuessTheNumber"

    fun isSupported(osName: String = System.getProperty("os.name")): Boolean =
        osName.startsWith("Windows", ignoreCase = true)

    fun isEnabled(): Boolean {
        if (!isSupported()) return false
        return runCatching {
            Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE_NAME)
        }.getOrDefault(false)
    }

    fun setEnabled(enabled: Boolean): Result<Unit> = runCatching {
        check(isSupported()) { "Automatic startup is only available on Windows." }
        if (enabled) {
            val launcher = resolvePackagedLauncher()
                ?: error("Automatic startup is available after installing the Windows desktop app.")
            Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, RUN_KEY)
            Advapi32Util.registrySetStringValue(
                WinReg.HKEY_CURRENT_USER,
                RUN_KEY,
                VALUE_NAME,
                startupCommand(launcher)
            )
        } else if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE_NAME)) {
            Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE_NAME)
        }
    }

    internal fun startupCommand(executable: Path): String {
        val normalized = executable.toAbsolutePath().normalize().toString()
        require(normalized.endsWith(".exe", ignoreCase = true)) {
            "The startup target must be a Windows executable."
        }
        require(!normalized.contains('"')) { "The startup executable path is invalid." }
        return "\"$normalized\""
    }

    private fun resolvePackagedLauncher(): Path? {
        val configured = System.getProperty("jpackage.app-path")
            ?.takeIf(String::isNotBlank)
            ?.let { Path.of(it) }
            ?.toAbsolutePath()
            ?.normalize()
            ?.takeIf { Files.isRegularFile(it) && it.fileName.toString().endsWith(".exe", true) }
        if (configured != null) return configured

        // Never register java.exe/javaw.exe: that would break after an app/runtime update.
        return ProcessHandle.current().info().command()
            .filter(String::isNotBlank)
            .map { Path.of(it) }
            .map { it.toAbsolutePath().normalize() }
            .filter { Files.isRegularFile(it) && it.fileName.toString().equals("GuessTheNumber.exe", true) }
            .orElse(null)
    }
}
