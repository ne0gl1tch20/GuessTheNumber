package com.jarrlyyy.guessthenumber.ui.screens

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

    fun parse(raw: String): ParseResult {
        if (raw.length > MAX_MESSAGE_LENGTH) {
            return ParseResult.Invalid("message_too_large")
        }

        val request = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return ParseResult.Invalid("invalid_json")
        }

        val id = request.optString("id")
        val type = request.optString("type")
        if (
            request.length() != 3 ||
            request.optInt("version", -1) != 1 ||
            !id.matches(Regex("[A-Za-z0-9_-]{1,48}")) ||
            type !in setOf("read_state", "increment_counter")
        ) {
            val error = if (
                request.length() == 3 &&
                request.optInt("version", -1) == 1 &&
                id.matches(Regex("[A-Za-z0-9_-]{1,48}")) &&
                type !in setOf("read_state", "increment_counter")
            ) "unknown_action" else "invalid_request"
            return ParseResult.Invalid(error, id)
        }

        return ParseResult.Valid(id, type)
    }
}
