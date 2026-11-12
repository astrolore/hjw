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

import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// The audio engine behind the music.
//
//   Score (composer thread)  ->  decides which note starts when, asks the NoteBank for the sound
//   NoteBank                 ->  synthesises each note once, ahead of time, and caches it
//   Mixer (audio thread)     ->  only adds ready-made buffers together, runs the reverb, writes
//
// Heavy synthesis never happens on the audio thread, so it can not make the sound stutter. The
// mixer measures itself and lowers the number of simultaneous notes if the phone falls behind,
// restarts the track if the system stops it, and fades in slowly after every start.
// ---------------------------------------------------------------------------------------------

internal const val AR = 24000

private val SINE_TABLE = FloatArray(4097) { sin(2.0 * PI * it / 4096).toFloat() }

internal fun sinT(ph: Float): Float {
    val x = (ph - floor(ph)) * 4096f
    val i = x.toInt().coerceAtMost(4095)
    val f = x - i
    return SINE_TABLE[i] + (SINE_TABLE[i + 1] - SINE_TABLE[i]) * f
}

internal fun midiHz(m: Float): Float = 440f * 2f.pow((m - 69f) / 12f)

/** One note or sound starting at an absolute sample of the mixer's clock. */
internal class Play(
    val buf: FloatArray,
    val start: Long,
    val gain: Float,
    val panL: Float,
    val panR: Float,
    val rate: Float,
    val bus: Int,
) {
    var pos = 0.0
}

/** Where a score hands finished decisions to the mixer. Bus 0 is music, bus 1 is fixed-level effects. */
internal class Emitter(private val queue: ConcurrentLinkedQueue<Play>) {
    fun play(buf: FloatArray, start: Long, gain: Float, pan: Float = 0f, rate: Float = 1f, bus: Int = 0) {
        val ang = (pan.coerceIn(-1f, 1f) + 1f) * 0.7853982f
        queue.add(Play(buf, start, gain, cos(ang), sin(ang), rate, bus))
    }
}

internal class RoomSpec(val size: Float, val feedback: Float, val wet: Float, val warmth: Float)

internal abstract class Score {
    /** Emit everything that should start in [from, to) (absolute mixer samples). */
    abstract fun generate(from: Long, to: Long, e: Emitter)

    open val room: RoomSpec = RoomSpec(1f, 0.85f, 0.35f, 0.7f)

    /** One line describing the music, for the menu. */
    open val info: String = ""

    /** The user's music volume, or the floor the scripted worlds apply to it. */
    open fun volume(): Float = GameSettings.musicVolume

    /** How far ahead of the mixer the score may compose (samples). */
    open val lookahead: Int = AR * 2
}

// ------------------------------------------------------------------ the note bank

internal object K {
    const val PIANO = 1
    const val MUTED = 2
    const val EPIANO = 3
    const val MARIMBA = 4
    const val KALIMBA = 5
    const val VIBES = 6
    const val CELESTA = 7
    const val BELL = 8
    const val MUSICBOX = 9
    const val GUITAR = 10
    const val HARP = 11
    const val CHIP = 12
    const val SAWPLUCK = 13
    const val BASSE = 14
    const val BASSS = 15
    const val WARMPAD = 16
    const val AIRYPAD = 17
    const val DRONE = 18
    const val REVPIANO = 19
    const val SHIMMER = 20
    const val SPACE = 21
    const val WIND = 30
    const val DROP = 31
    const val CRACKLE = 32
    const val TICK = 33
    const val KICK = 40
    const val SNARE = 41
    const val HAT = 42
    const val SHAKER = 43
    const val RIM = 44
    const val WOOD = 45
    const val BOOP = 46
    const val PLINK = 47

    // scripted effects (Void)
    const val HEART = 100
    const val RUMBLE = 101
    const val BOOM = 102
    const val SWEEP = 103
    const val RISER = 104
    const val RADIO = 105
    const val GLITCH = 106
    const val SHIMMERFX = 107
    const val DEBRIS = 108
    const val COLLAPSE = 109
    const val ABYSS = 110
    const val STATIC = 111
    const val GLASS = 112
    const val BEEP = 113
}

internal object NoteBank {
    private val cache = HashMap<Int, FloatArray>()
    private var bytes = 0L

    fun get(kind: Int, midi: Int = 0): FloatArray {
        val key = kind * 1000 + midi
        synchronized(cache) {
            cache[key]?.let { return it }
        }
        val buf = render(kind, midi)
        synchronized(cache) {
            if (bytes > 48L * 1024 * 1024) {
                cache.clear()
                bytes = 0
            }
            cache[key] = buf
            bytes += buf.size * 4L
        }
        return buf
    }

