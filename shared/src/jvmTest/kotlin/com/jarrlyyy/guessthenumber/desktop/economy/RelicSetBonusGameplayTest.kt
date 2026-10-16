package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class RelicSetBonusGameplayTest {
    private fun apply(set: Set<String>): GameState {
        val before = GameState(
            money = BigNumber(100),
            equippedRelicIds = set,
            statistics = com.jarrlyyy.guessthenumber.domain.model.GameStatistics(
                moneyEarned = BigNumber(100)
            )
        )
        val after = before.copy(
            money = BigNumber(200),
            statistics = before.statistics.copy(moneyEarned = BigNumber(200))
        )
        return DesktopEconomyTransactions.applyGuessBonuses(
            before = before,
            after = after,
            correct = true,
            rng = Random(1234)
        )
    }

    @Test
    fun twoEquippedBossRelicsApplyTenPercentSetBonus() {
        val result = apply(setOf("verdant_guardian", "ember_dragon"))
        assertEquals(0, result.money.compareTo(BigNumber(245)))
    }

    @Test
    fun threeEquippedBossRelicsApplyTwentyPercentSetBonus() {
        val result = apply(setOf("verdant_guardian", "ember_dragon", "nebula_titan"))
        assertEquals(0, result.money.compareTo(BigNumber(280)))
    }

    @Test
    fun fourEquippedBossRelicsApplyThirtyFivePercentSetBonusAndNebula() {
        val result = apply(setOf("verdant_guardian", "crystal_golem", "ember_dragon", "nebula_titan"))
        // Individual relic effects add 10% + 25% + 25%, plus the 35% set bonus.
        assertEquals(0, result.money.compareTo(BigNumber(295)))
        // Android also grants +1 Nebula from Ember Dragon, in addition to the full-set +1.
        assertEquals(BigNumber(2), result.nebula)
    }

    @Test
    fun oneEquippedBossRelicDoesNotActivateSetBonus() {
        val result = apply(setOf("verdant_guardian"))
        assertEquals(0, result.money.compareTo(BigNumber(210)))
        assertEquals(BigNumber.ZERO, result.nebula)
    }
}
