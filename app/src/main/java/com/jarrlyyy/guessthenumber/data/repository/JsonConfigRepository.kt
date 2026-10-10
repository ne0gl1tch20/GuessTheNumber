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
    val autoClickerIncomePerSec: String = "500",
    val schemaVersion: Int = 1
)

@Serializable
data class UpgradeConfigRoot(val upgrades: List<UpgradeDef>, val schemaVersion: Int = 1)
@Serializable
data class ShopConfigRoot(val shopItems: List<ShopItemDef>, val schemaVersion: Int = 1)
@Serializable
data class PrestigeShopItemDef(val id: String, val name: String, val description: String, val cost: Long)
@Serializable
data class PrestigeShopConfigRoot(val shopItems: List<PrestigeShopItemDef>, val schemaVersion: Int = 1)
@Serializable
data class UltraShopItemDef(val id: String, val name: String, val description: String, val cost: Long)
@Serializable
data class UltraShopConfigRoot(val shopItems: List<UltraShopItemDef>, val schemaVersion: Int = 1)
@Serializable
data class AchievementConfigRoot(val achievements: List<AchievementDef>, val schemaVersion: Int = 1)
@Serializable
data class MinigameDef(val id: String, val name: String, val description: String, val rewardMultiplier: Double)
@Serializable
data class MinigameConfigRoot(val minigames: List<MinigameDef>, val schemaVersion: Int = 1)
@Serializable
data class ChallengeDef(val id: String, val name: String, val description: String, val rewardNebula: Long)
@Serializable
data class ChallengeConfigRoot(val challenges: List<ChallengeDef>, val schemaVersion: Int = 1)
@Serializable
data class TalentDef(val id: String, val name: String, val description: String, val cost: Long, val requiredParentId: String? = null)
@Serializable
data class TalentConfigRoot(val talents: List<TalentDef>, val schemaVersion: Int = 1)
@Serializable
data class FeedbackMessagesRoot(
    val tooLowMessages: List<String>,
    val tooHighMessages: List<String>,
    val guessTipsAndClues: List<String> = emptyList(),
    val schemaVersion: Int = 1
)

class JsonConfigRepository(private val context: Context, private val localeTag: String = "en-US") {
    private val json = Json { ignoreUnknownKeys = true }
    val localeManager = LocaleManager(context)

    init {
        kotlinx.coroutines.runBlocking {
            localeManager.loadLocaleForTag(localeTag)
        }
    }

    fun loadGameConfig(): GameConfig {
        return try {
            val inputStream = context.assets.open("game/game_config.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<GameConfig>(jsonString).also { config ->
                ContentValidation.requireSupportedSchema(config.schemaVersion)
                require(config.startingRangeMin >= 0 && config.startingRangeMax > config.startingRangeMin)
                require(config.criticalChance in 0.0..1.0 && config.maxUltraCap > 0)
                require(config.startingMoney.toBigDecimalOrNull()?.signum() != null && config.startingMoney.toBigDecimalOrNull()!!.signum() >= 0)
                require(config.autoClickerIncomePerSec.toBigDecimalOrNull()?.signum() != null && config.autoClickerIncomePerSec.toBigDecimalOrNull()!!.signum() >= 0)
            }
        } catch (e: Exception) {
            GameConfig()
        }
    }