    private class Spec(
        val ratios: FloatArray,
        val amps: FloatArray,
        val decays: FloatArray,
        val tau: Float,
        val attack: Float,
        val fm: Float = 0f,
        val trem: Float = 0f,
    )

    private fun f(vararg v: Float) = v

    private val SPECS =
        mapOf(
            K.PIANO to Spec(f(1f, 2f, 3f, 4f), f(1f, .42f, .18f, .07f), f(1f, 1.6f, 2.4f, 3.5f), 2.6f, 0.012f),
            K.MUTED to Spec(f(1f, 2f, 3f), f(1f, .3f, .08f), f(1.6f, 2.8f, 4f), 1.0f, 0.008f),
            K.EPIANO to Spec(f(1f, 2f), f(1f, .18f), f(1f, 2.2f), 2.2f, 0.01f, fm = 1.1f),
            K.MARIMBA to Spec(f(1f, 4f), f(1f, .3f), f(1f, 5f), 0.9f, 0.004f),
            K.KALIMBA to Spec(f(1f, 5.4f), f(1f, .28f), f(1f, 4.5f), 1.3f, 0.003f),
            K.VIBES to Spec(f(1f, 4f, 7.5f), f(1f, .2f, .06f), f(1f, 3f, 6f), 3.2f, 0.006f, trem = 4.6f),
            K.CELESTA to Spec(f(1f, 2f, 5f), f(1f, .4f, .12f), f(1f, 2f, 4f), 2.0f, 0.004f),
            K.BELL to Spec(f(1f, 2.76f, 5.4f), f(1f, .5f, .22f), f(1f, 1.8f, 2.8f), 3.4f, 0.004f),
            K.MUSICBOX to Spec(f(1f, 3f, 6f), f(1f, .3f, .1f), f(1f, 2f, 3.5f), 1.5f, 0.002f),
            K.PLINK to Spec(f(1f, 2f), f(1f, .25f), f(1f, 3f), 0.5f, 0.002f),
        )

    private fun finish(b: FloatArray, peak: Float = 0.8f): FloatArray {
        var m = 0f
        for (v in b) m = max(m, abs(v))
        if (m > 0f) {
            val k = peak / m
            for (i in b.indices) b[i] *= k
        }
        val tail = min(400, b.size)
        for (i in 0 until tail) b[b.size - 1 - i] *= i / tail.toFloat()
        return b
    }

    private fun partials(freq: Float, s: Spec, dur: Float): FloatArray {
        val n = (dur * AR).toInt()
        val out = FloatArray(n)
        for (p in s.ratios.indices) {
            val inc = freq * s.ratios[p] / AR
            if (inc > 0.46f) continue
            var ph = 0f
            var env = 1f
            val mul = exp(-s.decays[p] / (s.tau * AR))
            for (i in 0 until n) {
                ph += inc
                if (ph > 1f) ph -= 1f
                val w = if (s.fm > 0f && p == 0) sinT(ph + s.fm * env * sinT(ph)) else sinT(ph)
                out[i] += w * s.amps[p] * env
                env *= mul
            }
        }
        val att = max(1, (s.attack * AR).toInt())
        for (i in 0 until min(att, n)) out[i] *= i / att.toFloat()
        if (s.trem > 0f) {
            for (i in 0 until n) out[i] *= 0.8f + 0.2f * sinT(i * s.trem / AR)
        }
        return out
    }

    private fun karplus(freq: Float, dur: Float, soft: Float, seed: Int): FloatArray {
        val n = (dur * AR).toInt()
        val out = FloatArray(n)
        val period = (AR / freq).toInt().coerceAtLeast(2)
        val line = FloatArray(period)
        val r = Random(seed)
        var prev = 0f
        for (i in 0 until period) {
            val x = r.nextFloat() * 2f - 1f
            prev += soft * (x - prev)
            line[i] = prev
        }
        val k = exp(ln(0.002f) / (dur * freq)).coerceAtMost(0.9995f)
        var idx = 0
        for (i in 0 until n) {
            val cur = line[idx]
            val nxt = line[(idx + 1) % period]
            line[idx] = (cur + nxt) * 0.5f * k
            out[i] = cur
            idx = (idx + 1) % period
        }
        return out
    }

    private fun noise(seed: Int, n: Int): FloatArray {
        val r = Random(seed)
        return FloatArray(n) { r.nextFloat() * 2f - 1f }
    }

