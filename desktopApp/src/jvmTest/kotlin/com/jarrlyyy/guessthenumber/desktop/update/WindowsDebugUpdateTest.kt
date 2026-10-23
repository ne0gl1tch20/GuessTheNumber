package com.jarrlyyy.guessthenumber.desktop.update

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
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
    fun discoversNewerBuildFromPublicDebugLatestRelease() {
        val update = clientForRun(6).findLatestUpdate("1.0.5")
        assertNotNull(update)
        assertEquals("1.0.6", update?.version)
        assertEquals("windows-debug-6-1", update?.tagName)
        assertEquals("$RELEASE_DOWNLOAD_PREFIX$ARCHIVE_NAME", update?.downloadUrl)
        assertEquals("$RELEASE_DOWNLOAD_PREFIX$MANIFEST_NAME", update?.manifestUrl)
        assertEquals(6L, update?.runNumber)
        assertEquals(1L, update?.runAttempt)
    }

    @Test
    fun doesNotOfferInstalledOrNewerVersion() {
        assertNull(clientForRun(6).findLatestUpdate("1.0.6"))
        assertNull(clientForRun(6).findLatestUpdate("1.0.7"))
    }

    @Test
    fun rejectsManifestWithUnexpectedAssetUrlOrInvalidTag() {
        val responses = standardResponses(6).toMutableMap()
        responses[MANIFEST_URL] = manifest(6).replace("windows-debug-6-1", "windows-debug-5-1")
        val client = WindowsDebugUpdateClient { url ->
            StubConnection(url, responses[url.toString()] ?: error("Unexpected request: $url"))
        }
        assertNull(client.findLatestUpdate("1.0.5"))
    }

    @Test
    fun extractsOnlyExpectedMsi() {
        val payload = "fake-msi-payload".toByteArray()
        val archive = createZip(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME to payload)
        val destination = Files.createTempDirectory("gtn-test-").resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME)
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

    private fun clientForRun(runNumber: Long): WindowsDebugUpdateClient {
        val responses = standardResponses(runNumber)
        return WindowsDebugUpdateClient { url ->
            StubConnection(url, responses[url.toString()] ?: error("Unexpected request: $url"))
        }
    }

    private fun standardResponses(runNumber: Long): Map<String, String> = mapOf(
        RELEASE_API to """{"assets":[
            {"name":"$ARCHIVE_NAME","browser_download_url":"$RELEASE_DOWNLOAD_PREFIX$ARCHIVE_NAME"},
            {"name":"$MANIFEST_NAME","browser_download_url":"$RELEASE_DOWNLOAD_PREFIX$MANIFEST_NAME"}
        ]}""",
        MANIFEST_URL to manifest(runNumber)
    )

    private fun manifest(runNumber: Long) =
        """{"channel":"windows-debug","version":"1.0.$runNumber","runNumber":"$runNumber","runAttempt":"1","commit":"0123456789abcdef0123456789abcdef01234567","assetName":"$ARCHIVE_NAME","sha256":"${"a".repeat(64)}","tag":"windows-debug-$runNumber-1"}"""

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
        const val RELEASE_API =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/releases/tags/debug-latest"
        const val RELEASE_DOWNLOAD_PREFIX =
            "https://github.com/ne0gl1tch20/GuessTheNumber/releases/download/debug-latest/"
        const val ARCHIVE_NAME = "GuessTheNumber-Windows-Desktop.zip"
        const val MANIFEST_NAME = "windows-debug-manifest.json"
        const val MANIFEST_URL = "$RELEASE_DOWNLOAD_PREFIX$MANIFEST_NAME"
    }
}
