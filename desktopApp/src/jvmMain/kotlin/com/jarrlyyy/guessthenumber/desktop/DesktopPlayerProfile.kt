package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.prefs.Preferences
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Serializable
data class DesktopPlayerProfile(
    val username: String = "",
    val bio: String = "",
    val avatarId: String = "star",
    val customAvatarPath: String? = null,
    val setupComplete: Boolean = false,
    val profileId: String = UUID.randomUUID().toString()
)

internal object DesktopPlayerProfileStore {
    private val preferences = Preferences.userNodeForPackage(DesktopPlayerProfileStore::class.java)
        .node("player-profile")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val profileKey = "profile_json"

    fun load(): DesktopPlayerProfile? = runCatching {
        preferences.get(profileKey, null)?.let { json.decodeFromString<DesktopPlayerProfile>(it) }
    }.getOrNull()

    fun save(profile: DesktopPlayerProfile) {
        preferences.put(profileKey, json.encodeToString(profile))
        preferences.flush()
    }

    fun chooseAndImportAvatar(): String? = runCatching {
        val chooser = JFileChooser().apply {
            dialogTitle = "Choose a profile picture"
            fileFilter = FileNameExtensionFilter("Image files (PNG, JPG, GIF, BMP)", "png", "jpg", "jpeg", "gif", "bmp")
            isAcceptAllFileFilterUsed = false
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return@runCatching null
        val selected = chooser.selectedFile ?: return@runCatching null
        if (ImageIO.read(selected) == null) return@runCatching null
        val profileDirectory = File(System.getProperty("user.home"), ".guessthenumber/profile").apply { mkdirs() }
        val extension = selected.extension.lowercase().takeIf { it in setOf("png", "jpg", "jpeg", "gif", "bmp") } ?: "png"
        val destination = File(profileDirectory, "avatar_${UUID.randomUUID()}.$extension")
        Files.copy(selected.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        destination.absolutePath
    }.getOrNull()
}

private val desktopAvatarPresets = listOf(
    "star" to "⭐", "cat" to "🐱", "fox" to "🦊", "dog" to "🐶",
    "robot" to "🤖", "ghost" to "👻", "dragon" to "🐉", "alien" to "👽",
    "rocket" to "🚀", "crown" to "👑", "gem" to "💎", "controller" to "🎮",
    "moon" to "🌙", "sun" to "☀️", "flower" to "🌸", "lightning" to "⚡"
)

internal fun desktopAvatarEmoji(id: String): String =
    desktopAvatarPresets.firstOrNull { it.first == id }?.second ?: "⭐"

@Composable
internal fun DesktopPlayerProfileButton(
    profile: DesktopPlayerProfile,
    onClick: () -> Unit
) {
    val avatarImage = remember(profile.customAvatarPath, profile.avatarId) {
        if (profile.avatarId == "custom" && !profile.customAvatarPath.isNullOrBlank()) {
            runCatching {
                SkiaImage.makeFromEncoded(File(profile.customAvatarPath.orEmpty()).readBytes())
                    .toComposeImageBitmap()
            }.getOrNull()
        } else null
    }
    TextButton(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (avatarImage != null) {
                Image(avatarImage, contentDescription = "Player profile picture", modifier = Modifier.size(28.dp))
            } else {
                Text(desktopAvatarEmoji(profile.avatarId))
            }
            Text("${profile.username} · Profile")
        }
    }
}

@Composable
internal fun DesktopPlayerProfileScreen(
    profile: DesktopPlayerProfile?,
    onSave: (DesktopPlayerProfile) -> Unit,
    onCancel: () -> Unit
) {
    var username by remember(profile?.profileId) { mutableStateOf(profile?.username.orEmpty()) }
    var bio by remember(profile?.profileId) { mutableStateOf(profile?.bio.orEmpty()) }
    var avatarId by remember(profile?.profileId) { mutableStateOf(profile?.avatarId ?: "star") }
    var customAvatarPath by remember(profile?.profileId) { mutableStateOf(profile?.customAvatarPath) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    val firstLaunch = profile?.setupComplete != true

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Card(modifier = Modifier.fillMaxWidth().size(width = 680.dp, height = 620.dp)) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (firstLaunch) "Create your player profile" else "Player profile",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        if (firstLaunch) "Choose a username before opening your save slots. This profile stays on this Windows PC."
                        else "Your offline profile is stored locally and does not affect game save slots.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val avatarImage = remember(customAvatarPath, avatarId) {
                            if (avatarId == "custom" && !customAvatarPath.isNullOrBlank()) {
                                runCatching {
                                    SkiaImage.makeFromEncoded(File(customAvatarPath!!).readBytes())
                                        .toComposeImageBitmap()
                                }.getOrNull()
                            } else null
                        }
                        if (avatarImage != null) {
                            Image(avatarImage, contentDescription = "Custom profile picture", modifier = Modifier.size(64.dp))
                        } else {
                            Text(desktopAvatarEmoji(avatarId), style = MaterialTheme.typography.displayMedium)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Profile picture", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = {
                                val chosen = DesktopPlayerProfileStore.chooseAndImportAvatar()
                                if (chosen != null) {
                                    customAvatarPath = chosen
                                    avatarId = "custom"
                                }
                            }) { Text("Choose image…") }
                        }
                    }
                    Text("Or pick a preset", style = MaterialTheme.typography.titleMedium)
                    desktopAvatarPresets.chunked(4).forEach { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            group.forEach { (id, emoji) ->
                                val selected = avatarId == id
                                OutlinedButton(
                                    onClick = { avatarId = id },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (selected) "✓ $emoji" else emoji)
                                }
                            }
                            repeat(4 - group.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.take(20); validationMessage = null },
                        label = { Text("Username") },
                        supportingText = { Text("2–20 characters") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it.take(120) },
                        label = { Text("Bio (optional)") },
                        supportingText = { Text("${bio.length}/120") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                    validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            val cleanName = username.trim()
                            if (cleanName.length !in 2..20) {
                                validationMessage = "Username must be 2–20 characters."
                            } else {
                                onSave(
                                    DesktopPlayerProfile(
                                        username = cleanName,
                                        bio = bio.trim(),
                                        avatarId = avatarId,
                                        customAvatarPath = customAvatarPath,
                                        setupComplete = true,
                                        profileId = profile?.profileId ?: UUID.randomUUID().toString()
                                    )
                                )
                            }
                        }) { Text("Save profile") }
                        if (!firstLaunch) {
                            TextButton(onClick = onCancel) { Text("Cancel") }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}
