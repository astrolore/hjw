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
// The composer for ordinary worlds: slow, spacious, sparse ambient music in the spirit of old
// sandbox-game soundtracks. Soft piano, celesta, bells, harp and vibraphone play a few unhurried
// notes over a warm pad or a deep drone, with a lot of silence and a lot of room. There are no
// beats, no percussion and no field-recording sounds.
//
// A world's seed picks an arrangement (ten of them, sometimes with a second blended in), a key
// and mode, tempo, chord movement, a motif, the instruments and the size of the room.
// ---------------------------------------------------------------------------------------------

private val MAJOR = intArrayOf(0, 2, 4, 5, 7, 9, 11)
private val LYDIAN = intArrayOf(0, 2, 4, 6, 7, 9, 11)
private val MIXO = intArrayOf(0, 2, 4, 5, 7, 9, 10)
private val DORIAN = intArrayOf(0, 2, 3, 5, 7, 9, 10)
private val AEOLIAN = intArrayOf(0, 2, 3, 5, 7, 8, 10)
private val PHRYG = intArrayOf(0, 1, 3, 5, 7, 8, 10)
private val MODES = arrayOf(MAJOR, LYDIAN, MIXO, DORIAN, AEOLIAN, PHRYG)
private val MODE_NAMES = arrayOf("major", "lydian", "mixolydian", "dorian", "aeolian", "phrygian")
private val NOTE_NAMES = arrayOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

private class Arch(
    val name: String,
    val lead: IntArray,
    val answer: IntArray,
    val arp: IntArray,
    val pad: IntArray,
    val texture: IntArray,
    val modes: IntArray,
    val pentP: Float,
    val bpmLo: Float,
    val bpmHi: Float,
    val density: Float,
    val arpP: Float,
    val droneP: Float,
    val textureRate: Float,
    val room: Float,
    val wet: Float,
    val warmth: Float,
    val staticP: Float,
    val lowNoteP: Float,
)

private fun ia(vararg v: Int) = v

private val ARCHS =
    listOf(
        Arch("sparse piano", ia(K.PIANO), ia(K.CELESTA), ia(K.HARP), ia(K.AIRYPAD, K.WARMPAD), ia(K.SHIMMER), ia(0, 1, 2), 0.5f, 46f, 62f, 0.22f, 0.1f, 0.2f, 0.2f, 1.3f, 0.45f, 0.6f, 0.4f, 0.5f),
        Arch("piano and pad", ia(K.PIANO), ia(K.CELESTA), ia(K.HARP), ia(K.WARMPAD), ia(K.SPACE), ia(0, 2, 3), 0.3f, 52f, 68f, 0.4f, 0.35f, 0.3f, 0.25f, 1.1f, 0.4f, 0.65f, 0.3f, 0.4f),
        Arch("celesta lullaby", ia(K.CELESTA), ia(K.BELL), ia(K.HARP), ia(K.AIRYPAD), ia(K.SHIMMER), ia(0, 1), 0.6f, 48f, 60f, 0.3f, 0.25f, 0.1f, 0.35f, 1.4f, 0.5f, 0.6f, 0.45f, 0.2f),
        Arch("bells and drone", ia(K.BELL), ia(K.BELL, K.CELESTA), ia(), ia(K.WARMPAD), ia(K.SPACE), ia(4, 3, 5), 0.3f, 42f, 54f, 0.16f, 0f, 1f, 0.5f, 1.6f, 0.55f, 0.5f, 0.8f, 0.3f),
        Arch("harp reverie", ia(K.HARP), ia(K.CELESTA, K.PIANO), ia(K.HARP), ia(K.WARMPAD, K.AIRYPAD), ia(K.SHIMMER), ia(0, 1, 2), 0.4f, 50f, 66f, 0.35f, 0.6f, 0.2f, 0.2f, 1.2f, 0.4f, 0.65f, 0.35f, 0.3f),
        Arch("vibraphone dusk", ia(K.VIBES), ia(K.PIANO), ia(K.VIBES), ia(K.WARMPAD), ia(K.REVPIANO), ia(3, 4, 2), 0.25f, 48f, 62f, 0.3f, 0.3f, 0.3f, 0.35f, 1.3f, 0.45f, 0.55f, 0.35f, 0.3f),
        Arch("glass shimmer", ia(K.CELESTA, K.BELL), ia(K.CELESTA), ia(), ia(K.AIRYPAD), ia(K.SHIMMER, K.REVPIANO, K.SPACE), ia(1, 0), 0.6f, 46f, 58f, 0.25f, 0f, 0.2f, 0.8f, 1.5f, 0.5f, 0.7f, 0.6f, 0.1f),
        Arch("deep space", ia(K.BELL, K.PIANO), ia(K.BELL), ia(), ia(K.WARMPAD), ia(K.SPACE, K.SHIMMER), ia(4, 5, 3), 0.25f, 40f, 52f, 0.12f, 0f, 1f, 0.6f, 1.7f, 0.55f, 0.5f, 0.85f, 0.5f),
        Arch("low piano", ia(K.PIANO), ia(K.PIANO, K.CELESTA), ia(K.HARP), ia(K.WARMPAD), ia(K.REVPIANO), ia(3, 4, 0), 0.3f, 44f, 58f, 0.28f, 0.2f, 0.4f, 0.3f, 1.2f, 0.4f, 0.55f, 0.4f, 0.7f),
        Arch("morning light", ia(K.PIANO, K.CELESTA), ia(K.BELL), ia(K.HARP), ia(K.AIRYPAD), ia(K.SHIMMER), ia(0, 1, 2), 0.55f, 54f, 70f, 0.38f, 0.4f, 0.15f, 0.25f, 1.2f, 0.45f, 0.7f, 0.3f, 0.3f),
    )

