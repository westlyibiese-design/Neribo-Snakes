package com.westly.snakes.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.westly.snakes.ui.GameLayout
import com.westly.snakes.ui.Geo
import com.westly.snakes.ui.layers.DiceToss
import com.westly.snakes.ui.layers.PawnAnimator
import com.westly.snakes.ui.snakeCurve
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class GameMode { VS_COMPUTER, MULTIPLAYER }

enum class GameEvent { DICE_ROLL, STEP, LADDER, SNAKE, NO_MOVE, WIN, BUTTON }

private const val COMPUTER_WAIT_MS = 700L
private const val STEP_MS = 170L
private const val LADDER_MS = 800L
private const val SNAKE_MS = 900L
private const val TURN_GAP_MS = 450L
private const val GAME_OVER_DELAY_MS = 600L

/**
 * The turn machine. One coroutine ([turnJob]) plays the whole round, one turn after the other,
 * so two turns can never overlap. Only [GameMode.VS_COMPUTER] is playable in this phase.
 */
class GameController(
    private val scope: CoroutineScope,
    private val layout: GameLayout,
    private val animator: PawnAnimator,
    private val toss: DiceToss,
    val mode: GameMode = GameMode.VS_COMPUTER
) {
    /** Whose turn it is (the seat whose tray shows the hand). */
    var activeSeat by mutableStateOf(Seat.HUMAN)
        private set

    /** True only while the game waits for the human to tap the bottom tray. */
    var waitingForHuman by mutableStateOf(false)
        private set

    /** Round wins of the human (red, bottom). Kept in memory only. */
    var humanWins by mutableIntStateOf(0)
        private set

    /** Round wins of the opponent (blue, top). Kept in memory only. */
    var opponentWins by mutableIntStateOf(0)
        private set

    var showGameOver by mutableStateOf(false)
        private set

    var winner by mutableStateOf<Seat?>(null)
        private set

    /** Called at the moments listed in the turn flow. Nothing is connected to it in this phase. */
    var onEvent: (GameEvent) -> Unit = {}

    private var turnJob: Job? = null
    private var tapSignal: CompletableDeferred<Unit>? = null

    fun winsOf(seat: Seat): Int = if (seat == Seat.HUMAN) humanWins else opponentWins

    /** Starts the first round. Call it once, from an effect (not during composition). */
    fun start() {
        if (mode != GameMode.VS_COMPUTER) return
        if (turnJob != null) return
        turnJob = scope.launch { playRound() }
    }

    /** Stops everything (the screen is leaving). */
    fun stop() {
        turnJob?.cancel()
        turnJob = null
        tapSignal = null
        waitingForHuman = false
    }

    /** A tap on a tray. Ignored unless it is the human's turn and nothing is animating. */
    fun onTrayTapped(seat: Seat) {
        if (seat != Seat.HUMAN) return
        if (!waitingForHuman || showGameOver) return
        waitingForHuman = false
        tapSignal?.complete(Unit)
    }

    /** The check button of the Game Over dialog: starts the next round, Wins stay as they are. */
    fun dismissGameOver() {
        if (!showGameOver) return
        onEvent(GameEvent.BUTTON)
        restartRound()
    }

    /**
     * Both pawns back to square 1 (instant), both trays cleared, the human first.
     * Wins are not changed.
     */
    fun restartRound() {
        if (mode != GameMode.VS_COMPUTER) return
        waitingForHuman = false
        tapSignal = null
        showGameOver = false
        winner = null
        val old = turnJob
        turnJob = scope.launch {
            // wait until the old round has really stopped, so its clean-up cannot undo the reset
            old?.cancelAndJoin()
            animator.placeAt(Seat.HUMAN, 1)
            animator.placeAt(Seat.OPPONENT, 1)
            toss.clearTray(true)
            toss.clearTray(false)
            playRound()
        }
    }

    // ----- the round -----

    private suspend fun playRound() {
        var seat = Seat.HUMAN
        while (true) {
            activeSeat = seat
            if (seat == Seat.HUMAN) {
                awaitHumanTap()
            } else {
                delay(COMPUTER_WAIT_MS)
            }
            val roundWon = runTurn(seat)
            if (roundWon) return
            seat = if (seat == Seat.HUMAN) Seat.OPPONENT else Seat.HUMAN
        }
    }

    private suspend fun awaitHumanTap() {
        val signal = CompletableDeferred<Unit>()
        tapSignal = signal
        waitingForHuman = true
        try {
            signal.await()
        } finally {
            waitingForHuman = false
            tapSignal = null
        }
    }

    /** One whole turn. Returns true when the round was won (Game Over is then showing). */
    private suspend fun runTurn(seat: Seat): Boolean {
        onEvent(GameEvent.DICE_ROLL)
        val dice = rollTwoDice()
        toss.toss(fromTop = seat == Seat.OPPONENT, dice = dice)
        val total = dice.first + dice.second
        val pos = animator.logicalSquare(seat)

        when (val r = resolveMove(pos, total)) {
            is MoveResult.Walk -> {
                walkWithSteps(seat, squaresBetween(pos, r.to))
            }
            is MoveResult.Climb -> {
                walkWithSteps(seat, squaresBetween(pos, r.landing))
                onEvent(GameEvent.LADDER)
                val line = listOf(layout.squareCenter(r.landing), layout.squareCenter(r.to))
                animator.moveAlong(seat, line, LADDER_MS)
                animator.placeAt(seat, r.to)
            }
            is MoveResult.Slide -> {
                walkWithSteps(seat, squaresBetween(pos, r.landing))
                onEvent(GameEvent.SNAKE)
                val curve = snakeCurve(Geo(layout.boardSize), r.landing, r.to)
                val body = ArrayList<Offset>(curve.size)
                for (p in curve) {
                    body.add(Offset(p.x + layout.boardLeft, p.y + layout.boardTop))
                }
                animator.moveAlong(seat, body, SNAKE_MS)
                animator.placeAt(seat, r.to)
            }
            is MoveResult.NoMove -> {
                onEvent(GameEvent.NO_MOVE)
                animator.shake(seat)
            }
            is MoveResult.Win -> {
                walkWithSteps(seat, squaresBetween(pos, BoardData.LAST_SQUARE))
                onEvent(GameEvent.WIN)
                delay(GAME_OVER_DELAY_MS)
                if (seat == Seat.HUMAN) humanWins += 1 else opponentWins += 1
                winner = seat
                showGameOver = true
                return true
            }
        }

        delay(TURN_GAP_MS)
        return false
    }

    /** The squares after [from] up to and including [to]. */
    private fun squaresBetween(from: Int, to: Int): List<Int> {
        val out = ArrayList<Int>(to - from)
        for (s in (from + 1)..to) out.add(s)
        return out
    }

    /** Walks one hop per square and fires a STEP event as each square is landed on. */
    private suspend fun walkWithSteps(seat: Seat, squares: List<Int>) {
        if (squares.isEmpty()) return
        coroutineScope {
            val startNs = System.nanoTime()
            val count = squares.size
            launch {
                for (i in 1..count) {
                    val dueNs = startNs + i * STEP_MS * 1_000_000L
                    val waitMs = (dueNs - System.nanoTime()) / 1_000_000L
                    if (waitMs > 0L) delay(waitMs)
                    onEvent(GameEvent.STEP)
                }
            }
            animator.walkTo(seat, squares, STEP_MS)
        }
    }
}
