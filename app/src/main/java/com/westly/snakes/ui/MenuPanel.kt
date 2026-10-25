package com.westly.snakes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.westly.snakes.ui.components.ButtonIcon
import com.westly.snakes.ui.components.LabelAlign
import com.westly.snakes.ui.components.drawOutlinedText
import com.westly.snakes.ui.components.drawRoundButton
import com.westly.snakes.ui.layers.runFrames
import kotlin.math.roundToInt

private val PanelTop = Color(0xFFC97A33)
private val PanelBottom = Color(0xFFA85A1E)
private val PanelGrain = Color(0x1F6B3510)
private val PanelEdge = Color(0xFF6B3510)
private val LabelCream = Color(0xFFFFF3D6)

/**
 * The wooden menu panel. It replaces the area above the board (from the top of the screen down to
 * `boardTop - 0.02W`) and slides down in 250 ms when [open] becomes true, and back up when false.
 * Four round buttons: Exit, Restart, Sound and Rate. It swallows every touch on its area.
 */
@Composable
fun MenuPanel(
    layout: GameLayout,
    open: Boolean,
    soundOn: Boolean,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    onSound: () -> Unit,
    onRate: () -> Unit
) {
    val progress = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(open) {
        val from = progress.floatValue
        val to = if (open) 1f else 0f
        if (from != to) {
            runFrames(250L) { t -> progress.floatValue = from + (to - from) * t * t * (3f - 2f * t) }
        }
    }
    val visible by remember { derivedStateOf { progress.floatValue > 0f } }
    if (!visible) return

    val density = LocalDensity.current
    val w = layout.boardSize
    val panelHeightPx = layout.boardTop - 0.02f * w
    val buttonDiameter = 0.115f * w
    val spacing = 0.145f * w
    val centerX = layout.boardLeft + w / 2f
    val centerY = panelHeightPx - 0.20f * w
    val blockTouches = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(with(density) { panelHeightPx.toDp() })
            .graphicsLayer { translationY = -(1f - progress.floatValue) * panelHeightPx }
            .clickable(interactionSource = blockTouches, indication = null, onClick = {})
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMenuWood(panelHeightPx, w)
            val labels = listOf("Exit", "Restart", "Sound", "Rate")
            for (i in 0 until 4) {
                drawOutlinedText(
                    text = labels[i],
                    anchor = Offset(centerX + (i - 1.5f) * spacing, centerY + buttonDiameter * 0.5f + 0.045f * w),
                    sizePx = 0.038f * w,
                    fillColor = LabelCream,
                    outlineColor = PanelEdge,
                    align = LabelAlign.CENTER
                )
            }
        }
        val icons = listOf(
            ButtonIcon.EXIT,
            ButtonIcon.RESTART,
            if (soundOn) ButtonIcon.SOUND_ON else ButtonIcon.SOUND_OFF,
            ButtonIcon.STAR
        )
        val actions = listOf(onExit, onRestart, onSound, onRate)
        for (i in 0 until 4) {
            MenuRoundButton(
                center = Offset(centerX + (i - 1.5f) * spacing, centerY),
                diameter = buttonDiameter,
                icon = icons[i],
                onClick = actions[i]
            )
        }
    }
}

@Composable
private fun MenuRoundButton(center: Offset, diameter: Float, icon: ButtonIcon, onClick: () -> Unit) {
    val density = LocalDensity.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .offset { IntOffset((center.x - diameter / 2f).roundToInt(), (center.y - diameter / 2f).roundToInt()) }
            .size(with(density) { diameter.toDp() })
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundButton(
                center = Offset(size.width / 2f, size.height / 2f),
                diameter = diameter,
                icon = icon,
                pressScale = if (pressed) 0.94f else 1f
            )
        }
    }
}

/** Wood gradient with faint vertical grain lines and a dark bottom edge. */
private fun DrawScope.drawMenuWood(height: Float, boardSize: Float) {
    drawRect(
        brush = Brush.verticalGradient(listOf(PanelTop, PanelBottom), startY = 0f, endY = height),
        size = androidx.compose.ui.geometry.Size(size.width, height)
    )
    val step = boardSize * 0.035f
    var x = step * 0.5f
    var i = 0
    while (x < size.width) {
        val wobble = if (i % 3 == 0) 0.004f else 0f
        drawLine(
            color = PanelGrain,
            start = Offset(x, 0f),
            end = Offset(x + boardSize * wobble, height),
            strokeWidth = if (i % 4 == 0) boardSize * 0.004f else boardSize * 0.0022f
        )
        x += step * (if (i % 2 == 0) 1f else 1.4f)
        i++
    }
    drawRect(
        color = PanelEdge,
        topLeft = Offset(0f, height - boardSize * 0.012f),
        size = androidx.compose.ui.geometry.Size(size.width, boardSize * 0.012f)
    )
}
