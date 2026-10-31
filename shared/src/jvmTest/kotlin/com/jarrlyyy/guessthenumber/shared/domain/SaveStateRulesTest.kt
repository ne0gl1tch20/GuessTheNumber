package com.jarrlyyy.guessthenumber.shared.domain

import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveStateRulesTest {
    @Test
    fun acceptsValidPersistedGameState() {
        assertTrue(SaveStateRules.validate(GameState(lastSaveTimestamp = 1L)))
    }

    @Test
    fun rejectsInvalidRangeDifficultyAndNegativeCounters() {
        val valid = GameState(lastSaveTimestamp = 1L)
        assertFalse(SaveStateRules.validate(valid.copy(difficultyId = "not-a-difficulty")))
        assertFalse(SaveStateRules.validate(valid.copy(currentRangeMin = 10L, currentRangeMax = 5L)))
        assertFalse(SaveStateRules.validate(valid.copy(attempts = -1L)))
        assertFalse(SaveStateRules.validate(valid.copy(statistics = valid.statistics.copy(totalGuesses = -1L))))
    }
}
