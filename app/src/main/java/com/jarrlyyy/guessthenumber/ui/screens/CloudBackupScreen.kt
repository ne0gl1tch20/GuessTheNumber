package com.jarrlyyy.guessthenumber.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.domain.model.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBackupScreen(
    state: GameState,
    onExportSave: ((String) -> Unit) -> Unit,
    onImportSave: (String, (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var importJson by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Save Export / Import & Cloud Backup") },
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cloud Backup & Transfer", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                        Text("Export your encrypted save string to back up your progress across devices or securely restore from a backup string.", fontSize = 14.sp)
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onExportSave { json ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("GuessTheNumberBackup", json)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Encrypted save string copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export & Copy Backup String")
                }
            }

            item {
                OutlinedTextField(
                    value = importJson,
                    onValueChange = { importJson = it },
                    label = { Text("Paste Backup String") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = {
                        onImportSave(importJson) { success ->
                            if (success) {
                                Toast.makeText(context, "Cloud backup restored successfully!", Toast.LENGTH_SHORT).show()
                                importJson = ""
                            } else {
                                Toast.makeText(context, "Invalid backup string!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore Backup String")
                }
            }
        }
    }
}
