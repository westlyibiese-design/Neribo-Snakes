package com.westly.snakes.ui

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import com.westly.snakes.game.BoardData
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Phase 2: the static SnakeMate board. Everything is drawn in code with Canvas.
// No dice, tokens, taps or movement yet.
// ---------------------------------------------------------------------------

/** Square colours, repeated in the same pattern as the reference board. */
private val CELL_COLORS = listOf(
    Color(0xFFF9B71C), // yellow
    Color(0xFFFBFAF4), // white
    Color(0xFFE5382E), // red
    Color(0xFF2D86DE), // blue
    Color(0xFF2DAA4B)  // green
)

private class SnakeStyle(val body: Color, val band: Color, val phase: Float)

private val SNAKE_STYLES = mapOf(
    96 to SnakeStyle(Color(0xFF8CC63F), Color(0xFFF2C230), 0.3f),
    87 to SnakeStyle(Color(0xFF2F7FE0), Color(0xFF5FD0F5), 0.0f),
    80 to SnakeStyle(Color(0xFF8E3FD1), Color(0xFFC27AE8), 1.0f),
    64 to SnakeStyle(Color(0xFF0FA89B), Color(0xFF38C6E8), 0.5f),
    53 to SnakeStyle(Color(0xFF34434F), Color(0xFF6F8190), 0.2f),
    41 to SnakeStyle(Color(0xFF5B3F9E), Color(0xFF8B6FD0), 0.9f),
    31 to SnakeStyle(Color(0xFF1F9E55), Color(0xFF7BDB9E), 0.4f),
    25 to SnakeStyle(Color(0xFFE0267A), Color(0xFFFF7DB0), 0.7f),
    13 to SnakeStyle(Color(0xFFC2185B), Color(0xFFF9A0C8), 0.1f)
)

private val TWO_PI_F = (2.0 * PI).toFloat()
private val PI_F = PI.toFloat()

private fun lighten(c: Color, a: Float): Color =
    Color(c.red + (1f - c.red) * a, c.green + (1f - c.green) * a, c.blue + (1f - c.blue) * a, c.alpha)

private fun darken(c: Color, a: Float): Color =
    Color(c.red * (1f - a), c.green * (1f - a), c.blue * (1f - a), c.alpha)

/** Board measurements for a given board size in pixels. */
internal class Geo(val size: Float) {
    val pad = size * 0.024f
    val cell = (size - 2f * pad) / 10f

    fun center(square: Int): Offset {
        val col = BoardData.column(square)
        val row = 9 - BoardData.rowFromBottom(square)
        return Offset(pad + (col + 0.5f) * cell, pad + (row + 0.5f) * cell)
    }
}

@Composable
fun BoardCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawBoard()
    }
}

private fun DrawScope.drawBoard() {
    val g = Geo(size.minDimension)
    drawFrame(g)
    drawCells(g)
    for ((bottom, top) in BoardData.LADDERS) drawLadder(g, bottom, top)
    for ((head, tail) in BoardData.SNAKES) drawSnake(g, head, tail)
    drawNumbers(g)
    drawRect(
        color = Color(0xFFC58B4B),
        topLeft = Offset(g.size * 0.005f, g.size * 0.005f),
        size = Size(g.size * 0.99f, g.size * 0.99f),
        style = Stroke(width = g.size * 0.003f)
    )
}

private fun DrawScope.drawFrame(g: Geo) {
    drawRect(color = Color(0xFF6E4220), size = Size(g.size, g.size))
    drawRect(
        color = Color(0xFF8B5A2B),
        topLeft = Offset(g.size * 0.008f, g.size * 0.008f),
        size = Size(g.size * 0.984f, g.size * 0.984f)
    )
    val edge = g.pad - g.size * 0.003f
    drawRect(
        color = Color(0xFF14213D),
        topLeft = Offset(edge, edge),
        size = Size(g.size - 2f * edge, g.size - 2f * edge)
    )
}

private fun DrawScope.drawCells(g: Geo) {
    val cell = g.cell
    for (n in 1..BoardData.LAST_SQUARE) {
        val c = g.center(n)
        val col = BoardData.column(n)
        val r = BoardData.rowFromBottom(n)
        val base = CELL_COLORS[(3 * r + col) % 5]
        val x = c.x - cell / 2f
        val y = c.y - cell / 2f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(lighten(base, 0.14f), darken(base, 0.07f)),
                startY = y,
                endY = y + cell
            ),
            topLeft = Offset(x, y),
            size = Size(cell, cell)
        )
        drawRect(
            color = Color(0x59141E3C),
            topLeft = Offset(x + 1f, y + 1f),
            size = Size(cell - 2f, cell - 2f),
            style = Stroke(width = cell * 0.02f)
        )
    }
}

