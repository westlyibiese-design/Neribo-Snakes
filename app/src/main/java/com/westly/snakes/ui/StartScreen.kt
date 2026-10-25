package com.westly.snakes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.westly.snakes.ui.components.drawWoodTray
import kotlin.math.roundToInt
import kotlin.random.Random

private val NightTop = Color(0xFF0B1233)
private val NightBottom = Color(0xFF1C2760)
private val PillTop = Color(0xFF3FD0F0)
private val PillBottom = Color(0xFF1A8FC8)
private val PillRim = Color(0xFF0E6F9E)
private val TitleCream = Color(0xFFFFF3D6)
private val TitleEdge = Color(0xFF6B3510)

/**
 * The Start screen: a navy night sky with faint stars, the logo, and a wooden "Start" panel with the
 * two game modes. A mode with a game in progress says "Resume game" and carries on where it was left.
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
    val stars = remember {
        val random = Random(7)
        List(40) { Triple(random.nextFloat(), random.nextFloat(), 0.4f + random.nextFloat() * 0.6f) }
    }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w: Dp = min(maxWidth - 12.dp, 520.dp)

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.verticalGradient(listOf(NightTop, NightBottom)))
            for (star in stars) {
                drawCircle(
                    color = Color(0xFFFFFFFF).copy(alpha = 0.12f + 0.3f * star.third),
                    radius = size.width * (0.0018f + 0.0032f * star.third),
                    center = Offset(star.first * size.width, star.second * size.height)
                )
            }
        }

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
                    painter = painterResource(R.drawable.splash_logo),
                    contentDescription = null,
                    modifier = Modifier.width(w * 0.7f)
                )
            }

            // the wooden Start panel
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
                val panelWidth = w * 0.86f
                val panelHeight = w * 0.70f
                Box(modifier = Modifier.width(panelWidth).height(panelHeight)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawWoodTray(Rect(0f, 0f, size.width, size.height), size.width / 0.62f)
                        drawOutlinedText(
                            text = "Start",
                            anchor = Offset(size.width / 2f, size.height * 0.14f),
                            sizePx = size.width * 0.12f,
                            fillColor = TitleCream,
                            outlineColor = TitleEdge,
                            align = LabelAlign.CENTER
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = panelHeight * 0.28f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CyanPillButton(
                            text = "You & Computer",
                            subtitle = if (savedVs) "Resume game" else null,
                            w = w,
                            onClick = { onStart(GameMode.VS_COMPUTER) }
                        )
                        Spacer(modifier = Modifier.height(w * 0.05f))
                        CyanPillButton(
                            text = "Multiplayer",
                            subtitle = if (savedMp) "Resume game" else null,
                            w = w,
                            onClick = { onStart(GameMode.MULTIPLAYER) }
                        )
                    }
                    // the red x at the top-right of the panel closes the app
                    val closeSize = w * 0.11f
                    RedRoundButton(
                        icon = ButtonIcon.CLOSE,
                        buttonSize = closeSize,
                        onClick = onClose,
                        modifier = Modifier.offset {
                            IntOffset(
                                (with(density) { (panelWidth - closeSize * 0.8f).toPx() }).roundToInt(),
                                (with(density) { (-closeSize * 0.2f).toPx() }).roundToInt()
                            )
                        }
                    )
                }
            }

            Text(
                text = "\u00A9 Neribo Group",
                color = Color(0x99FFFFFF),
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

@Composable
private fun CyanPillButton(text: String, subtitle: String?, w: Dp, onClick: () -> Unit) {
    val density = LocalDensity.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .width(w * 0.66f)
            .height(w * 0.15f)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PillTop, PillBottom)))
            .border(2.dp, PillRim, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = with(density) { (w * 0.055f).toSp() }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color(0xE6FFFFFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = with(density) { (w * 0.03f).toSp() }
                )
            }
        }
    }
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
