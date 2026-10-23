package com.westly.snakes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westly.snakes.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// SnakeMate animated splash. One clock (seconds since start) drives everything.
// Plays the intro, keeps looping gentle motion, then fades out and calls onFinished.
// ---------------------------------------------------------------------------

private const val TOTAL_MS = 3600L
private const val FADE_START = 3.2f
private const val FADE_DUR = 0.4f

private val TWO_PI = (2.0 * PI).toFloat()
private val GOLD = Color(0xFFFFD23F)
private val BG_FLAT = Color(0xFF0A4AAB)

private class DieSpec(
    val leftPct: Float,
    val sizeDp: Float,
    val period: Float,
    val delay: Float,
    val face: Int,
    val rotDeg: Float
)

private class StarSpec(
    val xPct: Float,
    val yPct: Float,
    val sizeDp: Float,
    val period: Float,
    val delay: Float
)

private val DICE = listOf(
    DieSpec(7f, 30f, 13f, 0.5f, 5, 300f),
    DieSpec(90f, 36f, 16f, 3f, 3, -260f),
    DieSpec(24f, 22f, 14f, 6f, 6, 220f),
    DieSpec(72f, 26f, 17f, 1.5f, 2, -320f),
    DieSpec(48f, 32f, 15f, 8f, 4, 280f),
    DieSpec(84f, 20f, 12f, 10f, 1, -200f),
    DieSpec(14f, 34f, 18f, 11f, 4, 240f)
)

private val STARS = listOf(
    StarSpec(16f, 30f, 18f, 3.4f, 1.6f),
    StarSpec(80f, 24f, 14f, 3.8f, 2.3f),
    StarSpec(86f, 60f, 20f, 4.2f, 3.0f),
    StarSpec(12f, 68f, 14f, 3.6f, 2.7f),
    StarSpec(50f, 10f, 16f, 4.0f, 3.6f),
    StarSpec(68f, 84f, 14f, 3.5f, 1.9f)
)

// pip positions on a die face (x, y as fractions of the die)
private val PIP_TL = Pair(0.27f, 0.27f)
private val PIP_TR = Pair(0.73f, 0.27f)
private val PIP_ML = Pair(0.27f, 0.50f)
private val PIP_MR = Pair(0.73f, 0.50f)
private val PIP_BL = Pair(0.27f, 0.73f)
private val PIP_BR = Pair(0.73f, 0.73f)
private val PIP_C = Pair(0.50f, 0.50f)

private val PIPS = listOf(
    listOf(PIP_C),
    listOf(PIP_TL, PIP_BR),
    listOf(PIP_TL, PIP_C, PIP_BR),
    listOf(PIP_TL, PIP_TR, PIP_BL, PIP_BR),
    listOf(PIP_TL, PIP_TR, PIP_C, PIP_BL, PIP_BR),
    listOf(PIP_TL, PIP_TR, PIP_ML, PIP_MR, PIP_BL, PIP_BR)
)

