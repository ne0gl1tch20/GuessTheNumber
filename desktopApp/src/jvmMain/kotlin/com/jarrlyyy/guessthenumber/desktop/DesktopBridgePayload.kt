package com.jarrlyyy.guessthenumber.desktop

import org.json.JSONObject
import org.json.JSONTokener

/** Normalizes the values crossing SwingWebView JavaScript callback boundaries. */
internal object DesktopBridgePayload {
    private const val MAX_CALLBACK_LENGTH = 32_768
    private const val MAX_REQUEST_LENGTH = 16_384

    fun normalize(raw: String): String? {
        if (raw.length > MAX_CALLBACK_LENGTH) return null
        val candidate = raw.trim()
        if (candidate.isEmpty()) return null
        if (isSingleJsonObject(candidate)) return candidate

        val decoded = decodeSingleJsonString(candidate)?.trim()
            ?.takeIf { it.isNotEmpty() && it.length <= MAX_REQUEST_LENGTH }
            ?: return null
        return decoded.takeIf(::isSingleJsonObject)
    }

    fun recoverRequestId(raw: String): String {
        if (raw.length > MAX_CALLBACK_LENGTH) return ""
        val candidate = raw.trim()
        val decoded = decodeSingleJsonString(candidate)
        return sequenceOf(candidate, decoded.orEmpty())
            .mapNotNull { value ->
                runCatching { JSONObject(value).opt("id") as? String }.getOrNull()
            }
            .firstOrNull { it.matches(Regex("[A-Za-z0-9_-]{1,48}")) }
            .orEmpty()
    }

    private fun isSingleJsonObject(value: String): Boolean = runCatching {
        val tokener = JSONTokener(value)
        val parsed = tokener.nextValue()
        parsed is JSONObject && tokener.nextClean() == '\u0000'
    }.getOrDefault(false)

    private fun decodeSingleJsonString(value: String): String? {
        return runCatching {
            val tokener = JSONTokener(value)
            val decoded = tokener.nextValue() as? String
            if (decoded == null || tokener.nextClean() != '\u0000') null else decoded
        }.getOrNull()
    }
}