private fun DrawScope.drawLadder(g: Geo, from: Int, to: Int) {
    val cell = g.cell
    val a = g.center(from)
    val b = g.center(to)
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len = hypot(dx, dy)
    val ux = dx / len
    val uy = dy / len
    val px = -uy
    val py = ux
    val w = cell * 0.21f
    val pad = cell * 0.16f
    val sx = a.x + ux * pad
    val sy = a.y + uy * pad
    val ex = b.x - ux * pad
    val ey = b.y - uy * pad
    val count = maxOf(2, ((len - 2f * pad) / (cell * 0.42f)).roundToInt())

    fun rails(ox: Float, oy: Float, color: Color, width: Float) {
        for (s in listOf(-1f, 1f)) {
            drawLine(
                color = color,
                start = Offset(sx + px * w * s + ox, sy + py * w * s + oy),
                end = Offset(ex + px * w * s + ox, ey + py * w * s + oy),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    }

    fun rungs(ox: Float, oy: Float, color: Color, width: Float) {
        for (i in 0..count) {
            val t = i.toFloat() / count
            val cx = sx + (ex - sx) * t
            val cy = sy + (ey - sy) * t
            drawLine(
                color = color,
                start = Offset(cx - px * w + ox, cy - py * w + oy),
                end = Offset(cx + px * w + ox, cy + py * w + oy),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    }

    val shadow = Color(0x4D0A1428)
    rails(cell * 0.05f, cell * 0.06f, shadow, cell * 0.12f)
    rungs(cell * 0.05f, cell * 0.06f, shadow, cell * 0.085f)
    rails(0f, 0f, Color(0xFF6F4020), cell * 0.12f)
    rungs(0f, 0f, Color(0xFF8E5A2B), cell * 0.085f)
    rails(-cell * 0.014f, -cell * 0.014f, Color(0xFFC48A4B), cell * 0.04f)
    rungs(-cell * 0.01f, -cell * 0.01f, Color(0xFFD9A468), cell * 0.028f)
}

/** The sampled body of one snake: 91 points from the head (index 0) to the tail. */
private class SnakeCurve(
    val xs: FloatArray,
    val ys: FloatArray,
    val ts: FloatArray,
    val lengthInCells: Float
)

private fun computeSnakeCurve(g: Geo, head: Int, tail: Int, phase: Float): SnakeCurve {
    val cell = g.cell
    val a = g.center(head)
    val b = g.center(tail)
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len = hypot(dx, dy)
    val ux = dx / len
    val uy = dy / len
    val px = -uy
    val py = ux
    val lc = len / cell
    val waves = maxOf(1.5f, lc / 2.6f)
    val amp = minOf(cell, cell * (0.22f + 0.16f * lc))

    val n = 90
    val xs = FloatArray(n + 1)
    val ys = FloatArray(n + 1)
    val ts = FloatArray(n + 1)
    for (i in 0..n) {
        val t = i.toFloat() / n
        val env = maxOf(0f, sin(PI_F * t)).pow(0.7f)
        val off = sin(TWO_PI_F * waves * t + phase * TWO_PI_F) * amp * env
        xs[i] = a.x + dx * t + px * off
        ys[i] = a.y + dy * t + py * off
        ts[i] = t
    }
    return SnakeCurve(xs, ys, ts, lc)
}

/**
 * The drawn body of a snake as 91 points, index 0 at the head, relative to the board's top-left
 * corner. A pawn sliding down the snake follows exactly these points.
 */
internal fun snakeCurve(g: Geo, head: Int, tail: Int): List<Offset> {
    val phase = SNAKE_STYLES[head]?.phase ?: 0f
    val curve = computeSnakeCurve(g, head, tail, phase)
    return List(curve.xs.size) { Offset(curve.xs[it], curve.ys[it]) }
}

private fun DrawScope.drawSnake(g: Geo, head: Int, tail: Int) {
    val style = SNAKE_STYLES[head] ?: return
    val cell = g.cell
    val curve = computeSnakeCurve(g, head, tail, style.phase)
    val xs = curve.xs
    val ys = curve.ys
    val ts = curve.ts
    val n = xs.size - 1
    val lc = curve.lengthInCells

    fun radius(t: Float): Float = cell * (0.2f - 0.115f * t) + (if (t < 0.06f) cell * 0.03f else 0f)

    // shadow
    val shadow = Color(0x470A1428)
    for (i in 0..n) {
        drawCircle(shadow, radius(ts[i]), Offset(xs[i] + cell * 0.04f, ys[i] + cell * 0.06f))
    }
    // outline
    val outline = darken(style.body, 0.55f)
    for (i in 0..n) {
        drawCircle(outline, radius(ts[i]) + cell * 0.022f, Offset(xs[i], ys[i]))
    }
    // body with bands
    for (i in 0..n) {
        val band = floor(ts[i] * lc * 2.2f).toInt() % 2 == 0
        drawCircle(if (band) style.body else style.band, radius(ts[i]), Offset(xs[i], ys[i]))
    }
    // soft highlight
    val shine = Color(0x38FFFFFF)
    for (i in 0..n) {
        drawCircle(shine, radius(ts[i]) * 0.45f, Offset(xs[i] - cell * 0.03f, ys[i] - cell * 0.04f))
    }

    // head: faces away from the body
    val hx = xs[0]
    val hy = ys[0]
    val hdx = xs[4] - hx
    val hdy = ys[4] - hy
    val hl = hypot(hdx, hdy)
    val hux = -hdx / hl
    val huy = -hdy / hl
    val hpx = -huy
    val hpy = hux
    val angle = atan2(huy, hux) * 180f / PI_F
    val hcx = hx + hux * cell * 0.04f
    val hcy = hy + huy * cell * 0.04f

    rotate(degrees = angle, pivot = Offset(hcx, hcy)) {
        drawOval(
            color = outline,
            topLeft = Offset(hcx - cell * 0.27f, hcy - cell * 0.23f),
            size = Size(cell * 0.54f, cell * 0.46f)
        )
        drawOval(
            color = style.body,
            topLeft = Offset(hcx - cell * 0.245f, hcy - cell * 0.205f),
            size = Size(cell * 0.49f, cell * 0.41f)
        )
    }
    rotate(degrees = angle, pivot = Offset(hcx, hcy - cell * 0.04f)) {
        drawOval(
            color = shine,
            topLeft = Offset(hcx - cell * 0.14f, hcy - cell * 0.04f - cell * 0.09f),
            size = Size(cell * 0.28f, cell * 0.18f)
        )
    }

    // tongue
    val tongue = Color(0xFFE5232E)
    val tx = hx + hux * cell * 0.27f
    val ty = hy + huy * cell * 0.27f
    val tipX = tx + hux * cell * 0.13f
    val tipY = ty + huy * cell * 0.13f
    drawLine(tongue, Offset(tx, ty), Offset(tipX, tipY), strokeWidth = cell * 0.03f, cap = StrokeCap.Round)
    for (s in listOf(-1f, 1f)) {
        drawLine(
            tongue,
            Offset(tipX, tipY),
            Offset(tx + hux * cell * 0.2f + hpx * cell * 0.05f * s, ty + huy * cell * 0.2f + hpy * cell * 0.05f * s),
            strokeWidth = cell * 0.03f,
            cap = StrokeCap.Round
        )
    }

    // eyes
    for (s in listOf(-1f, 1f)) {
        val ex = hx + hux * cell * 0.1f + hpx * cell * 0.115f * s
        val ey = hy + huy * cell * 0.1f + hpy * cell * 0.115f * s
        drawCircle(Color.White, cell * 0.075f, Offset(ex, ey))
        drawCircle(Color(0xFF111111), cell * 0.04f, Offset(ex + hux * cell * 0.02f, ey + huy * cell * 0.02f))
        drawCircle(
            Color.White,
            cell * 0.014f,
            Offset(ex + hux * cell * 0.025f - cell * 0.012f, ey + huy * cell * 0.025f - cell * 0.015f)
        )
    }
}

private fun DrawScope.drawNumbers(g: Geo) {
    val cell = g.cell
    val fill = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        textSize = cell * 0.27f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = AndroidPaint.Align.LEFT
    }
    val outline = AndroidPaint(fill).apply {
        style = AndroidPaint.Style.STROKE
        strokeWidth = cell * 0.08f
        strokeJoin = AndroidPaint.Join.ROUND
    }
    val ascent = fill.fontMetrics.ascent
    val canvas = drawContext.canvas.nativeCanvas
    for (n in 1..BoardData.LAST_SQUARE) {
        val c = g.center(n)
        val col = BoardData.column(n)
        val r = BoardData.rowFromBottom(n)
        val isWhite = (3 * r + col) % 5 == 1
        val x = c.x - cell / 2f + cell * 0.07f
        val baseline = (c.y - cell / 2f + cell * 0.05f) - ascent
        outline.color = if (isWhite) 0xE6FFFFFF.toInt() else 0xBF0F1932.toInt()
        fill.color = if (isWhite) 0xFF1B2A4A.toInt() else 0xFFFFFFFF.toInt()
        canvas.drawText(n.toString(), x, baseline, outline)
        canvas.drawText(n.toString(), x, baseline, fill)
    }
}
