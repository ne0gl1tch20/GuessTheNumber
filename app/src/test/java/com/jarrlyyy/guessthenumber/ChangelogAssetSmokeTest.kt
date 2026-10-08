package com.jarrlyyy.guessthenumber

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class ChangelogAssetSmokeTest {
    @Test
    fun changelogAssetExistsAndContainsParserStressCases() {
        val context = RuntimeEnvironment.getApplication()
        val text = context.assets.open("changelogs.md").bufferedReader().use { it.readText() }
        assertTrue(text.isNotBlank())
        assertTrue(text.contains("# 🚀 Guess The Number Simulator - Changelog"))
        assertTrue(text.contains("**"))
        assertTrue(text.contains("`"))
        assertTrue(text.contains("$"))
        assertFalse(text.contains("\u0000"))
    }

    @Test
    fun changelogAssetCanBeReadRepeatedly() {
        val context = RuntimeEnvironment.getApplication()
        repeat(100) {
            context.assets.open("changelogs.md").bufferedReader().use {
                assertTrue(it.readLine().orEmpty().contains("Guess The Number"))
            }
        }
    }
}
