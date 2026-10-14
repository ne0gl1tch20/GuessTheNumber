package com.jarrlyyy.guessthenumber.data

import com.jarrlyyy.guessthenumber.data.multiplayer.LocalMultiplayerProtocol
import com.jarrlyyy.guessthenumber.data.multiplayer.MultiplayerMessageValidation
import com.jarrlyyy.guessthenumber.data.multiplayer.RejectionReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMultiplayerProtocolTest {
    @Test
    fun acceptsWellFormedVersionOneMessage() {
        val result = LocalMultiplayerProtocol.validate(
            """{"protocolVersion":1,"sessionId":"room_1234","senderId":"peer_a","sequence":0,"type":"HELLO","payload":{"displayName":"Player"}}"""
        )

        assertTrue(result is MultiplayerMessageValidation.Accepted)
    }

    @Test
    fun rejectsOversizedMessageBeforeParsing() {
        val result = LocalMultiplayerProtocol.validate(" ".repeat(LocalMultiplayerProtocol.MAX_MESSAGE_CHARS + 1))

        assertEquals(
            RejectionReason.MESSAGE_TOO_LARGE,
            (result as MultiplayerMessageValidation.Rejected).reason
        )
    }

    @Test
    fun rejectsUnsupportedVersion() {
        val result = LocalMultiplayerProtocol.validate(
            """{"protocolVersion":2,"sessionId":"room_1234","senderId":"peer_a","sequence":0,"type":"HELLO","payload":{}}"""
        )

        assertEquals(
            RejectionReason.UNSUPPORTED_VERSION,
            (result as MultiplayerMessageValidation.Rejected).reason
        )
    }

    @Test
    fun rejectsUnknownMessageType() {
        val result = LocalMultiplayerProtocol.validate(
            """{"protocolVersion":1,"sessionId":"room_1234","senderId":"peer_a","sequence":0,"type":"EXECUTE_COMMAND","payload":{}}"""
        )

        assertEquals(
            RejectionReason.UNKNOWN_MESSAGE_TYPE,
            (result as MultiplayerMessageValidation.Rejected).reason
        )
    }

    @Test
    fun rejectsClientSuppliedRewardFields() {
        val result = LocalMultiplayerProtocol.validate(
            """{"protocolVersion":1,"sessionId":"room_1234","senderId":"peer_a","sequence":0,"type":"ROUND_RESULT","payload":{"reward":999999}}"""
        )

        assertEquals(
            RejectionReason.FORBIDDEN_AUTHORITATIVE_FIELD,
            (result as MultiplayerMessageValidation.Rejected).reason
        )
    }

    @Test
    fun rejectsNegativeSequenceAndInvalidSessionId() {
        val negativeSequence = LocalMultiplayerProtocol.validate(
            """{"protocolVersion":1,"sessionId":"room_1234","senderId":"peer_a","sequence":-1,"type":"HELLO","payload":{}}"""
        )
        val invalidSession = LocalMultiplayerProtocol.validate(
            """{"protocolVersion":1,"sessionId":"bad","senderId":"peer_a","sequence":0,"type":"HELLO","payload":{}}"""
        )

        assertEquals(
            RejectionReason.INVALID_SEQUENCE,
            (negativeSequence as MultiplayerMessageValidation.Rejected).reason
        )
        assertEquals(
            RejectionReason.INVALID_SESSION_ID,
            (invalidSession as MultiplayerMessageValidation.Rejected).reason
        )
    }
}
