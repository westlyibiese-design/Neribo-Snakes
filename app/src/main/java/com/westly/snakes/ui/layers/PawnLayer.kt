package com.westly.snakes.ui.layers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.westly.snakes.game.Seat
import com.westly.snakes.ui.GameLayout
import com.westly.snakes.ui.components.PawnColor
import com.westly.snakes.ui.components.drawPawn

private val RingSoft = Color(0x33FFFFFF)
private val RingMain = Color(0x99FFFFFF) // 60% white

/**
 * Draws both pawns above the board, plus the soft white ring under the pawn that is moving.
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
    if (animator.isMoving(seat)) {
        val ringCenter = Offset(x, ground)
        drawCircle(color = RingSoft, radius = 0.5f * cell, center = ringCenter, style = Stroke(width = 0.14f * cell))
        drawCircle(color = RingMain, radius = 0.5f * cell, center = ringCenter, style = Stroke(width = 0.05f * cell))
    }
    drawPawn(
        baseCenter = Offset(x, ground - animator.liftPx(seat)),
        height = 0.9f * cell,
        color = if (seat == Seat.HUMAN) PawnColor.RED else PawnColor.BLUE
    )
}
