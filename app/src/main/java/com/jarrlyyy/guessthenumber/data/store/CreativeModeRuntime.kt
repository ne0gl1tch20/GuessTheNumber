package com.jarrlyyy.guessthenumber.data.store

import java.util.UUID

/**
 * Isolated, deterministic Creative Mode run state. This model deliberately has no currency,
 * reward, progression, filesystem, or scripting capability. The host owns persistence and UI.
 */
data class CreativeModeRun(
    val runId: String = UUID.randomUUID().toString(),
    val rangeMin: Long,
    val rangeMax: Long,
    val target: Long,
    val attemptsUsed: Int = 0,
    val correctGuesses: Int = 0,
    val hintsUsed: Int = 0,
    val completed: Boolean = false
) {
    init {
        require(Regex("[a-zA-Z0-9_-]{8,64}").matches(runId))
        require(rangeMin >= 1L && rangeMax >= rangeMin && rangeMax <= CreativeModeSave.MAX_RANGE)
        require(target in rangeMin..rangeMax)
        require(attemptsUsed >= 0 && correctGuesses >= 0 && hintsUsed >= 0)
        require(correctGuesses <= attemptsUsed)
    }
}

enum class CreativeGuessOutcome { TOO_LOW, TOO_HIGH, CORRECT, OUT_OF_RANGE, ATTEMPT_LIMIT, COMPLETED }

data class CreativeGuessDecision(
    val outcome: CreativeGuessOutcome,
    val run: CreativeModeRun,
    val accepted: Boolean
)

/**
 * Applies only validated Creative Mode rules. A run is immutable, so a rejected guess cannot
 * consume an attempt and a completed run cannot be mutated by submitting more guesses.
 */
object CreativeModeRuntime {
    fun start(save: CreativeModeSave, target: Long): CreativeModeRun? {
        val rules = CreativeModeEffectPolicy.resolve(save) ?: return null
        if (!save.enabled || target !in rules.rangeMin..rules.rangeMax) return null
        return runCatching {
            CreativeModeRun(
                runId = UUID.randomUUID().toString(),
                rangeMin = rules.rangeMin,
                rangeMax = rules.rangeMax,
                target = target
            )
        }.getOrNull()
    }

    fun resume(save: CreativeModeSave): CreativeModeRun? {
        val snapshot = save.activeRun ?: return null
        if (!save.enabled || !CreativeModeSaveCodec.validate(save) ||
            !CreativeModeSaveCodec.validateRunSnapshot(snapshot)) return null
        return runCatching {
            CreativeModeRun(
                runId = snapshot.runId,
                rangeMin = snapshot.rangeMin,
                rangeMax = snapshot.rangeMax,
                target = snapshot.target,
                attemptsUsed = snapshot.attemptsUsed,
                correctGuesses = snapshot.correctGuesses,
                hintsUsed = snapshot.hintsUsed,
                completed = snapshot.completed
            )
        }.getOrNull()
    }

    fun persist(save: CreativeModeSave, run: CreativeModeRun): CreativeModeSave? {
        if (!save.enabled || !CreativeModeSaveCodec.validate(save)) return null
        val snapshot = CreativeModeRunSnapshot(
            runId = run.runId,
            rangeMin = run.rangeMin,
            rangeMax = run.rangeMax,
            target = run.target,
            attemptsUsed = run.attemptsUsed,
            correctGuesses = run.correctGuesses,
            hintsUsed = run.hintsUsed,
            completed = run.completed
        )
        return save.copy(activeRun = snapshot).takeIf(CreativeModeSaveCodec::validate)
    }

    fun clearPersistedRun(save: CreativeModeSave): CreativeModeSave =
        save.copy(activeRun = null)

    fun submitGuess(save: CreativeModeSave, run: CreativeModeRun, guess: Long): CreativeGuessDecision {
        val rules = CreativeModeEffectPolicy.resolve(save)
            ?: return CreativeGuessDecision(CreativeGuessOutcome.COMPLETED, run, false)
        if (!save.enabled || run.completed) {
            return CreativeGuessDecision(CreativeGuessOutcome.COMPLETED, run, false)
        }
        if (guess !in run.rangeMin..run.rangeMax) {
            return CreativeGuessDecision(CreativeGuessOutcome.OUT_OF_RANGE, run, false)
        }
        if (rules.attemptLimit != null && run.attemptsUsed >= rules.attemptLimit) {
            return CreativeGuessDecision(CreativeGuessOutcome.ATTEMPT_LIMIT, run, false)
        }
        val nextAttempts = run.attemptsUsed + 1
        return when {
            guess < run.target -> CreativeGuessDecision(
                CreativeGuessOutcome.TOO_LOW, run.copy(attemptsUsed = nextAttempts), true
            )
            guess > run.target -> CreativeGuessDecision(
                CreativeGuessOutcome.TOO_HIGH, run.copy(attemptsUsed = nextAttempts), true
            )
            else -> CreativeGuessDecision(
                CreativeGuessOutcome.CORRECT,
                run.copy(attemptsUsed = nextAttempts, correctGuesses = run.correctGuesses + 1, completed = true),
                true
            )
        }
    }

    fun useHint(save: CreativeModeSave, run: CreativeModeRun): CreativeModeRun? {
        val rules = CreativeModeEffectPolicy.resolve(save) ?: return null
        if (!save.enabled || run.completed || run.hintsUsed >= rules.extraHints) return null
        return run.copy(hintsUsed = run.hintsUsed + 1)
    }
}
