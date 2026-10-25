package com.westly.snakes.ui.components

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

enum class LabelAlign { LEFT, CENTER }

/**
 * Bold sans-serif text with an optional outline, drawn on the native canvas.
 * [anchor].x is the left edge (LEFT) or the centre (CENTER); [anchor].y is the vertical centre.
 */
fun DrawScope.drawOutlinedText(
    text: String,
    anchor: Offset,
    sizePx: Float,
    fillColor: Color,
    outlineColor: Color? = null,
    align: LabelAlign = LabelAlign.CENTER
) {
    val fill = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = if (align == LabelAlign.LEFT) AndroidPaint.Align.LEFT else AndroidPaint.Align.CENTER
        color = fillColor.toArgb()
    }
    val metrics = fill.fontMetrics
    val baseline = anchor.y - (metrics.ascent + metrics.descent) / 2f
    val canvas = drawContext.canvas.nativeCanvas
    if (outlineColor != null) {
        val outline = AndroidPaint(fill).apply {
            style = AndroidPaint.Style.STROKE
            strokeWidth = sizePx * 0.2f
            strokeJoin = AndroidPaint.Join.ROUND
            color = outlineColor.toArgb()
        }
        canvas.drawText(text, anchor.x, baseline, outline)
    }
    canvas.drawText(text, anchor.x, baseline, fill)
}
