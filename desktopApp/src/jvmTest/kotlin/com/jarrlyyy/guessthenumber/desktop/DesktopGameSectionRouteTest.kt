package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DesktopGameSectionRouteTest {
    @Test
    fun playerFacingSectionsResolveToTheCanonicalSharedWebRoutes() {
        val expected = mapOf(
            DesktopGameSection.PLAY to "play",
            DesktopGameSection.MORE to "more",
            DesktopGameSection.CHANGELOG to "changelog",
            DesktopGameSection.ABOUT to "about",
            DesktopGameSection.MUSIC_PLAYER to "music",
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

        val sharedHtml = javaClass.classLoader.getResource("web-play/index.html")
            ?.readText()
            ?: error("The canonical shared HTML bundle is missing from Windows resources.")
        expected.values.forEach { route ->
            org.junit.Assert.assertTrue("The canonical HTML bundle is missing route $route", sharedHtml.contains("data-screen=\"" + route + "\""))
        }

        val sharedScript = javaClass.classLoader.getResource("web-play/app.js")
            ?.readText()
            ?: error("The canonical shared JavaScript bundle is missing from Windows resources.")
        org.junit.Assert.assertTrue("Desktop route activation must use the shared screen controller.", sharedScript.contains("window.gtnActivateScreen=activateScreen"))

        listOf(
            "web-play/trait-extension-host.js",
            "web-play/vue-jarrlyy-adapter.js",
            "web-play/vue-bot-arena.js",
            "web-play/menu-studio.js",
            "libraries/vue/vue.global.prod.js",
            "libraries/jarrlyyuisdk/components/jarrlyy-ui-advanced.js",
            "libraries/jarrlyyuisdk/components/jarrlyy-ui-host.js",
            "fonts/Inter-VariableFont_opsz,wght.ttf"
        ).forEach { resource ->
            org.junit.Assert.assertNotNull("A required shared UI asset was not packaged: $resource", javaClass.classLoader.getResource(resource))
        }
    }

    @Test
    fun nativeShellAndSaveManagementSectionsDoNotPretendToBeSharedWebRoutes() {
        assertNull(DesktopGameSection.SAVE_SLOTS.sharedHtmlScreen)
    }
}
