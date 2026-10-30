package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import com.jarrlyyy.guessthenumber.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopWebPlayActionsTest {
    @Test
    fun dailyRewardUsesSharedStreakScheduleAndNebulaBoost() {
        val plain = DesktopWebPlayActions.claimDailyReward(GameState(), "2026-10-30")
        assertNotNull(plain)
        assertEquals("5", plain!!.nebula.format())
        assertEquals("2026-10-30", plain.lastDailyRewardDate)
        assertEquals(1, plain.dailyRewardStreak)
        assertEquals(5L, plain.statistics.nebulaEarned)

        val boosted = DesktopWebPlayActions.claimDailyReward(
            GameState(shopPurchases = setOf("nebula_boost")), "2026-10-30"
        )
        assertEquals("10", boosted!!.nebula.format())
        assertNull(DesktopWebPlayActions.claimDailyReward(plain, "2026-10-30"))
    }

    @Test
    fun creativeRangeRequiresCreativeModeAndResetsRoundProgress() {
        val before = GameState(
            creativeModeEnabled = true,
            attempts = 9,
            correctGuesses = 4,
            streak = 3,
            currentRangeMin = 1,
            currentRangeMax = 100
        )
        val updated = DesktopWebPlayActions.updateCreativeRange(before, 20, 80)
        assertNotNull(updated)
        assertEquals(20L, updated!!.currentRangeMin)
        assertEquals(80L, updated.currentRangeMax)
        assertEquals(50L, updated.targetNumber)
        assertEquals(0L, updated.attempts)
        assertEquals(0L, updated.correctGuesses)
        assertEquals(0, updated.streak)
        assertNull(DesktopWebPlayActions.updateCreativeRange(GameState(), 20, 80))
        assertNull(DesktopWebPlayActions.updateCreativeRange(before, 0, 80))
        assertNull(DesktopWebPlayActions.updateCreativeRange(before, 20, 1_000_001))
    }

    @Test
    fun creativeValuesAreValidatedAndPersistable() {
        val before = GameState(creativeModeEnabled = true, attempts = 12, correctGuesses = 4)
        val money = DesktopWebPlayActions.updateCreativeValue(before, "money", "1.5K")
        assertNotNull(money)
        assertEquals("1.5K", money!!.money.format())
        val attempts = DesktopWebPlayActions.updateCreativeValue(before, "attempts", "20")
        assertEquals(20L, attempts!!.attempts)
        assertNull(DesktopWebPlayActions.updateCreativeValue(GameState(), "money", "999"))
        assertNull(DesktopWebPlayActions.updateCreativeValue(before, "correctGuesses", "99"))
        assertNull(DesktopWebPlayActions.updateCreativeValue(before, "unknown", "1"))
    }

    @Test
    fun desktopAcceptsAndroidSettingsAndRejectsInvalidValues() {
        val settings = DesktopWebPlayActions.updateSetting(GameSettings(), "themeMode", "AMOLED")
        assertEquals("AMOLED", settings!!.themeMode)
        val locale = DesktopWebPlayActions.updateSetting(GameSettings(), "locale", "fil-PH")
        assertEquals("fil-PH", locale!!.locale)
        val volume = DesktopWebPlayActions.updateSetting(GameSettings(), "volume", 0.35)
        assertEquals(0.35f, volume!!.volume)
        assertNull(DesktopWebPlayActions.updateSetting(GameSettings(), "locale", "not-a-locale"))
        assertNull(DesktopWebPlayActions.updateSetting(GameSettings(), "volume", 1.5))
    }
}
