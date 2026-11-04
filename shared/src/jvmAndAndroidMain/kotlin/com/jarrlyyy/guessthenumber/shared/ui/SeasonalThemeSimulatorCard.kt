package com.jarrlyyy.guessthenumber.shared.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarrlyyy.guessthenumber.shared.seasonal.SeasonalThemeCalendar
import com.jarrlyyy.guessthenumber.shared.seasonal.SeasonalThemeCalendarConfig
import java.time.LocalDate

/** Preview-only calendar sandbox. It never writes game state or changes trait eligibility. */
@Composable
fun SeasonalThemeSimulatorCard() {
    val presets = listOf(
        "Christmas" to LocalDate.of(2026, 12, 15),
        "New Year" to LocalDate.of(2027, 1, 2),
        "Valentine's" to LocalDate.of(2027, 2, 14),
        "Lunar New Year" to LocalDate.of(2027, 2, 6),
        "Easter" to LocalDate.of(2027, 3, 26),
        "Philippine Summer" to LocalDate.of(2027, 4, 15),
        "Independence Day" to LocalDate.of(2027, 6, 12),
        "Back to School" to LocalDate.of(2027, 6, 15),
        "Halloween" to LocalDate.of(2027, 10, 31),
        "Winter" to LocalDate.of(2027, 1, 15)
    )
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var previewDate by remember { mutableStateOf(LocalDate.now()) }
    var dateError by remember { mutableStateOf(false) }
    val previewConfig = remember { SeasonalThemeCalendarConfig(winterEnabled = true) }
    val active = remember(previewDate) {
        SeasonalThemeCalendar.activeThemes(previewDate, previewConfig)
    }
    val labels = mapOf(
        "christmas" to ("🎄" to "Christmas"),
        "new_year" to ("🎆" to "New Year"),
        "valentines" to ("💗" to "Valentine's Day"),
        "lunar_new_year" to ("🧧" to "Lunar New Year"),
        "easter" to ("🐣" to "Easter"),
        "philippine_summer" to ("☀️" to "Philippine Summer"),
        "philippine_independence_day" to ("🇵🇭" to "Philippine Independence Day"),
        "back_to_school" to ("🎒" to "Back to School"),
        "halloween" to ("🎃" to "Halloween"),
        "winter" to ("❄️" to "Winter")
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Season Simulator", style = MaterialTheme.typography.titleLarge)
            Text(
                "Preview calendar windows without changing saves, rewards or trait eligibility. Winter is enabled here so it can be tested in any region.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it; dateError = false },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Preview date (YYYY-MM-DD)") },
                isError = dateError
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val parsed = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                    if (parsed == null) dateError = true
                    else { previewDate = parsed; dateError = false }
                }) { Text("Preview date") }
                OutlinedButton(onClick = {
                    previewDate = LocalDate.now()
                    dateText = previewDate.toString()
                    dateError = false
                }) { Text("Today") }
            }
            if (dateError) {
                Text("Enter a valid date like 2027-02-06.", color = MaterialTheme.colorScheme.error)
            }
            Text("Quick presets", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (label, date) ->
                    OutlinedButton(onClick = {
                        dateText = date.toString()
                        previewDate = date
                        dateError = false
                    }) { Text(label) }
                }
            }
            HorizontalDivider()
            Text("Preview: $previewDate", style = MaterialTheme.typography.titleMedium)
            if (active.isEmpty()) {
                Text("No seasonal event is active on this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                active.forEachIndexed { index, theme ->
                    val (emoji, label) = labels[theme.id] ?: ("✨" to theme.id)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("$emoji  $label", style = MaterialTheme.typography.titleSmall)
                            Text(
                                theme.id,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (index == 0) {
                            Text("PRIMARY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
