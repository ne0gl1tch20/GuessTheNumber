package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DesktopGlobalPreferencesBackupTest {
    @Test
    fun rejectsNonZipGlobalPreferencesBackup() {
        val result = runCatching {
            DesktopGlobalPreferencesBackup.parseBundle("not a zip".toByteArray())
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsUnexpectedArchiveEntries() {
        val bytes = zip("global-preferences.json" to "{}".toByteArray(), "../outside.txt" to byteArrayOf(1))
        val result = runCatching { DesktopGlobalPreferencesBackup.parseBundle(bytes) }
        assertTrue(result.isFailure)
    }

    @Test
    fun failedImportDoesNotChangeExistingProfileOrNavigation() {
        val invalid = zip("unexpected.txt" to byteArrayOf(1))
        assertEquals(false, DesktopGlobalPreferencesBackup.importBundle(invalid))
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { archive ->
            entries.forEach { (name, content) ->
                archive.putNextEntry(ZipEntry(name))
                archive.write(content)
                archive.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
