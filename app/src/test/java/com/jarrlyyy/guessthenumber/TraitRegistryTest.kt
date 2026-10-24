package com.jarrlyyy.guessthenumber

import com.jarrlyyy.guessthenumber.traits.BuiltInTraits
import com.jarrlyyy.guessthenumber.traits.TraitCategory
import com.jarrlyyy.guessthenumber.traits.TraitDefinition
import com.jarrlyyy.guessthenumber.traits.TraitEffect
import com.jarrlyyy.guessthenumber.traits.TraitRegistry
import com.jarrlyyy.guessthenumber.traits.TraitVisibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.int

class TraitRegistryTest {
    @Test fun builtInCatalogLoadsOfflineAndHasVisibleAndHiddenTraits() {
        val registry = BuiltInTraits.registry()
        assertEquals(316, registry.size)
        assertNotNull(registry.find("steady_start"))
        assertEquals(93, registry.hidden().size)
        assertEquals(223, registry.visible().size)
    }

    @Test fun rejectsDuplicateIds() {
        val trait = BuiltInTraits.definitions.first()
        assertThrows(IllegalArgumentException::class.java) {
            TraitRegistry(listOf(trait, trait))
        }
    }

    @Test fun rejectsMissingDependencies() {
        val trait = BuiltInTraits.definitions.first().copy(dependencies = setOf("missing_trait"))
        assertThrows(IllegalArgumentException::class.java) { TraitRegistry(listOf(trait)) }
    }

    @Test fun rejectsDependencyCycles() {
        val first = TraitDefinition(
            id = "trait_alpha", titleKey = "trait.alpha.title", descriptionKey = "trait.alpha.description",
            category = TraitCategory.GAMEPLAY, dependencies = setOf("trait_beta"),
            effects = setOf(TraitEffect.COSMETIC_BADGE)
        )
        val second = TraitDefinition(
            id = "trait_beta", titleKey = "trait.beta.title", descriptionKey = "trait.beta.description",
            category = TraitCategory.GAMEPLAY, dependencies = setOf("trait_alpha"),
            effects = setOf(TraitEffect.COSMETIC_BADGE)
        )
        assertThrows(IllegalArgumentException::class.java) { TraitRegistry(listOf(first, second)) }
    }

    @Test fun supportsHiddenTraitFiltering() {
        val registry = BuiltInTraits.registry()
        assertEquals(TraitVisibility.HIDDEN, registry.find("hidden_night_owl")?.visibility)
        assertNull(registry.find("not_a_real_trait"))
    }

    @Test fun largeBuiltInCatalogHasThirtyDistinctFoldersAndSupportedEffects() {
        val registry = BuiltInTraits.registry()
        assertEquals(30, registry.all().map { it.catalogId }.distinct().size)
        assertEquals(316, registry.all().map { it.id }.distinct().size)
        assertTrue(registry.all().all { it.effects.isNotEmpty() })
        assertEquals("100ConsecutiveGuessesTrait", registry.find("hundred_consecutive_guesses")?.packageFolder)
    }

