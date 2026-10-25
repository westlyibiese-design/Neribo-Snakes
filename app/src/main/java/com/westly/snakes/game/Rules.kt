package com.westly.snakes.game

/** The outcome of one roll for one pawn. */
sealed class MoveResult {
    /** Plain move to square [to]. */
    data class Walk(val to: Int) : MoveResult()

    /** The pawn lands on a ladder bottom ([landing]) and climbs to [to]. */
    data class Climb(val landing: Int, val to: Int) : MoveResult()

    /** The pawn lands on a snake head ([landing]) and slides to [to]. */
    data class Slide(val landing: Int, val to: Int) : MoveResult()

    /** The roll cannot be used. The pawn stays and the turn still passes. */
    object NoMove : MoveResult() {
        override fun toString(): String = "NoMove"
    }

    /** The pawn reaches square 100. */
    object Win : MoveResult() {
        override fun toString(): String = "Win"
    }
}

/**
 * Pure move rule. [position] is the pawn's square (1-99), [total] is the total of both dice.
 *
 * 1. From 98 or 99 any total wins.
 * 2. total == 100 - position wins.
 * 3. total > 100 - position is no move.
 * 4. Otherwise the pawn lands on position + total: a ladder bottom climbs, a snake head slides,
 *    anything else is a plain walk.
 */
fun resolveMove(position: Int, total: Int): MoveResult {
    val remaining = BoardData.LAST_SQUARE - position
    if (position >= 98) return MoveResult.Win
    if (total == remaining) return MoveResult.Win
    if (total > remaining) return MoveResult.NoMove
    val landing = position + total
    val ladderTop = BoardData.LADDERS[landing]
    if (ladderTop != null) return MoveResult.Climb(landing, ladderTop)
    val snakeTail = BoardData.SNAKES[landing]
    if (snakeTail != null) return MoveResult.Slide(landing, snakeTail)
    return MoveResult.Walk(landing)
}

/**
 * Checks every row of the self-check table against [resolveMove] and returns a description of each
 * row that fails. An empty list means every row passes. The app never calls this.
 */
fun selfCheck(): List<String> {
    val rows: List<Triple<Int, Int, MoveResult>> = listOf(
        Triple(5, 3, MoveResult.Climb(8, 30)),
        Triple(5, 8, MoveResult.Slide(13, 5)),
        Triple(30, 7, MoveResult.Walk(37)),
        Triple(37, 5, MoveResult.Walk(42)),
        Triple(42, 10, MoveResult.Walk(52)),
        Triple(52, 9, MoveResult.Walk(61)),
        Triple(61, 4, MoveResult.Walk(65)),
        Triple(65, 10, MoveResult.Climb(75, 95)),
        Triple(39, 5, MoveResult.Climb(44, 65)),
        Triple(69, 6, MoveResult.Climb(75, 95)),
        Triple(1, 12, MoveResult.Slide(13, 5)),
        Triple(60, 3, MoveResult.Climb(63, 81)),
        Triple(50, 4, MoveResult.Climb(54, 91)),
        Triple(23, 10, MoveResult.Climb(33, 51)),
        Triple(48, 5, MoveResult.Slide(53, 29)),
        Triple(59, 5, MoveResult.Slide(64, 24)),
        Triple(95, 5, MoveResult.Win),
        Triple(95, 6, MoveResult.NoMove),
        Triple(95, 4, MoveResult.Walk(99)),
        Triple(97, 3, MoveResult.Win),
        Triple(97, 4, MoveResult.NoMove),
        Triple(97, 2, MoveResult.Walk(99)),
        Triple(98, 2, MoveResult.Win),
        Triple(99, 12, MoveResult.Win)
    )
    val failures = ArrayList<String>()
    for (row in rows) {
        val actual = resolveMove(row.first, row.second)
        if (actual != row.third) {
            failures.add("position ${row.first}, total ${row.second}: expected ${row.third} but got $actual")
        }
    }
    return failures
}
