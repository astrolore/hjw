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

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// The Director runs the two special worlds:
//
//   * THE ULTIMATE VOID (the largest possible seed): a long final sequence. A quiet arrival, small
//     warnings from the star, its death, the planets breaking apart, the remains of the star
//     gathering along the edge of the universe, KASA's last order, the collection of a sample, a
//     failure of everything, then the poem on an empty night, and finally an endless, empty
//     universe where the ship is alone.
//   * HEAVEN (seed 7): seven stars joined by living lines that sing.
//
// Everything runs on the game's own clock, so it pauses when the app is in the background. Things
// never fade out here: they flicker out, like everything else that disappears in this game.
// ---------------------------------------------------------------------------------------------

/** The Ultimate Void lives at the very end of the seed range: the poem says it comes last. */
const val VOID_SEED = Long.MAX_VALUE
const val HEAVEN_SEED = 7L

private const val BOUNDARY = UNIVERSE_RANGE

private class Particle(var x: Float, var y: Float, val vx: Float, val vy: Float, var life: Float, val maxLife: Float, val color: Color, val size: Float)

private class Dust(
    val angle0: Float,
    val r0: Float,
    val target: Float,
    val delay: Float,
    val duration: Float,
    val curve: Float,
    val drift: Float,
    val color: Color,
    val size: Float,
    val seed: Float,
) {
    var absorbAt = -1f
}

private class PState(val planet: Planet, val vanishAt: Float) {
    var breakAt = -1f
    var gone = false
}

private class PoemParticle(var x: Float, var y: Float, val vx: Float, val vy: Float, val born: Float, val life: Float, val color: Color, val size: Float)

internal class Panel(val title: String, val lines: List<String>, val kind: Int, val seconds: Float, val typed: Boolean)

// ------------------------------------------------------------------ the poem (kept exactly as written)

private const val POEM_TEXT = """THE ULTIMATE VOID

When all the souls are gone, And time runs out of next, The Ultimate Void will then appear, And leave the stars perplexed.

It stretches past the edge of worlds, Beyond what eyes can see, A hunger born of nothingness, A fate that none can flee.

It reached across the universe, To swallow every light, To turn the endless sea of stars Into eternal night.

But seven stars stood in the way, Small lights holding back the dark. Seven flames against the end, Each one a dying spark.

They had no worlds to call their own, No voices left to sing, Yet still they stood against the Void, Defending everything.

Then the light turned to night.

Day after day, the skies turned grey, And tears fell from the sky. The dawn had faded far away, And stars forgot to shine.

The worlds grew cold, the winds grew still, The heavens lost their glow. And time, once racing ever on, Forgot which way to go.

The seven stars began to fade, Their light no longer stayed. And one by one, they disappeared, Until the dark had won.

The Void consumed the final star. The end had just begun.

And yet, somehow, you came this far.

You flew out here anyway, Past everything, to where nothing is. Past dying stars and broken worlds, Past all that ever lived.

You crossed the edge of space and time, Through emptiness untold. Perhaps you sought an answer here, Perhaps a truth to hold.

But before the dark, before the end, Before the Void had come to be, The stars once danced with their worlds in peace, In perfect harmony.

Seed 7 preserves the heaven.

A world from the before times, When stars and planets shone as one, When seven lights adorned the skies, Beneath a peaceful sun.

The planets turned, the stars held fast, And time flowed ever on. The heavens sang their quiet song, And darkness was unknown.

Perhaps the seed remembers all. Perhaps it holds the key. Perhaps the world was never lost, But waiting to be seen.

And now you stand here, at the edge, Where nothing dares to grow. The Void has swallowed everything, Or so it would have you know.

But tell me, player, what did you expect to find?

A treasure hidden past the end? A secret left behind? A final door, a final world, A meaning to the climb?

There is no final boss to fight, No last reward to claim. No ending that can close the world, No final screen to name.

Just you.

And the Void.

And the strange, quiet fact That you are still here.

Perhaps that is enough to wonder about.

For when everything had disappeared, When even time had ceased to flow, Something brought you to this place.

Something made you want to know.

Was it curiosity? Was it hope? Or did you simply want to see What lay beyond the end of things, Beyond what things could be?

You see, player, there is something the Void can never understand.

It can swallow worlds, It can extinguish stars, It can erase the distance between here and there.

But it cannot make the journey meaningless.

You came here. You explored. You wondered.

And that happened.

No ending can undo it.

Perhaps the seven stars knew this. Perhaps they never believed they could save the universe. Perhaps they stood against the dark simply because there was still something worth standing for.

Or perhaps they were just seven stars.

Sometimes, a light does not need a reason to shine.

Sometimes, a person does not need a reason to keep going.

And sometimes, the greatest discoveries are not waiting at the end of the universe, but right where you left them.

Your world.

Your home.

Your life.

You have good friends, player. You have a family, people who know you, people who care about you, even if neither of you always knows how to say it.

You have days ahead of you that you cannot yet imagine.

You have places to visit, things to create, people to meet, and memories you haven't made.

And I wonder...

Have you been so busy exploring this world that you have forgotten the one outside it?

Have you spent so long looking at these stars that you have forgotten the ones above your own sky?

Do you remember the feeling of standing outside, doing absolutely nothing, and somehow being perfectly content?

Do you remember laughing with a friend until you could hardly speak?

Do you remember a day when nothing special happened, yet you wished it would never end?

Those things may seem small.

But perhaps the universe was never made for the grandest things alone.

Perhaps it was made for the little moments, too.

The sun rising over an ordinary morning. The wind passing through the trees. The sound of someone calling you home. The simple joy of being somewhere you have never been.

The universe is impossibly vast, player. And your life is only a tiny part of it.

But tiny does not mean worthless.

A star is small compared to a galaxy. A galaxy is small compared to the universe. And yet, without the stars, the night would be empty.

So do not spend your life believing that the world is nothing but darkness.

There will be bad days. There will be disappointments. There will be moments when everything seems grey, and you wonder whether the light is ever coming back.

But the sky has always had more than one colour.

And the night has never been the whole story.

You do not have to save the universe, player.

You do not have to become the greatest person who ever lived. You do not have to turn every moment into something extraordinary.

You are allowed to live.

To make mistakes. To change your mind. To waste an afternoon doing something silly. To go somewhere without knowing exactly why.

You are allowed to enjoy the world without first proving that you deserve it.

Because the world outside this screen is not a perfect place.

But it is a real one.

And that makes all the difference.

Real life has the best graphics.

Not because every sunset is beautiful, or every day is bright, or every person is kind.

But because you can actually be there.

You can feel the warmth of the sun. You can hear the rain. You can walk through a forest, climb a hill, explore a city, or sit beside someone you love and talk about absolutely nothing.

You can make memories that no game could ever give you.

And perhaps, one day, when you are older, you will remember this world.

You will remember the endless darkness, the seven stars, and the strange journey that brought you here.

You might even return to see it again.

But you will have other worlds to explore by then.

Worlds made of memories, friendships, adventures, and all the little things you once took for granted.

So, player, before you go, look at the Void one last time.

It has swallowed everything.

Every star. Every world. Every soul.

And yet, here you are, still wondering what comes next.

Maybe the answer was never hidden in the darkness.

Maybe it was waiting for you outside all along.

So let the screen rest.

Go outside.

Watch the sky.

Travel somewhere new. Play with your friends. Spend time with your family. Make something you are proud of. Do something just because it makes you happy.

And when you return, this world will still be here.

The Void will still be waiting.

The seed will still be seven.

And somewhere, beyond the darkness, another sunrise will be waiting to happen.

Because perhaps the Void was never the end of the universe.

Perhaps it was only the end of one way of seeing it.

Perhaps every ending is a beginning that has not yet learned its name.

And perhaps, somewhere, seven stars are gathering once again.

Not to save the universe.

But to remind it that there is always another world to discover.

Another mountain to climb.

Another ocean to cross.

Another story to tell.

So go, player.

The world is not finished.

Neither are you.

There are sunsets beyond these pixels.

There are stars beyond this sky.

There are people waiting to hear your voice, places waiting to be explored, and days waiting to become memories.

Go live a little.

Go laugh a little.

Go see what happens next.

And if, someday, you find yourself staring into the darkness again, remember the seven stars.

Remember that even when everything seemed lost, there was once a light.

And remember that you, too, are part of a universe that is still unfolding.

The Void may be infinite.

But so are the possibilities of a world that has not yet ended.

The world goes on, player.

And so do you."""

