package com.jarrlyyy.guessthenumber.desktop.persistence

import com.jarrlyyy.guessthenumber.domain.engine.GameEngine
import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyTransactions
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
    @Test
    fun prestigeAndUltraResetTransactionsSurviveDesktopSaveReload() = runBlocking {
        val directory = Files.createTempDirectory("gtn-reset-save-parity")
        val store = DesktopSaveStore(directory)
        val engine = GameEngine()

        val prestigeInput = GameState(
            money = BigNumber("50000000"),
            prestigeUpgradeLevels = mapOf(
                "prestige_money_boost" to 2,
                "prestige_gain_boost" to 5,
                "prestige_starting_capital" to 2
            ),
            prestigeShopPurchases = setOf("talent_crit_1")
        )
        val prestige = DesktopEconomyTransactions.prestigeReset(prestigeInput, engine)
        assertTrue(prestige.accepted)
        assertEquals(0, prestige.state.money.compareTo(BigNumber(2_000)))
        assertEquals(prestigeInput.prestigeUpgradeLevels, prestige.state.prestigeUpgradeLevels)
        assertEquals(prestigeInput.prestigeShopPurchases, prestige.state.prestigeShopPurchases)
        assertTrue(store.save(1, prestige.state))
        assertEquals(prestige.state, store.load(1))

        val ultraInput = GameState(
            money = BigNumber("1000000000000"),
            prestige = BigNumber(1_000),
            prestigeUpgradeLevels = mapOf("prestige_starting_capital" to 3),
            ultraUpgradeLevels = mapOf("ultra_money_boost" to 2, "ultra_gain_boost" to 2),
            shopPurchases = setOf("nebula_boost")
        )
        val ultra = DesktopEconomyTransactions.ultraReset(ultraInput, engine)
        assertTrue(ultra.accepted)
        assertEquals(0, ultra.state.money.compareTo(BigNumber(3_000)))
        assertTrue(ultra.state.prestigeUpgradeLevels.isEmpty())
        assertEquals(ultraInput.ultraUpgradeLevels, ultra.state.ultraUpgradeLevels)
        assertEquals(ultraInput.shopPurchases, ultra.state.shopPurchases)
        assertTrue(store.save(2, ultra.state))
        assertEquals(ultra.state, store.load(2))
    }

}
