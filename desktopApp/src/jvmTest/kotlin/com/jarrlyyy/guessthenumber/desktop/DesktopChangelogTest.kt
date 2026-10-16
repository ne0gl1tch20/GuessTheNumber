package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopChangelogTest {
    @Test
    fun canonicalAndroidChangelogIsBundledForWindows() {
        val changelog = loadDesktopChangelog().getOrThrow()
        assertTrue(changelog.contains("Unreleased (Windows Android visual parity)"))
        assertTrue(changelog.contains("Windows exact-port UI parity"))
    }
}
