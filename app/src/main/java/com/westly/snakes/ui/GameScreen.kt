package com.westly.snakes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.westly.snakes.ui.components.ButtonIcon
import com.westly.snakes.ui.components.LabelAlign
import com.westly.snakes.ui.components.PawnColor
import com.westly.snakes.ui.components.drawDie
import com.westly.snakes.ui.components.drawHand
import com.westly.snakes.ui.components.drawOutlinedText
import com.westly.snakes.ui.components.drawPawn
import com.westly.snakes.ui.components.drawRoundButton
import com.westly.snakes.ui.components.drawTurnPill
import com.westly.snakes.ui.components.drawWoodCircle
import com.westly.snakes.ui.components.drawWoodTray
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Phase 3: the static game screen around the board. No behaviour, no taps.
// The only motion is the pointing hand bobbing.
// ---------------------------------------------------------------------------

private const val TEXTURE_COUNT = 300
private const val HAND_PERIOD_MS = 700L
private val HAND_TWO_PI = (2.0 * PI).toFloat()

@Composable
fun GameScreen(
    topName: String = "Computer",
    bottomName: String = "Me",
    topWins: Int = 0,
    bottomWins: Int = 0
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

            // placeholder dice 4 and 1 in my tray
            val dice = layout.diceCenters(false)
            drawDie(dice.first, layout.diceSizeInTray, 4)
            drawDie(dice.second, layout.diceSizeInTray, 1)

            // token slot and counter circles
            drawWoodCircle(layout.topSlotCenter, layout.circleDiameter)
            drawWoodCircle(layout.topCounterCenter, layout.circleDiameter)
            drawWoodCircle(layout.bottomSlotCenter, layout.circleDiameter)
            drawWoodCircle(layout.bottomCounterCenter, layout.circleDiameter)

            // my red pawn in my slot (centred vertically in the circle)
            val pawnHeight = w * 0.085f
            drawPawn(
                baseCenter = Offset(
                    layout.bottomSlotCenter.x,
                    layout.bottomSlotCenter.y + 0.385f * pawnHeight
                ),
                height = pawnHeight,
                color = PawnColor.RED
            )

            // counters
            drawOutlinedText("0", layout.topCounterCenter, w * 0.05f, GameColors.Cream)
            drawOutlinedText("0", layout.bottomCounterCenter, w * 0.05f, GameColors.Cream)

            // buttons
            drawRoundButton(layout.homeCenter, layout.roundButtonDiameter, ButtonIcon.HOME)
            drawRoundButton(layout.menuCenter, layout.roundButtonDiameter, ButtonIcon.MENU)

            // name labels
            drawOutlinedText(
                topName, layout.topNameCenter, w * 0.063f,
                GameColors.NameGreen, GameColors.NameGreenOutline, LabelAlign.CENTER
            )
            drawOutlinedText(
                bottomName, layout.bottomNameCenter, w * 0.063f,
                GameColors.NameGreen, GameColors.NameGreenOutline, LabelAlign.CENTER
            )

            // Wins and Rank
            drawOutlinedText(
                "Wins: $topWins", layout.topWinsAnchor, w * 0.05f,
                GameColors.Cream, GameColors.DarkBlueOutline, LabelAlign.LEFT
            )
            drawOutlinedText(
                "Rank: 0", layout.bottomRankAnchor, w * 0.05f,
                GameColors.Cream, GameColors.DarkBlueOutline, LabelAlign.LEFT
            )
            drawOutlinedText(
                "Wins: $bottomWins", layout.bottomWinsAnchor, w * 0.05f,
                GameColors.Cream, GameColors.DarkBlueOutline, LabelAlign.LEFT
            )

            // Your Turn pill
            drawTurnPill(layout.pillRect, w)
        }

        // 4. the pointing hand, bobbing above my tray
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
            val tip = layout.bottomHandAnchor
            drawHand(Offset(tip.x, tip.y - bob), layout.boardSize * 0.11f)
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
