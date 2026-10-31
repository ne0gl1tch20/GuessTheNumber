package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.awt.ComposePanel
import java.awt.Dialog
import java.awt.Dimension
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JDialog
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * A real Swing dialog window with Compose content. Swing supplies the Windows-native
 * title bar/window behavior while the content keeps the desktop game's live theme.
 */
internal class DesktopNativeDialogWindow(
    owner: Window,
    title: String,
    width: Int,
    height: Int,
    resizable: Boolean,
    private val onDismissRequest: () -> Unit
) {
    private val composePanel = ComposePanel()
    private val dialog: JDialog
    private var darkTitleBar = false

    init {
        if (SwingUtilities.isEventDispatchThread()) {
            runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
        } else {
            SwingUtilities.invokeAndWait {
                runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
            }
        }

        dialog = JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL).apply {
            defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
            isResizable = resizable
            contentPane = composePanel
            minimumSize = Dimension(width, height)
            setSize(width, height)
            setLocationRelativeTo(owner)
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(event: WindowEvent) {
                    onDismissRequest()
                }

                override fun windowOpened(event: WindowEvent) {
                    DesktopWindowChrome.apply(this@apply, darkTitleBar)
                }
            })
        }
    }

    fun setContent(content: @Composable () -> Unit) {
        composePanel.setContent(content)
    }

    fun applyTheme(isDark: Boolean) {
        darkTitleBar = isDark
        DesktopWindowChrome.apply(dialog, isDark)
    }

    fun show() = onEdt { if (!dialog.isVisible) dialog.isVisible = true }

    fun hide() = onEdt { dialog.isVisible = false }

    fun dispose() = onEdt { dialog.dispose() }

    private fun onEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) action() else SwingUtilities.invokeLater(action)
    }
}

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
    val latestContent = rememberUpdatedState(content)
    val latestColorScheme = rememberUpdatedState(colorScheme)
    val latestDismiss = rememberUpdatedState(onDismissRequest)
    val dialog = remember(owner, title, width, height, resizable) {
        DesktopNativeDialogWindow(owner, title, width, height, resizable) {
            latestDismiss.value.invoke()
        }
    }

    DisposableEffect(dialog) {
        dialog.setContent {
            val activeScheme = latestColorScheme.value
            MaterialTheme(
                colorScheme = activeScheme,
                shapes = DesktopThemePalette.shapes,
                typography = DesktopThemePalette.typography
            ) {
                latestContent.value.invoke()
            }
        }
        onDispose { dialog.dispose() }
    }

    LaunchedEffect(visible) {
        if (visible) dialog.show() else dialog.hide()
    }

    LaunchedEffect(colorScheme) {
        dialog.applyTheme(colorScheme.background.luminance() < 0.5f)
    }
}