internal object Poem {
    private class Para(val lines: List<String>, var hold: Float)

    private var paras: List<Para> = emptyList()
    private var idx = 0
    var t by mutableFloatStateOf(0f)
    var index by mutableIntStateOf(0)
    var finished = false

    /** Seconds of poem played so far (drives the visuals under it). */
    var elapsed = 0f
    const val TOTAL = 600f
    const val LINE_DELAY = 1.2f
    const val FADE_IN = 1.0f
    const val FADE_OUT = 1.0f
    const val GAP = 0.4f

    /** Reads the poem and stretches its pauses so that it lasts exactly ten minutes. */
    fun load() {
        if (paras.isNotEmpty()) return
        val split = Regex("(?<=[,.!?;:])\\s+(?=[A-Z])")
        val raw =
            POEM_TEXT.trim().split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }.map { p ->
                val lines = p.split(split).map { it.trim() }.filter { it.isNotEmpty() }
                val chars = p.length
                Para(lines, max(1.5f + 0.03f * chars + (if (chars < 45) 1.0f else 0f), (lines.size - 1) * LINE_DELAY + 2.2f))
            }
        // scale the holds until the whole poem fills ten minutes
        var k = 1f
        repeat(6) {
            val fixed = raw.size * (FADE_OUT + GAP)
            val holds = raw.sumOf { max(it.hold * k, (it.lines.size - 1) * LINE_DELAY + 2.2f).toDouble() }.toFloat()
            k *= ((TOTAL - fixed) / holds).coerceIn(0.5f, 3f)
        }
        raw.forEach { it.hold = max(it.hold * k, (it.lines.size - 1) * LINE_DELAY + 2.2f) }
        paras = raw
    }

    fun reset() {
        idx = 0
        index = 0
        t = 0f
        elapsed = 0f
        finished = false
    }

    fun current(): List<String>? = paras.getOrNull(idx)?.lines

    fun isTitle(): Boolean = idx == 0

    /** 0..1: how present line [i] of the current paragraph is right now (it flickers in and out). */
    fun lineAlpha(i: Int): Float {
        val p = paras.getOrNull(idx) ?: return 0f
        val appear = i * LINE_DELAY
        val fin = ((t - appear) / FADE_IN).coerceIn(0f, 1f)
        val fout = ((p.hold + FADE_OUT - t) / FADE_OUT).coerceIn(0f, 1f)
        return min(fin, fout)
    }

    /** Moves the poem on; true when the last paragraph is over. */
    fun advance(dt: Float): Boolean {
        val p = paras.getOrNull(idx)
        if (p == null) {
            finished = true
            return true
        }
        t += dt
        elapsed += dt
        if (t >= p.hold + FADE_OUT + GAP) {
            idx++
            index = idx
            t = 0f
            if (idx >= paras.size) {
                finished = true
                return true
            }
        }
        return false
    }
}

// ------------------------------------------------------------------ the director

object Director {
    enum class Mode { NORMAL, VOID, HEAVEN }

    var mode = Mode.NORMAL
        private set

    // ---- what the interface reads
    var hudStage by mutableIntStateOf(0) // 0 all, 1 no catalogue, 2 no controls/telemetry, 3 no menu button
    var poemActive by mutableStateOf(false)
    var fullVoid by mutableStateOf(false)
    var skipAvailable by mutableStateOf(false)
    var collectReady by mutableStateOf(false)
    private var panel by mutableStateOf<Panel?>(null)
    private var panelClock by mutableFloatStateOf(0f)

    // ---- what the game loop and the renderer read
    var shipVisible = true
    var forceGods = false
    var voidBlend = 0f
    var starGone = false
    var shakeOffset = Offset.Zero
    var controlsLocked = false

    @Volatile var audioPhase = 0
    @Volatile var audioProgress = 0f

    private var prefs: android.content.SharedPreferences? = null
    private var poemDoneBefore = false

    // ---- the Void
    private var phase = VoidPhase.ARRIVAL
    private var pt = 0f
    private var uni: Universe? = null
    private val rnd = Random(System.nanoTime())
    private var starR0 = 4000f
    private var systemR = 30_000f
    private var nearR = 10_000f
    private var flash = 0f
    private var redGlow = 0f
    private var shockR = 0f
    private var boundaryGlow = 0f
    private var anomalyAt = -1f
    private var anomaly2On = false
    private var starDark = false
    private val particles = ArrayList<Particle>()
    private val dust = ArrayList<Dust>()
    private var dustP = 1f // chance a dust particle shows this frame (flicker-out)
    private var dustOn = false
    private val pstates = ArrayList<PState>()
    private var frame = 0
    private val shown = BooleanArray(24)
    private var collectGlow = 0f
    private var ringBroken = 0f
    private var finalLines = false
    private val poemParticles = ArrayList<PoemParticle>()
    private var poemSpawned = 0

