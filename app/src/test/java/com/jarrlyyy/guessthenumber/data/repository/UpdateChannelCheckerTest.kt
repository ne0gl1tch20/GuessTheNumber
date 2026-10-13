package com.jarrlyyy.guessthenumber.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateChannelCheckerTest {
    @Test
    fun comparesReleaseVersionsNumerically() {
        assertEquals(1, UpdateChannelChecker.compareVersions("3.0.10", "3.0.2"))
        assertEquals(-1, UpdateChannelChecker.compareVersions("3.1.0", "3.2.0"))
        assertEquals(0, UpdateChannelChecker.compareVersions("v3.0.2", "3.0.2"))
    }
}
