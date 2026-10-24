package com.jarrlyyy.guessthenumber.traits

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraitGameplayCatalogTest {
    @Test fun everyLoadoutTraitExistsAndDeclaresRealGameplayEffect() {
        val registry = BuiltInTraits.registry()
        assertEquals(5, TraitGameplayCatalog.all.size)
        TraitGameplayCatalog.all.forEach { entry ->
            val definition = registry.find(entry.id)
            assertNotNull("Missing trait definition for ${entry.id}", definition)
            assertFalse(
                "${entry.id} must not be a cosmetic-only gameplay trait",
                definition!!.effects.all { it == TraitEffect.COSMETIC_BADGE }
            )
        }
    }

    @Test fun everySelectableArmHasOneValidDefaultAndUniqueId() {
        TraitGameplayCatalog.all.forEach { trait ->
            assertEquals(trait.arms.size, trait.arms.map { it.id }.distinct().size)
            if (trait.arms.isEmpty()) assertNull(trait.defaultArmId)
            else assertTrue(trait.defaultArmId != null && trait.arms.any { it.id == trait.defaultArmId })
            trait.arms.forEach { arm ->
                assertTrue(TraitGameplayCatalog.allowsArm(trait.id, arm.id))
                assertFalse(TraitGameplayCatalog.allowsArm(trait.id, "grant_money"))
            }
        }
    }

    @Test fun validatedRemoteGameplayEffectsAppearAsLogicNotCosmetics() {
        val remote = TraitDefinition(
            id = "remote_range_module",
            titleKey = "trait.remote_range.title",
            descriptionKey = "trait.remote_range.description",
            category = TraitCategory.GAMEPLAY,
            effects = setOf(TraitEffect.RANGE_HINT),
            source = TraitSource.LIVEOPS,
            catalogId = "community"
        )
        val cosmetic = TraitDefinition(
            id = "remote_badge_only",
            titleKey = "trait.remote_badge.title",
            descriptionKey = "trait.remote_badge.description",
            category = TraitCategory.GAMEPLAY,
            effects = setOf(TraitEffect.COSMETIC_BADGE),
            source = TraitSource.LIVEOPS,
            catalogId = "community"
        )
        val registry = TraitRegistry(BuiltInTraits.definitions + remote + cosmetic)
        val displayed = TraitGameplayCatalog.forRegistry(registry)
        assertTrue(displayed.any { it.id == "remote_range_module" })
        assertFalse(displayed.any { it.id == "remote_badge_only" })
        assertTrue(displayed.first { it.id == "remote_range_module" }.fallbackDescription.contains("search range"))
    }

    @Test fun unknownTraitsAndTraitsWithoutArmsCannotSelectVariants() {
        assertFalse(TraitGameplayCatalog.allowsArm("grant_money", "burst"))
        assertFalse(TraitGameplayCatalog.allowsArm("range_scout", "standard"))
        assertFalse(TraitGameplayCatalog.allowsArm("streak_guard", "standard"))
        assertTrue(TraitGameplayCatalog.allowsArm("combo_momentum", "steady"))
    }
}
