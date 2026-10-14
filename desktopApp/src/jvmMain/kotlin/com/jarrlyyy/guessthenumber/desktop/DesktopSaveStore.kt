package com.jarrlyyy.guessthenumber.desktop

import com.jarrlyyy.guessthenumber.shared.domain.GameState
import com.jarrlyyy.guessthenumber.shared.domain.GameStateCodec
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Windows/JVM persistence adapter. The shared codec owns the JSON contract;
 * this adapter owns desktop storage and keeps one last-known-good backup.
 */
internal class DesktopSaveStore(
    private val directory: Path = Paths.get(
        System.getProperty("user.home"),
        ".guessthenumber"
    )
) {
    private val currentFile: Path get() = directory.resolve("save.json")
    private val backupFile: Path get() = directory.resolve("save.backup.json")

    fun loadOrDefault(): GameState {
        val current = readState(currentFile)
        if (current != null) return current

        val backup = readState(backupFile)
        if (backup != null) {
            save(backup)
            return backup
        }

        return GameState()
    }

    fun save(state: GameState): Boolean = runCatching {
        Files.createDirectories(directory)
        val previousRaw = readTextOrNull(currentFile)
        if (GameStateCodec.decodeOrNull(previousRaw) != null) {
            writeAtomically(backupFile, previousRaw!!)
        }
        val timestamped = state.copy(lastSaveTimestamp = System.currentTimeMillis())
        writeAtomically(currentFile, GameStateCodec.encode(timestamped))
    }.isSuccess

    private fun readState(path: Path): GameState? =
        GameStateCodec.decodeOrNull(readTextOrNull(path))

    private fun readTextOrNull(path: Path): String? =
        runCatching { if (Files.isRegularFile(path)) Files.readString(path) else null }.getOrNull()

    private fun writeAtomically(destination: Path, content: String) {
        val temporary = destination.resolveSibling("${destination.fileName}.tmp")
        Files.writeString(
            temporary,
            content,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
        try {
            Files.move(
                temporary,
                destination,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
