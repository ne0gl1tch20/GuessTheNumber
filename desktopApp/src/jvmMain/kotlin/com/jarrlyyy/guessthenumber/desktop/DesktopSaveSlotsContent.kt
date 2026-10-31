package com.jarrlyyy.guessthenumber.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.jarrlyyy.guessthenumber.desktop.persistence.DesktopSaveSlotSummary
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.text.DateFormat
import java.util.Date
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants

/**
 * Save-slot management is rendered with real Swing controls. This is deliberately
 * not a Compose screen embedded in a native dialog: the dialog and its contents
 * are owned by the desktop AWT/Swing UI toolkit.
 */
@Composable
internal fun DesktopSaveSlotsContent(
    activeSlot: Int,
    slots: List<DesktopSaveSlotSummary>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSwitchSlot: (Int) -> Unit,
    onCreateSlot: (Int) -> Unit,
    onDuplicateSlot: (Int, Int) -> Unit,
    onRestoreBackup: (Int) -> Unit,
    onExportSlot: (Int) -> Unit,
    onImportSlot: (Int) -> Unit,
    onExportAllSlots: () -> Unit,
    onImportAllSlots: () -> Unit,
    onContinueWithActiveSlot: (() -> Unit)? = null,
    showCloseButton: Boolean = true
) {
    SwingPanel(
        modifier = Modifier,
        factory = { JPanel(BorderLayout(12, 12)) },
        update = { root ->
            root.removeAll()
            root.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)

            val header = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
            val title = JLabel("Save Slots")
            title.font = title.font.deriveFont(Font.BOLD, 21f)
            header.add(title)
            if (showCloseButton) header.add(JButton("Close").apply { addActionListener { onBack() } })
            header.add(JButton("Refresh").apply { addActionListener { onRefresh() } })
            if (onContinueWithActiveSlot != null) {
                header.add(JButton("Continue with active slot").apply {
                    addActionListener { onContinueWithActiveSlot() }
                })
            }

            val actions = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                add(JButton("Export all slots").apply { addActionListener { onExportAllSlots() } })
                add(JButton("Import all slots").apply { addActionListener { onImportAllSlots() } })
            }
            val top = JPanel()
            top.layout = BoxLayout(top, BoxLayout.Y_AXIS)
            top.add(header)
            top.add(Box.createVerticalStrut(8))
            top.add(actions)
            top.add(Box.createVerticalStrut(8))
            top.add(JLabel("Ten independent local saves. Switching saves the current slot first. Valid previous saves can be restored."))
            top.add(JLabel("Importing an all-slot bundle replaces all ten local slots. Recovery copies are kept where possible."))
            top.add(JLabel("Portable backups are plain JSON files compatible with Android single-save import. They are not encrypted."))
            root.add(top, BorderLayout.NORTH)

            val slotList = JPanel()
            slotList.layout = BoxLayout(slotList, BoxLayout.Y_AXIS)
            slots.sortedBy { it.slot }.forEach { slot ->
                val card = JPanel()
                card.layout = BoxLayout(card, BoxLayout.Y_AXIS)
                card.border = BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)
                )
                card.alignmentX = JPanel.LEFT_ALIGNMENT
                val status = when {
                    slot.isCorrupt -> "Needs recovery"
                    slot.isEmpty -> "Empty"
                    else -> "Saved"
                }
                val slotTitle = JLabel("Slot ${slot.slot}${if (slot.slot == activeSlot) " · Active" else ""}    •    $status")
                slotTitle.font = slotTitle.font.deriveFont(Font.BOLD, 15f)
                card.add(slotTitle)

                val data = slot.state
                if (data != null) {
                    card.add(JLabel("${data.profileName.ifBlank { "Player" }} · ${data.difficultyId.replaceFirstChar { it.uppercase() }}"))
                    card.add(JLabel("Money ${data.money} · Prestige ${data.prestige} · Ultra ${data.ultra}"))
                    card.add(JLabel("Attempts ${data.attempts} · Correct ${data.correctGuesses}"))
                    if (data.lastSaveTimestamp > 0L) {
                        card.add(JLabel("Last saved: ${DateFormat.getDateTimeInstance().format(Date(data.lastSaveTimestamp))}"))
                    }
                }
                if (slot.hasBackup) card.add(JLabel("A valid recovery backup is available."))

                val row = JPanel(FlowLayout(FlowLayout.LEFT, 6, 4))
                if (!slot.isEmpty && !slot.isCorrupt) {
                    row.add(JButton("Export JSON").apply { addActionListener { onExportSlot(slot.slot) } })
                }
                row.add(JButton("Import JSON").apply { addActionListener { onImportSlot(slot.slot) } })
                if (slot.isEmpty) {
                    row.add(JButton("Create slot").apply { addActionListener { onCreateSlot(slot.slot) } })
                    if (slots.firstOrNull { it.slot == activeSlot }?.state != null) {
                        row.add(JButton("Duplicate active here").apply {
                            addActionListener { onDuplicateSlot(activeSlot, slot.slot) }
                        })
                    }
                } else if (!slot.isCorrupt && slot.slot != activeSlot) {
                    row.add(JButton("Switch to slot").apply { addActionListener { onSwitchSlot(slot.slot) } })
                    row.add(JButton("Duplicate active here").apply {
                        addActionListener { onDuplicateSlot(activeSlot, slot.slot) }
                    })
                }
                if (slot.hasBackup && !slot.isCorrupt) {
                    row.add(JButton("Restore backup").apply { addActionListener { onRestoreBackup(slot.slot) } })
                }
                card.add(row)
                slotList.add(card)
                slotList.add(Box.createVerticalStrut(10))
            }
            root.add(JScrollPane(slotList).apply {
                preferredSize = Dimension(900, 500)
                border = BorderFactory.createEmptyBorder()
            }, BorderLayout.CENTER)
            root.revalidate()
            root.repaint()
        }
    )
}
