package com.jarrlyyy.guessthenumber.data.store

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable backup envelope: the key is derived from a user passphrase, never from Android
 * Keystore. The backup can therefore be restored on another device using the same passphrase.
 */
object PortableSaveBackup {
    const val PREFIX = "GTNPORT1:"
    private const val AAD = "GuessTheNumber/portable-save/v1"
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 310_000
    const val MIN_PASSPHRASE_LENGTH = 12

    fun encrypt(payload: String, passphrase: CharArray): String {
        require(passphrase.size >= MIN_PASSPHRASE_LENGTH) {
            "Passphrase must be at least $MIN_PASSPHRASE_LENGTH characters."
        }
        return try {
            val salt = ByteArray(SALT_BYTES)
            val iv = ByteArray(IV_BYTES)
            SecureRandom().nextBytes(salt)
            SecureRandom().nextBytes(iv)
            val key = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
            val ciphertext = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
            val envelope = ByteArray(salt.size + iv.size + ciphertext.size)
            System.arraycopy(salt, 0, envelope, 0, salt.size)
            System.arraycopy(iv, 0, envelope, salt.size, iv.size)
            System.arraycopy(ciphertext, 0, envelope, salt.size + iv.size, ciphertext.size)
            PREFIX + Base64.getEncoder().withoutPadding().encodeToString(envelope)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    fun decrypt(input: String, passphrase: CharArray): String? {
        if (!isPortableFormat(input) || passphrase.size < MIN_PASSPHRASE_LENGTH) {
            passphrase.fill('\u0000')
            return null
        }
        return try {
            val envelope = Base64.getDecoder().decode(input.removePrefix(PREFIX))
            require(envelope.size > SALT_BYTES + IV_BYTES + TAG_BITS / 8)
            val salt = envelope.copyOfRange(0, SALT_BYTES)
            val iv = envelope.copyOfRange(SALT_BYTES, SALT_BYTES + IV_BYTES)
            val ciphertext = envelope.copyOfRange(SALT_BYTES + IV_BYTES, envelope.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        } finally {
            passphrase.fill('\u0000')
        }
    }

    fun isPortableFormat(input: String): Boolean = input.startsWith(PREFIX)

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
                "AES"
            )
        } finally {
            spec.clearPassword()
        }
    }
}

suspend fun SaveManager.exportPortableBackup(passphrase: CharArray, slot: Int = -1): String =
    PortableSaveBackup.encrypt(exportSave(slot), passphrase)

internal suspend fun importPortableOrLegacyBackup(
    input: String,
    passphrase: CharArray,
    importPortable: suspend (String) -> Boolean,
    importLegacy: suspend (String) -> Boolean
): Boolean {
    if (PortableSaveBackup.isPortableFormat(input)) {
        val payload = PortableSaveBackup.decrypt(input, passphrase) ?: return false
        return importPortable(payload)
    }
    passphrase.fill('\u0000')
    return importLegacy(input)
}

suspend fun SaveManager.importPortableOrLegacyBackup(
    input: String,
    passphrase: CharArray,
    slot: Int = -1
): Boolean = importPortableOrLegacyBackup(
    input = input,
    passphrase = passphrase,
    importPortable = { payload -> importSave(payload, slot) },
    importLegacy = { payload -> importSecureOrLegacySave(payload, slot) }
)
