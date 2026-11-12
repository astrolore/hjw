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

import kotlin.math.max
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// The two hand-scripted scores: the Ultimate Void (which follows the cutscene) and Heaven.
// ---------------------------------------------------------------------------------------------

/** The phases of the Ultimate Void, in order (the score and the Director both use them). */
internal object VoidPhase {
    const val ARRIVAL = 0
    const val EXPLORE = 1
    const val CUT1 = 2
    const val PULSE = 3
    const val COLLAPSE = 4
    const val BLAST = 5
    const val VANISH = 6
    const val DUST = 7
    const val GATHER = 8
    const val TRANSMIT = 9
    const val MESSAGE = 10
    const val MISSION = 11
    const val JOURNEY = 12
    const val INSPECT = 13
    const val COLLECT = 14
    const val FAILURE = 15
    const val POEM = 16
    const val SILENCE = 17
    const val REVEAL = 18
    const val CONTROL = 19
    const val AFTER = 20
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

/**
 * The Ultimate Void's score follows the sequence: calm and tender at first, then thinner and darker
 * as the star fails, silent at the moment of collapse, a quiet bed under the poem, and almost
 * nothing at the very end. All of it is slow, soft ambient: piano, bells, a warm pad, deep drones.
 */
internal class VoidScore : Score() {
    override val room = RoomSpec(1.6f, 0.88f, 0.5f, 0.6f)
    override val info = "the ultimate void · scripted"
    override val lookahead = AR

    // the Void is meant to be immersive: the music is never quieter than half
    override fun volume(): Float = max(0.5f, GameSettings.musicVolume)

    private val r = Random(System.nanoTime())
    private var started = false
    private var tNote = 0L
    private var tPad = 0L
    private var tDark = 0L
    private var tMisc = 0L
    private var padFlip = 0

    private fun sec(s: Float) = (s * AR).toLong()

    private fun put(e: Emitter, kind: Int, midi: Int, at: Long, gain: Float, pan: Float = 0f, rate: Float = 1f) {
        e.play(NoteBank.get(kind, midi), at, gain, pan, rate, 0)
    }

