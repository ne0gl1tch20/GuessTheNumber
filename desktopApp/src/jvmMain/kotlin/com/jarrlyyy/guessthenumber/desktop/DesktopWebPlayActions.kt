package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.desktop.economy.DesktopEconomyRepository
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.DailyRewardPolicy
import com.jarrlyyy.guessthenumber.domain.model.GameSettings
import com.jarrlyyy.guessthenumber.domain.model.GameState
import java.time.LocalDate

/**
 * Desktop implementation of the Android Web UI action contract.
 * Every returned state is still validated and persisted by the desktop host.
 */
internal object DesktopWebPlayActions {
    private const val MAX_CREATIVE_RANGE = 1_000_000L

    fun claimDailyReward(state: GameState, today: String = LocalDate.now().toString()): GameState? {
        if (state.lastDailyRewardDate == today) return null
        val streak = DailyRewardPolicy.nextStreak(state.lastDailyRewardDate, state.dailyRewardStreak, today)
        val baseReward = DailyRewardPolicy.rewardForStreak(streak)
        val multiplier = if ("nebula_boost" in state.shopPurchases) 2L else 1L
        val reward = baseReward * multiplier
        return state.copy(
            nebula = state.nebula + BigNumber(reward),
            lastDailyRewardDate = today,
            dailyRewardStreak = streak,
            statistics = state.statistics.copy(
                nebulaEarned = state.statistics.nebulaEarned + reward
            )
        )
    }

    fun updateCreativeRange(state: GameState, min: Long, max: Long): GameState? {
        if (!state.creativeModeEnabled || min < 1L || max < min || max > MAX_CREATIVE_RANGE) return null
        return state.copy(
            currentRangeMin = min,
            currentRangeMax = max,
            targetNumber = min + ((max - min) / 2L),
            attempts = 0L,
            correctGuesses = 0L,
            streak = 0
        )
    }

    fun updateCreativeValue(
        state: GameState,
        key: String,
        value: String,
        repository: DesktopEconomyRepository = DesktopEconomyRepository()
    ): GameState? {
        if (!state.creativeModeEnabled || value.length !in 1..100) return null
        return when (key) {
            "money", "prestige", "ultra", "nebula" -> {
                if (!Regex("[0-9]+(?:\\.[0-9]+)?(?:[A-Za-z]{1,4})?").matches(value)) return null
                val amount = runCatching { BigNumber(value) }.getOrNull() ?: return null
                when (key) {
                    "money" -> state.copy(money = amount)
                    "prestige" -> state.copy(prestige = amount)
                    "ultra" -> state.copy(ultra = amount)
                    else -> state.copy(nebula = amount)
                }
            }
            "attempts" -> value.toLongOrNull()
                ?.takeIf { it in state.correctGuesses..1_000_000_000_000L }
                ?.let { state.copy(attempts = it) }
            "correctGuesses" -> value.toLongOrNull()
                ?.takeIf { it in 0L..state.attempts }
                ?.let { state.copy(correctGuesses = it) }
            "streak" -> value.toIntOrNull()
                ?.takeIf { it in 0..1_000_000 }
                ?.let { state.copy(streak = it, bestStreak = maxOf(state.bestStreak, it)) }
            "bestStreak" -> value.toIntOrNull()
                ?.takeIf { it in state.streak..1_000_000 }
                ?.let { state.copy(bestStreak = it) }
            else -> {
                val split = key.split(':', limit = 2)
                if (split.size != 2 || split[1].isBlank()) return null
                val id = split[1]
                val level = value.toIntOrNull() ?: return null
                when (split[0]) {
                    "upgradeLevel" -> {
                        val definition = repository.upgrades().firstOrNull { it.id == id } ?: return null
                        if (level !in 0..definition.maxLevel) return null
                        state.copy(upgradeLevels = state.upgradeLevels + (id to level))
                    }
                    "prestigeUpgradeLevel" -> {
                        val definition = repository.prestigeUpgrades().firstOrNull { it.id == id } ?: return null
                        if (level !in 0..definition.maxLevel) return null
                        state.copy(prestigeUpgradeLevels = state.prestigeUpgradeLevels + (id to level))
                    }
                    "ultraUpgradeLevel" -> {
                        val definition = repository.ultraUpgrades().firstOrNull { it.id == id } ?: return null
                        if (level !in 0..definition.maxLevel) return null
                        state.copy(ultraUpgradeLevels = state.ultraUpgradeLevels + (id to level))
                    }
                    else -> null
                }
            }
        }
    }

