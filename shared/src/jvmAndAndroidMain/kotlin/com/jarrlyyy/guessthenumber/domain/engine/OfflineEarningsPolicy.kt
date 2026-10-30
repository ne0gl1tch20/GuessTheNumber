package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState

/** Shared offline catch-up rule used by Android and desktop save loaders. */
object OfflineEarningsPolicy {
    const val MAX_OFFLINE_SECONDS = 28_800L
    val EARNINGS_PER_SECOND = BigNumber(500)

    fun effectiveSeconds(elapsedSeconds: Long): Long =
        elapsedSeconds.coerceIn(0L, MAX_OFFLINE_SECONDS)

    fun calculate(state: GameState, elapsedSeconds: Long): BigNumber {
        if (!state.autoClickerActive) return BigNumber.ZERO
        val seconds = effectiveSeconds(elapsedSeconds)
        if (seconds <= 0L) return BigNumber.ZERO
        return EARNINGS_PER_SECOND * BigNumber(seconds.toDouble())
    }
}
