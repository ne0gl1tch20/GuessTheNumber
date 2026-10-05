package com.jarrlyyy.guessthenumber.domain.model

object Difficulty {
    const val CLASSIC = "classic"
    const val HARD = "hard"
    const val EXTREME = "extreme"

    val ids: List<String> = listOf(CLASSIC, HARD, EXTREME)

    fun isValid(id: String): Boolean = id in ids

    fun nameKey(id: String): String = when (id) {
        HARD -> "difficulty_hard"
        EXTREME -> "difficulty_extreme"
        else -> "difficulty_classic"
    }

    fun baseRangeMax(id: String): Long = when (id) {
        HARD -> 150L
        EXTREME -> 250L
        else -> 100L
    }

    fun rangeMax(id: String, betterRangeLevel: Int): Long =
        maxOf(50L, baseRangeMax(id) - (betterRangeLevel * 5L))

    fun rewardMultiplier(id: String): Double = when (id) {
        HARD -> 1.5
        EXTREME -> 2.5
        else -> 1.0
    }

    fun wrongGuessPenalty(id: String): Double = when (id) {
        HARD -> 0.02
        EXTREME -> 0.05
        else -> 0.0
    }
