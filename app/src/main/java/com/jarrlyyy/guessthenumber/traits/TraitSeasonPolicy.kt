package com.jarrlyyy.guessthenumber.traits

import java.time.LocalDate
import java.time.YearMonth

/**
 * Deterministic calendar policy for seasonal traits. It is offline-first and uses the supplied
 * date rather than a network clock, making boundary behavior testable and reproducible.
 */
data class TraitSeason(
    val id: String,
    val start: LocalDate,
    val endExclusive: LocalDate
) {
    fun contains(date: LocalDate): Boolean = date >= start && date < endExclusive
    fun daysRemaining(date: LocalDate): Long =
        if (contains(date)) java.time.temporal.ChronoUnit.DAYS.between(date, endExclusive) else 0L
}

object TraitSeasonPolicy {
    fun seasonFor(date: LocalDate): TraitSeason {
        val year = date.year
        return when (date.monthValue) {
            12 -> TraitSeason("winter", LocalDate.of(year, 12, 1), LocalDate.of(year + 1, 3, 1))
            1, 2 -> TraitSeason("winter", LocalDate.of(year - 1, 12, 1), LocalDate.of(year, 3, 1))
            in 3..5 -> TraitSeason("spring", LocalDate.of(year, 3, 1), LocalDate.of(year, 6, 1))
            in 6..8 -> TraitSeason("summer", LocalDate.of(year, 6, 1), LocalDate.of(year, 9, 1))
            else -> TraitSeason("autumn", LocalDate.of(year, 9, 1), LocalDate.of(year, 12, 1))
        }
    }

    fun isActive(seasonId: String, date: LocalDate): Boolean =
        seasonId == seasonFor(date).id

    fun monthsRemaining(date: LocalDate): Long {
        val season = seasonFor(date)
        val current = YearMonth.from(date)
        val endMonth = YearMonth.from(season.endExclusive)
        return java.time.temporal.ChronoUnit.MONTHS.between(current, endMonth).coerceAtLeast(0L)
    }
}