    override fun generate(from: Long, to: Long, e: Emitter) {
        if (!started) {
            started = true
            tNote = from + sec(2f)
            tPad = from
            tDark = from
            tMisc = from + sec(4f)
        }
        val phase = Director.audioPhase
        val p = Director.audioProgress.coerceIn(0f, 1f)
        val pent = intArrayOf(72, 74, 76, 79, 81, 84, 79, 76)
        val chordsC = arrayOf(intArrayOf(48, 55, 60, 64), intArrayOf(45, 52, 57, 64), intArrayOf(41, 48, 57, 60), intArrayOf(43, 50, 55, 59))

        when (phase) {
            VoidPhase.ARRIVAL, VoidPhase.EXPLORE -> {
                // peaceful: a few soft piano notes and a warm pad
                while (tNote < to) {
                    if (r.nextFloat() < 0.55f) put(e, K.PIANO, pent[r.nextInt(pent.size)] - 12, tNote, 0.55f, r.nextFloat() - 0.5f)
                    tNote += sec(2.5f + r.nextFloat() * 3.5f)
                }
                while (tPad < to) {
                    chordsC[padFlip % chordsC.size].forEach { put(e, K.WARMPAD, it, tPad, 0.2f, r.nextFloat() - 0.5f) }
                    padFlip++
                    tPad += sec(10f)
                }
            }
            VoidPhase.CUT1 -> {
                // the notes thin out and turn uneasy
                while (tNote < to) {
                    if (r.nextFloat() < lerpF(0.5f, 0.15f, p)) put(e, K.BELL, 60 + intArrayOf(0, 1, 6, 7)[r.nextInt(4)], tNote, 0.35f, r.nextFloat() - 0.5f, 1f - 0.04f * p)
                    tNote += sec(3f + r.nextFloat() * 3f)
                }
                while (tDark < to) {
                    put(e, K.ABYSS, 0, tDark, 0.3f + 0.3f * p)
                    tDark += sec(11f)
                }
            }
            VoidPhase.PULSE -> {
                while (tDark < to) {
                    put(e, K.ABYSS, 0, tDark, 0.6f)
                    tDark += sec(10f)
                }
                while (tMisc < to) {
                    put(e, K.HEART, 0, tMisc, 0.5f + 0.4f * p)
                    tMisc += sec(lerpF(1.0f, 0.4f, p))
                }
            }
            VoidPhase.COLLAPSE, VoidPhase.BLAST -> {
                // the silence before and after the explosion; only a low shadow remains
                while (tDark < to) {
                    if (phase == VoidPhase.BLAST && p > 0.3f) put(e, K.ABYSS, 0, tDark, 0.35f)
                    tDark += sec(11f)
                }
            }
            VoidPhase.VANISH, VoidPhase.DUST, VoidPhase.GATHER -> {
                while (tDark < to) {
                    put(e, K.ABYSS, 0, tDark, 0.3f)
                    tDark += sec(11f)
                }
                while (tMisc < to) {
                    // the dust: tiny glassy sparkles
                    if (phase != VoidPhase.VANISH) put(e, K.SHIMMER, 72 + r.nextInt(8), tMisc, 0.28f, r.nextFloat() * 2f - 1f)
                    tMisc += sec(9f)
                }
            }
            VoidPhase.TRANSMIT, VoidPhase.MESSAGE, VoidPhase.MISSION, VoidPhase.JOURNEY, VoidPhase.INSPECT -> {
                while (tPad < to) {
                    intArrayOf(36, 43, 50, 53).forEach { put(e, K.AIRYPAD, it, tPad, 0.17f, r.nextFloat() - 0.5f) }
                    tPad += sec(8f)
                }
                while (tNote < to) {
                    if (r.nextFloat() < 0.6f) put(e, K.BELL, 60 + intArrayOf(0, 3, 7, 10)[r.nextInt(4)], tNote, 0.2f, r.nextFloat() * 2f - 1f)
                    tNote += sec(6f + 4f * r.nextFloat())
                }
            }
            VoidPhase.COLLECT -> {
                // golden: a soft rising chord
                while (tPad < to) {
                    intArrayOf(60, 64, 67, 71).forEach { put(e, K.CELESTA, it, tPad, 0.3f, r.nextFloat() - 0.5f) }
                    tPad += sec(5f)
                }
            }
            VoidPhase.POEM -> {
                // a very quiet bed so the words can breathe
                while (tPad < to) {
                    intArrayOf(36, 43, 48).forEach { put(e, K.AIRYPAD, it, tPad, 0.14f, r.nextFloat() - 0.5f) }
                    tPad += sec(8.5f)
                }
                while (tMisc < to) {
                    put(e, K.BELL, 72 + intArrayOf(0, 7, 12, 4)[r.nextInt(4)], tMisc, 0.1f, r.nextFloat() * 2f - 1f)
                    tMisc += sec(28f + r.nextFloat() * 20f)
                }
            }
            VoidPhase.AFTER -> {
                // the empty universe: almost nothing, a faint drifting tone now and then
                while (tMisc < to) {
                    put(e, K.SPACE, 55 + r.nextInt(12), tMisc, 0.12f, r.nextFloat() * 2f - 1f)
                    tMisc += sec(26f + r.nextFloat() * 20f)
                }
            }
            else -> {
                // FAILURE, SILENCE, REVEAL, CONTROL: nothing at all
            }
        }
    }
}

/** Heaven: seven colours, seven notes of a warm C major scale. */
internal object Heaven {
    // red, orange, yellow, green, blue, indigo, violet
    val NOTES = intArrayOf(60, 62, 64, 65, 67, 69, 71)
}

internal class HeavenScore : Score() {
    override val room = RoomSpec(1.5f, 0.88f, 0.5f, 0.7f)
    override val info = "heaven · seven colours, seven notes · C major"

    private val r = Random(System.nanoTime())
    private var started = false
    private var tChord = 0L
    private var tPhrase = 0L
    private var chordI = 0
    private val chords =
        arrayOf(intArrayOf(48, 55, 60, 64), intArrayOf(45, 52, 57, 60), intArrayOf(41, 48, 53, 57), intArrayOf(43, 50, 55, 59))
    private val motifs =
        arrayOf(intArrayOf(0, 2, 4, 2, 1, 0), intArrayOf(4, 5, 6, 5, 4, 2), intArrayOf(0, 1, 2, 4, 6, 4), intArrayOf(2, 4, 6, 4, 2, 0))

    private fun sec(s: Float) = (s * AR).toLong()

    override fun generate(from: Long, to: Long, e: Emitter) {
        if (!started) {
            started = true
            tChord = from
            tPhrase = from + sec(6f)
        }
        while (tChord < to) {
            val c = chords[chordI % chords.size]
            c.forEachIndexed { i, m -> e.play(NoteBank.get(K.WARMPAD, m), tChord, 0.26f, if (i % 2 == 0) -0.5f else 0.5f) }
            // a little harp rising through the chord
            for (i in 0 until 4) e.play(NoteBank.get(K.HARP, c[i % c.size] + 12), tChord + sec(0.6f + 0.5f * i), 0.4f, r.nextFloat() - 0.5f)
            chordI++
            tChord += sec(12f)
        }
        while (tPhrase < to) {
            val m = motifs[r.nextInt(motifs.size)]
            var t = tPhrase
            for (idx in m) {
                e.play(NoteBank.get(K.PIANO, Heaven.NOTES[idx]), t, 0.6f, -0.15f)
                t += sec(1.0f + r.nextFloat() * 0.7f)
            }
            if (r.nextFloat() < 0.6f) {
                e.play(NoteBank.get(K.CELESTA, Heaven.NOTES[m[m.size - 1]] + 12), t + sec(1.2f), 0.45f, 0.4f)
            }
            tPhrase = t + sec(7f + r.nextFloat() * 9f)
        }
    }
}
