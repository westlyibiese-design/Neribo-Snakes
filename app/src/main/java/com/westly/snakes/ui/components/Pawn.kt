package com.westly.snakes.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.westly.snakes.ui.GameColors

enum class PawnColor { RED, BLUE }

/**
 * Glossy pawn: round head on a cone body with a base ellipse.
 * [baseCenter] is the centre of the base ellipse, [height] the pawn height.
 */
fun DrawScope.drawPawn(baseCenter: Offset, height: Float, color: PawnColor) {
    val main = if (color == PawnColor.RED) GameColors.PawnRed else GameColors.PawnBlue
    val dark = if (color == PawnColor.RED) GameColors.PawnRedDark else GameColors.PawnBlueDark
    val h = height
    val x = baseCenter.x
    val bottom = baseCenter.y + 0.08f * h

    // soft shadow under the base
    drawOval(
        color = Color(0x40000000),
        topLeft = Offset(x - 0.34f * h, bottom - 0.16f * h + 0.03f * h),
        size = Size(0.68f * h, 0.18f * h)
    )
    // cone body outline and head position (also used for the outlines below)
    val neckY = bottom - 0.58f * h
    val cone = Path().apply {
        moveTo(x - 0.25f * h, baseCenter.y)
        cubicTo(x - 0.20f * h, baseCenter.y - 0.20f * h, x - 0.12f * h, neckY + 0.15f * h, x - 0.10f * h, neckY)
        lineTo(x + 0.10f * h, neckY)
        cubicTo(x + 0.12f * h, neckY + 0.15f * h, x + 0.20f * h, baseCenter.y - 0.20f * h, x + 0.25f * h, baseCenter.y)
        close()
    }
    val headCenter = Offset(x, bottom - 0.74f * h)

    // the whole pawn shape grown by [grow], in one flat colour
    fun silhouette(edge: Color, grow: Float) {
        drawOval(
            color = edge,
            topLeft = Offset(x - 0.31f * h - grow, bottom - 0.16f * h - grow),
            size = Size(0.62f * h + 2f * grow, 0.16f * h + 2f * grow)
        )
        drawPath(path = cone, color = edge)
        drawPath(path = cone, color = edge, style = Stroke(width = 2f * grow, join = StrokeJoin.Round))
        drawCircle(color = edge, radius = 0.19f * h + grow, center = headCenter)
    }
    // white outer edge, then a thin dark edge: the pawn stands out on every square colour
    silhouette(Color(0xFFFFFFFF), 0.050f * h)
    silhouette(Color(0xFF111118), 0.024f * h)

    // base ellipse
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(main, dark),
            startY = bottom - 0.16f * h,
            endY = bottom
        ),
        topLeft = Offset(x - 0.31f * h, bottom - 0.16f * h),
        size = Size(0.62f * h, 0.16f * h)
    )
    // cone body
    drawPath(
        path = cone,
        brush = Brush.verticalGradient(
            colors = listOf(main, dark),
            startY = neckY,
            endY = baseCenter.y
        )
    )
    // head
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(main, dark),
            startY = headCenter.y - 0.19f * h,
            endY = headCenter.y + 0.19f * h
        ),
        radius = 0.19f * h,
        center = headCenter
    )
    // specular dot
    drawCircle(
        color = Color(0xD9FFFFFF),
        radius = 0.06f * h,
        center = Offset(headCenter.x - 0.07f * h, headCenter.y - 0.08f * h)
    )
}
