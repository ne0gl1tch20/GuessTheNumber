package com.jarrlyyy.guessthenumber.desktop.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream

internal class WindowsDebugInstaller {
    fun downloadVerifyAndLaunch(update: WindowsDebugUpdate) {
        require(update.downloadUrl.startsWith(ARTIFACT_DOWNLOAD_PREFIX) && update.downloadUrl.endsWith("/zip")) {
            "The debug artifact URL is not from the expected GitHub Actions endpoint."
        }

        val temporaryDirectory = Files.createTempDirectory("gtn-windows-debug-update-")
        try {
            val artifactPath = temporaryDirectory.resolve("windows-actions-artifact.zip")
            val archivePath = temporaryDirectory.resolve("GuessTheNumber-Windows-Desktop.zip")
            val manifestPath = temporaryDirectory.resolve("windows-debug-manifest.json")
            downloadArchive(update.downloadUrl, artifactPath)
            extractArtifactFiles(artifactPath, archivePath, manifestPath)
            verifyManifest(manifestPath, update)
            val msiPath = temporaryDirectory.resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME)
            WindowsDebugArchiveValidator.extractExpectedMsi(archivePath, msiPath)
            launchInstallerAndCleanup(msiPath, temporaryDirectory)
        } catch (error: Exception) {
            deleteTree(temporaryDirectory)
            throw error
        }
    }

    private fun verifyManifest(manifestPath: Path, update: WindowsDebugUpdate) {
        val manifest = Json.parseToJsonElement(Files.readString(manifestPath)).jsonObject
        require(manifest["channel"]?.jsonPrimitive?.content == "windows-debug") {
            "The downloaded artifact is not a Windows Debug build."
        }
        require(manifest["version"]?.jsonPrimitive?.content == update.version) {
            "The Windows Debug artifact version does not match the update check."
        }
        require(manifest["runNumber"]?.jsonPrimitive?.content == update.runNumber.toString()) {
            "The Windows Debug artifact run number does not match."
        }
        require(manifest["runAttempt"]?.jsonPrimitive?.content == update.runAttempt.toString()) {
            "The Windows Debug artifact attempt does not match."
        }
        require(manifest["tag"]?.jsonPrimitive?.content == update.tagName) {
            "The Windows Debug artifact tag does not match."
        }
        require(manifest["assetName"]?.jsonPrimitive?.content == EXPECTED_ARCHIVE_NAME) {
            "The Windows Debug artifact names an unexpected installer archive."
        }
        val checksum = manifest["sha256"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("The Windows Debug manifest has no SHA-256 checksum.")
        val archivePath = manifestPath.parent.resolve(EXPECTED_ARCHIVE_NAME)
        WindowsDebugArchiveValidator.verifySha256(archivePath, checksum)
    }

    private fun extractArtifactFiles(artifact: Path, archiveDestination: Path, manifestDestination: Path) {
        var archiveFound = false
        var manifestFound = false
        ZipInputStream(Files.newInputStream(artifact)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.replace('\\\\', '/')
                if (name.startsWith("/") || Regex("^[A-Za-z]:").containsMatchIn(name) ||
                    name.split('/').any { it == ".." }
                ) {
                    throw IllegalStateException("The GitHub Actions artifact contains an unsafe path.")
                }
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                val destination = when (name) {
                    EXPECTED_ARCHIVE_NAME -> {
                        if (archiveFound) throw IllegalStateException("The artifact contains duplicate installer archives.")
                        archiveFound = true
                        archiveDestination
                    }
                    EXPECTED_MANIFEST_NAME -> {
                        if (manifestFound) throw IllegalStateException("The artifact contains duplicate manifests.")
                        manifestFound = true
                        manifestDestination
                    }
                    else -> throw IllegalStateException("The GitHub Actions artifact contains an unexpected file.")
                }
                val limit = if (name == EXPECTED_MANIFEST_NAME) MAX_MANIFEST_BYTES else MAX_ARCHIVE_BYTES
                Files.newOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > limit) {
                            throw IllegalStateException("A file in the GitHub Actions artifact exceeded the size limit.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
                zip.closeEntry()
            }
        }
        if (!archiveFound || !manifestFound ||
            !Files.isRegularFile(archiveDestination) || Files.size(archiveDestination) == 0L ||
            !Files.isRegularFile(manifestDestination) || Files.size(manifestDestination) == 0L
        ) {
            throw IllegalStateException("The GitHub Actions artifact is missing the installer or manifest.")
        }
    }

    private fun downloadArchive(downloadUrl: String, destination: Path) {
        val connection = URL(downloadUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "GuessTheNumber-Windows-Debug-Updater")
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Installer download failed with HTTP ${connection.responseCode}.")
            }
            val declaredSize = connection.contentLengthLong
            if (declaredSize > MAX_ARTIFACT_BYTES) {
                throw IllegalStateException("The GitHub Actions artifact is too large.")
            }
            connection.inputStream.use { input ->
                Files.newOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_ARTIFACT_BYTES) {
                            throw IllegalStateException("The GitHub Actions artifact exceeded the size limit.")
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
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                scriptPath.toAbsolutePath().toString()
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
        const val ARTIFACT_DOWNLOAD_PREFIX =
            "https://api.github.com/repos/ne0gl1tch20/GuessTheNumber/actions/artifacts/"
        const val EXPECTED_ARCHIVE_NAME = "GuessTheNumber-Windows-Desktop.zip"
        const val EXPECTED_MANIFEST_NAME = "windows-debug-manifest.json"
        const val MAX_ARTIFACT_BYTES = 350L * 1024 * 1024
        const val MAX_ARCHIVE_BYTES = 300L * 1024 * 1024
        const val MAX_MANIFEST_BYTES = 64L * 1024
    }
}
