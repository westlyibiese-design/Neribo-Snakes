package com.westly.snakes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.westly.snakes.game.GameController
import com.westly.snakes.game.Seat
import com.westly.snakes.ui.components.ButtonIcon
import com.westly.snakes.ui.components.LabelAlign
import com.westly.snakes.ui.components.PawnColor
import com.westly.snakes.ui.components.drawHand
import com.westly.snakes.ui.components.drawOutlinedText
import com.westly.snakes.ui.components.drawPawn
import com.westly.snakes.ui.components.drawRoundButton
import com.westly.snakes.ui.components.drawTurnPill
import com.westly.snakes.ui.components.drawWoodCircle
import com.westly.snakes.ui.components.drawWoodTray
import com.westly.snakes.ui.layers.DiceToss
import com.westly.snakes.ui.layers.DiceTossLayer
import com.westly.snakes.ui.layers.PawnAnimator
import com.westly.snakes.ui.layers.PawnLayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Phase 5: the game screen around the board, now driven by the GameController:
// turns, dice, snakes, ladders, the Computer player, winning and the Game Over dialog.
// ---------------------------------------------------------------------------

private const val TEXTURE_COUNT = 300
private const val HAND_PERIOD_MS = 700L
private val HAND_TWO_PI = (2.0 * PI).toFloat()

@Composable
fun GameScreen(
    topName: String = "Computer",
    bottomName: String = "Me"
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dens = LocalDensity.current
        val pxPerDp = dens.density
        val widthPx = with(dens) { maxWidth.toPx() }
        val heightPx = with(dens) { maxHeight.toPx() }
        val topInsetPx = WindowInsets.statusBars.getTop(dens).toFloat()
        val bottomInsetPx = WindowInsets.navigationBars.getBottom(dens).toFloat()

        val layout = remember(widthPx, heightPx, topInsetPx, bottomInsetPx, pxPerDp) {
            GameLayout(widthPx, heightPx, topInsetPx, bottomInsetPx, pxPerDp)
        }
        val texture = remember(widthPx, heightPx, pxPerDp) {
            buildTexture(widthPx, heightPx, pxPerDp)
        }
        val animator = remember(layout) { PawnAnimator(layout) }
        val toss = remember(layout) { DiceToss(layout) }
        val scope = rememberCoroutineScope()
        val controller = remember(layout) { GameController(scope, layout, animator, toss) }
        DisposableEffect(controller) {
            controller.start()
            onDispose { controller.stop() }
        }

        // 1. background gradient and texture
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(GameColors.BackgroundTop, GameColors.BackgroundBottom)
                )
            )
            drawTexture(texture, pxPerDp)
        }

        // 2. the board (Phase 2 drawing, unchanged)
        BoardCanvas(
            modifier = Modifier
                .size(with(dens) { layout.boardSize.toDp() })
                .graphicsLayer {
                    translationX = layout.boardLeft
                    translationY = layout.boardTop
                }
        )

        // 3. trays, circles, buttons, labels, texts and the Your Turn pill
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = layout.boardSize

            // trays
            drawWoodTray(layout.trayRect(true), w)
            drawWoodTray(layout.trayRect(false), w)

            // token slot and counter circles
            drawWoodCircle(layout.topSlotCenter, layout.circleDiameter)
            drawWoodCircle(layout.topCounterCenter, layout.circleDiameter)
            drawWoodCircle(layout.bottomSlotCenter, layout.circleDiameter)
            drawWoodCircle(layout.bottomCounterCenter, layout.circleDiameter)

            // each pawn waits in its own circle until its first roll; once on the board the circle is empty
            val pawnHeight = w * 0.095f
            if (animator.inCircle(Seat.OPPONENT)) {
                drawPawn(
                    baseCenter = Offset(layout.topSlotCenter.x, layout.topSlotCenter.y + 0.385f * pawnHeight),
                    height = pawnHeight,
                    color = PawnColor.BLUE
                )
            }
            if (animator.inCircle(Seat.HUMAN)) {
                drawPawn(
                    baseCenter = Offset(layout.bottomSlotCenter.x, layout.bottomSlotCenter.y + 0.385f * pawnHeight),
                    height = pawnHeight,
                    color = PawnColor.RED
                )
            }

            // counters
            drawOutlinedText("0", layout.topCounterCenter, w * 0.075f, GameColors.Cream, GameColors.InnerShadow)
            drawOutlinedText("0", layout.bottomCounterCenter, w * 0.075f, GameColors.Cream, GameColors.InnerShadow)

            // buttons
            drawRoundButton(layout.homeCenter, layout.roundButtonDiameter, ButtonIcon.HOME)
            drawRoundButton(layout.menuCenter, layout.roundButtonDiameter, ButtonIcon.MENU)

            // name labels
            drawOutlinedText(
                topName, layout.topNameCenter, w * 0.072f,
                GameColors.NameGreen, GameColors.NameGreenOutline, LabelAlign.CENTER
            )
            drawOutlinedText(
                bottomName, layout.bottomNameCenter, w * 0.072f,
                GameColors.NameGreen, GameColors.NameGreenOutline, LabelAlign.CENTER
            )

            // Wins and Rank
            drawOutlinedText(
                "Wins: ${controller.winsOpponent}", layout.topWinsAnchor, w * 0.065f,
                GameColors.Cream, GameColors.DarkBlueOutline, LabelAlign.LEFT
            )
            drawOutlinedText(
                "Rank: 0", layout.bottomRankAnchor, w * 0.065f,
                GameColors.Cream, GameColors.DarkBlueOutline, LabelAlign.LEFT
            )
            drawOutlinedText(
                "Wins: ${controller.winsHuman}", layout.bottomWinsAnchor, w * 0.065f,
                GameColors.Cream, GameColors.DarkBlueOutline, LabelAlign.LEFT
            )

            // Your Turn pill: during the human's whole turn
            if (controller.activeSeat == Seat.HUMAN) {
                drawTurnPill(layout.pillRect, w)
            }
        }

        // 3b. pawns, then dice (flying dice and the faces resting in the trays); no touch handling
        PawnLayer(layout, animator)
        DiceTossLayer(layout, toss)

        // 4. the pointing hand: above my tray while it waits for my tap, above the Computer's tray
        //    (pressing it) while the Computer plays
        val phase = remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanos { nanos ->
                    val ms = nanos / 1_000_000L
                    phase.floatValue = (ms % HAND_PERIOD_MS).toFloat() / HAND_PERIOD_MS.toFloat()
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bob = sin(phase.floatValue * HAND_TWO_PI) * layout.boardSize * 0.01f
            if (controller.waitingForHuman) {
                val tip = layout.bottomHandAnchor
                drawHand(Offset(tip.x, tip.y - bob), layout.boardSize * 0.095f)
            } else if (controller.waitingForPawn) {
                // the dice are thrown: the hand points at my glowing pawn
                val tipX = animator.drawX(Seat.HUMAN)
                val tipY = animator.groundY(Seat.HUMAN) - layout.cell * 1.0f
                drawHand(Offset(tipX, tipY - bob), layout.boardSize * 0.095f)
            } else if (controller.activeSeat == Seat.OPPONENT && !controller.showGameOver) {
                val tip = layout.topHandAnchor
                val press = controller.handPress * layout.boardSize * 0.03f
                drawHand(Offset(tip.x, tip.y - bob * (1f - controller.handPress) + press), layout.boardSize * 0.095f)
            }
        }

        // 5. invisible tap area on my tray (no ripple, no visible change)
        val tray = remember(layout) { layout.trayRect(false) }
        val noInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .offset { IntOffset(tray.left.roundToInt(), tray.top.roundToInt()) }
                .size(with(dens) { tray.width.toDp() }, with(dens) { tray.height.toDp() })
                .clickable(
                    interactionSource = noInteraction,
                    indication = null,
                    onClick = { controller.onTrayTapped(Seat.HUMAN) }
                )
        )

        // 5b. tap area on my pawn: only exists while the pawn glows and waits for my tap
        if (controller.waitingForPawn) {
            val half = layout.cell * 0.9f
            val left = animator.drawX(Seat.HUMAN) - half
            val top = animator.groundY(Seat.HUMAN) - layout.cell * 0.45f - half
            Box(
                modifier = Modifier
                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .size(with(dens) { (2f * half).toDp() })
                    .clickable(
                        interactionSource = noInteraction,
                        indication = null,
                        onClick = { controller.onPawnTapped(Seat.HUMAN) }
                    )
            )
        }

        // 6. the Game Over dialog, above everything
        if (controller.showGameOver) {
            val humanWon = controller.winner == Seat.HUMAN
            GameOverDialog(
                title = "Game Over",
                winnerLine = if (humanWon) "You are the winner" else "$topName is the winner",
                rows = listOf(
                    bottomName to controller.winsHuman,
                    topName to controller.winsOpponent
                ),
                onContinue = { controller.dismissGameOver() }
            )
        }
    }
}

