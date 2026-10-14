package com.jarrlyyy.guessthenumber.shared.engine

import com.jarrlyyy.guessthenumber.shared.rules.GuessFeedback
import kotlin.test.Test
import kotlin.test.assertEquals

class GuessingBotTest {
    @Test
    fun narrowsRangeOnlyFromFeedback() {
        val bot = GuessingBot()
        bot.reset(1L, 100L)

        assertEquals(50L, bot.nextGuess(1L, 100L))
        bot.observeGuess(50L, GuessFeedback.TOO_LOW)
        assertEquals(75L, bot.nextGuess(1L, 100L))
        bot.observeGuess(75L, GuessFeedback.TOO_HIGH)
        assertEquals(62L, bot.nextGuess(1L, 100L))
    }
}
