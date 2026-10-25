package com.westly.snakes.game

/** The outcome of one roll for one pawn. See [resolveMove]. */
sealed class MoveResult {
    /** Plain move to [to]. */
    data class Walk(val to: Int) : MoveResult()

    /** The pawn lands on a ladder bottom ([landing]) and climbs to [to]. */
    data class Climb(val landing: Int, val to: Int) : MoveResult()

    /** The pawn lands on a snake head ([landing]) and slides down to [to]. */
    data class Slide(val landing: Int, val to: Int) : MoveResult()

    /** The roll is too big near the end: the pawn stays and the turn passes. */
    object NoMove : MoveResult()

    /** The pawn reaches square 100. */
    object Win : MoveResult()
}

/**
 * Pure rule for one move.
 *
 * - From square 98 or 99 any total wins.
 * - Otherwise the exact number wins and a bigger total is ignored.
 * - Otherwise the pawn lands on position + total: a ladder bottom climbs, a snake head slides,
 *   anything else is a plain walk. Ladder tops and snake tails do nothing.
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

private class CheckRow(val position: Int, val total: Int, val expected: String)

private fun describe(result: MoveResult): String = when (result) {
    is MoveResult.Walk -> "Walk to ${result.to}"
    is MoveResult.Climb -> "lands ${result.landing}, Climb to ${result.to}"
    is MoveResult.Slide -> "lands ${result.landing}, Slide to ${result.to}"
    MoveResult.NoMove -> "NoMove"
    MoveResult.Win -> "Win"
}

private val CHECK_TABLE = listOf(
    CheckRow(5, 3, "lands 8, Climb to 30"),
    CheckRow(5, 8, "lands 13, Slide to 5"),
    CheckRow(30, 7, "Walk to 37"),
    CheckRow(37, 5, "Walk to 42"),
    CheckRow(42, 10, "Walk to 52"),
    CheckRow(52, 9, "Walk to 61"),
    CheckRow(61, 4, "Walk to 65"),
    CheckRow(65, 10, "lands 75, Climb to 95"),
    CheckRow(39, 5, "lands 44, Climb to 65"),
    CheckRow(69, 6, "lands 75, Climb to 95"),
    CheckRow(1, 12, "lands 13, Slide to 5"),
    CheckRow(60, 3, "lands 63, Climb to 81"),
    CheckRow(50, 4, "lands 54, Climb to 91"),
    CheckRow(23, 10, "lands 33, Climb to 51"),
    CheckRow(48, 5, "lands 53, Slide to 29"),
    CheckRow(59, 5, "lands 64, Slide to 24"),
    CheckRow(95, 5, "Win"),
    CheckRow(95, 6, "NoMove"),
    CheckRow(95, 4, "Walk to 99"),
    CheckRow(97, 3, "Win"),
    CheckRow(97, 4, "NoMove"),
    CheckRow(97, 2, "Walk to 99"),
    CheckRow(98, 2, "Win"),
    CheckRow(99, 12, "Win")
)

/**
 * Runs the agreed self-check table through [resolveMove] and returns the rows that fail.
 * An empty list means every row passed. The app never calls this.
 */
fun selfCheck(): List<String> {
    val failures = ArrayList<String>()
    for (row in CHECK_TABLE) {
        val actual = describe(resolveMove(row.position, row.total))
        if (actual != row.expected) {
            failures.add("position ${row.position}, total ${row.total}: expected ${row.expected} but got $actual")
        }
    }
    return failures
}
