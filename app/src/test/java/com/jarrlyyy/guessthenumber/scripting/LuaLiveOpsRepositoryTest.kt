package com.jarrlyyy.guessthenumber.scripting

import com.jarrlyyy.guessthenumber.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LuaLiveOpsRepositoryTest {
    @Test
    fun refreshFailsClosedWithoutSigningKey() = runBlocking {
        if (BuildConfig.LIVEOPS_PUBLIC_KEY_BASE64.isBlank()) {
            val result = LuaLiveOpsRepository(RuntimeEnvironment.getApplication()).refresh()
            assertTrue(result is LuaLiveOpsRefreshResult.Unavailable)
            assertTrue((result as LuaLiveOpsRefreshResult.Unavailable).reason.contains("signing key"))
        }
    }
    @Test
    fun rejectsDuplicateScriptFileNamesBeforeActivation() {
        val repository = LuaLiveOpsRepository(RuntimeEnvironment.getApplication())
        val hash = "a".repeat(64)
        val payload = LuaRemotePayload(
            schemaVersion = 1,
            hostApiVersion = LuaEngine.HOST_API_VERSION,
            minAppVersionCode = 19,
            maxAppVersionCode = 999999,
            bundleVersion = 3,
            scripts = listOf(
                LuaRemoteScript("welcome", "shared.lua", "Welcome", "Welcome activity", hash),
                LuaRemoteScript("second", "shared.lua", "Second", "Second activity", hash)
            )
        )

        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            repository.validatePayload(payload)
        }
        assertTrue(error.message!!.contains("Duplicate LiveOps script file names"))
    }

    @Test
    fun rejectsDuplicateScriptIdsBeforeActivation() {
        val repository = LuaLiveOpsRepository(RuntimeEnvironment.getApplication())
        val hash = "b".repeat(64)
        val payload = LuaRemotePayload(
            schemaVersion = 1,
            hostApiVersion = LuaEngine.HOST_API_VERSION,
            minAppVersionCode = 19,
            maxAppVersionCode = 999999,
            bundleVersion = 3,
            scripts = listOf(
                LuaRemoteScript("duplicate", "first.lua", "First", "First activity", hash),
                LuaRemoteScript("duplicate", "second.lua", "Second", "Second activity", hash)
            )
        )

        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            repository.validatePayload(payload)
        }
        assertTrue(error.message!!.contains("Duplicate LiveOps script IDs"))
    }

    @Test
    fun rejectsVersionBelowHighWaterMarkAfterRollback() {
        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            validateIncomingVersion(incomingVersion = 2, activeVersion = 2, highestAcceptedVersion = 3)
        }
        assertTrue(error.message!!.contains("highest accepted version"))
    }

    @Test
    fun allowsReactivationOfHighestVersionAfterRollback() {
        validateIncomingVersion(incomingVersion = 3, activeVersion = 2, highestAcceptedVersion = 3)
    }

    @Test
    fun rejectsNonPositiveIncomingVersion() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            validateIncomingVersion(incomingVersion = 0, activeVersion = 0, highestAcceptedVersion = 0)
        }
    }


    @Test
    fun cachedEntriesFailClosedWhenNoVerifiedActiveBundleExists() {
        val context = RuntimeEnvironment.getApplication()
        val liveOpsRoot = java.io.File(context.filesDir, "lua-liveops")
        liveOpsRoot.deleteRecursively()
        liveOpsRoot.mkdirs()

        try {
            val repository = LuaLiveOpsRepository(context)
            assertTrue(repository.loadVerifiedCachedEntries().isEmpty())
            assertTrue(repository.readVerifiedCachedScript(
                LuaScriptEntry(
                    id = "unverified",
                    file = "unverified.lua",
                    title = "Unverified",
                    description = "Must not execute",
                    source = "liveops"
                )
            ) == null)
        } finally {
            liveOpsRoot.deleteRecursively()
        }
    }

    @Test
    fun rejectsUnsupportedRemoteCapabilitiesBeforeActivation() {
        val repository = LuaLiveOpsRepository(RuntimeEnvironment.getApplication())
        val payload = LuaRemotePayload(
            schemaVersion = 1,
            hostApiVersion = LuaEngine.HOST_API_VERSION,
            minAppVersionCode = 19,
            maxAppVersionCode = 999999,
            bundleVersion = 4,
            scripts = listOf(
                LuaRemoteScript(
                    id = "unsupported_capability",
                    fileName = "unsupported.lua",
                    title = "Unsupported",
                    description = "Requests an unavailable host capability",
                    sha256 = "c".repeat(64),
                    capabilities = listOf("log", "filesystem")
                )
            )
        )

        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            repository.validatePayload(payload)
        }
        assertTrue(error.message!!.contains("unsupported capability"))
    }
}
