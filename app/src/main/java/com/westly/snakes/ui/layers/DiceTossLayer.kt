package com.westly.snakes.ui.layers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.westly.snakes.ui.GameLayout
import com.westly.snakes.ui.components.drawDie
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val FLIGHT_MS = 900L
private const val REST_MS = 350L
private const val RETURN_MS = 250L
private const val FLIGHT_PART = 0.78f // share of the 900 ms used for the flight, the rest is the bounce
private val TOSS_PI = PI.toFloat()

/** Animated state of one die that is in the air or lying on the board. */
internal class FlyingDie {
    val x = mutableFloatStateOf(0f)
    val y = mutableFloatStateOf(0f)
    val size = mutableFloatStateOf(0f)
    val rotation = mutableFloatStateOf(0f)
    val value = mutableIntStateOf(1)
}

/**
 * Plays the dice toss and remembers the faces resting in each tray.
 * Only one toss may run at a time (the caller guards this).
 */
class DiceToss(private val layout: GameLayout) {
    internal val dieA = FlyingDie()
    internal val dieB = FlyingDie()
    internal val flying = mutableStateOf(false)
    internal val shadowOn = mutableStateOf(true)

    private val topFaces = mutableStateOf<Pair<Int, Int>?>(null)
    private val bottomFaces = mutableStateOf<Pair<Int, Int>?>(null)

    fun trayDice(top: Boolean): Pair<Int, Int>? = if (top) topFaces.value else bottomFaces.value

    fun clearTray(top: Boolean) {
        if (top) topFaces.value = null else bottomFaces.value = null
    }

    /** Plays the whole toss and leaves the faces in the roller's tray. */
    suspend fun toss(fromTop: Boolean, dice: Pair<Int, Int>) {
        val board = layout.boardSize
        val trayCentres = layout.diceCenters(fromTop)
        val startA = trayCentres.first
        val startB = trayCentres.second

        // landing spots inside the central 60% of the board, at least 0.14W apart
        val rnd = Random.Default
        var landA = randomSpot(rnd, board)
        var landB = randomSpot(rnd, board)
        var tries = 0
        while (distance(landA, landB) < 0.14f * board && tries < 200) {
            landB = randomSpot(rnd, board)
            tries++
        }
        if (distance(landA, landB) < 0.14f * board) {
            landA = Offset(layout.boardLeft + 0.35f * board, layout.boardTop + 0.5f * board)
            landB = Offset(layout.boardLeft + 0.65f * board, layout.boardTop + 0.5f * board)
        }

        // spin 1 to 2 turns in a random direction, curved paths with a random bend side
        val spinA = (360f + rnd.nextFloat() * 360f) * (if (rnd.nextBoolean()) 1f else -1f)
        val spinB = (360f + rnd.nextFloat() * 360f) * (if (rnd.nextBoolean()) 1f else -1f)
        val bendA = (0.18f + rnd.nextFloat() * 0.14f) * (if (rnd.nextBoolean()) 1f else -1f)
        val bendB = (0.18f + rnd.nextFloat() * 0.14f) * (if (rnd.nextBoolean()) 1f else -1f)

        val ctrlA = controlPoint(startA, landA, bendA)
        val ctrlB = controlPoint(startB, landB, bendB)
        val restRotA = round(spinA / 360f) * 360f
        val restRotB = round(spinB / 360f) * 360f

        val traySize = layout.diceSizeInTray
        val boardSize = layout.diceSizeOnBoard
        val bounceHeight = 0.03f * board

        // the faces of the previous roll leave both trays when a toss begins
        topFaces.value = null
        bottomFaces.value = null
        dieA.value.intValue = dice.first
        dieB.value.intValue = dice.second
        setPlaced(dieA, startA, 0f, traySize, 0f)
        setPlaced(dieB, startB, 0f, traySize, 0f)
        shadowOn.value = true
        flying.value = true

        try {
            // 1. flight and bounce, 900 ms
            runFrames(FLIGHT_MS) { t ->
                if (t < FLIGHT_PART) {
                    val v = t / FLIGHT_PART
                    val inv = 1f - v
                    val u = 1f - inv * inv * inv // ease-out
                    val bump = 1f + 0.28f * sin(TOSS_PI * v)
                    val size = (traySize + (boardSize - traySize) * u) * bump
                    setFlight(dieA, startA, ctrlA, landA, u, size, spinA * u)
                    setFlight(dieB, startB, ctrlB, landB, u, size, spinB * u)
                } else {
                    val s = (t - FLIGHT_PART) / (1f - FLIGHT_PART)
                    val hop = sin(TOSS_PI * s) * (1f - s)
                    val size = boardSize * (1f + 0.10f * hop)
                    setPlaced(dieA, landA, hop * bounceHeight, size, spinA)
                    setPlaced(dieB, landB, hop * bounceHeight, size, spinB)
                }
            }
            setPlaced(dieA, landA, 0f, boardSize, spinA)
            setPlaced(dieB, landB, 0f, boardSize, spinB)

            // 2. rest on the board
            delay(REST_MS)

            // 3. move into the roller's tray at its resting size, showing the final faces
            shadowOn.value = false
            runFrames(RETURN_MS) { t ->
                val u = t * t * (3f - 2f * t)
                val size = boardSize + (traySize - boardSize) * u
                setPlaced(dieA, lerp(landA, startA, u), 0f, size, spinA + (restRotA - spinA) * u)
                setPlaced(dieB, lerp(landB, startB, u), 0f, size, spinB + (restRotB - spinB) * u)
            }
        } finally {
            // always leave a consistent state, even when the toss is cancelled
            if (fromTop) topFaces.value = dice else bottomFaces.value = dice
            flying.value = false
            shadowOn.value = true
        }
    }

