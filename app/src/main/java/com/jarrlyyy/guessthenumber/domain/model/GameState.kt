package com.jarrlyyy.guessthenumber.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val money: BigNumber = BigNumber.ZERO,
    val prestige: BigNumber = BigNumber.ZERO,
    val ultra: BigNumber = BigNumber.ZERO,
    val nebula: BigNumber = BigNumber.ZERO,
    val currentRangeMin: Long = 1,
    val currentRangeMax: Long = 100,
    val targetNumber: Long = 50,
    val attempts: Long = 0,
    val correctGuesses: Long = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val upgradeLevels: Map<String, Int> = emptyMap(),
    val prestigeUpgradeLevels: Map<String, Int> = emptyMap(),
    val ultraUpgradeLevels: Map<String, Int> = emptyMap(),
    val shopPurchases: Set<String> = emptySet(),
    val prestigeShopPurchases: Set<String> = emptySet(),
    val ultraShopPurchases: Set<String> = emptySet(),
    val achievements: Set<String> = emptySet(),
    val completedChallenges: Set<String> = emptySet(),
    val statistics: GameStatistics = GameStatistics(),
    val settings: GameSettings = GameSettings(),
    val lastSaveTimestamp: Long = System.currentTimeMillis(),
    val autoClickerActive: Boolean = false,
    val autoClickerSpeed: Double = 1.0, // clicks per second
    val tutorialCompleted: Boolean = false,
    val buyMultiplier: String = "1", // "1", "10", "100", "MAX"
    val prestigeCount: Long = 0,
    val ultraCount: Long = 0
)

@Serializable
data class GameStatistics(
    val totalGuesses: Long = 0,
    val correctGuesses: Long = 0,
    val failedGuesses: Long = 0,
    val moneyEarned: BigNumber = BigNumber.ZERO,
    val moneySpent: BigNumber = BigNumber.ZERO,
    val prestigesCount: Long = 0,
    val ultrasCount: Long = 0,
    val nebulaEarned: Long = 0,
    val playtimeSeconds: Long = 0,
    val arcadePlayed: Long = 0,
    val arcadeBestScores: Map<String, Int> = emptyMap()
)

@Serializable
data class GameSettings(
    val musicEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notificationIntervalHours: Long = 24L,
    val themeMode: String = "System", // Light, Dark, System
    val numberNotation: String = "Standard", // Standard, Scientific, Engineering
    val reducedMotion: Boolean = false,
    val volume: Float = 1.0f,
    val reduceFlashes: Boolean = false,
    val saveLogsToStorage: Boolean = false,
    val backgroundMusicPath: String? = null
)

@Serializable
data class UpgradeDef(
    val id: String,
    val name: String,
    val description: String,
    val maxLevel: Int,
    val baseCost: String,
    val costMultiplier: Double,
    val effectPerLevel: Double
)

@Serializable
data class ShopItemDef(
    val id: String,
    val name: String,
    val description: String,
    val nebulaCost: Long
)

@Serializable
data class AchievementDef(
    val id: String,
    val name: String,
    val description: String,
    val rewardNebula: Long
)
