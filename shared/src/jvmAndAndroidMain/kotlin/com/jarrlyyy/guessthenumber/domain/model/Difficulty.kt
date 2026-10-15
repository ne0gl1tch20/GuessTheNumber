package com.jarrlyyy.guessthenumber.domain.model

import com.jarrlyyy.guessthenumber.shared.domain.DifficultyRules

object Difficulty {
    const val CLASSIC = DifficultyRules.CLASSIC
    const val HARD = DifficultyRules.HARD
    const val EXTREME = DifficultyRules.EXTREME

    val ids: List<String> = DifficultyRules.ids

    fun isValid(id: String): Boolean = DifficultyRules.isValid(id)

    fun nameKey(id: String): String = DifficultyRules.nameKey(id)

    fun baseRangeMax(id: String): Long = DifficultyRules.baseRangeMax(id)

    fun rangeMax(id: String, betterRangeLevel: Int): Long =
        DifficultyRules.rangeMax(id, betterRangeLevel)

    fun rewardMultiplier(id: String): Double = DifficultyRules.rewardMultiplier(id)

    fun wrongGuessPenalty(id: String): Double = DifficultyRules.wrongGuessPenalty(id)
}
