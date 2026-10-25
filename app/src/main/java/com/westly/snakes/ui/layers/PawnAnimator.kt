package com.westly.snakes.ui.layers

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import com.westly.snakes.game.Seat
import com.westly.snakes.ui.GameLayout
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

private const val HOP_HEIGHT_CELLS = 0.22f
private const val SHARE_OFFSET_CELLS = 0.2f
private const val SHAKE_AMPLITUDE_CELLS = 0.12f
private const val SHAKE_MS = 300L
private const val SHAKE_COUNT = 3
private const val HIGHLIGHT_MS = 700L
private val PI_F = PI.toFloat()

/**
 * Runs [onFrame] once per display frame for [durationMs] milliseconds.
 * The argument goes from 0 to 1 and the last call always receives exactly 1.
 */
internal suspend fun runFrames(durationMs: Long, onFrame: (Float) -> Unit) {
    if (durationMs <= 0L) {
        onFrame(1f)
        return
    }
    val durationNs = durationMs * 1_000_000L
    var startNs = -1L
    var finished = false
    while (!finished) {
        withFrameNanos { now ->
            if (startNs < 0L) startNs = now
            val elapsed = now - startNs
            if (elapsed >= durationNs) {
                finished = true
                onFrame(1f)
            } else {
                onFrame(elapsed.toFloat() / durationNs.toFloat())
            }
        }
    }
}

/** Animated state of one pawn. Positions are in the coordinates of GameLayout.squareCenter. */
internal class PawnPose {
    val square = mutableIntStateOf(1)
    val x = mutableFloatStateOf(0f)
    val y = mutableFloatStateOf(0f)
    val lift = mutableFloatStateOf(0f)
    val shift = mutableFloatStateOf(0f) // sideways shift in cells (sharing a square)
    val shakeX = mutableFloatStateOf(0f) // sideways shake in pixels
    val moving = mutableStateOf(false)
    val halo = mutableFloatStateOf(-1f) // -1 = no halo, otherwise 0..1 progress of the pre-move halo
}

/**
 * Holds, per seat, the logical square and the animated position of the pawn.
 * The caller must run only one animation per pawn at a time.
 */
class PawnAnimator(private val layout: GameLayout) {
    private val human = PawnPose()
    private val opponent = PawnPose()

    init {
        placeAt(Seat.HUMAN, 1)
        placeAt(Seat.OPPONENT, 1)
    }

    private fun pose(seat: Seat): PawnPose = if (seat == Seat.HUMAN) human else opponent

    private fun otherSeat(seat: Seat): Seat = if (seat == Seat.HUMAN) Seat.OPPONENT else Seat.HUMAN

    /** Red goes left, blue goes right when they share a square. */
    private fun shareShift(seat: Seat): Float =
        if (seat == Seat.HUMAN) -SHARE_OFFSET_CELLS else SHARE_OFFSET_CELLS

    /** Instant placement on a square (also refreshes the sharing shifts). */
    fun placeAt(seat: Seat, square: Int) {
        val p = pose(seat)
        val c = layout.squareCenter(square)
        p.square.intValue = square
        p.x.floatValue = c.x
        p.y.floatValue = c.y
        p.lift.floatValue = 0f
        p.shakeX.floatValue = 0f
        p.moving.value = false
        p.halo.floatValue = -1f
        refreshShifts()
    }

    fun logicalSquare(seat: Seat): Int = pose(seat).square.intValue

    private fun refreshShifts() {
        val same = human.square.intValue == opponent.square.intValue
        human.shift.floatValue = if (same) shareShift(Seat.HUMAN) else 0f
        opponent.shift.floatValue = if (same) shareShift(Seat.OPPONENT) else 0f
    }

