package com.jarrlyyy.guessthenumber.scripting

import android.content.Context
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.shared.domain.InvisibleTraitEngine
import org.luaj.vm2.LuaValue

/**
 * Runs the invisible-trait policy in the existing restricted Lua sandbox.
 *
 * The script can only return approved IDs. Kotlin validates the IDs again and remains the
 * authority for every reward and save mutation. Failure falls back to the deterministic
 * shared Kotlin policy so Lua cannot disable gameplay.
 */
class LuaInvisibleTraitSelector(context: Context) {
    private val appContext = context.applicationContext

    fun evaluate(state: GameState): InvisibleTraitEngine.Decision {
        return try {
            val source = appContext.assets.open(SCRIPT_PATH).bufferedReader().use { it.readText() }
            val input = mapOf(
                "failed_guesses" to state.traitProfile.failedGuessesObserved.toString(),
                "correct_guesses" to state.traitProfile.correctGuessesObserved.toString(),
                "best_streak" to state.bestStreak.toString(),
                "current_streak" to state.streak.toString(),
                "prestige_count" to state.prestigeCount.toString(),
                "ultra_count" to state.ultraCount.toString(),
                "first_prestige_awarded" to bool(state.traitProfile.awardedBonuses.contains(InvisibleTraitEngine.BONUS_FIRST_PRESTIGE)),
                "first_ultra_awarded" to bool(state.traitProfile.awardedBonuses.contains(InvisibleTraitEngine.BONUS_FIRST_ULTRA)),
                "veteran_awarded" to bool(state.traitProfile.awardedBonuses.contains(InvisibleTraitEngine.BONUS_VETERAN))
            )
            val result = LuaSandbox {}.execute(SCRIPT_ID, source, input)
            require(result.istable()) { "Invisible trait script must return a table." }
            val helpers = readIds(result.get("helpers"), VALID_HELPERS)
            val bonuses = readIds(result.get("bonuses"), VALID_BONUSES)
            InvisibleTraitEngine.Decision(
                helperIds = helpers.take(InvisibleTraitEngine.MAX_HELPERS_PER_GUESS).toSet(),
                bonusIds = bonuses.toSet()
            )
        } catch (error: Exception) {
            GameLogger.log(
                LogLevel.WARN,
                LoggerCategory.GAME,
                "LUA_INVISIBLE_TRAIT_FAILED",
                (error.message ?: "Invisible trait Lua policy failed.").take(240)
            )
            InvisibleTraitEngine.evaluate(state)
        }
    }

    private fun readIds(value: LuaValue, allowed: Set<String>): List<String> {
        if (!value.istable()) return emptyList()
        val table = value.checktable()
        return (1..table.length())
            .mapNotNull { index ->
                val id = table.get(index).tojstring()
                id.takeIf { it in allowed }
            }
    }

    private fun bool(value: Boolean): String = if (value) "1" else "0"

    private companion object {
        const val SCRIPT_ID = "invisible_traits"
        const val SCRIPT_PATH = "scripts/traits/invisible_traits.lua"
        val VALID_HELPERS = setOf(
            InvisibleTraitEngine.HELPER_RANGE_SCOUT,
            InvisibleTraitEngine.HELPER_MISTAKE_INSURANCE,
            InvisibleTraitEngine.HELPER_STREAK_GUARD,
            InvisibleTraitEngine.HELPER_COMEBACK,
            InvisibleTraitEngine.HELPER_MOMENTUM
        )
        val VALID_BONUSES = setOf(
            InvisibleTraitEngine.BONUS_FIRST_PRESTIGE,
            InvisibleTraitEngine.BONUS_FIRST_ULTRA,
            InvisibleTraitEngine.BONUS_VETERAN
        )
    }
}
