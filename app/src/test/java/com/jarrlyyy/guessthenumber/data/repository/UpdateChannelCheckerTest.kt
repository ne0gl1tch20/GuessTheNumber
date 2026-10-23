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
    fun comparesReleaseVersionsNumerically() {
        assertEquals(1, UpdateChannelChecker.compareVersions("3.0.10", "3.0.2"))
        assertEquals(-1, UpdateChannelChecker.compareVersions("3.1.0", "3.2.0"))
        assertEquals(0, UpdateChannelChecker.compareVersions("v3.0.2", "3.0.2"))
    }
}
