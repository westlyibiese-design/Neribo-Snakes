package com.westly.snakes.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.westly.snakes.ui.GameColors
import kotlin.math.max

/** Wooden dice tray: rounded body, dark border and a recessed floor with a soft inner shadow. */
fun DrawScope.drawWoodTray(rect: Rect, boardSize: Float) {
    val radius = boardSize * 0.02f
    val border = 2.dp.toPx()

    // drop shadow
    drawRoundRect(
        color = Color(0x40000000),
        topLeft = rect.topLeft + Offset(0f, boardSize * 0.012f),
        size = rect.size,
        cornerRadius = CornerRadius(radius)
    )
    // body
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(GameColors.WoodLight, GameColors.WoodDark),
            startY = rect.top,
            endY = rect.bottom
        ),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(radius)
    )
    // border (kept inside the body)
    drawRoundRect(
        color = GameColors.WoodBorder,
        topLeft = rect.topLeft + Offset(border / 2f, border / 2f),
        size = Size(rect.width - border, rect.height - border),
        cornerRadius = CornerRadius(radius),
        style = Stroke(width = border)
    )

    // recess
    val inset = boardSize * 0.012f
    val rLeft = rect.left + inset
    val rTop = rect.top + inset
    val rW = rect.width - 2f * inset
    val rH = rect.height - 2f * inset
    val recessRadius = radius * 0.7f
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(GameColors.RecessCenter, GameColors.RecessEdge),
            center = Offset(rLeft + rW / 2f, rTop + rH / 2f),
            radius = max(rW, rH) / 2f
        ),
        topLeft = Offset(rLeft, rTop),
        size = Size(rW, rH),
        cornerRadius = CornerRadius(recessRadius)
    )

    // soft inner shadow: a few fading strokes along the recess edge
    val steps = 5
    val step = boardSize * 0.0035f
    for (i in 0 until steps) {
        val alpha = 0.26f * (1f - i / steps.toFloat())
        val o = i * step
        drawRoundRect(
            color = GameColors.InnerShadow.copy(alpha = alpha),
            topLeft = Offset(rLeft + o, rTop + o),
            size = Size(rW - 2f * o, rH - 2f * o),
            cornerRadius = CornerRadius(max(0f, recessRadius - o)),
            style = Stroke(width = step * 1.2f)
        )
    }
}

/** Round wood-ringed circle with a dark brown disc inside (token slot and counter). */
fun DrawScope.drawWoodCircle(center: Offset, diameter: Float) {
    val r = diameter / 2f
    val border = 1.5.dp.toPx()

    drawCircle(
        color = Color(0x40000000),
        radius = r,
        center = Offset(center.x, center.y + r * 0.1f)
    )
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(GameColors.WoodLight, GameColors.WoodDark),
            startY = center.y - r,
            endY = center.y + r
        ),
        radius = r,
        center = center
    )
    drawCircle(
        color = GameColors.WoodBorder,
        radius = r - border / 2f,
        center = center,
        style = Stroke(width = border)
    )

    val disc = r * 0.7f
    drawCircle(color = GameColors.SlotDisc, radius = disc, center = center)
    // inner shadow along the disc edge
    drawCircle(
        color = GameColors.InnerShadow.copy(alpha = 0.35f),
        radius = disc - r * 0.04f,
        center = center,
        style = Stroke(width = r * 0.08f)
    )
}
