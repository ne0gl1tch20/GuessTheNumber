package com.jarrlyyy.guessthenumber.scripting

import android.content.Context
import com.jarrlyyy.guessthenumber.data.repository.LiveOpsContentValidator
import com.jarrlyyy.guessthenumber.data.repository.LiveOpsEvent
import com.jarrlyyy.guessthenumber.data.repository.LiveOpsManifest
import org.luaj.vm2.LuaValue

/**
 * Parses bundled Lua seasonal content. This loader cannot grant rewards or mutate saves.
 * The returned model is validated by the same Kotlin validator used for JSON fallback.
 */
class LuaSeasonalEventCatalog(context: Context) {
    private val appContext = context.applicationContext

    fun loadBundled(): LiveOpsManifest {
        val source = appContext.assets.open(SCRIPT_PATH).bufferedReader().use { it.readText() }
        val result = LuaSandbox {}.execute(SCRIPT_ID, source)
        require(result.istable()) { "Seasonal catalog must return a table." }

        val schemaVersion = result.requiredInt("schemaVersion")
        val manifestVersion = result.requiredInt("manifestVersion")
        val eventTable = result.get("events")
        require(eventTable.istable()) { "Seasonal catalog events must be a table." }

        val count = eventTable.length()
        require(count in 1..MAX_EVENTS) { "Seasonal catalog event count is outside the allowed range." }
        val events = (1..count).map { index ->
            val value = eventTable.get(index)
            require(value.istable()) { "Seasonal catalog entries must be tables." }
            LiveOpsEvent(
                id = value.requiredString("id", 64),
                title = value.requiredString("title", 100),
                description = value.requiredString("description", 500),
                status = value.requiredString("status", 16),
                startDate = value.requiredString("startDate", 40),
                endDate = value.requiredString("endDate", 40),
                currencyName = value.requiredString("currencyName", 32),
                maxClaims = value.requiredInt("maxClaims"),
                rewardNebula = value.requiredLong("rewardNebula"),
                permanentBoostPerClaim = value.requiredInt("permanentBoostPerClaim"),
                minCorrectGuesses = value.requiredLong("minCorrectGuesses"),
                minPrestigeCount = value.requiredLong("minPrestigeCount"),
                minUltraCount = value.requiredLong("minUltraCount")
            )
        }

        return LiveOpsManifest(
            manifestVersion = manifestVersion,
            events = events,
            schemaVersion = schemaVersion
        ).also(LiveOpsContentValidator::validate)
    }

    private fun LuaValue.requiredString(key: String, maxLength: Int): String {
        val value = get(key)
        require(value.isstring()) { "Seasonal catalog field '$key' must be a string." }
        return value.tojstring().also {
            require(it.isNotBlank() && it.length <= maxLength) {
                "Seasonal catalog field '$key' is blank or too long."
            }
        }
    }

    private fun LuaValue.requiredInt(key: String): Int {
        val value = get(key)
        require(value.isnumber()) { "Seasonal catalog field '$key' must be an integer." }
        val number = value.todouble()
        require(number.isFinite() && number % 1.0 == 0.0 &&
            number >= Int.MIN_VALUE.toDouble() && number <= Int.MAX_VALUE.toDouble()
        ) { "Seasonal catalog field '$key' must be an integer." }
        return number.toInt()
    }

    private fun LuaValue.requiredLong(key: String): Long {
        val value = get(key)
        require(value.isnumber()) { "Seasonal catalog field '$key' must be an integer." }
        val number = value.todouble()
        // Lua numbers are doubles, so only accept integers that are represented exactly.
        require(number.isFinite() && number % 1.0 == 0.0 &&
            number >= 0.0 && number <= MAX_EXACT_LUA_INTEGER
        ) { "Seasonal catalog field '$key' must be a non-negative exact integer." }
        return number.toLong()
    }

    private companion object {
        const val SCRIPT_ID = "seasonal_event_catalog"
        const val SCRIPT_PATH = "scripts/events/seasonal_events.lua"
        const val MAX_EVENTS = 32
        const val MAX_EXACT_LUA_INTEGER = 9_007_199_254_740_991.0
    }
}
