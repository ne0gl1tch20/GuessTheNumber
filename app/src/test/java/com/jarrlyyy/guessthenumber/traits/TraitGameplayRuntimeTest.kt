package com.jarrlyyy.guessthenumber.traits

import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.engine.GuessResult
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TraitGameplayRuntimeTest {
    @Test fun comboAndRiskTraitsIncreaseActualCorrectGuessPayout() {
        val before = GameState(
            money = BigNumber(1_000),
            currentRangeMin = 1,
            currentRangeMax = 100,
            targetNumber = 50,
            correctGuesses = 100,
            streak = 5
        )
        val after = before.copy(
            money = BigNumber(1_100),
            correctGuesses = 101,
            streak = 6,
            bestStreak = 6,
            statistics = before.statistics.copy(moneyEarned = BigNumber(100))
        )
        val base = GuessResult(GuessFeedback.CORRECT, BigNumber(100), false, 6, after)
        val result = TraitGameplayRuntime.apply(before, 50, base, "summer")
        assertEquals(135.0, result.reward.value.toDouble(), 0.001)
        assertEquals(1_135.0, result.newState.money.value.toDouble(), 0.001)
        assertEquals(135.0, result.newState.statistics.moneyEarned.value.toDouble(), 0.001)
    }

    @Test fun mistakeInsuranceRefundsHalfOfMissPenaltyAndRangeScoutNarrowsHints() {
        val before = GameState(
            money = BigNumber(1_000),
            currentRangeMin = 1,
            currentRangeMax = 100,
            targetNumber = 50,
            correctGuesses = 50,
            streak = 4
        )
        val after = before.copy(
            money = BigNumber(900),
            attempts = 1,
            streak = 0,
            statistics = before.statistics.copy(totalGuesses = 1, failedGuesses = 1)
        )
        val base = GuessResult(GuessFeedback.TOO_LOW, BigNumber.ZERO, false, 0, after)
        val result = TraitGameplayRuntime.apply(before, 40, base, "summer")
        assertEquals(950.0, result.newState.money.value.toDouble(), 0.001)
        assertEquals(41L, result.newState.currentRangeMin)
        assertEquals(100L, result.newState.currentRangeMax)
        assertEquals(4, result.newState.streak)
    }

    @Test fun selectedComboArmChangesTheActualPayout() {
        val before = GameState(
            money = BigNumber(1_000),
            currentRangeMin = 1,
            currentRangeMax = 100,
            targetNumber = 50,
            correctGuesses = 25,
            streak = 5
        )
        val selected = TraitGameplayRuntime.selectArm(before, "combo_momentum", "burst")
        val after = selected.copy(
            money = BigNumber(1_100),
            correctGuesses = 26,
            streak = 6,
            bestStreak = 6,
            statistics = selected.statistics.copy(moneyEarned = BigNumber(100))
        )
        val base = GuessResult(GuessFeedback.CORRECT, BigNumber(100), false, 6, after)
        val result = TraitGameplayRuntime.apply(selected, 50, base, "summer")
        assertEquals(115.0, result.reward.value.toDouble(), 0.001)
        assertEquals("burst", result.newState.selectedTraitArms["combo_momentum"])
    }

    @Test fun unknownOrLockedArmsAreRejected() {
        val state = GameState(correctGuesses = 1)
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            TraitGameplayRuntime.selectArm(state, "combo_momentum", "burst")
        }
        val unlocked = state.copy(correctGuesses = 25)
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            TraitGameplayRuntime.selectArm(unlocked, "combo_momentum", "not-real")
        }
    }

    @Test fun validatedRemoteTraitEffectChangesActualGameplay() {
        val originalRegistry = ActiveTraitCatalog.current()
        try {
            val remote = TraitDefinition(
                id = "remote_combo_module",
                titleKey = "trait.remote_combo.title",
                descriptionKey = "trait.remote_combo.description",
                category = TraitCategory.GAMEPLAY,
                effects = setOf(TraitEffect.COMBO_REWARD),
                activation = TraitActivationConditions(minimumCorrectGuesses = 0L),
                source = TraitSource.LIVEOPS,
                catalogId = "community"
            )
            ActiveTraitCatalog.install(TraitRegistry(BuiltInTraits.definitions + remote))
            val before = GameState(
                money = BigNumber(1_000),
                currentRangeMin = 1,
                currentRangeMax = 100,
                targetNumber = 50,
                correctGuesses = 0,
                streak = 5,
                selectedTraitArms = mapOf("combo_momentum" to "burst")
            )
            val after = before.copy(
                money = BigNumber(1_100),
                correctGuesses = 1,
                streak = 6,
                statistics = before.statistics.copy(moneyEarned = BigNumber(100))
            )
            val base = GuessResult(GuessFeedback.CORRECT, BigNumber(100), false, 6, after)
            val result = TraitGameplayRuntime.apply(before, 50, base, "summer")
            assertEquals(110.0, result.reward.value.toDouble(), 0.001)
            assertEquals("Remote trait must not inherit a locked built-in arm", 1_110.0, result.newState.money.value.toDouble(), 0.001)
            assertEquals(1_110.0, result.newState.money.value.toDouble(), 0.001)
        } finally {
            ActiveTraitCatalog.install(originalRegistry)
        }
    }

    @Test fun traitLogicDoesNotRunForOutOfRangeGuess() {
        val before = GameState(currentRangeMin = 1, currentRangeMax = 100, correctGuesses = 100)
        val after = before.copy(attempts = 1)
        val base = GuessResult(GuessFeedback.TOO_LOW, BigNumber.ZERO, false, 0, after)
        val result = TraitGameplayRuntime.apply(before, 101, base, "summer")
        assertEquals(after, result.newState)
    }

    @Test fun rangeScoutDoesNotMoveRangePastAnExtremeGuess() {
        val before = GameState(currentRangeMin = 1, currentRangeMax = 100, correctGuesses = 10)
        val after = before.copy(attempts = 1)
        val base = GuessResult(GuessFeedback.TOO_LOW, BigNumber.ZERO, false, 0, after)
        val result = TraitGameplayRuntime.apply(before, 100, base, "summer")
        assertTrue(result.newState.currentRangeMin <= result.newState.currentRangeMax)
    }
}
