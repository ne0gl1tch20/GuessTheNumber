package com.jarrlyyy.guessthenumber.data.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CreativeModeSaveTest {
    @Test
    fun versionTwoRoundTripsWithValidatedEffects() {
        val save = CreativeModeSave(
            name = "Puzzle Lab",
            enabled = true,
            rangeMin = 10,
            rangeMax = 100,
            effects = listOf(
                CreativeModeEffect("range-scale", CreativeEffectType.RANGE_SCALE, 2),
                CreativeModeEffect("bonus-hints", CreativeEffectType.EXTRA_HINTS, 3),
                CreativeModeEffect("attempt-cap", CreativeEffectType.ATTEMPT_LIMIT, 20)
            )
        )
        val decoded = CreativeModeSaveCodec.decodeAndMigrate(CreativeModeSaveCodec.encode(save))
        assertEquals(save, decoded)
        val rules = CreativeModeEffectPolicy.resolve(decoded!!)
        assertEquals(10L, rules?.rangeMin)
        assertEquals(191L, rules?.rangeMax)
        assertEquals(3, rules?.extraHints)
        assertEquals(20, rules?.attemptLimit)
    }

    @Test
    fun fullBackupBundleRoundTripsCreativeSlotData() {
        val creative = CreativeModeSaveCodec.encode(
            CreativeModeSave(name = "Slot Two Lab", enabled = true, rangeMax = 500L)
        )
        val bundle = EncryptedBackupBundle(
            formatVersion = 3,
            appPreferencesJson = "{}",
            activeSlot = 2,
            creativeSlots = mapOf(2 to creative)
        )
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val decoded = json.decodeFromString<EncryptedBackupBundle>(json.encodeToString(bundle))
        assertEquals(3, decoded.formatVersion)
        assertEquals("Slot Two Lab", CreativeModeSaveCodec.decodeAndMigrate(decoded.creativeSlots.getValue(2))?.name)
    }

    @Test
    fun versionOneMigratesToCurrentSchemaWithSafeDefaults() {
        val old = """{"schemaVersion":1,"name":"Old creative save","enabled":true,"rangeMin":1,"rangeMax":50}"""
        val migrated = CreativeModeSaveCodec.decodeAndMigrate(old)
        assertNotNull(migrated)
        assertEquals(CreativeModeSave.CURRENT_SCHEMA_VERSION, migrated?.schemaVersion)
        assertTrue(migrated?.effects.isNullOrEmpty())
    }

    @Test
    fun malformedOrUnsupportedCreativeSavesAreRejected() {
        assertNull(CreativeModeSaveCodec.decodeAndMigrate("""{"schemaVersion":99,"name":"future","rangeMin":1,"rangeMax":2}"""))
        assertNull(CreativeModeSaveCodec.decodeAndMigrate("""{"schemaVersion":2,"name":"bad","rangeMin":9,"rangeMax":2}"""))
        assertFalse(CreativeModeSaveCodec.validate(CreativeModeSave(effects = listOf(
            CreativeModeEffect("untrusted", CreativeEffectType.EXTRA_HINTS, 999)
        ))))
        assertFalse(CreativeModeSaveCodec.validate(CreativeModeSave(effects = listOf(
            CreativeModeEffect("duplicate", CreativeEffectType.EXTRA_HINTS, 1),
            CreativeModeEffect("duplicate", CreativeEffectType.ATTEMPT_LIMIT, 5)
        ))))
    }

    @Test
    fun effectsAreBoundedAndCannotRepresentRewardsOrArbitraryCode() {
        val save = CreativeModeSave(
            enabled = true,
            rangeMin = 1,
            rangeMax = CreativeModeSave.MAX_RANGE,
            effects = listOf(
                CreativeModeEffect("hints", CreativeEffectType.EXTRA_HINTS, 5),
                CreativeModeEffect("scale", CreativeEffectType.RANGE_SCALE, 5),
                CreativeModeEffect("attempts", CreativeEffectType.ATTEMPT_LIMIT, 100)
            )
        )
        val rules = CreativeModeEffectPolicy.resolve(save)
        assertNotNull(rules)
        assertTrue(rules!!.rangeMax <= CreativeModeSave.MAX_RANGE)
        assertTrue(rules.extraHints <= 10)
        assertEquals(100, rules.attemptLimit)
    }

    @Test
    fun disabledCreativeModeDoesNotApplyCustomEffects() {
        val save = CreativeModeSave(
            enabled = false,
            rangeMin = 1,
            rangeMax = 50,
            effects = listOf(CreativeModeEffect("hints", CreativeEffectType.EXTRA_HINTS, 4))
        )
        val rules = CreativeModeEffectPolicy.resolve(save)
        assertEquals(0, rules?.extraHints)
        assertEquals(50L, rules?.rangeMax)
    }
}
