package com.jarrlyyy.guessthenumber.shared.engine

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Representative JVM baseline for the Phase 6 native adaptive-target core.
 * The algorithm intentionally mirrors app/src/main/cpp/gtn_native_core.h.
 */
class NativeVsKotlinPerformanceBenchmarkTest {
    @Test
    fun kotlinCoreReportsMedianP95AndThroughput() {
        val cases = BenchmarkCaseSet
        repeat(WARMUP_ITERATIONS) { iteration ->
            for (case in cases) kotlinAdaptiveTarget(case, iteration)
        }

        var expectedChecksum = 0L
        for (case in cases) expectedChecksum = expectedChecksum xor kotlinAdaptiveTarget(case, 0)
        val samples = DoubleArray(SAMPLE_COUNT)
        repeat(SAMPLE_COUNT) {
            val started = System.nanoTime()
            var checksum = 0L
            repeat(ITERATIONS_PER_SAMPLE) { iteration ->
                for (case in cases) checksum = checksum xor kotlinAdaptiveTarget(case, iteration)
            }
            sink = checksum
            samples[it] = (System.nanoTime() - started).toDouble() / (ITERATIONS_PER_SAMPLE * cases.size)
        }

        val ordered = samples.sorted()
        val median = ordered[ordered.size / 2]
        val p95 = ordered[((ordered.size - 1) * 95) / 100]
        val throughput = if (median <= 0.0) 0.0 else 1_000_000_000.0 / median

        assertTrue("Benchmark samples must be positive", samples.all { it > 0.0 })
        assertTrue("Representative output must remain bounded", cases.all {
            val result = kotlinAdaptiveTarget(it, 0)
            result in it.rangeMin..it.rangeMax
        })

        val report = buildString {
            appendLine("KOTLIN_CORE")
            appendLine("javaVersion=${System.getProperty("java.version")}")
            appendLine("vmName=${System.getProperty("java.vm.name")}")
            appendLine("cases=${cases.size}")
            appendLine("warmupIterations=$WARMUP_ITERATIONS")
            appendLine("samples=$SAMPLE_COUNT")
            appendLine("iterationsPerSample=$ITERATIONS_PER_SAMPLE")
            appendLine("medianNsPerEvaluation=%.3f".format(median))
            appendLine("p95NsPerEvaluation=%.3f".format(p95))
            appendLine("medianEvaluationsPerSecond=%.3f".format(throughput))
            appendLine("checksum=$expectedChecksum")
            appendLine("bounded=true")
        }
        val reportFile = File("app/build/reports/native-vs-kotlin-kotlin.txt")
        reportFile.parentFile?.mkdirs()
        reportFile.writeText(report)
        println("NATIVE_VS_KOTLIN_KOTLIN_BENCHMARK " + report.replace("\n", " ").trim())
    }

    private fun kotlinAdaptiveTarget(case: BenchmarkCase, iteration: Int): Long {
        if (case.rangeMax < case.rangeMin) return case.rangeMin
        val width = (case.rangeMax - case.rangeMin) + 1L
        if (width <= 0L) return case.rangeMin

        var state = case.seed xor iteration.toLong() * ITERATION_SALT
        state = state xor difficultySalt(case.difficulty)
        state = state xor maxOf(0, case.streak).toLong() * STREAK_MULTIPLIER
        state = state xor maxOf(0L, case.attempts) * ATTEMPTS_MULTIPLIER

        val sample = mix(state)
        var offset = (sample.toULong() % width.toULong()).toLong()
        val pressure = minOf(case.streak, 32).toLong() + when (case.difficulty) {
            "extreme" -> 3L
            "hard" -> 2L
            else -> 1L
        }
        if (width > 4L) {
            val edgeBand = minOf(width / 4L, pressure * width / 128L)
            if (edgeBand > 0L && (sample and 1L) != 0L) {
                offset = minOf(width - 1L, offset + edgeBand)
            }
        }
        return case.rangeMin + offset
    }

    private fun mix(input: Long): Long {
        var value = input + MIX_INCREMENT
        value = (value xor (value ushr 30)) * MIX_MULTIPLIER_A
        value = (value xor (value ushr 27)) * MIX_MULTIPLIER_B
        return value xor (value ushr 31)
    }

    private fun difficultySalt(difficulty: String): Long = when (difficulty) {
        "extreme" -> 0xE17E4E9B5A7C31D1UL.toLong()
        "hard" -> 0xA11CE5D4B8F23071UL.toLong()
        else -> 0xC1A551C0DE2026A1UL.toLong()
    }

    private data class BenchmarkCase(
        val rangeMin: Long,
        val rangeMax: Long,
        val difficulty: String,
        val streak: Int,
        val attempts: Long,
        val seed: Long
    )

    private object BenchmarkCaseSet : ArrayList<BenchmarkCase>() {
        init {
            for (index in 0 until 128) {
                add(
                    BenchmarkCase(
                        rangeMin = 1L,
                        rangeMax = 1000L + (index % 4) * 9000L,
                        difficulty = when (index % 3) {
                            0 -> "classic"
                            1 -> "hard"
                            else -> "extreme"
                        },
                        streak = index % 41,
                        attempts = (index * 17L) % 500L,
                        seed = 0x1234ABCDL + index * 0x9E37L
                    )
                )
            }
        }
    }

    private companion object {
        const val WARMUP_ITERATIONS = 2_000
        const val SAMPLE_COUNT = 21
        const val ITERATIONS_PER_SAMPLE = 5_000
        const val MIX_INCREMENT = -7046029254386353131L
        const val MIX_MULTIPLIER_A = -4658895280553007687L
        const val MIX_MULTIPLIER_B = -7723592293110705685L
        const val STREAK_MULTIPLIER = -2960836687051489901L
        const val ATTEMPTS_MULTIPLIER = -6511265742569879269L
        const val ITERATION_SALT = -7046029254386353131L

        @Volatile
        var sink = 0L
    }
}
