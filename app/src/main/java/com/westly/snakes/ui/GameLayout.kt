package com.westly.snakes.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.min

/**
 * Pure maths for the game screen. Every value is in screen pixels.
 * W (boardSize) is the board side; all other sizes are fractions of W.
 */
class GameLayout(
    screenWidthPx: Float,
    screenHeightPx: Float,
    topInsetPx: Float,
    bottomInsetPx: Float,
    density: Float
) {
    private val availableHeight = screenHeightPx - topInsetPx - bottomInsetPx

    /** W: the board side. */
    val boardSize: Float = min(
        min(screenWidthPx - 12f * density, 520f * density),
        (availableHeight - 8f * density) / 2.03f
    ).coerceAtLeast(1f)

    val boardLeft: Float = (screenWidthPx - boardSize) / 2f
    val boardTop: Float = topInsetPx + 0.565f * boardSize + (availableHeight - 2.03f * boardSize) / 2f

    /** Side of one square: the board has a 2.4% frame on each side. */
    val cell: Float = (boardSize - 2f * boardSize * 0.024f) / 10f

    private val centerX: Float = boardLeft + 0.5f * boardSize
    private val boardBottom: Float = boardTop + boardSize

    /** Centre of a square in screen coordinates. */
    fun squareCenter(square: Int): Offset {
        val local = Geo(boardSize).center(square)
        return Offset(boardLeft + local.x, boardTop + local.y)
    }

    // Top (Computer) side
    val roundButtonDiameter: Float = 0.13f * boardSize
    val homeCenter: Offset = Offset(boardLeft + 0.065f * boardSize, boardTop - 0.495f * boardSize)
    val menuCenter: Offset = Offset(boardLeft + boardSize - 0.065f * boardSize, boardTop - 0.495f * boardSize)

    val trayWidth: Float = 0.25f * boardSize
    val trayHeight: Float = 0.14f * boardSize
    val circleDiameter: Float = 0.115f * boardSize

    val topTrayCenter: Offset = Offset(centerX, boardTop - 0.286f * boardSize)
    val bottomTrayCenter: Offset = Offset(centerX, boardBottom + 0.281f * boardSize)

    fun trayRect(top: Boolean): Rect {
        val c = if (top) topTrayCenter else bottomTrayCenter
        return Rect(
            c.x - trayWidth / 2f,
            c.y - trayHeight / 2f,
            c.x + trayWidth / 2f,
            c.y + trayHeight / 2f
        )
    }

    val topSlotCenter: Offset = Offset(boardLeft + 0.295f * boardSize, topTrayCenter.y)
    val bottomSlotCenter: Offset = Offset(boardLeft + 0.295f * boardSize, bottomTrayCenter.y)
    val topCounterCenter: Offset = Offset(boardLeft + 0.705f * boardSize, topTrayCenter.y)
    val bottomCounterCenter: Offset = Offset(boardLeft + 0.705f * boardSize, bottomTrayCenter.y)

    val diceSizeInTray: Float = 0.104f * boardSize
    val diceSizeOnBoard: Float = 0.112f * boardSize

    /** Centres of the two dice resting inside a tray. */
    fun diceCenters(top: Boolean): Pair<Offset, Offset> {
        val c = if (top) topTrayCenter else bottomTrayCenter
        val dx = 0.057f * boardSize
        return Pair(Offset(c.x - dx, c.y), Offset(c.x + dx, c.y))
    }

    // Labels (name labels are centred; Wins and Rank anchors are the left edge, vertically centred)
    val topNameCenter: Offset = Offset(centerX, boardTop - 0.057f * boardSize)
    val bottomNameCenter: Offset = Offset(centerX, boardBottom + 0.043f * boardSize)
    val topWinsAnchor: Offset = Offset(boardLeft + 0.005f * boardSize, boardTop - 0.06f * boardSize)
    val bottomWinsAnchor: Offset = Offset(boardLeft + 0.005f * boardSize, boardBottom + 0.125f * boardSize)
    val bottomRankAnchor: Offset = Offset(boardLeft + 0.005f * boardSize, boardBottom + 0.038f * boardSize)

    /** "Your Turn" pill: bottom edge on the bottom inset line, centred. */
    val pillRect: Rect = Rect(
        centerX - 0.2f * boardSize,
        screenHeightPx - bottomInsetPx - 0.083f * boardSize,
        centerX + 0.2f * boardSize,
        screenHeightPx - bottomInsetPx
    )

    /** Fingertip positions: 0.02W above the top edge of the tray. */
    val topHandAnchor: Offset = Offset(centerX, topTrayCenter.y - trayHeight / 2f - 0.02f * boardSize)
    val bottomHandAnchor: Offset = Offset(centerX, bottomTrayCenter.y - trayHeight / 2f - 0.02f * boardSize)
}
