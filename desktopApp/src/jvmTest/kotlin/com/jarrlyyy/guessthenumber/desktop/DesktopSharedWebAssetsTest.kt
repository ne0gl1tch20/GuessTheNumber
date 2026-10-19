package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyCatalog
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Test

class DesktopSharedWebAssetsTest {
    private fun asset(path: String): String =
        javaClass.classLoader.getResource(path)?.readText()
            ?: error("Shared web UI asset is not packaged for desktop: $path")

    @Test
    fun desktopWebStateExposesCatalogsButNotTheSecretTargetNumber() {
        val state = desktopWebPlayState(GameState(targetNumber = 73), DesktopEconomyCatalog.load())
        assertTrue(state.getJSONArray("upgrades").length() > 0)
        assertTrue(state.getJSONArray("shopItems").length() > 0)
        assertFalse(state.has("targetNumber"))
        assertTrue(state.has("rangeMin"))
        assertTrue(state.has("buyMultiplier"))
        assertTrue(state.has("prestigeRequirement"))
        assertTrue(state.has("prestigeReward"))
        assertTrue(state.has("canPrestige"))
        assertTrue(state.has("ultraRequirement"))
        assertTrue(state.has("ultraReward"))
        assertTrue(state.has("canUltra"))
        assertTrue(state.has("talents"))
        assertTrue(state.has("activeMutators"))
        assertTrue(state.getString("initialScreen") == "play")
        val worlds = state.getJSONArray("worlds")
        assertTrue("World screen should use real desktop progression", worlds.length() >= 4)
        assertTrue(worlds.getJSONObject(0).has("bossName"))
        assertTrue(worlds.getJSONObject(0).has("canFight"))
        assertTrue(state.has("activeBossHpTotal"))
        assertTrue(state.has("relicInventory"))
        assertTrue(state.has("equippedRelicIds"))
        assertTrue(state.has("codexEntries"))
        assertTrue(state.has("codexRewardClaimed"))
        assertTrue(state.has("settings"))
        assertTrue(state.getJSONObject("settings").has("reducedMotion"))
    }

    @Test
    fun desktopDestinationsSelectTheirMatchingSharedHtmlScreen() {
        assertTrue(DesktopGameSection.PLAY.sharedHtmlScreen == "play")
        assertTrue(DesktopGameSection.UPGRADES.sharedHtmlScreen == "upgrades")
        assertTrue(DesktopGameSection.SHOP.sharedHtmlScreen == "shop")
        assertTrue(DesktopGameSection.PRESTIGE.sharedHtmlScreen == "prestige")
        assertTrue(DesktopGameSection.ULTRA.sharedHtmlScreen == "ultra")
        assertTrue(DesktopGameSection.TALENT.sharedHtmlScreen == "talent")
        assertTrue(DesktopGameSection.ACHIEVEMENTS.sharedHtmlScreen == "rewards")
        assertTrue(DesktopGameSection.WORLD_MAP.sharedHtmlScreen == "worlds")
        assertTrue(DesktopGameSection.MUTATORS.sharedHtmlScreen == "mutators")
        assertTrue(DesktopGameSection.RELICS.sharedHtmlScreen == "relics")
        assertTrue(DesktopGameSection.HOME_BASE.sharedHtmlScreen == "homebase")
        assertTrue(DesktopGameSection.CODEX.sharedHtmlScreen == "codex")
        assertTrue(DesktopGameSection.ENDGAME.sharedHtmlScreen == "journey")
        assertTrue(DesktopGameSection.STATS.sharedHtmlScreen == "stats")
        assertTrue(DesktopGameSection.ARCADE.sharedHtmlScreen == "arcade")
        assertTrue(DesktopGameSection.LUCKY_STAKE.sharedHtmlScreen == "stake")
        assertTrue(DesktopGameSection.SETTINGS.sharedHtmlScreen == "settings")
    }

