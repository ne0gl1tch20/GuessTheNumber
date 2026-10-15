package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.model.ShopItemDef
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef

data class DesktopEconomyCatalog(
    val upgrades: List<UpgradeDef>,
    val prestigeUpgrades: List<UpgradeDef>,
    val ultraUpgrades: List<UpgradeDef>,
    val shopItems: List<ShopItemDef>
) {
    companion object {
        fun load(): DesktopEconomyCatalog {
            fun read(path: String): String =
                DesktopEconomyCatalog::class.java.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
                    ?: error("Missing bundled economy catalog: $path")

            fun upgrades(path: String): List<UpgradeDef> =
                DesktopEconomyJson.upgrades(read(path)).also {
                    require(it.isNotEmpty()) { "Economy catalog is empty: $path" }
                }

            val shopItems = DesktopEconomyJson.shopItems(read("/game/shop_items.json"))
            require(shopItems.isNotEmpty()) { "Shop catalog is empty." }
            return DesktopEconomyCatalog(
                upgrades = upgrades("/game/upgrades.json"),
                prestigeUpgrades = upgrades("/game/prestige_upgrades.json"),
                ultraUpgrades = upgrades("/game/ultra_upgrades.json"),
                shopItems = shopItems
            )
        }
    }
}
