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
        val many = (1..181).map { i -> RemoteTraitDefinition("remote_trait_" + i,
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
            RemoteTraitFile("assets/traits/catalogs/exploration/NebulaFinderTrait/config.json", "0".repeat(64), "json"),
            RemoteTraitFile("assets/traits/catalogs/exploration/NebulaFinderTrait/module.json", "0".repeat(64), "json"),
            RemoteTraitFile("assets/traits/catalogs/exploration/NebulaFinderTrait/locales/en.json", "0".repeat(64), "json")
        )
        val trait = RemoteTraitDefinition(
            "remote_nebula_finder", "trait.remote_nebula_finder.title", "trait.remote_nebula_finder.description",
            "COLLECTION", effects = listOf("COSMETIC_BADGE"), catalogId = "exploration",
            folderName = "NebulaFinderTrait", files = files
        )
        val result = validateAndMapTraitPayload(payload(listOf(trait)), 21)
        assertEquals("remote_nebula_finder", result.single().id)
    }

    @Test fun rejectsScriptPackagesWithoutModuleManifestOrEnglishLocale() {
        val base = "assets/traits/catalogs/exploration/RemoteTrait/"
        val script = RemoteTraitFile(base + "logic.lua", "0".repeat(64), "lua")
        val locale = RemoteTraitFile(base + "locales/en.json", "0".repeat(64), "json")
        val missingManifest = RemoteTraitDefinition(
            "remote_script", "trait.remote_script.title", "trait.remote_script.description",
            "COLLECTION", effects = listOf("COSMETIC_BADGE"), catalogId = "exploration",
            folderName = "RemoteTrait", files = listOf(script, locale)
        )
        assertThrows(IllegalArgumentException::class.java) {
            validateAndMapTraitPayload(payload(listOf(missingManifest)), 21)
        }
        val missingEnglish = missingManifest.copy(files = listOf(
            script,
            RemoteTraitFile(base + "module.json", "0".repeat(64), "json"),
            RemoteTraitFile(base + "locales/fil.json", "0".repeat(64), "json")
        ))
        assertThrows(IllegalArgumentException::class.java) {
            validateAndMapTraitPayload(payload(listOf(missingEnglish)), 21)
        }
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

    private fun createScriptPackage(root: java.io.File, localeSource: String, javascriptSource: String): RemoteTraitDefinition {
        val base = "assets/traits/catalogs/extensions/RemoteScriptTrait/"
        val moduleManifest = """{"schemaVersion":1,"traitId":"remote_script","modules":[{"id":"logic","type":"LUA","entrypoint":"logic.lua","capabilities":["read_snapshot"]},{"id":"view","type":"JAVASCRIPT","entrypoint":"presentation.js","capabilities":["render_ui"]},{"id":"rules","type":"JSON","entrypoint":"rules.json","capabilities":["read_rules"]}]}"""
        val contents = mapOf(
            "module.json" to moduleManifest,
            "logic.lua" to """return { eligible = tonumber(gtn.input.bestStreak or "0") >= 5, effects = {"COSMETIC_BADGE"} }""",
            "presentation.js" to javascriptSource,
            "rules.json" to """{"minimumBestStreak":5,"effects":["COSMETIC_BADGE"]}""",
            "locales/en.json" to localeSource
        )
        contents.forEach { (relative, text) ->
            val file = java.io.File(root, base + relative)
            file.parentFile.mkdirs()
            file.writeText(text)
        }
        val files = contents.map { (relative, text) ->
            val kind = relative.substringAfterLast('.')
            RemoteTraitFile(base + relative, java.security.MessageDigest.getInstance("SHA-256")
                .digest(text.toByteArray()).joinToString("") { "%02x".format(it) }, kind)
        }
        return RemoteTraitDefinition(
            "remote_script", "trait.remote_script.title", "trait.remote_script.description",
            "COLLECTION", effects = listOf("COSMETIC_BADGE"), catalogId = "extensions",
            folderName = "RemoteScriptTrait", files = files
        )
    }

    @Test fun validatesDownloadedMixedModulePackageAndLocaleTogether() {
        val root = java.nio.file.Files.createTempDirectory("trait-module-test").toFile()
        try {
            val trait = createScriptPackage(
                root,
                """{"id":"remote_script","title":"Remote Script","description":"A sandboxed extension."}""",
                "function renderTrait(api) { api.setText('status', 'Ready'); }"
            )
            validateTraitPackageContent(root, trait)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun rejectsUnsafeJavascriptAndLocaleIdentityMismatchInDownloadedPackage() {
        val root = java.nio.file.Files.createTempDirectory("trait-module-invalid-test").toFile()
        try {
            val unsafe = createScriptPackage(
                root,
                """{"id":"remote_script","title":"Remote Script","description":"A sandboxed extension."}""",
                "function renderTrait(api) { window.Android.earnReward(); }"
            )
            assertThrows(IllegalArgumentException::class.java) { validateTraitPackageContent(root, unsafe) }

            val wrongLocale = createScriptPackage(
                root,
                """{"id":"other_trait","title":"Wrong","description":"Wrong identity."}""",
                "function renderTrait(api) { api.setText('status', 'Ready'); }"
            )
            assertThrows(IllegalArgumentException::class.java) { validateTraitPackageContent(root, wrongLocale) }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun rejectsTooManyCatalogFolders() {
        val traits = (1..33).map { i ->
            RemoteTraitDefinition("catalog_trait_$i", "trait.catalog_$i.title", "trait.catalog_$i.description",
                "COLLECTION", effects = listOf("COSMETIC_BADGE"), catalogId = "catalog_$i")
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateAndMapTraitPayload(payload(traits), 21)
        }
    }

}
