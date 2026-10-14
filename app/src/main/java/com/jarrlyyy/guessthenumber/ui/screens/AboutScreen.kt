package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.jarrlyyy.guessthenumber.data.repository.UpdateChannelChecker
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.BuildConfig

data class OpenSourceLibrary(val name: String, val license: String, val author: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: com.jarrlyyy.guessthenumber.ui.viewmodel.GameViewModel,
    onBack: () -> Unit
) {
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE
    val locale = viewModel.localeManager
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checkingUpdates by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<UpdateChannelChecker.AvailableUpdate?>(null) }
    val channelLabel = if (BuildConfig.UPDATE_CHANNEL == "debug") {
        locale.getString("update_channel_debug", "Debug channel")
    } else {
        locale.getString("update_channel_release", "Release channel")
    }

    val libraries = listOf(
        OpenSourceLibrary("Jetpack Compose", "Apache 2.0", "Google / Android Open Source Project"),
        OpenSourceLibrary("Material 3", "Apache 2.0", "Google"),
        OpenSourceLibrary("Kotlinx Serialization", "Apache 2.0", "JetBrains"),
        OpenSourceLibrary("Kotlinx Coroutines", "Apache 2.0", "JetBrains"),
        OpenSourceLibrary("DataStore Preferences", "Apache 2.0", "Google"),
        OpenSourceLibrary("Navigation Compose", "Apache 2.0", "Google"),
        OpenSourceLibrary("LuaJ", "MIT", "LuaJ project; James Roseborough and Ian Farmer")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(locale.getString("about_title", "About & Open Source Licenses")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = locale.getString("back", "Back")
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            locale.getString(
                                "about_header",
                                "Gamified Guess the Number Simulator"
                            ),
                            fontSize = 20.sp,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            locale.getString(
                                "about_version_format",
                                "Version: %s (%d)",
                                versionName,
                                versionCode
                            ),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            locale.getString("update_channel_label", "Update channel: %s", channelLabel),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    checkingUpdates = true
                                    updateMessage = null
                                    availableUpdate = null
                                    when (val result = UpdateChannelChecker.check()) {
                                        is UpdateChannelChecker.Result.Available -> {
                                            availableUpdate = result.update
                                            updateMessage = locale.getString("update_available", "An update is available.")
                                        }
                                        UpdateChannelChecker.Result.UpToDate -> {
                                            updateMessage = locale.getString("update_up_to_date", "You're up to date.")
                                        }
                                        UpdateChannelChecker.Result.Unavailable -> {
                                            updateMessage = locale.getString("update_unavailable", "Couldn't check for updates. Try again later.")
                                        }
                                    }
                                    checkingUpdates = false
                                }
                            },
                            enabled = !checkingUpdates,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (checkingUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(locale.getString("update_checking", "Checking for updates…"))
                            } else {
                                Text(locale.getString("update_check", "Check for updates"))
                            }
                        }
                        updateMessage?.let { message ->
                            Text(message, style = MaterialTheme.typography.bodyMedium)
                        }
                        availableUpdate?.let { update ->
                            Text(update.title, style = MaterialTheme.typography.titleSmall)
                            if (update.details.isNotBlank()) {
                                Text(update.details, style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.url)))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(locale.getString("update_open", "Open update page"))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            locale.getString(
                                "about_description",
                                "A polished Android incremental game combining classic guessing mechanics with deep prestige/ultra progression, anti-cheat protection, offline progression! Made with love by @jarrlyyy!"
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    locale.getString(
                        "about_licenses_title",
                        "Open Source Licenses (Android Standards)"
                    ),
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(libraries) { lib ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            lib.name,
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            locale.getString(
                                "about_library_author_format",
                                "Author: %s",
                                lib.author
                            ),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            locale.getString(
                                "about_library_license_format",
                                "License: %s",
                                lib.license
                            ),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