    // ---- Heaven
    private val pulse = FloatArray(12)
    private val prevPulse = FloatArray(12)
    private val heavenColors =
        arrayOf(Color(0xFFFF4D4D), Color(0xFFFF9800), Color(0xFFFFD54F), Color(0xFF34A853), Color(0xFF4285F4), Color(0xFF5C6BC0), Color(0xFF9C6BFF))
    private val heavenIndex = intArrayOf(0, 1, 3, 4, 5, 6)

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("landroid_void", Context.MODE_PRIVATE)
        prefs = p
        poemDoneBefore = p.getBoolean("poemDone", false)
        Poem.load()
    }

    // ------------------------------------------------------------------ setup

    fun begin(u: Universe) {
        uni = u
        particles.clear()
        dust.clear()
        pstates.clear()
        poemParticles.clear()
        poemSpawned = 0
        hudStage = 0
        poemActive = false
        panel = null
        fullVoid = false
        voidBlend = 0f
        shipVisible = true
        forceGods = false
        starGone = false
        shakeOffset = Offset.Zero
        controlsLocked = false
        flash = 0f
        redGlow = 0f
        shockR = 0f
        boundaryGlow = 0f
        anomalyAt = -1f
        anomaly2On = false
        starDark = false
        dustOn = false
        dustP = 1f
        collectReady = false
        collectGlow = 0f
        ringBroken = 0f
        finalLines = false
        skipAvailable = false
        for (i in shown.indices) shown[i] = false
        audioPhase = VoidPhase.ARRIVAL
        audioProgress = 0f
        mode =
            when (u.randomSeed) {
                VOID_SEED -> Mode.VOID
                HEAVEN_SEED -> Mode.HEAVEN
                else -> Mode.NORMAL
            }
        for (i in pulse.indices) {
            pulse[i] = (i * 0.37f) % 1f
            prevPulse[i] = pulse[i]
        }
        if (mode == Mode.VOID) {
            Poem.reset()
            phase = VoidPhase.ARRIVAL
            pt = 0f
            skipAvailable = poemDoneBefore
            controlsLocked = true
            starR0 = max(u.star.radius, 1500f)
            nearR = u.planets.minOfOrNull { it.pos.mag() } ?: (starR0 * 3f)
            systemR = max(u.planets.maxOfOrNull { it.pos.mag() } ?: (starR0 * 6f), starR0 * 3f)
            // the ship starts a little outside the star, in a calm circular orbit
            val d = max(starR0 * 1.4f, min(starR0 * 1.8f, (starR0 + nearR) * 0.5f))
            u.ship.pos = Vec2(d, 0f)
            val v = kotlin.math.sqrt(GRAVITATION * u.ship.mass * u.star.mass / d)
            u.ship.velocity = Vec2(0f, v)
            u.ship.angle = (PI / 2).toFloat()
            forceGods = true
        }
    }

    // ------------------------------------------------------------------ camera helpers

    private fun geo(a: Float, b: Float, t: Float): Float = exp(ln(a) + (ln(b) - ln(a)) * t.coerceIn(0f, 1f))

    /** The zoom the scene wants (null = let the game decide). */
    fun sceneZoom(u: Universe, minDim: Float, godZoom: Float): Float? {
        if (mode != Mode.VOID) return null
        val half = minDim * 0.5f
        val zWhole = half / (BOUNDARY * 1.08f)
        val zSystem = half / (systemR * 1.3f)
        val zPlanets = half / (nearR * 1.6f)
        val zStar = half / (starR0 * 1.2f)
        val zBoundary = half / 3200f
        return when (phase) {
            VoidPhase.ARRIVAL -> geo(zWhole, zSystem, pt / 45f)
            VoidPhase.EXPLORE -> if (anomaly2On) zPlanets else zSystem
            VoidPhase.CUT1 -> geo(zPlanets, zStar, pt / 30f)
            VoidPhase.PULSE, VoidPhase.COLLAPSE -> zStar
            VoidPhase.BLAST -> geo(zStar, zWhole, pt / 20f)
            VoidPhase.VANISH, VoidPhase.DUST, VoidPhase.GATHER -> zWhole
            VoidPhase.TRANSMIT, VoidPhase.MESSAGE, VoidPhase.MISSION -> zSystem
            VoidPhase.JOURNEY -> {
                val r = u.ship.pos.mag()
                geo(zSystem, zBoundary, (r - BOUNDARY * 0.55f) / (BOUNDARY * 0.45f - 3200f))
            }
            VoidPhase.INSPECT, VoidPhase.COLLECT, VoidPhase.FAILURE -> zBoundary
            else -> null
        }
    }

    /** What the camera centres on while it is held on the middle of the universe. */
    fun centre(u: Universe): Vec2 = if (starGone) Vec2.Zero else u.star.pos

    // ------------------------------------------------------------------ small helpers

    private fun show(title: String, lines: List<String>, kind: Int, seconds: Float, typed: Boolean = false) {
        panel = Panel(title, lines, kind, seconds, typed)
        panelClock = 0f
    }

    private fun once(i: Int): Boolean {
        if (shown[i]) return false
        shown[i] = true
        return true
    }

    private fun shake(amp: Float) {
        shakeOffset = Offset((rnd.nextFloat() - 0.5f) * 2f * amp, (rnd.nextFloat() - 0.5f) * 2f * amp)
    }

    private fun pal(): Color =
        when (rnd.nextInt(10)) {
            0, 1 -> Color(0xFFA94C32)
            2, 3, 4 -> Color(0xFFC96B3D)
            5, 6, 7 -> Color(0xFFE59A50)
            8 -> Color(0xFFF6D59A)
            else -> Color(0xFFFFF0D2)
        }

    private fun enter(u: Universe, next: Int) {
        phase = next
        pt = 0f
        audioPhase = next
        audioProgress = 0f
        MusicEngine.flush()
        when (next) {
            VoidPhase.EXPLORE -> {
                controlsLocked = false
                forceGods = false
            }
            VoidPhase.CUT1 -> {
                controlsLocked = true
                forceGods = true
                u.ship.thrust = Vec2.Zero
                u.ship.autopilot?.let {
                    it.enabled = false
                    it.target = null
                }
                DYNAMIC_ZOOM = false
                anomaly2On = false
            }
            VoidPhase.PULSE -> Sound.sfx(K.RISER, 0.9f)
            VoidPhase.COLLAPSE -> {
                // from here the star is drawn by the Director: a collapsing disc
                u.starVisible = false
                starDark = true
            }
            VoidPhase.BLAST -> {
                flash = 1f
                shockR = starR0
                boundaryGlow = 1f
                starDark = false
                starGone = true
                u.star.mass = 0f
                u.star.radius = 0f
                for (i in 0 until 260) {
                    val a = rnd.nextFloat() * 6.2831855f
                    val s = 2500f + rnd.nextFloat() * 7000f
                    particles.add(Particle(0f, 0f, cos(a) * s, sin(a) * s, 3f + rnd.nextFloat() * 3f, 6f, pal(), 2f + rnd.nextFloat() * 5f))
                }
                // each planet will break when the shockwave reaches it, and vanish a little later
                pstates.clear()
                val n = max(1, u.planets.size)
                u.planets.forEachIndexed { i, p -> pstates.add(PState(p, 0f + 30f * i / n + rnd.nextFloat() * 3f)) }
                Sound.sfx(K.BOOM, 1f)
                Sound.sfx(K.SWEEP, 0.9f)
                Sound.sfx(K.COLLAPSE, 0.7f)
            }
            VoidPhase.VANISH -> {}
            VoidPhase.DUST -> {
                // what is left of the star: a cloud of glowing fragments
                dust.clear()
                for (i in 0 until 420) {
                    val a = rnd.nextFloat() * 6.2831855f
                    dust.add(
                        Dust(
                            angle0 = a,
                            r0 = rnd.nextFloat() * 2600f,
                            target = BOUNDARY - 120f - rnd.nextFloat() * 520f,
                            delay = rnd.nextFloat() * 12f,
                            duration = 15f + rnd.nextFloat() * 8f,
                            curve = (rnd.nextFloat() - 0.5f) * 0.9f,
                            drift = (rnd.nextFloat() - 0.5f) * 0.0012f,
                            color = pal(),
                            size = 1.2f + rnd.nextFloat() * rnd.nextFloat() * 4.5f,
                            seed = rnd.nextFloat(),
                        )
                    )
                }
                dustOn = true
                dustP = 1f
            }
            VoidPhase.GATHER -> {}
            VoidPhase.TRANSMIT -> {
                controlsLocked = false
                forceGods = false
                Sound.sfx(K.RADIO, 0.9f)
                show(
                    "KASA // INTERUNIVERSAL RESEARCH DIVISION",
                    listOf("PRIORITY: CRITICAL", "UNIVERSE STATUS: TERMINAL", "STELLAR STATUS: EXTINCT", "PLANETARY STATUS: LOST", "MATERIAL DETECTED: UNKNOWN STELLAR RESIDUE", "LOCATION: THE EDGE OF THE UNIVERSE"),
                    1,
                    29f,
                    typed = true,
                )
            }
            VoidPhase.MESSAGE -> {
                Sound.sfx(K.BEEP, 0.6f)
                show(
                    "INCOMING MESSAGE",
                    listOf(
                        "The final stellar event has concluded.",
                        "Our instruments have detected an unusual concentration of stellar residue along the edge of this universe.",
                        "Your mission is to retrieve a sample of this material. Approach the edge and collect the residue.",
                        "This may be our final opportunity to study the remains of this universe.",
                    ),
                    1,
                    34f,
                    typed = true,
                )
            }
            VoidPhase.MISSION -> {
                Sound.sfx(K.BEEP, 0.8f)
                show("NEW OBJECTIVE", listOf("RETRIEVE STELLAR RESIDUE"), 2, 12f)
            }
            VoidPhase.JOURNEY -> collectReady = false
            VoidPhase.INSPECT -> {
                Sound.sfx(K.BEEP, 0.8f)
                show("SCANNER", listOf("STELLAR RESIDUE DETECTED", "SAMPLE CONCENTRATION: HIGH"), 0, 8f)
            }
            VoidPhase.COLLECT -> {
                controlsLocked = true
                collectReady = false
                u.ship.thrust = Vec2.Zero
                collectGlow = 0f
                for (d in dust) {
                    val x = cos(d.angle0 + d.drift * 400f) * d.target
                    val y = sin(d.angle0 + d.drift * 400f) * d.target
                    if ((Vec2(x, y) - u.ship.pos).mag() < 2600f) d.absorbAt = rnd.nextFloat() * 3f
                }
                Sound.sfx(K.BEEP, 0.8f)
            }
            VoidPhase.FAILURE -> {
                panel = null
                Sound.sfx(K.STATIC, 0.8f)
                Sound.sfx(K.COLLAPSE, 0.6f)
            }
            VoidPhase.POEM -> {
                enterVoid(u)
                poemActive = true
                fullVoid = true
                hudStage = 3
                shipVisible = false
                dustOn = false
                dust.clear()
                particles.clear()
                boundaryGlow = 0f
                panel = null
                Poem.reset()
                poemParticles.clear()
                poemSpawned = 0
            }
            VoidPhase.SILENCE -> poemActive = false
            VoidPhase.REVEAL -> {}
            VoidPhase.CONTROL -> {
                shipVisible = true
                controlsLocked = false
                show(
                    "STATUS",
                    listOf("LOCATION: THE ULTIMATE VOID", "KASA CONNECTION: LOST", "ENVIRONMENT: UNKNOWN", "OBJECTIVE: NONE"),
                    4,
                    9f,
                )
            }
            VoidPhase.AFTER -> {
                hudStage = 0
            }
        }
    }

    /** The empty universe: no star, no planets, no edge, a clean slate for the ship. */
    private fun enterVoid(u: Universe) {
        u.planets.toList().forEach {
            u.planets.remove(it)
            u.remove(it)
        }
        u.ship.landing?.let {
            it.ship = null
            u.remove(it)
        }
        u.ship.landing = null
        u.starVisible = false
        u.star.mass = 0f
        u.star.radius = 0f
        starGone = true
        starDark = false
        if (!shown[23]) {
            shown[23] = true
            u.remove(u.ringfence)
        }
        u.ship.pos = Vec2.Zero
        u.ship.velocity = Vec2.Zero
        u.ship.thrust = Vec2.Zero
        u.ship.angle = rnd.nextFloat() * 6.2831855f
        u.ship.track.positions.clear()
        fullVoid = true
        voidBlend = 1f
        forceGods = false
        flash = 0f
        redGlow = 0f
        shakeOffset = Offset.Zero
    }

    /** The COLLECT button. */
    fun collect() {
        val u = uni ?: return
        if (mode != Mode.VOID || phase != VoidPhase.INSPECT || !collectReady) return
        enter(u, VoidPhase.COLLECT)
    }

    /** SKIP (only after the poem has been finished once): straight to the empty universe. */
    fun skip() {
        val u = uni ?: return
        if (mode != Mode.VOID || phase >= VoidPhase.CONTROL || !skipAvailable) return
        panel = null
        collectReady = false
        poemActive = false
        dustOn = false
        dust.clear()
        particles.clear()
        poemParticles.clear()
        enterVoid(u)
        shipVisible = true
        controlsLocked = false
        hudStage = 0
        skipAvailable = false
        phase = VoidPhase.AFTER
        audioPhase = VoidPhase.AFTER
        MusicEngine.flush()
    }

    private fun finishPoem() {
        poemDoneBefore = true
        prefs?.edit()?.putBoolean("poemDone", true)?.apply()
        Achievements.unlock("end_to_ending")
        uni?.let { enter(it, VoidPhase.SILENCE) }
    }

    // ------------------------------------------------------------------ the main update

    fun update(u: Universe) {
        if (mode == Mode.NORMAL || uni !== u) return
        val dt = min(u.dt, 0.1f)
        frame++
        if (mode == Mode.HEAVEN) {
            updateHeaven(u, dt)
            return
        }
        pt += dt
        panelClock += dt
        panel?.let { if (panelClock >= it.seconds) panel = null }
        if (controlsLocked) u.ship.thrust = Vec2.Zero

        var i = 0
        while (i < particles.size) {
            val p = particles[i]
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt
            if (p.life <= 0f) particles.removeAt(i) else i++
        }
        flash = max(0f, flash - dt / 1.6f)
        redGlow = max(0f, redGlow - dt * 0.3f)
        if (phase >= VoidPhase.VANISH && phase <= VoidPhase.GATHER) boundaryGlow = max(0.15f, boundaryGlow - dt * 0.04f)

        when (phase) {
            VoidPhase.ARRIVAL -> {
                audioProgress = pt / 45f
                if (pt >= 45f) enter(u, VoidPhase.EXPLORE)
            }
            VoidPhase.EXPLORE -> {
                audioProgress = pt / 120f
                if (pt > 5f && once(0)) show("SCANNER", listOf("STELLAR ACTIVITY: STABLE", "ENVIRONMENT: NOMINAL"), 0, 7f)
                // first anomaly (a flicker of two seconds)
                if (pt > 45f && once(1)) {
                    anomalyAt = pt
                    Sound.sfx(K.BEEP, 0.7f)
                    show("SCANNER", listOf("UNUSUAL STELLAR FLUCTUATION DETECTED"), 0, 7f)
                }
                // second anomaly: stronger and longer
                if (pt > 75f && once(2)) {
                    anomaly2On = true
                    Sound.sfx(K.RUMBLE, 0.6f)
                    Sound.sfx(K.BEEP, 0.7f)
                    show("SCANNER", listOf("STELLAR ENERGY: FLUCTUATING", "WARNING LEVEL: ELEVATED"), 3, 9f)
                }
                if (pt >= 120f) enter(u, VoidPhase.CUT1)
            }
            VoidPhase.CUT1 -> {
                audioProgress = pt / 45f
                shake(if (pt > 10f) 1.5f + 3f * (pt / 45f) else 0f)
                if (pt > 28f && once(3)) {
                    Sound.sfx(K.BEEP, 0.9f)
                    show("SCANNER", listOf("CRITICAL STELLAR ANOMALY", "PREDICTED STELLAR COLLAPSE: IMMINENT"), 3, 15f)
                }
                if (pt >= 45f) enter(u, VoidPhase.PULSE)
            }
            VoidPhase.PULSE -> {
                audioProgress = pt / 30f
                shake(4f + 12f * (pt / 30f))
                if (pt >= 30f) enter(u, VoidPhase.COLLAPSE)
            }
            VoidPhase.COLLAPSE -> {
                audioProgress = pt / 20f
                shake(if (pt < 10f) 6f * (1f - pt / 10f) else 0f)
                if (pt >= 20f) enter(u, VoidPhase.BLAST)
            }
            VoidPhase.BLAST -> {
                audioProgress = pt / 35f
                shake(30f * exp(-pt * 0.9f))
                shockR += 4000f * dt
                for (ps in pstates) {
                    if (ps.breakAt < 0f && ps.planet.pos.mag() <= shockR) ps.breakAt = pt
                }
                breakPlanets(dt)
                if (pt >= 20f) forceGods = true
                if (pt >= 35f) enter(u, VoidPhase.VANISH)
            }
            VoidPhase.VANISH -> {
                audioProgress = pt / 30f
                shakeOffset = Offset.Zero
                shockR += 4000f * dt
                breakPlanets(dt)
                for (ps in pstates) {
                    if (!ps.gone && pt >= ps.vanishAt) {
                        ps.gone = true
                        burst(ps.planet, 70)
                        u.planets.remove(ps.planet)
                        u.remove(ps.planet)
                        if (u.ship.landing?.planet === ps.planet) {
                            u.ship.landing?.let {
                                it.ship = null
                                u.remove(it)
                            }
                            u.ship.landing = null
                        }
                        Sound.sfx(K.DEBRIS, 0.8f, (rnd.nextFloat() - 0.5f) * 1.4f)
                    }
                }
                if (pt >= 30f) enter(u, VoidPhase.DUST)
            }
            VoidPhase.DUST -> {
                audioProgress = pt / 30f
                shockR += 4000f * dt
                if (pt >= 30f) enter(u, VoidPhase.GATHER)
            }
            VoidPhase.GATHER -> {
                audioProgress = pt / 35f
                shockR += 4000f * dt
                if (pt >= 35f) enter(u, VoidPhase.TRANSMIT)
            }
            VoidPhase.TRANSMIT -> {
                audioProgress = pt / 30f
                if (pt >= 30f) enter(u, VoidPhase.MESSAGE)
            }
            VoidPhase.MESSAGE -> {
                audioProgress = pt / 35f
                if (pt >= 35f) enter(u, VoidPhase.MISSION)
            }
            VoidPhase.MISSION -> {
                audioProgress = pt / 20f
                if (pt >= 20f) enter(u, VoidPhase.JOURNEY)
            }
            VoidPhase.JOURNEY -> {
                audioProgress = 0f
                if (u.ship.pos.mag() >= BOUNDARY - 3600f) enter(u, VoidPhase.INSPECT)
                // a hint if the player has wandered for a long time
                if (pt > 180f && once(4)) show("KASA", listOf("The residue lies along the edge of the universe.", "Approach the boundary."), 1, 10f)
            }
            VoidPhase.INSPECT -> {
                audioProgress = 0f
                collectReady = u.ship.pos.mag() >= BOUNDARY - 3600f
                if (u.ship.pos.mag() < BOUNDARY - 7000f) enter(u, VoidPhase.JOURNEY)
            }
            VoidPhase.COLLECT -> {
                audioProgress = pt / 15f
                collectGlow = if (pt < 9f) pt / 9f else 1f - (pt - 9f) / 6f
                if (pt > 9f && once(5)) {
                    Sound.sfx(K.GLASS, 0.9f)
                    show("SCANNER", listOf("SAMPLE ACQUIRED"), 2, 5f)
                }
                if (pt >= 15f) enter(u, VoidPhase.FAILURE)
            }
            VoidPhase.FAILURE -> {
                audioProgress = pt / 15f
                // the glow of the edge breaks up, the dust flickers out, the interface goes
                ringBroken = (pt / 12f).coerceIn(0f, 1f)
                dustP = (1f - pt / 12f).coerceIn(0f, 1f)
                val stage = if (pt > 9f) 3 else if (pt > 6f) 2 else if (pt > 3f) 1 else 0
                if (stage > hudStage) {
                    hudStage = stage
                    Sound.sfx(K.GLITCH, 0.8f)
                }
                if (pt > 2f && once(6)) show("SCANNER", listOf("SIGNAL LOST", "KASA CONNECTION: UNSTABLE"), 3, 6f)
                voidBlend = if (pt > 10f) ((pt - 10f) / 5f).coerceIn(0f, 1f) else 0f
                if (pt >= 15f) enter(u, VoidPhase.POEM)
            }
            VoidPhase.POEM -> {
                audioProgress = 0f
                updatePoemVisuals(dt)
                if (Poem.advance(dt)) finishPoem()
            }
            VoidPhase.SILENCE -> {
                if (pt >= 10f) enter(u, VoidPhase.REVEAL)
            }
            VoidPhase.REVEAL -> {
                if (pt >= 10f) enter(u, VoidPhase.CONTROL)
            }
            VoidPhase.CONTROL -> {
                if (pt >= 10f) enter(u, VoidPhase.AFTER)
            }
            else -> audioProgress = 0f
        }
    }

    private fun burst(p: Planet, n: Int) {
        for (k in 0 until n) {
            val a = rnd.nextFloat() * 6.2831855f
            val s = 100f + rnd.nextFloat() * 1600f
            particles.add(Particle(p.pos.x, p.pos.y, cos(a) * s, sin(a) * s, 2.5f + rnd.nextFloat() * 3.5f, 6f, if (rnd.nextBoolean()) p.color else pal(), 2f + rnd.nextFloat() * 5f))
        }
    }

    /** Planets touched by the shockwave lose pieces of themselves until they vanish. */
    private fun breakPlanets(dt: Float) {
        for (ps in pstates) {
            if (ps.breakAt >= 0f && !ps.gone && rnd.nextFloat() < 14f * dt) {
                val a = rnd.nextFloat() * 6.2831855f
                val r = ps.planet.radius
                particles.add(
                    Particle(
                        ps.planet.pos.x + cos(a) * r,
                        ps.planet.pos.y + sin(a) * r,
                        cos(a) * 160f,
                        sin(a) * 160f,
                        3f,
                        3f,
                        if (rnd.nextBoolean()) ps.planet.color else pal(),
                        2f + rnd.nextFloat() * 3f,
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------ the poem's visuals

    private fun updatePoemVisuals(dt: Float) {
        val t = Poem.elapsed
        var i = 0
        while (i < poemParticles.size) {
            val p = poemParticles[i]
            p.x += p.vx * dt
            p.y += p.vy * dt
            if (t - p.born > p.life) poemParticles.removeAt(i) else i++
        }
        fun spawn(n: Int, gold: Boolean) {
            repeat(n) {
                val side = rnd.nextFloat()
                poemParticles.add(
                    PoemParticle(
                        0.15f + 0.7f * rnd.nextFloat(),
                        0.2f + 0.6f * rnd.nextFloat(),
                        (rnd.nextFloat() - 0.5f) * 0.02f,
                        (rnd.nextFloat() - 0.5f) * 0.015f,
                        t,
                        12f + 8f * side,
                        if (gold) Color(0xFFF6D59A) else Color(0xFFC96B3D),
                        if (gold) 2.2f else 1.8f + 2f * side,
                    )
                )
            }
        }
        // the choreography, stretched over ten minutes
        if (t > 20f && poemSpawned < 1) {
            poemSpawned = 1
            spawn(1, false)
        }
        if (t > 70f && poemSpawned < 2) {
            poemSpawned = 2
            spawn(2, false)
        }
        if (t in 90f..150f && poemParticles.size < 5 && rnd.nextFloat() < 0.02f) spawn(1, false)
        if (t in 150f..270f && poemParticles.size < 6 && rnd.nextFloat() < 0.012f) spawn(1, false)
        if (t > 400f && poemSpawned < 3) {
            poemSpawned = 3
            spawn(1, true)
        }
    }

    // ------------------------------------------------------------------ Heaven

    private fun heavenPos(u: Universe, i: Int): Offset {
        val a = u.now * 0.03f + i * (PI / 3.0).toFloat()
        return Offset(cos(a) * HEAVEN_RING, sin(a) * HEAVEN_RING)
    }

    private fun updateHeaven(u: Universe, dt: Float) {
        for (k in 0 until 12) {
            val period = if (k < 6) 7f else 11f
            prevPulse[k] = pulse[k]
            pulse[k] = (pulse[k] + dt / period) % 1f
            if (pulse[k] < prevPulse[k]) {
                if (k < 6) {
                    val dest = (k + 1) % 6
                    Sound.note(K.CELESTA, Heaven.NOTES[heavenIndex[dest]] + 12, 0.4f, (dest - 2.5f) / 3f)
                } else {
                    Sound.note(K.BELL, Heaven.NOTES[2] - 12, 0.3f, 0f)
                }
            }
        }
    }

    // ------------------------------------------------------------------ drawing: the world

    private fun flick(p: Float): Boolean = rnd.nextFloat() < p

    /** World-space extras. */
    fun drawWorld(scope: DrawScope, u: Universe, zoom: Float) {
        if (mode == Mode.NORMAL || uni !== u) return
        with(scope) {
            val px = 1f / zoom
            if (mode == Mode.HEAVEN) {
                drawHeaven(u, px)
                return
            }
            drawVoidWorld(u, px)
        }
    }

    private fun DrawScope.drawVoidWorld(u: Universe, px: Float) {
        val c = Offset.Zero
        // ---- the star's moods
        if (phase == VoidPhase.EXPLORE || phase == VoidPhase.CUT1 || phase == VoidPhase.PULSE) {
            var tint = Color.Transparent
            var a = 0f
            var grow = 1f
            if (phase == VoidPhase.EXPLORE) {
                if (anomalyAt >= 0f && pt - anomalyAt < 2f) {
                    // the first flicker: it darkens and a reddish patch shows near its lower edge
                    val k = 1f - abs((pt - anomalyAt) - 1f)
                    drawCircle(Color(0xFF3A1410).copy(alpha = 0.45f * k), starR0 * 0.98f, c)
                    drawCircle(Color(0xFFC94B36).copy(alpha = 0.5f * k), starR0 * 0.28f, c + Offset(0f, starR0 * 0.55f))
                }
                if (anomaly2On && pt > 75f) {
                    val k = ((pt - 75f) / 45f).coerceIn(0f, 1f)
                    tint = Color(0xFFF5A04B)
                    a = 0.18f + 0.22f * k * (0.6f + 0.4f * sin(pt * 3f))
                    grow = 1f + 0.03f * sin(pt * 5f) * k
                    // the planets lose some of their light
                    for (p in u.planets) drawCircle(Color(0xFF050710).copy(alpha = 0.35f * k * (0.5f + 0.5f * sin(pt * 2f))), p.radius * 1.02f, p.pos)
                }
            } else if (phase == VoidPhase.CUT1) {
                val k = (pt / 45f).coerceIn(0f, 1f)
                tint = if (pt < 20f) Color(0xFFF5A04B) else Color(0xFFC94B36)
                val beat = max(0f, sin(pt * 0.63f * 2f))
                a = 0.25f + 0.25f * k
                grow = 1f + 0.05f * beat
                // a faint ripple across the nearby space
                drawCircle(Color(0xFFC94B36).copy(alpha = 0.18f * beat), starR0 * (1.3f + 1.6f * beat), c, style = Stroke(3f * px))
            } else {
                val k = (pt / 30f).coerceIn(0f, 1f)
                val rate = 2.2f + 7f * k
                val s = 0.5f + 0.5f * sin(pt * rate)
                tint = Color(0xFFC94B36)
                a = 0.35f + 0.3f * k
                grow = 1f + (0.08f + 0.12f * k) * s
                drawCircle(Color(0xFFFFE0A0).copy(alpha = 0.05f + 0.25f * k * s), starR0 * 0.45f, c)
            }
            if (a > 0f) {
                drawCircle(tint.copy(alpha = a), starR0 * grow, c)
                drawCircle(tint.copy(alpha = min(1f, a * 1.6f)), starR0 * (grow + 0.05f), c, style = Stroke(4f * px))
            }
        }
        // ---- the collapse: a crimson disc that shrinks around a white heart
        if (starDark) {
            val k = (pt / 20f).coerceIn(0f, 1f)
            val r = starR0 * (1f - 0.45f * min(1f, pt / 5f))
            drawCircle(Color(0xFF8F302B), r, c)
            drawCircle(Color(0xFFC94B36).copy(alpha = 0.6f), r * 1.1f, c, style = Stroke(5f * px))
            val core = if (pt < 10f) 0.15f + 0.1f * (pt / 10f) else 0.25f + 0.75f * ((pt - 10f) / 10f)
            drawCircle(Color.White.copy(alpha = min(1f, core + k * 0.2f)), r * (0.18f + 0.5f * k), c)
        }
        // ---- the shockwave rings
        if (phase >= VoidPhase.BLAST && phase <= VoidPhase.GATHER && shockR < BOUNDARY * 1.05f) {
            for (ring in 0 until 3) {
                val rr = shockR - ring * 9000f
                if (rr > starR0) {
                    val fade = (1f - rr / BOUNDARY).coerceIn(0f, 1f)
                    drawCircle(Color.White.copy(alpha = 0.8f * fade), rr, c, style = Stroke((6f - ring * 1.5f) * px))
                    drawCircle(Color(0xFFFFB74D).copy(alpha = 0.4f * fade), rr * 0.97f, c, style = Stroke(14f * px))
                }
            }
        }
        // ---- planets under attack
        for (ps in pstates) {
            if (ps.breakAt >= 0f && !ps.gone) {
                val k = ((pt - ps.breakAt) / 12f).coerceIn(0f, 1f).let { if (phase > VoidPhase.BLAST) 1f else it }
                drawCircle(Color(0xFFFFB74D).copy(alpha = 0.25f + 0.4f * k), ps.planet.radius * 1.06f, ps.planet.pos, style = Stroke(2.2f * px))
                drawCircle(Color(0xFFFFE0A0).copy(alpha = 0.22f * k), ps.planet.radius, ps.planet.pos)
            }
        }
        for (p in particles) {
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawCircle(p.color.copy(alpha = a), p.size * px, Offset(p.x, p.y))
        }
        // ---- the edge of the universe, lit by what is left of the star
        if (phase >= VoidPhase.BLAST && phase <= VoidPhase.FAILURE) {
            var glow = boundaryGlow
            if (phase >= VoidPhase.GATHER) {
                var arrived = 0
                for (d in dust) if (pt >= d.delay + d.duration || phase > VoidPhase.GATHER) arrived++
                glow = max(glow, 0.25f + 0.6f * arrived / max(1, dust.size))
            }
            val col = if (phase <= VoidPhase.VANISH) Color(0xFFF08B45) else Color(0xFFDFA15D)
            if (phase == VoidPhase.FAILURE) {
                // the glow breaks into pieces that flicker out one by one
                for (s in 0 until 16) {
                    if (flick(1f - ringBroken * (0.5f + 0.5f * ((s * 7) % 16) / 16f))) {
                        val a0 = s * 6.2831855f / 16f
                        val path = Path()
                        var first = true
                        for (q in 0..8) {
                            val a = a0 + q * (6.2831855f / 16f) / 8f
                            val x = cos(a) * BOUNDARY
                            val y = sin(a) * BOUNDARY
                            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                        }
                        drawPath(path, col.copy(alpha = 0.75f), style = Stroke(5f * px))
                    }
                }
            } else {
                drawCircle(col.copy(alpha = 0.7f * glow), BOUNDARY, c, style = Stroke(5f * px))
                drawCircle(col.copy(alpha = 0.22f * glow), BOUNDARY - 120f, c, style = Stroke(26f * px))
            }
        }
        // ---- the stardust
        if (dustOn) {
            val t = u.now
            for ((n, d) in dust.withIndex()) {
                // each fragment shows or hides at random while the dust flickers out
                if (dustP < 1f && rnd.nextFloat() > dustP) continue
                var x: Float
                var y: Float
                var a: Float
                if (phase < VoidPhase.GATHER) {
                    // the star's fragments drift apart and dim
                    val k = (if (phase == VoidPhase.DUST) pt / 30f else 0f).coerceIn(0f, 1f)
                    val ang = d.angle0 + k * d.curve
                    val rr = d.r0 + k * 6000f * d.seed
                    x = cos(ang) * rr
                    y = sin(ang) * rr
                    a = 0.9f * (1f - 0.35f * k)
                } else {
                    val s0 = ((pt - d.delay) / d.duration).coerceIn(0f, 1f)
                    val s = if (phase > VoidPhase.GATHER) 1f else s0 * s0 * (3f - 2f * s0)
                    val ang = d.angle0 + d.curve * (1f - s) + (if (s >= 1f) d.drift * (t) else 0f)
                    val rr = d.r0 + (d.target - d.r0) * s
                    x = cos(ang) * rr
                    y = sin(ang) * rr
                    a = 0.95f
                    if (d.absorbAt >= 0f && phase == VoidPhase.COLLECT) {
                        val k = ((pt - d.absorbAt) / 5f).coerceIn(0f, 1f)
                        x += (u.ship.pos.x - x) * k
                        y += (u.ship.pos.y - y) * k
                        if (k >= 0.98f) continue
                    } else if (d.absorbAt >= 0f && phase > VoidPhase.COLLECT) {
                        continue
                    }
                }
                val tw = 0.55f + 0.45f * sin(t * 2.2f + d.seed * 20f + n)
                drawCircle(d.color.copy(alpha = 0.2f * a * tw), d.size * 3.4f * px, Offset(x, y))
                drawCircle(d.color.copy(alpha = a * tw), d.size * px, Offset(x, y))
            }
        }
        // ---- the sample the ship has just taken: a small golden glow
        if (phase == VoidPhase.COLLECT && collectGlow > 0f) {
            val sp = u.ship.pos
            drawCircle(Color(0xFF9FD8FF).copy(alpha = 0.35f), 16f * px, sp, style = Stroke(1.5f * px))
            drawCircle(Color(0xFFF6D59A).copy(alpha = 0.3f * collectGlow), 24f * px * collectGlow, sp)
            drawCircle(Color(0xFFFFF0D2).copy(alpha = collectGlow), 6f * px * collectGlow, sp)
        }
    }

    // ------------------------------------------------------------------ drawing: the screen

    /** Screen-space extras: flashes, the arrival, the poem's particles, the ship's silhouette. */
    fun drawScreen(scope: DrawScope) {
        if (mode != Mode.VOID) return
        with(scope) {
            if (flash > 0f) drawRect(Color.White.copy(alpha = flash.coerceIn(0f, 1f)), Offset.Zero, size)
            if (redGlow > 0f) drawRect(Color(0xFFFF3B30).copy(alpha = 0.10f * redGlow), Offset.Zero, size)
            // the arrival: the black screen flickers away
            if (phase == VoidPhase.ARRIVAL && pt < 6f) {
                if (rnd.nextFloat() > pt / 6f) drawRect(Color.Black, Offset.Zero, size)
            }
            // a last flicker into the void when everything fails
            if (phase == VoidPhase.POEM) {
                val minD = min(size.width, size.height)
                val t = Poem.elapsed
                // the faint memory of the edge of the universe (150 to 270 seconds in)
                if (t in 150f..270f) {
                    val chance =
                        when {
                            t < 180f -> (t - 150f) / 30f * 0.35f
                            t < 210f -> 0.35f - (t - 180f) / 30f * 0.2f
                            t < 240f -> 0.15f * (1f - (t - 210f) / 30f)
                            else -> 0.04f * (1f - (t - 240f) / 30f)
                        }
                    val segments = 24
                    for (s in 0 until segments) {
                        val broken = if (t > 210f) (t - 210f) / 60f else 0f
                        if (rnd.nextFloat() < chance * (1f - broken * ((s * 5) % segments) / segments)) {
                            val a0 = s * 6.2831855f / segments
                            val path = Path()
                            for (q in 0..4) {
                                val a = a0 + q * (6.2831855f / segments) / 4f
                                val x = size.width / 2f + cos(a) * minD * 0.37f
                                val y = size.height / 2f + sin(a) * minD * 0.37f
                                if (q == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, Color(0xFFDFA15D).copy(alpha = 0.6f), style = Stroke(2f))
                        }
                    }
                }
                for (p in poemParticles) {
                    val age = t - p.born
                    // flickers out at the end of its life instead of fading
                    val show = if (p.life - age < 3f) rnd.nextFloat() < (p.life - age) / 3f else true
                    if (show) {
                        val x = p.x * size.width
                        val y = p.y * size.height
                        drawCircle(p.color.copy(alpha = 0.22f), p.size * 4f, Offset(x, y))
                        drawCircle(p.color.copy(alpha = 0.9f), p.size, Offset(x, y))
                    }
                }
            }
            // the ship, returning as a faint silhouette
            if (phase == VoidPhase.REVEAL && pt > 0f) {
                val on = rnd.nextFloat() < (pt / 10f).coerceIn(0f, 1f)
                if (on) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val s = min(size.width, size.height) * 0.025f
                    val path = Path()
                    path.moveTo(cx, cy - s * 1.4f)
                    path.lineTo(cx + s, cy + s)
                    path.lineTo(cx, cy + s * 0.5f)
                    path.lineTo(cx - s, cy + s)
                    path.close()
                    drawPath(path, Color(0xFFF2F2F7).copy(alpha = 0.22f))
                }
            }
        }
    }

    private const val HEAVEN_RING = 11_000f

    private fun DrawScope.drawHeaven(u: Universe, px: Float) {
        val beat = 0.5f + 0.5f * sin(u.now * 3.5f).let { max(0f, it) }
        val centre = u.star.pos
        val pos = Array(6) { heavenPos(u, it) }
        val col = Array(6) { heavenColors[heavenIndex[it]] }
        val yellow = heavenColors[2]

        for (k in 0 until 12) {
            val from: Offset
            val to: Offset
            val c1: Color
            val c2: Color
            if (k < 6) {
                from = pos[k]
                to = pos[(k + 1) % 6]
                c1 = col[k]
                c2 = col[(k + 1) % 6]
            } else {
                from = pos[k - 6]
                to = centre
                c1 = col[k - 6]
                c2 = yellow
            }
            drawLine(
                brush = Brush.linearGradient(listOf(c1.copy(alpha = 0.55f), c2.copy(alpha = 0.55f)), start = from, end = to),
                start = from,
                end = to,
                strokeWidth = (2f + 1.6f * beat) * px,
            )
            val f = pulse[k]
            val at = Offset(from.x + (to.x - from.x) * f, from.y + (to.y - from.y) * f)
            val pc = Color(c1.red + (c2.red - c1.red) * f, c1.green + (c2.green - c1.green) * f, c1.blue + (c2.blue - c1.blue) * f, 1f)
            drawCircle(pc.copy(alpha = 0.25f), 11f * px, at)
            drawCircle(Color.White.copy(alpha = 0.9f), 3.2f * px, at)
        }
        for (i in 0 until 6) {
            drawCircle(col[i].copy(alpha = 0.18f + 0.12f * beat), 1250f, pos[i])
            drawCircle(col[i], 520f, pos[i])
            drawCircle(Color.White.copy(alpha = 0.5f), 200f, pos[i])
        }
        drawCircle(yellow.copy(alpha = 0.14f + 0.1f * beat), 4600f, centre)
    }

    // ------------------------------------------------------------------ helpers for the overlay

    internal fun panelNow(): Pair<Panel, Float>? = panel?.let { it to panelClock }
    internal fun phaseNow(): Int = phase
}

// ------------------------------------------------------------------ the overlays

private fun panelColor(kind: Int): Color =
    when (kind) {
        0 -> Color(0xFFFFB74D)
        1 -> Color(0xFFFFB74D)
        2 -> Color(0xFF34A853)
        3 -> Color(0xFFFF4D4D)
        else -> Color(0xFFF2F2F7)
    }

/** A tiny hash that makes lines flicker on and off in a repeatable way. */
private fun flickerOn(alpha: Float, i: Int, t: Float): Boolean {
    if (alpha >= 0.999f) return true
    if (alpha <= 0.001f) return false
    var n = i * 374761393 + (t * 24f).toInt() * 668265263
    n = (n xor (n ushr 13)) * 1274126177
    return ((n xor (n ushr 16)) and 0xFFFF) / 65535f < alpha
}

@Composable
fun DirectorOverlay() {
    if (Director.mode == Director.Mode.NORMAL) return
    BackHandler(enabled = Director.poemActive) {} // the poem can not be skipped with Back

    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeContent)) {
        // scanner readings, KASA's transmission, objectives and the final status
        val pn = Director.panelNow()
        AnimatedVisibility(
            visible = pn != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp, start = 12.dp, end = 12.dp),
            enter = flickerFadeIn,
        ) {
            if (pn != null) {
                val (p, clock) = pn
                val color = panelColor(p.kind)
                val shape = RoundedCornerShape(14.dp)
                // typed text: the characters appear one after another
                var budget = if (p.typed) (clock * 38f).toInt() else Int.MAX_VALUE
                Column(modifier = Modifier.fillMaxWidth().clip(shape).background(Colors.Eigengrau).border(1.5.dp, color, shape).padding(14.dp)) {
                    Text(style = MenuText.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = color, text = p.title)
                    Spacer(Modifier.height(6.dp))
                    for (line in p.lines) {
                        val take = min(line.length, max(0, budget))
                        budget -= line.length
                        if (take > 0) {
                            Text(style = MenuText.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Colors.Console, text = line.substring(0, take))
                        }
                    }
                }
            }
        }

        // COLLECT, only next to the residue
        if (Director.collectReady) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)) {
                ConsoleButton(
                    textStyle = MenuText,
                    color = Colors.Eigengrau,
                    bgColor = Color(0xFFF6D59A),
                    borderColor = Color(0xFFF6D59A),
                    text = "COLLECT STELLAR RESIDUE",
                ) {
                    Director.collect()
                }
            }
        }

        // the poem: lines flicker in and out, they do not fade
        if (Director.poemActive) {
            val lines = Poem.current()
            if (lines != null) {
                @Suppress("UNUSED_VARIABLE") val clock = Poem.t
                Column(
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    lines.forEachIndexed { i, line ->
                        val a = Poem.lineAlpha(i)
                        Text(
                            modifier = Modifier.alpha(if (flickerOn(a, i, Poem.t)) 1f else 0f).padding(vertical = 5.dp),
                            style =
                                MenuText.copy(
                                    fontSize = if (Poem.isTitle()) 22.sp else 17.sp,
                                    lineHeight = if (Poem.isTitle()) 28.sp else 25.sp,
                                    fontWeight = if (Poem.isTitle()) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                ),
                            color = Color.White,
                            text = line,
                        )
                    }
                }
            }
        }

        // SKIP: only once the poem has been finished, in the slot where AUTO / REVEAL normally sits
        if (Director.skipAvailable) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = if (Director.hudStage < 2) 70.dp else 6.dp)) {
                ConsoleButton(
                    textStyle = MenuText,
                    color = Colors.Console,
                    bgColor = Color.Transparent,
                    borderColor = Colors.Console,
                    text = "SKIP",
                ) {
                    Director.skip()
                }
            }
        }
    }
}
