package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.BuildConfig

data class OpenSourceLibrary(val name: String, val license: String, val author: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE

    val libraries = listOf(
        OpenSourceLibrary("Jetpack Compose", "Apache 2.0", "Google / Android Open Source Project"),
        OpenSourceLibrary("Material 3", "Apache 2.0", "Google"),
        OpenSourceLibrary("Kotlinx Serialization", "Apache 2.0", "JetBrains"),
        OpenSourceLibrary("Kotlinx Coroutines", "Apache 2.0", "JetBrains"),
        OpenSourceLibrary("DataStore Preferences", "Apache 2.0", "Google"),
        OpenSourceLibrary("Navigation Compose", "Apache 2.0", "Google")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About & Open Source Licenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        Text("Gamified Guess the Number Simulator", fontSize = 20.sp, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Version: $versionName ($versionCode)", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("A polished Android incremental game combining classic guessing mechanics with deep prestige/ultra progression, anti-cheat protection, offline progression! Made with love by @jarrlyyy!")
                    }
                }
            }

            item {
                Text("Open Source Licenses (Android Standards)", fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
            }

            items(libraries) { lib ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(lib.name, fontSize = 16.sp, style = MaterialTheme.typography.titleSmall)
                        Text("Author: ${lib.author}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("License: ${lib.license}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
