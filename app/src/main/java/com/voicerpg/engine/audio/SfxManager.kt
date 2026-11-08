package com.voicerpg.engine.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

/**
 * Sound Effects (SFX) Manager for the Visual Novel Engine.
 * Plays dialogue cues, scene sound effects, and UI feedback from assets/audio/sfx/.
 */
class SfxManager(
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "SfxManager"
        private const val MAX_STREAMS = 4
    }

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()
    var isMuted: Boolean = false

    init {
        if (context != null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(audioAttributes)
                .build()
        }
    }

    fun playSfx(assetPath: String, volume: Float = 1.0f) {
        if (isMuted || context == null || soundPool == null) return
        val pool = soundPool ?: return

        try {
            val soundId = soundMap.getOrPut(assetPath) {
                var afd: AssetFileDescriptor? = null
                try {
                    afd = context.assets.openFd(assetPath)
                    pool.load(afd, 1)
                } finally {
                    afd?.close()
                }
            }
            pool.play(soundId, volume, volume, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play SFX $assetPath: ${e.message}")
        }
    }

    fun mute(muted: Boolean) {
        isMuted = muted
    }

    fun destroy() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}
