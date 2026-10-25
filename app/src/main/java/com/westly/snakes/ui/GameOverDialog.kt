package com.westly.snakes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

// ---------------------------------------------------------------------------
// Phase 5: the Game Over dialog. All sizes are fractions of W, the board side.
// ---------------------------------------------------------------------------

private val DialogGold = Color(0xFFFFD23F)
private val DialogGreen = Color(0xFF3DFF30)
private val DialogCyan = Color(0xFF4FE3F0)
private val PanelTop = Color(0xFF0E7C8C)
private val PanelBottom = Color(0xFF0A5663)
private val DimColor = Color(0x8C000000) // 55% black
private val LineColor = Color(0x33FFFFFF) // 20% white

/**
 * Dim background, a centred panel and a round gold check button.
 * The dim swallows every touch so nothing underneath can be tapped.
 * W is worked out with the same maths as the game screen, so the dialog must be placed
 * where it gets the full screen size.
 */
@Composable
fun GameOverDialog(
    title: String,
    winnerLine: String,
    rows: List<Pair<String, Int>>,
    onContinue: () -> Unit
) {
    val dimInteraction = remember { MutableInteractionSource() }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DimColor)
            .clickable(interactionSource = dimInteraction, indication = null, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        val dens = LocalDensity.current
        val widthPx = with(dens) { maxWidth.toPx() }
        val heightPx = with(dens) { maxHeight.toPx() }
        val topInsetPx = WindowInsets.statusBars.getTop(dens).toFloat()
        val bottomInsetPx = WindowInsets.navigationBars.getBottom(dens).toFloat()
        val w = remember(widthPx, heightPx, topInsetPx, bottomInsetPx, dens.density) {
            GameLayout(widthPx, heightPx, topInsetPx, bottomInsetPx, dens.density).boardSize
        }

        fun dpOf(px: Float): Dp = with(dens) { px.toDp() }
        fun spOf(px: Float): TextUnit = with(dens) { px.toSp() }

        val lineThickness = dpOf(maxOf(1f, 0.004f * w))

        Column(
            modifier = Modifier
                .width(dpOf(0.8f * w))
                .background(
                    brush = Brush.verticalGradient(listOf(PanelTop, PanelBottom)),
                    shape = RoundedCornerShape(dpOf(0.05f * w))
                )
                .padding(horizontal = dpOf(0.06f * w), vertical = dpOf(0.05f * w)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = DialogGold,
                fontSize = spOf(0.065f * w),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(dpOf(0.03f * w)))
            Text(
                text = winnerLine,
                color = DialogGreen,
                fontSize = spOf(0.05f * w),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(dpOf(0.04f * w)))
            Text(
                text = "Score",
                color = DialogCyan,
                fontSize = spOf(0.045f * w),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(dpOf(0.015f * w)))
            ThinLine(lineThickness)
            for (row in rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dpOf(0.1f * w)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = row.first,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontSize = spOf(0.05f * w),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = row.second.toString(),
                        color = DialogGold,
                        fontSize = spOf(0.05f * w),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                ThinLine(lineThickness)
            }
            Spacer(modifier = Modifier.height(dpOf(0.04f * w)))
            CheckButton(diameter = dpOf(0.12f * w), onClick = onContinue)
        }
    }
}

@Composable
private fun ThinLine(thickness: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thickness)
            .background(LineColor)
    )
}

@Composable
private fun CheckButton(diameter: Dp, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(diameter)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCheckButton()
        }
    }
}

private fun DrawScope.drawCheckButton() {
    val r = size.minDimension / 2f
    val c = Offset(size.width / 2f, size.height / 2f)
    drawCircle(color = Color(0xFF9A6A00), radius = r, center = c)
    drawCircle(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFE680), Color(0xFFFFD23F), Color(0xFFF2A900))),
        radius = r * 0.92f,
        center = c
    )
    val tick = Path().apply {
        moveTo(c.x - r * 0.42f, c.y + r * 0.02f)
        lineTo(c.x - r * 0.12f, c.y + r * 0.34f)
        lineTo(c.x + r * 0.44f, c.y - r * 0.30f)
    }
    drawPath(
        path = tick,
        color = Color.White,
        style = Stroke(width = r * 0.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
