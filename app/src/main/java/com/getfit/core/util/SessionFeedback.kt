package com.getfit.core.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Fires a short vibration + tone the moment rest ends and work resumes automatically — the one
 * point in the session flow with no user tap to hang existing feedback off of (every other cue in
 * SessionScreen.kt fires from a Compose click handler via LocalHapticFeedback/View.playSoundEffect,
 * neither of which is reachable from SessionController's background ticker coroutine). Respects the
 * same sound/haptics settings as every other feedback point in the app.
 */
object SessionFeedback {
    fun restEnded(context: Context, sound: Boolean, haptics: Boolean) {
        if (haptics) vibrate(context)
        if (sound) beep()
    }

    private fun vibrate(context: Context) {
        runCatching {
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 80), -1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(effect)
            }
        }
    }

    /** ToneGenerator plays asynchronously on its own thread; released ~400ms later (after the
     *  150ms tone finishes) rather than immediately, which would cut it off. */
    private fun beep() {
        runCatching {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 150)
            Handler(Looper.getMainLooper()).postDelayed({ runCatching { tg.release() } }, 400)
        }
    }
}
