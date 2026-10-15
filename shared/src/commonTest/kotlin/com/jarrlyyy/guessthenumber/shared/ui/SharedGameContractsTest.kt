package com.jarrlyyy.guessthenumber.shared.ui

import com.jarrlyyy.guessthenumber.shared.domain.GuessGameReducer
import com.jarrlyyy.guessthenumber.shared.domain.GuessGameStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedGameContractsTest {
    @Test
    fun sharedDestinationRoutesAreUniqueAndRoundTrip() {
        val routes = SharedGameDestination.entries.map { it.route }

        assertEquals(routes.size, routes.toSet().size)
        SharedGameDestination.entries.forEach { destination ->
            assertEquals(destination, SharedGameDestination.fromRoute(destination.route))
        }
        assertNull(SharedGameDestination.fromRoute("unknown"))
    }

    @Test
    fun actionsCarryHostIndependentIntent() {
        assertEquals(
            42L,
            (SharedGameAction.SubmitGuess(42L) as SharedGameAction.SubmitGuess).guess
        )
        assertEquals(
            SharedGameDestination.SHOP,
            (SharedGameAction.Navigate(SharedGameDestination.SHOP) as SharedGameAction.Navigate).destination
        )
        assertEquals(SharedGameAction.StartNewGame, SharedGameAction.StartNewGame)
    }

    @Test
    fun presentationSnapshotReflectsSharedGameState() {
        val state = GuessGameReducer.submitGuess(
            GuessGameReducer.newGame(targetNumber = 42, rangeMin = 1, rangeMax = 99),
            42L
        )

        assertEquals(
            SharedGuessPresentationState(
                rangeMin = 1,
                rangeMax = 99,
                guessCount = 1,
                status = GuessGameStatus.CORRECT,
                solved = true
            ),
            SharedGuessPresentationState.from(state)
        )
    }
}
