package com.jarrlyyy.guessthenumber.shared.seasonal

import java.time.LocalDate
import java.time.MonthDay

/**
 * Calendar configuration for presentation-only seasonal themes.
 *
 * The host supplies [date] after resolving the player's local calendar/time zone. This class
 * deliberately does not read the device clock, access the network, or mutate player state.
 */
data class SeasonalThemeCalendarConfig(
    val regionCode: String = "PH",
    val backToSchoolStart: MonthDay = MonthDay.of(6, 1),
    val backToSchoolEndExclusive: MonthDay = MonthDay.of(7, 1),
    val winterEnabled: Boolean = false,
    val winterStart: MonthDay = MonthDay.of(12, 1),
    val winterEndExclusive: MonthDay = MonthDay.of(3, 1),
    /** Explicit, validated date windows supplied by the regional calendar data source. */
    val lunarNewYearWindows: Map<Int, SeasonalDateWindow> = LunarNewYearCalendar.windows
) {
    init {
        require(regionCode.matches(Regex("[A-Za-z0-9_-]{2,16}"))) { "Region code is invalid." }
        require(backToSchoolStart != backToSchoolEndExclusive) { "Back-to-school window must have a positive duration." }
        require(winterStart != winterEndExclusive) { "Winter window must have a positive duration." }
    }
}

data class SeasonalDateWindow(
    val start: LocalDate,
    val endExclusive: LocalDate
) {
    init {
        require(endExclusive.isAfter(start)) { "Seasonal date window must have a positive duration." }
    }

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && date.isBefore(endExclusive)
}

/**
 * Official Gregorian dates for Lunar New Year, based on the Hong Kong Observatory's
 * Gregorian-Lunar conversion tables. Each event window covers Lunar New Year's Day
 * through the 15th day (end-exclusive). Extend this table when new official years are published.
 * Source: https://www.hko.gov.hk/en/gts/time/conversion.htm
 */
object LunarNewYearCalendar {
    private val newYearDates = mapOf(
        2026 to LocalDate.of(2026, 2, 17),
        2027 to LocalDate.of(2027, 2, 6),
        2028 to LocalDate.of(2028, 1, 26),
        2029 to LocalDate.of(2029, 2, 13),
        2030 to LocalDate.of(2030, 2, 3),
        2031 to LocalDate.of(2031, 1, 23),
        2032 to LocalDate.of(2032, 2, 11),
        2033 to LocalDate.of(2033, 1, 31),
        2034 to LocalDate.of(2034, 2, 19),
        2035 to LocalDate.of(2035, 2, 8)
    )

    val windows: Map<Int, SeasonalDateWindow> = newYearDates.mapValues { (_, start) ->
        SeasonalDateWindow(start, start.plusDays(15))
    }
}

data class ActiveSeasonalTheme(
    val id: String,
    val start: LocalDate,
    val endExclusive: LocalDate,
    /** Higher priority themes are presented first when event windows overlap. */
    val priority: Int
) {
    fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && date.isBefore(endExclusive)
}

/**
 * Shared Android/Windows calendar resolver for visual themes.
 *
 * This is separate from TraitSeasonPolicy: four-season trait eligibility must not be changed
 * by limited-time presentation events such as Halloween or Christmas.
 */
object SeasonalThemeCalendar {
    private data class RecurringWindow(
        val id: String,
        val start: MonthDay,
        val endExclusive: MonthDay,
        val priority: Int,
        val philippinesOnly: Boolean = false
    )

    private val recurringWindows = listOf(
        RecurringWindow("new_year", MonthDay.of(12, 26), MonthDay.of(1, 8), 110),
        RecurringWindow("christmas", MonthDay.of(12, 1), MonthDay.of(1, 7), 100),
        RecurringWindow("halloween", MonthDay.of(10, 15), MonthDay.of(11, 2), 90),
        RecurringWindow("valentines", MonthDay.of(2, 7), MonthDay.of(2, 16), 70),
        RecurringWindow("philippine_summer", MonthDay.of(3, 1), MonthDay.of(6, 1), 50, true),
        RecurringWindow("philippine_independence_day", MonthDay.of(6, 12), MonthDay.of(6, 13), 80, true)
    )

    /**
     * Returns every active presentation theme, highest priority first.
     * Overlapping events are retained so compatible layers can coexist; [primaryTheme] identifies
     * the theme that should own the main visual treatment.
     */
    fun activeThemes(
        date: LocalDate,
        config: SeasonalThemeCalendarConfig = SeasonalThemeCalendarConfig()
    ): List<ActiveSeasonalTheme> {
        val active = mutableListOf<ActiveSeasonalTheme>()
        val isPhilippines = config.regionCode.equals("PH", ignoreCase = true)

        recurringWindows
            .filterNot { it.philippinesOnly }
            .plus(recurringWindows.filter { it.philippinesOnly && isPhilippines })
            .forEach { rule ->
                recurringWindowForDate(rule, date)?.let(active::add)
            }

        if (isPhilippines) {
            recurringWindowForDate(
                RecurringWindow(
                    id = "back_to_school",
                    start = config.backToSchoolStart,
                    endExclusive = config.backToSchoolEndExclusive,
                    priority = 60,
                    philippinesOnly = true
                ),
                date
            )?.let(active::add)
        }

        config.lunarNewYearWindows[date.year]
            ?.takeIf { it.contains(date) }
            ?.let { active += ActiveSeasonalTheme("lunar_new_year", it.start, it.endExclusive, 85) }

        easterWindow(date.year)
            .takeIf { it.contains(date) }
            ?.let { active += ActiveSeasonalTheme("easter", it.start, it.endExclusive, 95) }

        if (config.winterEnabled) {
            recurringWindowForDate(
                RecurringWindow("winter", config.winterStart, config.winterEndExclusive, 40),
                date
            )?.let(active::add)
        }

        return active
            .distinctBy { it.id }
            .sortedWith(compareByDescending<ActiveSeasonalTheme> { it.priority }.thenBy { it.id })
    }

    fun primaryTheme(
        date: LocalDate,
        config: SeasonalThemeCalendarConfig = SeasonalThemeCalendarConfig()
    ): ActiveSeasonalTheme? = activeThemes(date, config).firstOrNull()

    private fun recurringWindowForDate(
        rule: RecurringWindow,
        date: LocalDate
    ): ActiveSeasonalTheme? {
        // Check the previous year too because New Year, Christmas and winter cross year boundaries.
        for (startYear in (date.year - 1)..date.year) {
            val start = rule.start.atYear(startYear)
            val endYear = if (rule.endExclusive <= rule.start) startYear + 1 else startYear
            val end = rule.endExclusive.atYear(endYear)
            if (!date.isBefore(start) && date.isBefore(end)) {
                return ActiveSeasonalTheme(rule.id, start, end, rule.priority)
            }
        }
        return null
    }

    /** Good Friday through Easter Sunday; end is exclusive at the following Monday. */
    private fun easterWindow(year: Int): SeasonalDateWindow {
        val easterSunday = gregorianEasterSunday(year)
        return SeasonalDateWindow(easterSunday.minusDays(2), easterSunday.plusDays(1))
    }

    /** Meeus/Jones/Butcher Gregorian Easter algorithm. */
    private fun gregorianEasterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }
}
