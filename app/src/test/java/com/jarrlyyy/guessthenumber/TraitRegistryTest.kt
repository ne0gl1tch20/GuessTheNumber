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
import org.junit.Test
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
            assertEquals(definition.effects, metadata["effects"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet())
            assertEquals(definition.activation.minimumCorrectGuesses,
                rules["activation"]!!.jsonObject["minimumCorrectGuesses"]!!.jsonPrimitive.long)
            assertTrue("Unsafe SVG in ${definition.id}", !icon.readText().contains("<script", ignoreCase = true))
        }
    }

}
