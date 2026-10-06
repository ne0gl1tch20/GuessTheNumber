package com.jarrlyyy.guessthenumber.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.unit.dp

@Composable
fun ReusableColorPickerDialog(
    initialHex: String,
    title: String = "🎨 Pick a color",
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    val initial = remember(initialHex) {
        runCatching { android.graphics.Color.parseColor(initialHex) }
            .getOrDefault(android.graphics.Color.MAGENTA)
    }
    val initialHsv = remember(initial) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) }
    }

    var hue by remember(initialHex) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialHex) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initialHex) { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by remember(initialHex) { mutableStateOf(initialHex.uppercase()) }

    val selectedColorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
    val selectedColor = Color(selectedColorInt)
    val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
    val hex = "#%02X%02X%02X".format(
        android.graphics.Color.red(selectedColorInt),
        android.graphics.Color.green(selectedColorInt),
        android.graphics.Color.blue(selectedColorInt)
    )

    fun updateFromPosition(position: Offset, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        saturation = (position.x / width).coerceIn(0f, 1f)
        value = (1f - position.y / height).coerceIn(0f, 1f)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Color", style = MaterialTheme.typography.labelLarge)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.45f)
                        .background(Color.White, MaterialTheme.shapes.medium)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        updateFromPosition(it, size.width.toFloat(), size.height.toFloat())
                                    },
                                    onDrag = { change, _ ->
                                        change.consumePositionChange()
                                        updateFromPosition(
                                            change.position,
                                            size.width.toFloat(),
                                            size.height.toFloat()
                                        )
                                    }
                                )
                            }
                    ) {
                        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
                        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

                        val markerX = saturation * size.width
                        val markerY = (1f - value) * size.height

                        drawCircle(
                            color = Color.White,
                            radius = 10.dp.toPx(),
                            center = Offset(markerX, markerY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.55f),
                            radius = 7.dp.toPx(),
                            center = Offset(markerX, markerY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                Text("Hue: ${hue.toInt()}°")
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        color = selectedColor,
                        shape = CircleShape,
                        tonalElevation = 4.dp
                    ) {}
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live preview", style = MaterialTheme.typography.labelLarge)
                        Text(hex, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input.uppercase()
                        runCatching {
                            val parsed = android.graphics.Color.parseColor(hexInput)
                            val next = FloatArray(3)
                            android.graphics.Color.colorToHSV(parsed, next)
                            hue = next[0]
                            saturation = next[1]
                            value = next[2]
                        }
                    },
                    label = { Text("Hex") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { onApply(hex) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
