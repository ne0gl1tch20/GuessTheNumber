package com.jarrlyyy.guessthenumber.data.repository

import android.content.Context
import org.robolectric.RuntimeEnvironment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

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
    fun currencyFormattingUsesLocaleCurrencyRules() = runBlocking {
        val manager = LocaleManager(context)
        manager.loadLocaleForTag("ja-JP")
        assertTrue(manager.formatCurrency(BigDecimal("1234.5"), "JPY").contains("¥"))
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
