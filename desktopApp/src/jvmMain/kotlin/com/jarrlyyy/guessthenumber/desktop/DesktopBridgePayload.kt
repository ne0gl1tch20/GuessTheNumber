package com.jarrlyyy.guessthenumber.desktop

import org.json.JSONObject
import org.json.JSONTokener

/** Normalizes the values crossing SwingWebView JavaScript callback boundaries. */
internal object DesktopBridgePayload {
    private const val MAX_CALLBACK_LENGTH = 32_768
    private const val MAX_REQUEST_LENGTH = 16_384
    private const val WINDOWS_CALLBACK_NAME = "gtnDesktopPostMessage"

    fun normalize(raw: String): String? {
        if (raw.length > MAX_CALLBACK_LENGTH) return null
        val candidate = raw.trim()
        if (candidate.isEmpty()) return null
        val payload = if (isWindowsCallbackEnvelope(candidate)) {
            unwrapWindowsCallback(candidate) ?: return null
        } else candidate
        if (payload.length > MAX_REQUEST_LENGTH) return null
        if (isSingleJsonObject(payload)) return payload

        val decoded = decodeSingleJsonString(payload)?.trim()
            ?.takeIf { it.isNotEmpty() && it.length <= MAX_REQUEST_LENGTH }
            ?: return null
        return decoded.takeIf(::isSingleJsonObject)
    }

    fun recoverRequestId(raw: String): String {
        if (raw.length > MAX_CALLBACK_LENGTH) return ""
        val candidate = raw.trim()
        val payload = if (isWindowsCallbackEnvelope(candidate)) {
            unwrapWindowsCallback(candidate) ?: return ""
        } else candidate
        val decoded = decodeSingleJsonString(payload)
        return sequenceOf(payload, decoded.orEmpty())
            .mapNotNull { value ->
                runCatching { JSONObject(value).opt("id") as? String }.getOrNull()
            }
            .firstOrNull { it.matches(Regex("[A-Za-z0-9_-]{1,48}")) }
            .orEmpty()
    }

    private fun isWindowsCallbackEnvelope(value: String): Boolean = runCatching {
        val obj = JSONObject(value)
        obj.has("name") || obj.has("seq") || obj.has("args")
    }.getOrDefault(false)

    /**
     * SwingWebView's Windows WebView2 backend sends callback arguments inside
     * a native envelope: {name, seq, args:[...]} rather than passing the
     * original argument directly. Other backends pass the argument itself.
     */
    private fun unwrapWindowsCallback(value: String): String? = runCatching {
        val envelope = JSONObject(value)
        if (envelope.optString("name") != WINDOWS_CALLBACK_NAME) return null
        val args = envelope.optJSONArray("args") ?: return null
        if (args.length() != 1) return null
        args.opt(0) as? String
    }.getOrNull()

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