    /**
     * One hop per listed square, [msPerSquare] each. Updates the logical square at the end.
     * [onStep] is called with each square the moment the pawn lands on it.
     */
    suspend fun walkTo(
        seat: Seat,
        squares: List<Int>,
        msPerSquare: Long = 100,
        onStep: (Int) -> Unit = {}
    ) {
        if (squares.isEmpty()) return
        val me = pose(seat)
        val otherSeat = otherSeat(seat)
        val other = pose(otherSeat)
        val n = squares.size
        val xs = FloatArray(n + 1)
        val ys = FloatArray(n + 1)
        xs[0] = me.x.floatValue
        ys[0] = me.y.floatValue
        for (i in 0 until n) {
            val c = layout.squareCenter(squares[i])
            xs[i + 1] = c.x
            ys[i + 1] = c.y
        }
        val dest = squares[n - 1]
        val sharing = other.square.intValue == dest
        val myStart = me.shift.floatValue
        val otherStart = other.shift.floatValue
        val myEnd = if (sharing) shareShift(seat) else 0f
        val otherEnd = if (sharing) shareShift(otherSeat) else 0f
        val hopLift = layout.cell * HOP_HEIGHT_CELLS

        me.moving.value = true
        try {
            var announced = 0
            runFrames(msPerSquare * n) { t ->
                val pos = t * n
                val landed = minOf(n, floor(pos).toInt())
                while (announced < landed) {
                    announced++
                    onStep(squares[announced - 1])
                }
                var i = floor(pos).toInt()
                if (i > n - 1) i = n - 1
                if (i < 0) i = 0
                val f = pos - i
                me.x.floatValue = xs[i] + (xs[i + 1] - xs[i]) * f
                me.y.floatValue = ys[i] + (ys[i + 1] - ys[i]) * f
                me.lift.floatValue = sin(PI_F * f) * hopLift

                val myFrom = if (i == 0) myStart else 0f
                val myTo = if (i == n - 1) myEnd else 0f
                me.shift.floatValue = myFrom + (myTo - myFrom) * f
                val otherFrom = if (i == 0) otherStart else 0f
                val otherTo = if (i == n - 1) otherEnd else 0f
                other.shift.floatValue = otherFrom + (otherTo - otherFrom) * f
            }
            me.x.floatValue = xs[n]
            me.y.floatValue = ys[n]
            me.square.intValue = dest
            me.shift.floatValue = myEnd
            other.shift.floatValue = otherEnd
        } finally {
            me.lift.floatValue = 0f
            me.moving.value = false
        }
    }

    /**
     * Smooth constant-speed travel through [points] (same coordinates as GameLayout.squareCenter).
     * If the last point is the centre of a square, that square becomes the logical square.
     */
    suspend fun moveAlong(seat: Seat, points: List<Offset>, durationMs: Long) {
        if (points.isEmpty()) return
        val me = pose(seat)
        val otherSeat = otherSeat(seat)
        val other = pose(otherSeat)
        val n = points.size
        val xs = FloatArray(n)
        val ys = FloatArray(n)
        val cum = FloatArray(n)
        for (i in 0 until n) {
            xs[i] = points[i].x
            ys[i] = points[i].y
            if (i > 0) {
                val dx = xs[i] - xs[i - 1]
                val dy = ys[i] - ys[i - 1]
                cum[i] = cum[i - 1] + sqrt(dx * dx + dy * dy)
            }
        }
        val total = cum[n - 1]
        val destSquare = squareAt(xs[n - 1], ys[n - 1])
        val sharing = destSquare != 0 && other.square.intValue == destSquare
        val myStart = me.shift.floatValue
        val otherStart = other.shift.floatValue
        val myEnd = if (sharing) shareShift(seat) else 0f
        val otherEnd = if (sharing) shareShift(otherSeat) else 0f

        me.moving.value = true
        try {
            me.x.floatValue = xs[0]
            me.y.floatValue = ys[0]
            var seg = 0
            runFrames(durationMs) { t ->
                if (n == 1 || total <= 0f) {
                    me.x.floatValue = xs[n - 1]
                    me.y.floatValue = ys[n - 1]
                } else {
                    val d = t * total
                    while (seg < n - 2 && cum[seg + 1] < d) seg++
                    val segLen = cum[seg + 1] - cum[seg]
                    val f = if (segLen <= 0f) 1f else ((d - cum[seg]) / segLen).coerceIn(0f, 1f)
                    me.x.floatValue = xs[seg] + (xs[seg + 1] - xs[seg]) * f
                    me.y.floatValue = ys[seg] + (ys[seg + 1] - ys[seg]) * f
                }
                me.shift.floatValue = blendShift(myStart, myEnd, t)
                other.shift.floatValue = blendShift(otherStart, otherEnd, t)
            }
            me.x.floatValue = xs[n - 1]
            me.y.floatValue = ys[n - 1]
            if (destSquare != 0) me.square.intValue = destSquare
            me.shift.floatValue = myEnd
            other.shift.floatValue = otherEnd
        } finally {
            me.lift.floatValue = 0f
            me.moving.value = false
        }
    }

