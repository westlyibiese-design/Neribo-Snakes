package com.westly.snakes.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.westly.snakes.ui.GameColors

/** Black "Your Turn" pill: top corners rounded 0.03W, bottom corners square. */
fun DrawScope.drawTurnPill(rect: Rect, boardSize: Float, text: String = "Your Turn") {
    val radius = CornerRadius(boardSize * 0.03f)
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = rect,
                topLeft = radius,
                topRight = radius,
                bottomRight = CornerRadius.Zero,
                bottomLeft = CornerRadius.Zero
            )
        )
    }
    drawPath(path = path, color = GameColors.PillBlack)
    drawOutlinedText(
        text = text,
        anchor = Offset(rect.center.x, rect.center.y),
        sizePx = boardSize * 0.06f,
        fillColor = GameColors.PillText,
        outlineColor = null,
        align = LabelAlign.CENTER
    )
}
