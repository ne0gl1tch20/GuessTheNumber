package com.jarrlyyy.guessthenumber.desktop.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.zip.ZipInputStream

internal class WindowsDebugInstaller {
    fun downloadVerifyAndLaunch(update: WindowsDebugUpdate) {
        require(update.artifactDownloadUrl.matches(
            Regex("https://api\\.github\\.com/repos/ne0gl1tch20/GuessTheNumber/actions/artifacts/\\d+/zip")
        )) {
            "The Windows Debug download is not a GitHub Actions artifact URL."
        }

        val temporaryDirectory = Files.createTempDirectory("gtn-windows-debug-update-")
        try {
            val artifactPath = temporaryDirectory.resolve(EXPECTED_ARTIFACT_NAME)
            val archivePath = temporaryDirectory.resolve(WindowsDebugArtifactValidator.EXPECTED_ARCHIVE_NAME)
            val manifestPath = temporaryDirectory.resolve(WindowsDebugArtifactValidator.EXPECTED_MANIFEST_NAME)
            downloadFile(update.artifactDownloadUrl, artifactPath, MAX_ARTIFACT_BYTES)
            WindowsDebugArchiveValidator.verifySha256(artifactPath, update.artifactSha256)
            WindowsDebugArtifactValidator.extractArtifactFiles(artifactPath, archivePath, manifestPath)
            verifyManifest(manifestPath, archivePath, update)
            val msiPath = temporaryDirectory.resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME)
            WindowsDebugArchiveValidator.extractExpectedMsi(archivePath, msiPath)
            launchInstallerAndCleanup(msiPath, temporaryDirectory)
        } catch (error: Exception) {
            deleteTree(temporaryDirectory)
            throw error
        }
    }

    private fun writeEntry(input: java.io.InputStream, destination: Path, maximumBytes: Long) {
        Files.newOutputStream(destination).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > maximumBytes) throw IllegalStateException("A Windows Debug artifact file exceeded the size limit.")
                output.write(buffer, 0, count)
            }
        }
    }

    private fun verifyManifest(manifestPath: Path, archivePath: Path, update: WindowsDebugUpdate) {
        val manifest = Json.parseToJsonElement(Files.readString(manifestPath)).jsonObject
        require(manifest["channel"]?.jsonPrimitive?.content == "windows-debug") {
            "The downloaded Actions artifact is not a Windows Debug build."
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
            "The Windows Debug commit SHA does not match the successful CI run."
        }
        require(manifest["tag"]?.jsonPrimitive?.content == update.tagName) {
            "The Windows Debug build tag does not match."
        }
        require(manifest["assetName"]?.jsonPrimitive?.content == WindowsDebugArtifactValidator.EXPECTED_ARCHIVE_NAME) {
            "The Windows Debug manifest names an unexpected installer archive."
        }
        val checksum = manifest["sha256"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("The Windows Debug manifest has no SHA-256 checksum.")
        WindowsDebugArchiveValidator.verifySha256(archivePath, checksum)
    }

    private fun downloadFile(address: String, destination: Path, maximumBytes: Long) {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "GuessTheNumber-Windows-Debug-Updater")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("GitHub Actions artifact download failed with HTTP ${connection.responseCode}.")
            }
            if (connection.contentLengthLong > maximumBytes) {
                throw IllegalStateException("A Windows Debug Actions artifact exceeded the size limit.")
            }
            connection.inputStream.use { input -> writeEntry(input, destination, maximumBytes) }
        } finally {
            connection.disconnect()
        }
    }

    private fun launchInstallerAndCleanup(msiPath: Path, temporaryDirectory: Path) {
        val scriptPath = temporaryDirectory.resolve("install-and-cleanup.ps1")
        val msiLiteral = powershellLiteral(msiPath.toAbsolutePath().toString())
        val directoryLiteral = powershellLiteral(temporaryDirectory.toAbsolutePath().toString())
        val script = """
            $ErrorActionPreference = 'Stop'
            $msiPath = '$msiLiteral'
            $temporaryDirectory = '$directoryLiteral'
            try {
                $installer = Start-Process -FilePath 'msiexec.exe' -ArgumentList @('/i', ('"{0}"' -f $msiPath)) -Wait -PassThru
                exit $installer.ExitCode
            } finally {
                Start-Sleep -Seconds 2
                Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force -ErrorAction SilentlyContinue
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
        const val EXPECTED_ARTIFACT_NAME = "GuessTheNumber-Windows-Debug.zip"
        const val MAX_ARTIFACT_BYTES = 400L * 1024 * 1024
    }
}
