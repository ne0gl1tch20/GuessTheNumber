package com.jarrlyyy.guessthenumber.traits

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TraitSeasonPolicyTest {
    @Test fun everyMonthMapsToExpectedSeason() {
        val expected = listOf(
            "winter", "winter", "spring", "spring", "spring", "summer",
            "summer", "summer", "autumn", "autumn", "autumn", "winter"
        )
        for (month in 1..12) {
            assertEquals(expected[month - 1], TraitSeasonPolicy.seasonFor(LocalDate.of(2026, month, 15)).id)
        }
    }

    @Test fun winterCrossesYearBoundaryAndLeapDayIsHandled() {
        val winterStart = TraitSeasonPolicy.seasonFor(LocalDate.of(2026, 12, 1))
        assertTrue(winterStart.contains(LocalDate.of(2027, 1, 1)))
        val leapWinter = TraitSeasonPolicy.seasonFor(LocalDate.of(2028, 2, 29))
        assertTrue(leapWinter.contains(LocalDate.of(2028, 2, 29)))
        assertFalse(leapWinter.contains(LocalDate.of(2028, 3, 1)))
    }

    @Test fun seasonEndIsExclusiveAndSeasonCheckIsDeterministic() {
        assertTrue(TraitSeasonPolicy.isActive("spring", LocalDate.of(2026, 5, 31)))
        assertFalse(TraitSeasonPolicy.isActive("spring", LocalDate.of(2026, 6, 1)))
        val spring = TraitSeasonPolicy.seasonFor(LocalDate.of(2026, 5, 31))
        assertEquals(0L, spring.daysRemaining(LocalDate.of(2026, 6, 1)))
    }
}
