package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopMutatorsContentTest {
    @Test
    fun weeklyChallengeIdsAndRewardsAreStableForCurrentWeek() {
        val first = desktopWeeklyChallenges()
        val second = desktopWeeklyChallenges()
        assertEquals(3, first.size)
        assertEquals(first, second)
        assertEquals(3, first.map { it.id }.distinct().size)
        assertEquals(listOf(75L, 90L, 100L, 80L, 125L, 175L, 110L, 95L}.toSet().containsAll(first.map { it.rewardNebula }.toSet()), true)
    }
}
