package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class DesktopStartupLaunchTest {
    @Test
    fun startupIsSupportedOnlyOnWindows() {
        assertTrue(DesktopStartupLaunch.isSupported("Windows 10"))
        assertTrue(DesktopStartupLaunch.isSupported("Windows 11"))
        assertFalse(DesktopStartupLaunch.isSupported("Linux"))
        assertFalse(DesktopStartupLaunch.isSupported("Mac OS X"))
    }

    @Test
    fun startupCommandQuotesPackagedPathsWithSpaces() {
        assertEquals(
            "\"C:\\Program Files\\Guess The Number\\GuessTheNumber.exe\"",
            DesktopStartupLaunch.startupCommand(Path.of("C:\\Program Files\\Guess The Number\\GuessTheNumber.exe"))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun startupCommandRejectsNonExecutableTargets() {
        DesktopStartupLaunch.startupCommand(Path.of("C:\\Program Files\\Guess The Number\\app.jar"))
    }
}
