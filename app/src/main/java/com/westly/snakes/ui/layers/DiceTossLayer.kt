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
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// Timing measured from the original game: about 0.45 s of flight, a short rest, a quick return.
private const val FLIGHT_MS = 950L
private const val REST_MS = 400L
private const val RETURN_MS = 220L
private const val OUT_PART = 0.78f // share of the flight spent travelling out to the far point
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
 *
 * Like the original game, both dice are thrown from the roller's tray, swing out to either side
 * and land side by side in the middle of the board, then jump back into the tray.
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

    /**
     * Puts two dice at rest in a tray (the "ready" dice of the player whose turn it is).
     * Dice only ever rest in one tray at a time: filling a tray empties the other one.
     */
    fun setTray(top: Boolean, dice: Pair<Int, Int>?) {
        if (top) {
            topFaces.value = dice
            if (dice != null) bottomFaces.value = null
        } else {
            bottomFaces.value = dice
            if (dice != null) topFaces.value = null
        }
    }

    /** Plays the whole toss and leaves the faces in the roller's tray. */
    suspend fun toss(fromTop: Boolean, dice: Pair<Int, Int>) {
        val board = layout.boardSize
        val trayCentres = layout.diceCenters(fromTop)
        val startA = trayCentres.first
        val startB = trayCentres.second
        val rnd = Random.Default

        // landing: side by side in the middle of the board (the original always lands there)
        val cx = layout.boardLeft + 0.5f * board
        val cy = layout.boardTop + 0.5f * board
        val jitterY = (rnd.nextFloat() - 0.5f) * 0.02f * board
        val gap = 0.052f * board
        val landA = Offset(cx - gap, cy + jitterY)
        val landB = Offset(cx + gap, cy + jitterY)

        // far points: the dice swing out to opposite sides, then come back to the middle
        val side = if (rnd.nextBoolean()) 1f else -1f
        val dir = if (fromTop) -1f else 1f // the throw comes from below (bottom tray) or above (top tray)
        val apexA = Offset(cx + side * 0.31f * board, cy - dir * 0.05f * board)
        val apexB = Offset(cx - side * 0.245f * board, cy + dir * 0.07f * board)
        val ctrlA = bendPoint(startA, apexA, 0.05f * board * (if (rnd.nextBoolean()) 1f else -1f))
        val ctrlB = bendPoint(startB, apexB, 0.05f * board * (if (rnd.nextBoolean()) 1f else -1f))

        // spin 1.5 to 2 turns, slowing down until the dice lie upright
        val spinA = (900f + rnd.nextFloat() * 360f) * (if (rnd.nextBoolean()) 1f else -1f)
        val spinB = (900f + rnd.nextFloat() * 360f) * (if (rnd.nextBoolean()) 1f else -1f)

        val traySize = layout.diceSizeInTray
        val boardSize = layout.diceSizeOnBoard

        // the dice leave the tray when a toss begins; the other tray is emptied too
        topFaces.value = null
        bottomFaces.value = null
        dieA.value.intValue = dice.first
        dieB.value.intValue = dice.second
        setPlaced(dieA, startA, traySize, 0f)
        setPlaced(dieB, startB, traySize, 0f)
        shadowOn.value = true
        flying.value = true

        try {
            // 1. flight: out to the far points, then back to the middle (450 ms)
            runFrames(FLIGHT_MS) { t ->
                val slow = (1f - t) * (1f - t) * (1f - t) // spin slows to zero
                val rotA = spinA * slow
                val rotB = spinB * slow
                if (t < OUT_PART) {
                    val v = t / OUT_PART
                    val inv = 1f - v
                    val u = 1f - inv * inv * inv // ease-out
                    val bump = 1f + 0.22f * sin(TOSS_PI * v)
                    val size = (traySize + (boardSize - traySize) * u) * bump
                    setAlongCurve(dieA, startA, ctrlA, apexA, u, size, rotA)
                    setAlongCurve(dieB, startB, ctrlB, apexB, u, size, rotB)
                } else {
                    val v = (t - OUT_PART) / (1f - OUT_PART)
                    val u = v * v * (3f - 2f * v)
                    setPlaced(dieA, lerp(apexA, landA, u), boardSize, rotA)
                    setPlaced(dieB, lerp(apexB, landB, u), boardSize, rotB)
                }
            }
            setPlaced(dieA, landA, boardSize, 0f)
            setPlaced(dieB, landB, boardSize, 0f)

            // 2. a short rest with a tiny settle bounce (150 ms)
            runFrames(REST_MS) { t ->
                val size = boardSize * (1f + 0.07f * sin(TOSS_PI * t))
                setPlaced(dieA, landA, size, 0f)
                setPlaced(dieB, landB, size, 0f)
            }

            // 3. quick jump back into the roller's tray, showing the final faces (110 ms)
            shadowOn.value = false
            runFrames(RETURN_MS) { t ->
                val u = t * t * (3f - 2f * t)
                val size = boardSize + (traySize - boardSize) * u
                setPlaced(dieA, lerp(landA, startA, u), size, 0f)
                setPlaced(dieB, lerp(landB, startB, u), size, 0f)
            }
        } finally {
            // always leave a consistent state, even when the toss is cancelled
            setTray(fromTop, dice)
            flying.value = false
            shadowOn.value = true
        }
    }

    private fun lerp(a: Offset, b: Offset, u: Float): Offset =
        Offset(a.x + (b.x - a.x) * u, a.y + (b.y - a.y) * u)

    /** Control point of a gentle curve: the middle of the way pushed sideways by [bend] pixels. */
    private fun bendPoint(from: Offset, to: Offset, bend: Float): Offset {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy)
        if (len <= 0f) return from
        return Offset(
            (from.x + to.x) / 2f - dy / len * bend,
            (from.y + to.y) / 2f + dx / len * bend
        )
    }

    private fun setAlongCurve(die: FlyingDie, p0: Offset, c: Offset, p1: Offset, u: Float, size: Float, rot: Float) {
        val inv = 1f - u
        die.x.floatValue = inv * inv * p0.x + 2f * inv * u * c.x + u * u * p1.x
        die.y.floatValue = inv * inv * p0.y + 2f * inv * u * c.y + u * u * p1.y
        die.size.floatValue = size
        die.rotation.floatValue = rot
    }

    private fun setPlaced(die: FlyingDie, at: Offset, size: Float, rot: Float) {
        die.x.floatValue = at.x
        die.y.floatValue = at.y
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
