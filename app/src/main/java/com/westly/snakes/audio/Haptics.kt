package com.westly.snakes.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.westly.snakes.data.Prefs

/**
 * Short vibrations: a snake bite and a win. Nothing happens while the vibration switch is off or
 * when the phone has no vibrator.
 */
class Haptics(context: Context) {
    private val prefs = Prefs(context)
    private val vibrator: Vibrator?

    init {
        val appContext = context.applicationContext
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** One 60 ms buzz. */
    fun snakeBite() {
        vibrate(longArrayOf(0L, 60L))
    }

    /** Two 80 ms buzzes with a 60 ms pause between them. */
    fun win() {
        vibrate(longArrayOf(0L, 80L, 60L, 80L))
    }

    private fun vibrate(pattern: LongArray) {
        val target = vibrator ?: return
        if (!prefs.vibrationOn || !target.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                target.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                target.vibrate(pattern, -1)
            }
        } catch (e: SecurityException) {
            // no permission or not allowed: stay silent
        }
    }
}
