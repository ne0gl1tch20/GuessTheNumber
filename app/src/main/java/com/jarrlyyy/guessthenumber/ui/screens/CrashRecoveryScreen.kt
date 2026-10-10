package com.jarrlyyy.guessthenumber.ui.screens

import com.jarrlyyy.guessthenumber.data.repository.LocaleManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.animateContentSize

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarrlyyy.guessthenumber.data.crash.AppErrorHandler

@Composable
fun CrashRecoveryScreen(locale: LocaleManager, onDismiss: () -> Unit, reducedMotion: Boolean = false) {
    val context = LocalContext.current
    val crashLog = remember { AppErrorHandler.getCrashLog() }
    val scrollState = rememberScrollState()

    var showRecoveryContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showRecoveryContent = true }

    Surface(
        modifier = Modifier.fillMaxSize().animateContentSize(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        AnimatedVisibility(
            visible = showRecoveryContent,
            enter = if (reducedMotion) fadeIn() else scaleIn(initialScale = 0.97f) + fadeIn()
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(locale.getString("crash_detected_title", "⚠️ Unexpected Crash Detected"), fontSize = 24.sp, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.height(16.dp))
            Text(locale.getString("crash_detected_message", "The game encountered an unexpected error during the previous session and has safely recovered your last valid progress."), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = crashLog,
                onValueChange = {},
                readOnly = true,
                label = { Text(locale.getString("crash_diagnostic_logs", "Crash Diagnostic Logs")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(locale.getString("crash_log_label", "Crash Log"), crashLog)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, locale.getString("crash_logs_copied_to_clipboard_toast", "Crash logs copied to clipboard!"), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(locale.getString("copy_logs", "Copy Logs"), fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, crashLog)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, locale.getString("share_crash_logs_title", "Share Crash Logs"))
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(locale.getString("share_logs", "Share Logs"), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(locale.getString("continue_playing", "Continue Playing"))
            }
        }
        }
    }
}
