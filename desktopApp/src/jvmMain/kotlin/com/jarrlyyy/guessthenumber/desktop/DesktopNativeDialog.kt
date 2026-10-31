package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.awt.Window

/**
 * Compose-owned desktop popup. The dialog surface and every control inside it
 * are Compose UI; no SwingPanel or ComposePanel is used for popup content.
 */
@Composable
internal fun DesktopNativeDialogHost(
    owner: Window,
    title: String,
    visible: Boolean,
    width: Int,
    height: Int,
    resizable: Boolean = false,
    colorScheme: ColorScheme,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    @Suppress("UNUSED_VARIABLE")
    val desktopOwner = owner
    @Suppress("UNUSED_VARIABLE")
    val dialogTitle = title
    @Suppress("UNUSED_VARIABLE")
    val canResize = resizable

    if (visible) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                shapes = DesktopThemePalette.shapes,
                typography = DesktopThemePalette.typography
            ) {
                Surface(
                    modifier = Modifier.size(width = width.dp, height = height.dp),
                    color = MaterialTheme.colorScheme.background,
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 8.dp
                ) {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
        }
    }
}
