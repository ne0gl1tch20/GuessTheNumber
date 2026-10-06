package com.jarrlyyy.guessthenumber.data.repository

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
class LocaleManagerTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun supportedLocalesHaveAssetsAndFallbackKeys() = runBlocking {
        val manager = LocaleManager(context)

        LocaleManager.supportedLocales.forEach { supported ->
            manager.loadLocale(supported.fileName)
            assertEquals(supported.tag, manager.getCurrentLocaleTag())
            assertTrue(manager.getString("play").isNotBlank())
            assertTrue(manager.getString("settings").isNotBlank())
            assertTrue(manager.getString("save_slots").isNotBlank())
        }
    }

    @Test
    fun localeFormattingUsesSelectedLocale() = runBlocking {
        val manager = LocaleManager(context)

        manager.loadLocaleForTag("de-DE")
        assertTrue(manager.formatNumber(1234567L).contains("."))

        manager.loadLocaleForTag("en-US")
        assertEquals("1,234,567", manager.formatNumber(1234567L))
    }

    @Test
    fun formattedStringsReplaceAllPlaceholders() = runBlocking {
        val manager = LocaleManager(context)
        manager.loadLocaleForTag("en-US")

        val version = manager.getString(
            "about_version_format",
            "Version: %s (%d)",
            "1.14.0",
            14
        )
        val author = manager.getString(
            "about_library_author_format",
            "Author: %s",
            "Google"
        )
        val license = manager.getString(
            "about_library_license_format",
            "License: %s",
            "Apache 2.0"
        )

        assertEquals("Version: 1.14.0 (14)", version)
        assertEquals("Author: Google", author)
        assertEquals("License: Apache 2.0", license)
        assertTrue(!version.contains("%s") && !version.contains("%d"))
        assertTrue(!author.contains("%s"))
        assertTrue(!license.contains("%s"))
    }

    @Test
    fun currencyFormattingUsesLocaleCurrencyRules() = runBlocking {
        val manager = LocaleManager(context)
        manager.loadLocaleForTag("ja-JP")
        val formatted = manager.formatCurrency(BigDecimal("1234.5"), "JPY")

        assertTrue(formatted.isNotBlank())
        assertTrue(
            formatted.contains("¥") ||
                formatted.contains("￥") ||
                formatted.contains("JPY")
        )
    }

    @Test
    fun unknownLocaleFallsBackToEnglish() = runBlocking {
        val manager = LocaleManager(context)
        manager.loadLocaleForTag("xx-XX")
        assertEquals("en-US", manager.getCurrentLocaleTag())
        assertEquals("Play", manager.getString("play"))
    }

    @Test
    fun rtlLocaleIsDetected() = runBlocking {
        val manager = LocaleManager(context)
        manager.loadLocaleForTag("ar-SA")
        assertTrue(manager.isRtl())
    }
}