    private fun render(kind: Int, midi: Int): FloatArray {
        val freq = midiHz(midi.toFloat())
        SPECS[kind]?.let { return finish(partials(freq, it, min(it.tau * 4.5f, 7f))) }
        return when (kind) {
            K.GUITAR -> finish(karplus(freq, 3.2f, 0.45f, midi))
            K.HARP -> finish(karplus(freq, 4.5f, 0.7f, midi + 7))
            K.CHIP -> {
                val n = (0.7f * AR).toInt()
                val out = FloatArray(n)
                var ph = 0f
                var lp = 0f
                for (i in 0 until n) {
                    ph += freq / AR
                    if (ph > 1f) ph -= 1f
                    val s = if (ph < 0.25f) 1f else -1f
                    lp += 0.6f * (s - lp)
                    out[i] = lp * exp(-i / (0.22f * AR))
                }
                finish(out, 0.55f)
            }
            K.SAWPLUCK -> {
                val n = (1.4f * AR).toInt()
                val out = FloatArray(n)
                for (h in 1..14) {
                    val inc = freq * h / AR
                    if (inc > 0.46f) break
                    var ph = 0f
                    val mul = exp(-(1.4f + h * 0.9f) / AR)
                    var env = 1f
                    for (i in 0 until n) {
                        ph += inc
                        if (ph > 1f) ph -= 1f
                        out[i] += sinT(ph) / h * env
                        env *= mul
                    }
                }
                for (i in 0 until min(60, n)) out[i] *= i / 60f
                finish(out)
            }
            K.BASSE -> {
                val n = (1.8f * AR).toInt()
                val out = FloatArray(n)
                var ph = 0f
                for (i in 0 until n) {
                    ph += freq / AR
                    if (ph > 1f) ph -= 1f
                    val env = exp(-i / (0.7f * AR))
                    out[i] = (sinT(ph) + 0.3f * sinT(ph * 2f)) * env * min(1f, i / 200f)
                }
                finish(out)
            }
            K.BASSS -> {
                val n = (1.6f * AR).toInt()
                val out = FloatArray(n)
                for (h in 1..8) {
                    val inc = freq * h / AR
                    var ph = 0f
                    for (i in 0 until n) {
                        ph += inc
                        if (ph > 1f) ph -= 1f
                        val cut = exp(-i / (0.35f * AR))
                        out[i] += sinT(ph) / h * (if (h == 1) 1f else cut) * exp(-i / (0.8f * AR))
                    }
                }
                for (i in 0 until min(100, n)) out[i] *= i / 100f
                finish(out)
            }
            K.WARMPAD, K.AIRYPAD -> {
                val ratios = if (kind == K.WARMPAD) f(1f, 1.0045f, 2f, 2.006f, 3f) else f(1f, 1.003f, 2f, 3f, 4f)
                val amps = if (kind == K.WARMPAD) f(.6f, .6f, .22f, .22f, .08f) else f(.5f, .5f, .28f, .18f, .1f)
                val dur = 10f
                val n = (dur * AR).toInt()
                val out = FloatArray(n)
                for (p in ratios.indices) {
                    val inc = freq * ratios[p] / AR
                    if (inc > 0.46f) continue
                    var ph = 0f
                    for (i in 0 until n) {
                        ph += inc
                        if (ph > 1f) ph -= 1f
                        out[i] += sinT(ph) * amps[p]
                    }
                }
                val att = (3.2f * AR).toInt()
                val rel = (4f * AR).toInt()
                for (i in 0 until n) {
                    var e = 0.82f + 0.18f * sinT(i * 0.17f / AR)
                    if (i < att) e *= i / att.toFloat()
                    if (i > n - rel) e *= (n - i) / rel.toFloat()
                    out[i] *= e
                }
                finish(out, 0.7f)
            }
            K.DRONE -> {
                val n = (14f * AR).toInt()
                val out = FloatArray(n)
                var p1 = 0f
                var p2 = 0f
                for (i in 0 until n) {
                    p1 += freq / AR
                    p2 += freq * 1.5f / AR
                    if (p1 > 1f) p1 -= 1f
                    if (p2 > 1f) p2 -= 1f
                    val e = min(1f, i / (5f * AR)) * min(1f, (n - i) / (6f * AR))
                    out[i] = (sinT(p1) + 0.25f * sinT(p2)) * e
                }
                finish(out, 0.7f)
            }
            K.REVPIANO -> {
                val src = partials(freq, SPECS[K.PIANO]!!, 3f)
                src.reverse()
                val n = src.size
                for (i in 0 until min(2000, n)) src[i] *= i / 2000f
                for (i in 0 until min(2500, n)) src[n - 1 - i] *= i / 2500f
                finish(src, 0.7f)
            }
            K.SHIMMER -> {
                val n = (5f * AR).toInt()
                val out = FloatArray(n)
                val r = Random(midi * 31 + 7)
                val mults = f(1f, 2f, 3f, 1.5f, 4f, 2.5f)
                for (g in 0 until 46) {
                    val t0 = (r.nextFloat() * 3.6f * AR).toInt()
                    val len = ((0.25f + r.nextFloat() * 0.35f) * AR).toInt()
                    val gf = freq * mults[r.nextInt(mults.size)]
                    if (gf > 9000f) continue
                    var ph = 0f
                    for (i in 0 until len) {
                        val idx = t0 + i
                        if (idx >= n) break
                        ph += gf / AR
                        if (ph > 1f) ph -= 1f
                        val w = 0.5f - 0.5f * cos(2f * PI.toFloat() * i / len)
                        out[idx] += sinT(ph) * w * 0.15f
                    }
                }
                finish(out, 0.5f)
            }
            K.SPACE -> {
                val n = (6f * AR).toInt()
                val out = FloatArray(n)
                var p1 = 0f
                var p2 = 0f
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    p1 += freq * (1f + 0.012f * sinT(t * 0.31f)) / AR
                    p2 += freq * 2.003f / AR
                    if (p1 > 1f) p1 -= 1f
                    if (p2 > 1f) p2 -= 1f
                    val w = sinT(0.5f * i / n.toFloat()).let { it * it }
                    out[i] = (sinT(p1) * 0.6f + sinT(p2) * 0.25f) * w * (0.85f + 0.15f * sinT(t * 3.1f))
                }
                finish(out, 0.6f)
            }
            // ---- organic
            K.WIND -> {
                val n = (7f * AR).toInt()
                val src = noise(11 + midi, n)
                val out = FloatArray(n)
                var l1 = 0f
                var l2 = 0f
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    val a = 0.012f + 0.03f * (0.5f + 0.5f * sinT(t * 0.2f))
                    l1 += a * (src[i] - l1)
                    l2 += a * (l1 - l2)
                    out[i] = l2 * sinT(0.5f * i / n.toFloat()).let { it * it }
                }
                finish(out, 0.5f)
            }
            K.DROP -> {
                val n = (0.55f * AR).toInt()
                val out = FloatArray(n)
                var ph = 0f
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    val fq = freq * (1f + 0.7f * (1f - exp(-t * 45f)))
                    ph += fq / AR
                    if (ph > 1f) ph -= 1f
                    var s = sinT(ph) * exp(-t * 16f)
                    val t2 = t - 0.075f
                    if (t2 > 0f) s += sinT(ph * 0.5f) * exp(-t2 * 22f) * 0.35f
                    out[i] = s * min(1f, i / 40f)
                }
                finish(out, 0.55f)
            }
            K.CRACKLE -> {
                val n = (5f * AR).toInt()
                val r = Random(99 + midi)
                val out = FloatArray(n)
                var y = 0f
                var lp = 0f
                for (i in 0 until n) {
                    val imp = if (r.nextFloat() < 0.0005f) (0.2f + r.nextFloat() * 0.8f) * (if (r.nextBoolean()) 1f else -1f) else 0f
                    y = y * 0.55f + imp
                    lp += 0.25f * (y - lp)
                    out[i] = y - lp
                }
                finish(out, 0.4f)
            }
            K.TICK -> {
                val n = (0.07f * AR).toInt()
                val out = FloatArray(n) { i -> sinT(i * (1500f + midi * 10f) / AR) * exp(-i / (0.012f * AR)) }
                finish(out, 0.5f)
            }
            // ---- percussion
            K.KICK -> {
                val n = (0.38f * AR).toInt()
                val out = FloatArray(n)
                var ph = 0f
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    ph += (48f + 85f * exp(-t * 28f)) / AR
                    if (ph > 1f) ph -= 1f
                    out[i] = sinT(ph) * exp(-t * 9f) * min(1f, i / 30f)
                }
                finish(out, 0.9f)
            }
            K.SNARE -> {
                val n = (0.28f * AR).toInt()
                val nz = noise(5, n)
                var lp = 0f
                val out = FloatArray(n)
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    lp += 0.5f * (nz[i] - lp)
                    out[i] = (nz[i] - lp) * exp(-t * 20f) * 0.6f + sinT(i * 190f / AR) * exp(-t * 26f) * 0.5f
                }
                finish(out, 0.6f)
            }
            K.HAT -> {
                val n = (0.09f * AR).toInt()
                val nz = noise(6, n)
                var lp = 0f
                val out = FloatArray(n)
                for (i in 0 until n) {
                    lp += 0.7f * (nz[i] - lp)
                    out[i] = (nz[i] - lp) * exp(-i / (0.02f * AR))
                }
                finish(out, 0.4f)
            }
            K.SHAKER -> {
                val n = (0.14f * AR).toInt()
                val nz = noise(7, n)
                var lp = 0f
                val out = FloatArray(n)
                for (i in 0 until n) {
                    lp += 0.55f * (nz[i] - lp)
                    out[i] = (nz[i] - lp) * min(1f, i / (0.012f * AR)) * exp(-i / (0.035f * AR))
                }
                finish(out, 0.35f)
            }
            K.RIM -> {
                val n = (0.07f * AR).toInt()
                finish(FloatArray(n) { i -> (sinT(i * 1700f / AR) + 0.4f * sinT(i * 620f / AR)) * exp(-i / (0.011f * AR)) }, 0.5f)
            }
            K.WOOD -> {
                val fq = 700f + midi * 12f
                val n = (0.1f * AR).toInt()
                finish(FloatArray(n) { i -> (sinT(i * fq / AR) + 0.4f * sinT(i * fq * 2f / AR)) * exp(-i / (0.016f * AR)) }, 0.5f)
            }
            K.BOOP -> {
                val n = (0.22f * AR).toInt()
                val out = FloatArray(n)
                var ph = 0f
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    ph += (200f + 120f * exp(-t * 14f)) / AR
                    if (ph > 1f) ph -= 1f
                    out[i] = sinT(ph) * exp(-t * 11f)
                }
                finish(out, 0.5f)
            }
            else -> renderFx(kind)
        }
    }

    // ------------------------------------------------------------------ scripted effects

    private fun renderFx(kind: Int): FloatArray {
        when (kind) {
            K.HEART -> {
                val n = (0.7f * AR).toInt()
                val out = FloatArray(n)
                for (b in 0..1) {
                    val t0 = (b * 0.2f * AR).toInt()
                    var ph = 0f
                    for (i in 0 until (0.25f * AR).toInt()) {
                        val idx = t0 + i
                        if (idx >= n) break
                        val t = i / AR.toFloat()
                        ph += (58f - 18f * t * 4f).coerceAtLeast(34f) / AR
                        if (ph > 1f) ph -= 1f
                        out[idx] += sinT(ph) * exp(-t * 14f) * (if (b == 0) 1f else 0.7f) * min(1f, i / 40f)
                    }
                }
                return finish(out, 0.9f)
            }
            K.RUMBLE -> {
                val n = (6f * AR).toInt()
                val nz = noise(21, n)
                val out = FloatArray(n)
                var l1 = 0f
                var l2 = 0f
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    l1 += 0.006f * (nz[i] - l1)
                    l2 += 0.006f * (l1 - l2)
                    val e = min(1f, t / 0.8f) * min(1f, (6f - t) / 0.8f)
                    out[i] = (l2 * 14f + sinT(i * 38f / AR) * 0.6f + sinT(i * 55f / AR) * 0.35f * (0.7f + 0.3f * sinT(t * 5f))) * e
                }
                return finish(out, 0.8f)
            }
            K.BOOM -> {
                val n = (5.5f * AR).toInt()
                val nz = noise(22, n)
                val out = FloatArray(n)
                var ph = 0f
                var lp = 0f
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    ph += (24f + 60f * exp(-t * 2.2f)) / AR
                    if (ph > 1f) ph -= 1f
                    lp += 0.08f * (nz[i] - lp)
                    val s = sinT(ph) * exp(-t * 0.9f) + lp * 3f * exp(-t * 2.4f)
                    out[i] = tanh(s * 2.2f) * min(1f, i / 24f)
                }
                return finish(out, 0.95f)
            }
            K.SWEEP -> {
                val n = (5f * AR).toInt()
                val nz = noise(23, n)
                val out = FloatArray(n)
                var l1 = 0f
                var l2 = 0f
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    val a = (0.45f * exp(-t * 1.1f)).coerceAtLeast(0.006f)
                    l1 += a * (nz[i] - l1)
                    l2 += a * (l1 - l2)
                    out[i] = l2 * exp(-t * 0.5f) * min(1f, i / (0.15f * AR))
                }
                return finish(out, 0.8f)
            }
            K.RISER -> {
                val n = (6f * AR).toInt()
                val nz = noise(24, n)
                val out = FloatArray(n)
                var ph = 0f
                var l1 = 0f
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    val u = t / 6f
                    ph += (80f + 900f * u * u) / AR
                    if (ph > 1f) ph -= 1f
                    l1 += (0.02f + 0.3f * u) * (nz[i] - l1)
                    out[i] = (sinT(ph) * 0.6f + l1 * 0.8f) * u * u
                }
                return finish(out, 0.8f)
            }
            K.RADIO -> {
                val n = (1.0f * AR).toInt()
                val nz = noise(25, n)
                val out = FloatArray(n)
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    var s = nz[i] * 0.08f * (if (t < 0.12f || t > 0.62f) 1f else 0.25f)
                    for (b in 0..1) {
                        val tb = t - 0.12f - b * 0.2f
                        if (tb in 0f..0.11f) {
                            val sq = if (sinT(tb * 1250f) > 0f) 1f else -1f
                            s += sq * 0.25f
                        }
                    }
                    out[i] = s
                }
                var lp = 0f
                for (i in 0 until n) {
                    lp += 0.45f * (out[i] - lp)
                    out[i] = lp
                }
                return finish(out, 0.6f)
            }
            K.GLITCH -> {
                val n = (0.1f * AR).toInt()
                val r = Random(26)
                val out = FloatArray(n)
                var held = 0f
                for (i in 0 until n) {
                    if (i % 24 == 0) held = r.nextFloat() * 2f - 1f
                    out[i] = held * exp(-i / (0.03f * AR))
                }
                return finish(out, 0.5f)
            }
            K.GLASS, K.SHIMMERFX -> {
                val n = (4f * AR).toInt()
                val out = FloatArray(n)
                val r = Random(27)
                for (g in 0 until 70) {
                    val t0 = (r.nextFloat() * 3f * AR).toInt()
                    val len = ((0.12f + r.nextFloat() * 0.3f) * AR).toInt()
                    val gf = 900f + r.nextFloat() * 3200f
                    var ph = 0f
                    for (i in 0 until len) {
                        val idx = t0 + i
                        if (idx >= n) break
                        ph += gf / AR
                        if (ph > 1f) ph -= 1f
                        out[idx] += sinT(ph) * exp(-i / (0.09f * AR)) * 0.12f
                    }
                }
                return finish(out, 0.5f)
            }
            K.DEBRIS -> {
                val n = (1.4f * AR).toInt()
                val r = Random(28)
                val out = FloatArray(n)
                var y = 0f
                for (i in 0 until n) {
                    val imp = if (r.nextFloat() < 0.004f * exp(-i / (0.5f * AR))) (r.nextFloat() * 2f - 1f) else 0f
                    y = y * 0.7f + imp
                    out[i] = y
                }
                return finish(out, 0.6f)
            }
            K.COLLAPSE -> {
                val n = (6.5f * AR).toInt()
                val nz = noise(29, n)
                val out = FloatArray(n)
                var ph = 0f
                var lp = 0f
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    ph += (150f * exp(-t * 0.55f) + 26f) / AR
                    if (ph > 1f) ph -= 1f
                    lp += 0.02f * (nz[i] - lp)
                    out[i] = (sinT(ph) * 0.8f + lp * 4f * exp(-t * 0.6f)) * min(1f, t / 0.3f) * min(1f, (6.5f - t) / 2f)
                }
                return finish(out, 0.8f)
            }
            K.ABYSS -> {
                val n = (12f * AR).toInt()
                val out = FloatArray(n)
                for (i in 0 until n) {
                    val t = i / AR.toFloat()
                    val e = min(1f, t / 3f) * min(1f, (12f - t) / 4f)
                    val s = sinT(i * 41f / AR) * 0.7f + sinT(i * 41.3f / AR) * 0.5f + sinT(i * 61.5f / AR) * 0.25f +
                        sinT(i * 2700f / AR) * 0.03f * (0.5f + 0.5f * sinT(t * 0.37f))
                    out[i] = s * e
                }
                return finish(out, 0.75f)
            }
            K.BEEP -> {
                val n = (0.16f * AR).toInt()
                val out = FloatArray(n) { i -> sinT(i * 1100f / AR) * min(1f, i / 120f) * exp(-i / (0.07f * AR)) }
                return finish(out, 0.5f)
            }
            K.STATIC -> {
                val n = (2f * AR).toInt()
                val nz = noise(30, n)
                var lp = 0f
                val out = FloatArray(n)
                for (i in 0 until n) {
                    lp += 0.3f * (nz[i] - lp)
                    out[i] = lp * min(1f, i / (0.2f * AR)) * min(1f, (n - i) / (0.4f * AR))
                }
                return finish(out, 0.3f)
            }
        }
        return FloatArray(64)
    }
}

