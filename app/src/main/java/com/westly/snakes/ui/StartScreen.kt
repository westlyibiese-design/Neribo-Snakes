package com.westly.snakes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.westly.snakes.R
import com.westly.snakes.data.Prefs
import com.westly.snakes.game.GameMode
import com.westly.snakes.ui.components.ButtonIcon
import com.westly.snakes.ui.components.LabelAlign
import com.westly.snakes.ui.components.drawOutlinedText
import com.westly.snakes.ui.components.drawRoundButton
import kotlin.math.roundToInt

private val TitleCream = Color(0xFFFFF3D6)
private val TitleEdge = Color(0xFF5A2E0E)
private val ButtonTextEdge = Color(0xFF0B4F86)

/** Height divided by width of the picture `start_panel.webp`. */
private const val PANEL_ASPECT = 0.8f

/**
 * The Start screen: the sunset jungle picture fills the screen, the logo sits in the sky and the
 * wooden panel picture carries the "Start" title and the two game modes. A mode with a game in
 * progress says "Resume game" and carries on where it was left.
 */
@Composable
fun StartScreen(
    prefs: Prefs,
    onStart: (GameMode) -> Unit,
    onClose: () -> Unit
) {
    var soundOn by remember { mutableStateOf(prefs.soundOn) }
    val savedVs = remember { prefs.hasSavedGame(multiplayer = false) }
    val savedMp = remember { prefs.hasSavedGame(multiplayer = true) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w: Dp = min(maxWidth - 12.dp, 520.dp)

        Image(
            painter = painterResource(R.drawable.bg_start),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // the logo, centred in the upper half
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.start_logo),
                    contentDescription = null,
                    modifier = Modifier.width(w * 0.7f)
                )
            }

            // the wooden Start panel
            Box(modifier = Modifier.fillMaxWidth().weight(1.1f), contentAlignment = Alignment.Center) {
                StartPanel(
                    w = w,
                    savedVs = savedVs,
                    savedMp = savedMp,
                    onVs = { onStart(GameMode.VS_COMPUTER) },
                    onMp = { onStart(GameMode.MULTIPLAYER) },
                    onClose = onClose
                )
            }

            Text(
                text = "\u00A9 Neribo Group",
                color = Color(0xE6FFFFFF),
                fontSize = with(density) { (w * 0.035f).toSp() },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // the speaker at the top-right of the screen toggles the sound
        RedRoundButton(
            icon = if (soundOn) ButtonIcon.SOUND_ON else ButtonIcon.SOUND_OFF,
            buttonSize = w * 0.11f,
            onClick = {
                soundOn = !soundOn
                prefs.soundOn = soundOn
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 14.dp)
        )
    }
}

/**
 * The panel picture with its texts drawn on top in code (so they stay sharp) and invisible tap
 * areas over the two blue slots and the red button. All positions are fractions of the picture.
 */
@Composable
private fun StartPanel(
    w: Dp,
    savedVs: Boolean,
    savedMp: Boolean,
    onVs: () -> Unit,
    onMp: () -> Unit,
    onClose: () -> Unit
) {
    val density = LocalDensity.current
    val panelHeight = w * PANEL_ASPECT
    val panelWidthPx = with(density) { w.toPx() }
    val panelHeightPx = with(density) { panelHeight.toPx() }

    Box(modifier = Modifier.width(w).height(panelHeight)) {
        Image(
            painter = painterResource(R.drawable.start_panel),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pw = size.width
            val ph = size.height
            drawOutlinedText(
                text = "Start",
                anchor = Offset(0.492f * pw, 0.254f * ph),
                sizePx = 0.115f * ph,
                fillColor = TitleCream,
                outlineColor = TitleEdge,
                align = LabelAlign.CENTER
            )
            drawSlotText("You & Computer", if (savedVs) "Resume game" else null, Offset(0.495f * pw, 0.474f * ph), ph)
            drawSlotText("Multiplayer", if (savedMp) "Resume game" else null, Offset(0.495f * pw, 0.685f * ph), ph)
            drawCloseCross(Offset(0.865f * pw, 0.29f * ph), pw)
        }

        PanelTapArea(0.495f, 0.474f, 0.235f * panelWidthPx, 0.07f * panelHeightPx, panelWidthPx, panelHeightPx, true, onVs)
        PanelTapArea(0.495f, 0.685f, 0.235f * panelWidthPx, 0.07f * panelHeightPx, panelWidthPx, panelHeightPx, true, onMp)
        PanelTapArea(0.865f, 0.29f, 0.055f * panelWidthPx, 0.055f * panelWidthPx, panelWidthPx, panelHeightPx, true, onClose)
    }
}

/** The mode name centred in its blue slot, with a smaller "Resume game" line under it when there is one. */
private fun DrawScope.drawSlotText(title: String, subtitle: String?, center: Offset, panelHeight: Float) {
    if (subtitle == null) {
        drawOutlinedText(
            text = title,
            anchor = center,
            sizePx = (if (title.length > 12) 0.062f else 0.072f) * panelHeight,
            fillColor = Color.White,
            outlineColor = ButtonTextEdge,
            align = LabelAlign.CENTER
        )
    } else {
        drawOutlinedText(
            text = title,
            anchor = Offset(center.x, center.y - 0.02f * panelHeight),
            sizePx = 0.062f * panelHeight,
            fillColor = Color.White,
            outlineColor = ButtonTextEdge,
            align = LabelAlign.CENTER
        )
        drawOutlinedText(
            text = subtitle,
            anchor = Offset(center.x, center.y + 0.037f * panelHeight),
            sizePx = 0.036f * panelHeight,
            fillColor = Color.White,
            outlineColor = ButtonTextEdge,
            align = LabelAlign.CENTER
        )
    }
}

/** A white cross on the red button of the panel picture. */
private fun DrawScope.drawCloseCross(c: Offset, panelWidth: Float) {
    val arm = 0.022f * panelWidth
    val width = 0.013f * panelWidth
    val lift = 0.003f * panelWidth
    for ((color, dy) in listOf(Color(0x66000000) to lift, Color.White to 0f)) {
        drawLine(color, Offset(c.x - arm, c.y - arm + dy), Offset(c.x + arm, c.y + arm + dy), strokeWidth = width, cap = StrokeCap.Round)
        drawLine(color, Offset(c.x + arm, c.y - arm + dy), Offset(c.x - arm, c.y + arm + dy), strokeWidth = width, cap = StrokeCap.Round)
    }
}

/** An invisible tap area centred at (fractionX, fractionY) of the panel; it shows a soft white flash while pressed. */
@Composable
private fun PanelTapArea(
    fractionX: Float,
    fractionY: Float,
    halfWidthPx: Float,
    halfHeightPx: Float,
    panelWidthPx: Float,
    panelHeightPx: Float,
    rounded: Boolean,
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
                shape = RoundedCornerShape(if (rounded) 50 else 0)
            )
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    )
}

/** A glossy red round button of the given size with a white icon. It shrinks to 0.94 while pressed. */
@Composable
private fun RedRoundButton(icon: ButtonIcon, buttonSize: Dp, onClick: () -> Unit, modifier: Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(buttonSize)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundButton(
                center = Offset(size.width / 2f, size.height / 2f),
                diameter = size.minDimension,
                icon = icon,
                pressScale = if (pressed) 0.94f else 1f
            )
        }
    }
}
