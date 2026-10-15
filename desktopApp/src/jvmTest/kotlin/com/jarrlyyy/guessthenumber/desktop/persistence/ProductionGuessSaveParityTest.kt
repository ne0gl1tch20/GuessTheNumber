package com.jarrlyyy.guessthenumber.desktop.persistence

import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.model.GameState
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionGuessSaveParityTest {
    @Test
    fun productionGuessTransitionsSurviveDesktopSaveReload() = runBlocking {
        val directory = Files.createTempDirectory("gtn-stage2d-parity")
        val store = DesktopSaveStore(directory)
        val engine = GameEngine(Random(1234))
        val initial = GameState(
            targetNumber = 50,
            currentRangeMin = 1,
            currentRangeMax = 100
        )

        assertTrue(store.save(1, initial))

        val wrongGuess = engine.processGuess(initial, 51)
        assertEquals(GuessFeedback.TOO_HIGH, wrongGuess.feedback)
        assertEquals(50L, wrongGuess.newState.targetNumber)
        assertEquals(1L, wrongGuess.newState.attempts)
        assertTrue(store.save(1, wrongGuess.newState))

        val reloadedAfterWrongGuess = store.load(1)
        assertNotNull(reloadedAfterWrongGuess)
        assertEquals(wrongGuess.newState, reloadedAfterWrongGuess)

        val correctGuess = engine.processGuess(reloadedAfterWrongGuess!!, 50)
        assertEquals(GuessFeedback.CORRECT, correctGuess.feedback)
        assertEquals(2L, correctGuess.newState.attempts)
        assertEquals(1L, correctGuess.newState.correctGuesses)
        assertEquals(2L, correctGuess.newState.statistics.totalGuesses)
        assertEquals(1L, correctGuess.newState.statistics.correctGuesses)
        assertTrue(correctGuess.newState.targetNumber in 1L..100L)
        assertTrue(store.save(1, correctGuess.newState))

        assertEquals(correctGuess.newState, store.load(1))
    }
}
