package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

/** Developer panels share one in-window navigation stack on Windows. */
internal enum class DesktopDeveloperPanel {
    SETTINGS,
    CONSOLE,
    PROCESS_INSPECTOR
}

@Composable
internal fun DesktopDeveloperCenterScreen(
    initialPanel: DesktopDeveloperPanel = DesktopDeveloperPanel.SETTINGS,
    onBack: () -> Unit
) {
    val backStack = remember(initialPanel) { mutableStateListOf(initialPanel) }
    val saveableStateHolder = rememberSaveableStateHolder()
    val panel = backStack.last()

    fun navigate(next: DesktopDeveloperPanel) {
        if (backStack.lastOrNull() != next) backStack.add(next)
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else {
            onBack()
        }
    }

    key(panel) {
        saveableStateHolder.SaveableStateProvider(panel.name) {
            when (panel) {
                DesktopDeveloperPanel.SETTINGS -> DesktopDeveloperSettingsScreen(
                    onBack = ::goBack,
                    onNavigate = { route ->
                        when (route) {
                            "process_inspector" -> navigate(DesktopDeveloperPanel.PROCESS_INSPECTOR)
                            "advanced_dev_tools" -> navigate(DesktopDeveloperPanel.CONSOLE)
                        }
                    }
                )
                DesktopDeveloperPanel.CONSOLE -> DesktopDeveloperConsoleScreen(
                    onBack = ::goBack,
                    onOpenProcessInspector = { navigate(DesktopDeveloperPanel.PROCESS_INSPECTOR) }
                )
                DesktopDeveloperPanel.PROCESS_INSPECTOR -> DesktopProcessInspectorScreen(
                    onBack = ::goBack
                )
            }
        }
    }
}
