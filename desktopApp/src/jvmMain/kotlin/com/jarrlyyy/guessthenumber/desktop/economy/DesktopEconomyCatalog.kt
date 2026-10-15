package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.model.ShopItemDef
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class UpgradeCatalogFile(
    val schemaVersion: Int = 1,
    val upgrades: List<UpgradeDef> = emptyList()
)

@Serializable
private data class ShopCatalogFile(
    val schemaVersion: Int = 1,
    val shopItems: List<ShopItemDef> = emptyList()
)

data class DesktopEconomyCatalog(
    val upgrades: List<UpgradeDef>,
    val prestigeUpgrades: List<UpgradeDef>,
    val ultraUpgrades: List<UpgradeDef>,
    val shopItems: List<ShopItemDef>
) {
    companion object {
        fun load(): DesktopEconomyCatalog {
            val json = Json { ignoreUnknownKeys = true }
            fun read(path: String): String =
                DesktopEconomyCatalog::class.java.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
                    ?: error("Missing bundled economy catalog: $path")

            fun upgrades(path: String): List<UpgradeDef> =
                json.decodeFromString<UpgradeCatalogFile>(read(path)).upgrades.also {
                    require(it.isNotEmpty()) { "Economy catalog is empty: $path" }
                }

            val shop = json.decodeFromString<ShopCatalogFile>(read("/game/shop_items.json"))
            require(shop.shopItems.isNotEmpty()) { "Shop catalog is empty." }
            return DesktopEconomyCatalog(
                upgrades = upgrades("/game/upgrades.json"),
                prestigeUpgrades = upgrades("/game/prestige_upgrades.json"),
                ultraUpgrades = upgrades("/game/ultra_upgrades.json"),
                shopItems = shop.shopItems
            )
        }
    }
}
