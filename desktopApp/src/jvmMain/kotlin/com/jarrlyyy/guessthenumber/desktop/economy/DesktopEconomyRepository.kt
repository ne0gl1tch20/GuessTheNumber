package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.model.ShopItemDef
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class UpgradeRoot(val schemaVersion: Int = 1, val upgrades: List<UpgradeDef> = emptyList())

@Serializable
private data class ShopRoot(val schemaVersion: Int = 1, val shopItems: List<ShopItemDef> = emptyList())

@Serializable
data class DesktopCurrencyShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Long
)

@Serializable
private data class CurrencyShopRoot(val schemaVersion: Int = 1, val shopItems: List<DesktopCurrencyShopItem> = emptyList())

@Serializable
data class DesktopTalentDef(val id: String, val name: String, val description: String, val cost: Long, val requiredParentId: String? = null)

@Serializable
private data class TalentRoot(val schemaVersion: Int = 1, val talents: List<DesktopTalentDef> = emptyList())

/** Reads the same checked-in game JSON used by Android. No copied desktop economy values. */
class DesktopEconomyRepository(
    private val readResource: (String) -> String = ::readClasspathResource
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun upgrades(): List<UpgradeDef> = json.decodeFromString<UpgradeRoot>(readResource("upgrades.json")).upgrades
    fun prestigeUpgrades(): List<UpgradeDef> = json.decodeFromString<UpgradeRoot>(readResource("prestige_upgrades.json")).upgrades
    fun ultraUpgrades(): List<UpgradeDef> = json.decodeFromString<UpgradeRoot>(readResource("ultra_upgrades.json")).upgrades
    fun shopItems(): List<ShopItemDef> = json.decodeFromString<ShopRoot>(readResource("shop_items.json")).shopItems
    fun prestigeShopItems(): List<DesktopCurrencyShopItem> =
        json.decodeFromString<CurrencyShopRoot>(readResource("prestige_shop.json")).shopItems
    fun ultraShopItems(): List<DesktopCurrencyShopItem> =
        json.decodeFromString<CurrencyShopRoot>(readResource("ultra_shop.json")).shopItems
    fun talents(): List<DesktopTalentDef> = json.decodeFromString<TalentRoot>(readResource("talents.json")).talents

    private companion object {
        fun readClasspathResource(name: String): String {
            val stream = DesktopEconomyRepository::class.java.classLoader.getResourceAsStream(name)
                ?: error("Missing game economy resource: $name")
            return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }
}
