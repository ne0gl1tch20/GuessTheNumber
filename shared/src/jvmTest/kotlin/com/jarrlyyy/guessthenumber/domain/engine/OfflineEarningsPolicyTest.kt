package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineEarningsPolicyTest {
    @Test
    fun offlineEarningsAreSharedAndCappedAtEightHours() {
        val active = GameState(autoClickerActive = true)
        assertEquals("0", OfflineEarningsPolicy.calculate(active, 0).format())
        assertEquals("30000", OfflineEarningsPolicy.calculate(active, 60).format())
        assertEquals("14400000", OfflineEarningsPolicy.calculate(active, 86_400).format())
        assertEquals(28_800L, OfflineEarningsPolicy.effectiveSeconds(86_400))
    }

    @Test
    fun inactiveAutoClickerNeverEarnsOfflineMoney() {
        assertEquals("0", OfflineEarningsPolicy.calculate(GameState(autoClickerActive = false), 3_600).format())
    }
}
