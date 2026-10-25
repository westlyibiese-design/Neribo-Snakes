package com.westly.snakes.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.westly.snakes.ui.GameLayout
import com.westly.snakes.ui.Geo
import com.westly.snakes.ui.layers.DiceToss
import com.westly.snakes.ui.layers.PawnAnimator
import com.westly.snakes.ui.layers.runFrames
import com.westly.snakes.ui.snakeCurve
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** The two ways to play. Only [VS_COMPUTER] is playable in Phase 5. */
enum class GameMode { VS_COMPUTER, MULTIPLAYER }

/** Moments of the game that sounds and haptics will hook into later (nothing is connected yet). */
enum class GameEvent { DICE_ROLL, STEP, LADDER, SNAKE, NO_MOVE, WIN, BUTTON }

// Timings measured from the original game (milliseconds).
private const val WALK_MS = 500L           // per square: 2 squares per second
private const val CLIMB_MS = 1500L         // along the ladder
private const val SLIDE_MS = 1600L         // along the snake's body
private const val PULSE_MS = 700L          // halo ring around the Computer's pawn just before it moves
private const val PAUSE_AFTER_MOVE_MS = 700L
private const val OPPONENT_THINK_MS = 600L // Computer waits before its finger taps the tray
private const val HAND_PRESS_MS = 250L     // the finger pressing the Computer's tray
private const val GAME_OVER_DELAY_MS = 600L
private val CONTROLLER_PI = PI.toFloat()

/**
 * The turn machine of the game. One coroutine plays a whole round: it waits for the human's tap
 * (the Computer plays by itself), tosses the dice, applies [resolveMove] and animates the result,
 * then hands the turn to the other seat. All visible state is Compose state.
 *
 * Wins are kept in memory only (saving comes in a later phase).
 */
