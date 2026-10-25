package com.westly.snakes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min

private val ConfirmNavy = Color(0xFF0F2A5C)
private val ConfirmGold = Color(0xFFF5C542)

/**
 * A navy panel with a gold border over a dimmed screen: a gold title, white text and two buttons.
 * Yes is gold with navy text, No is navy with a gold border. It swallows every touch outside the buttons.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    val blockTouches = remember { MutableInteractionSource() }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x8C000000))
            .clickable(interactionSource = blockTouches, indication = null, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        val w: Dp = min(maxWidth - 12.dp, 520.dp)
        val shape = RoundedCornerShape(w * 0.04f)
        fun textSize(fraction: Float) = with(density) { (w * fraction).toSp() }

        Column(
            modifier = Modifier
                .width(w * 0.8f)
                .clip(shape)
                .background(ConfirmNavy)
                .border(2.dp, ConfirmGold, shape)
                .padding(horizontal = w * 0.06f, vertical = w * 0.06f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = ConfirmGold,
                fontWeight = FontWeight.ExtraBold,
                fontSize = textSize(0.06f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(w * 0.03f))
            Text(
                text = message,
                color = Color.White,
                fontSize = textSize(0.042f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(w * 0.05f))
            Row(
                modifier = Modifier.width(w * 0.68f),
                horizontalArrangement = Arrangement.spacedBy(w * 0.04f)
            ) {
                ConfirmButton("Yes", filled = true, w = w, onClick = onYes, modifier = Modifier.weight(1f))
                ConfirmButton("No", filled = false, w = w, onClick = onNo, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ConfirmButton(
    text: String,
    filled: Boolean,
    w: Dp,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val density = LocalDensity.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(w * 0.12f)
            .clip(shape)
            .background(if (filled) ConfirmGold else ConfirmNavy)
            .border(2.dp, ConfirmGold, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (filled) ConfirmNavy else ConfirmGold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = with(density) { (w * 0.05f).toSp() }
        )
    }
}
