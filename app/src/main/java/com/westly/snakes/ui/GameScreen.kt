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
import com.westly.snakes.game.GameMode
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
// Phase 5: the game screen around the board, driven by the GameController (You & Computer).
// Only the bottom tray is tappable; the Computer's turns play by themselves.
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
        val controller = remember(layout) {
            GameController(scope, layout, animator, toss, GameMode.VS_COMPUTER)
        }
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

            // the token of the player whose turn it is, in that player's slot (centred vertically)
            val pawnHeight = w * 0.095f
            if (controller.activeSeat == Seat.HUMAN) {
                drawPawn(
                    baseCenter = Offset(
                        layout.bottomSlotCenter.x,
                        layout.bottomSlotCenter.y + 0.385f * pawnHeight
                    ),
                    height = pawnHeight,
                    color = PawnColor.RED
                )
            } else {
                drawPawn(
                    baseCenter = Offset(
                        layout.topSlotCenter.x,
                        layout.topSlotCenter.y + 0.385f * pawnHeight
                    ),
                    height = pawnHeight,
                    color = PawnColor.BLUE
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
                "Wins: ${controller.opponentWins}", layout.topWinsAnchor, w * 0.065f,
                GameColors.Cream, GameColors.DarkBlueOutline, LabelAlign.LEFT
            )
            drawOutlinedText(
                "Rank: 0", layout.bottomRankAnchor, w * 0.065f,
                GameColors.Cream, GameColors.DarkBlueOutline, LabelAlign.LEFT
            )
            drawOutlinedText(
                "Wins: ${controller.humanWins}", layout.bottomWinsAnchor, w * 0.065f,
                GameColors.Cream, GameColors.DarkBlueOutline, LabelAlign.LEFT
            )

            // Your Turn pill (only during the human's turn)
            if (controller.activeSeat == Seat.HUMAN) {
                drawTurnPill(layout.pillRect, w)
            }
        }

        // 3b. pawns, then dice (flying dice and the faces resting in the trays); no touch handling
        PawnLayer(layout, animator)
        DiceTossLayer(layout, toss)

        // 4. the pointing hand, bobbing above the tray of the player whose turn it is
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
            val tip = if (controller.activeSeat == Seat.HUMAN) layout.bottomHandAnchor else layout.topHandAnchor
            drawHand(Offset(tip.x, tip.y - bob), layout.boardSize * 0.095f)
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

        // 6. Game Over dialog over everything (its dim background blocks all touches below)
        val gameWinner = controller.winner
        if (controller.showGameOver && gameWinner != null) {
            val winnerLine = if (gameWinner == Seat.HUMAN) "You are the winner" else "$topName is the winner"
            GameOverDialog(
                title = "Game Over",
                winnerLine = winnerLine,
                rows = listOf(
                    Pair(bottomName, controller.humanWins),
                    Pair(topName, controller.opponentWins)
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
