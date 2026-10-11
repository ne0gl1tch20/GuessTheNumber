package com.jarrlyyy.guessthenumber.scripting

import android.content.Context
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.domain.model.RandomEventType

/**
 * Selects whether a random event occurs and which registered event type is selected.
 * The Lua result is data only; Kotlin validates it and applies all game-state changes.
 */
class LuaRandomEventSelector(context: Context) {
    private val appContext = context.applicationContext

    fun roll(): RandomEventType? {
        return try {
            val source = appContext.assets.open(SCRIPT_PATH).bufferedReader().use { it.readText() }
            val result = LuaSandbox {}.execute(SCRIPT_ID, source)
            require(result.istable()) { "Random-event script must return a table." }

            if (!result.get("trigger").toboolean()) {
                null
            } else {
                val typeValue = result.get("type")
                require(typeValue.isstring()) { "Random-event script must return an event type." }
                val typeName = typeValue.tojstring()
                RandomEventType.values().firstOrNull { it.name == typeName }
                    ?: throw IllegalArgumentException("Random-event script returned an unknown event type.")
            }
        } catch (error: Exception) {
            GameLogger.log(
                LogLevel.WARN,
                LoggerCategory.GAME,
                "LUA_RANDOM_EVENT_FAILED",
                (error.message ?: "Lua random-event selection failed.").take(240)
            )
            null
        }
    }

    private companion object {
        const val SCRIPT_ID = "random_event_selector"
        const val SCRIPT_PATH = "scripts/events/random_event.lua"
    }
}
