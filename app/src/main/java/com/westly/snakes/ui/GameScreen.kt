package com.westly.snakes.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.westly.snakes.R
import com.westly.snakes.audio.Haptics
import com.westly.snakes.audio.Sound
import com.westly.snakes.audio.SoundEngine
import com.westly.snakes.data.Prefs
import com.westly.snakes.game.GameController
import com.westly.snakes.game.GameEvent
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
// The game screen around the board, driven by the GameController: turns, dice, snakes, ladders,
// the Computer player or a second person (Multiplayer), winning and the Game Over dialog, plus
// the menu panel, the confirm dialogs and the Android Back button.
// ---------------------------------------------------------------------------

private const val TEXTURE_COUNT = 300
private const val HAND_PERIOD_MS = 700L
private val HAND_TWO_PI = (2.0 * PI).toFloat()

private enum class ConfirmKind { NONE, LEAVE, RESTART }

/**
 * One game in the given [mode]. [onLeave] is called when the player confirms leaving; the game in
 * progress stays saved in [prefs] so the same mode can carry on later.
 */
@Composable
fun GameScreen(
    mode: GameMode,
    prefs: Prefs,
    sound: SoundEngine,
    haptics: Haptics,
    onLeave: () -> Unit
) {
    val multiplayer = mode == GameMode.MULTIPLAYER
    val topName = if (multiplayer) "Player 2" else "Computer"
    val bottomName = if (multiplayer) "Player 1" else "Me"
    val context = LocalContext.current

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
        val animator = remember(layout) { PawnAnimator(layout) }
        val toss = remember(layout) { DiceToss(layout) }
        val scope = rememberCoroutineScope()
        val controller = remember(layout) {
            GameController(scope, layout, animator, toss, mode, prefs, prefs.loadGame(multiplayer))
        }
        DisposableEffect(controller) {
            // the one place where the game's events become sounds and vibrations
            controller.onEvent = { event ->
                when (event) {
                    GameEvent.DICE_ROLL -> sound.play(Sound.DICE_ROLL)
                    GameEvent.STEP -> sound.play(Sound.STEP)
                    GameEvent.LADDER -> sound.play(Sound.LADDER_UP)
                    GameEvent.SNAKE -> {
                        sound.play(Sound.SNAKE_DOWN)
                        haptics.snakeBite()
                    }
                    GameEvent.NO_MOVE -> Unit // never happens now: a roll that is too big wins
                    GameEvent.WIN -> {
                        sound.play(Sound.WIN)
                        haptics.win()
                    }
                    GameEvent.BUTTON -> sound.play(Sound.BUTTON)
                }
            }
            controller.start()
            onDispose { controller.stop() }
        }

        var menuOpen by remember { mutableStateOf(false) }
        var confirm by remember { mutableStateOf(ConfirmKind.NONE) }
        var soundOn by remember { mutableStateOf(prefs.soundOn) }
        // while the menu or a dialog is open, taps on trays and pawns are ignored
        val blocked = menuOpen || confirm != ConfirmKind.NONE || controller.showGameOver
        val homeSource = remember { MutableInteractionSource() }
        val menuSource = remember { MutableInteractionSource() }
        val homePressed by homeSource.collectIsPressedAsState()
        val menuPressed by menuSource.collectIsPressedAsState()

        // the Android Back button asks before leaving; with a dialog open it closes that dialog
        BackHandler(enabled = true) {
            if (confirm != ConfirmKind.NONE) {
                confirm = ConfirmKind.NONE
            } else {
                menuOpen = false
                confirm = ConfirmKind.LEAVE
            }
        }

        // 1. the night jungle picture fills the whole screen (cropped, never stretched)
        Image(
            painter = painterResource(R.drawable.bg_game),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

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
                // above the tray of the person whose turn it is
                val tip = if (controller.activeSeat == Seat.OPPONENT) layout.topHandAnchor else layout.bottomHandAnchor
                drawHand(Offset(tip.x, tip.y - bob), layout.boardSize * 0.095f)
            } else if (controller.waitingForPawn) {
                // the dice are thrown: the hand points at the glowing pawn of the active person
                val pawnSeat = controller.activeSeat
                val tipX = animator.drawX(pawnSeat)
                val tipY = animator.groundY(pawnSeat) - layout.cell * 1.25f
                drawHand(Offset(tipX, tipY - bob), layout.boardSize * 0.095f)
            } else if (!multiplayer && controller.activeSeat == Seat.OPPONENT && !controller.showGameOver) {
                val tip = layout.topHandAnchor
                val press = controller.handPress * layout.boardSize * 0.03f
                drawHand(Offset(tip.x, tip.y - bob * (1f - controller.handPress) + press), layout.boardSize * 0.095f)
            }
        }

        // 5. invisible tap areas on the trays (no ripple, no visible change)
        val noInteraction = remember { MutableInteractionSource() }
        val tray = remember(layout) { layout.trayRect(false) }
        Box(
            modifier = Modifier
                .offset { IntOffset(tray.left.roundToInt(), tray.top.roundToInt()) }
                .size(with(dens) { tray.width.toDp() }, with(dens) { tray.height.toDp() })
                .semantics { contentDescription = if (multiplayer) "Player 1 dice tray" else "My dice tray" }
                .clickable(
                    interactionSource = noInteraction,
                    indication = null,
                    onClick = { if (!blocked) controller.onTrayTapped(Seat.HUMAN) }
                )
        )
        if (multiplayer) {
            // Player 2 taps the top tray on their own turn
            val topTray = remember(layout) { layout.trayRect(true) }
            Box(
                modifier = Modifier
                    .offset { IntOffset(topTray.left.roundToInt(), topTray.top.roundToInt()) }
                    .size(with(dens) { topTray.width.toDp() }, with(dens) { topTray.height.toDp() })
                    .semantics { contentDescription = "Player 2 dice tray" }
                    .clickable(
                        interactionSource = noInteraction,
                        indication = null,
                        onClick = { if (!blocked) controller.onTrayTapped(Seat.OPPONENT) }
                    )
            )
        }

        // 5b. tap area on the active pawn: only exists while that pawn glows and waits for its tap
        if (controller.waitingForPawn) {
            val pawnSeat = controller.activeSeat
            val half = layout.cell * 0.9f
            val left = animator.drawX(pawnSeat) - half
            val top = animator.groundY(pawnSeat) - layout.cell * 0.45f - half
            Box(
                modifier = Modifier
                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .size(with(dens) { (2f * half).toDp() })
                    .semantics { contentDescription = "Tap your pawn to move" }
                    .clickable(
                        interactionSource = noInteraction,
                        indication = null,
                        onClick = { if (!blocked) controller.onPawnTapped(pawnSeat) }
                    )
            )
        }

        // 5c. the menu panel slides down over the area above the board
        MenuPanel(
            layout = layout,
            open = menuOpen,
            soundOn = soundOn,
            onExit = {
                sound.play(Sound.BUTTON)
                menuOpen = false
                confirm = ConfirmKind.LEAVE
            },
            onRestart = {
                sound.play(Sound.BUTTON)
                menuOpen = false
                confirm = ConfirmKind.RESTART
            },
            onSound = {
                // the Sound button switches sound and vibration together
                soundOn = !soundOn
                prefs.soundOn = soundOn
                prefs.vibrationOn = soundOn
                sound.play(Sound.BUTTON)
            },
            onRate = {
                sound.play(Sound.BUTTON)
                openRatePage(context)
            }
        )

        // 5d. the Home and grid buttons stay above the menu panel
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundButton(
                layout.homeCenter, layout.roundButtonDiameter, ButtonIcon.HOME,
                if (homePressed) 0.94f else 1f
            )
            drawRoundButton(
                layout.menuCenter, layout.roundButtonDiameter, ButtonIcon.MENU,
                if (menuPressed) 0.94f else 1f
            )
        }
        val buttonDp = with(dens) { layout.roundButtonDiameter.toDp() }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (layout.homeCenter.x - layout.roundButtonDiameter / 2f).roundToInt(),
                        (layout.homeCenter.y - layout.roundButtonDiameter / 2f).roundToInt()
                    )
                }
                .size(buttonDp)
                .semantics { contentDescription = "Home" }
                .clickable(
                    interactionSource = homeSource,
                    indication = null,
                    onClick = {
                        if (confirm == ConfirmKind.NONE && !controller.showGameOver) {
                            sound.play(Sound.BUTTON)
                            menuOpen = false
                            confirm = ConfirmKind.LEAVE
                        }
                    }
                )
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (layout.menuCenter.x - layout.roundButtonDiameter / 2f).roundToInt(),
                        (layout.menuCenter.y - layout.roundButtonDiameter / 2f).roundToInt()
                    )
                }
                .size(buttonDp)
                .semantics { contentDescription = "Menu" }
                .clickable(
                    interactionSource = menuSource,
                    indication = null,
                    onClick = {
                        if (confirm == ConfirmKind.NONE && !controller.showGameOver) {
                            sound.play(Sound.BUTTON)
                            menuOpen = !menuOpen
                        }
                    }
                )
        )

        // 6. the Game Over dialog, above everything
        if (controller.showGameOver) {
            val humanWon = controller.winner == Seat.HUMAN
            GameOverDialog(
                won = if (multiplayer) true else humanWon,
                scoreLine = if (multiplayer) {
                    val winnerName = if (humanWon) bottomName else topName
                    "$winnerName wins  (${controller.winsHuman} - ${controller.winsOpponent})"
                } else {
                    "$bottomName ${controller.winsHuman} - ${controller.winsOpponent} $topName"
                },
                onPlayAgain = {
                    sound.play(Sound.BUTTON)
                    controller.dismissGameOver()
                },
                onLeave = {
                    sound.play(Sound.BUTTON)
                    confirm = ConfirmKind.LEAVE
                }
            )
            // confetti falls over the dialog for 1.5 seconds, only when someone has won to celebrate
            if (multiplayer || humanWon) ConfettiOverlay()
        }

        // 7. the confirm dialogs, above the Game Over dialog
        when (confirm) {
            ConfirmKind.LEAVE -> ConfirmDialog(
                title = "Leave this game?",
                message = if (controller.showGameOver) {
                    "Your Wins are saved."
                } else {
                    "Your game is saved. You can carry on later."
                },
                onYes = {
                    sound.play(Sound.BUTTON)
                    confirm = ConfirmKind.NONE
                    onLeave()
                },
                onNo = {
                    sound.play(Sound.BUTTON)
                    confirm = ConfirmKind.NONE
                }
            )
            ConfirmKind.RESTART -> ConfirmDialog(
                title = "Restart this round?",
                message = "Your Wins are kept.",
                onYes = {
                    sound.play(Sound.BUTTON)
                    confirm = ConfirmKind.NONE
                    menuOpen = false
                    controller.restartRound()
                },
                onNo = {
                    sound.play(Sound.BUTTON)
                    confirm = ConfirmKind.NONE
                }
            )
            ConfirmKind.NONE -> Unit
        }
    }
}

/** Opens the Play Store page of SnakeMate; falls back to the website when no store app answers. */
private fun openRatePage(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.westly.snakes"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
    } catch (e: ActivityNotFoundException) {
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=com.westly.snakes")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(web)
        } catch (e2: ActivityNotFoundException) {
            // no app can open a link: nothing more to do
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
