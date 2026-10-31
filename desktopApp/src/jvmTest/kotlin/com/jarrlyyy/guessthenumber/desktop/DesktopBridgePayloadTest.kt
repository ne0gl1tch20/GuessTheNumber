package com.jarrlyyy.guessthenumber.desktop

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DesktopBridgePayloadTest {
    private val request = """{"version":1,"id":"bridge-test","type":"read_state"}"""

    @Test fun acceptsRawJsonObject() {
        assertEquals(request, DesktopBridgePayload.normalize(request))
    }

    @Test fun acceptsWhitespaceAroundRawJsonObject() {
        assertEquals(request, DesktopBridgePayload.normalize("  \n$request\t "))
    }

    @Test fun decodesJsonEncodedStringPayload() {
        assertEquals(request, DesktopBridgePayload.normalize(JSONObject.quote(request)))
    }

    @Test fun rejectsMalformedJson() {
        assertNull(DesktopBridgePayload.normalize("""{"version":1,"type":"""))
    }

    @Test fun rejectsNonObjectJsonValues() {
        assertNull(DesktopBridgePayload.normalize("""["not","an","object"]"""))
        assertNull(DesktopBridgePayload.normalize("null"))
        assertNull(DesktopBridgePayload.normalize("42"))
    }

    @Test fun rejectsTrailingGarbageAfterObject() {
        assertNull(DesktopBridgePayload.normalize("$request trailing"))
    }

    @Test fun rejectsTrailingGarbageAfterEncodedString() {
        assertNull(DesktopBridgePayload.normalize(JSONObject.quote(request) + " trailing"))
    }

    @Test fun rejectsOversizedCallbackBeforeParsing() {
        assertNull(DesktopBridgePayload.normalize(" ".repeat(32_769)))
    }

    @Test fun rejectsOversizedDecodedRequest() {
        val largeRequest = """{"payload":"${"x".repeat(16_400)}"}"""
        assertNull(DesktopBridgePayload.normalize(JSONObject.quote(largeRequest)))
    }
}