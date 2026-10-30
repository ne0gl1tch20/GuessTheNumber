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
    fun discoversNewestSuccessfulMasterActionsArtifact() {
        val update = clientForRun(6).findLatestUpdate("1.0.5")
        assertNotNull(update)
        assertEquals("1.0.6", update?.version)
        assertEquals("windows-debug-6-2", update?.tagName)
        assertEquals("$ARTIFACT_DOWNLOAD_PREFIX", update?.artifactDownloadUrl)
        assertEquals(6L, update?.runNumber)
        assertEquals(2L, update?.runAttempt)
    }

    @Test
    fun doesNotOfferInstalledOrNewerVersion() {
        assertNull(clientForRun(6).findLatestUpdate("1.0.6"))
        assertNull(clientForRun(6).findLatestUpdate("1.0.7"))
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsExpiredLatestArtifactInsteadOfFallingBackToAnOlderBuild() {
        val responses = standardResponses(6).toMutableMap()
        responses[ARTIFACTS_URL] = """{"artifacts":[
            {"name":"GuessTheNumber-Windows-Debug","expired":true,"archive_download_url":"$ARTIFACT_DOWNLOAD_PREFIX"}
        ]}"""
        val client = WindowsDebugUpdateClient { url ->
            StubConnection(url, responses[url.toString()] ?: error("Unexpected request: $url"))
        }
        client.findLatestUpdate("1.0.5")
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
    fun extractsExpectedFilesFromActionsArtifact() {
        val archiveBytes = "inner-installer-zip".toByteArray()
        val manifestBytes = """{"channel":"windows-debug"}""".toByteArray()
        val artifact = createZip(
            WindowsDebugArtifactValidator.EXPECTED_ARCHIVE_NAME to archiveBytes,
            WindowsDebugArtifactValidator.EXPECTED_MANIFEST_NAME to manifestBytes
        )
        val directory = Files.createTempDirectory("gtn-artifact-test-")
        val archive = directory.resolve(WindowsDebugArtifactValidator.EXPECTED_ARCHIVE_NAME)
        val manifest = directory.resolve(WindowsDebugArtifactValidator.EXPECTED_MANIFEST_NAME)
        try {
            WindowsDebugArtifactValidator.extractArtifactFiles(artifact, archive, manifest)
            assertArrayEquals(archiveBytes, Files.readAllBytes(archive))
            assertArrayEquals(manifestBytes, Files.readAllBytes(manifest))
        } finally {
            Files.deleteIfExists(archive)
            Files.deleteIfExists(manifest)
            Files.deleteIfExists(directory)
            Files.deleteIfExists(artifact)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsUnexpectedFilesInActionsArtifact() {
        val artifact = createZip(
            WindowsDebugArtifactValidator.EXPECTED_ARCHIVE_NAME to byteArrayOf(1),
            WindowsDebugArtifactValidator.EXPECTED_MANIFEST_NAME to byteArrayOf(2),
            "extra.txt" to byteArrayOf(3)
        )
        val directory = Files.createTempDirectory("gtn-artifact-test-")
        try {
            WindowsDebugArtifactValidator.extractArtifactFiles(
                artifact,
                directory.resolve(WindowsDebugArtifactValidator.EXPECTED_ARCHIVE_NAME),
                directory.resolve(WindowsDebugArtifactValidator.EXPECTED_MANIFEST_NAME)
            )
        } finally {
            Files.deleteIfExists(directory.resolve(WindowsDebugArtifactValidator.EXPECTED_ARCHIVE_NAME))
            Files.deleteIfExists(directory.resolve(WindowsDebugArtifactValidator.EXPECTED_MANIFEST_NAME))
            Files.deleteIfExists(directory)
            Files.deleteIfExists(artifact)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsPathTraversalInActionsArtifact() {
        val artifact = createZip(
            "../windows-debug-manifest.json" to byteArrayOf(1)
        )
        val directory = Files.createTempDirectory("gtn-artifact-test-")
        try {
            WindowsDebugArtifactValidator.extractArtifactFiles(
                artifact,
                directory.resolve(WindowsDebugArtifactValidator.EXPECTED_ARCHIVE_NAME),
                directory.resolve(WindowsDebugArtifactValidator.EXPECTED_MANIFEST_NAME)
            )
        } finally {
            Files.deleteIfExists(directory.resolve(WindowsDebugArtifactValidator.EXPECTED_ARCHIVE_NAME))
            Files.deleteIfExists(directory.resolve(WindowsDebugArtifactValidator.EXPECTED_MANIFEST_NAME))
            Files.deleteIfExists(directory)
            Files.deleteIfExists(artifact)
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
        WORKFLOW_RUNS_URL to """{"workflow_runs":[
            {"id":600,"run_number":$runNumber,"run_attempt":2,"head_sha":"0123456789abcdef0123456789abcdef01234567","event":"push","head_branch":"master","status":"completed","conclusion":"success"},
            {"id":599,"run_number":5,"run_attempt":1,"event":"push","head_branch":"master","status":"completed","conclusion":"success"},
            {"id":598,"run_number":7,"run_attempt":1,"event":"pull_request","head_branch":"feature","status":"completed","conclusion":"success"}
        ]}""",
        ARTIFACTS_URL to """{"artifacts":[
            {"name":"GuessTheNumber-Windows-Debug","expired":false,"archive_download_url":"$ARTIFACT_DOWNLOAD_PREFIX"}
        ]}"""
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
        const val WORKFLOW_RUNS_URL =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/workflows/windows-desktop.yml/runs?branch=master&status=success&per_page=20"
        const val ARTIFACTS_URL =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/runs/600/artifacts"
        const val ARTIFACT_DOWNLOAD_PREFIX =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/artifacts/9000/zip"
    }
}
