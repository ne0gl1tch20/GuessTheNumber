package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.model.ShopItemDef
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef

data class DesktopCurrencyShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Long
)

data class DesktopTalentDef(
    val id: String,
    val name: String,
    val description: String,
    val cost: Long,
    val requiredParentId: String? = null
)

data class DesktopMinigameDef(
    val id: String,
    val name: String,
    val description: String,
    val rewardMultiplier: Double
)

/** Reads the same checked-in game JSON used by Android. */
class DesktopEconomyRepository(
    private val readResource: (String) -> String = ::readClasspathResource
) {
    fun upgrades(): List<UpgradeDef> = DesktopEconomyJson.upgrades(readResource("upgrades.json"))
    fun prestigeUpgrades(): List<UpgradeDef> = DesktopEconomyJson.upgrades(readResource("prestige_upgrades.json"))
    fun ultraUpgrades(): List<UpgradeDef> = DesktopEconomyJson.upgrades(readResource("ultra_upgrades.json"))
    fun shopItems(): List<ShopItemDef> = DesktopEconomyJson.shopItems(readResource("shop_items.json"))
    fun prestigeShopItems(): List<DesktopCurrencyShopItem> =
        DesktopEconomyJson.currencyShopItems(readResource("prestige_shop.json"))
    fun ultraShopItems(): List<DesktopCurrencyShopItem> =
        DesktopEconomyJson.currencyShopItems(readResource("ultra_shop.json"))
    fun talents(): List<DesktopTalentDef> = DesktopEconomyJson.talents(readResource("talents.json"))
    fun minigames(): List<DesktopMinigameDef> = DesktopEconomyJson.minigames(readResource("minigames.json"))

    private companion object {
        fun readClasspathResource(name: String): String {
            val resourcePath = "game/$name"
            val stream = DesktopEconomyRepository::class.java.classLoader.getResourceAsStream(resourcePath)
                ?: error("Missing game economy resource: $resourcePath")
            return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }
}
