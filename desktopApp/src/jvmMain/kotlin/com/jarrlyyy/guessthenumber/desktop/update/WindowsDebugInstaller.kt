package com.jarrlyyy.guessthenumber.desktop.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

internal class WindowsDebugInstaller {
    fun downloadVerifyAndLaunch(update: WindowsDebugUpdate) {
        require(isDebugReleaseAssetUrl(update.archiveDownloadUrl) &&
            isDebugReleaseAssetUrl(update.manifestDownloadUrl)
        ) {
            "The Windows Debug download must come from the public debug-latest release."
        }

        val temporaryDirectory = Files.createTempDirectory("gtn-windows-debug-update-")
        try {
            val archivePath = temporaryDirectory.resolve(WindowsDebugArtifactValidator.EXPECTED_ARCHIVE_NAME)
            val manifestPath = temporaryDirectory.resolve(WindowsDebugArtifactValidator.EXPECTED_MANIFEST_NAME)
            downloadFile(update.manifestDownloadUrl, manifestPath, MAX_MANIFEST_BYTES)
            downloadFile(update.archiveDownloadUrl, archivePath, MAX_ARCHIVE_BYTES)
            verifyManifest(manifestPath, archivePath, update)
            val msiPath = temporaryDirectory.resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME)
            WindowsDebugArchiveValidator.extractExpectedMsi(archivePath, msiPath)
            launchInstallerAndCleanup(msiPath, temporaryDirectory)
        } catch (error: Exception) {
            deleteTree(temporaryDirectory)
            throw error
        }
    }

    private fun isDebugReleaseAssetUrl(value: String): Boolean {
        val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return false
        return uri.scheme == "https" && uri.host == "github.com" &&
            uri.path.startsWith("/ne0gl1tch20/GuessTheNumber/releases/download/debug-latest/")
    }

    private fun verifyManifest(manifestPath: Path, archivePath: Path, update: WindowsDebugUpdate) {
        val manifest = Json.parseToJsonElement(Files.readString(manifestPath)).jsonObject
        require(manifest["channel"]?.jsonPrimitive?.content == "windows-debug") {
            "The downloaded release asset is not a Windows Debug build."
        }
        require(manifest["version"]?.jsonPrimitive?.content == update.version) {
            "The Windows Debug package version does not match the update check."
        }
        require(manifest["runNumber"]?.jsonPrimitive?.content == update.runNumber.toString()) {
            "The Windows Debug run number does not match."
        }
        require(manifest["runAttempt"]?.jsonPrimitive?.content == update.runAttempt.toString()) {
            "The Windows Debug run attempt does not match."
        }
        require(manifest["commit"]?.jsonPrimitive?.content == update.commitSha) {
            "The Windows Debug commit SHA does not match the release manifest."
        }
        require(manifest["tag"]?.jsonPrimitive?.content == update.tagName) {
            "The Windows Debug build tag does not match."
        }
        require(manifest["assetName"]?.jsonPrimitive?.content == WindowsDebugArtifactValidator.EXPECTED_ARCHIVE_NAME) {
            "The Windows Debug manifest names an unexpected installer archive."
        }
        val checksum = manifest["sha256"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("The Windows Debug release manifest has no SHA-256 checksum.")
        require(checksum == update.archiveSha256) {
            "The Windows Debug release manifest checksum changed after the update check."
        }
        WindowsDebugArchiveValidator.verifySha256(archivePath, checksum)
    }

    private fun downloadFile(address: String, destination: Path, maximumBytes: Long) {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "GuessTheNumber-Windows-Debug-Updater")
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("GitHub release asset download failed with HTTP ${connection.responseCode}.")
            }
            if (connection.contentLengthLong > maximumBytes) {
                throw IllegalStateException("A Windows Debug release asset exceeded the size limit.")
            }
            Files.newOutputStream(destination).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > maximumBytes) {
                            throw IllegalStateException("A Windows Debug release asset exceeded the size limit.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun launchInstallerAndCleanup(msiPath: Path, temporaryDirectory: Path) {
        val scriptPath = temporaryDirectory.resolve("install-and-cleanup.ps1")
        val msiLiteral = powershellLiteral(msiPath.toAbsolutePath().toString())
        val directoryLiteral = powershellLiteral(temporaryDirectory.toAbsolutePath().toString())
        val script = """
            ${'$'}ErrorActionPreference = 'Stop'
            ${'$'}msiPath = '$msiLiteral'
            ${'$'}temporaryDirectory = '$directoryLiteral'
            try {
                ${'$'}installer = Start-Process -FilePath 'msiexec.exe' -ArgumentList @('/i', ('"{0}"' -f ${'$'}msiPath)) -Wait -PassThru
                exit ${'$'}installer.ExitCode
            } finally {
                Start-Sleep -Seconds 2
                Remove-Item -LiteralPath ${'$'}temporaryDirectory -Recurse -Force -ErrorAction SilentlyContinue
            }
        """.trimIndent()
        Files.writeString(scriptPath, script)
        try {
            ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-File", scriptPath.toAbsolutePath().toString()
            ).start()
        } catch (error: Exception) {
            deleteTree(temporaryDirectory)
            throw IllegalStateException("Could not start the Windows MSI installer.", error)
        }
    }

    private fun powershellLiteral(value: String): String = value.replace("'", "''")

    private fun deleteTree(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { path ->
                runCatching { Files.deleteIfExists(path) }
            }
        }
    }

    private companion object {
        const val MAX_ARCHIVE_BYTES = 400L * 1024 * 1024
        const val MAX_MANIFEST_BYTES = 64L * 1024
    }
}
