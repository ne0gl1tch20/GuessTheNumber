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
}
