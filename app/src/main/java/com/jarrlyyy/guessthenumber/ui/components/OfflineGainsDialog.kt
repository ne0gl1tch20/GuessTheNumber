package com.jarrlyyy.guessthenumber.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.data.repository.LocaleManager
import com.jarrlyyy.guessthenumber.domain.model.BigNumber

@Composable
fun OfflineGainsDialog(
    gains: BigNumber,
    locale: LocaleManager,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(locale.getString("welcome_back_title", "🌙 Welcome Back!")) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    locale.getString(
                        "offline_gains_text",
                        "While you were away, your auto-clicker generated %s money!",
                        gains.format()
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    locale.getString(
                        "offline_gains_amount",
                        "+%s Money",
                        gains.format()
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(locale.getString("collect", "Collect"))
            }
        }
    )
}
