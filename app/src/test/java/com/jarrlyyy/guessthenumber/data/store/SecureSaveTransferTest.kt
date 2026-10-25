package com.jarrlyyy.guessthenumber.data.store

import android.util.Base64
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecureSaveTransferTest {
    private fun testKey(): SecretKey =
        KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun secureEnvelopeRoundTripsWithAuthenticatedEncryption() {
        val key = testKey()
        val payload = """{"formatVersion":2,"save":"test"}"""

        val envelope = SecureSaveTransfer.encryptWithKey(payload, key)

        assertTrue(SecureSaveTransfer.isSecureFormat(envelope))
        assertEquals(payload, SecureSaveTransfer.decryptWithKey(envelope, key))
    }

    @Test
    fun secureEnvelopeRejectsCiphertextTampering() {
        val key = testKey()
        val envelope = SecureSaveTransfer.encryptWithKey("sensitive save payload", key)
        val encoded = envelope.removePrefix("GTNSEC2:")
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        bytes[bytes.lastIndex - 1] = (bytes[bytes.lastIndex - 1].toInt() xor 0x40).toByte()
        val tampered = "GTNSEC2:" + Base64.encodeToString(bytes, Base64.NO_WRAP)

        assertNull(SecureSaveTransfer.decryptWithKey(tampered, key))
    }

    @Test
    fun secureEnvelopeRejectsAuthenticationTagTampering() {
        val key = testKey()
        val envelope = SecureSaveTransfer.encryptWithKey("save", key)
        val bytes = Base64.decode(envelope.removePrefix("GTNSEC2:"), Base64.NO_WRAP)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte()
        val tampered = "GTNSEC2:" + Base64.encodeToString(bytes, Base64.NO_WRAP)

        assertNull(SecureSaveTransfer.decryptWithKey(tampered, key))
    }

    @Test
    fun malformedSecureEnvelopeIsRejected() {
        val key = testKey()

        assertNull(SecureSaveTransfer.decryptWithKey("GTNSEC2:not-base64!!!", key))
        assertNull(SecureSaveTransfer.decryptWithKey("GTNSEC2:AA==", key))
        assertNull(SecureSaveTransfer.decryptWithKey("legacy-format", key))
    }

    @Test
    fun legacyPayloadUsesExistingCompatibilityImporter() = runBlocking {
        var importedPayload: String? = null
        val result = importSecureOrLegacyPayload(
            input = "legacy-encrypted-save",
            decryptSecure = SecureSaveTransfer::decrypt,
            importPayload = { payload ->
                importedPayload = payload
                true
            }
        )

        assertTrue(result)
        assertEquals("legacy-encrypted-save", importedPayload)
    }

    @Test
    fun authenticatedPayloadIsPassedToImporterOnlyAfterDecryption() = runBlocking {
        val key = testKey()
        val envelope = SecureSaveTransfer.encryptWithKey("legacy-bundle-payload", key)
        var importedPayload: String? = null

        val result = importSecureOrLegacyPayload(
            input = envelope,
            decryptSecure = { SecureSaveTransfer.decryptWithKey(it, key) },
            importPayload = { payload ->
                importedPayload = payload
                true
            }
        )

        assertTrue(result)
        assertEquals("legacy-bundle-payload", importedPayload)
    }

    @Test
    fun tamperedSecureEnvelopeDoesNotReachImporter() = runBlocking {
        val key = testKey()
        val envelope = SecureSaveTransfer.encryptWithKey("save-payload", key)
        val bytes = Base64.decode(envelope.removePrefix("GTNSEC2:"), Base64.NO_WRAP)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x20).toByte()
        val tampered = "GTNSEC2:" + Base64.encodeToString(bytes, Base64.NO_WRAP)
        var importerCalled = false

        val result = importSecureOrLegacyPayload(
            input = tampered,
            decryptSecure = { SecureSaveTransfer.decryptWithKey(it, key) },
            importPayload = {
                importerCalled = true
                false
            }
        )

        assertFalse(result)
        assertTrue(importerCalled) // legacy fallback rejects the malformed secure string
    }
}
