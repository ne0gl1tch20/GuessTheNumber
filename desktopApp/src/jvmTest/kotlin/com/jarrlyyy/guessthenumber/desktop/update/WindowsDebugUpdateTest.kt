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
    fun discoversNewerMatchingWindowsDebugRelease() {
        val manifestUrl = "https://github.com/ne0gl1tch20/GuessTheNumber/releases/download/windows-debug-6-1/windows-debug-manifest.json"
        val archiveUrl = "https://github.com/ne0gl1tch20/GuessTheNumber/releases/download/windows-debug-6-1/GuessTheNumber-Windows-Desktop.zip"
        val responses = mapOf(
            RELEASES_URL to """
                [
                  {
                    "tag_name":"windows-debug-6-1",
                    "prerelease":true,
                    "assets":[
                      {"name":"windows-debug-manifest.json","browser_download_url":"$manifestUrl"},
                      {"name":"GuessTheNumber-Windows-Desktop.zip","browser_download_url":"$archiveUrl"}
                    ]
                  }
                ]
            """.trimIndent(),
            manifestUrl to """
                {
                  "channel":"windows-debug",
                  "version":"1.0.6",
                  "assetName":"GuessTheNumber-Windows-Desktop.zip",
                  "sha256":"${"a".repeat(64)}",
                  "tag":"windows-debug-6-1"
                }
            """.trimIndent()
        )
        val client = WindowsDebugUpdateClient { url -> StubConnection(url, responses.getValue(url.toString())) }

        val update = client.findLatestUpdate("1.0.5")

        assertNotNull(update)
        assertEquals("1.0.6", update?.version)
        assertEquals(archiveUrl, update?.downloadUrl)
    }

    @Test
    fun doesNotOfferInstalledOrOlderVersion() {
        val client = clientWithManifest(version = "1.0.6", tag = "windows-debug-6-1")

        assertNull(client.findLatestUpdate("1.0.6"))
        assertNull(client.findLatestUpdate("1.0.7"))
    }

    @Test
    fun rejectsManifestWithWrongTagOrInvalidChecksum() {
        val wrongTag = clientWithManifest(version = "1.0.6", tag = "windows-debug-5-1")
        val badChecksum = clientWithManifest(version = "1.0.6", tag = "windows-debug-6-1", checksum = "bad")

        assertNull(wrongTag.findLatestUpdate("1.0.5"))
        assertNull(badChecksum.findLatestUpdate("1.0.5"))
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
        try {
            WindowsDebugArchiveValidator.extractExpectedMsi(
                archive,
                Files.createTempDirectory("gtn-test-").resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME)
            )
        } finally {
            Files.deleteIfExists(archive)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsUnexpectedFilesInArchive() {
        val archive = createZip("unexpected.txt" to byteArrayOf(1))
        try {
            WindowsDebugArchiveValidator.extractExpectedMsi(
                archive,
                Files.createTempDirectory("gtn-test-").resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME)
            )
        } finally {
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

    private fun clientWithManifest(version: String, tag: String, checksum: String = "a".repeat(64)): WindowsDebugUpdateClient {
        val manifestUrl = "https://github.com/ne0gl1tch20/GuessTheNumber/releases/download/windows-debug-6-1/windows-debug-manifest.json"
        val archiveUrl = "https://github.com/ne0gl1tch20/GuessTheNumber/releases/download/windows-debug-6-1/GuessTheNumber-Windows-Desktop.zip"
        val releases = """
            [{
              "tag_name":"windows-debug-6-1",
              "prerelease":true,
              "assets":[
                {"name":"windows-debug-manifest.json","browser_download_url":"$manifestUrl"},
                {"name":"GuessTheNumber-Windows-Desktop.zip","browser_download_url":"$archiveUrl"}
              ]
            }]
        """.trimIndent()
        val manifest = """
            {
              "channel":"windows-debug",
              "version":"$version",
              "assetName":"GuessTheNumber-Windows-Desktop.zip",
              "sha256":"$checksum",
              "tag":"$tag"
            }
        """.trimIndent()
        val responses = mapOf(RELEASES_URL to releases, manifestUrl to manifest)
        return WindowsDebugUpdateClient { url -> StubConnection(url, responses.getValue(url.toString())) }
    }

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
        const val RELEASES_URL = "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/releases?per_page=100"
    }
}