    @Test
    fun sharedSettingsUseLocalizedLabelsAndDescriptions() {
        val html = asset("web-play/index.html")
        val state = desktopWebPlayState(GameState(), DesktopEconomyCatalog.load())
        val strings = state.getJSONObject("uiStrings")
        val keys = listOf(
            "settings", "game_preferences", "background_music", "sound_effects", "vibration_label",
            "reduced_motion_label", "reduce_flashes_label", "settings_desc_sound_fx",
            "settings_desc_vibration", "reduced_motion_description", "settings_desc_reduce_flashes"
        )
        keys.forEach { key -> assertTrue("Missing shared UI translation: $key", strings.has(key)) }
        keys.forEach { key -> assertTrue("Settings markup must use $key", html.contains("data-i18n=\"$key\"")) }
        assertFalse(html.contains("Changes apply on both Android and Windows"))
        // The About screen intentionally credits native libraries; scope jargon checks to the game/settings UI.
        val playerCopy = (html.substringBefore("data-screen=\\\"about\\\"") + html.substringAfter("id=\\\"screen-settings\\\"") + "\\n" + asset("web-play/app.js")).lowercase()
        listOf("kotlin", "javascript", "typescript", "lua", "python", "scripting language").forEach { term ->
            assertFalse("Player-facing shared UI should not expose implementation jargon: $term", playerCopy.contains(term))
        }
    }

    @Test
    fun everySupportedLocaleContainsSharedSettingsTranslations() {
        val locales = listOf(
            "ar.json", "de.json", "en_gb.json", "en_us.json", "es.json", "fil_ph.json",
            "fr.json", "hi_in.json", "id.json", "it.json", "ja.json", "ko.json", "nl.json",
            "pl.json", "pt_br.json", "pt_pt.json", "ru.json", "th.json", "tr.json",
            "uk.json", "vi.json", "zh_cn.json", "zh_tw.json"
        )
        val keys = listOf(
            "settings", "game_preferences", "background_music", "sound_effects", "vibration_label",
            "reduced_motion_label", "reduce_flashes_label", "settings_desc_sound_fx",
            "settings_desc_vibration", "reduced_motion_description", "settings_desc_reduce_flashes"
        )
        locales.forEach { locale ->
            val strings = org.json.JSONObject(asset("locales/$locale"))
            keys.forEach { key ->
                assertTrue("Missing $key in locale $locale", strings.optString(key).isNotBlank())
            }
            if (locale != "en_us.json" && locale != "en_gb.json") {
                assertTrue("background_music must be translated in $locale", strings.getString("background_music") != "Background Music")
                assertTrue("sound_effects must be translated in $locale", strings.getString("sound_effects") != "Sound Effects")
            }
        }
    }

    @Test
    fun canonicalAndroidWebPlayBundleIsAvailableOnDesktopClasspath() {
        val html = asset("web-play/index.html")
        val script = asset("web-play/app.js")
        val styles = asset("web-play/styles.css")

        assertTrue(html.contains("id=\"screen-play\""))
        assertTrue(html.contains("id=\"screen-upgrades\""))
        assertTrue(html.contains("id=\"screen-shop\""))
        assertTrue(html.contains("id=\"screen-homebase\""))
        assertTrue(html.contains("id=\"screen-relics\""))
        assertTrue(html.contains("id=\"screen-codex\""))
        assertTrue(html.contains("id=\"screen-settings\""))
        assertTrue(html.contains("id=\"screen-prestige\""))
        assertTrue(html.contains("id=\"screen-ultra\""))
        assertTrue(html.contains("id=\"screen-talent\""))
        assertTrue(html.contains("id=\"screen-mutators\""))
        assertTrue(script.contains("buy_upgrade"))
        assertTrue(script.contains("buy_shop_item"))
        assertTrue(script.contains("homebase_upgrade"))
        assertTrue(script.contains("relic_action"))
        assertTrue(script.contains("codex_reward"))
        assertTrue(script.contains("update_setting"))
        assertTrue(script.contains("prestige_reset"))
        assertTrue(script.contains("ultra_reset"))
        assertTrue(script.contains("buy_talent"))
        assertTrue(script.contains("set_mutator"))
        assertTrue(script.contains("initialScreenApplied=true"))
        assertTrue(styles.contains(".screen"))
    }
}
