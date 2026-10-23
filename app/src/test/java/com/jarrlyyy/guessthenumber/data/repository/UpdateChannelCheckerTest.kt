package com.jarrlyyy.guessthenumber.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateChannelCheckerTest {
    @Test
    fun stableDebugBuildNumberComparisonTreatsOlderAndSameBuildAsCurrent() {
        assertEquals(true, UpdateChannelChecker.isStableDebugBuildCurrent(54L, 55L))
        assertEquals(true, UpdateChannelChecker.isStableDebugBuildCurrent(55L, 55L))
        assertEquals(false, UpdateChannelChecker.isStableDebugBuildCurrent(56L, 55L))
    }

    @Test
    fun newerStableDebugBuildIsNotHiddenByMatchingSourceCommit() {
        // Run numbers distinguish reruns even when both builds share the same commit.
        assertEquals(false, UpdateChannelChecker.isStableDebugBuildCurrent(78L, 77L))
        assertEquals(true, UpdateChannelChecker.isStableDebugBuildCurrent(77L, 77L))
    }

    @Test
    fun newerVersionCodeIsAvailableEvenWhenReleaseRunNumberLooksOld() {
        assertEquals(
            false,
            UpdateChannelChecker.isStableDebugReleaseCurrent(
                remoteVersionName = "3.0.4",
                remoteVersionCode = 22L,
                remoteBuildNumber = 87L,
                installedVersionName = "3.0.3",
                installedVersionCode = 21L,
                installedBuildNumber = 88L
            )
        )
    }

    @Test
    fun newerVersionNameIsAvailableWhenVersionCodeIsReusedAndRunNumberIsStale() {
        assertEquals(
            false,
            UpdateChannelChecker.isStableDebugReleaseCurrent(
                remoteVersionName = "3.0.4",
                remoteVersionCode = 21L,
                remoteBuildNumber = 87L,
                installedVersionName = "3.0.3",
                installedVersionCode = 21L,
                installedBuildNumber = 88L
            )
        )
    }

    @Test
    fun matchingVersionUsesBuildNumberAsTieBreaker() {
        assertEquals(
            true,
            UpdateChannelChecker.isStableDebugReleaseCurrent(
                remoteVersionName = "3.0.3",
                remoteVersionCode = 21L,
                remoteBuildNumber = 87L,
                installedVersionName = "3.0.3",
                installedVersionCode = 21L,
                installedBuildNumber = 88L
            )
        )
        assertEquals(
            false,
            UpdateChannelChecker.isStableDebugReleaseCurrent(
                remoteVersionName = "3.0.3",
                remoteVersionCode = 21L,
                remoteBuildNumber = 89L,
                installedVersionName = "3.0.3",
                installedVersionCode = 21L,
                installedBuildNumber = 88L
            )
        )
    }

    @Test
    fun olderVersionCodeIsNeverOfferedAsAnUpdate() {
        assertEquals(
            true,
            UpdateChannelChecker.isStableDebugReleaseCurrent(
                remoteVersionName = "3.9.0",
                remoteVersionCode = 20L,
                remoteBuildNumber = 999L,
                installedVersionName = "3.0.3",
                installedVersionCode = 21L,
                installedBuildNumber = 1L
            )
        )
    }

    @Test
    fun parsesStableDebugMetadataFromPublicReleaseNotes() {
        val metadata = UpdateChannelChecker.parseStableDebugReleaseMetadata(
            """
            Stable Debug #1084

            Version: 3.0.2 (20)
            Commit: 0123456789abcdef0123456789abcdef01234567
            """.trimIndent()
        )

        assertEquals(1084L, metadata?.runNumber)
        assertEquals("3.0.2", metadata?.versionName)
        assertEquals(20L, metadata?.versionCode)
        assertEquals("0123456789abcdef0123456789abcdef01234567", metadata?.commitSha)
    }

    @Test
    fun rejectsStableDebugReleaseNotesWithMissingOrInvalidMetadata() {
        assertEquals(
            null,
            UpdateChannelChecker.parseStableDebugReleaseMetadata(
                "Stable Debug #1084\\nVersion: 3.0.2 (20)\\nCommit: invalid"
            )
        )
    }

    @Test
    fun comparesReleaseVersionsNumerically() {
        assertEquals(1, UpdateChannelChecker.compareVersions("3.0.10", "3.0.2"))
        assertEquals(-1, UpdateChannelChecker.compareVersions("3.1.0", "3.2.0"))
        assertEquals(0, UpdateChannelChecker.compareVersions("v3.0.2", "3.0.2"))
    }
}
