package com.jarrlyyy.guessthenumber.data.repository

import android.content.Context
import com.jarrlyyy.guessthenumber.data.logger.GameLogger
import com.jarrlyyy.guessthenumber.data.logger.LoggerCategory
import com.jarrlyyy.guessthenumber.data.logger.LogLevel
import com.jarrlyyy.guessthenumber.domain.model.AchievementDef
import com.jarrlyyy.guessthenumber.domain.model.ShopItemDef
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GameConfig(
    val startingMoney: String = "0",
    val startingRangeMin: Long = 1,
    val startingRangeMax: Long = 100,
    val criticalChance: Double = 0.05,
    val maxUltraCap: Int = 2000,
    val autoClickerIncomePerSec: String = "500"
)

@Serializable
data class UpgradeConfigRoot(val upgrades: List<UpgradeDef>)
@Serializable
data class ShopConfigRoot(val shopItems: List<ShopItemDef>)
@Serializable
data class PrestigeShopItemDef(val id: String, val name: String, val description: String, val cost: Long)
@Serializable
data class PrestigeShopConfigRoot(val shopItems: List<PrestigeShopItemDef>)
@Serializable
data class UltraShopItemDef(val id: String, val name: String, val description: String, val cost: Long)
@Serializable
data class UltraShopConfigRoot(val shopItems: List<UltraShopItemDef>)
@Serializable
data class AchievementConfigRoot(val achievements: List<AchievementDef>)
@Serializable
data class MinigameDef(val id: String, val name: String, val description: String, val rewardMultiplier: Double)
@Serializable
data class MinigameConfigRoot(val minigames: List<MinigameDef>)
@Serializable
data class ChallengeDef(val id: String, val name: String, val description: String, val rewardNebula: Long)
@Serializable
data class ChallengeConfigRoot(val challenges: List<ChallengeDef>)

class JsonConfigRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadGameConfig(): GameConfig {
        return try {
            val inputStream = context.assets.open("game/game_config.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<GameConfig>(jsonString)
        } catch (e: Exception) {
            GameConfig()
        }
    }

    fun loadUpgrades(): List<UpgradeDef> {
        return try {
            val inputStream = context.assets.open("game/upgrades.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<UpgradeConfigRoot>(jsonString).upgrades
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadShopItems(): List<ShopItemDef> {
        return try {
            val inputStream = context.assets.open("game/shop_items.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<ShopConfigRoot>(jsonString).shopItems
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadAchievements(): List<AchievementDef> {
        return try {
            val inputStream = context.assets.open("game/achievements.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<AchievementConfigRoot>(jsonString).achievements
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadMinigames(): List<MinigameDef> {
        return try {
            val inputStream = context.assets.open("game/minigames.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<MinigameConfigRoot>(jsonString).minigames
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadChallenges(): List<ChallengeDef> {
        return try {
            val inputStream = context.assets.open("game/challenges.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<ChallengeConfigRoot>(jsonString).challenges
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadPrestigeUpgrades(): List<UpgradeDef> {
        return try {
            val inputStream = context.assets.open("game/prestige_upgrades.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<UpgradeConfigRoot>(jsonString).upgrades
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadPrestigeShopItems(): List<PrestigeShopItemDef> {
        return try {
            val inputStream = context.assets.open("game/prestige_shop.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<PrestigeShopConfigRoot>(jsonString).shopItems
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadUltraUpgrades(): List<UpgradeDef> {
        return try {
            val inputStream = context.assets.open("game/ultra_upgrades.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<UpgradeConfigRoot>(jsonString).upgrades
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadUltraShopItems(): List<UltraShopItemDef> {
        return try {
            val inputStream = context.assets.open("game/ultra_shop.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<UltraShopConfigRoot>(jsonString).shopItems
        } catch (e: Exception) {
            emptyList()
        }
    }
}
