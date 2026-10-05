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

    suspend fun loadLocale(localeFileName: String = "locales/en_us.json") {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open(localeFileName)
                val reader = InputStreamReader(inputStream)
                val jsonString = reader.readText()
                reader.close()
                translations = JSONObject(jsonString)
                currentLocale = localeFromFileName(localeFileName)
            } catch (e: Exception) {
                e.printStackTrace()
                translations = JSONObject()
                currentLocale = Locale.US
            }
        }
    }

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
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, currentLocale)
            .format(timestampMillis)

    fun formatIsoDateTime(timestampMillis: Long): String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", currentLocale)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(timestampMillis))

    fun formatDuration(totalSeconds: Long): String {
        val duration = Duration.ofSeconds(totalSeconds.coerceAtLeast(0))
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()
        return if (hours > 0) {
            "\${formatNumber(hours)}h \${formatNumber(minutes)}m \${formatNumber(seconds)}s"
        } else if (minutes > 0) {
            "\${formatNumber(minutes)}m \${formatNumber(seconds)}s"
        } else {
            "\${formatNumber(seconds)}s"
        }
    }

    fun formatClock(totalSeconds: Int): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        return "%d:%02d".format(Locale.US, safeSeconds / 60, safeSeconds % 60)
    }

    private fun formatString(raw: String, vararg args: Any): String =
        try {
            String.format(currentLocale, raw, *args)
        } catch (_: Exception) {
            raw
        }

    private fun localeFromFileName(fileName: String): Locale =
        when {
            fileName.contains("fil_ph", ignoreCase = true) -> Locale("fil", "PH")
            fileName.contains("en_us", ignoreCase = true) -> Locale.US
            else -> Locale.US
        }
}
