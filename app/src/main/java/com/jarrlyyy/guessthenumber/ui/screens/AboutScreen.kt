package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.jarrlyyy.guessthenumber.data.repository.UpdateChannelChecker
import com.jarrlyyy.guessthenumber.data.repository.ApkUpdateInstaller
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.BuildConfig

data class OpenSourceLibrary(val name: String, val license: String, val author: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(viewModel: com.jarrlyyy.guessthenumber.ui.viewmodel.GameViewModel, onBack: () -> Unit) {
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE
    val locale = viewModel.localeManager
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var checkingUpdates by remember { mutableStateOf(false) }
    var installingUpdate by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<UpdateChannelChecker.AvailableUpdate?>(null) }
    var updateErrorLogs by remember { mutableStateOf<String?>(null) }
    val channelLabel = if (BuildConfig.UPDATE_CHANNEL == "debug") locale.getString("update_channel_debug", "Debug channel") else locale.getString("update_channel_release", "Release channel")
    val libraries = listOf(
        OpenSourceLibrary("Jetpack Compose", "Apache 2.0", "Google / Android Open Source Project"),
        OpenSourceLibrary("Material 3", "Apache 2.0", "Google"), OpenSourceLibrary("Kotlinx Serialization", "Apache 2.0", "JetBrains"),
        OpenSourceLibrary("Kotlinx Coroutines", "Apache 2.0", "JetBrains"), OpenSourceLibrary("DataStore Preferences", "Apache 2.0", "Google"),
        OpenSourceLibrary("Navigation Compose", "Apache 2.0", "Google"), OpenSourceLibrary("LuaJ", "MIT", "LuaJ project; James Roseborough and Ian Farmer")
    )
    Scaffold(topBar = {
        TopAppBar(title = { Text(locale.getString("about_title", "About & Open Source Licenses")) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = locale.getString("back", "Back")) }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(locale.getString("about_header", "Gamified Guess the Number Simulator"), fontSize = 20.sp, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(locale.getString("about_version_format", "Version: %s (%d)", versionName, versionCode), fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(locale.getString("update_channel_label", "Update channel: %s", channelLabel), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            scope.launch {
                                checkingUpdates = true; updateMessage = null; availableUpdate = null; updateErrorLogs = null
                                when (val result = UpdateChannelChecker.check()) {
                                    is UpdateChannelChecker.Result.Available -> { availableUpdate = result.update; updateMessage = locale.getString("update_available", "An update is available.") }
                                    UpdateChannelChecker.Result.UpToDate -> updateMessage = locale.getString("update_up_to_date", "You're up to date.")
                                    is UpdateChannelChecker.Result.Unavailable -> {
                                        updateMessage = locale.getString("update_unavailable", "Couldn't check for updates. Try again later.")
                                        updateErrorLogs = result.diagnostics
                                    }
                                }
                                checkingUpdates = false
                            }
                        }, enabled = !checkingUpdates && !installingUpdate, modifier = Modifier.fillMaxWidth()) {
                            if (checkingUpdates) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text(locale.getString("update_checking", "Checking for updates…")) } else Text(locale.getString("update_check", "Check for updates"))
                        }
                        updateMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        updateErrorLogs?.let { logs ->
                            OutlinedButton(
                                onClick = { clipboardManager.setText(AnnotatedString(logs)) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(locale.getString("update_copy_error_logs", "Copy update error logs")) }
                            Text(locale.getString("update_error_logs_description", "These diagnostic logs include the failure reason, app version and time. They do not include account tokens."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        availableUpdate?.let { update ->
                            Text(update.title, style = MaterialTheme.typography.titleSmall)
                            if (update.details.isNotBlank()) Text(update.details, style = MaterialTheme.typography.bodySmall)
                            Button(onClick = {
                                if (!context.packageManager.canRequestPackageInstalls()) {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                                    updateMessage = locale.getString("update_permission_required", "Allow this app to install updates, then return and tap Install again.")
                                } else {
                                    scope.launch {
                                        installingUpdate = true
                                        updateMessage = locale.getString("update_downloading", "Downloading and preparing update…")
                                        val result = withContext(Dispatchers.IO) { ApkUpdateInstaller(context).install(update.downloadUrl) }
                                        installingUpdate = false
                                        updateMessage = result.fold(
                                            {
                                                updateErrorLogs = null
                                                locale.getString("update_install_started", "Installation started. Confirm the Android installer prompt.")
                                            },
                                            { error ->
                                                updateErrorLogs = buildString {
                                                    appendLine("Update installation failed")
                                                    appendLine("App version: $versionName ($versionCode)")
                                                    appendLine("Channel: ${BuildConfig.UPDATE_CHANNEL}")
                                                    appendLine("Failure: ${error.javaClass.name}")
                                                    appendLine("Message: ${error.message ?: "(no message)"}")
                                                    error.stackTraceToString().take(8000).let { appendLine(); append(it) }
                                                }
                                                locale.getString("update_install_failed", "Update failed: %s", error.message ?: "unknown error")
                                            }
                                        )
                                    }
                                }
                            }, enabled = !installingUpdate, modifier = Modifier.fillMaxWidth()) {
                                if (installingUpdate) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(locale.getString("update_install", "Install update"))
                            }
                            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.url))) }, modifier = Modifier.fillMaxWidth()) { Text(locale.getString("update_open", "Open update page")) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(locale.getString("about_description", "A polished Android incremental game combining classic guessing mechanics with deep prestige/ultra progression, anti-cheat protection, offline progression! Made with love by @jarrlyyy!"))
                    }
                }
            }
            item { Text(locale.getString("about_licenses_title", "Open Source Licenses (Android Standards)"), fontSize = 18.sp, style = MaterialTheme.typography.titleMedium) }
            items(libraries) { lib ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(lib.name, fontSize = 16.sp, style = MaterialTheme.typography.titleSmall)
                        Text(locale.getString("about_library_author_format", "Author: %s", lib.author), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(locale.getString("about_library_license_format", "License: %s", lib.license), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
