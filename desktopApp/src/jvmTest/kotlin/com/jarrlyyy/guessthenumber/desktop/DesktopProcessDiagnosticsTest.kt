package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopProcessDiagnosticsTest {
    @Test
    fun reportsOnlyLocalRuntimeAndBoundedMemoryMetrics() {
        val snapshot = DesktopProcessDiagnostics.capture()

        assertTrue(snapshot.osName.length <= 120)
        assertTrue(snapshot.osVersion.length <= 80)
        assertTrue(snapshot.architecture.length <= 40)
        assertTrue(snapshot.javaVersion.length <= 40)
        assertTrue(snapshot.availableProcessors >= 1)
        assertTrue(snapshot.uptimeMillis >= 0L)
        assertTrue(snapshot.heapUsedBytes >= 0L)
        assertTrue(snapshot.heapCommittedBytes >= 0L)
        assertTrue(snapshot.nonHeapUsedBytes >= 0L)
    }
}