private fun ease(p: Float): Float {
    val c = p.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

private fun easeOut(p: Float): Float {
    val q = 1f - p.coerceIn(0f, 1f)
    return 1f - q * q * q
}

private fun seg(t: Float, start: Float, dur: Float): Float =
    ((t - start) / dur).coerceIn(0f, 1f)

private fun mix(a: Float, b: Float, p: Float): Float = a + (b - a) * p

private fun logoScale(t: Float): Float {
    val p = seg(t, 0.25f, 1.25f)
    return when {
        p < 0.55f -> mix(0.55f, 1.07f, easeOut(p / 0.55f))
        p < 0.78f -> mix(1.07f, 0.985f, ease((p - 0.55f) / 0.23f))
        else -> mix(0.985f, 1f, ease((p - 0.78f) / 0.22f))
    }
}

private fun DrawScope.drawDie(x: Float, y: Float, s: Float, face: Int, alpha: Float) {
    drawRoundRect(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(x, y),
        size = Size(s, s),
        cornerRadius = CornerRadius(s * 0.24f, s * 0.24f)
    )
    val pip = Color(0xFF0B3A8A).copy(alpha = alpha)
    val r = s * 0.09f
    for (pos in PIPS[face - 1]) {
        drawCircle(color = pip, radius = r, center = Offset(x + s * pos.first, y + s * pos.second))
    }
}

private fun starPath(c: Offset, sz: Float): Path {
    val pts = floatArrayOf(
        50f, 0f, 62f, 38f, 100f, 50f, 62f, 62f,
        50f, 100f, 38f, 62f, 0f, 50f, 38f, 38f
    )
    val path = Path()
    for (i in 0 until 8) {
        val x = c.x + (pts[i * 2] - 50f) / 100f * sz
        val y = c.y + (pts[i * 2 + 1] - 50f) / 100f * sz
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

// light band that sweeps across the logo now and then
private fun DrawScope.drawShine(t: Float) {
    if (t < 2.2f) return
    val p = ((t - 2.2f) % 4.2f) / 4.2f
    val s = min(p / 0.38f, 1f)
    val a = when {
        p < 0.08f -> p / 0.08f
        p < 0.38f -> 1f
        p < 0.42f -> 1f - (p - 0.38f) / 0.04f
        else -> 0f
    }
    if (a <= 0f) return
    val bw = size.width * 0.34f
    val x = bw * (-1.6f + 4.9f * ease(s))
    val top = -size.height * 0.1f
    val bottom = size.height * 1.1f
    val dx = (bottom - top) * 0.3249f
    val path = Path()
    path.moveTo(x + dx, top)
    path.lineTo(x + bw + dx, top)
    path.lineTo(x + bw, bottom)
    path.lineTo(x, bottom)
    path.close()
    val brush = Brush.horizontalGradient(
        0f to Color.White.copy(alpha = 0f),
        0.5f to Color.White.copy(alpha = 0.42f * a),
        1f to Color.White.copy(alpha = 0f),
        startX = x,
        endX = x + bw + dx
    )
    drawPath(path = path, brush = brush, blendMode = BlendMode.Screen)
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val clock = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> clock.floatValue = (now - start) / 1_000_000_000f }
        }
    }
    LaunchedEffect(Unit) {
        delay(TOTAL_MS)
        onFinished()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - ease(seg(clock.floatValue, FADE_START, FADE_DUR)) }
    ) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val logoPx = min(wPx * 0.92f, with(density) { 420.dp.toPx() })
        val blockPx = logoPx + with(density) { 50.dp.toPx() }
        val logoTop = hPx * 0.42f - blockPx / 2f
        val logoLeft = (wPx - logoPx) / 2f
        val cx = wPx / 2f
        val cy = logoTop + logoPx / 2f
        val logoDp = with(density) { logoPx.toDp() }
        val overlapPx = with(density) { min(wPx * 0.09f, 36.dp.toPx()) }

        // background, light rays, glow, rings and drifting dice
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = clock.floatValue
            val w = size.width
            val h = size.height

            drawRect(color = BG_FLAT)
            val bgAlpha = ease(seg(t, 0f, 0.6f))
            val bgRadius = sqrt((0.5f * w) * (0.5f * w) + (0.58f * h) * (0.58f * h))
            drawRect(
                brush = Brush.radialGradient(
                    0f to Color(0xFF0F5FC9),
                    0.38f to Color(0xFF0A4AAB),
                    0.66f to Color(0xFF094197),
                    1f to Color(0xFF072D6E),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = bgRadius
                ),
                alpha = bgAlpha
            )

            // rotating light rays
            val rayAlpha = ease(seg(t, 0.1f, 1.6f))
            val rayR = max(w, h) * 0.4f
            val rayBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.055f * rayAlpha),
                    Color.White.copy(alpha = 0f)
                ),
                center = Offset(cx, cy),
                radius = rayR
            )
            rotate(degrees = t * 9f, pivot = Offset(cx, cy)) {
                for (i in 0 until 16) {
                    drawArc(
                        brush = rayBrush,
                        startAngle = i * 22.5f,
                        sweepAngle = 7f,
                        useCenter = true,
                        topLeft = Offset(cx - rayR, cy - rayR),
                        size = Size(rayR * 2f, rayR * 2f)
                    )
                }
            }

            // drifting dice
            for (d in DICE) {
                if (t < d.delay) continue
                val p = ((t - d.delay) % d.period) / d.period
                val a = when {
                    p < 0.12f -> p / 0.12f
                    p > 0.85f -> (1f - p) / 0.15f
                    else -> 1f
                } * 0.34f
                val s = d.sizeDp.dp.toPx()
                val x = w * d.leftPct / 100f
                val y = h - p * 1.18f * h
                rotate(degrees = p * d.rotDeg, pivot = Offset(x + s / 2f, y + s / 2f)) {
                    drawDie(x, y, s, d.face, a)
                }
            }

            // glow behind the logo
            val gp = seg(t, 0.15f, 1.1f)
            val breathe = if (t > 1.6f) cos(TWO_PI * (((t - 1.6f) % 3.6f) / 3.6f)) else 1f
            val glowAlpha = ease(gp) * (0.86f + 0.14f * breathe)
            val glowScale = (0.6f + 0.4f * ease(gp)) * (1.035f - 0.035f * breathe)
            val glowR = logoPx * 0.44f * glowScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF46A0FF).copy(alpha = 0.55f * glowAlpha),
                        Color(0xFF46A0FF).copy(alpha = 0f)
                    ),
                    center = Offset(cx, cy),
                    radius = glowR
                ),
                radius = glowR,
                center = Offset(cx, cy)
            )

            // pulse rings
            for (k in 0..1) {
                val start = 0.85f + k * 1.6f
                if (t >= start) {
                    val p = ((t - start) % 3.2f) / 3.2f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f * (1f - p)),
                        radius = logoPx * 0.31f * (0.7f + 1.05f * p),
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // the logo
        Image(
            painter = painterResource(R.drawable.splash_logo),
            contentDescription = null,
            modifier = Modifier
                .offset { IntOffset(logoLeft.roundToInt(), logoTop.roundToInt()) }
                .size(logoDp)
                .graphicsLayer {
                    val t = clock.floatValue
                    alpha = ease(seg(t, 0.25f, 0.7f))
                    val sc = logoScale(t)
                    scaleX = sc
                    scaleY = sc
                    val w = ease(seg(t, 1.7f, 0.8f))
                    val ph = (max(t - 1.7f, 0f) % 4.4f) / 4.4f
                    translationY = (1f - easeOut(seg(t, 0.25f, 1.0f))) * 26.dp.toPx() -
                        w * 5.5f.dp.toPx() * (1f - cos(TWO_PI * ph))
                    rotationZ = w * 1.8f * sin(TWO_PI * ph)
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawShine(clock.floatValue)
                    // soft round edge so the logo blends into the background
                    drawRect(
                        brush = Brush.radialGradient(
                            0f to Color.Black,
                            0.82f to Color.Black,
                            1f to Color.Transparent,
                            center = Offset(size.width * 0.5f, size.height * 0.54f),
                            radius = size.width * 0.46f
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        )

        // twinkling stars around the logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = clock.floatValue
            for (st in STARS) {
                if (t < st.delay) continue
                val p = ((t - st.delay) % st.period) / st.period
                val op = when {
                    p < 0.45f -> p / 0.45f
                    p < 0.70f -> 1f - (p - 0.45f) / 0.25f
                    else -> 0f
                }
                if (op <= 0f) continue
                val sc = when {
                    p < 0.45f -> mix(0.2f, 1f, p / 0.45f)
                    else -> mix(1f, 0.3f, ((p - 0.45f) / 0.25f).coerceIn(0f, 1f))
                }
                val full = st.sizeDp.dp.toPx()
                val c = Offset(
                    logoLeft + logoPx * st.xPct / 100f + full / 2f,
                    logoTop + logoPx * st.yPct / 100f + full / 2f
                )
                rotate(degrees = p * 120f, pivot = c) {
                    drawPath(path = starPath(c, full * sc), color = Color.White.copy(alpha = op))
                }
            }
        }

        // name + gold line
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (logoTop + logoPx - overlapPx).roundToInt()) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                "SnakeMate".forEachIndexed { i, ch ->
                    Text(
                        text = ch.toString(),
                        color = if (i >= 5) GOLD else Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0x73031E3C),
                                offset = Offset(0f, 4f),
                                blurRadius = 14f
                            )
                        ),
                        modifier = Modifier.graphicsLayer {
                            val t = clock.floatValue
                            val p = seg(t, 1.25f + i * 0.055f, 0.75f)
                            alpha = easeOut(p)
                            var y = (1f - easeOut(p)) * 26.dp.toPx()
                            val ws = 2.5f + i * 0.11f
                            if (t > ws) {
                                val ph = ((t - ws) % 3f) / 3f
                                if (ph < 0.6f) {
                                    y -= 6.dp.toPx() * sin(PI.toFloat() * ph / 0.6f)
                                }
                            }
                            translationY = y
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(3.dp)
                    .graphicsLayer { scaleX = ease(seg(clock.floatValue, 1.95f, 0.8f)) }
                    .background(
                        Brush.horizontalGradient(
                            listOf(GOLD.copy(alpha = 0f), GOLD, GOLD.copy(alpha = 0f))
                        )
                    )
            )
        }

        // copyright near the bottom
        Text(
            text = "\u00A9 Neribo Group",
            color = Color.White.copy(alpha = 0.74f),
            fontSize = 12.5.sp,
            letterSpacing = 1.1.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 26.dp)
                .graphicsLayer {
                    val p = seg(clock.floatValue, 2.2f, 1.0f)
                    alpha = easeOut(p)
                    translationY = (1f - easeOut(p)) * 8.dp.toPx()
                }
        )
    }
}
