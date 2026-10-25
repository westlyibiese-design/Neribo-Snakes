package com.westly.snakes.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.westly.snakes.ui.GameColors
import kotlin.math.cos
import kotlin.math.sin

enum class ButtonIcon { HOME, MENU, CLOSE, EXIT, RESTART, SOUND_ON, SOUND_OFF, STAR }

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
            ButtonIcon.CLOSE -> drawCloseIcon(center, diameter)
            ButtonIcon.EXIT -> drawRunnerIcon(center, diameter)
            ButtonIcon.RESTART -> drawRestartIcon(center, diameter)
            ButtonIcon.SOUND_ON -> drawSpeakerIcon(center, diameter, on = true)
            ButtonIcon.SOUND_OFF -> drawSpeakerIcon(center, diameter, on = false)
            ButtonIcon.STAR -> drawStarIcon(center, diameter)
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

private fun DrawScope.drawCloseIcon(c: Offset, d: Float) {
    val a = d * 0.17f
    val width = d * 0.09f
    drawLine(Color.White, Offset(c.x - a, c.y - a), Offset(c.x + a, c.y + a), strokeWidth = width, cap = StrokeCap.Round)
    drawLine(Color.White, Offset(c.x + a, c.y - a), Offset(c.x - a, c.y + a), strokeWidth = width, cap = StrokeCap.Round)
}

/** A running person made of rounded strokes. */
private fun DrawScope.drawRunnerIcon(c: Offset, d: Float) {
    val width = d * 0.075f
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        drawLine(Color.White, Offset(c.x + d * x1, c.y + d * y1), Offset(c.x + d * x2, c.y + d * y2), strokeWidth = width, cap = StrokeCap.Round)
    }
    drawCircle(Color.White, radius = d * 0.065f, center = Offset(c.x + d * 0.10f, c.y - d * 0.26f))
    line(0.06f, -0.16f, -0.04f, 0.05f) // body
    line(0.04f, -0.11f, 0.21f, -0.03f) // front arm
    line(0.04f, -0.11f, -0.13f, -0.12f) // back arm
    line(-0.04f, 0.05f, 0.12f, 0.14f) // front leg, thigh
    line(0.12f, 0.14f, 0.09f, 0.29f) // front leg, shin
    line(-0.04f, 0.05f, -0.17f, 0.16f) // back leg, thigh
    line(-0.17f, 0.16f, -0.28f, 0.25f) // back leg, shin
}

/** A circular arrow. */
private fun DrawScope.drawRestartIcon(c: Offset, d: Float) {
    val r = d * 0.22f
    drawArc(
        color = Color.White,
        startAngle = -60f,
        sweepAngle = 290f,
        useCenter = false,
        topLeft = Offset(c.x - r, c.y - r),
        size = Size(2f * r, 2f * r),
        style = Stroke(width = d * 0.075f, cap = StrokeCap.Round)
    )
    // arrow head at the end of the arc (230 degrees), pointing along the circle
    val endX = c.x + r * cos(230f * 0.017453292f)
    val endY = c.y + r * sin(230f * 0.017453292f)
    val tx = -sin(230f * 0.017453292f)
    val ty = cos(230f * 0.017453292f)
    val nx = -ty
    val ny = tx
    val head = Path().apply {
        moveTo(endX + tx * d * 0.14f, endY + ty * d * 0.14f)
        lineTo(endX - tx * d * 0.02f + nx * d * 0.10f, endY - ty * d * 0.02f + ny * d * 0.10f)
        lineTo(endX - tx * d * 0.02f - nx * d * 0.10f, endY - ty * d * 0.02f - ny * d * 0.10f)
        close()
    }
    drawPath(path = head, color = Color.White)
}

/** A speaker; when [on] is false a slash crosses it. */
private fun DrawScope.drawSpeakerIcon(c: Offset, d: Float, on: Boolean) {
    val body = Path().apply {
        moveTo(c.x - d * 0.26f, c.y - d * 0.09f)
        lineTo(c.x - d * 0.14f, c.y - d * 0.09f)
        lineTo(c.x + d * 0.02f, c.y - d * 0.24f)
        lineTo(c.x + d * 0.02f, c.y + d * 0.24f)
        lineTo(c.x - d * 0.14f, c.y + d * 0.09f)
        lineTo(c.x - d * 0.26f, c.y + d * 0.09f)
        close()
    }
    drawPath(path = body, color = Color.White)
    if (on) {
        for (i in 1..2) {
            val r = d * (0.10f + 0.09f * i)
            drawArc(
                color = Color.White,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(c.x + d * 0.04f - r, c.y - r),
                size = Size(2f * r, 2f * r),
                style = Stroke(width = d * 0.05f, cap = StrokeCap.Round)
            )
        }
    } else {
        val from = Offset(c.x - d * 0.27f, c.y - d * 0.29f)
        val to = Offset(c.x + d * 0.27f, c.y + d * 0.29f)
        drawLine(GameColors.ButtonRim, from, to, strokeWidth = d * 0.12f, cap = StrokeCap.Round)
        drawLine(Color.White, from, to, strokeWidth = d * 0.06f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawStarIcon(c: Offset, d: Float) {
    val outer = d * 0.29f
    val inner = d * 0.12f
    val star = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = (-90f + i * 36f) * 0.017453292f
        val x = c.x + radius * cos(angle)
        val y = c.y + d * 0.015f + radius * sin(angle)
        if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
    }
    star.close()
    drawPath(path = star, color = Color.White)
    drawPath(path = star, color = Color.White, style = Stroke(width = d * 0.03f, join = StrokeJoin.Round))
}
