package com.jarrlyyy.guessthenumber.domain.model

import com.jarrlyyy.guessthenumber.BuildConfig
import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val difficultyId: String = Difficulty.CLASSIC,
    val profileName: String = "",
    val profileIconId: String = SaveProfile.DEFAULT_ICON,
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
    val equippedCosmeticId: String? = null,
    val activeMutators: Set<String> = emptySet(),
    val prestigeShopPurchases: Set<String> = emptySet(),
    val ultraShopPurchases: Set<String> = emptySet(),
    val achievements: Set<String> = emptySet(),
    val completedChallenges: Set<String> = emptySet(),
    val statistics: GameStatistics = GameStatistics(),
    val settings: GameSettings = GameSettings(),
    val lastSaveTimestamp: Long = System.currentTimeMillis(),
    val lastSavedVersion: String = BuildConfig.VERSION_NAME,
    val timeTravelPenaltyUntil: Long = 0L,
    val autoClickerActive: Boolean = false,
    val autoClickerSpeed: Double = 1.0,
    val tutorialCompleted: Boolean = false,
    val buyMultiplier: String = "1",
    val prestigeCount: Long = 0,
    val ultraCount: Long = 0,
    val liveOpsClaims: Map<String, Int> = emptyMap(),
    val permanentEventBoosts: Int = 0,
    val lastRandomEventId: String? = null,
    val dailyQuestDate: String = "",
    val dailyQuestGuesses: Int = 0,
    val dailyQuestCorrectGuesses: Int = 0,
    val dailyQuestBestStreak: Int = 0,
    val dailyQuestUpgradePurchases: Int = 0,
    val claimedDailyQuests: Set<String> = emptySet(),
    val lootCommonChests: Int = 0,
    val lootRareChests: Int = 0,
    val lootEpicChests: Int = 0,
    val lootDropsFound: Long = 0,
    val lootChestsOpened: Long = 0,
    val lastLootDropTier: String? = null,
    val lootEventId: Long = 0,
    val unlockedWorldIds: Set<String> = setOf("verdant_grove"),
    val defeatedBossIds: Set<String> = emptySet(),
    val activeWorldId: String = "verdant_grove",
    val worldBossVictories: Int = 0,
    val bossBattleProgress: Map<String, Int> = emptyMap(),
    val relicInventory: Set<String> = emptySet(),
    val equippedRelicIds: Set<String> = emptySet(),
    val worldMasteryLevels: Map<String, Int> = emptyMap(),
    val discoveredSecretIds: Set<String> = emptySet(),
    val codexEntries: Set<String> = emptySet(),
    val homeBaseLevel: Int = 0,
    val activeBossBattleWorldId: String? = null,
    val bossBattleMistakes: Map<String, Int> = emptyMap(),
    val codexRewardClaimed: Boolean = false,
    val endgameRewardsClaimed: Set<String> = emptySet(),
    val endlessRiftTier: Int = 0,
    val endlessRiftBestTier: Int = 0,
    val endlessRiftActive: Boolean = false
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
    val arcadeBestScores: Map<String, Int> = emptyMap(),
    val randomEventsTriggered: Long = 0
)

val DEFAULT_MORE_SCREEN_ORDER = buildList {
    addAll(listOf(
        "world_map", "achievements", "live_ops", "lua_hub", "mutators", "talent", "relics",
        "home_base", "codex", "endgame", "arcade", "stats", "changelog", "settings", "about"
    ))
    if (BuildConfig.DEBUG) add("dev_settings")
    addAll(listOf("staking", "ultra", "prestige", "save_slots", "music"))
}

@Serializable
data class GameSettings(
    val musicEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notificationRemindersEnabled: Boolean = true,
    val notificationProgressionEnabled: Boolean = true,
    val notificationEventsEnabled: Boolean = true,
    val notificationIntervalHours: Long = 24L,
    val themeMode: String = "System",
    val numberNotation: String = "Standard",
    val locale: String = "en-US",
    val reducedMotion: Boolean = false,
    val volume: Float = 1.0f,
    val reduceFlashes: Boolean = false,
    val saveLogsToStorage: Boolean = false,
    val verboseLogging: Boolean = false,
    val backgroundMusicPath: String? = null,
    val musicLibraryPaths: List<String> = emptyList(),
    val musicQueuePaths: List<String> = emptyList(),
    val musicPlaylistPaths: Map<String, List<String>> = emptyMap(),
    val musicRepeatMode: String = "off",
    val musicShuffle: Boolean = false,
    val themePreset: String = "Neon",
    val customPrimaryColor: String = "#7C4DFF",
    val customSecondaryColor: String = "#00BCD4",
    val customTertiaryColor: String = "#00C853",
    val moreScreenOrder: List<String> = DEFAULT_MORE_SCREEN_ORDER,
    val moreScreenButtonColors: Map<String, String> = emptyMap(),
    val tutorialCompleted: Boolean = false
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
    val nebulaCost: Long,
    val category: String = "general",
    val isCosmetic: Boolean = false
)

@Serializable
data class AchievementDef(
    val id: String,
    val name: String,
    val description: String,
    val rewardNebula: Long,
    val tier: String = "bronze"
)