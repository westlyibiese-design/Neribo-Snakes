package com.westly.snakes.game

/**
 * Fixed layout of the SnakeMate board: 100 squares, 1 at the bottom-left,
 * numbering zig-zags upward, 100 at the top-left.
 */
object BoardData {
    const val SIZE = 10
    const val LAST_SQUARE = 100

    /** Ladders: bottom square to top square. */
    val LADDERS: Map<Int, Int> = mapOf(
        8 to 30,
        17 to 42,
        27 to 45,
        33 to 51,
        44 to 65,
        54 to 91,
        63 to 81,
        75 to 95,
        83 to 98
    )

    /** Snakes: head square to tail square. */
    val SNAKES: Map<Int, Int> = mapOf(
        96 to 62,
        87 to 15,
        80 to 43,
        64 to 24,
        53 to 29,
        41 to 19,
        31 to 10,
        25 to 3,
        13 to 5
    )

    /** Row counted from the bottom: 0 for squares 1-10, 9 for squares 91-100. */
    fun rowFromBottom(square: Int): Int = (square - 1) / SIZE

    /** Column counted from the left: 0..9. */
    fun column(square: Int): Int {
        val index = (square - 1) % SIZE
        return if (rowFromBottom(square) % 2 == 0) index else SIZE - 1 - index
    }
}