    @Test fun everyBuiltInTraitHasAValidatedMultiFilePackage() {
        val start = java.io.File(System.getProperty("user.dir")).absoluteFile
        var root: java.io.File? = start
        while (root != null && !java.io.File(root, "app/src/main/assets/traits/catalogs").isDirectory) {
            root = root.parentFile
        }
        val projectRoot = root ?: throw AssertionError("Could not locate repository root for trait package assets")
        val json = kotlinx.serialization.json.Json
        val all = BuiltInTraits.registry().all()
        all.forEach { definition ->
            val folder = java.io.File(projectRoot,
                "app/src/main/assets/traits/catalogs/${definition.catalogId}/${definition.packageFolder}")
            assertTrue("Missing trait package folder for ${definition.id}", folder.isDirectory)
            val traitJson = java.io.File(folder, "trait.json")
            val rulesJson = java.io.File(folder, "rules.json")
            val localeJson = java.io.File(folder, "locales/en.json")
            val icon = java.io.File(folder, "icon.svg")
            assertTrue("Missing trait.json for ${definition.id}", traitJson.isFile)
            assertTrue("Missing rules.json for ${definition.id}", rulesJson.isFile)
            assertTrue("Missing locale data for ${definition.id}", localeJson.isFile)
            assertTrue("Missing icon for ${definition.id}", icon.isFile)
            val metadata = json.parseToJsonElement(traitJson.readText()).jsonObject
            val rules = json.parseToJsonElement(rulesJson.readText()).jsonObject
            assertEquals(definition.id, metadata["id"]!!.jsonPrimitive.content)
            assertEquals(definition.catalogId, metadata["catalogId"]!!.jsonPrimitive.content)
            assertEquals(definition.category.name, metadata["category"]!!.jsonPrimitive.content)
            assertEquals(definition.packageFolder, metadata["packageFolder"]!!.jsonPrimitive.content)
            val packageFiles = metadata["packageFiles"]!!.jsonArray.map { it.jsonPrimitive.content }
            assertEquals("Duplicate package file entry for ${definition.id}", packageFiles.size, packageFiles.distinct().size)
            packageFiles.forEach { relativePath ->
                assertTrue("Unsafe package file path for ${definition.id}: $relativePath",
                    !relativePath.startsWith("/") && relativePath.split('/').all { it.isNotBlank() && it != "." && it != ".." })
                val packageAsset = java.io.File(folder, relativePath)
                assertTrue("Missing declared package asset for ${definition.id}: $relativePath", packageAsset.isFile)
                assertTrue("Package asset escapes trait folder: $relativePath",
                    packageAsset.canonicalPath.startsWith(folder.canonicalPath + java.io.File.separator))
            }
            assertEquals(definition.effects.map { it.name }.toSet(), metadata["effects"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet())
            assertEquals(definition.activation.minimumCorrectGuesses,
                rules["activation"]!!.jsonObject["minimumCorrectGuesses"]!!.jsonPrimitive.long)
            assertEquals(definition.activation.minimumBestStreak,
                rules["activation"]!!.jsonObject["minimumBestStreak"]?.jsonPrimitive?.int ?: 0)
            assertTrue("Unsafe SVG in ${definition.id}", !icon.readText().contains("<script", ignoreCase = true))
            val localeDir = java.io.File(folder, "locales")
            val localeFiles = localeDir.listFiles().orEmpty().filter { it.isFile && it.extension.equals("json", true) }
            assertTrue("Missing English fallback locale for ${definition.id}", localeFiles.any { it.name == "en.json" })
            val english = json.parseToJsonElement(localeJson.readText()).jsonObject
            assertEquals(definition.id, english["id"]!!.jsonPrimitive.content)
            assertTrue("Missing English title for ${definition.id}", english["title"]!!.jsonPrimitive.content.isNotBlank())
            assertTrue("Missing English description for ${definition.id}", english["description"]!!.jsonPrimitive.content.isNotBlank())
            val localeSources = localeFiles.associate { it.nameWithoutExtension to it.readText() }
            com.jarrlyyy.guessthenumber.traits.TraitLocaleCatalog().validate(localeSources, definition.id)
            localeFiles.forEach { localeFile ->
                assertTrue("Invalid locale filename: ${localeFile.name}", localeFile.nameWithoutExtension.matches(Regex("[a-z]{2}(-[A-Z]{2})?")))
                val localized = json.parseToJsonElement(localeFile.readText()).jsonObject
                assertEquals("Wrong trait ID in ${localeFile.path}", definition.id, localized["id"]!!.jsonPrimitive.content)
                listOf("title", "description").forEach { key ->
                    assertTrue("Missing $key in ${localeFile.path}", !localized[key]!!.jsonPrimitive.content.isBlank())
                }
                assertEquals("Locale keys differ in ${localeFile.path}", english.keys, localized.keys)
            }
            if (definition.id == "hundred_consecutive_guesses") {
                assertTrue(english["description"]!!.jsonPrimitive.content.contains("100 consecutive"))
                val moduleFile = java.io.File(folder, "module.json")
                assertTrue("Missing module manifest for milestone trait", moduleFile.isFile)
                val moduleSources = mapOf(
                    "logic.lua" to java.io.File(folder, "logic.lua").readText(),
                    "presentation.js" to java.io.File(folder, "presentation.js").readText(),
                    "rules.json" to rulesJson.readText()
                )
                val runtime = com.jarrlyyy.guessthenumber.traits.TraitExtensionRuntime()
                val extension = runtime.validate(moduleFile.readText(), moduleSources, definition.id)
                val before = runtime.evaluateLua(extension, "logic", mapOf("bestStreak" to "99"), definition.effects)
                val reached = runtime.evaluateLua(extension, "logic", mapOf("bestStreak" to "100"), definition.effects)
                assertTrue("Milestone must not activate early", !before.eligible)
                assertTrue("Milestone should activate at 100", reached.eligible)
                assertEquals(definition.effects, reached.requestedEffects)
                assertTrue(runtime.javascriptPresentation(extension, "view").contains("renderTrait"))
            }
        }
        all.map { it.catalogId }.distinct().forEach { catalogId ->
            val indexFile = java.io.File(projectRoot, "app/src/main/assets/traits/catalogs/${catalogId}/catalog.json")
            assertTrue("Missing catalog index for ${catalogId}", indexFile.isFile)
            val index = json.parseToJsonElement(indexFile.readText()).jsonObject
            val expectedIds = all.filter { it.catalogId == catalogId }.map { it.id }.toSet()
            assertEquals(expectedIds, index["traitIds"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet())
            assertEquals(expectedIds.size, index["traitCount"]!!.jsonPrimitive.int)
        }
    }

}
