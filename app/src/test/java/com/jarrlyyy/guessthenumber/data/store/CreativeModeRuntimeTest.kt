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

    @Test fun activeRunSnapshotSurvivesCodecRoundTripAndCanResume() {
        val config = save(CreativeModeEffect("hints", CreativeEffectType.EXTRA_HINTS, 2))
        val run = CreativeModeRuntime.start(config, 15)!!
        val firstGuess = CreativeModeRuntime.submitGuess(config, run, 12).run
        val usedHint = CreativeModeRuntime.useHint(config, firstGuess)!!
        val persisted = CreativeModeRuntime.persist(config, usedHint)!!
        val encoded = CreativeModeSaveCodec.encode(persisted)
        val decoded = CreativeModeSaveCodec.decodeAndMigrate(encoded)!!
        val resumed = CreativeModeRuntime.resume(decoded)!!
        assertEquals(usedHint.runId, resumed.runId)
        assertEquals(usedHint.attemptsUsed, resumed.attemptsUsed)
        assertEquals(usedHint.hintsUsed, resumed.hintsUsed)
        assertEquals(usedHint.target, resumed.target)
    }

    @Test fun versionTwoCreativeSavesMigrateWithoutInventingARun() {
        val old = """{"schemaVersion":2,"name":"Old v2","enabled":true,"rangeMin":1,"rangeMax":40,"effects":[]}"""
        val migrated = CreativeModeSaveCodec.decodeAndMigrate(old)
        assertNotNull(migrated)
        assertEquals(CreativeModeSave.CURRENT_SCHEMA_VERSION, migrated?.schemaVersion)
        assertNull(migrated?.activeRun)
    }

    @Test fun invalidRunSnapshotsAreRejected() {
        val bad = CreativeModeSave(
            enabled = true,
            activeRun = CreativeModeRunSnapshot(
                runId = "short",
                rangeMin = 1,
                rangeMax = 10,
                target = 11,
                attemptsUsed = -1
            )
        )
        assertFalse(CreativeModeSaveCodec.validate(bad))
        assertNull(CreativeModeRuntime.resume(bad))
    }


    @Test fun runtimeRejectsForgedOrStaleRunsThatDoNotMatchCurrentRules() {
        val config = save(CreativeModeEffect("hints", CreativeEffectType.EXTRA_HINTS, 1))
        val valid = CreativeModeRuntime.start(config, 15)!!
        val forged = valid.copy(rangeMax = 100)
        val decision = CreativeModeRuntime.submitGuess(config, forged, 15)
        assertEquals(CreativeGuessOutcome.INVALID_CONFIGURATION, decision.outcome)
        assertFalse(decision.accepted)
        assertNull(CreativeModeRuntime.persist(config, forged))
        val changedConfig = config.copy(effects = emptyList())
        val usedHint = CreativeModeRuntime.useHint(config, valid)!!
        val persisted = CreativeModeRuntime.persist(config, usedHint)!!
        assertNull(CreativeModeRuntime.resume(persisted.copy(effects = emptyList())))
        assertNull(CreativeModeRuntime.useHint(changedConfig, valid))
    }


    @Test fun allCreativeEffectsComposeIntoOneBoundedRuntimePolicy() {
        val config = CreativeModeSave(
            enabled = true,
            rangeMin = 10,
            rangeMax = 20,
            effects = listOf(
                CreativeModeEffect("scale-range", CreativeEffectType.RANGE_SCALE, 2),
                CreativeModeEffect("extra-hints", CreativeEffectType.EXTRA_HINTS, 3),
                CreativeModeEffect("limit-attempts", CreativeEffectType.ATTEMPT_LIMIT, 4),
                CreativeModeEffect("more-hints", CreativeEffectType.EXTRA_HINTS, 4),
                CreativeModeEffect("stricter-limit", CreativeEffectType.ATTEMPT_LIMIT, 2)
            )
        )
        val rules = CreativeModeEffectPolicy.resolve(config)!!
        assertEquals(10L, rules.rangeMin)
        assertEquals(31L, rules.rangeMax)
        assertEquals(7, rules.extraHints)
        assertEquals(2, rules.attemptLimit)
        val run = CreativeModeRuntime.start(config, 30)!!
        assertEquals(CreativeGuessOutcome.TOO_HIGH, CreativeModeRuntime.submitGuess(config, run, 31).outcome)
    }

    @Test fun completedSnapshotMustRepresentAnActualCorrectGuess() {
        val invalid = CreativeModeSave(
            enabled = true,
            activeRun = CreativeModeRunSnapshot(
                runId = "creative-run-123",
                rangeMin = 1,
                rangeMax = 10,
                target = 5,
                attemptsUsed = 0,
                correctGuesses = 0,
                completed = true
            )
        )
        assertFalse(CreativeModeSaveCodec.validate(invalid))
        val raw = """{"schemaVersion":3,"name":"My Creative Mode","enabled":true,"rangeMin":1,"rangeMax":100,"effects":[],"activeRun":{"runId":"creative-run-123","rangeMin":1,"rangeMax":10,"target":5,"attemptsUsed":0,"correctGuesses":0,"hintsUsed":0,"completed":true}}"""
        assertNull(CreativeModeSaveCodec.decodeAndMigrate(raw))
    }

}