    fun updateSetting(settings: GameSettings, key: String, rawValue: Any?): GameSettings? {
        fun text(): String? = when (rawValue) {
            is String -> rawValue
            is Boolean, is Number -> rawValue.toString()
            else -> null
        }
        fun boolean(): Boolean? = when (rawValue) {
            is Boolean -> rawValue
            is String -> when (rawValue.lowercase()) { "true" -> true; "false" -> false; else -> null }
            else -> null
        }
        fun decimal(): Float? = text()?.toFloatOrNull()?.takeIf { it.isFinite() && it in 0f..1f }
        val value = text() ?: return null
        return when (key) {
            "musicEnabled" -> boolean()?.let { settings.copy(musicEnabled = it) }
            "soundEnabled" -> boolean()?.let { settings.copy(soundEnabled = it) }
            "vibrationEnabled" -> boolean()?.let { settings.copy(vibrationEnabled = it) }
            "notificationsEnabled" -> boolean()?.let { settings.copy(notificationsEnabled = it) }
            "notificationRemindersEnabled" -> boolean()?.let { settings.copy(notificationRemindersEnabled = it) }
            "notificationProgressionEnabled" -> boolean()?.let { settings.copy(notificationProgressionEnabled = it) }
            "notificationEventsEnabled" -> boolean()?.let { settings.copy(notificationEventsEnabled = it) }
            "notificationIntervalHours" -> value.toLongOrNull()?.takeIf { it in 1L..168L }
                ?.let { settings.copy(notificationIntervalHours = it) }
            "themeMode" -> value.takeIf { it in setOf("System", "Light", "Dark", "AMOLED") }
                ?.let { settings.copy(themeMode = it) }
            "numberNotation" -> value.takeIf { it in setOf("Standard", "Scientific", "Engineering") }
                ?.let { settings.copy(numberNotation = it) }
            "locale" -> value.takeIf(::isDesktopWebLocaleSupported)?.let { settings.copy(locale = it) }
            "reducedMotion" -> boolean()?.let { settings.copy(reducedMotion = it) }
            "reduceFlashes" -> boolean()?.let { settings.copy(reduceFlashes = it) }
            "volume" -> decimal()?.let { settings.copy(volume = it) }
            "sfxVolume" -> decimal()?.let { settings.copy(sfxVolume = it) }
            "themePreset" -> when (value) {
                "Neon" -> settings.copy(themePreset = value, customPrimaryColor = "#7C4DFF", customSecondaryColor = "#00BCD4", customTertiaryColor = "#00C853")
                "Sunset" -> settings.copy(themePreset = value, customPrimaryColor = "#FF4081", customSecondaryColor = "#FF9800", customTertiaryColor = "#FFC107")
                "Ocean" -> settings.copy(themePreset = value, customPrimaryColor = "#2196F3", customSecondaryColor = "#00BCD4", customTertiaryColor = "#3F51B5")
                "Forest" -> settings.copy(themePreset = value, customPrimaryColor = "#4CAF50", customSecondaryColor = "#009688", customTertiaryColor = "#8BC34A")
                "Custom" -> settings.copy(themePreset = "Custom")
                else -> null
            }
            "customPrimaryColor" -> value.takeIf(::isHexColor)?.let { settings.copy(themePreset = "Custom", customPrimaryColor = it) }
            "customSecondaryColor" -> value.takeIf(::isHexColor)?.let { settings.copy(themePreset = "Custom", customSecondaryColor = it) }
            "customTertiaryColor" -> value.takeIf(::isHexColor)?.let { settings.copy(themePreset = "Custom", customTertiaryColor = it) }
            "webMenuStudioLayout" -> value.takeIf { it.length <= 100_000 }?.let { settings.copy(webMenuStudioLayout = it) }
            else -> null
        }
    }

    private fun isHexColor(value: String): Boolean = Regex("#[0-9A-Fa-f]{6}").matches(value)
}