    fun loadUpgrades(): List<UpgradeDef> {
        return try {
            val inputStream = context.assets.open("game/upgrades.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val config = json.decodeFromString<UpgradeConfigRoot>(jsonString)
            ContentValidation.requireSupportedSchema(config.schemaVersion)
            val raw = config.upgrades
            ContentValidation.validateUpgrades(raw)
            raw.map { up ->
                up.copy(
                    name = localeManager.getString("upgrade_${up.id}_name", up.name),
                    description = localeManager.getString("upgrade_${up.id}_desc", up.description)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadShopItems(): List<ShopItemDef> {
        return try {
            val inputStream = context.assets.open("game/shop_items.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val config = json.decodeFromString<ShopConfigRoot>(jsonString)
            ContentValidation.requireSupportedSchema(config.schemaVersion)
            val raw = config.shopItems
            ContentValidation.validateShopItems(raw)
            raw.map { item ->
                item.copy(
                    name = localeManager.getString("shop_item_${item.id}_name", item.name),
                    description = localeManager.getString("shop_item_${item.id}_desc", item.description)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadAchievements(): List<AchievementDef> {
        return try {
            val inputStream = context.assets.open("game/achievements.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val config = json.decodeFromString<AchievementConfigRoot>(jsonString)
            ContentValidation.requireSupportedSchema(config.schemaVersion)
            val raw = config.achievements
            ContentValidation.validateAchievements(raw)
            raw.map { ach ->
                ach.copy(
                    name = localeManager.getString("achievement_${ach.id}_name", ach.name),
                    description = localeManager.getString("achievement_${ach.id}_desc", ach.description)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadMinigames(): List<MinigameDef> {
        return try {
            val inputStream = context.assets.open("game/minigames.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val config = json.decodeFromString<MinigameConfigRoot>(jsonString)
            ContentValidation.requireSupportedSchema(config.schemaVersion)
            val raw = config.minigames
            ContentValidation.validateMinigames(raw)
            raw.map { mg ->
                mg.copy(
                    name = localeManager.getString("minigame_${mg.id}_name", mg.name),
                    description = localeManager.getString("minigame_${mg.id}_desc", mg.description)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadChallenges(): List<ChallengeDef> {
        return try {
            val inputStream = context.assets.open("game/challenges.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val config = json.decodeFromString<ChallengeConfigRoot>(jsonString)
            ContentValidation.requireSupportedSchema(config.schemaVersion)
            val raw = config.challenges
            ContentValidation.validateChallenges(raw)
            raw.map { ch ->
                ch.copy(
                    name = localeManager.getString("challenge_${ch.id}_name", ch.name),
                    description = localeManager.getString("challenge_${ch.id}_desc", ch.description)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadPrestigeUpgrades(): List<UpgradeDef> {
        return try {
            val inputStream = context.assets.open("game/prestige_upgrades.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val config = json.decodeFromString<UpgradeConfigRoot>(jsonString)
            ContentValidation.requireSupportedSchema(config.schemaVersion)
            val raw = config.upgrades
            ContentValidation.validateUpgrades(raw)
            raw.map { up ->
                up.copy(
                    name = localeManager.getString("prestige_${up.id}_name", up.name),
                    description = localeManager.getString("prestige_${up.id}_desc", up.description)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadPrestigeShopItems(): List<PrestigeShopItemDef> {
        return try {
            val inputStream = context.assets.open("game/prestige_shop.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val config = json.decodeFromString<PrestigeShopConfigRoot>(jsonString)
            ContentValidation.requireSupportedSchema(config.schemaVersion)
            val raw = config.shopItems
            ContentValidation.requireUniqueIds(raw.map { it.id })
            require(raw.all { it.name.isNotBlank() && it.description.isNotBlank() && it.cost >= 0 }) { "Invalid shop content" }
            raw.map { item ->
                item.copy(
                    name = localeManager.getString("prestige_shop_${item.id}_name", item.name),
                    description = localeManager.getString("prestige_shop_${item.id}_desc", item.description)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadUltraUpgrades(): List<UpgradeDef> {
        return try {
            val inputStream = context.assets.open("game/ultra_upgrades.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val config = json.decodeFromString<UpgradeConfigRoot>(jsonString)
            ContentValidation.requireSupportedSchema(config.schemaVersion)
            val raw = config.upgrades
            ContentValidation.validateUpgrades(raw)
            raw.map { up ->
                up.copy(
                    name = localeManager.getString("ultra_${up.id}_name", up.name),
                    description = localeManager.getString("ultra_${up.id}_desc", up.description)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadUltraShopItems(): List<UltraShopItemDef> {
        return try {
            val inputStream = context.assets.open("game/ultra_shop.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val config = json.decodeFromString<UltraShopConfigRoot>(jsonString)
            ContentValidation.requireSupportedSchema(config.schemaVersion)
            val raw = config.shopItems
            ContentValidation.requireUniqueIds(raw.map { it.id })
            require(raw.all { it.name.isNotBlank() && it.description.isNotBlank() && it.cost >= 0 }) { "Invalid shop content" }
            raw.map { item ->
                item.copy(
                    name = localeManager.getString("ultra_shop_${item.id}_name", item.name),
                    description = localeManager.getString("ultra_shop_${item.id}_desc", item.description)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }



    fun loadTalents(): List<TalentDef> {
        return try {
            val inputStream = context.assets.open("game/talents.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val config = json.decodeFromString<TalentConfigRoot>(jsonString)
            ContentValidation.requireSupportedSchema(config.schemaVersion)
            val raw = config.talents
            ContentValidation.validateTalents(raw)
            raw.map { t ->
                t.copy(
                    name = localeManager.getString("talent_${t.id}_name", t.name),
                    description = localeManager.getString("talent_${t.id}_desc", t.description)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadFeedbackMessages(): FeedbackMessagesRoot {
        val root = try {
            val inputStream = context.assets.open("game/guess_feedback_messages.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<FeedbackMessagesRoot>(jsonString).also {
                ContentValidation.requireSupportedSchema(it.schemaVersion)
                require(it.tooLowMessages.isNotEmpty() && it.tooHighMessages.isNotEmpty())
                require((it.tooLowMessages + it.tooHighMessages + it.guessTipsAndClues).all(String::isNotBlank))
            }
        } catch (e: Exception) {
            FeedbackMessagesRoot(
                listOf("Too low! Try higher."),
                listOf("Too high! Try lower.")
            )
        }

        return FeedbackMessagesRoot(
            tooLowMessages = root.tooLowMessages.mapIndexed { index, defaultMsg ->
                localeManager.getString("feedback_toolow_$index", defaultMsg)
            },
            tooHighMessages = root.tooHighMessages.mapIndexed { index, defaultMsg ->
                localeManager.getString("feedback_toohigh_$index", defaultMsg)
            },
            guessTipsAndClues = root.guessTipsAndClues.mapIndexed { index, defaultMsg ->
                localeManager.getString("feedback_tip_$index", defaultMsg)
            }
        )
    }
}
