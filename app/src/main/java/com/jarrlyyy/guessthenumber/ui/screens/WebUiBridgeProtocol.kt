package com.jarrlyyy.guessthenumber.ui.screens

import android.net.Uri
import org.json.JSONObject

/**
 * Parses the deliberately tiny protocol used by the debug-only shared web UI.
 * This parser never executes actions; the Android host decides how valid actions are handled.
 */
internal object WebUiBridgeProtocol {
    const val MAX_MESSAGE_LENGTH = 512

    sealed interface ParseResult {
        data class Valid(val id: String, val type: String) : ParseResult
        data class Invalid(val error: String, val id: String = "") : ParseResult
    }

    fun isAllowedSource(isMainFrame: Boolean, sourceOrigin: Uri, currentUrl: String?): Boolean {
        if (!isMainFrame || sourceOrigin.scheme != "https" ||
            sourceOrigin.host != "appassets.androidplatform.net"
        ) return false

        val currentUri = currentUrl?.let(Uri::parse) ?: return false
        return currentUri.scheme == "https" &&
            currentUri.host == "appassets.androidplatform.net" &&
            currentUri.path == "/assets/web-prototype/index.html"
    }

    fun parse(raw: String): ParseResult {
        if (raw.length > MAX_MESSAGE_LENGTH) {
            return ParseResult.Invalid("message_too_large")
        }

        val request = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return ParseResult.Invalid("invalid_json")
        }

        val rawId = request.opt("id")
        val rawVersion = request.opt("version")
        val rawType = request.opt("type")
        val id = rawId as? String ?: ""
        val type = rawType as? String ?: ""
        val validEnvelope = request.length() == 3 &&
            rawVersion is Int && rawVersion == 1 &&
            rawId is String && id.matches(Regex("[A-Za-z0-9_-]{1,48}")) &&
            rawType is String
        if (!validEnvelope) return ParseResult.Invalid("invalid_request", id)
        if (type !in setOf("read_state", "increment_counter")) {
            return ParseResult.Invalid("unknown_action", id)
        }

        return ParseResult.Valid(id, type)
    }
}
