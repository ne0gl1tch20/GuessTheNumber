package com.jarrlyyy.guessthenumber.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GameStateCodecTest {
    @Test
    fun roundTripPreservesCurrenciesAndProgression() {
        val state = GameState(
            money = BigNumber("123.45"),
            prestige = BigNumber("2.5M"),
            correctGuesses = 17,
            streak = 4,
            unlockedWorldIds = setOf("verdant_grove", "crystal_caves")
        )

        val decoded = GameStateCodec.decode(GameStateCodec.encode(state))

        assertEquals(state.money, decoded.money)
        assertEquals(state.prestige, decoded.prestige)
        assertEquals(state.correctGuesses, decoded.correctGuesses)
        assertEquals(state.streak, decoded.streak)
        assertEquals(state.unlockedWorldIds, decoded.unlockedWorldIds)
    }

    @Test
    fun malformedSaveDoesNotDecode() {
        assertEquals(null, GameStateCodec.decodeOrNull("{broken json"))
        assertEquals(null, GameStateCodec.decodeOrNull(null))
        assertNotNull(GameStateCodec.decodeOrNull(GameStateCodec.encode(GameState())))
    }
}
