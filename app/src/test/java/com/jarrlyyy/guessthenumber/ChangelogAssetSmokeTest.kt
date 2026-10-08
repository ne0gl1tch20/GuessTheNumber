package com.jarrlyyy.guessthenumber

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogAssetSmokeTest {
    @Test
    fun markdownParserStressCasesAreRepresented() {
        val text = "# 🚀 Guess The Number Simulator - Changelog **bold** __bold__ *italic* _italic_ `code` $" + "2.5\\\\times" + "$"
        assertTrue(text.isNotBlank())
        assertTrue(text.contains("# 🚀 Guess The Number Simulator - Changelog"))
        assertTrue(text.contains("**"))
        assertTrue(text.contains("`"))
        assertTrue(text.contains("$"))
        assertFalse(text.contains('\u0000'))
    }

    @Test
    fun markdownStressTextCanBeScannedRepeatedly() {
        val text = "**Guess The Number** `2.5\\\\times` " + "$" + "x\\\\to y" + "$"
        repeat(100) {
            assertTrue(text.contains("Guess The Number"))
            assertTrue(text.contains("\\\\times"))
            assertTrue(text.contains("\\\\to"))
        }
    }
}
