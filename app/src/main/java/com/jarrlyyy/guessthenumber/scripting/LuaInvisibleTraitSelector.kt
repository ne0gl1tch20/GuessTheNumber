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
 * Lua can return approved intent IDs only; Kotlin owns all rewards and state mutations.
 */
class LuaInvisibleTraitSelector(context: Context) {
    private val appContext = context.applicationContext

    fun evaluate(state: GameState): InvisibleTraitEngine.Decision =
        evaluateWithSource(
            state = state,
            loadSource = {
                appContext.assets.open(SCRIPT_PATH).bufferedReader().use { it.readText() }
            },
            onFailure = { message ->
                GameLogger.log(
                    LogLevel.WARN,
                    LoggerCategory.GAME,
                    "LUA_INVISIBLE_TRAIT_FAILED",
                    message.take(240)
                )
            }
        )

    companion object {
        private const val SCRIPT_ID = "invisible_traits"
        private const val SCRIPT_PATH = "scripts/traits/invisible_traits.lua"

        private val VALID_HELPERS = setOf(
            InvisibleTraitEngine.HELPER_RANGE_SCOUT,
            InvisibleTraitEngine.HELPER_MISTAKE_INSURANCE,
            InvisibleTraitEngine.HELPER_STREAK_GUARD,
            InvisibleTraitEngine.HELPER_COMEBACK,
            InvisibleTraitEngine.HELPER_MOMENTUM
        )
        private val VALID_BONUSES = setOf(
            InvisibleTraitEngine.BONUS_FIRST_PRESTIGE,
            InvisibleTraitEngine.BONUS_FIRST_ULTRA,
            InvisibleTraitEngine.BONUS_VETERAN
        )

        /** Exposed internally so malformed-script behavior can be tested without Android assets. */
        internal fun evaluateWithSource(
            state: GameState,
            loadSource: () -> String,
            onFailure: (String) -> Unit = {}
        ): InvisibleTraitEngine.Decision {
            return try {
                val input = mapOf(
                    "failed_guesses" to state.traitProfile.failedGuessesObserved.toString(),
                    "correct_guesses" to state.traitProfile.correctGuessesObserved.toString(),
                    "long_streaks_observed" to state.traitProfile.longStreaksObserved.toString(),
                    "current_streak" to state.streak.toString(),
                    "prestige_count" to state.prestigeCount.toString(),
                    "ultra_count" to state.ultraCount.toString(),
                    "first_prestige_awarded" to bool(state.traitProfile.awardedBonuses.contains(InvisibleTraitEngine.BONUS_FIRST_PRESTIGE)),
                    "first_ultra_awarded" to bool(state.traitProfile.awardedBonuses.contains(InvisibleTraitEngine.BONUS_FIRST_ULTRA)),
                    "veteran_awarded" to bool(state.traitProfile.awardedBonuses.contains(InvisibleTraitEngine.BONUS_VETERAN))
                )
                val result = LuaSandbox {}.execute(SCRIPT_ID, loadSource(), input)
                require(result.istable()) { "Invisible trait script must return a table." }
                val helpers = readIds(result.get("helpers"), VALID_HELPERS)
                val bonuses = readIds(result.get("bonuses"), VALID_BONUSES)
                InvisibleTraitEngine.Decision(
                    helperIds = helpers.take(InvisibleTraitEngine.MAX_HELPERS_PER_GUESS).toSet(),
                    bonusIds = bonuses.toSet()
                )
            } catch (error: LuaBudgetExceededError) {
                onFailure(error.message ?: "Invisible trait Lua policy exceeded its instruction budget.")
                InvisibleTraitEngine.evaluate(state)
            } catch (error: Exception) {
                onFailure(error.message ?: "Invisible trait Lua policy failed.")
                InvisibleTraitEngine.evaluate(state)
            }
        }

        private fun readIds(value: LuaValue, allowed: Set<String>): List<String> {
            require(value.istable()) { "Invisible trait script must return helper and bonus tables." }
            val table = value.checktable()
            return (1..table.length())
                .mapNotNull { index ->
                    val idValue = table.get(index)
                    if (!idValue.isstring()) return@mapNotNull null
                    idValue.tojstring().takeIf { it in allowed }
                }
                .distinct()
        }

        private fun bool(value: Boolean): String = if (value) "1" else "0"
    }
}
