package com.jarrlyyy.guessthenumber.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val Context.playerProfileDataStore by preferencesDataStore(name = "player_profile")

@Serializable
data class PlayerProfile(
    val username: String = "",
    val bio: String = "",
    val avatarId: String = "star",
    val customAvatarPath: String? = null,
    val setupComplete: Boolean = false,
    val profileId: String = java.util.UUID.randomUUID().toString()
)

class PlayerProfileStore(private val context: Context) {
    private val key = stringPreferencesKey("player_profile_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val profile: Flow<PlayerProfile?> = context.playerProfileDataStore.data.map { prefs ->
        prefs[key]?.let { raw -> runCatching { json.decodeFromString<PlayerProfile>(raw) }.getOrNull() }
    }

    suspend fun save(profile: PlayerProfile) {
        context.playerProfileDataStore.edit { it[key] = json.encodeToString(profile) }
    }

    fun importCustomAvatar(uri: Uri): String? = runCatching {
        val destination = File(context.filesDir, "player_profile_avatar_${java.util.UUID.randomUUID()}.img")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        destination.absolutePath
    }.getOrNull()
}

internal fun loadPlayerAvatar(path: String?): androidx.compose.ui.graphics.ImageBitmap? {
    if (path.isNullOrBlank()) return null
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
    }.getOrNull()
}

private val avatarPresets = listOf(
    "star" to "⭐", "cat" to "🐱", "fox" to "🦊", "dog" to "🐶",
    "robot" to "🤖", "ghost" to "👻", "dragon" to "🐉", "alien" to "👽",
    "rocket" to "🚀", "crown" to "👑", "gem" to "💎", "controller" to "🎮",
    "moon" to "🌙", "sun" to "☀️", "flower" to "🌸", "lightning" to "⚡"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    profile: PlayerProfile?,
    firstLaunch: Boolean,
    store: PlayerProfileStore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by remember(profile?.profileId) { mutableStateOf(profile?.username.orEmpty()) }
    var bio by remember(profile?.profileId) { mutableStateOf(profile?.bio.orEmpty()) }
    var avatarId by remember(profile?.profileId) { mutableStateOf(profile?.avatarId ?: "star") }
    var customAvatarPath by remember(profile?.profileId) { mutableStateOf(profile?.customAvatarPath) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val path = store.importCustomAvatar(uri)
            if (path != null) {
                customAvatarPath = path
                avatarId = "custom"
            } else error = "Couldn't load that picture. Try another image."
        }
    }
    val customBitmap = remember(customAvatarPath) { loadPlayerAvatar(customAvatarPath) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (firstLaunch) "Create your profile" else "Player profile") },
                navigationIcon = {
                    if (!firstLaunch) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    }
                }
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                if (firstLaunch) "Make this game yours." else "Your identity stays on this device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(112.dp).clip(CircleShape),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (customBitmap != null && avatarId == "custom") {
                            Image(customBitmap, contentDescription = "Custom profile picture", modifier = Modifier.fillMaxSize().clip(CircleShape))
                        } else {
                            Text(avatarPresets.firstOrNull { it.first == avatarId }?.second ?: "⭐", style = MaterialTheme.typography.displayLarge)
                        }
                    }
                }
                FilledTonalIconButton(onClick = { imagePicker.launch("image/*") }) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Choose a custom picture")
                }
            }
            Text("Choose an avatar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().heightIn(max = 170.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(avatarPresets) { (id, emoji) ->
                    FilterChip(
                        selected = avatarId == id,
                        onClick = { avatarId = id; customAvatarPath = null },
                        label = { Text(emoji, style = MaterialTheme.typography.titleLarge) },
                        leadingIcon = if (avatarId == id) ({ Icon(Icons.Default.Check, contentDescription = null) }) else null
                    )
                }
            }
            OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose a picture from this device")
            }
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.take(20); error = null },
                label = { Text("Username") },
                supportingText = { Text("2–20 characters") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(120) },
                label = { Text("Bio") },
                placeholder = { Text("A little about you (optional)") },
                supportingText = { Text("${bio.length}/120") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.weight(1f, fill = false))
            Button(
                onClick = {
                    val cleanName = username.trim()
                    if (cleanName.length !in 2..20) {
                        error = "Username must be 2–20 characters."
                    } else if (cleanName.any { it.isISOControl() }) {
                        error = "Please choose a different username."
                    } else {
                        saving = true
                        val updated = (profile ?: PlayerProfile()).copy(
                            username = cleanName,
                            bio = bio.trim(),
                            avatarId = avatarId,
                            customAvatarPath = if (avatarId == "custom") customAvatarPath else null,
                            setupComplete = true
                        )
                        scope.launch {
                            runCatching { store.save(updated) }
                                .onFailure { error = "Couldn't save your profile. Please try again." }
                            saving = false
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (saving) "Saving…" else if (firstLaunch) "Create profile & continue" else "Save profile")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
