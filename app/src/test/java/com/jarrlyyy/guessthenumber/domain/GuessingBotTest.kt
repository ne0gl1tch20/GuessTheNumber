package com.jarrlyyy.guessthenumber.domain

import com.jarrlyyy.guessthenumber.domain.engine.GuessFeedback
import com.jarrlyyy.guessthenumber.domain.engine.GuessingBot
import org.junit.Assert.assertEquals
import org.junit.Test

class GuessingBotTest {

    @Test
    fun binarySearchBotFindsTargetUsingOnlyFeedback() {
        val bot = GuessingBot()
        bot.reset(1L, 100L)

        val target = 73L
        var guess = bot.nextGuess(1L, 100L)
        var feedback = feedbackFor(guess, target)
        bot.observeGuess(guess, feedback)

        repeat(10) {
            if (feedback == GuessFeedback.CORRECT) return@repeat
            guess = bot.nextGuess(1L, 100L)
            feedback = feedbackFor(guess, target)
            bot.observeGuess(guess, feedback)
        }

        assertEquals(target, guess)
        assertEquals(GuessFeedback.CORRECT, feedback)
    }

    @Test
    fun botRespectsDifficultyRanges() {
        val bot = GuessingBot()
        bot.reset(1L, 250L)

        val firstGuess = bot.nextGuess(1L, 250L)

        assertEquals(125L, firstGuess)
    }

    private fun feedbackFor(guess: Long, target: Long): GuessFeedback = when {
        guess < target -> GuessFeedback.TOO_LOW
        guess > target -> GuessFeedback.TOO_HIGH
        else -> GuessFeedback.CORRECT
    }
}
