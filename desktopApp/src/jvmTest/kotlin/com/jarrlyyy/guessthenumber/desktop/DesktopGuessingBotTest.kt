package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.shared.rules.GuessSourceContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopGuessingBotTest {
    @After
    fun clearPendingBotGuessMarker() {
        GuessSourceContext.clearBotGuess()
    }

    @Test
    fun botNarrowsOnlyFromFeedbackAndNeverNeedsTargetAccess() {
        val bot = DesktopGuessingBot()
        bot.reset(1, 100)

        assertEquals(50L, bot.nextGuess(1, 100))
        bot.observeGuess(50, GuessFeedback.TOO_LOW)
        assertEquals(75L, bot.nextGuess(1, 100))
        bot.observeGuess(75, GuessFeedback.TOO_HIGH)
        assertEquals(62L, bot.nextGuess(1, 100))
    }

    @Test
    fun resetStartsANewRoundRange() {
        val bot = DesktopGuessingBot()
        bot.reset(1, 100)
        bot.observeGuess(50, GuessFeedback.TOO_LOW)
        bot.reset(1, 200)

        assertEquals(100L, bot.nextGuess(1, 200))
    }
}
