package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class DesktopLuaLiveOpsRepositoryTest {
    @Test
    fun refreshFailsClosedWhenSigningKeyIsNotConfigured() {
        val repository = DesktopLuaLiveOpsRepository(
            root = Files.createTempDirectory("gtn-liveops-empty-key"),
            manifestUrl = "https://example.invalid/manifest.json",
            publicKeyBase64 = ""
        )

        val result = repository.refresh()

        assertTrue(result is DesktopLuaLiveOpsRefreshResult.Unavailable)
        assertTrue((result as DesktopLuaLiveOpsRefreshResult.Unavailable).reason.contains("signing key"))
        assertTrue(repository.loadVerifiedCachedEntries().isEmpty())
    }

    @Test
    fun acceptsCurrentOrNewerSignedBundleVersion() {
        val repository = DesktopLuaLiveOpsRepository(
            root = Files.createTempDirectory("gtn-liveops-version"),
            manifestUrl = "https://example.invalid/manifest.json",
            publicKeyBase64 = "unused"
        )

        repository.validateIncomingVersion(incomingVersion = 5, activeVersion = 4, highestAcceptedVersion = 5)
        repository.validateIncomingVersion(incomingVersion = 6, activeVersion = 4, highestAcceptedVersion = 5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLiveOpsBundleRollbackBelowHighestAcceptedVersion() {
        val repository = DesktopLuaLiveOpsRepository(
            root = Files.createTempDirectory("gtn-liveops-rollback"),
            manifestUrl = "https://example.invalid/manifest.json",
            publicKeyBase64 = "unused"
        )
        repository.validateIncomingVersion(incomingVersion = 4, activeVersion = 3, highestAcceptedVersion = 5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveLiveOpsBundleVersion() {
        val repository = DesktopLuaLiveOpsRepository(
            root = Files.createTempDirectory("gtn-liveops-invalid-version"),
            manifestUrl = "https://example.invalid/manifest.json",
            publicKeyBase64 = "unused"
        )
        repository.validateIncomingVersion(incomingVersion = 0, activeVersion = 0, highestAcceptedVersion = 0)
    }
}
