package com.westly.snakes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.westly.snakes.ui.layers.runFrames

private val DialogGold = Color(0xFFFFD23F)
private val DialogGreen = Color(0xFF3DFF30)
private val DialogCyan = Color(0xFF4FE3F0)

/**
 * The Game Over dialog: a teal panel over a dimmed screen with the winner, the score of both
 * players and a round gold check button that starts the next round.
 * [rows] are (name, Wins) pairs in display order.
 */
@Composable
fun GameOverDialog(
    title: String,
    winnerLine: String,
    rows: List<Pair<String, Int>>,
    onContinue: () -> Unit
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
        val w: Dp = min(maxWidth - 12.dp, 520.dp)
        val corner = w * 0.05f
        fun textSize(fraction: Float) = with(density) { (w * fraction).toSp() }

        Column(
            modifier = Modifier
                .width(w * 0.8f)
                .clip(RoundedCornerShape(corner))
                .background(Brush.verticalGradient(listOf(Color(0xFF0E7C8C), Color(0xFF0A5663))))
                .border(2.dp, Color(0x4DFFFFFF), RoundedCornerShape(corner))
                .padding(horizontal = w * 0.06f, vertical = w * 0.05f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = DialogGold,
                fontWeight = FontWeight.ExtraBold,
                fontSize = textSize(0.065f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(w * 0.03f))
            Text(
                text = winnerLine,
                color = DialogGreen,
                fontWeight = FontWeight.Bold,
                fontSize = textSize(0.05f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(w * 0.04f))
            Text(
                text = "Score",
                color = DialogCyan,
                fontWeight = FontWeight.Bold,
                fontSize = textSize(0.05f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(w * 0.02f))

            DialogDivider()
            for (row in rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = w * 0.02f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.first,
                        color = DialogGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = textSize(0.05f),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = row.second.toString(),
                        color = DialogGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = textSize(0.05f)
                    )
                }
                DialogDivider()
            }

            Spacer(modifier = Modifier.height(w * 0.04f))
            Box(
                modifier = Modifier
                    .size(w * 0.12f)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFE0A21B))))
                    .border(2.dp, Color(0xFF8A5A00), CircleShape)
                    .clickable(onClick = onContinue),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(w * 0.06f)) {
                    val s = size.minDimension
                    val check = Path().apply {
                        moveTo(s * 0.08f, s * 0.55f)
                        lineTo(s * 0.38f, s * 0.85f)
                        lineTo(s * 0.92f, s * 0.15f)
                    }
                    drawPath(
                        path = check,
                        color = Color(0xFF14213D),
                        style = Stroke(width = s * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0x33FFFFFF))
    )
}
