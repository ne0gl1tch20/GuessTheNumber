package com.jarrlyyy.guessthenumber.desktop.update

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream

internal object WindowsDebugArtifactValidator {
    const val EXPECTED_ARCHIVE_NAME = "GuessTheNumber-Windows-Desktop.zip"
    const val EXPECTED_MANIFEST_NAME = "windows-debug-manifest.json"
    private const val MAX_ARCHIVE_BYTES = 350L * 1024 * 1024
    private const val MAX_MANIFEST_BYTES = 64L * 1024

    fun extractArtifactFiles(artifact: Path, archive: Path, manifest: Path) {
        var archiveFound = false
        var manifestFound = false
        ZipInputStream(Files.newInputStream(artifact)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.replace(92.toChar(), '/')
                if (name.startsWith("/") || Regex("^[A-Za-z]:").containsMatchIn(name) ||
                    name.split('/').any { it == ".." }
                ) throw IllegalStateException("The Actions artifact contains an unsafe path.")
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                val destination = when (name) {
                    EXPECTED_ARCHIVE_NAME -> {
                        if (archiveFound) throw IllegalStateException("The Actions artifact contains duplicate installer archives.")
                        archiveFound = true
                        archive
                    }
                    EXPECTED_MANIFEST_NAME -> {
                        if (manifestFound) throw IllegalStateException("The Actions artifact contains duplicate manifests.")
                        manifestFound = true
                        manifest
                    }
                    else -> throw IllegalStateException("The Actions artifact contains an unexpected file.")
                }
                val maximum = if (name == EXPECTED_ARCHIVE_NAME) MAX_ARCHIVE_BYTES else MAX_MANIFEST_BYTES
                writeEntry(zip, destination, maximum)
                zip.closeEntry()
            }
        }
        if (!archiveFound || !manifestFound || Files.size(archive) == 0L || Files.size(manifest) == 0L) {
            throw IllegalStateException("The Actions artifact is missing its installer archive or manifest.")
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
}
