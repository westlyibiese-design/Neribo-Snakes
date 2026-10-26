package com.westly.snakes.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.westly.snakes.R
import com.westly.snakes.data.Prefs

/** The sounds of the game. Each one is a WAV file in `res/raw`. */
enum class Sound { DICE_ROLL, STEP, LADDER_UP, SNAKE_DOWN, WIN, BUTTON }

/**
 * Plays the game sounds. Every WAV file is loaded once into a [SoundPool] when the engine is created,
 * so playing a sound later is instant and sounds can overlap (rapid steps). [play] does nothing
 * while the Sound switch is off. Call [release] when the activity is destroyed.
 */
class SoundEngine(context: Context) {
    private val prefs = Prefs(context)
    private val pool: SoundPool
    private val ids = IntArray(Sound.values().size)
    private val ready = BooleanArray(Sound.values().size)
    @Volatile
    private var released = false

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attributes)
            .build()
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                for (i in ids.indices) {
                    if (ids[i] == sampleId) ready[i] = true
                }
            }
        }
        for (sound in Sound.values()) {
            ids[sound.ordinal] = pool.load(context.applicationContext, resourceOf(sound), 1)
        }
    }

    /** Plays [sound] once, unless the Sound switch is off or the sound is not loaded yet. */
    fun play(sound: Sound) {
        if (released || !prefs.soundOn) return
        val index = sound.ordinal
        if (!ready[index]) return
        pool.play(ids[index], 1f, 1f, 1, 0, 1f)
    }

    /** Frees the sound player. Safe to call more than once. */
    fun release() {
        if (released) return
        released = true
        pool.release()
    }

    private fun resourceOf(sound: Sound): Int = when (sound) {
        Sound.DICE_ROLL -> R.raw.dice_roll
        Sound.STEP -> R.raw.step
        Sound.LADDER_UP -> R.raw.ladder_up
        Sound.SNAKE_DOWN -> R.raw.snake_down
        Sound.WIN -> R.raw.win
        Sound.BUTTON -> R.raw.button
    }
}
