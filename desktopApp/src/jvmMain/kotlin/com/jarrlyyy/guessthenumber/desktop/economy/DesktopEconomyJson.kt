package com.jarrlyyy.guessthenumber.desktop.economy

import com.jarrlyyy.guessthenumber.domain.model.ShopItemDef
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Decodes the canonical JSON structurally, without coupling desktop loading to generated serializers. */
internal object DesktopEconomyJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun root(source: String): JsonObject = json.parseToJsonElement(source).jsonObject

    fun upgrades(source: String): List<UpgradeDef> =
        root(source).requiredArray("upgrades").map { value ->
            val item = value.jsonObject
            UpgradeDef(
                id = item.requiredString("id"),
                name = item.requiredString("name"),
                description = item.requiredString("description"),
                maxLevel = item.requiredInt("maxLevel"),
                baseCost = item.requiredString("baseCost"),
                costMultiplier = item.requiredDouble("costMultiplier"),
                effectPerLevel = item.requiredDouble("effectPerLevel")
            )
        }

    fun shopItems(source: String): List<ShopItemDef> =
        root(source).requiredArray("shopItems").map { value ->
            val item = value.jsonObject
            ShopItemDef(
                id = item.requiredString("id"),
                name = item.requiredString("name"),
                description = item.requiredString("description"),
                nebulaCost = item.requiredLong("nebulaCost"),
                category = item["category"]?.jsonPrimitive?.content ?: "general",
                isCosmetic = item["isCosmetic"]?.jsonPrimitive?.boolean ?: false
            )
        }

    private fun JsonObject.requiredArray(key: String): JsonArray =
        (this[key] ?: error("Missing required economy field: $key")).jsonArray
    private fun JsonObject.requiredString(key: String): String =
        (this[key] ?: error("Missing required economy field: $key")).jsonPrimitive.content
    private fun JsonObject.requiredInt(key: String): Int =
        (this[key] ?: error("Missing required economy field: $key")).jsonPrimitive.int
    private fun JsonObject.requiredLong(key: String): Long =
        (this[key] ?: error("Missing required economy field: $key")).jsonPrimitive.long
    private fun JsonObject.requiredDouble(key: String): Double =
        (this[key] ?: error("Missing required economy field: $key")).jsonPrimitive.double
}
