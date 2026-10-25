package com.westly.snakes.data

import android.content.Context
import android.content.SharedPreferences

/** A game in progress, saved at the start of every turn so the player can carry on later. */
class SavedGame(
    /** Square of the bottom (red) pawn, 0 while it still waits in its circle. */
    val humanSquare: Int,
    /** Square of the top (blue) pawn, 0 while it still waits in its circle. */
    val opponentSquare: Int,
    /** True when it is the bottom player's turn. */
    val humanTurn: Boolean
)

/**
 * Everything the game keeps on the phone, in the SharedPreferences file `snakemate_prefs`:
 * the Wins of each player, the sound and vibration switches, and the game in progress
 * (one per mode: You & Computer and Multiplayer).
 */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var winsMe: Int
        get() = sp.getInt(KEY_WINS_ME, 0)
        set(value) { sp.edit().putInt(KEY_WINS_ME, value).apply() }

    var winsComputer: Int
        get() = sp.getInt(KEY_WINS_COMPUTER, 0)
        set(value) { sp.edit().putInt(KEY_WINS_COMPUTER, value).apply() }

    var winsP1: Int
        get() = sp.getInt(KEY_WINS_P1, 0)
        set(value) { sp.edit().putInt(KEY_WINS_P1, value).apply() }

    var winsP2: Int
        get() = sp.getInt(KEY_WINS_P2, 0)
        set(value) { sp.edit().putInt(KEY_WINS_P2, value).apply() }

    var soundOn: Boolean
        get() = sp.getBoolean(KEY_SOUND_ON, true)
        set(value) { sp.edit().putBoolean(KEY_SOUND_ON, value).apply() }

    var vibrationOn: Boolean
        get() = sp.getBoolean(KEY_VIBRATION_ON, true)
        set(value) { sp.edit().putBoolean(KEY_VIBRATION_ON, value).apply() }

    // ------------------------------------------------------------ game in progress

    /** Saves the game in progress of one mode (multiplayer = true for Multiplayer). */
    fun saveGame(multiplayer: Boolean, game: SavedGame) {
        val p = gamePrefix(multiplayer)
        sp.edit()
            .putBoolean(p + "exists", true)
            .putInt(p + "human", game.humanSquare)
            .putInt(p + "opponent", game.opponentSquare)
            .putBoolean(p + "human_turn", game.humanTurn)
            .apply()
    }

    /** The saved game of one mode, or null when there is none (or the saved data is not valid). */
    fun loadGame(multiplayer: Boolean): SavedGame? {
        val p = gamePrefix(multiplayer)
        if (!sp.getBoolean(p + "exists", false)) return null
        val human = sp.getInt(p + "human", 0)
        val opponent = sp.getInt(p + "opponent", 0)
        if (human !in 0..99 || opponent !in 0..99) return null
        return SavedGame(human, opponent, sp.getBoolean(p + "human_turn", true))
    }

    /** Forgets the saved game of one mode (a round ended or was restarted). */
    fun clearGame(multiplayer: Boolean) {
        val p = gamePrefix(multiplayer)
        sp.edit()
            .remove(p + "exists")
            .remove(p + "human")
            .remove(p + "opponent")
            .remove(p + "human_turn")
            .apply()
    }

    /** True when a valid saved game exists for the mode. */
    fun hasSavedGame(multiplayer: Boolean): Boolean = loadGame(multiplayer) != null

    private fun gamePrefix(multiplayer: Boolean): String = if (multiplayer) "game_mp_" else "game_vs_"

    private companion object {
        const val FILE_NAME = "snakemate_prefs"
        const val KEY_WINS_ME = "wins_me"
        const val KEY_WINS_COMPUTER = "wins_computer"
        const val KEY_WINS_P1 = "wins_p1"
        const val KEY_WINS_P2 = "wins_p2"
        const val KEY_SOUND_ON = "sound_on"
        const val KEY_VIBRATION_ON = "vibration_on"
    }
}