class GameController(
    private val scope: CoroutineScope,
    private val layout: GameLayout,
    private val animator: PawnAnimator,
    private val toss: DiceToss,
    val mode: GameMode = GameMode.VS_COMPUTER
) {
    /** Whose turn it is. */
    var activeSeat by mutableStateOf(Seat.HUMAN)
        private set

    /** True while the game waits for the human to tap the tray. */
    var waitingForHuman by mutableStateOf(false)
        private set

    /** True while the human's pawn glows and waits to be tapped (the dice are already thrown). */
    var waitingForPawn by mutableStateOf(false)
        private set

    /** True while the Game Over dialog is open. */
    var showGameOver by mutableStateOf(false)
        private set

    /** The winner of the round that just ended (null while a round is running). */
    var winner by mutableStateOf<Seat?>(null)
        private set

    var winsHuman by mutableIntStateOf(0)
        private set

    var winsOpponent by mutableIntStateOf(0)
        private set

    /** 0..1 while the Computer's finger presses its tray, 0 otherwise. */
    var handPress by mutableFloatStateOf(0f)
        private set

    /** Called at the moments listed in [GameEvent]. Nothing is connected to it in Phase 5. */
    var onEvent: (GameEvent) -> Unit = {}

    fun winsOf(seat: Seat): Int = if (seat == Seat.HUMAN) winsHuman else winsOpponent

    private var loopJob: Job? = null
    private val taps = Channel<Unit>(Channel.CONFLATED)
    private val pawnTaps = Channel<Unit>(Channel.CONFLATED)

    /** Starts the first round. */
    fun start() {
        beginRound()
    }

    /** The human tapped a tray. Only the human's own tray counts, and only while it waits for a tap. */
    fun onTrayTapped(seat: Seat) {
        if (seat != Seat.HUMAN) return
        if (!waitingForHuman || showGameOver) return
        waitingForHuman = false
        taps.trySend(Unit)
    }

    /** The human tapped the glowing pawn. Only counts while the pawn waits for that tap. */
    fun onPawnTapped(seat: Seat) {
        if (seat != Seat.HUMAN) return
        if (!waitingForPawn || showGameOver) return
        waitingForPawn = false
        pawnTaps.trySend(Unit)
    }

    /** Stops the running round for good (the screen that owns this controller is gone). */
    fun stop() {
        loopJob?.cancel()
        loopJob = null
        waitingForHuman = false
        waitingForPawn = false
    }

    /** The check button of the Game Over dialog: close it and start a fresh round. */
    fun dismissGameOver() {
        if (!showGameOver) return
        showGameOver = false
        beginRound()
    }

    /** Starts the round again without changing the Wins. */
    fun restartRound() {
        showGameOver = false
        beginRound()
    }

    // ------------------------------------------------------------------ round

    private fun beginRound() {
        val old = loopJob
        loopJob = scope.launch {
            old?.cancelAndJoin()
            taps.tryReceive() // forget any tap that arrived before the new round
            pawnTaps.tryReceive()
            resetBoard()
            playRound()
        }
    }

    private fun resetBoard() {
        // both pawns wait in their circles (square 0) until their first roll
        animator.placeAt(Seat.HUMAN, 0)
        animator.placeAt(Seat.OPPONENT, 0)
        toss.clearTray(true)
        toss.clearTray(false)
        winner = null
        waitingForHuman = false
        waitingForPawn = false
        handPress = 0f
        activeSeat = Seat.HUMAN
    }

    private suspend fun playRound() {
        var seat = Seat.HUMAN
        while (true) {
            activeSeat = seat
            showReadyDice(seat)
            if (seat == Seat.HUMAN) {
                waitingForHuman = true
                taps.receive()
                waitingForHuman = false
            } else {
                delay(OPPONENT_THINK_MS)
                pressHand()
            }
            val won = playTurn(seat)
            if (won) {
                finishRound(seat)
                return
            }
            delay(PAUSE_AFTER_MOVE_MS)
            seat = if (seat == Seat.HUMAN) Seat.OPPONENT else Seat.HUMAN
        }
    }

    /**
     * The active player's own tray shows two dice ready to throw and the other tray is empty:
     * my tray (bottom) when it is my turn, the Computer's tray (top) when it is the Computer's turn.
     */
    private fun showReadyDice(seat: Seat) {
        val inTopTray = seat == Seat.OPPONENT
        toss.clearTray(!inTopTray)
        toss.setTray(inTopTray, rollTwoDice())
    }

    private suspend fun pressHand() {
        try {
            runFrames(HAND_PRESS_MS) { t -> handPress = sin(CONTROLLER_PI * t) }
        } finally {
            handPress = 0f
        }
    }

    // ------------------------------------------------------------------- turn

    /** Plays one whole turn. Returns true when the pawn reached 100. */
    private suspend fun playTurn(seat: Seat): Boolean {
        onEvent(GameEvent.DICE_ROLL)
        val dice = rollTwoDice()
        toss.toss(seat == Seat.OPPONENT, dice)
        val total = dice.first + dice.second
        val position = animator.logicalSquare(seat)

        // The human must tap the glowing pawn before it moves; the Computer's pawn moves by itself.
        if (seat == Seat.HUMAN) waitForPawnTap(seat) else animator.pulse(seat, PULSE_MS)

        when (val result = resolveMove(position, total)) {
            is MoveResult.Walk -> {
                walkSquares(seat, position + 1, result.to)
            }
            is MoveResult.Climb -> {
                walkSquares(seat, position + 1, result.landing)
                onEvent(GameEvent.LADDER)
                animator.moveAlong(
                    seat,
                    listOf(layout.squareCenter(result.landing), layout.squareCenter(result.to)),
                    CLIMB_MS
                )
            }
            is MoveResult.Slide -> {
                walkSquares(seat, position + 1, result.landing)
                onEvent(GameEvent.SNAKE)
                animator.moveAlong(seat, slidePoints(result.landing, result.to), SLIDE_MS)
            }
            MoveResult.NoMove -> {
                onEvent(GameEvent.NO_MOVE)
                animator.shake(seat)
            }
            MoveResult.Win -> {
                walkSquares(seat, position + 1, BoardData.LAST_SQUARE)
                onEvent(GameEvent.WIN)
                return true
            }
        }
        return false
    }

    /** Makes the pawn glow and waits until the human taps it. */
    private suspend fun waitForPawnTap(seat: Seat) {
        waitingForPawn = true
        val glow = scope.launch { animator.highlight(seat) }
        try {
            pawnTaps.receive()
        } finally {
            waitingForPawn = false
            glow.cancel()
        }
        glow.join()
    }

    private suspend fun walkSquares(seat: Seat, first: Int, last: Int) {
        if (last < first) return
        animator.walkTo(seat, (first..last).toList(), WALK_MS) { onEvent(GameEvent.STEP) }
    }

    /** The snake's drawn body in screen coordinates, from the head to the tail. */
    private fun slidePoints(head: Int, tail: Int): List<Offset> {
        val curve = snakeCurve(Geo(layout.boardSize), head, tail)
        return curve.map { Offset(layout.boardLeft + it.x, layout.boardTop + it.y) }
    }

    private suspend fun finishRound(winnerSeat: Seat) {
        delay(GAME_OVER_DELAY_MS)
        winner = winnerSeat
        if (winnerSeat == Seat.HUMAN) winsHuman += 1 else winsOpponent += 1
        showGameOver = true
    }
}
