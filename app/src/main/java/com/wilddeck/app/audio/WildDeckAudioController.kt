package com.wilddeck.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.wilddeck.app.R
import kotlin.random.Random

class WildDeckAudioController(context: Context) {
    private val appContext = context.applicationContext
    private val random = Random.Default
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val sounds = mapOf(
        Effect.PLAYER_ATTACK to soundPool.load(appContext, R.raw.sfx_player_attacking, 1),
        Effect.ENEMY_DAMAGE to soundPool.load(appContext, R.raw.sfx_enemy_damage, 1),
        Effect.TOUCH_HOLD_CARD to soundPool.load(appContext, R.raw.sfx_touch_hold_card, 1),
        Effect.EXTRA_1 to soundPool.load(appContext, R.raw.sfx_extra_1, 1),
        Effect.EXTRA_2 to soundPool.load(appContext, R.raw.sfx_extra_2, 1),
        Effect.EXTRA_3 to soundPool.load(appContext, R.raw.sfx_extra_3, 1)
    )

    private val battleTracks = listOf(
        R.raw.music_glitch_medusa,
        R.raw.music_glitch_panacea
    )
    private var enabled = true
    private var musicPlayer: MediaPlayer? = null
    private var currentMusicResId: Int? = null
    private var lastBattleTrackResId: Int? = null

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!enabled) {
            musicPlayer?.pause()
        } else {
            musicPlayer?.start()
        }
    }

    fun playMainTheme() {
        playMusic(R.raw.music_bumbumchack, loop = true)
    }

    fun playBattleMusic() {
        val nextTrack = battleTracks
            .filterNot { it == lastBattleTrackResId }
            .ifEmpty { battleTracks }
            .random(random)
        lastBattleTrackResId = nextTrack
        playMusic(nextTrack, loop = false, onComplete = ::playBattleMusic)
    }

    fun play(effect: Effect) {
        if (!enabled) return
        sounds[effect]?.let { soundId ->
            soundPool.play(soundId, 0.85f, 0.85f, 1, 0, 1f)
        }
    }

    fun release() {
        musicPlayer?.release()
        musicPlayer = null
        soundPool.release()
    }

    private fun playMusic(resId: Int, loop: Boolean, onComplete: (() -> Unit)? = null) {
        if (currentMusicResId == resId && musicPlayer?.isPlaying == true) return
        musicPlayer?.release()
        currentMusicResId = resId
        musicPlayer = MediaPlayer.create(appContext, resId)?.apply {
            isLooping = loop
            setVolume(0.55f, 0.55f)
            setOnCompletionListener {
                if (!loop) onComplete?.invoke()
            }
            if (enabled) start()
        }
    }

    enum class Effect {
        PLAYER_ATTACK,
        ENEMY_DAMAGE,
        TOUCH_HOLD_CARD,
        EXTRA_1,
        EXTRA_2,
        EXTRA_3
    }
}
