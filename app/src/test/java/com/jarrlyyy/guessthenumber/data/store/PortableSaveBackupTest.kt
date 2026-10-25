package com.jarrlyyy.guessthenumber.data.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableSaveBackupTest {
    @Test
    fun passphraseBackupRoundTripsAcrossIndependentCalls() {
        val envelope = PortableSaveBackup.encrypt(
            """{"formatVersion":2,"slot":"save-data"}""",
            "correct horse battery staple".toCharArray()
        )
        assertTrue(envelope.startsWith(PortableSaveBackup.PREFIX))
        assertEquals(
            """{"formatVersion":2,"slot":"save-data"}""",
            PortableSaveBackup.decrypt(envelope, "correct horse battery staple".toCharArray())
        )
    }

    @Test
    fun wrongPassphraseAndTamperedCiphertextAreRejected() {
        val envelope = PortableSaveBackup.encrypt("private-save-payload", "a long unique backup passphrase".toCharArray())
        assertNull(PortableSaveBackup.decrypt(envelope, "another incorrect passphrase".toCharArray()))
        val encoded = envelope.removePrefix(PortableSaveBackup.PREFIX)
        val changed = encoded.toCharArray()
        changed[changed.lastIndex] = if (changed.last() == 'A') 'B' else 'A'
        assertNull(PortableSaveBackup.decrypt(PortableSaveBackup.PREFIX + String(changed), "a long unique backup passphrase".toCharArray()))
    }

    @Test
    fun malformedPortableEnvelopeNeverFallsBackToLegacyImport() {
        var portableCalled = false
        var legacyCalled = false
        val result = kotlinx.coroutines.runBlocking {
            importPortableOrLegacyBackup(
                input = PortableSaveBackup.PREFIX + "not-base64!",
                passphrase = "a long unique backup passphrase".toCharArray(),
                importPortable = { portableCalled = true; true },
                importLegacy = { legacyCalled = true; true }
            )
        }
        assertFalse(result)
        assertFalse(portableCalled)
        assertFalse(legacyCalled)
    }

    @Test
    fun legacyFormatsStillUseLegacyImportPath() {
        var imported = ""
        val result = kotlinx.coroutines.runBlocking {
            importPortableOrLegacyBackup(
                input = "GTNSEC2:legacy-envelope",
                passphrase = "ignored legacy passphrase".toCharArray(),
                importPortable = { false },
                importLegacy = { imported = it; true }
            )
        }
        assertTrue(result)
        assertEquals("GTNSEC2:legacy-envelope", imported)
    }

    @Test
    fun tooShortPassphrasesAreRejected() {
        val result = runCatching {
            PortableSaveBackup.encrypt("payload", "short".toCharArray())
        }
        assertTrue(result.isFailure)
    }
}
