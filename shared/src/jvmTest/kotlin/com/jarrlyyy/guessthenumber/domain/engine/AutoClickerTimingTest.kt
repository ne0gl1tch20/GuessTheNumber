package com.jarrlyyy.guessthenumber.domain.engine

import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoClickerTimingTest {
    private val engine = GameEngine()

    @Test
    fun defaultAutoClickerUsesOneSecondInterval() {
        assertEquals(1_000L, engine.autoClickerDelayMillis(GameState(autoClickerActive = true)))
    }

    @Test
    fun speedTalentsStackLikeAndroid() {
        val firstTalent = GameState(
            autoClickerActive = true,
            prestigeShopPurchases = setOf("talent_speed_1")
        )
        val bothTalents = firstTalent.copy(
            prestigeShopPurchases = setOf("talent_speed_1", "talent_speed_2")
        )

        assertEquals(800L, engine.autoClickerDelayMillis(firstTalent))
        assertEquals(533L, engine.autoClickerDelayMillis(bothTalents))
    }

    @Test
    fun speedMutatorStacksWithTalentSpeed() {
        val state = GameState(
            autoClickerActive = true,
            activeMutators = setOf("mut_speed"),
            prestigeShopPurchases = setOf("talent_speed_1", "talent_speed_2")
        )

        assertEquals(177L, engine.autoClickerDelayMillis(state))
    }

    @Test
    fun inactiveOrNonPositiveSpeedFallsBackToOneSecond() {
        assertEquals(1_000L, engine.autoClickerDelayMillis(GameState(autoClickerActive = false)))
        assertEquals(
            1_000L,
            engine.autoClickerDelayMillis(GameState(autoClickerActive = true, autoClickerSpeed = 0.0))
        )
    }
}
