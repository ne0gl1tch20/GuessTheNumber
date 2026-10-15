package com.jarrlyyy.guessthenumber.desktop.persistence

import com.jarrlyyy.guessthenumber.domain.model.BigNumber
import com.jarrlyyy.guessthenumber.domain.model.Difficulty
import com.jarrlyyy.guessthenumber.domain.model.GameState
import com.jarrlyyy.guessthenumber.shared.domain.SaveProfileRules
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

private const val MAX_SAVE_SLOTS = 10

internal interface DesktopSaveFileOps {
    fun createDirectories(path: Path)
    fun read(path: Path): String
    fun exists(path: Path): Boolean
    fun write(path: Path, content: String)
    fun moveReplace(source: Path, target: Path)
}

internal object DefaultDesktopSaveFileOps : DesktopSaveFileOps {
    override fun createDirectories(path: Path) {
        Files.createDirectories(path)
    }

    override fun read(path: Path): String = Files.readString(path)

    override fun exists(path: Path): Boolean = Files.exists(path)

    override fun write(path: Path, content: String) {
        Files.writeString(
            path,
            content,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
    }

    override fun moveReplace(source: Path, target: Path) {
        try {
            Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

/**
 * Windows-native persistence for the shared GameState JSON contract.
 * Android DataStore stays on Android; this adapter owns only JVM desktop files.
 */
class DesktopSaveStore internal constructor(
    private val rootDirectory: Path,
    private val fileOps: DesktopSaveFileOps = DefaultDesktopSaveFileOps,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {
    constructor(rootDirectory: Path) : this(rootDirectory, DefaultDesktopSaveFileOps)

    suspend fun load(slot: Int): GameState? {
        val normalizedSlot = normalizeSlot(slot)
        val primary = primaryPath(normalizedSlot)
        val backup = backupPath(normalizedSlot)
        val primaryState = readValid(primary)
        if (primaryState != null) return primaryState

        val backupState = readValid(backup) ?: return null
        val backupJson = json.encodeToString(backupState)
        runCatching {
            writeAtomically(primary, backupJson)
        }
        return backupState
    }

    suspend fun save(slot: Int, state: GameState): Boolean {
        val normalizedSlot = normalizeSlot(slot)
        if (!validate(state)) return false

        return runCatching {
            fileOps.createDirectories(rootDirectory)
            val primary = primaryPath(normalizedSlot)
            val backup = backupPath(normalizedSlot)
            val currentPrimary = readValidJson(primary)

            if (currentPrimary != null) {
                writeAtomically(backup, currentPrimary)
            }
            writeAtomically(primary, json.encodeToString(state))
        }.isSuccess
    }

    fun validate(state: GameState): Boolean =
        Difficulty.isValid(state.difficultyId) &&
            state.money.value >= BigNumber.ZERO.value &&
            state.prestige.value >= BigNumber.ZERO.value &&
            state.ultra.value >= BigNumber.ZERO.value &&
            state.nebula.value >= BigNumber.ZERO.value &&
            state.currentRangeMin >= 1L &&
            state.currentRangeMin <= state.currentRangeMax &&
            state.targetNumber in state.currentRangeMin..state.currentRangeMax &&
            state.attempts >= 0L &&
            state.correctGuesses >= 0L &&
            state.streak >= 0 &&
            state.bestStreak >= 0 &&
            state.statistics.totalGuesses >= 0L &&
            state.statistics.correctGuesses >= 0L &&
            state.statistics.failedGuesses >= 0L &&
            state.statistics.moneyEarned.value >= BigNumber.ZERO.value &&
            state.statistics.moneySpent.value >= BigNumber.ZERO.value &&
            state.statistics.prestigesCount >= 0L &&
            state.statistics.ultrasCount >= 0L &&
            state.statistics.nebulaEarned >= 0L &&
            state.statistics.playtimeSeconds >= 0L &&
            state.statistics.arcadePlayed >= 0L &&
            state.statistics.randomEventsTriggered >= 0L &&
            state.statistics.arcadeBestScores.values.all { it >= 0 } &&
            state.upgradeLevels.values.all { it >= 0 } &&
            state.prestigeUpgradeLevels.values.all { it >= 0 } &&
            state.ultraUpgradeLevels.values.all { it >= 0 } &&
            state.liveOpsClaims.values.all { it >= 0 } &&
            state.permanentEventBoosts >= 0 &&
            state.dailyQuestGuesses >= 0 &&
            state.dailyQuestCorrectGuesses >= 0 &&
            state.dailyQuestBestStreak >= 0 &&
            state.dailyQuestUpgradePurchases >= 0 &&
            state.lootCommonChests >= 0 &&
            state.lootRareChests >= 0 &&
            state.lootEpicChests >= 0 &&
            state.lootDropsFound >= 0L &&
            state.lootChestsOpened >= 0L &&
            state.endlessRiftTier >= 0 &&
            state.endlessRiftBestTier >= 0 &&
            state.lastSaveTimestamp > 0L &&
            state.profileName.length <= 24 &&
            SaveProfileRules.isValidIcon(state.profileIconId)

    private fun readValid(path: Path): GameState? =
        readValidJson(path)?.let { raw -> runCatching { json.decodeFromString<GameState>(raw) }.getOrNull() }

    private fun readValidJson(path: Path): String? {
        if (!fileOps.exists(path)) return null
        val raw = runCatching { fileOps.read(path) }.getOrNull() ?: return null
        return runCatching {
            val state = json.decodeFromString<GameState>(raw)
            raw.takeIf { validate(state) }
        }.getOrNull()
    }

    private fun writeAtomically(target: Path, content: String) {
        fileOps.createDirectories(rootDirectory)
        val temp = target.resolveSibling("${target.fileName}.tmp")
        fileOps.write(temp, content)
        fileOps.moveReplace(temp, target)
    }

    private fun primaryPath(slot: Int): Path = rootDirectory.resolve("slot-${slot}.json")

    private fun backupPath(slot: Int): Path = rootDirectory.resolve("slot-${slot}.backup.json")

    private fun normalizeSlot(slot: Int): Int = slot.coerceIn(1, MAX_SAVE_SLOTS)
}

fun defaultDesktopSaveDirectory(): Path {
    val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
    val base = appData?.let(Path::of) ?: Path.of(System.getProperty("user.home"), ".config")
    return base.resolve("GuessTheNumber").resolve("saves")
}
