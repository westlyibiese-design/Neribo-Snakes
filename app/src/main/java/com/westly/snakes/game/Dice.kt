package com.westly.snakes.game

import kotlin.random.Random

/** Rolls two dice, each independently and uniformly 1-6. The move is always the total of both. */
fun rollTwoDice(random: Random = Random.Default): Pair<Int, Int> {
    return Pair(random.nextInt(1, 7), random.nextInt(1, 7))
}
