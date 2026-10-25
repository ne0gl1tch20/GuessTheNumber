package com.jarrlyyy.guessthenumber.data.store

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
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
    fun legacySaveExportRemainsImportableThroughCompatibilityEntryPoint() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val saveManager = SaveManager(context)
        val legacyExport = saveManager.exportSave()

        assertFalse(SecureSaveTransfer.isSecureFormat(legacyExport))
        assertTrue(saveManager.importSecureOrLegacySave(legacyExport))
    }
}
