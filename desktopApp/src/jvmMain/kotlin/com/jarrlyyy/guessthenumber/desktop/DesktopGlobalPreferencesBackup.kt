package com.jarrlyyy.guessthenumber.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

@Serializable
private data class DesktopGlobalPreferencesPayload(
    val schemaVersion: Int,
    val navigationPosition: String,
    val profile: DesktopPlayerProfile?,
    val avatarEntry: String? = null
)

internal data class ParsedDesktopGlobalPreferences(
    val navigationPosition: DesktopNavigationPosition,
    val profile: DesktopPlayerProfile?,
    val avatarBytes: ByteArray?
)

internal object DesktopGlobalPreferencesBackup {
    private const val MAX_BUNDLE_BYTES = 6 * 1024 * 1024
    private const val MAX_AVATAR_BYTES = 4 * 1024 * 1024
    private const val PREFERENCES_ENTRY = "global-preferences.json"
    private const val AVATAR_ENTRY = "avatar.img"
    private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true }
    private val validAvatarIds = setOf(
        "star", "cat", "fox", "dog", "robot", "ghost", "dragon", "alien",
        "rocket", "crown", "gem", "controller", "moon", "sun", "flower", "lightning", "custom"
    )

    fun exportBundle(): ByteArray {
        val originalProfile = DesktopPlayerProfileStore.load()
        val avatarBytes = if (originalProfile?.avatarId == "custom") {
            val path = originalProfile.customAvatarPath?.takeIf(String::isNotBlank)
                ?: error("The custom profile avatar is missing.")
            Files.readAllBytes(Path.of(path)).also(::validateAvatar)
        } else null
        val profile = originalProfile?.copy(customAvatarPath = null)
        val payload = DesktopGlobalPreferencesPayload(
            schemaVersion = 1,
            navigationPosition = DesktopNavigationPosition.load().name,
            profile = profile,
            avatarEntry = if (avatarBytes != null) AVATAR_ENTRY else null
        )
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(PREFERENCES_ENTRY))
            zip.write(json.encodeToString(payload).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            if (avatarBytes != null) {
                zip.putNextEntry(ZipEntry(AVATAR_ENTRY))
                zip.write(avatarBytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray().also {
            require(it.size <= MAX_BUNDLE_BYTES) { "Global preferences backup exceeds the size limit." }
        }
    }

    fun parseBundle(bytes: ByteArray): ParsedDesktopGlobalPreferences {
        require(bytes.isNotEmpty() && bytes.size <= MAX_BUNDLE_BYTES) {
            "Global preferences backup is empty or too large."
        }
        val entries = linkedMapOf<String, ByteArray>()
        var totalBytes = 0
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && entry.name in setOf(PREFERENCES_ENTRY, AVATAR_ENTRY)) {
                    "Global preferences backup contains an unexpected entry."
                }
                require(entry.name !in entries) { "Global preferences backup contains duplicate entries." }
                val limit = if (entry.name == AVATAR_ENTRY) MAX_AVATAR_BYTES else 64 * 1024
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    totalBytes += count
                    require(totalBytes <= MAX_BUNDLE_BYTES && output.size() + count <= limit) {
                        "Global preferences backup entry exceeds the size limit."
                    }
                    output.write(buffer, 0, count)
                }
                entries[entry.name] = output.toByteArray()
                zip.closeEntry()
            }
        }
        val payloadBytes = entries[PREFERENCES_ENTRY]
            ?: error("Global preferences backup is missing its settings manifest.")
        val payload = json.decodeFromString<DesktopGlobalPreferencesPayload>(payloadBytes.toString(Charsets.UTF_8))
        require(payload.schemaVersion == 1) { "Unsupported global preferences backup version." }
        val navigation = runCatching { DesktopNavigationPosition.valueOf(payload.navigationPosition) }
            .getOrElse { error("Global preferences backup has an invalid navigation position.") }
        val profile = payload.profile
        profile?.let {
            require(it.profileId.isNotBlank() && it.profileId.length <= 64) { "Profile ID is invalid." }
            require(it.bio.length <= 120 && it.avatarId in validAvatarIds) { "Profile data is invalid." }
            require(!it.setupComplete || it.username.trim().length in 2..20) { "Profile username is invalid." }
            require(it.customAvatarPath == null) { "Backup profile must not contain an external avatar path." }
        }
        val avatarBytes = entries[AVATAR_ENTRY]
        require(payload.avatarEntry == (if (avatarBytes != null) AVATAR_ENTRY else null)) {
            "Global preferences avatar metadata does not match the archive."
        }
        require((profile?.avatarId == "custom") == (avatarBytes != null)) {
            "Custom profile avatar data is missing or unexpected."
        }
        avatarBytes?.let(::validateAvatar)
        return ParsedDesktopGlobalPreferences(navigation, profile, avatarBytes)
    }

    fun importBundle(bytes: ByteArray): Boolean {
        val parsed = runCatching { parseBundle(bytes) }.getOrNull() ?: return false
        val previousProfile = DesktopPlayerProfileStore.load()
        val previousNavigation = DesktopNavigationPosition.load()
        var importedAvatar: Path? = null
        return try {
            val restoredProfile = parsed.profile?.let { profile ->
                if (parsed.avatarBytes == null) profile
                else {
                    val directory = Path.of(System.getProperty("user.home"), ".guessthenumber", "profile")
                    Files.createDirectories(directory)
                    val destination = directory.resolve("avatar_import_${UUID.randomUUID()}.img")
                    Files.write(destination, parsed.avatarBytes)
                    importedAvatar = destination
                    profile.copy(customAvatarPath = destination.toAbsolutePath().toString())
                }
            }
            if (restoredProfile == null) DesktopPlayerProfileStore.clear()
            else DesktopPlayerProfileStore.save(restoredProfile)
            DesktopNavigationPosition.save(parsed.navigationPosition)
            true
        } catch (_: Exception) {
            runCatching {
                if (previousProfile == null) DesktopPlayerProfileStore.clear()
                else DesktopPlayerProfileStore.save(previousProfile)
                DesktopNavigationPosition.save(previousNavigation)
            }
            importedAvatar?.let { runCatching { Files.deleteIfExists(it) } }
            false
        }
    }

    private fun validateAvatar(bytes: ByteArray) {
        require(bytes.isNotEmpty() && bytes.size <= MAX_AVATAR_BYTES) { "Custom avatar is empty or too large." }
        val image = ImageIO.read(ByteArrayInputStream(bytes))
            ?: error("Custom avatar is not a supported image.")
        require(image.width in 1..4096 && image.height in 1..4096 &&
            image.width.toLong() * image.height.toLong() <= 16_000_000L
        ) { "Custom avatar dimensions exceed the supported limit." }
    }
}
