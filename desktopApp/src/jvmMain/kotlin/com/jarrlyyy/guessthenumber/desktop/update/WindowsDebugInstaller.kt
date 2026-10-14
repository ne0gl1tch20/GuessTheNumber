package com.jarrlyyy.guessthenumber.desktop.update

import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipInputStream

internal class WindowsDebugInstaller {
    fun downloadVerifyAndLaunch(update: WindowsDebugUpdate) {
        require(update.downloadUrl.startsWith("https://github.com/")) {
            "The debug installer URL is not from the expected GitHub host."
        }
        require(update.sha256.matches(Regex("[0-9a-f]{64}"))) {
            "The debug installer checksum is invalid."
        }

        val temporaryDirectory = Files.createTempDirectory("gtn-windows-debug-update-")
        try {
            val archivePath = temporaryDirectory.resolve("GuessTheNumber-Windows-Desktop.zip")
            downloadArchive(update.downloadUrl, archivePath)
            verifySha256(archivePath, update.sha256)
            val msiPath = temporaryDirectory.resolve("GuessTheNumber-Windows-Desktop.msi")
            extractExpectedMsi(archivePath, msiPath)
            launchInstallerAndCleanup(msiPath, temporaryDirectory)
        } catch (error: Exception) {
            deleteTree(temporaryDirectory)
            throw error
        }
    }

    private fun downloadArchive(downloadUrl: String, destination: Path) {
        val connection = URL(downloadUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "GuessTheNumber-Windows-Debug-Updater")
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Installer download failed with HTTP ${connection.responseCode}.")
            }
            val declaredSize = connection.contentLengthLong
            if (declaredSize > MAX_ARCHIVE_BYTES) {
                throw IllegalStateException("The debug installer archive is too large.")
            }
            connection.inputStream.use { input ->
                Files.newOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_ARCHIVE_BYTES) {
                            throw IllegalStateException("The debug installer archive exceeded the size limit.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun verifySha256(archive: Path, expected: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(archive).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expected, ignoreCase = true)) {
            throw IllegalStateException("The downloaded debug installer failed SHA-256 verification.")
        }
    }

    private fun extractExpectedMsi(archive: Path, destination: Path) {
        var found = false
        ZipInputStream(Files.newInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.replace('\\', '/')
                if (name.startsWith("/") || name.split('/').any { it == ".." }) {
                    throw IllegalStateException("The debug archive contains an unsafe path.")
                }
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                if (name != EXPECTED_MSI_NAME || found) {
                    throw IllegalStateException("The debug archive contains an unexpected file.")
                }
                Files.newOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_MSI_BYTES) {
                            throw IllegalStateException("The MSI exceeded the extraction size limit.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
                found = true
                zip.closeEntry()
            }
        }
        if (!found || !Files.isRegularFile(destination) || Files.size(destination) == 0L) {
            throw IllegalStateException("The expected Windows MSI was not found in the debug archive.")
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
                $installer = Start-Process -FilePath 'msiexec.exe' -ArgumentList @('/i', $msiPath) -Wait -PassThru
                exit $installer.ExitCode
            } finally {
                Start-Sleep -Seconds 2
                Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force -ErrorAction SilentlyContinue
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
        const val EXPECTED_MSI_NAME = "GuessTheNumber-Windows-Desktop.msi"
        const val MAX_ARCHIVE_BYTES = 300L * 1024 * 1024
        const val MAX_MSI_BYTES = 500L * 1024 * 1024
    }
}
