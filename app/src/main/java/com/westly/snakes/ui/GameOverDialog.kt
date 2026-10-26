package com.westly.snakes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.westly.snakes.R
import com.westly.snakes.ui.components.LabelAlign
import com.westly.snakes.ui.components.drawOutlinedText
import com.westly.snakes.ui.layers.runFrames
import kotlin.math.roundToInt

/** Height divided by width of the pictures `game_over_won.webp` and `game_over_lost.webp`. */
private const val SIGN_ASPECT = 910f / 1100f

private val LabelFill = Color(0xFFFFFFFF)
private val LabelEdge = Color(0xFF0B5FA8)
private val ScoreFill = Color(0xFFFFE9A8)
private val ScoreEdge = Color(0xFF3A1D08)

/**
 * The end-of-round dialog: a wooden snake sign over a dimmed screen. The title on the sign says
 * "YOU WON" or "YOU LOST" (picture chosen by [won]). The two blue buttons are "Play Again"
 * ([onPlayAgain]) and "Leave" ([onLeave]); [scoreLine] sits on the wood above them.
 */
@Composable
fun GameOverDialog(
    won: Boolean,
    scoreLine: String,
    onPlayAgain: () -> Unit,
    onLeave: () -> Unit
) {
    val fade = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        runFrames(400) { t -> fade.floatValue = t }
    }
    val blockTouches = remember { MutableInteractionSource() }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.floatValue }
            .background(Color(0x8C000000))
            .clickable(interactionSource = blockTouches, indication = null, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        // as wide as the screen allows, but never taller than 85% of it
        val byWidth: Dp = min(maxWidth * 0.92f, 560.dp)
        val byHeight: Dp = maxHeight * 0.85f / SIGN_ASPECT
        val panelWidth: Dp = min(byWidth, byHeight)
        val panelHeight: Dp = panelWidth * SIGN_ASPECT
        val panelWidthPx = with(density) { panelWidth.toPx() }
        val panelHeightPx = with(density) { panelHeight.toPx() }

        Box(modifier = Modifier.width(panelWidth).height(panelHeight)) {
            Image(
                painter = painterResource(if (won) R.drawable.game_over_won else R.drawable.game_over_lost),
                contentDescription = if (won) "You won" else "You lost",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pw = size.width
                val ph = size.height
                drawOutlinedText(
                    text = scoreLine,
                    anchor = Offset(0.5f * pw, 0.438f * ph),
                    sizePx = 0.042f * ph,
                    fillColor = ScoreFill,
                    outlineColor = ScoreEdge,
                    align = LabelAlign.CENTER
                )
                drawOutlinedText(
                    text = "Play Again",
                    anchor = Offset(0.502f * pw, 0.551f * ph),
                    sizePx = 0.07f * ph,
                    fillColor = LabelFill,
                    outlineColor = LabelEdge,
                    align = LabelAlign.CENTER
                )
                drawOutlinedText(
                    text = "Leave",
                    anchor = Offset(0.502f * pw, 0.766f * ph),
                    sizePx = 0.07f * ph,
                    fillColor = LabelFill,
                    outlineColor = LabelEdge,
                    align = LabelAlign.CENTER
                )
            }

            SignTapArea(0.502f, 0.551f, 0.255f * panelWidthPx, 0.085f * panelHeightPx, panelWidthPx, panelHeightPx, "Play again", onPlayAgain)
            SignTapArea(0.502f, 0.766f, 0.255f * panelWidthPx, 0.085f * panelHeightPx, panelWidthPx, panelHeightPx, "Leave the game", onLeave)
        }
    }
}

@Composable
private fun SignTapArea(
    fractionX: Float,
    fractionY: Float,
    halfWidthPx: Float,
    halfHeightPx: Float,
    panelWidthPx: Float,
    panelHeightPx: Float,
    description: String,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (fractionX * panelWidthPx - halfWidthPx).roundToInt(),
                    (fractionY * panelHeightPx - halfHeightPx).roundToInt()
                )
            }
            .size(
                with(density) { (2f * halfWidthPx).toDp() },
                with(density) { (2f * halfHeightPx).toDp() }
            )
            .background(
                color = if (pressed) Color(0x33FFFFFF) else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .semantics { contentDescription = description }
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    )
}
