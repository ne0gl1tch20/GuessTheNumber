package com.jarrlyyy.guessthenumber

import com.jarrlyyy.guessthenumber.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRouteSmokeTest {
    @Test
    fun changelogRouteIsStable() {
        assertEquals("changelog_viewer", Screen.ChangelogViewer.route)
        assertEquals("Changelog", Screen.ChangelogViewer.title)
    }

    @Test
    fun coreRoutesAreUnique() {
        val routes = listOf(
            Screen.Play, Screen.Upgrade, Screen.Shop, Screen.More,
            Screen.Prestige, Screen.Ultra, Screen.Arcade, Screen.Stats,
            Screen.Settings, Screen.About, Screen.DevSettings,
            Screen.SaveSlots, Screen.ChangelogViewer
        ).map { it.route }

        assertEquals(routes.size, routes.distinct().size)
        assertTrue(routes.contains(Screen.ChangelogViewer.route))
    }
}
