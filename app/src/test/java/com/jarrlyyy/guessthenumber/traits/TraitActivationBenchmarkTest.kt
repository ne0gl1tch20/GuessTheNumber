package com.jarrlyyy.guessthenumber.traits

import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
        assertSame("Sorted catalog view should be cached", registry.all(), registry.all())
        assertSame("Visible catalog view should be cached", registry.visible(), registry.visible())
        assertSame("Hidden catalog view should be cached", registry.hidden(), registry.hidden())

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
        val report = buildString {
            appendLine("TRAIT ACTIVATION PROFILE")
            appendLine("javaVersion=${System.getProperty("java.version")}")
            appendLine("vmName=${System.getProperty("java.vm.name")}")
            appendLine("catalogSize=${registry.size}")
            appendLine("warmupIterations=$WARMUP_ITERATIONS")
            appendLine("samples=$SAMPLE_COUNT")
            appendLine("iterationsPerSample=$ITERATIONS_PER_SAMPLE")
            appendLine("medianNsPerEvaluation=$medianNanos")
            appendLine("p95NsPerEvaluation=$p95Nanos")
            appendLine("medianEvaluationsPerSecond=$evaluationsPerSecond")
            appendLine("deterministic=true")
            appendLine("cachedRegistryViews=true")
        }
        val reportFile = File("build/reports/trait-activation-benchmark.txt")
        reportFile.parentFile?.mkdirs()
        reportFile.writeText(report)
        println("TRAIT_ACTIVATION_BENCHMARK " + report.replace("\n", " ").trim())
    }

    private companion object {
        const val WARMUP_ITERATIONS = 1_000
        const val SAMPLE_COUNT = 21
        const val ITERATIONS_PER_SAMPLE = 200
    }
}
