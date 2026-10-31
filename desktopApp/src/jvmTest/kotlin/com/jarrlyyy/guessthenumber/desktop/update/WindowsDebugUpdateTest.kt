package com.jarrlyyy.guessthenumber.desktop.update

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class WindowsDebugUpdateTest {
    @Test
    fun discoversWindowsAssetsFromPublicDebugLatestRelease() {
        val update = client().findLatestUpdate("1.0.5")
        assertNotNull(update)
        assertEquals("1.0.6", update?.version)
        assertEquals("windows-debug-6-2", update?.tagName)
        assertEquals(WINDOWS_ARCHIVE_URL, update?.archiveDownloadUrl)
        assertEquals(WINDOWS_MANIFEST_URL, update?.manifestDownloadUrl)
        assertEquals("a".repeat(64), update?.archiveSha256)
        assertEquals(6L, update?.runNumber)
        assertEquals(2L, update?.runAttempt)
        assertEquals("0123456789abcdef0123456789abcdef01234567", update?.commitSha)
    }

    @Test
    fun doesNotOfferInstalledOrNewerVersion() {
        assertNull(client().findLatestUpdate("1.0.6"))
        assertNull(client().findLatestUpdate("1.0.7"))
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsReleaseWithoutWindowsManifest() {
        val responses = standardResponses().toMutableMap()
        responses[RELEASE_URL] = """{"assets":[
            {"name":"GuessTheNumber-Windows-Desktop.zip","browser_download_url":"$WINDOWS_ARCHIVE_URL"}
        ]}"""
        WindowsDebugUpdateClient { url ->
            StubConnection(url, responses[url.toString()] ?: error("Unexpected request: $url"))
        }.findLatestUpdate("1.0.5")
    }

    @Test
    fun extractsOnlyExpectedMsi() {
        val payload = "fake-msi-payload".toByteArray()
        val archive = createZip(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME to payload)
        val destination = Files.createTempDirectory("gtn-test-")
            .resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME)
        try {
            WindowsDebugArchiveValidator.extractExpectedMsi(archive, destination)
            assertArrayEquals(payload, Files.readAllBytes(destination))
        } finally {
            Files.deleteIfExists(destination)
            Files.deleteIfExists(destination.parent)
            Files.deleteIfExists(archive)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsZipPathTraversal() {
        val archive = createZip("../GuessTheNumber-Windows-Desktop.msi" to byteArrayOf(1))
        val directory = Files.createTempDirectory("gtn-test-")
        try {
            WindowsDebugArchiveValidator.extractExpectedMsi(
                archive, directory.resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME)
            )
        } finally {
            Files.deleteIfExists(directory.resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME))
            Files.deleteIfExists(directory)
            Files.deleteIfExists(archive)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsUnexpectedFilesInArchive() {
        val archive = createZip("unexpected.txt" to byteArrayOf(1))
        val directory = Files.createTempDirectory("gtn-test-")
        try {
            WindowsDebugArchiveValidator.extractExpectedMsi(
                archive, directory.resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME)
            )
        } finally {
            Files.deleteIfExists(directory.resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME))
            Files.deleteIfExists(directory)
            Files.deleteIfExists(archive)
        }
    }

    @Test
    fun verifiesArchiveChecksum() {
        val archive = Files.createTempFile("gtn-test-", ".zip")
        val payload = "checksum-payload".toByteArray()
        Files.write(archive, payload)
        val expected = MessageDigest.getInstance("SHA-256").digest(payload)
            .joinToString("") { "%02x".format(it) }
        try {
            WindowsDebugArchiveValidator.verifySha256(archive, expected)
        } finally {
            Files.deleteIfExists(archive)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsMismatchedArchiveChecksum() {
        val archive = Files.createTempFile("gtn-test-", ".zip")
        Files.write(archive, "wrong".toByteArray())
        try {
            WindowsDebugArchiveValidator.verifySha256(archive, "a".repeat(64))
        } finally {
            Files.deleteIfExists(archive)
        }
    }

    private fun client(): WindowsDebugUpdateClient {
        val responses = standardResponses()
        return WindowsDebugUpdateClient { url ->
            StubConnection(url, responses[url.toString()] ?: error("Unexpected request: $url"))
        }
    }

    private fun standardResponses(): Map<String, String> = mapOf(
        RELEASE_URL to "{\"assets\":[" +
            "{\"name\":\"GuessTheNumber-debug.zip\",\"browser_download_url\":\"https://github.com/ne0gl1tch20/GuessTheNumber/releases/download/debug-latest/GuessTheNumber-debug.zip\"}," +
            "{\"name\":\"GuessTheNumber-Windows-Desktop.zip\",\"browser_download_url\":\"$WINDOWS_ARCHIVE_URL\"}," +
            "{\"name\":\"windows-debug-manifest.json\",\"browser_download_url\":\"$WINDOWS_MANIFEST_URL\"}" +
            "]}",
        WINDOWS_MANIFEST_URL to "{\"channel\":\"windows-debug\",\"version\":\"1.0.6\",\"runNumber\":\"6\",\"runAttempt\":\"2\",\"commit\":\"0123456789abcdef0123456789abcdef01234567\",\"assetName\":\"GuessTheNumber-Windows-Desktop.zip\",\"sha256\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"tag\":\"windows-debug-6-2\"}"
    )

    private fun createZip(vararg entries: Pair<String, ByteArray>): java.nio.file.Path {
        val path = Files.createTempFile("gtn-test-", ".zip")
        ZipOutputStream(Files.newOutputStream(path)).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return path
    }

    private class StubConnection(url: URL, private val body: String) : HttpURLConnection(url) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false
        override fun getResponseCode(): Int = HTTP_OK
        override fun getInputStream() = ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
    }

    private companion object {
        const val RELEASE_URL =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/releases/tags/debug-latest"
        const val WINDOWS_ARCHIVE_URL =
            "https://github.com/ne0gl1tch20/GuessTheNumber/releases/download/debug-latest/GuessTheNumber-Windows-Desktop.zip"
        const val WINDOWS_MANIFEST_URL =
            "https://github.com/ne0gl1tch20/GuessTheNumber/releases/download/debug-latest/windows-debug-manifest.json"
    }
}