internal class WorldScore(private val seed: Long) : Score() {
    private val fp = Random(seed * 6364136223846793005L + 1442695040888963407L)
    private val perf = Random(seed xor System.nanoTime())

    private val a1: Arch = ARCHS[fp.nextInt(ARCHS.size)]
    private val a2: Arch? = if (fp.nextFloat() < 0.4f) ARCHS[fp.nextInt(ARCHS.size)].takeIf { it !== a1 } else null

    private fun pick(arr: IntArray): Int = arr[fp.nextInt(arr.size)]

    private val bpm = a1.bpmLo + fp.nextFloat() * (a1.bpmHi - a1.bpmLo)
    private val modeIdx = a1.modes[fp.nextInt(a1.modes.size)]
    private val scale7 = MODES[modeIdx]
    private val usePent = fp.nextFloat() < a1.pentP
    private val melDeg = if (usePent) intArrayOf(0, 1, 2, 4, 5) else intArrayOf(0, 1, 2, 3, 4, 5, 6)
    private val root = 41 + fp.nextInt(12)
    private val width = 0.3f + 0.5f * fp.nextFloat()
    private val leadPan = (fp.nextFloat() - 0.5f) * 0.4f
    private val register = fp.nextInt(3) // 0 low, 1 middle, 2 high lead

    private val leadKind = pick(a1.lead)
    private val answerKind = pick(a1.answer)
    private val arpKind = if (a1.arp.isNotEmpty()) pick(a1.arp) else K.HARP
    private val padKind = pick(a1.pad)
    private val texture: IntArray = (a1.texture + (if (a2 != null) a2.texture else ia())).distinct().toIntArray()

    private val density = (a1.density + (fp.nextFloat() - 0.5f) * 0.12f).coerceIn(0.08f, 0.6f)
    private val arpOn = a1.arpP > 0f && fp.nextFloat() < a1.arpP
    private val droneOn = fp.nextFloat() < a1.droneP
    private val staticWorld = fp.nextFloat() < a1.staticP
    private val lowNote = fp.nextFloat() < a1.lowNoteP
    private val ext = fp.nextInt(4)

    private val stepSamples = (60f / bpm / 4f * AR).toInt().coerceAtLeast(800)
    private val barSteps = 16
    private val prog: IntArray = run {
        val progs = arrayOf(intArrayOf(0, 3, 4, 0), intArrayOf(0, 5, 3, 4), intArrayOf(0, 2, 3, 0), intArrayOf(0, 6, 4, 0), intArrayOf(0, 3, 0, 6), intArrayOf(0, 1, 3, 0), intArrayOf(0, 4, 0, 2), intArrayOf(0, 5, 0, 3))
        if (staticWorld) intArrayOf(0) else progs[fp.nextInt(progs.size)].copyOf(2 + fp.nextInt(3))
    }
    private val chordBars = if (staticWorld) 8 else 4 + 2 * fp.nextInt(2)
    private val chordSteps = (barSteps * chordBars).toLong()

    private fun scaleNote(idx: Int): Int = root + scale7[Math.floorMod(idx, 7)] + 12 * Math.floorDiv(idx, 7)

