package com.jarrlyyy.guessthenumber.shared.seasonal

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeasonalThemeCalendarTest {
    @Test
    fun newYearAndChristmasWindowsCrossTheYearBoundary() {
        assertEquals("new_year", SeasonalThemeCalendar.primaryTheme(LocalDate.of(2026, 12, 26))?.id)
        assertEquals("new_year", SeasonalThemeCalendar.primaryTheme(LocalDate.of(2027, 1, 7))?.id)
        assertNull(SeasonalThemeCalendar.primaryTheme(LocalDate.of(2027, 1, 8)))

        assertEquals("christmas", SeasonalThemeCalendar.primaryTheme(LocalDate.of(2026, 12, 1))?.id)
        assertTrue(SeasonalThemeCalendar.activeThemes(LocalDate.of(2027, 1, 6)).any { it.id == "christmas" })
        assertFalse(SeasonalThemeCalendar.activeThemes(LocalDate.of(2027, 1, 7)).any { it.id == "christmas" })
    }

    @Test
    fun fixedEventsUseInclusiveStartAndExclusiveEnd() {
        assertEquals("valentines", SeasonalThemeCalendar.primaryTheme(LocalDate.of(2027, 2, 7))?.id)
        assertEquals("valentines", SeasonalThemeCalendar.primaryTheme(LocalDate.of(2027, 2, 15))?.id)
        assertFalse(SeasonalThemeCalendar.activeThemes(LocalDate.of(2027, 2, 16)).any { it.id == "valentines" })

        assertEquals("halloween", SeasonalThemeCalendar.primaryTheme(LocalDate.of(2027, 10, 15))?.id)
        assertTrue(SeasonalThemeCalendar.activeThemes(LocalDate.of(2027, 11, 1)).any { it.id == "halloween" })
        assertFalse(SeasonalThemeCalendar.activeThemes(LocalDate.of(2027, 11, 2)).any { it.id == "halloween" })
    }

    @Test
    fun philippinesOnlyThemesDoNotLeakIntoOtherRegions() {
        val independenceDay = LocalDate.of(2027, 6, 12)
        assertTrue(SeasonalThemeCalendar.activeThemes(independenceDay).any {
            it.id == "philippine_independence_day"
        })
        assertFalse(SeasonalThemeCalendar.activeThemes(
            independenceDay,
            SeasonalThemeCalendarConfig(regionCode = "US")
        ).any { it.id.startsWith("philippine_") || it.id == "back_to_school" })
    }

    @Test
    fun philippinesSummerEndsBeforeConfiguredSchoolWindowBegins() {
        val active = SeasonalThemeCalendar.activeThemes(LocalDate.of(2027, 6, 15))
        assertTrue(active.any { it.id == "back_to_school" })
        assertTrue(active.none { it.id == "philippine_summer" })
    }

    @Test
    fun bundledLunarNewYearCalendarCoversTheOfficialFifteenDayWindow() {
        assertEquals("lunar_new_year", SeasonalThemeCalendar.primaryTheme(LocalDate.of(2027, 2, 6))?.id)
        assertTrue(SeasonalThemeCalendar.activeThemes(LocalDate.of(2027, 2, 20)).any {
            it.id == "lunar_new_year"
        })
        assertFalse(SeasonalThemeCalendar.activeThemes(LocalDate.of(2027, 2, 21)).any {
            it.id == "lunar_new_year"
        })
        assertEquals("lunar_new_year", SeasonalThemeCalendar.primaryTheme(LocalDate.of(2028, 1, 26))?.id)
    }

    @Test
    fun lunarNewYearUsesAnExplicitRegionalCalendarWindow() {
        val start = LocalDate.of(2027, 2, 5)
        val end = LocalDate.of(2027, 2, 8)
        val config = SeasonalThemeCalendarConfig(
            lunarNewYearWindows = mapOf(2027 to SeasonalDateWindow(start, end))
        )
        assertEquals("lunar_new_year", SeasonalThemeCalendar.primaryTheme(LocalDate.of(2027, 2, 6), config)?.id)
        assertFalse(SeasonalThemeCalendar.activeThemes(LocalDate.of(2027, 2, 8), config).any {
            it.id == "lunar_new_year"
        })
    }

    @Test
    fun easterUsesGregorianCalculationAndIncludesGoodFridayThroughSunday() {
        // Easter Sunday 2027 is March 28, so the theme runs March 26–28.
        val friday = LocalDate.of(2027, 3, 26)
        val sunday = LocalDate.of(2027, 3, 28)
        val monday = LocalDate.of(2027, 3, 29)
        assertTrue(SeasonalThemeCalendar.activeThemes(friday).any { it.id == "easter" })
        assertTrue(SeasonalThemeCalendar.activeThemes(sunday).any { it.id == "easter" })
        assertFalse(SeasonalThemeCalendar.activeThemes(monday).any { it.id == "easter" })
    }

    @Test
    fun winterIsOptInAndUsesConfiguredRegionalDates() {
        val date = LocalDate.of(2027, 1, 15)
        assertFalse(SeasonalThemeCalendar.activeThemes(date).any { it.id == "winter" })
        val config = SeasonalThemeCalendarConfig(
            regionCode = "US",
            winterEnabled = true
        )
        assertTrue(SeasonalThemeCalendar.activeThemes(date, config).any { it.id == "winter" })
    }
}
