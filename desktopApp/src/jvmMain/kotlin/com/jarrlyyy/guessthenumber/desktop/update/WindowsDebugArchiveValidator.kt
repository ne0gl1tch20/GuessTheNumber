package com.jarrlyyy.guessthenumber.desktop.update

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipInputStream

internal object WindowsDebugArchiveValidator {
    const val EXPECTED_MSI_NAME = "GuessTheNumber-Windows-Desktop.msi"
    private const val MAX_MSI_BYTES = 500L * 1024 * 1024

    fun verifySha256(archive: Path, expected: String) {
        require(expected.matches(Regex("[0-9a-fA-F]{64}"))) {
            "The debug installer checksum is invalid."
        }
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

    fun extractExpectedMsi(archive: Path, destination: Path) {
        var found = false
        ZipInputStream(Files.newInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.replace('\\', '/')
                if (name.startsWith("/") || Regex("^[A-Za-z]:").containsMatchIn(name) ||
                    name.split('/').any { it == ".." }
                ) {
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
}
