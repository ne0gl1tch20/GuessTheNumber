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
    fun acceptsSubmitGuessWithDecimalLongString() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("guess_1", "submit_guess", 42L),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"guess_1","type":"submit_guess","guess":"42"}""")
        )
    }

    @Test
    fun acceptsLongMinimumAndMaximumValues() {
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("max", "submit_guess", Long.MAX_VALUE),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"max","type":"submit_guess","guess":"9223372036854775807"}""")
        )
        assertEquals(
            WebUiBridgeProtocol.ParseResult.Valid("min", "submit_guess", Long.MIN_VALUE),
            WebUiBridgeProtocol.parse("""{"version":1,"id":"min","type":"submit_guess","guess":"-9223372036854775808"}""")
        )
    }

    @Test
    fun rejectsInvalidOrOverflowingGuessStrings() {
        val invalidValues = listOf("", " 4", "4.0", "1e2", "+4", "--4", "9223372036854775808", "-9223372036854775809")
        invalidValues.forEachIndexed { index, guess ->
            val result = WebUiBridgeProtocol.parse(
                """{"version":1,"id":"bad-$index","type":"submit_guess","guess":"$guess"}"""
            )
            assertTrue("Expected invalid guess for '$guess'", result is WebUiBridgeProtocol.ParseResult.Invalid)
            assertEquals("invalid_guess", (result as WebUiBridgeProtocol.ParseResult.Invalid).error)
        }
    }

    @Test
    fun rejectsSubmitGuessWithMissingOrExtraFields() {
        val missing = WebUiBridgeProtocol.parse(
            """{"version":1,"id":"missing","type":"submit_guess"}"""
        )
        assertEquals("invalid_request", (missing as WebUiBridgeProtocol.ParseResult.Invalid).error)

        val extra = WebUiBridgeProtocol.parse(
            """{"version":1,"id":"extra","type":"submit_guess","guess":"4","debug":true}"""
        )
        assertEquals("invalid_request", (extra as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }

    @Test
    fun rejectsNonStringGuessValues() {
        val result = WebUiBridgeProtocol.parse(
            """{"version":1,"id":"number","type":"submit_guess","guess":4}"""
        )
        assertEquals("invalid_guess", (result as WebUiBridgeProtocol.ParseResult.Invalid).error)
    }

}
