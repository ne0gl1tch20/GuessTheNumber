package com.jarrlyyy.guessthenumber.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SaveManagerIsolatedStoreTest {
    private val json = Json { encodeDefaults = true }

    private fun isolatedStore(context: Context): Pair<DataStore<Preferences>, Pair<CoroutineScope, File>> {
        val file = File(context.cacheDir, "gtn-save-lab-${System.nanoTime()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }) to (scope to file)
    }

    @Test
    fun legacySaveMigrationMovesValidFixtureIntoSlotOne() = runBlocking {
        val context = RuntimeEnvironment.getApplication<android.app.Application>()
        val (store, cleanup) = isolatedStore(context)
        try {
            val legacy = json.encodeToString(GameState(money = BigNumber(1234), lastSaveTimestamp = 1))
            store.edit { it[stringPreferencesKey("game_state_json")] = legacy }
            val manager = SaveManager(context, store)
            assertFalse(manager.validateSave(json.encodeToString(GameState(money = BigNumber(-1), lastSaveTimestamp = 1))))
            assertFalse(manager.validateSave("{ malformed"))

            assertTrue(manager.migrateLegacySaveToSlot1())
            val migrated = manager.getSaveJson(1)
            assertNotNull(migrated)
            assertTrue(manager.validateSave(migrated!!))
            assertEquals(0, manager.loadGame(1).money.compareTo(BigNumber(1234)))
            assertTrue(store.data.first()[stringPreferencesKey("game_state_json")].isNullOrEmpty())
        } finally {
            cleanup.first.cancel()
            cleanup.second.delete()
        }
    }

    @Test
    fun invalidPrimaryRestoresValidBackupWithoutUsingRealSaveStore() = runBlocking {
        val context = RuntimeEnvironment.getApplication<Context>()
        val (store, cleanup) = isolatedStore(context)
        try {
            val validBackup = json.encodeToString(GameState(money = BigNumber(9876), lastSaveTimestamp = 1))
            store.edit {
                it[stringPreferencesKey("game_state_slot_1_json")] = "{ corrupted primary"
                it[stringPreferencesKey("game_state_slot_1_backup_json")] = validBackup
            }
            val manager = SaveManager(context, store)

            val restored = manager.loadGame(1)
            assertEquals(0, restored.money.compareTo(BigNumber(9876)))
            val repairedPrimary = manager.getSaveJson(1)
            assertNotNull(repairedPrimary)
            assertTrue(manager.validateSave(repairedPrimary!!))
        } finally {
            cleanup.first.cancel()
            cleanup.second.delete()
        }
    }

    @Test
    fun corruptedPrimaryAndBackupSafelyFallBackToFreshState() = runBlocking {
        val context = RuntimeEnvironment.getApplication<Context>()
        val (store, cleanup) = isolatedStore(context)
        try {
            store.edit {
                it[stringPreferencesKey("game_state_slot_1_json")] = "{ corrupted primary"
                it[stringPreferencesKey("game_state_slot_1_backup_json")] = "{ corrupted backup"
            }
            val manager = SaveManager(context, store)
            val state = manager.loadGame(1)
            assertEquals(0, state.money.compareTo(BigNumber.ZERO))
            assertTrue(manager.validateSave(json.encodeToString(state.copy(lastSaveTimestamp = System.currentTimeMillis()))))
        } finally {
            cleanup.first.cancel()
            cleanup.second.delete()
        }
    }
}
