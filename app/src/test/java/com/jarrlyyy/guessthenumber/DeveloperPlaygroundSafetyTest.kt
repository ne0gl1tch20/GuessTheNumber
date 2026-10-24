package com.jarrlyyy.guessthenumber

import com.jarrlyyy.guessthenumber.data.store.SaveFixtureLab
import com.jarrlyyy.guessthenumber.domain.engine.OfflineEarningsCalculator
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DeveloperPlaygroundSafetyTest {
    @Test
    fun offlineEarningsUsesSharedCapAndRate() {
        val active = GameState(autoClickerActive = true)
        assertEquals(0, OfflineEarningsCalculator.calculate(active, 0).compareTo(BigNumber.ZERO))
        assertEquals(0, OfflineEarningsCalculator.calculate(active, 60).value.compareTo(BigDecimal("30000")))
        assertEquals(0, OfflineEarningsCalculator.calculate(active, 86_400).value.compareTo(BigDecimal("14400000")))
    }

    @Test
    fun offlineEarningsRequiresActiveAutoClicker() {
        assertEquals(
            0,
            OfflineEarningsCalculator.calculate(GameState(autoClickerActive = false), 3600)
                .compareTo(BigNumber.ZERO)
        )
    }

    @Test
    fun fixtureSuitePassesWithoutTouchingDataStore() {
        val results = SaveFixtureLab.runSafetySuite(GameState(lastSaveTimestamp = 1))
        assertEquals(6, results.size)
        assertTrue(results.joinToString { "${it.name}: ${it.detail}" }, results.all { it.passed })
    }

    @Test
    fun fixtureValidatorRejectsNegativeMoneyAndMalformedJson() {
        val state = GameState(lastSaveTimestamp = 1)
        val negative = SaveFixtureLab.encode(state.copy(money = BigNumber(-1)))
        assertFalse(SaveFixtureLab.validate(negative).passed)
        assertFalse(SaveFixtureLab.validate("{broken").passed)
    }
}
