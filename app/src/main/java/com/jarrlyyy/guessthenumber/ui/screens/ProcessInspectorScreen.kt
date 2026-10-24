package com.jarrlyyy.guessthenumber.ui.screens

import androidx.compose.runtime.Composable
import com.jarrlyyy.guessthenumber.domain.model.GameState

@Composable
fun ProcessInspectorScreen(
    state: GameState,
    onExecuteCommand: (String) -> String,
    onTestDailyRewards: () -> Unit,
    onBack: () -> Unit
) {
    DeveloperPlaygroundScreen(
        state = state,
        onExecuteCommand = onExecuteCommand,
        onTestDailyRewards = onTestDailyRewards,
        onBack = onBack
    )
}
