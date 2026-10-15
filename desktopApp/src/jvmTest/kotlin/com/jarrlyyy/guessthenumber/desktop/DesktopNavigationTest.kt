package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DesktopNavigationTest {
    @Test
    fun startsOnPlay() {
        val controller = DesktopShellController()

        assertEquals(DesktopDestination.PLAY, controller.state.destination)
        assertNull(controller.state.message)
    }

    @Test
    fun navigationChangesDestinationAndClearsOldMessage() {
        val controller = DesktopShellController()
        controller.showMessage("Could not load a screen")

        controller.navigate(DesktopDestination.ABOUT)

        assertEquals(DesktopDestination.ABOUT, controller.state.destination)
        assertNull(controller.state.message)
    }

    @Test
    fun blankMessagesAreClearedAndMessagesAreTrimmed() {
        val controller = DesktopShellController()

        controller.showMessage("  Saved locally  ")
        assertEquals("Saved locally", controller.state.message)

        controller.showMessage("  ")
        assertNull(controller.state.message)
    }
    @Test
    fun ctrlNumberShortcutsMapToDestinations() {
        assertEquals(DesktopDestination.PLAY, DesktopDestination.fromShortcut(Key.One, ctrlPressed = true))
        assertEquals(DesktopDestination.ABOUT, DesktopDestination.fromShortcut(Key.Two, ctrlPressed = true))
    }

    @Test
    fun shortcutsRequireControlAndIgnoreUnknownKeys() {
        assertNull(DesktopDestination.fromShortcut(Key.One, ctrlPressed = false))
        assertNull(DesktopDestination.fromShortcut(Key.Three, ctrlPressed = true))
    }

}