    /** Leaves the shift in the first quarter, arrives at the final shift in the last quarter. */
    private fun blendShift(start: Float, end: Float, t: Float): Float {
        return if (t < 0.25f) {
            start * (1f - t / 0.25f)
        } else if (t > 0.75f) {
            end * ((t - 0.75f) / 0.25f)
        } else {
            0f
        }
    }

    /** The square whose centre is within half a cell of the point, or 0 when there is none. */
    private fun squareAt(x: Float, y: Float): Int {
        val limit = layout.cell * 0.5f
        for (s in 1..100) {
            val c = layout.squareCenter(s)
            val dx = c.x - x
            val dy = c.y - y
            if (dx * dx + dy * dy <= limit * limit) return s
        }
        return 0
    }

    /** Shows the halo ring around the pawn for [durationMs] (the pre-move highlight of the original game). */
    suspend fun pulse(seat: Seat, durationMs: Long) {
        val me = pose(seat)
        try {
            runFrames(durationMs) { t -> me.halo.floatValue = t }
        } finally {
            me.halo.floatValue = -1f
        }
    }

    /**
     * Pulses the halo ring around the pawn again and again until the caller cancels this call.
     * Used while the pawn waits for the player to tap it. The halo is removed when it ends.
     */
    suspend fun highlight(seat: Seat) {
        val me = pose(seat)
        try {
            while (true) {
                runFrames(HIGHLIGHT_MS) { t -> me.halo.floatValue = sin(PI_F * t) }
            }
        } finally {
            me.halo.floatValue = -1f
        }
    }

    /** Left and right 3 times in 300 ms, then back in place. */
    suspend fun shake(seat: Seat) {
        val me = pose(seat)
        val amplitude = layout.cell * SHAKE_AMPLITUDE_CELLS
        try {
            runFrames(SHAKE_MS) { t ->
                me.shakeX.floatValue = sin(2f * PI_F * SHAKE_COUNT * t) * amplitude
            }
        } finally {
            me.shakeX.floatValue = 0f
        }
    }

    // ----- read access for PawnLayer (read inside draw lambdas) -----

    internal fun isMoving(seat: Seat): Boolean = pose(seat).moving.value

    /** 0..1 while the pre-move halo is shown, -1 otherwise. */
    internal fun haloProgress(seat: Seat): Float = pose(seat).halo.floatValue

    /** Horizontal centre of the pawn on screen. */
    internal fun drawX(seat: Seat): Float {
        val p = pose(seat)
        return p.x.floatValue + p.shift.floatValue * layout.cell + p.shakeX.floatValue
    }

    /** Vertical position of the pawn's base on the ground (0.30 cell below the square centre). */
    internal fun groundY(seat: Seat): Float = pose(seat).y.floatValue + 0.30f * layout.cell

    /** Height of the current hop in pixels. */
    internal fun liftPx(seat: Seat): Float = pose(seat).lift.floatValue
}
