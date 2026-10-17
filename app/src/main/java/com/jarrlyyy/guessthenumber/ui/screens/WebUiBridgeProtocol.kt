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
        data class Valid(val id: String, val type: String, val guess: Long? = null) : ParseResult
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
        val validId = rawId is String && id.matches(Regex("[A-Za-z0-9_-]{1,48}"))
        if (rawVersion !is Int || rawVersion != 1 || !validId || rawType !is String) {
            return ParseResult.Invalid("invalid_request", id)
        }

        return when (type) {
            "read_state", "increment_counter" -> {
                if (request.length() != 3) ParseResult.Invalid("invalid_request", id)
                else if (type == "increment_counter") ParseResult.Valid(id, type)
                else ParseResult.Valid(id, type)
            }
            "submit_guess" -> {
                if (request.length() != 4 || !request.has("guess")) {
                    return ParseResult.Invalid("invalid_request", id)
                }
                val rawGuess = request.opt("guess")
                val guessText = rawGuess as? String
                    ?: return ParseResult.Invalid("invalid_guess", id)
                if (guessText.length !in 1..20 || !guessText.matches(Regex("-?[0-9]+"))) {
                    return ParseResult.Invalid("invalid_guess", id)
                }
                val guess = guessText.toLongOrNull()
                    ?: return ParseResult.Invalid("invalid_guess", id)
                ParseResult.Valid(id, type, guess)
            }
            else -> ParseResult.Invalid("unknown_action", id)
        }
    }
}


/**
 * Bounded per-page replay guard. IDs are recorded before dispatch so a repeated
 * request cannot apply a gameplay action twice while the page is alive.
 */
internal class WebUiRequestIdWindow(private val capacity: Int = 128) {
    private val seen = LinkedHashSet<String>()

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    fun markIfNew(id: String): Boolean {
        if (!seen.add(id)) return false
        if (seen.size > capacity) {
            val oldest = seen.iterator()
            oldest.next()
            oldest.remove()
        }
        return true
    }
}
