package com.jarrlyyy.guessthenumber.data.repository

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WorldDefinition(
    val id: String,
    val nameKey: String,
    val fallback: String,
    val x: Float,
    val y: Float,
    val bossId: String,
    val bossKey: String,
    val bossFallback: String,
    val hp: Int,
    val secretId: String,
    val secretAt: Long,
    val unlockBossId: String? = null,
    val unlockCorrectGuesses: Long = 0,
    val unlockUpgradeCount: Int = 0,
    val unlockPrestigeCount: Int = 0,
    val unlockUltraCount: Int = 0,
    val fightCorrectGuesses: Long = 0,
    val fightUpgradeCount: Int = 0,
    val fightPrestigeCount: Int = 0,
    val fightUltraCount: Int = 0,
    val worldRequirementKey: String,
    val bossRequirementKey: String,
    val bossRewardKey: String
)

@Serializable
private data class WorldConfigRoot(
    val schemaVersion: Int = 1,
    val worlds: List<WorldDefinition>
)

/**
 * Loads content-only world and boss definitions. Game rules and rewards remain authoritative
 * in Kotlin; malformed or incompatible assets fall back to the built-in baseline.
 */
class WorldConfigRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadWorlds(): List<WorldDefinition> {
        val loaded = runCatching {
            context.assets.open("game/worlds.json").bufferedReader().use { reader ->
                json.decodeFromString<WorldConfigRoot>(reader.readText())
            }
        }.getOrNull()

        return loaded
            ?.takeIf { it.schemaVersion == 1 && isValid(it.worlds) }
            ?.worlds
            ?: defaultWorlds
    }

    private fun isValid(worlds: List<WorldDefinition>): Boolean {
        if (worlds.isEmpty() || worlds.map { it.id }.distinct().size != worlds.size) return false
        if (worlds.map { it.bossId }.distinct().size != worlds.size) return false
        return worlds.all { world ->
            world.id.isNotBlank() &&
                world.nameKey.isNotBlank() &&
                world.fallback.isNotBlank() &&
                world.bossId.isNotBlank() &&
                world.bossKey.isNotBlank() &&
                world.bossFallback.isNotBlank() &&
                world.secretId.isNotBlank() &&
                world.hp in 1..100_000 &&
                world.secretAt >= 0 &&
                world.x in 0f..1f &&
                world.y in 0f..1f &&
                world.unlockCorrectGuesses >= 0 &&
                world.unlockUpgradeCount >= 0 &&
                world.unlockPrestigeCount >= 0 &&
                world.unlockUltraCount >= 0 &&
                world.fightCorrectGuesses >= 0 &&
                world.fightUpgradeCount >= 0 &&
                world.fightPrestigeCount >= 0 &&
                world.fightUltraCount >= 0 &&
                world.worldRequirementKey.isNotBlank() &&
                world.bossRequirementKey.isNotBlank() &&
                world.bossRewardKey.isNotBlank()
        }
    }

    private companion object {
        val defaultWorlds = listOf(
            WorldDefinition("verdant_grove", "world_verdant_name", "Verdant Grove", 0.16f, 0.24f, "verdant_guardian", "boss_verdant_name", "Verdant Guardian", 3, "whispering_hollow", 50, null, 0, 0, 0, 0, 10, 3, 0, 0, "world_verdant_requirement", "boss_verdant_requirement", "boss_verdant_reward"),
            WorldDefinition("crystal_caverns", "world_crystal_name", "Crystal Caverns", 0.43f, 0.43f, "crystal_golem", "boss_crystal_name", "Crystal Golem", 5, "shard_archive", 150, "verdant_guardian", 25, 5, 0, 0, 50, 10, 0, 0, "world_crystal_requirement", "boss_crystal_requirement", "boss_crystal_reward"),
            WorldDefinition("ember_summit", "world_ember_name", "Ember Summit", 0.67f, 0.24f, "ember_dragon", "boss_ember_name", "Ember Dragon", 7, "ashen_vault", 300, "crystal_golem", 100, 15, 1, 0, 150, 25, 1, 0, "world_ember_requirement", "boss_ember_requirement", "boss_ember_reward"),
            WorldDefinition("nebula_rift", "world_nebula_name", "Nebula Rift", 0.82f, 0.59f, "nebula_titan", "boss_nebula_name", "Nebula Titan", 10, "lost_observatory", 600, "ember_dragon", 250, 40, 3, 1, 500, 75, 5, 1, "world_nebula_requirement", "boss_nebula_requirement", "boss_nebula_reward")
        )
    }
}
