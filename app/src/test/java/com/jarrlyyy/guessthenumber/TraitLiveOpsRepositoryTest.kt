package com.jarrlyyy.guessthenumber

import com.jarrlyyy.guessthenumber.traits.BuiltInTraits
import com.jarrlyyy.guessthenumber.traits.RemoteTraitDefinition
import com.jarrlyyy.guessthenumber.traits.RemoteTraitFile
import com.jarrlyyy.guessthenumber.traits.TraitLiveOpsPayload
import com.jarrlyyy.guessthenumber.traits.validateAndMapTraitPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TraitLiveOpsRepositoryTest {
    private fun payload(traits: List<RemoteTraitDefinition> = listOf(
        RemoteTraitDefinition("seasonal_echo", "trait.seasonal_echo.title", "trait.seasonal_echo.description",
            "SEASONAL", seasonId = "autumn", effects = listOf("COSMETIC_BADGE"))
    ), minApp: Int = 1, maxApp: Int = 100) = TraitLiveOpsPayload(
        schemaVersion = 2, hostApiVersion = 1, minAppVersionCode = minApp,
        maxAppVersionCode = maxApp, catalogVersion = 1, traits = traits
    )

    @Test fun acceptsCompatibleDeclarativeSeasonalTrait() {
        val result = validateAndMapTraitPayload(payload(), 21)
        assertEquals(1, result.size)
        assertEquals("seasonal_echo", result.single().id)
        assertTrue(result.single().seasonId == "autumn")
    }

    @Test fun rejectsIncompatibleAppVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            validateAndMapTraitPayload(payload(minApp = 22), 21)
        }
    }

    @Test fun rejectsBuiltInIdReplacement() {
        val replacement = RemoteTraitDefinition(BuiltInTraits.definitions.first().id,
            "trait.replacement.title", "trait.replacement.description", "PROGRESSION",
            effects = listOf("XP_MULTIPLIER"))
        assertThrows(IllegalArgumentException::class.java) {
            validateAndMapTraitPayload(payload(listOf(replacement)), 21)
        }
    }

    @Test fun rejectsMissingDependenciesBeforeActivation() {
        val dependent = RemoteTraitDefinition("remote_dependent", "trait.remote_dependent.title",
            "trait.remote_dependent.description", "PROGRESSION", dependencies = listOf("missing_remote_trait"),
            effects = listOf("XP_MULTIPLIER"))
        assertThrows(IllegalArgumentException::class.java) {
            validateAndMapTraitPayload(payload(listOf(dependent)), 21)
        }
    }

    @Test fun rejectsUnknownSchemaAndOversizedCatalog() {
        assertThrows(IllegalArgumentException::class.java) {
            validateAndMapTraitPayload(payload().copy(schemaVersion = 99), 21)
        }
        val many = (1..101).map { i -> RemoteTraitDefinition("remote_trait_" + i,
            "trait.remote_trait_" + i + ".title", "trait.remote_trait_" + i + ".description",
            "PROGRESSION", effects = listOf("XP_MULTIPLIER")) }
        assertThrows(IllegalArgumentException::class.java) {
            validateAndMapTraitPayload(payload(many), 21)
        }
    }
    @Test fun acceptsMixedFilesInsideNamedCatalogAndTraitFolder() {
        val files = listOf(
            RemoteTraitFile("assets/traits/catalogs/exploration/NebulaFinderTrait/logic.lua", "0".repeat(64), "lua"),
            RemoteTraitFile("assets/traits/catalogs/exploration/NebulaFinderTrait/presentation.js", "0".repeat(64), "js"),
            RemoteTraitFile("assets/traits/catalogs/exploration/NebulaFinderTrait/theme.css", "0".repeat(64), "css"),
            RemoteTraitFile("assets/traits/catalogs/exploration/NebulaFinderTrait/icon.svg", "0".repeat(64), "svg"),
            RemoteTraitFile("assets/traits/catalogs/exploration/NebulaFinderTrait/config.json", "0".repeat(64), "json")
        )
        val trait = RemoteTraitDefinition(
            "nebula_finder", "trait.nebula_finder.title", "trait.nebula_finder.description",
            "COLLECTION", effects = listOf("COSMETIC_BADGE"), catalogId = "exploration",
            folderName = "NebulaFinderTrait", files = files
        )
        val result = validateAndMapTraitPayload(payload(listOf(trait)), 21)
        assertEquals("nebula_finder", result.single().id)
    }

    @Test fun rejectsTraitFilesOutsideTheirCatalogFolder() {
        val trait = RemoteTraitDefinition(
            "unsafe_path", "trait.unsafe_path.title", "trait.unsafe_path.description",
            "COLLECTION", effects = listOf("COSMETIC_BADGE"), catalogId = "exploration",
            folderName = "NebulaFinderTrait",
            files = listOf(RemoteTraitFile(
                "assets/traits/catalogs/core/NebulaFinderTrait/logic.lua", "0".repeat(64), "lua"
            ))
        )
        assertThrows(IllegalArgumentException::class.java) {
            validateAndMapTraitPayload(payload(listOf(trait)), 21)
        }
    }

    @Test fun rejectsTooManyCatalogFolders() {
        val traits = (1..17).map { i ->
            RemoteTraitDefinition("catalog_trait_$i", "trait.catalog_$i.title", "trait.catalog_$i.description",
                "COLLECTION", effects = listOf("COSMETIC_BADGE"), catalogId = "catalog_$i")
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateAndMapTraitPayload(payload(traits), 21)
        }
    }

}
