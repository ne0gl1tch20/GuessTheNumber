package com.jarrlyyy.guessthenumber.data.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreativeModeRuntimeTest {
    private fun save(vararg effects: CreativeModeEffect) = CreativeModeSave(
        enabled = true,
        rangeMin = 10,
        rangeMax = 20,
        effects = effects.toList()
    )

    @Test fun sessionUsesValidatedRangeAndDoesNotTouchEconomyState() {
        val config = save(CreativeModeEffect("hints", CreativeEffectType.EXTRA_HINTS, 2))
        val run = CreativeModeRuntime.start(config, 15)
        assertNotNull(run)
        assertEquals(10L, run?.rangeMin)
        assertEquals(20L, run?.rangeMax)
        assertEquals(0, run?.attemptsUsed)
        assertEquals(0, run?.correctGuesses)
    }

    @Test fun wrongGuessConsumesOneAttemptAndCorrectGuessCompletesRun() {
        val config = save(CreativeModeEffect("limit", CreativeEffectType.ATTEMPT_LIMIT, 3))
        val run = CreativeModeRuntime.start(config, 15)!!
        val low = CreativeModeRuntime.submitGuess(config, run, 12)
        assertEquals(CreativeGuessOutcome.TOO_LOW, low.outcome)
        assertEquals(1, low.run.attemptsUsed)
        val correct = CreativeModeRuntime.submitGuess(config, low.run, 15)
        assertEquals(CreativeGuessOutcome.CORRECT, correct.outcome)
        assertTrue(correct.run.completed)
        assertEquals(1, correct.run.correctGuesses)
        val after = CreativeModeRuntime.submitGuess(config, correct.run, 15)
        assertFalse(after.accepted)
        assertEquals(CreativeGuessOutcome.COMPLETED, after.outcome)
    }

    @Test fun invalidRangeAndAttemptLimitDoNotConsumeAttempts() {
        val config = save(CreativeModeEffect("limit", CreativeEffectType.ATTEMPT_LIMIT, 1))
        val run = CreativeModeRuntime.start(config, 15)!!
        val outside = CreativeModeRuntime.submitGuess(config, run, 9)
        assertEquals(CreativeGuessOutcome.OUT_OF_RANGE, outside.outcome)
        assertEquals(0, outside.run.attemptsUsed)
        val first = CreativeModeRuntime.submitGuess(config, run, 12)
        assertEquals(1, first.run.attemptsUsed)
        val blocked = CreativeModeRuntime.submitGuess(config, first.run, 15)
        assertEquals(CreativeGuessOutcome.ATTEMPT_LIMIT, blocked.outcome)
        assertFalse(blocked.accepted)
        assertEquals(1, blocked.run.attemptsUsed)
    }

    @Test fun hintsAreBoundedAndUnavailableAfterCompletion() {
        val config = save(CreativeModeEffect("hints", CreativeEffectType.EXTRA_HINTS, 1))
        val run = CreativeModeRuntime.start(config, 15)!!
        val used = CreativeModeRuntime.useHint(config, run)
        assertEquals(1, used?.hintsUsed)
        assertNull(CreativeModeRuntime.useHint(config, used!!))
        val completed = CreativeModeRuntime.submitGuess(config, used, 15).run
        assertNull(CreativeModeRuntime.useHint(config, completed))
    }

    @Test fun disabledOrInvalidCreativeConfigurationCannotStartARun() {
        assertNull(CreativeModeRuntime.start(CreativeModeSave(enabled = false), 50))
        assertNull(CreativeModeRuntime.start(
            CreativeModeSave(enabled = true, rangeMin = 10, rangeMax = 20), 21
        ))
    }
}
