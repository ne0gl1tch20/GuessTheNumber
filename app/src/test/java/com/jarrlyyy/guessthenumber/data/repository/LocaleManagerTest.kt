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

    @Test fun everySupportedLocaleResolvesSettingsDescriptions() = runBlocking {
        val manager = LocaleManager(context)
        val keys = listOf("settings_desc_language","settings_desc_theme_mode","settings_desc_theme_preset","settings_desc_theme_colors","settings_desc_number_notation","settings_desc_notifications","settings_desc_reminder_frequency","settings_desc_sound_fx","settings_desc_vibration","settings_desc_reduce_flashes","settings_desc_reduced_motion","settings_desc_master_volume","settings_desc_reset_all_data")
        LocaleManager.supportedLocales.forEach { supported -> manager.loadLocale(supported.fileName); keys.forEach { key -> assertTrue("${supported.tag} is missing settings description: $key", manager.getString(key).isNotBlank()) } }
    }

    @Test fun supportedLocalesHaveAssetsAndFallbackKeys() = runBlocking {
        val manager = LocaleManager(context)
        LocaleManager.supportedLocales.forEach { supported -> manager.loadLocale(supported.fileName); assertEquals(supported.tag, manager.getCurrentLocaleTag()); assertEquals("RTL metadata mismatch for ${supported.tag}", supported.rtl, manager.isRtl()); assertTrue(manager.getString("play").isNotBlank()); assertTrue(manager.getString("settings").isNotBlank()); assertTrue(manager.getString("save_slots").isNotBlank()) }
    }

    @Test fun luaHubAndBundledActivityCopyResolveInEverySupportedLocale() = runBlocking {
        val manager = LocaleManager(context)
        LocaleManager.supportedLocales.forEach { supported ->
            manager.loadLocale(supported.fileName)
            assertTrue(manager.getStringByEnglish("Lua Activity Hub").isNotBlank())
            assertTrue(manager.getStringByEnglish("Which number is a prime number?").isNotBlank())
            val updated = manager.getString("lua_liveops_updated_format", "Updated LiveOps bundle %d (%d scripts).", 3, 2)
            assertTrue(!updated.contains("%d")); assertTrue(!updated.contains("%s"))
        }
    }

    @Test fun switchingLocalesOnTheSameManagerRefreshesTextFormattingAndDirection() = runBlocking {
        val manager = LocaleManager(context)
        manager.loadLocaleForTag("en-US"); assertEquals("Settings", manager.getString("settings")); assertEquals(false, manager.isRtl())
        manager.loadLocaleForTag("fil-PH"); assertEquals("Mga Setting", manager.getString("settings")); assertEquals(false, manager.isRtl())
        manager.loadLocaleForTag("ar-SA"); assertEquals("الإعدادات", manager.getString("settings")); assertEquals(true, manager.isRtl())
        manager.loadLocaleForTag("en-US"); assertEquals("Settings", manager.getString("settings")); assertEquals(false, manager.isRtl())
    }

    @Test fun localeFormattingUsesSelectedLocale() = runBlocking {
        val manager = LocaleManager(context); manager.loadLocaleForTag("de-DE"); assertTrue(manager.formatNumber(1234567L).contains(".")); manager.loadLocaleForTag("en-US"); assertEquals("1,234,567", manager.formatNumber(1234567L))
    }

    @Test fun formattedStringsReplaceAllPlaceholders() = runBlocking {
        val manager = LocaleManager(context); manager.loadLocaleForTag("en-US")
        val version = manager.getString("about_version_format", "Version: %s (%d)", "1.16.0", 15)
        val author = manager.getString("about_library_author_format", "Author: %s", "Google")
        val license = manager.getString("about_library_license_format", "License: %s", "Apache 2.0")
        assertEquals("Version: 1.16.0 (15)", version); assertEquals("Author: Google", author); assertEquals("License: Apache 2.0", license)
        assertTrue(!version.contains("%s") && !version.contains("%d")); assertTrue(!author.contains("%s")); assertTrue(!license.contains("%s"))
    }

    @Test fun rewardFormatDoesNotTreatValueAsFallback() = runBlocking {
        val manager = LocaleManager(context); manager.loadLocaleForTag("en-US")
        assertEquals("Reward: +25 Nebula", manager.getString("reward_format", "25"))
    }

    @Test fun whatsNewPopupStringsAreLocalizedAndVersionFormatted() = runBlocking {
        val manager = LocaleManager(context); manager.loadLocaleForTag("en-US")
        val englishTitle = manager.getString("changelog_popup_title", "🚀 What's New in v%s!", "1.16.0")
        val englishText = manager.getString("changelog_popup_text", "Guess The Number v%s is installed! Check out the changelog for the latest features, polish, and fixes.", "1.16.0")
        assertEquals("🚀 What's New in v1.16.0!", englishTitle); assertEquals("Guess The Number v1.16.0 is installed! Check out the changelog for the latest features, polish, and fixes.", englishText)
        manager.loadLocaleForTag("fil-PH")
        assertEquals("🚀 Ano ang Bago sa v1.16.0!", manager.getString("changelog_popup_title", "🚀 What's New in v%s!", "1.16.0"))
        assertEquals("Naka-install na ang Guess The Number v1.16.0! Tingnan ang changelog para sa mga bagong feature, polish, at fixes.", manager.getString("changelog_popup_text", "Guess The Number v%s is installed! Check out the changelog for the latest features, polish, and fixes.", "1.16.0"))
    }

    @Test fun currencyFormattingUsesLocaleCurrencyRules() = runBlocking {
        val manager = LocaleManager(context); manager.loadLocaleForTag("ja-JP"); val formatted = manager.formatCurrency(BigDecimal("1234.5"), "JPY"); assertTrue(formatted.isNotBlank()); assertTrue(formatted.contains("¥") || formatted.contains("￥") || formatted.contains("JPY"))
    }

    @Test fun englishUiLiteralsResolveThroughActiveLocale() = runBlocking {
        val manager = LocaleManager(context); manager.loadLocaleForTag("fil-PH"); assertEquals("Mga Setting", manager.getStringByEnglish("Settings")); assertEquals("Iba Pa", manager.getStringByEnglish("More")); manager.loadLocaleForTag("en-US"); assertEquals("Settings", manager.getStringByEnglish("Settings"))
    }

    @Test fun unknownLocaleFallsBackToEnglish() = runBlocking {
        val manager = LocaleManager(context); manager.loadLocaleForTag("xx-XX"); assertEquals("en-US", manager.getCurrentLocaleTag()); assertEquals("Play", manager.getString("play"))
    }

    @Test fun invalidCurrencyCodeFallsBackWithoutThrowing() {
        val manager = LocaleManager(context); val formatted = manager.formatCurrency(BigDecimal("1234.50"), "not-a-currency"); assertTrue(formatted.isNotBlank()); assertTrue(formatted.contains("1") || formatted.contains("1,"))
    }

    @Test fun rtlLocaleIsDetected() = runBlocking { val manager = LocaleManager(context); manager.loadLocaleForTag("ar-SA"); assertTrue(manager.isRtl()) }
}
