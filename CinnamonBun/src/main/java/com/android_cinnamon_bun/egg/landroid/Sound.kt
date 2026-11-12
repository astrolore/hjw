/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android_cinnamon_bun.egg.landroid

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// Sound, all generated in code (no audio files): soft effects, an engine hum that follows the
// thrust, and quiet generative ambient music for every world. The phone's volume buttons are the
// master volume; the three sliders in Settings only mix music, effects and engine.
// ---------------------------------------------------------------------------------------------

private const val FX_RATE = 22050
private val TWO_PI = (2.0 * PI).toFloat()

internal fun attrs(music: Boolean) =
    AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(if (music) AudioAttributes.CONTENT_TYPE_MUSIC else AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

object Sound {
    enum class Fx {
        BLIP,
        CHIME,
        THUD,
        BOUNCE,
        AUTO_ON,
        AUTO_OFF,
    }

    @Volatile private var started = false
    @Volatile @JvmField internal var paused = false

    private val fxTracks = ConcurrentHashMap<Fx, AudioTrack>()
    @Volatile private var engineTrack: AudioTrack? = null
    private var engineLevel = 0f
    private var enginePlaying = false
    private var engineRate = 0


    // ------------------------------------------------------------------ setup

    /** Builds the effect buffers off the main thread. Safe to call more than once. */
    fun init() {
        if (started) return
        started = true
        Thread {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                try {
                    for (fx in Fx.values()) {
                        val data = makeFx(fx)
                        buildStatic(data, loop = false)?.let { fxTracks[fx] = it }
                    }
                    engineTrack = buildStatic(makeEngineLoop(), loop = true)
                } catch (e: Throwable) {
                    // no sound is better than no game
                }
            }
            .start()
    }

    private fun buildStatic(data: ShortArray, loop: Boolean): AudioTrack? =
        try {
            val bytes = data.size * 2
            val t =
                AudioTrack.Builder()
                    .setAudioAttributes(attrs(false))
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(FX_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            t.write(data, 0, data.size)
            if (loop) t.setLoopPoints(0, data.size, -1)
            t
        } catch (e: Throwable) {
            null
        }

    // ------------------------------------------------------------------ effects

    /** Plays a short effect at the effects volume (times [scale]). */
    fun play(fx: Fx, scale: Float = 1f) {
        if (paused) return
        val vol = GameSettings.effectsVolume * scale
        if (vol < 0.01f) return
        val t = fxTracks[fx] ?: return
        try {
            t.stop()
            t.reloadStaticData()
            t.setVolume(vol.coerceIn(0f, 1f))
            t.play()
        } catch (e: Throwable) {
            // ignore
        }
    }

    private fun synth(seconds: Float, f: (Float) -> Float): ShortArray {
        val n = (seconds * FX_RATE).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i / FX_RATE.toFloat()
            // a short fade at the very end so nothing ever clicks
            val tail = min(1f, (n - i) / 200f)
            out[i] = (f(t).coerceIn(-1f, 1f) * tail * 32000f).toInt().toShort()
        }
        return out
    }

    private fun note(t: Float, start: Float, freq: Float, decay: Float, amp: Float): Float {
        val x = t - start
        if (x < 0f) return 0f
        val env = min(1f, x / 0.006f) * exp(-x * decay)
        return (sin(TWO_PI * freq * x) + 0.35f * sin(TWO_PI * freq * 2f * x)) * env * amp
    }

    private fun makeFx(fx: Fx): ShortArray =
        when (fx) {
            Fx.BLIP -> synth(0.09f) { t -> note(t, 0f, 880f, 42f, 0.35f) }
            Fx.CHIME ->
                synth(1.3f) { t ->
                    note(t, 0f, 784f, 3.6f, 0.30f) + note(t, 0.13f, 1175f, 3.2f, 0.26f) + note(t, 0.28f, 1568f, 3.4f, 0.16f)
                }
            Fx.THUD ->
                synth(0.34f) { t ->
                    val ph = TWO_PI * (55f * t + (70f / 16f) * (1f - exp(-16f * t)))
                    sin(ph) * min(1f, t / 0.004f) * exp(-t * 12f) * 0.95f
                }
            Fx.BOUNCE ->
                synth(0.22f) { t ->
                    val ph = TWO_PI * (85f * t + (120f / 22f) * (1f - exp(-22f * t)))
                    sin(ph) * min(1f, t / 0.003f) * exp(-t * 20f) * 0.85f
                }
            Fx.AUTO_ON -> synth(0.4f) { t -> note(t, 0f, 523f, 13f, 0.3f) + note(t, 0.09f, 784f, 11f, 0.3f) }
            Fx.AUTO_OFF -> synth(0.4f) { t -> note(t, 0f, 659f, 13f, 0.3f) + note(t, 0.09f, 440f, 11f, 0.3f) }
        }

    /** One second of a low, smooth hum. Whole numbers of cycles, so the loop never clicks. */
    private fun makeEngineLoop(): ShortArray =
        synth(1f) { t ->
            val trem = 0.8f + 0.2f * sin(TWO_PI * 2f * t)
            (0.5f * sin(TWO_PI * 55f * t) * trem + 0.28f * sin(TWO_PI * 83f * t) + 0.16f * sin(TWO_PI * 110f * t)) * 0.8f
        }

    // ------------------------------------------------------------------ engine (called every frame)

    /** [target] is 1 while thrusting, 0 otherwise; [speedNorm] (0..1) raises the pitch a little. */
    fun engine(target: Float, speedNorm: Float) {
        val t = engineTrack ?: return
        engineLevel += (target - engineLevel) * 0.12f
        val vol = engineLevel * GameSettings.engineVolume * 0.7f
        try {
            if (vol < 0.004f || paused) {
                if (enginePlaying) {
                    t.pause()
                    enginePlaying = false
                }
                return
            }
            if (!enginePlaying) {
                t.play()
                enginePlaying = true
            }
            t.setVolume(vol.coerceIn(0f, 1f))
            val rate = (FX_RATE * (0.9f + 0.35f * speedNorm.coerceIn(0f, 1f))).toInt()
            if (abs(rate - engineRate) > 400) {
                t.playbackRate = rate
                engineRate = rate
            }
        } catch (e: Throwable) {
            // ignore
        }
    }

    // ------------------------------------------------------------------ lifecycle

    /** The game went to the background: everything goes quiet. */
    fun pause() {
        lifecyclePaused = true
        paused = true
        engineTrack?.let {
            try {
                it.pause()
            } catch (e: Throwable) {
                // ignore
            }
        }
        enginePlaying = false
    }

    fun resume() {
        lifecyclePaused = false
        paused = focusLost
        // coming back: ask for the audio focus again (a call or another app may have had it)
        try {
            val am = audioManager
            val req = focusRequest
            if (am != null && req != null) {
                val result = am.requestAudioFocus(req)
                if (result == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    focusLost = false
                    paused = false
                }
            }
        } catch (e: Throwable) {
            // ignore
        }
    }

    // ------------------------------------------------------------------ music

    /** The line shown under the world info in the menu. */
    @Volatile var musicInfo: String = ""

    /** Starts the music of a world: the Void and Heaven have scripted scores, the rest are composed. */
    fun startMusic(seed: Long) {
        val score: Score =
            when (seed) {
                VOID_SEED -> VoidScore()
                7L -> HeavenScore()
                else -> WorldScore(seed)
            }
        musicInfo = score.info
        MusicEngine.start(score)
    }

    fun stopMusic() {
        MusicEngine.stop()
    }

    /** A scripted effect (the Void): fixed level, ignores the effects slider. */
    fun sfx(kind: Int, gain: Float = 1f, pan: Float = 0f) {
        MusicEngine.playNow(kind, 0, gain, pan, 1)
    }

    /** A note from the music instruments, at the music volume (Heaven's light pulses). */
    fun note(kind: Int, midi: Int, gain: Float, pan: Float = 0f) {
        MusicEngine.playNow(kind, midi, gain, pan, 0)
    }

    // ------------------------------------------------------------------ audio focus

    private var audioManager: android.media.AudioManager? = null
    private var focusRequest: android.media.AudioFocusRequest? = null
    private var lifecyclePaused = false
    private var focusLost = false

    private val focusListener =
        android.media.AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                android.media.AudioManager.AUDIOFOCUS_GAIN -> {
                    focusLost = false
                    MusicEngine.duck = 1f
                }
                android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> MusicEngine.duck = 0.3f
                else -> focusLost = true
            }
            paused = lifecyclePaused || focusLost
        }

    /** Gives the game's audio a proper place next to calls and other apps. */
    fun initFocus(context: android.content.Context) {
        try {
            val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager = am
            val req =
                android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs(true))
                    .setOnAudioFocusChangeListener(focusListener)
                    .build()
            focusRequest = req
            am.requestAudioFocus(req)
        } catch (e: Throwable) {
            // ignore
        }
    }
}
