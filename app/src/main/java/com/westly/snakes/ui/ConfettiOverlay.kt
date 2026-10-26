package com.westly.snakes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.westly.snakes.ui.layers.runFrames
import kotlin.math.sin
import kotlin.random.Random

private const val PIECE_COUNT = 60
private const val FIELDS = 6 // x, start y, speed, size, spin, phase
private const val DURATION_MS = 1500L
private const val GRAVITY = 1.1f

private val ConfettiColors = arrayOf(
    Color(0xFFFF4B2B),
    Color(0xFFFFD23F),
    Color(0xFF3DFF30),
    Color(0xFF4FE3F0),
    Color(0xFFFF7DB0),
    Color(0xFFFFFFFF)
)

/**
 * About 60 coloured paper pieces falling with gravity and spinning for 1.5 seconds. It is drawn
 * over the whole screen and does not take touches. The pieces come from a fixed random seed, so
 * nothing is allocated while it plays.
 */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val progress = remember { mutableFloatStateOf(0f) }
    val pieces = remember { buildPieces() }
    LaunchedEffect(Unit) {
        runFrames(DURATION_MS) { t -> progress.floatValue = t }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.floatValue
        if (t >= 1f) return@Canvas
        val fade = if (t > 0.8f) (1f - t) / 0.2f else 1f
        for (i in 0 until PIECE_COUNT) {
            val b = i * FIELDS
            val x = (pieces[b] + 0.04f * sin(pieces[b + 5] + t * 8f)) * size.width
            val y = (pieces[b + 1] + pieces[b + 2] * t + 0.5f * GRAVITY * t * t) * size.height
            val side = pieces[b + 3] * size.width
            val color = ConfettiColors[i % ConfettiColors.size].copy(alpha = fade)
            rotate(degrees = pieces[b + 4] * t, pivot = Offset(x, y)) {
                drawRect(
                    color = color,
                    topLeft = Offset(x - side / 2f, y - side * 0.3f),
                    size = Size(side, side * 0.6f)
                )
            }
        }
    }
}

private fun buildPieces(): FloatArray {
    val random = Random(11)
    val out = FloatArray(PIECE_COUNT * FIELDS)
    for (i in 0 until PIECE_COUNT) {
        val b = i * FIELDS
        out[b] = random.nextFloat()
        out[b + 1] = -0.25f + random.nextFloat() * 0.22f
        out[b + 2] = 0.15f + random.nextFloat() * 0.30f
        out[b + 3] = 0.018f + random.nextFloat() * 0.014f
        out[b + 4] = (if (random.nextBoolean()) 1f else -1f) * (360f + random.nextFloat() * 720f)
        out[b + 5] = random.nextFloat() * 6.2831855f
    }
    return out
}
