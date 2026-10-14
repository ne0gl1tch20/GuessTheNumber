package com.jarrlyyy.guessthenumber.desktop.update

import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

internal class WindowsDebugInstaller {
    fun downloadVerifyAndLaunch(update: WindowsDebugUpdate) {
        require(update.downloadUrl.startsWith("https://github.com/")) {
            "The debug installer URL is not from the expected GitHub host."
        }
        require(update.sha256.matches(Regex("[0-9a-fA-F]{64}"))) {
            "The debug installer checksum is invalid."
        }

        val temporaryDirectory = Files.createTempDirectory("gtn-windows-debug-update-")
        try {
            val archivePath = temporaryDirectory.resolve("GuessTheNumber-Windows-Desktop.zip")
            downloadArchive(update.downloadUrl, archivePath)
            WindowsDebugArchiveValidator.verifySha256(archivePath, update.sha256)
            val msiPath = temporaryDirectory.resolve(WindowsDebugArchiveValidator.EXPECTED_MSI_NAME)
            WindowsDebugArchiveValidator.extractExpectedMsi(archivePath, msiPath)
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
        const val MAX_ARCHIVE_BYTES = 300L * 1024 * 1024
    }
}
