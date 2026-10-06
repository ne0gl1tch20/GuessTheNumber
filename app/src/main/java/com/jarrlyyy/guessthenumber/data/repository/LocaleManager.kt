package com.jarrlyyy.guessthenumber.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

class LocaleManager(private val context: Context) {
    private var translations: JSONObject = JSONObject()
    private var currentLocale: Locale = Locale.US

    suspend fun loadLocale(localeFileName: String = ENGLISH_FILE) {
        withContext(Dispatchers.IO) {
            val requestedTag = supportedLocales.firstOrNull { it.fileName == localeFileName }?.tag ?: "en-US"
            val locale = Locale.forLanguageTag(requestedTag)
            try {
                val fallback = readJson(ENGLISH_FILE)
                val overlay = if (localeFileName == ENGLISH_FILE) JSONObject() else readJson(localeFileName)
                val merged = JSONObject(fallback.toString())
                val keys = overlay.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    merged.put(key, overlay.get(key))
                }
                translations = merged
                currentLocale = locale
            } catch (e: Exception) {
                e.printStackTrace()
                translations = JSONObject()
                currentLocale = Locale.US
            }
        }
    }

    suspend fun loadLocaleForTag(localeTag: String?) {
        loadLocale(resolveFileName(localeTag))
    }

    fun resolveFileName(localeTag: String?): String {
        val normalized = localeTag?.trim()?.replace('_', '-')?.lowercase(Locale.ROOT)
        return supportedLocales.firstOrNull { it.tag.lowercase(Locale.ROOT) == normalized }?.fileName
            ?: supportedLocales.firstOrNull {
                normalized != null && it.tag.substringBefore('-').lowercase(Locale.ROOT) == normalized.substringBefore('-')
            }?.fileName
            ?: ENGLISH_FILE
    }

    fun getCurrentLocaleTag(): String = currentLocale.toLanguageTag()

    fun isRtl(): Boolean = currentLocale.language in setOf("ar", "fa", "he", "ur")

    fun getString(key: String, default: String = key): String =
        translations.optString(key, default)

    fun getString(key: String, vararg formatArgs: Any): String =
        formatString(translations.optString(key, key), *formatArgs)

    fun getString(key: String, default: String, vararg formatArgs: Any): String =
        formatString(translations.optString(key, default), *formatArgs)

    fun formatNumber(value: Long): String =
        NumberFormat.getIntegerInstance(currentLocale).format(value)

    fun formatNumber(value: Double, maxFractionDigits: Int = 2): String =
        NumberFormat.getNumberInstance(currentLocale).apply {
            maximumFractionDigits = maxFractionDigits
            minimumFractionDigits = 0
        }.format(value)

    fun formatDecimal(value: BigDecimal, maxFractionDigits: Int = 2): String =
        NumberFormat.getNumberInstance(currentLocale).apply {
            maximumFractionDigits = maxFractionDigits
            minimumFractionDigits = 0
        }.format(value)

    fun formatPercent(value: Double, fractionDigits: Int = 1): String =
        NumberFormat.getPercentInstance(currentLocale).apply {
            maximumFractionDigits = fractionDigits
            minimumFractionDigits = fractionDigits
        }.format(value)

    fun formatCurrency(amount: BigDecimal, currencyCode: String): String =
        NumberFormat.getCurrencyInstance(currentLocale).apply {
            currency = Currency.getInstance(currencyCode)
            maximumFractionDigits = 2
        }.format(amount)

    fun formatDate(timestampMillis: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, currentLocale).format(timestampMillis)

    fun formatTime(timestampMillis: Long): String =
        DateFormat.getTimeInstance(DateFormat.SHORT, currentLocale).format(timestampMillis)

    fun formatDateTime(timestampMillis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, currentLocale).format(timestampMillis)

    fun formatIsoDateTime(timestampMillis: Long): String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", currentLocale)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(timestampMillis))

    fun formatDuration(totalSeconds: Long): String {
        val duration = Duration.ofSeconds(totalSeconds.coerceAtLeast(0))
        val total = duration.seconds
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return if (hours > 0) {
            getString("duration_hms_format", "%s:%s:%s", formatNumber(hours), formatNumber(minutes), formatNumber(seconds))
        } else if (minutes > 0) {
            getString("duration_ms_format", "%s:%s", formatNumber(minutes), formatNumber(seconds))
        } else {
            getString("duration_seconds_format", "%ss", formatNumber(seconds))
        }
    }

    fun formatClock(totalSeconds: Int): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        return "%d:%02d".format(Locale.ROOT, safeSeconds / 60, safeSeconds % 60)
    }

    private fun readJson(fileName: String): JSONObject {
        val inputStream = context.assets.open("locales/$fileName")
        return InputStreamReader(inputStream).use { JSONObject(it.readText()) }
    }

    private fun formatString(raw: String, vararg args: Any): String =
        try {
            String.format(currentLocale, raw, *args)
        } catch (_: Exception) {
            raw
        }

    data class SupportedLocale(
        val tag: String,
        val fileName: String,
        val displayName: String,
        val rtl: Boolean = false
    )

    companion object {
        const val ENGLISH_FILE = "en_us.json"

        val supportedLocales = listOf(
            SupportedLocale("en-US", "en_us.json", "English (US)"),
            SupportedLocale("en-GB", "en_gb.json", "English (UK)"),
            SupportedLocale("fil-PH", "fil_ph.json", "Filipino"),
            SupportedLocale("zh-CN", "zh_cn.json", "简体中文"),
            SupportedLocale("zh-TW", "zh_tw.json", "繁體中文"),
            SupportedLocale("ja-JP", "ja.json", "日本語"),
            SupportedLocale("ko-KR", "ko.json", "한국어"),
            SupportedLocale("es-ES", "es.json", "Español"),
            SupportedLocale("fr-FR", "fr.json", "Français"),
            SupportedLocale("de-DE", "de.json", "Deutsch"),
            SupportedLocale("it-IT", "it.json", "Italiano"),
            SupportedLocale("pt-PT", "pt_pt.json", "Português (Portugal)"),
            SupportedLocale("pt-BR", "pt_br.json", "Português (Brasil)"),
            SupportedLocale("ru-RU", "ru.json", "Русский"),
            SupportedLocale("hi-IN", "hi_in.json", "हिन्दी"),
            SupportedLocale("id-ID", "id.json", "Bahasa Indonesia"),
            SupportedLocale("th-TH", "th.json", "ไทย"),
            SupportedLocale("vi-VN", "vi.json", "Tiếng Việt"),
            SupportedLocale("tr-TR", "tr.json", "Türkçe"),
            SupportedLocale("pl-PL", "pl.json", "Polski"),
            SupportedLocale("uk-UA", "uk.json", "Українська"),
            SupportedLocale("nl-NL", "nl.json", "Nederlands"),
            SupportedLocale("ar-SA", "ar.json", "العربية", rtl = true)
        )
    }
}
