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
    fun discoversNewerSuccessfulMasterDesktopArtifact() {
        val client = clientForRun(runNumber = 6)
        val update = client.findLatestUpdate("1.0.5")

        assertNotNull(update)
        assertEquals("1.0.6", update?.version)
        assertEquals("windows-debug-6-1", update?.tagName)
        assertEquals("https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/artifacts/88/zip", update?.downloadUrl)
        assertEquals(6L, update?.runNumber)
        assertEquals(1L, update?.runAttempt)
    }

    @Test
    fun doesNotOfferInstalledOrNewerVersion() {
        assertNull(clientForRun(6).findLatestUpdate("1.0.6"))
        assertNull(clientForRun(6).findLatestUpdate("1.0.7"))
    }

    @Test
    fun findsNewerBuildEvenWhenApiReturnsOlderRunFirst() {
        val downloadUrl = "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/artifacts/88/zip"
        val responses = mapOf(
            WORKFLOW_RUNS_URL to """{"workflow_runs":[
                {"id":122,"run_number":5,"run_attempt":1,"event":"push","head_branch":"master","conclusion":"success"},
                {"id":123,"run_number":6,"run_attempt":1,"event":"push","head_branch":"master","conclusion":"success"}
            ]}""",
            "$ACTIONS_API/runs/123/artifacts?per_page=100" to
                """{"artifacts":[{"name":"GuessTheNumber-Windows-Debug","expired":false,"archive_download_url":"$downloadUrl"}]}"""
        )
        val client = WindowsDebugUpdateClient { url ->
            StubConnection(url, responses[url.toString()] ?: error("Unexpected request: $url"))
        }

        val update = client.findLatestUpdate("1.0.5")

        assertNotNull(update)
        assertEquals("1.0.6", update?.version)
        assertEquals(6L, update?.runNumber)
    }

    @Test
    fun ignoresNonMasterRunsAndExpiredArtifacts() {
        val runsUrl = WORKFLOW_RUNS_URL
        val artifactsUrl = "$ACTIONS_API/runs/123/artifacts?per_page=100"
        val responses = mapOf(
            runsUrl to """{"workflow_runs":[
                {"id":123,"run_number":7,"run_attempt":1,"event":"pull_request","head_branch":"master","conclusion":"success"},
                {"id":123,"run_number":6,"run_attempt":1,"event":"push","head_branch":"master","conclusion":"success"}
            ]}""",
            artifactsUrl to """{"artifacts":[{"name":"GuessTheNumber-Windows-Debug","expired":true,"archive_download_url":"https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/artifacts/88/zip"}]}"""
        )
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
                archive,
                directory.resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME)
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
                archive,
                directory.resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME)
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
        val runsUrl = WORKFLOW_RUNS_URL
        val artifactsUrl = "$ACTIONS_API/runs/123/artifacts?per_page=100"
        val downloadUrl = "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/artifacts/88/zip"
        val responses = mapOf(
            runsUrl to """{"workflow_runs":[{"id":123,"run_number":$runNumber,"run_attempt":1,"event":"push","head_branch":"master","conclusion":"success"}]}""",
            artifactsUrl to """{"artifacts":[{"name":"GuessTheNumber-Windows-Debug","expired":false,"archive_download_url":"$downloadUrl"}]}"""
        )
        return WindowsDebugUpdateClient { url ->
            StubConnection(url, responses[url.toString()] ?: error("Unexpected request: $url"))
        }
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
        const val ACTIONS_API =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions"
        const val WORKFLOW_RUNS_URL =
            "$ACTIONS_API/workflows/windows-desktop.yml/runs?branch=master&status=success&per_page=100"
    }
}
