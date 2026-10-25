package com.jarrlyyy.guessthenumber.data.store

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Versioned authenticated save-transfer wrapper.
 *
 * The old XOR/Base64 format remains importable through SaveManager.importSave(). New exports
 * use AES-256-GCM with a non-exportable Android Keystore key, so tampering is rejected before
 * the wrapped legacy-compatible payload reaches SaveManager. This is intentionally a narrow
 * security boundary; gameplay state and migration remain owned by SaveManager.
 */
object SecureSaveTransfer {
    private const val PREFIX = "GTNSEC2:"
    private const val KEY_ALIAS = "gtn_save_transfer_v2"
    private const val TRANSFER_AAD = "GuessTheNumber/save-transfer/v2"
    private const val GCM_TAG_BITS = 128
    private const val AES_KEY_BITS = 256

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(AES_KEY_BITS)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(payload: String): String = encryptWithKey(payload, getOrCreateKey())

    /** Injectable-key core keeps authentication behavior testable without Android Keystore. */
    internal fun encryptWithKey(payload: String, key: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(TRANSFER_AAD.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(input: String): String? =
        if (!input.startsWith(PREFIX)) null else decryptWithKey(input, getOrCreateKey())

    /** Returns null for malformed envelopes or any failed GCM authentication check. */
    internal fun decryptWithKey(input: String, key: SecretKey): String? {
        if (!input.startsWith(PREFIX)) return null
        return runCatching {
            val combined = Base64.decode(input.removePrefix(PREFIX), Base64.NO_WRAP)
            require(combined.size > 12 + GCM_TAG_BITS / 8) { "Invalid secure save envelope" }
            val iv = combined.copyOfRange(0, 12)
            val ciphertext = combined.copyOfRange(12, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(TRANSFER_AAD.toByteArray(Charsets.UTF_8))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }

    fun isSecureFormat(input: String): Boolean = input.startsWith(PREFIX)
}

suspend fun SaveManager.exportSecureSave(slot: Int = -1): String =
    SecureSaveTransfer.encrypt(exportSave(slot))

internal suspend fun importSecureOrLegacyPayload(
    input: String,
    decryptSecure: (String) -> String?,
    importPayload: suspend (String) -> Boolean
): Boolean {
    if (SecureSaveTransfer.isSecureFormat(input)) {
        // Never downgrade a malformed/tampered secure envelope into the legacy parser.
        val securePayload = decryptSecure(input) ?: return false
        return importPayload(securePayload)
    }
    // Explicit compatibility path for all supported legacy encrypted/plain imports.
    return importPayload(input)
}

suspend fun SaveManager.importSecureOrLegacySave(input: String, slot: Int = -1): Boolean =
    importSecureOrLegacyPayload(
        input = input,
        decryptSecure = SecureSaveTransfer::decrypt,
        importPayload = { payload -> importSave(payload, slot) }
    )