    private val melPool = ArrayList<Int>().also { l ->
        val base = 3 + 5 * register // the register moves the lead up or down
        for (idx in (base)..(base + 13)) if (Math.floorMod(idx, 7) in melDeg) l.add(scaleNote(idx))
    }
    private val motifLen = 3 + fp.nextInt(3)
    private val motifStep = IntArray(motifLen)
    private val motifPos = IntArray(motifLen)
    private val motifSteps: Int

    init {
        // unhurried: a note every one to three beats
        val gaps = intArrayOf(4, 6, 8, 5, 4, 10)
        var sp = 0
        var mp = melPool.size / 2
        for (i in 0 until motifLen) {
            motifStep[i] = sp
            sp += gaps[fp.nextInt(gaps.size)]
            mp = (mp + fp.nextInt(-2, 3)).coerceIn(0, melPool.size - 1)
            motifPos[i] = mp
        }
        motifSteps = sp
    }

    override val room = RoomSpec(a1.room * (0.9f + 0.3f * fp.nextFloat()), 0.82f + 0.08f * fp.nextFloat(), a1.wet, a1.warmth)

    override val info: String =
        a1.name + (if (a2 != null) " + " + a2.name else "") + " · " + bpm.toInt() + " BPM · " + NOTE_NAMES[root % 12] + " " + MODE_NAMES[modeIdx] +
            (if (usePent) " pent" else "")

    // ---- performance state
    private var nextStepAt = -1L
    private var step = 0L
    private var chordI = 0
    private var curTones = intArrayOf(0, 2, 4)
    private var phraseCool = 0
    private var arpCounter = 0
    private var sectionStepsLeft = 0
    private var padNextAt = 0L
    private var droneNextAt = 0L
    private var textureNextAt = 0L

    private var onPad = false
    private var onDrone = false
    private var onLead = false
    private var onAnswer = false
    private var onArp = false
    private var onTexture = false

    private fun put(e: Emitter, kind: Int, midi: Int, at: Long, gain: Float, pan: Float) {
        val jitter = (perf.nextFloat() * 0.02f * AR).toLong()
        e.play(NoteBank.get(kind, midi), at + jitter, gain * (0.8f + 0.35f * perf.nextFloat()), pan * width, 1f + (perf.nextFloat() - 0.5f) * 0.003f)
    }

    private fun newSection(first: Boolean) {
        val seconds = 35f + perf.nextFloat() * 55f
        sectionStepsLeft = (seconds * AR / stepSamples).toInt()
        if (first) {
            // the first half minute is atmosphere only; the piano arrives afterwards
            sectionStepsLeft = (26f * AR / stepSamples).toInt()
            onPad = true
            onDrone = droneOn
            onLead = false
            onAnswer = false
            onArp = false
            onTexture = false
            return
        }
        val quiet = perf.nextFloat() < 0.2f
        onPad = perf.nextFloat() < 0.85f
        onDrone = droneOn && perf.nextFloat() < 0.85f
        onLead = !quiet && perf.nextFloat() < 0.55f + density
        onAnswer = onLead && perf.nextFloat() < 0.55f
        onArp = arpOn && !quiet && perf.nextFloat() < 0.5f
        onTexture = texture.isNotEmpty() && perf.nextFloat() < 0.5f
        if (!onPad && !onDrone && !onLead) onPad = true
    }

    private fun chordTones(): IntArray {
        val d = prog[chordI]
        return when (ext) {
            1 -> intArrayOf(d, d + 2, d + 4, d + 6)
            2 -> intArrayOf(d, d + 2, d + 4, d + 8)
            3 -> intArrayOf(d, d + 1, d + 4)
            else -> intArrayOf(d, d + 2, d + 4)
        }
    }

    private fun snapToChord(midi: Int): Int {
        var best = midi
        var bestDist = Int.MAX_VALUE
        for (m in melPool) {
            var inChord = false
            for (t in curTones) if (Math.floorMod(scaleNote(t) - m, 12) == 0) inChord = true
            if (inChord && Math.abs(m - midi) < bestDist) {
                bestDist = Math.abs(m - midi)
                best = m
            }
        }
        return best
    }

    private fun placeChord(e: Emitter, at: Long) {
        curTones.forEachIndexed { i, t ->
            val pan = if (i % 2 == 0) -0.6f else 0.6f
            put(e, padKind, scaleNote(t), at, 0.42f / curTones.size * 1.6f, pan)
        }
    }

