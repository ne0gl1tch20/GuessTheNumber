package com.jarrlyyy.guessthenumber.ui.screens

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebUiBridgeProtocolTest {
    @Test
    fun acceptsReadStateRequest() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("req_1", "read_state"),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"req_1","type":"read_state"}""")
        )
    }

    @Test
    fun acceptsIncrementRequest() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("tap-2", "increment_counter"),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"tap-2","type":"increment_counter"}""")
        )
    }

    @Test
    fun rejectsMalformedJson() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Invalid("invalid_json"),
            WebUiBridgeProtocol.parse("{not-json")
        )
    }

    @Test
    fun rejectsOversizedMessagesBeforeParsing() {
        val result = WebUiBridgeProtocol.parse("x".repeat(WebUiBridgeProtocol.MAX_MESSAGE_LENGTH + 1))
        assertEquals(WebUiBridgeProtocol.ParseResult.Invalid("message_too_large"), result)
    }

    @Test
    fun rejectsUnknownActions() {
        val result = WebUiBridgeProtocol.parse("""{"version":1,"id":"req-3","type":"delete_save"}""")
        assertEquals(WebUiBridgeProtocol.ParseResult.Invalid("unknown_action", "req-3"), result)
    }

    @Test
    fun rejectsUnexpectedFields() {
        val result = WebUiBridgeProtocol.parse(
            """{"version":1,"id":"req-4","type":"read_state","payload":"unexpected"}"""
        )
        assertTrue(result is WebUiBridgeProtocol.ParseResult.Invalid)
        assertEquals("invalid_request", (result as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }

    @Test
    fun rejectsUnsupportedVersions() {
        val result = WebUiBridgeProtocol.parse("""{"version":2,"id":"req-5","type":"read_state"}""")
        assertEquals("invalid_request", (result as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }

    @Test
    fun rejectsInvalidRequestIds() {
        val result = WebUiBridgeProtocol.parse("""{"version":1,"id":"bad id","type":"read_state"}""")
        assertEquals("invalid_request", (result as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }
    @Test
    fun allowsOnlyMainFrameFromExactBundledPrototypePage() {
        assertTrue(
            WebUiBridgeProtocol.isAllowedSource(
                isMainFrame = true,
                sourceOrigin = Uri.parse("https://appassets.androidplatform.net"),
                currentUrl = "https://appassets.androidplatform.net/assets/web-prototype/index.html"
            )
        )
    }

    @Test
    fun rejectsDisallowedOriginsAndPages() {
        assertEquals(
            false,
            WebUiBridgeProtocol.isAllowedSource(
                isMainFrame = true,
                sourceOrigin = Uri.parse("https://evil.example"),
                currentUrl = "https://evil.example/assets/web-prototype/index.html"
            )
        )
        assertEquals(
            false,
            WebUiBridgeProtocol.isAllowedSource(
                isMainFrame = false,
                sourceOrigin = Uri.parse("https://appassets.androidplatform.net"),
                currentUrl = "https://appassets.androidplatform.net/assets/web-prototype/index.html"
            )
        )
        assertEquals(
            false,
            WebUiBridgeProtocol.isAllowedSource(
                isMainFrame = true,
                sourceOrigin = Uri.parse("https://appassets.androidplatform.net"),
                currentUrl = "https://appassets.androidplatform.net/assets/web-prototype/other.html"
            )
        )
    }

    @Test
    fun rejectsCoercedJsonFieldTypes() {
        val stringVersion = WebUiBridgeProtocol.parse(
            """{"version":"1","id":"req-6","type":"read_state"}"""
        )
        assertEquals(
            "invalid_request",
            (stringVersion as WebUiBridgeProtocol.ParseResult.Invalid).error
        )

        val numericId = WebUiBridgeProtocol.parse(
            """{"version":1,"id":7,"type":"read_state"}"""
        )
        assertEquals(
            "invalid_request",
            (numericId as WebUiBridgeProtocol.ParseResult.Invalid).error
        )
    }


    @Test
    fun requestIdWindowRejectsDuplicates() {
        val window = WebUiRequestIdWindow(capacity = 3)
        assertTrue(window.markIfNew("guess-1"))
        assertEquals(false, window.markIfNew("guess-1"))
        assertTrue(window.markIfNew("guess-2"))
    }

    @Test
    fun requestIdWindowEvictsOldIdsAtItsBound() {
        val window = WebUiRequestIdWindow(capacity = 2)
        assertTrue(window.markIfNew("guess-1"))
        assertTrue(window.markIfNew("guess-2"))
        assertTrue(window.markIfNew("guess-3"))
        assertTrue(window.markIfNew("guess-1"))
        assertEquals(false, window.markIfNew("guess-1"))
    }

    @Test
    fun requestIdWindowRequiresPositiveCapacity() {
        try {
            WebUiRequestIdWindow(capacity = 0)
            throw AssertionError("Expected zero capacity to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: a zero-sized replay window cannot enforce uniqueness.
        }
    }

}
