package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProductionGuessPresentationAdapterTest {
    @Test
    fun mapsProductionRangeAttemptsAndFeedbackWithoutEndingProductionGame() {
        val state = GameState().copy(
            currentRangeMin = 10L,
            currentRangeMax = 90L,
            attempts = 12L
        )

        GuessGameStatus.values().forEach { status ->
            val presentation = ProductionGuessPresentationAdapter.from(
                state = state,
                status = status
            )

            assertEquals(10L, presentation.rangeMin)
            assertEquals(90L, presentation.rangeMax)
            assertEquals(12L, presentation.guessCount)
            assertEquals(status, presentation.status)
            assertFalse(presentation.solved)
        }
    }
}