// ------------------------------------------------------------------ the engine

internal object MusicEngine {
    private var mixer: MixerThread? = null
    private var composer: ComposerThread? = null
    private val queue = ConcurrentLinkedQueue<Play>()

    @Volatile var mixPos = 0L
    @Volatile var flushEpoch = 0
    @Volatile var duck = 1f

    fun start(score: Score) {
        stop()
        queue.clear()
        mixPos = 0L
        val m = MixerThread(score, queue)
        val c = ComposerThread(score, queue)
        mixer = m
        composer = c
        m.start()
        c.start()
    }

    fun stop() {
        mixer?.stopFlag = true
        composer?.stopFlag = true
        mixer = null
        composer = null
    }

    /** Drops everything that is queued or still sounding on the music bus (for scripted moments). */
    fun flush() {
        flushEpoch++
    }

    /** Plays something now, from any thread (the Void's effects). */
    fun playNow(kind: Int, midi: Int, gain: Float, pan: Float = 0f, bus: Int = 1) {
        if (mixer == null) return
        val buf = NoteBank.get(kind, midi)
        Emitter(queue).play(buf, mixPos + 300L, gain, pan, 1f, bus)
    }

    private class ComposerThread(private val score: Score, private val queue: ConcurrentLinkedQueue<Play>) : Thread("landroid-composer") {
        @Volatile var stopFlag = false

        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            val em = Emitter(queue)
            var cursor = AR / 3L
            var epoch = flushEpoch
            try {
                while (!stopFlag) {
                    if (epoch != flushEpoch) {
                        epoch = flushEpoch
                        cursor = mixPos + AR / 5L
                    }
                    if (cursor >= mixPos + score.lookahead) {
                        sleep(30)
                        continue
                    }
                    val to = cursor + AR / 4L
                    score.generate(cursor, to, em)
                    cursor = to
                }
            } catch (e: Throwable) {
                // the music just stops
            }
        }
    }

    private class Reverb(size: Float, private val fb: Float) {
        private val lenL = intArrayOf(1116, 1188, 1277, 1356).map { (it * size).toInt().coerceAtLeast(64) }
        private val lenR = intArrayOf(1139, 1211, 1300, 1379).map { (it * size).toInt().coerceAtLeast(64) }
        private val bufL = Array(4) { FloatArray(lenL[it]) }
        private val bufR = Array(4) { FloatArray(lenR[it]) }
        private val idxL = IntArray(4)
        private val idxR = IntArray(4)
        private val lpL = FloatArray(4)
        private val lpR = FloatArray(4)
        private val apL = arrayOf(FloatArray(225), FloatArray(556))
        private val apR = arrayOf(FloatArray(241), FloatArray(587))
        private val aiL = IntArray(2)
        private val aiR = IntArray(2)
        var outL = 0f
        var outR = 0f

        private fun ap(buf: FloatArray, idx: IntArray, k: Int, x: Float): Float {
            val bo = buf[idx[k]]
            buf[idx[k]] = x + bo * 0.5f
            idx[k] = (idx[k] + 1) % buf.size
            return bo - x * 0.5f
        }

        fun process(input: Float) {
            var l = 0f
            var r = 0f
            for (c in 0 until 4) {
                val yl = bufL[c][idxL[c]]
                lpL[c] += 0.38f * (yl - lpL[c])
                bufL[c][idxL[c]] = input + lpL[c] * fb
                idxL[c] = (idxL[c] + 1) % bufL[c].size
                l += yl
                val yr = bufR[c][idxR[c]]
                lpR[c] += 0.38f * (yr - lpR[c])
                bufR[c][idxR[c]] = input + lpR[c] * fb
                idxR[c] = (idxR[c] + 1) % bufR[c].size
                r += yr
            }
            l *= 0.25f
            r *= 0.25f
            for (a in 0 until 2) {
                l = ap(apL[a], aiL, a, l)
                r = ap(apR[a], aiR, a, r)
            }
            outL = l
            outR = r
        }
    }

    private class MixerThread(private val score: Score, private val queue: ConcurrentLinkedQueue<Play>) : Thread("landroid-mixer") {
        @Volatile var stopFlag = false

        private fun buildTrack(): AudioTrack? =
            try {
                val minBuf = AudioTrack.getMinBufferSize(AR, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
                AudioTrack.Builder()
                    .setAudioAttributes(attrs(true))
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(AR)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(max(minBuf, AR * 4)) // about one second
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (e: Throwable) {
                null
            }

        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val room = score.room
            val reverb = Reverb(room.size, room.feedback)
            val chunk = 1024
            val out = ShortArray(chunk * 2)
            val waiting = ArrayList<Play>()
            val active = ArrayList<Play>()
            var track: AudioTrack? = null
            var epoch = flushEpoch
            var cap = 36
            var slow = 0
            var fast = 0
            var gain = 0f
            var lpL = 0f
            var lpR = 0f
            var duckNow = 1f
            val chunkNanos = chunk * 1_000_000_000L / AR

            try {
                while (!stopFlag) {
                    if (Sound.paused) {
                        track?.let {
                            try {
                                it.pause()
                            } catch (e: Throwable) {
                                // ignore
                            }
                        }
                        sleep(100)
                        continue
                    }
                    if (track == null) {
                        track = buildTrack()
                        if (track == null) {
                            sleep(500)
                            continue
                        }
                    }
                    val t = track
                    if (t != null && t.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        try {
                            t.play()
                        } catch (e: Throwable) {
                            try {
                                t.release()
                            } catch (e2: Throwable) {
                                // ignore
                            }
                            track = null
                            continue
                        }
                    }
                    val began = System.nanoTime()
                    if (epoch != flushEpoch) {
                        epoch = flushEpoch
                        waiting.removeAll { it.bus == 0 }
                        active.removeAll { it.bus == 0 }
                        queue.removeAll { it.bus == 0 }
                    }
                    while (true) {
                        val p = queue.poll() ?: break
                        waiting.add(p)
                    }
                    val chunkStart = mixPos
                    val chunkEnd = chunkStart + chunk
                    var w = 0
                    while (w < waiting.size) {
                        val p = waiting[w]
                        if (p.start < chunkEnd) {
                            active.add(p)
                            waiting.removeAt(w)
                        } else {
                            w++
                        }
                    }
                    while (active.size > cap) active.removeAt(0)

                    val vol = score.volume().coerceIn(0f, 1f) * 0.85f
                    val wet = room.wet
                    val warmth = room.warmth
                    val duckTarget = duck
                    for (i in 0 until chunk) {
                        val now = chunkStart + i
                        var mL = 0f
                        var mR = 0f
                        var sL = 0f
                        var sR = 0f
                        var a = 0
                        while (a < active.size) {
                            val p = active[a]
                            if (now < p.start) {
                                a++
                                continue
                            }
                            val ix = p.pos.toInt()
                            if (ix + 1 >= p.buf.size) {
                                active.removeAt(a)
                                continue
                            }
                            val fr = (p.pos - ix).toFloat()
                            val s = (p.buf[ix] + (p.buf[ix + 1] - p.buf[ix]) * fr) * p.gain
                            if (p.bus == 0) {
                                mL += s * p.panL
                                mR += s * p.panR
                            } else {
                                sL += s * p.panL
                                sR += s * p.panR
                            }
                            p.pos += p.rate
                            a++
                        }
                        duckNow += (duckTarget - duckNow) * 0.0005f
                        mL *= duckNow
                        mR *= duckNow
                        reverb.process((mL + mR) * 0.5f * 0.25f + (sL + sR) * 0.5f * 0.08f)
                        gain = min(1f, gain + 1f / (AR * 3f))
                        var l = tanh((mL * 0.22f * vol * 1.7f + sL * 0.45f + reverb.outL * wet * vol * 1.5f) * gain * 1.4f)
                        var r = tanh((mR * 0.22f * vol * 1.7f + sR * 0.45f + reverb.outR * wet * vol * 1.5f) * gain * 1.4f)
                        lpL += warmth * (l - lpL)
                        lpR += warmth * (r - lpR)
                        l = lpL
                        r = lpR
                        out[i * 2] = (l * 30000f).toInt().toShort()
                        out[i * 2 + 1] = (r * 30000f).toInt().toShort()
                    }
                    mixPos = chunkEnd

                    // measure ourselves: too slow means fewer simultaneous notes
                    val took = System.nanoTime() - began
                    if (took > chunkNanos * 6 / 10) {
                        slow++
                        fast = 0
                        if (slow >= 4) {
                            cap = max(10, cap - 6)
                            slow = 0
                        }
                    } else {
                        slow = 0
                        fast++
                        if (fast >= 200) {
                            cap = min(40, cap + 2)
                            fast = 0
                        }
                    }

                    val written = track?.write(out, 0, out.size) ?: -1
                    if (written < 0) {
                        try {
                            track?.release()
                        } catch (e: Throwable) {
                            // ignore
                        }
                        track = null
                    }
                }
            } catch (e: Throwable) {
                // the music just stops
            } finally {
                try {
                    track?.setVolume(0f)
                    track?.stop()
                    track?.release()
                } catch (e: Throwable) {
                    // ignore
                }
            }
        }
    }
}
