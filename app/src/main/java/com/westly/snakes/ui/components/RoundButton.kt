package com.westly.snakes.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.westly.snakes.ui.GameColors

enum class ButtonIcon { HOME, MENU }

/** Glossy red round button with a white icon drawn in code. [pressScale] is 0.94 while pressed. */
fun DrawScope.drawRoundButton(
    center: Offset,
    diameter: Float,
    icon: ButtonIcon,
    pressScale: Float = 1f
) {
    scale(scale = pressScale, pivot = center) {
        val r = diameter / 2f

        // soft shadow
        drawCircle(
            color = Color(0x26000000),
            radius = r * 1.08f,
            center = Offset(center.x, center.y + r * 0.16f)
        )
        drawCircle(
            color = Color(0x4D000000),
            radius = r,
            center = Offset(center.x, center.y + r * 0.12f)
        )
        // rim and body
        drawCircle(color = GameColors.ButtonRim, radius = r, center = center)
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(GameColors.ButtonTop, GameColors.ButtonBottom),
                startY = center.y - r,
                endY = center.y + r
            ),
            radius = r * 0.93f,
            center = center
        )
        // glossy highlight
        val glossTop = center.y - r * 0.86f
        val glossH = r * 0.8f
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xB3FFFFFF), Color(0x00FFFFFF)),
                startY = glossTop,
                endY = glossTop + glossH
            ),
            topLeft = Offset(center.x - r * 0.62f, glossTop),
            size = Size(r * 1.24f, glossH)
        )

        when (icon) {
            ButtonIcon.HOME -> drawHomeIcon(center, diameter)
            ButtonIcon.MENU -> drawMenuGridIcon(center, diameter)
        }
    }
}

private fun DrawScope.drawHomeIcon(c: Offset, d: Float) {
    val roof = Path().apply {
        moveTo(c.x - d * 0.30f, c.y - d * 0.01f)
        lineTo(c.x, c.y - d * 0.30f)
        lineTo(c.x + d * 0.30f, c.y - d * 0.01f)
        close()
    }
    drawPath(path = roof, color = Color.White)
    drawPath(path = roof, color = Color.White, style = Stroke(width = d * 0.04f, join = StrokeJoin.Round))
    drawRect(
        color = Color.White,
        topLeft = Offset(c.x - d * 0.2f, c.y - d * 0.02f),
        size = Size(d * 0.4f, d * 0.3f)
    )
    drawRect(
        color = GameColors.ButtonBottom,
        topLeft = Offset(c.x - d * 0.055f, c.y + d * 0.11f),
        size = Size(d * 0.11f, d * 0.17f)
    )
}

private fun DrawScope.drawMenuGridIcon(c: Offset, d: Float) {
    val gap = d * 0.17f
    for (i in -1..1) {
        for (j in -1..1) {
            drawCircle(
                color = Color.White,
                radius = d * 0.05f,
                center = Offset(c.x + i * gap, c.y + j * gap)
            )
        }
    }
}
