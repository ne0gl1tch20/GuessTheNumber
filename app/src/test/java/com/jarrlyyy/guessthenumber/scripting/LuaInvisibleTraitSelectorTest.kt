package com.jarrlyyy.guessthenumber.scripting

import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.TraitProfile
import com.jarrlyyy.guessthenumber.shared.domain.InvisibleTraitEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LuaInvisibleTraitSelectorTest {
    @Test
    fun malformedLuaOutputFallsBackToKotlinPolicy() {
        val state = GameState(
            traitProfile = TraitProfile(
                totalGuessesObserved = 3L,
                failedGuessesObserved = 3L
            )
        )
        var failureLogged = false

        val decision = LuaInvisibleTraitSelector.evaluateWithSource(
            state = state,
            loadSource = { "return 42" },
            onFailure = { failureLogged = true }
        )

        assertTrue(failureLogged)
        assertTrue(InvisibleTraitEngine.HELPER_MISTAKE_INSURANCE in decision.helperIds)
    }

    @Test
    fun LuaOutputIsFilteredAndHelperCountIsBounded() {
        val decision = LuaInvisibleTraitSelector.evaluateWithSource(
            state = GameState(),
            loadSource = {
                """
                return {
                    helpers = {
                        "mistake_insurance",
                        "not_a_real_helper",
                        "range_scout",
                        "comeback_reward",
                        "momentum_reward"
                    },
                    bonuses = { "not_a_real_bonus" }
                }
                """.trimIndent()
            }
        )

        assertEquals(
            setOf(
                InvisibleTraitEngine.HELPER_MISTAKE_INSURANCE,
                InvisibleTraitEngine.HELPER_RANGE_SCOUT,
                InvisibleTraitEngine.HELPER_COMEBACK
            ),
            decision.helperIds
        )
        assertTrue(decision.helperIds.size <= InvisibleTraitEngine.MAX_HELPERS_PER_GUESS)
        assertTrue(decision.bonusIds.isEmpty())
    }
}
