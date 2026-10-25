package com.westly.snakes.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.westly.snakes.ui.GameColors

/** Blue rounded die showing [value] (1-6), optionally rotated and with a soft shadow. */
fun DrawScope.drawDie(
    center: Offset,
    side: Float,
    value: Int,
    rotationDegrees: Float = 0f,
    shadow: Boolean = false
) {
    val left = center.x - side / 2f
    val top = center.y - side / 2f
    val radius = side * 0.18f
    val pipRadius = side * 0.09f

    fun pip(u: Float, v: Float) {
        drawCircle(color = GameColors.DiePip, radius = pipRadius, center = Offset(left + u * side, top + v * side))
    }

    rotate(degrees = rotationDegrees, pivot = center) {
        if (shadow) {
            for (i in 0 until 4) {
                val grow = side * 0.02f * i
                drawRoundRect(
                    color = Color(0x1A000000),
                    topLeft = Offset(left - grow + side * 0.05f, top - grow + side * 0.09f),
                    size = Size(side + 2f * grow, side + 2f * grow),
                    cornerRadius = CornerRadius(radius + grow)
                )
            }
        }
        // body
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(GameColors.DieTop, GameColors.DieBottom),
                startY = top,
                endY = top + side
            ),
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = CornerRadius(radius)
        )
        // thin darker rim
        val rim = side * 0.03f
        drawRoundRect(
            color = GameColors.DieRim,
            topLeft = Offset(left + rim / 2f, top + rim / 2f),
            size = Size(side - rim, side - rim),
            cornerRadius = CornerRadius(radius),
            style = Stroke(width = rim)
        )
        // glossy highlight on the upper half
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x66FFFFFF), Color(0x00FFFFFF)),
                startY = top + side * 0.05f,
                endY = top + side * 0.5f
            ),
            topLeft = Offset(left + side * 0.07f, top + side * 0.05f),
            size = Size(side * 0.86f, side * 0.45f),
            cornerRadius = CornerRadius(side * 0.14f)
        )
        // pips
        when (value) {
            1 -> pip(0.5f, 0.5f)
            2 -> { pip(0.25f, 0.25f); pip(0.75f, 0.75f) }
            3 -> { pip(0.25f, 0.25f); pip(0.5f, 0.5f); pip(0.75f, 0.75f) }
            4 -> { pip(0.25f, 0.25f); pip(0.75f, 0.25f); pip(0.25f, 0.75f); pip(0.75f, 0.75f) }
            5 -> {
                pip(0.25f, 0.25f); pip(0.75f, 0.25f); pip(0.25f, 0.75f); pip(0.75f, 0.75f)
                pip(0.5f, 0.5f)
            }
            6 -> {
                pip(0.25f, 0.25f); pip(0.25f, 0.5f); pip(0.25f, 0.75f)
                pip(0.75f, 0.25f); pip(0.75f, 0.5f); pip(0.75f, 0.75f)
            }
        }
    }
}