    private fun startPhrase(e: Emitter, at: Long) {
        val variation = perf.nextInt(5)
        val shift = if (variation == 1) 1 else if (variation == 2) -1 else 0
        val count = if (variation == 3) max(2, motifLen - 1) else motifLen
        var lastMidi = melPool[0]
        for (i in 0 until count) {
            var p = (motifPos[i] + shift).coerceIn(0, melPool.size - 1)
            if (variation == 4 && i == count - 1) p = (p + 2).coerceAtMost(melPool.size - 1)
            if (perf.nextFloat() < 0.1f) p = (p + (if (perf.nextBoolean()) 1 else -1)).coerceIn(0, melPool.size - 1)
            var midi = melPool[p]
            if (i == 0 || i == count - 1) midi = snapToChord(midi)
            lastMidi = midi
            put(e, leadKind, midi, at + motifStep[i].toLong() * stepSamples, 0.6f + 0.2f * perf.nextFloat(), leadPan)
        }
        // a second instrument answers softly, now and then
        if (onAnswer && perf.nextFloat() < 0.7f) {
            val base = melPool.indexOf(lastMidi).coerceAtLeast(0)
            val a = melPool[(base + 2).coerceAtMost(melPool.size - 1)]
            val b = melPool[(base + 4).coerceAtMost(melPool.size - 1)]
            val t0 = at + (motifStep[count - 1] + 6 + perf.nextInt(4)).toLong() * stepSamples
            put(e, answerKind, a, t0, 0.45f, -leadPan - 0.3f)
            put(e, answerKind, b, t0 + 4L * stepSamples, 0.4f, -leadPan - 0.3f)
        }
        phraseCool = (motifSteps + barSteps - 1) / barSteps + 2 + perf.nextInt(4)
    }

    private fun doStep(e: Emitter, at: Long) {
        val sInBar = (step % barSteps).toInt()

        if (step % chordSteps == 0L) {
            chordI = ((step / chordSteps) % prog.size).toInt()
            curTones = chordTones()
            if (onPad) {
                placeChord(e, at)
                padNextAt = at + 8L * AR
            }
            if (onDrone && at >= droneNextAt) {
                put(e, K.DRONE, scaleNote(prog[chordI]) - 12, at, 0.5f, 0f)
                droneNextAt = at + 12L * AR
            }
            // one low, soft note under a new chord
            if (lowNote && perf.nextFloat() < 0.6f) put(e, K.PIANO, scaleNote(prog[chordI]) - 12, at + 2L * stepSamples, 0.55f, 0f)
        } else {
            if (onPad && at >= padNextAt) {
                placeChord(e, at)
                padNextAt = at + 8L * AR
            }
            if (onDrone && at >= droneNextAt) {
                put(e, K.DRONE, scaleNote(prog[chordI]) - 12, at, 0.45f, 0f)
                droneNextAt = at + 12L * AR
            }
        }

        if (sInBar == 0) {
            if (phraseCool > 0) phraseCool--
            if (onLead && phraseCool <= 0 && perf.nextFloat() < 0.5f + density) startPhrase(e, at)

            // a slow, quiet arpeggio of the chord
            if (onArp && perf.nextFloat() < 0.35f) {
                for (k in 0 until 4) {
                    val tone = curTones[k % curTones.size]
                    put(e, arpKind, scaleNote(tone + 7), at + (k * 3L + 2L) * stepSamples, 0.32f, 0.4f + leadPan)
                }
            }
            if (onTexture && at >= textureNextAt && perf.nextFloat() < 0.4f) {
                val kind = texture[perf.nextInt(texture.size)]
                val m = scaleNote(curTones[perf.nextInt(curTones.size)]) + (if (kind == K.SHIMMER) 12 else 0)
                put(e, kind, m, at, 0.5f, perf.nextFloat() * 1.6f - 0.8f)
                textureNextAt = at + (22f + perf.nextFloat() * 18f).toLong() * AR
            }
        }

        step++
        sectionStepsLeft--
        if (sectionStepsLeft <= 0) newSection(false)
    }

    private var started = false

    override fun generate(from: Long, to: Long, e: Emitter) {
        if (!started) {
            started = true
            nextStepAt = from
            newSection(true)
        }
        while (nextStepAt < to) {
            doStep(e, nextStepAt)
            nextStepAt += stepSamples
        }
    }
}