/** 300 short strokes with a fixed seed: x1, y1, x2, y2, signed alpha (positive white, negative black). */
private fun buildTexture(width: Float, height: Float, pxPerDp: Float): FloatArray {
    val rnd = Random(20260725)
    val out = FloatArray(TEXTURE_COUNT * 5)
    for (i in 0 until TEXTURE_COUNT) {
        val x = rnd.nextFloat() * width
        val y = rnd.nextFloat() * height
        val len = (6f + rnd.nextFloat() * 18f) * pxPerDp
        val angle = rnd.nextFloat() * HAND_TWO_PI
        val alpha = 0.04f + rnd.nextFloat() * 0.02f
        val white = rnd.nextBoolean()
        out[i * 5] = x
        out[i * 5 + 1] = y
        out[i * 5 + 2] = x + cos(angle) * len
        out[i * 5 + 3] = y + sin(angle) * len
        out[i * 5 + 4] = if (white) alpha else -alpha
    }
    return out
}

private fun DrawScope.drawTexture(texture: FloatArray, pxPerDp: Float) {
    for (i in 0 until TEXTURE_COUNT) {
        val code = texture[i * 5 + 4]
        val color: Color = if (code > 0f) {
            GameColors.TextureLight.copy(alpha = code)
        } else {
            GameColors.TextureDark.copy(alpha = -code)
        }
        drawLine(
            color = color,
            start = Offset(texture[i * 5], texture[i * 5 + 1]),
            end = Offset(texture[i * 5 + 2], texture[i * 5 + 3]),
            strokeWidth = pxPerDp
        )
    }
}
