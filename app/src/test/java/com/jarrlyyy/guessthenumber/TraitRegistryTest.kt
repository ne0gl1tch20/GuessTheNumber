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

class TraitRegistryTest {
    @Test fun builtInCatalogLoadsOfflineAndHasVisibleAndHiddenTraits() {
        val registry = BuiltInTraits.registry()
        assertEquals(8, registry.size)
        assertNotNull(registry.find("steady_start"))
        assertEquals(1, registry.hidden().size)
        assertEquals(7, registry.visible().size)
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
}