    private fun randomSpot(rnd: Random, board: Float): Offset {
        val fx = 0.2f + rnd.nextFloat() * 0.6f
        val fy = 0.2f + rnd.nextFloat() * 0.6f
        return Offset(layout.boardLeft + fx * board, layout.boardTop + fy * board)
    }

    private fun distance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun lerp(a: Offset, b: Offset, u: Float): Offset =
        Offset(a.x + (b.x - a.x) * u, a.y + (b.y - a.y) * u)

    /** Bend point of the curved path: the middle of the way pushed sideways by [bend]. */
    private fun controlPoint(from: Offset, to: Offset, bend: Float): Offset {
        val dx = to.x - from.x
        val dy = to.y - from.y
        return Offset(
            (from.x + to.x) / 2f - dy * bend * 2f,
            (from.y + to.y) / 2f + dx * bend * 2f
        )
    }

    private fun setFlight(die: FlyingDie, p0: Offset, c: Offset, p1: Offset, u: Float, size: Float, rot: Float) {
        val inv = 1f - u
        die.x.floatValue = inv * inv * p0.x + 2f * inv * u * c.x + u * u * p1.x
        die.y.floatValue = inv * inv * p0.y + 2f * inv * u * c.y + u * u * p1.y
        die.size.floatValue = size
        die.rotation.floatValue = rot
    }

    private fun setPlaced(die: FlyingDie, at: Offset, lift: Float, size: Float, rot: Float) {
        die.x.floatValue = at.x
        die.y.floatValue = at.y - lift
        die.size.floatValue = size
        die.rotation.floatValue = rot
    }
}

/** Draws the dice resting in the trays and the dice flying over the board. Ignores all touches. */
@Composable
fun DiceTossLayer(layout: GameLayout, toss: DiceToss) {
    val topCentres = remember(layout) { layout.diceCenters(true) }
    val bottomCentres = remember(layout) { layout.diceCenters(false) }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val traySide = layout.diceSizeInTray
        val top = toss.trayDice(true)
        if (top != null) {
            drawDie(topCentres.first, traySide, top.first)
            drawDie(topCentres.second, traySide, top.second)
        }
        val bottom = toss.trayDice(false)
        if (bottom != null) {
            drawDie(bottomCentres.first, traySide, bottom.first)
            drawDie(bottomCentres.second, traySide, bottom.second)
        }
        if (toss.flying.value) {
            val shadow = toss.shadowOn.value
            drawFlyingDie(toss.dieA, shadow)
            drawFlyingDie(toss.dieB, shadow)
        }
    }
}

private fun DrawScope.drawFlyingDie(die: FlyingDie, shadow: Boolean) {
    drawDie(
        center = Offset(die.x.floatValue, die.y.floatValue),
        side = die.size.floatValue,
        value = die.value.intValue,
        rotationDegrees = die.rotation.floatValue,
        shadow = shadow
    )
}
