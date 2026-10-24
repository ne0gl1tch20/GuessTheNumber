package com.jarrlyyy.guessthenumber

import com.jarrlyyy.guessthenumber.traits.TraitEffect
import com.jarrlyyy.guessthenumber.traits.TraitExtensionRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TraitExtensionRuntimeTest {
    private val runtime = TraitExtensionRuntime()

    @Test fun luaModuleReadsOnlyProvidedSnapshotAndReturnsDeclarativeDecision() {
        val extension = runtime.validate(
            """{"schemaVersion":1,"traitId":"remote_streak","modules":[{"id":"logic","type":"LUA","entrypoint":"logic.lua","capabilities":["read_snapshot"]}]}""",
            mapOf("logic.lua" to """
                local count = tonumber(gtn.input.correctGuesses or "0")
                local streak = tonumber(gtn.input.bestStreak or "0")
                return { eligible = count >= 100 and streak >= 10, effects = {"COSMETIC_BADGE"} }
            """.trimIndent()),
            "remote_streak"
        )
        val no = runtime.evaluateLua(extension, "logic", mapOf("correctGuesses" to "200", "bestStreak" to "9"), setOf(TraitEffect.COSMETIC_BADGE))
        val yes = runtime.evaluateLua(extension, "logic", mapOf("correctGuesses" to "100", "bestStreak" to "10"), setOf(TraitEffect.COSMETIC_BADGE))
        assertFalse(no.eligible)
        assertTrue(yes.eligible)
        assertEquals(setOf(TraitEffect.COSMETIC_BADGE), yes.requestedEffects)
    }

    @Test fun luaCannotRequestUndeclaredEffects() {
        val extension = runtime.validate(
            """{"schemaVersion":1,"traitId":"remote_streak","modules":[{"id":"logic","type":"LUA","entrypoint":"logic.lua"}]}""",
            mapOf("logic.lua" to """return { eligible = true, effects = {"NEBULA_BONUS"} }"""),
            "remote_streak"
        )
        assertThrows(IllegalArgumentException::class.java) {
            runtime.evaluateLua(extension, "logic", emptyMap(), setOf(TraitEffect.COSMETIC_BADGE))
        }
    }

    @Test fun jsonModuleIsParsedAsDeclarativeData() {
        val extension = runtime.validate(
            """{"schemaVersion":1,"traitId":"remote_rules","modules":[{"id":"rules","type":"JSON","entrypoint":"rules.json","capabilities":["read_rules"]}]}""",
            mapOf("rules.json" to """{"minimumBestStreak":25,"effects":["COSMETIC_BADGE"]}"""),
            "remote_rules"
        )
        assertEquals("25", runtime.readJsonModule(extension, "rules")["minimumBestStreak"]!!.jsonPrimitive.content)
    }

    @Test fun javascriptModuleMustUseUiContractAndCannotAccessNativeOrNetworkApis() {
        val good = runtime.validate(
            """{"schemaVersion":1,"traitId":"remote_view","modules":[{"id":"view","type":"JAVASCRIPT","entrypoint":"view.js","capabilities":["render_ui"]}]}""",
            mapOf("view.js" to "function renderTrait(api) { api.setText('status', 'Ready'); }"),
            "remote_view"
        )
        assertTrue(runtime.javascriptPresentation(good, "view").contains("renderTrait"))
        assertThrows(IllegalArgumentException::class.java) {
            runtime.validate(
                """{"schemaVersion":1,"traitId":"remote_view","modules":[{"id":"view","type":"JAVASCRIPT","entrypoint":"view.js"}]}""",
                mapOf("view.js" to "function renderTrait(api) { window.Android.earnReward(); }"),
                "remote_view"
            )
        }
    }

    @Test fun rejectsTraversalMissingFilesDuplicateModulesAndUnknownCapabilities() {
        val base = """{"schemaVersion":1,"traitId":"remote_rules","modules":[{"id":"rules","type":"JSON","entrypoint":"../rules.json"}]}"""
        assertThrows(IllegalArgumentException::class.java) {
            runtime.validate(base, mapOf("../rules.json" to "{}"), "remote_rules")
        }
        assertThrows(IllegalStateException::class.java) {
            runtime.validate(base.replace("../rules.json", "rules.json"), emptyMap(), "remote_rules")
        }
        assertThrows(IllegalArgumentException::class.java) {
            runtime.validate(
                """{"schemaVersion":1,"traitId":"remote_rules","modules":[{"id":"rules","type":"JSON","entrypoint":"rules.json","capabilities":["award_currency"]}]}""",
                mapOf("rules.json" to "{}"), "remote_rules"
            )
        }
    }

    @Test fun rejectsWrongTraitIdentityAndUnknownSchema() {
        assertThrows(IllegalArgumentException::class.java) {
            runtime.validate("""{"schemaVersion":9,"traitId":"remote_rules","modules":[{"id":"rules","type":"JSON","entrypoint":"rules.json"}]}""",
                mapOf("rules.json" to "{}"), "remote_rules")
        }
        assertThrows(IllegalArgumentException::class.java) {
            runtime.validate("""{"schemaVersion":1,"traitId":"other_trait","modules":[{"id":"rules","type":"JSON","entrypoint":"rules.json"}]}""",
                mapOf("rules.json" to "{}"), "remote_rules")
        }
    }
}
