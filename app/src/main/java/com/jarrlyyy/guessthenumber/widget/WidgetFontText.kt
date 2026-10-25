package com.jarrlyyy.guessthenumber.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.roundToInt

/**
 * Glance cannot apply bundled typefaces to Text directly. Render only the two
 * high-visibility text elements as small, accessible bitmap images; all other
 * widget text remains native. These bitmaps are created only when the widget
 * is updated, never on a timer or in a polling loop.
 */
internal fun renderWidgetFontText(
    context: Context,
    text: String,
    fontAssetPath: String,
    widthDp: Int,
    heightDp: Int,
    textSizeSp: Float,
    color: Int,
    bold: Boolean
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaledDensity = context.resources.displayMetrics.scaledDensity
    val widthPx = (widthDp * density).roundToInt().coerceAtLeast(1)
    val heightPx = (heightDp * density).roundToInt().coerceAtLeast(1)
    val paddingPx = (4f * density).roundToInt()
    val typeface = runCatching {
        Typeface.createFromAsset(context.assets, fontAssetPath)
    }.getOrElse {
        Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = textSizeSp * scaledDensity
        this.typeface = Typeface.create(typeface, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    val availableWidth = (widthPx - paddingPx * 2).coerceAtLeast(1)
    var displayText = text
    if (paint.measureText(displayText) > availableWidth) {
        val ellipsis = "…"
        while (displayText.isNotEmpty() && paint.measureText(displayText + ellipsis) > availableWidth) {
            displayText = displayText.dropLast(1)
        }
        displayText += ellipsis
    }
    return Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        val metrics = paint.fontMetrics
        val baseline = (heightPx - metrics.ascent - metrics.descent) / 2f
        canvas.drawText(displayText, paddingPx.toFloat(), baseline, paint)
    }
}

internal fun widgetTextColor(context: Context, lightColor: Int, darkColor: Int): Int {
    val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return if (nightMode == Configuration.UI_MODE_NIGHT_YES) darkColor else lightColor
}
