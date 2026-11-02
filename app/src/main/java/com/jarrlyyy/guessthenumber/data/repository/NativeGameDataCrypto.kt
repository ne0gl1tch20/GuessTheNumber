package com.jarrlyyy.guessthenumber.data.repository

/**
 * Native AES-256-GCM decryptor for GTNGBIN1 chunks.
 *
 * Returns null only when the native library or JNI entry point is unavailable;
 * authentication failures are propagated and never downgraded to the JVM path.
 */
internal object NativeGameDataCrypto {
    @Volatile
    private var loadAttempted = false

    @Volatile
    private var nativeLoaded = false

    private fun ensureLoaded(): Boolean {
        if (loadAttempted) return nativeLoaded
        synchronized(this) {
            if (!loadAttempted) {
                nativeLoaded = runCatching {
                    System.loadLibrary("gtn")
                    true
                }.getOrDefault(false)
                loadAttempted = true
            }
        }
        return nativeLoaded
    }

    fun decryptChunk(blob: ByteArray, group: String, id: String): ByteArray? {
        if (!ensureLoaded()) return null
        return try {
            nativeDecryptChunk(blob, group, id)
        } catch (_: UnsatisfiedLinkError) {
            nativeLoaded = false
            null
        }
    }

    /** Strict entry point used by on-device tests so they cannot silently exercise the JVM fallback. */
    internal fun decryptNativeForTest(blob: ByteArray, group: String, id: String): ByteArray {
        check(ensureLoaded()) { "Native game-data crypto library is unavailable" }
        return nativeDecryptChunk(blob, group, id)
    }

    private external fun nativeDecryptChunk(blob: ByteArray, group: String, id: String): ByteArray
}
