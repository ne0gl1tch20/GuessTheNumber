package com.jarrlyyy.guessthenumber.traits

import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Repeatable JVM microbenchmark for the trait activation hot path.
 *
 * This deliberately reports timings instead of enforcing a wall-clock threshold:
 * shared CI runners vary, so correctness must never depend on machine speed.
 */
class TraitActivationBenchmarkTest {
    @Test
    fun activationHotPathReportsWarmThroughputAndKeepsResultsDeterministic() {
        val registry = BuiltInTraits.registry()
        val engine = TraitActivationEngine(registry)
        val context = TraitActivationContext(
            state = GameState(
                correctGuesses = 175,
                bestStreak = 30,
                prestigeCount = 2
            ),
            playerLevel = 12,
            activeSeasonId = "autumn",
            discoveredTraitIds = setOf("hidden_night_owl", "hidden_century_club")
        )

        assertTrue("Benchmark should exercise the expanded catalog", registry.size >= 316)

        repeat(WARMUP_ITERATIONS) { engine.evaluate(context) }
        val expected = engine.evaluate(context).activeTraitIds
        val samples = LongArray(SAMPLE_COUNT)

        repeat(SAMPLE_COUNT) { sample ->
            val started = System.nanoTime()
            repeat(ITERATIONS_PER_SAMPLE) { engine.evaluate(context) }
            samples[sample] = (System.nanoTime() - started) / ITERATIONS_PER_SAMPLE
        }

        val ordered = samples.sorted()
        val medianNanos = ordered[ordered.size / 2]
        val p95Nanos = ordered[((ordered.size - 1) * 95) / 100]
        val evaluationsPerSecond = if (medianNanos == 0L) Long.MAX_VALUE else 1_000_000_000L / medianNanos

        assertEquals("Activation results must be deterministic", expected, engine.evaluate(context).activeTraitIds)
        println(
            "TRAIT_ACTIVATION_BENCHMARK " +
                "catalogSize=${registry.size} warmup=$WARMUP_ITERATIONS " +
                "samples=$SAMPLE_COUNT iterationsPerSample=$ITERATIONS_PER_SAMPLE " +
                "medianNsPerEvaluation=$medianNanos p95NsPerEvaluation=$p95Nanos " +
                "medianEvaluationsPerSecond=$evaluationsPerSecond"
        )
    }

    private companion object {
        const val WARMUP_ITERATIONS = 1_000
        const val SAMPLE_COUNT = 21
        const val ITERATIONS_PER_SAMPLE = 200
    }
}
