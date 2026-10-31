package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState

/**
 * Compose-owned desktop popup. Compose renders the complete surface and controls;
 * the platform window supplies only its normal title bar and window behavior.
 */
@Composable
internal fun DesktopNativeDialogHost(
    title: String,
    visible: Boolean,
    width: Int,
    height: Int,
    resizable: Boolean = false,
    colorScheme: ColorScheme,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    if (visible) {
        DialogWindow(
            onCloseRequest = onDismissRequest,
            state = rememberDialogState(size = DpSize(width.dp, height.dp)),
            title = title,
            resizable = resizable,
            visible = true
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                shapes = DesktopThemePalette.shapes,
                typography = DesktopThemePalette.typography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    content()
                }
            }
        }
    }
}
