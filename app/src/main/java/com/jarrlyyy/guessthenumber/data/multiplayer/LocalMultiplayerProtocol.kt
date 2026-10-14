package com.jarrlyyy.guessthenumber.data.multiplayer

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Versioned message envelope for opt-in local multiplayer transports.
 *
 * This defines and validates messages only; it does not open sockets, discover peers, or
 * authorize rewards. Game state and reward settlement must remain local and host-validated.
 */
@Serializable
data class LocalMultiplayerEnvelope(
    val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
    val sessionId: String,
    val senderId: String,
    val sequence: Long,
    val type: String,
    val payload: JsonObject = JsonObject(emptyMap())
) {
    companion object {
        const val CURRENT_PROTOCOL_VERSION = 1
    }
}

sealed interface MultiplayerMessageValidation {
    data class Accepted(val message: LocalMultiplayerEnvelope) : MultiplayerMessageValidation
    data class Rejected(val reason: RejectionReason) : MultiplayerMessageValidation
}

enum class RejectionReason {
    MESSAGE_TOO_LARGE,
    MALFORMED_JSON,
    UNSUPPORTED_VERSION,
    INVALID_SESSION_ID,
    INVALID_SENDER_ID,
    INVALID_SEQUENCE,
    UNKNOWN_MESSAGE_TYPE,
    TOO_MANY_PAYLOAD_FIELDS,
    INVALID_PAYLOAD_KEY,
    FORBIDDEN_AUTHORITATIVE_FIELD
}

object LocalMultiplayerProtocol {
    const val MAX_MESSAGE_CHARS = 4_096
    const val MAX_PAYLOAD_FIELDS = 12

    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = false
    }
    private val messageTypes = setOf(
        "HELLO",
        "READY",
        "START_ROUND",
        "SUBMIT_GUESS",
        "ROUND_RESULT",
        "LEAVE",
        "HEARTBEAT"
    )
    private val forbiddenAuthoritativeFields = setOf(
        "reward",
        "rewards",
        "money",
        "currency",
        "save",
        "claims",
        "progression",
        "achievement_reward"
    )

    fun validate(rawMessage: String): MultiplayerMessageValidation {
        if (rawMessage.length > MAX_MESSAGE_CHARS) {
            return MultiplayerMessageValidation.Rejected(RejectionReason.MESSAGE_TOO_LARGE)
        }

        val message = try {
            json.decodeFromString<LocalMultiplayerEnvelope>(rawMessage)
        } catch (_: SerializationException) {
            return MultiplayerMessageValidation.Rejected(RejectionReason.MALFORMED_JSON)
        } catch (_: IllegalArgumentException) {
            return MultiplayerMessageValidation.Rejected(RejectionReason.MALFORMED_JSON)
        }

        if (message.protocolVersion != LocalMultiplayerEnvelope.CURRENT_PROTOCOL_VERSION) {
            return MultiplayerMessageValidation.Rejected(RejectionReason.UNSUPPORTED_VERSION)
        }
        if (!message.sessionId.matches(Regex("[A-Za-z0-9_-]{8,64}"))) {
            return MultiplayerMessageValidation.Rejected(RejectionReason.INVALID_SESSION_ID)
        }
        if (!message.senderId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) {
            return MultiplayerMessageValidation.Rejected(RejectionReason.INVALID_SENDER_ID)
        }
        if (message.sequence < 0L) {
            return MultiplayerMessageValidation.Rejected(RejectionReason.INVALID_SEQUENCE)
        }
        if (message.type !in messageTypes) {
            return MultiplayerMessageValidation.Rejected(RejectionReason.UNKNOWN_MESSAGE_TYPE)
        }
        if (message.payload.size > MAX_PAYLOAD_FIELDS) {
            return MultiplayerMessageValidation.Rejected(RejectionReason.TOO_MANY_PAYLOAD_FIELDS)
        }
        if (message.payload.keys.any { !it.matches(Regex("[A-Za-z][A-Za-z0-9_]{0,31}")) }) {
            return MultiplayerMessageValidation.Rejected(RejectionReason.INVALID_PAYLOAD_KEY)
        }
        if (message.payload.keys.any { it.lowercase() in forbiddenAuthoritativeFields }) {
            return MultiplayerMessageValidation.Rejected(RejectionReason.FORBIDDEN_AUTHORITATIVE_FIELD)
        }

        return MultiplayerMessageValidation.Accepted(message)
    }
}
