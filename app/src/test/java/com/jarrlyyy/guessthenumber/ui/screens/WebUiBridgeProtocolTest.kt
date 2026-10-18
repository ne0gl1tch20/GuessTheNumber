package com.jarrlyyy.guessthenumber.ui.screens

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebUiBridgeProtocolTest {
    @Test
    fun acceptsReadStateRequest() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("req_1", "read_state"),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"req_1","type":"read_state"}""")
        )
    }


    @Test
    fun acceptsOnlyAllowlistedDailyQuestClaims() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("claim-1", "claim_daily_quest", questId = "daily_correct_5"),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"claim-1","type":"claim_daily_quest","questId":"daily_correct_5"}""")
        )
        val unknown = WebUiBridgeProtocol.parse("""{"version":1,"id":"claim-2","type":"claim_daily_quest","questId":"grant_infinite_money"}""")
        assertEquals("invalid_quest", (unknown as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }

    @Test
    fun acceptsOnlyKnownLootTiers() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("chest-1", "open_loot_chest", tier = "rare"),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"chest-1","type":"open_loot_chest","tier":"rare"}""")
        )
        val unknown = WebUiBridgeProtocol.parse("""{"version":1,"id":"chest-2","type":"open_loot_chest","tier":"legendary"}""")
        assertEquals("invalid_tier", (unknown as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }

    @Test
    fun rejectsExtraFieldsOnGameplayActions() {
        val result = WebUiBridgeProtocol.parse(
            """{"version":1,"id":"claim-3","type":"claim_daily_quest","questId":"daily_correct_5","reward":999999999}"""
        )
        assertEquals("invalid_request", (result as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }


    @Test
    fun acceptsOnlyAllowlistedProgressionRoutes() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("nav-1", "navigate", route = "world_map"),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"nav-1","type":"navigate","route":"world_map"}""")
        )
        val unknown = WebUiBridgeProtocol.parse("""{"version":1,"id":"nav-2","type":"navigate","route":"dev_settings"}""")
        assertEquals("invalid_route", (unknown as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }


    @Test
    fun acceptsOnlyAllowlistedWorldActions() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("world-1", "world_action", worldId = "verdant_grove", worldAction = "boss"),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"world-1","type":"world_action","worldId":"verdant_grove","action":"boss"}""")
        )
        val unknownAction = WebUiBridgeProtocol.parse("""{"version":1,"id":"world-2","type":"world_action","worldId":"verdant_grove","action":"grant_reward"}""")
        assertEquals("invalid_world_action", (unknownAction as WebUiBridgeProtocol.ParseResult.Invalid).error)
        val malformedId = WebUiBridgeProtocol.parse("""{"version":1,"id":"world-3","type":"world_action","worldId":"../save","action":"unlock"}""")
        assertEquals("invalid_world", (malformedId as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }

    @Test
    fun rejectsExtraFieldsOnWorldActions() {
        val result = WebUiBridgeProtocol.parse("""{"version":1,"id":"world-4","type":"world_action","worldId":"verdant_grove","action":"boss","reward":100000}""")
        assertEquals("invalid_request", (result as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }


    @Test
    fun acceptsOnlyKnownSharedProgressionActions() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("base-1", "homebase_upgrade"),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"base-1","type":"homebase_upgrade"}""")
        )
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("codex-1", "codex_reward"),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"codex-1","type":"codex_reward"}""")
        )
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("relic-1", "relic_action", relicId = "verdant_guardian", relicAction = "equip"),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"relic-1","type":"relic_action","relicId":"verdant_guardian","action":"equip"}""")
        )
        val invalid = WebUiBridgeProtocol.parse("""{"version":1,"id":"relic-2","type":"relic_action","relicId":"grant_money","action":"equip"}""")
        assertEquals("invalid_relic", (invalid as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }


    @Test
    fun acceptsOnlyKnownBooleanSettings() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("setting-1", "update_setting", settingKey = "reducedMotion", settingValue = true),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"setting-1","type":"update_setting","key":"reducedMotion","value":"true"}""")
        )
        val unknown = WebUiBridgeProtocol.parse("""{"version":1,"id":"setting-2","type":"update_setting","key":"grantMoney","value":"true"}""")
        assertEquals("invalid_setting", (unknown as WebUiBridgeProtocol.ParseResult.Invalid).error)
        val invalidValue = WebUiBridgeProtocol.parse("""{"version":1,"id":"setting-3","type":"update_setting","key":"soundEnabled","value":"yes"}""")
        assertEquals("invalid_setting_value", (invalidValue as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }

    @Test
    fun acceptsIncrementRequest() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("tap-2", "increment_counter"),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"tap-2","type":"increment_counter"}""")
        )
    }

    @Test
    fun rejectsMalformedJson() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Invalid("invalid_json"),
            WebUiBridgeProtocol.parse("{not-json")
        )
    }

    @Test
    fun rejectsOversizedMessagesBeforeParsing() {
        val result = WebUiBridgeProtocol.parse("x".repeat(WebUiBridgeProtocol.MAX_MESSAGE_LENGTH + 1))
        assertEquals(WebUiBridgeProtocol.ParseResult.Invalid("message_too_large"), result)
    }

    @Test
    fun rejectsUnknownActions() {
        val result = WebUiBridgeProtocol.parse("""{"version":1,"id":"req-3","type":"delete_save"}""")
        assertEquals(WebUiBridgeProtocol.ParseResult.Invalid("unknown_action", "req-3"), result)
    }

    @Test
    fun rejectsUnexpectedFields() {
        val result = WebUiBridgeProtocol.parse(
            """{"version":1,"id":"req-4","type":"read_state","payload":"unexpected"}"""
        )
        assertTrue(result is WebUiBridgeProtocol.ParseResult.Invalid)
        assertEquals("invalid_request", (result as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }

    @Test
    fun rejectsUnsupportedVersions() {
        val result = WebUiBridgeProtocol.parse("""{"version":2,"id":"req-5","type":"read_state"}""")
        assertEquals("invalid_request", (result as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }

    @Test
    fun rejectsInvalidRequestIds() {
        val result = WebUiBridgeProtocol.parse("""{"version":1,"id":"bad id","type":"read_state"}""")
        assertEquals("invalid_request", (result as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }
    @Test
    fun allowsOnlyMainFrameFromExactBundledPrototypePage() {
        assertTrue(
            WebUiBridgeProtocol.isAllowedSource(
                isMainFrame = true,
                sourceOrigin = Uri.parse("https://appassets.androidplatform.net"),
                currentUrl = "https://appassets.androidplatform.net/assets/web-prototype/index.html"
            )
        )
    }

    @Test
    fun rejectsDisallowedOriginsAndPages() {
        assertEquals(
            false,
            WebUiBridgeProtocol.isAllowedSource(
                isMainFrame = true,
                sourceOrigin = Uri.parse("https://evil.example"),
                currentUrl = "https://evil.example/assets/web-prototype/index.html"
            )
        )
        assertEquals(
            false,
            WebUiBridgeProtocol.isAllowedSource(
                isMainFrame = false,
                sourceOrigin = Uri.parse("https://appassets.androidplatform.net"),
                currentUrl = "https://appassets.androidplatform.net/assets/web-prototype/index.html"
            )
        )
        assertEquals(
            false,
            WebUiBridgeProtocol.isAllowedSource(
                isMainFrame = true,
                sourceOrigin = Uri.parse("https://appassets.androidplatform.net"),
                currentUrl = "https://appassets.androidplatform.net/assets/web-prototype/other.html"
            )
        )
    }

    @Test
    fun rejectsCoercedJsonFieldTypes() {
        val stringVersion = WebUiBridgeProtocol.parse(
            """{"version":"1","id":"req-6","type":"read_state"}"""
        )
        assertEquals(
            "invalid_request",
            (stringVersion as WebUiBridgeProtocol.ParseResult.Invalid).error
        )

        val numericId = WebUiBridgeProtocol.parse(
            """{"version":1,"id":7,"type":"read_state"}"""
        )
        assertEquals(
            "invalid_request",
            (numericId as WebUiBridgeProtocol.ParseResult.Invalid).error
        )
    }


    @Test
    fun requestIdWindowRejectsDuplicates() {
        val window = WebUiRequestIdWindow(capacity = 3)
        assertTrue(window.markIfNew("guess-1"))
        assertEquals(false, window.markIfNew("guess-1"))
        assertTrue(window.markIfNew("guess-2"))
    }

    @Test
    fun requestIdWindowEvictsOldIdsAtItsBound() {
        val window = WebUiRequestIdWindow(capacity = 2)
        assertTrue(window.markIfNew("guess-1"))
        assertTrue(window.markIfNew("guess-2"))
        assertTrue(window.markIfNew("guess-3"))
        assertTrue(window.markIfNew("guess-1"))
        assertEquals(false, window.markIfNew("guess-1"))
    }

    @Test
    fun requestIdWindowRequiresPositiveCapacity() {
        try {
            WebUiRequestIdWindow(capacity = 0)
            throw AssertionError("Expected zero capacity to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: a zero-sized replay window cannot enforce uniqueness.
        }
    }

}
