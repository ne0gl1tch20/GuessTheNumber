package com.jarrlyyy.guessthenumber.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader

class LocaleManager(private val context: Context) {
    private var translations: JSONObject = JSONObject()

    suspend fun loadLocale(localeFileName: String = "locales/en_us.json") {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open(localeFileName)
                val reader = InputStreamReader(inputStream)
                val jsonString = reader.readText()
                reader.close()
                translations = JSONObject(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                translations = JSONObject()
            }
        }
    }

    fun getString(key: String, default: String = key): String {
        return translations.optString(key, default)
    }

    fun getString(key: String, vararg formatArgs: Any): String {
        val raw = translations.optString(key, key)
        return try {
            String.format(raw, *formatArgs)
        } catch (e: Exception) {
            raw
        }
    }

    fun getString(key: String, default: String, vararg formatArgs: Any): String {
        val raw = translations.optString(key, default)
        return try {
            String.format(raw, *formatArgs)
        } catch (e: Exception) {
            raw
        }
    }
}
