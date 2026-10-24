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

    @Test fun unknownTraitsAndTraitsWithoutArmsCannotSelectVariants() {
        assertFalse(TraitGameplayCatalog.allowsArm("grant_money", "burst"))
        assertFalse(TraitGameplayCatalog.allowsArm("range_scout", "standard"))
        assertFalse(TraitGameplayCatalog.allowsArm("streak_guard", "standard"))
        assertTrue(TraitGameplayCatalog.allowsArm("combo_momentum", "steady"))
    }
}
