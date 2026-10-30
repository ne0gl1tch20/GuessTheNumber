package com.jarrlyyy.guessthenumber.domain.engine

import java.math.BigDecimal
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineEarningsPolicyTest {
    @Test
    fun offlineEarningsAreSharedAndCappedAtEightHours() {
        val active = GameState(autoClickerActive = true)
        assertEquals(0, OfflineEarningsPolicy.calculate(active, 0).value.compareTo(BigDecimal.ZERO))
        assertEquals(0, OfflineEarningsPolicy.calculate(active, 60).value.compareTo(BigDecimal("30000")))
        assertEquals(0, OfflineEarningsPolicy.calculate(active, 86_400).value.compareTo(BigDecimal("14400000")))
        assertEquals(28_800L, OfflineEarningsPolicy.effectiveSeconds(86_400))
    }

    @Test
    fun inactiveAutoClickerNeverEarnsOfflineMoney() {
        assertEquals(0, OfflineEarningsPolicy.calculate(GameState(autoClickerActive = false), 3_600).value.compareTo(BigDecimal.ZERO))
    }
}
