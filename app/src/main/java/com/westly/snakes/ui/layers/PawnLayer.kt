package com.westly.snakes.ui.layers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.westly.snakes.game.Seat
import com.westly.snakes.ui.GameLayout
import com.westly.snakes.ui.components.PawnColor
import com.westly.snakes.ui.components.drawPawn

private val HaloGlow = Color(0x664FE3F0) // soft teal glow
private val HaloMain = Color(0xE6FFFFFF) // dashed white ring

/**
 * Draws both pawns above the board, plus the teal halo ring the original game shows around a pawn
 * just before it moves.
 * It reads animation state only inside the draw lambda and does not touch input.
 */
@Composable
fun PawnLayer(layout: GameLayout, animator: PawnAnimator) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // the moving pawn is drawn last so it is never hidden by the other one
        val first = if (animator.isMoving(Seat.HUMAN)) Seat.OPPONENT else Seat.HUMAN
        val second = if (first == Seat.HUMAN) Seat.OPPONENT else Seat.HUMAN
        drawSeat(layout, animator, first)
        drawSeat(layout, animator, second)
    }
}

private fun DrawScope.drawSeat(layout: GameLayout, animator: PawnAnimator, seat: Seat) {
    val cell = layout.cell
    val x = animator.drawX(seat)
    val ground = animator.groundY(seat)
    val halo = animator.haloProgress(seat)
    if (halo >= 0f) {
        val ringCenter = Offset(x, ground - 0.12f * cell)
        val radius = (0.55f + 0.08f * halo) * cell
        drawCircle(color = HaloGlow, radius = radius, center = ringCenter, style = Stroke(width = 0.16f * cell))
        drawCircle(
            color = HaloMain,
            radius = radius,
            center = ringCenter,
            style = Stroke(
                width = 0.045f * cell,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.14f * cell, 0.1f * cell))
            )
        )
    }
    if (animator.inCircle(seat)) return // waiting in its circle: the game screen draws it there
    drawPawn(
        baseCenter = Offset(x, ground - animator.liftPx(seat)),
        height = 0.9f * cell,
        color = if (seat == Seat.HUMAN) PawnColor.RED else PawnColor.BLUE
    )
}
