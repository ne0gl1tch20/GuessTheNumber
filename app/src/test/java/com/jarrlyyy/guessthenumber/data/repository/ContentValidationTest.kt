package com.jarrlyyy.guessthenumber.data.repository

import com.jarrlyyy.guessthenumber.domain.model.AchievementDef
import com.jarrlyyy.guessthenumber.domain.model.ShopItemDef
import com.jarrlyyy.guessthenumber.domain.model.UpgradeDef
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentValidationTest {
    @Test
    fun acceptsSupportedSchemaVersion() {
        ContentValidation.requireSupportedSchema(1)
    }

    @Test
    fun rejectsUnknownSchemaVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            ContentValidation.requireSupportedSchema(99)
        }
    }

    @Test
    fun rejectsDuplicateContentIds() {
        assertThrows(IllegalArgumentException::class.java) {
            ContentValidation.requireUniqueIds(listOf("reward_one", "reward_one"))
        }
    }

    @Test
    fun rejectsInvalidUpgradeCostsAndMultipliers() {
        assertThrows(IllegalArgumentException::class.java) {
            ContentValidation.validateUpgrades(
                listOf(UpgradeDef("fast", "Fast", "A test upgrade", 10, "-5", 0.5, 1.0))
            )
        }
    }

    @Test
    fun rejectsNegativeShopAndAchievementRewards() {
        assertThrows(IllegalArgumentException::class.java) {
            ContentValidation.validateShopItems(
                listOf(ShopItemDef("item", "Item", "Test item", -1))
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ContentValidation.validateAchievements(
                listOf(AchievementDef("achievement", "Achievement", "Test achievement", -10))
            )
        }
    }

    @Test
    fun acceptsValidUniqueIds() {
        ContentValidation.requireUniqueIds(listOf("first", "second"))
    }
}
