package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState

/**
 * Android-facing compatibility wrapper around the shared offline catch-up policy.
 * Keep this name stable for existing app callers and tests.
 */
object OfflineEarningsCalculator {
    const val MAX_OFFLINE_SECONDS = OfflineEarningsPolicy.MAX_OFFLINE_SECONDS
    val EARNINGS_PER_SECOND: BigNumber = OfflineEarningsPolicy.EARNINGS_PER_SECOND

    fun effectiveSeconds(elapsedSeconds: Long): Long =
        OfflineEarningsPolicy.effectiveSeconds(elapsedSeconds)

    fun calculate(state: GameState, elapsedSeconds: Long): BigNumber =
        OfflineEarningsPolicy.calculate(state, elapsedSeconds)
}
