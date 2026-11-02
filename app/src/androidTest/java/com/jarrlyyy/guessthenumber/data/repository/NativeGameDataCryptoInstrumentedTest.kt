package com.jarrlyyy.guessthenumber.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeGameDataCryptoInstrumentedTest {
    @Test
    fun decryptsArchiveChunkUsingNativeAesGcm() {
        val expected = "native game-data archive fixture".toByteArray()
        val chunk = encryptedChunk(expected, group = "web-play", id = "01")

        val actual = NativeGameDataCrypto.decryptNativeForTest(chunk, "web-play", "01")

        assertArrayEquals(expected, actual)
    }

    @Test
    fun rejectsTamperedAuthenticationTag() {
        val chunk = encryptedChunk("authenticated fixture".toByteArray(), "web-play", "01")
        val tampered = chunk.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 0x01).toByte() }

        assertThrows(SecurityException::class.java) {
            NativeGameDataCrypto.decryptNativeForTest(tampered, "web-play", "01")
        }
    }

    @Test
    fun authenticatesSubsystemAndChunkIdAsAdditionalData() {
        val chunk = encryptedChunk("context-bound fixture".toByteArray(), "web-play", "01")

        assertThrows(SecurityException::class.java) {
            NativeGameDataCrypto.decryptNativeForTest(chunk, "traits", "01")
        }
        assertThrows(SecurityException::class.java) {
            NativeGameDataCrypto.decryptNativeForTest(chunk, "web-play", "02")
        }
    }

    @Test
    fun rejectsMalformedArchiveHeader() {
        val chunk = encryptedChunk("header fixture".toByteArray(), "web-play", "01")
        val malformed = chunk.copyOf().also { it[0] = 'X'.code.toByte() }

        assertThrows(SecurityException::class.java) {
            NativeGameDataCrypto.decryptNativeForTest(malformed, "web-play", "01")
        }
    }

    private fun encryptedChunk(plaintext: ByteArray, group: String, id: String): ByteArray {
        val key = MessageDigest.getInstance("SHA-256")
            .digest(KEY_MATERIAL.toByteArray(Charsets.UTF_8))
        val nonce = ByteArray(12) { index -> (index + 1).toByte() }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD("1:$group:$id".toByteArray(Charsets.UTF_8))
        val sealed = cipher.doFinal(plaintext)
        return MAGIC + byteArrayOf(1) + nonce + sealed
    }

    private companion object {
        const val KEY_MATERIAL = "GuessTheNumber|game-data|archive-v1|2026"
        val MAGIC = "GTNGBIN1".toByteArray(Charsets.US_ASCII)
    }
}
