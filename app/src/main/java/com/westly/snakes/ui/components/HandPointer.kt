package com.westly.snakes.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.westly.snakes.ui.GameColors

/**
 * A simple hand with the index finger pointing down. [fingertip] is the lowest point of the
 * finger; [height] is the whole hand height (the hand is about 0.64 x height wide).
 */
fun DrawScope.drawHand(fingertip: Offset, height: Float) {
    val left = fingertip.x - 0.32f * height
    val top = fingertip.y - height
    val stroke = Stroke(width = height * 0.035f, join = StrokeJoin.Round)

    fun part(u: Float, v: Float, w: Float, h: Float, rad: Float) {
        val tl = Offset(left + u * height, top + v * height)
        val sz = Size(w * height, h * height)
        drawRoundRect(color = GameColors.HandSkin, topLeft = tl, size = sz, cornerRadius = CornerRadius(rad * height))
        drawRoundRect(
            color = GameColors.HandOutline,
            topLeft = tl,
            size = sz,
            cornerRadius = CornerRadius(rad * height),
            style = stroke
        )
    }

    part(0.00f, 0.20f, 0.26f, 0.17f, 0.085f) // thumb
    part(0.22f, 0.30f, 0.20f, 0.70f, 0.10f)  // index finger
    part(0.04f, 0.02f, 0.56f, 0.50f, 0.14f)  // palm

    // creases of the folded fingers
    for (i in 0 until 3) {
        val v = 0.17f + 0.12f * i
        drawLine(
            color = GameColors.HandOutline,
            start = Offset(left + 0.44f * height, top + v * height),
            end = Offset(left + 0.57f * height, top + v * height),
            strokeWidth = height * 0.025f,
            cap = StrokeCap.Round
        )
    }
}
