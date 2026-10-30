package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DesktopGameSectionRouteTest {
    @Test
    fun playerFacingSectionsResolveToTheCanonicalSharedWebRoutes() {
        val expected = mapOf(
            DesktopGameSection.PLAY to "play",
            DesktopGameSection.UPGRADES to "upgrades",
            DesktopGameSection.SHOP to "shop",
            DesktopGameSection.PRESTIGE to "prestige",
            DesktopGameSection.ULTRA to "ultra",
            DesktopGameSection.TALENT to "talent",
            DesktopGameSection.SETTINGS to "settings",
            DesktopGameSection.ACHIEVEMENTS to "rewards",
            DesktopGameSection.WORLD_MAP to "worlds",
            DesktopGameSection.MUTATORS to "mutators",
            DesktopGameSection.RELICS to "relics",
            DesktopGameSection.HOME_BASE to "homebase",
            DesktopGameSection.CODEX to "codex",
            DesktopGameSection.ENDGAME to "journey",
            DesktopGameSection.STATS to "stats",
            DesktopGameSection.ARCADE to "arcade",
            DesktopGameSection.LUCKY_STAKE to "stake"
        )

        expected.forEach { (section, route) ->
            org.junit.Assert.assertTrue("Unexpected shared route for $section: expected=$route actual=${section.sharedHtmlScreen}", route == section.sharedHtmlScreen)
        }
    }

    @Test
    fun nativeShellAndSaveManagementSectionsDoNotPretendToBeSharedWebRoutes() {
        assertNull(DesktopGameSection.MORE.sharedHtmlScreen)
        assertNull(DesktopGameSection.SAVE_SLOTS.sharedHtmlScreen)
    }
}
