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
import android.content.SharedPreferences
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.GregorianCalendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

// ---------------------------------------------------------------------------------------------
// Achievements: definitions, persistence, the per-frame tracker, and the unlock banner.
// ---------------------------------------------------------------------------------------------

/** True while the game is in front of the user. The Reference timer (RTFM) uses it. */
object AppState {
    var foreground by mutableStateOf(true)
}

class AchDef(
    val id: String,
    val title: String,
    /** Instructions or an ambiguous hint: one text for every achievement. */
    val text: String,
    /** Earnable even in worlds where mods were used (world-identity and menu achievements). */
    val always: Boolean = false,
    val earnable: Boolean = true,
    /** Can only be earned by using the mod menu (shown as "requires cheating" when mods are off). */
    val needsMods: Boolean = false,
    /** The last tile of the list (Perfectionist): set apart by a line and its own colour. */
    val bonus: Boolean = false,
)

val ALL_ACHIEVEMENTS: List<AchDef> =
    listOf(
        AchDef("first_contact", "First Contact", "explore your first ever planet."),
        AchDef("cartographer", "Cartographer", "explore an entire system."),
        AchDef("full_send", "Hold On To Something", "get bery very fast."),
        AchDef("orbiter", "Orbiter", "welp become a satellite."),
        AchDef("sunbather", "Sunbather", "the stars are hot but not enough to vaporize our vehicle."),
        AchDef("edge_lord", "Edge Lord", "spoiler: the universe is not infinite.", always = true),
        AchDef("time_traveler", "Time Traveler", "burp, I ate the instructions for this one.", always = true),
        AchDef("soft_touch", "Soft Touch", "land peacefully, else you'll hurt the fauna."),
        AchDef("sprint_finish", "Sprint Finish", "speedrun. but not on a small (<3 planets) system. and hurry up, the timer doesn't wait."),
        AchDef("slingshot", "Slingshot", "who knew they use gravity to save fuel."),
        AchDef("uno_planeta", "Uno-Planeta", "awh what a smol n lonely system.", always = true),
        AchDef("creator", "Creator", "become the creator herself.", earnable = false),
        AchDef("redacted", "[REDACTED]", "Some jobs are not for your eyes."),
        AchDef("icecream", "IceCream", "ooo it's cool star.", always = true),
        AchDef("spoiler_alert", "Spoiler Alert", "Peek but still explore.", always = true),
        AchDef("purist", "Purist", "welp just don't cheat."),
        AchDef("lucky_7", "Lucky 7", "preserves the heaven.", always = true),
        AchDef("rtfm", "RTFM", "Have you read the math?", always = true),
        AchDef("editor", "Editor", "Rewrite the rules of the book.", always = true),
        AchDef("archivist", "Archivist", "hmm you may have to perform a secret action for this on.", always = true),
        AchDef("y2k", "Y2K", "the day the computers were supposed to get messed up.", always = true),
        AchDef("leap_of_faith", "Leap of Faith", "A day that only exists sometimes.", always = true),
        AchDef("cosmic_joke", "Cosmic Joke", "a number close to asty's heart.", always = true),
        AchDef("triple_transit", "Triple Transit", "keep trying, xytranium doesn't melt so easily.", always = true),
        AchDef("bullseye", "Bullseye", "Straight in, dead on."),
        AchDef("crash_test", "Crash Test", "the xytranium doesn't get a scratch."),
        AchDef("pinball_wizard", "Pinball Wizard", "Ping, pong, ping, ping."),
        AchDef("houston", "Shady, We Have a Problem", "The first time does not always go well."),
        AchDef("hold_my_controller", "Hold My Controller", "totally no need to care about the fuel levels. cuz there aren't any."),
        AchDef("overqualified", "Overqualified", "peek then sleek.", always = true),
        AchDef("autopilot_passenger", "Autopilot Passenger", "let the computer control the steering."),
        AchDef("gps_duh", "i use it as my GPS duh", "The blue line is more of a suggestion."),
        AchDef("name_drop", "Name Drop", "Collect rare system names.", always = true),
        AchDef("stuck_landing", "Stuck Landing Bonus", "Up and back down in a blink."),
        AchDef("developer_mode", "Developer Mode", "sometimes we all get scammed.", always = true),
        AchDef("petridish", "Petridish", "Step back and look at it all.", always = true),
        AchDef("happy_birthday", "Happy Birthday", "One day a year, the sky celebrates.", always = true),
        AchDef("cheater", "Cheater", "Some switches have consequences.", always = true, needsMods = true),
        AchDef("void", "Void", "Everything ends with nothing.", always = true),
        AchDef("when_started", "When It All Started", "the very first day.", always = true),
        AchDef("when_ends", "When It All Ends", "And the very last.", always = true),
        AchDef("small_universe", "What a Small Universe", "paper.io ninja technique"),
        AchDef("first_dawn", "The First Dawn", "In the beginning there was one.", always = true),
        AchDef("world_not_found", "World Not Found", "ERROR 404 achievement description not found.", always = true),
        AchDef("end_to_ending", "The End to Ending", "Stay until the last word.", earnable = false),
        AchDef("cat_astrophe", "Cat-astrophe", "Mind the cats on the day that rarely comes.", earnable = false),
        AchDef("full_stack", "Full Stack", "world to the brim.", earnable = false),
        AchDef("multitasker", "Multitasker", "the autopilot should never rest."),
        AchDef("tweak_freak", "Tweak Freak", "mess with the toggles.", always = true),
        // always the very last tile
        AchDef("perfectionist", "Perfectionist", "Unlock everything.", always = true, bonus = true),
    )

/** How many achievements there are (Perfectionist is not counted in its own total). */
val ACH_TOTAL: Int = ALL_ACHIEVEMENTS.count { !it.bonus }

private val BY_ID: Map<String, AchDef> = ALL_ACHIEVEMENTS.associateBy { it.id }
private const val TWO_PI = 6.2831855f

/** The regular Settings toggles that Tweak Freak wants touched at least once. */
private val TWEAK_KEYS = setOf("grid", "hud", "awake", "notification")

/** What the page says on a locked tile about the mod menu. */
enum class ModNote {
    NONE,
    DISABLED,
    STILL_EARNABLE,
    NEEDS_CHEATING,
}

object Achievements {
    private var prefs: SharedPreferences? = null

    /** id -> time unlocked (used for the DEFAULT sort; never shown). */
    val unlocked = mutableStateMapOf<String, Long>()

    /** Counters. Some are kept only for future stats (bounces, planets, launches, worlds). */
    val counters = mutableStateMapOf<String, Int>()

    /** Waiting to be shown by the banner. */
    val queue = mutableStateListOf<AchDef>()

    /** True while the current world has achievements disabled (mods were used in it). */
    var achOff by mutableStateOf(false)
        private set

    /** Set by the activity: is the ship notification really showing right now? */
    var notificationUp: () -> Boolean = { false }

    private val randomSeeds = HashSet<Long>()
    private val worldsPlayed = HashSet<Long>()
    private val rareBases = HashSet<String>()
    private val touchedSettings = HashSet<String>()
    private var multitaskMs = 0L

    private var world: WorldState? = null

    // ------------------------------------------------------------------ persistence

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("landroid_achievements", Context.MODE_PRIVATE)
        prefs = p
        try {
            p.getString("unlocked", null)?.let { raw ->
                val o = JSONObject(raw)
                o.keys().forEach { k -> unlocked[k] = o.getLong(k) }
            }
            p.getString("counters", null)?.let { raw ->
                val o = JSONObject(raw)
                o.keys().forEach { k -> counters[k] = o.getInt(k) }
            }
            p.getString("randomSeeds", null)?.let { raw -> readLongs(raw, randomSeeds) }
            p.getString("worldsPlayed", null)?.let { raw -> readLongs(raw, worldsPlayed) }
            p.getString("rareBases", null)?.let { raw -> readStrings(raw, rareBases) }
            p.getString("touched", null)?.let { raw -> readStrings(raw, touchedSettings) }
            multitaskMs = p.getLong("multitaskMs", 0L)
        } catch (e: Exception) {
            // a damaged save must never stop the game
        }
    }

    private fun readLongs(raw: String, into: MutableSet<Long>) {
        val a = JSONArray(raw)
        for (i in 0 until a.length()) into.add(a.getLong(i))
    }

    private fun readStrings(raw: String, into: MutableSet<String>) {
        val a = JSONArray(raw)
        for (i in 0 until a.length()) into.add(a.getString(i))
    }

    private fun persist() {
        val p = prefs ?: return
        try {
            val u = JSONObject()
            unlocked.forEach { (k, v) -> u.put(k, v) }
            val c = JSONObject()
            counters.forEach { (k, v) -> c.put(k, v) }
            p.edit()
                .putString("unlocked", u.toString())
                .putString("counters", c.toString())
                .putString("randomSeeds", JSONArray(randomSeeds.toList()).toString())
                .putString("worldsPlayed", JSONArray(worldsPlayed.toList()).toString())
                .putString("rareBases", JSONArray(rareBases.toList()).toString())
                .putString("touched", JSONArray(touchedSettings.toList()).toString())
                .putLong("multitaskMs", multitaskMs)
                .apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    // ------------------------------------------------------------------ queries

    fun isUnlocked(id: String) = unlocked.containsKey(id)

    /** Unlocked achievements that still exist (old saves may hold ids that were removed). */
    val totalUnlocked: Int
        get() = ALL_ACHIEVEMENTS.count { !it.bonus && unlocked.containsKey(it.id) }

    /**
     * Progress for the few achievements that show a bar while locked: (now, goal), or null.
     * Unlocked achievements never show a bar.
     */
    fun progressOf(def: AchDef): Pair<Int, Int>? {
        if (isUnlocked(def.id)) return null
        return when (def.id) {
            "cartographer" -> world?.universe?.let { u -> u.planets.count { it.explored } to u.planets.size }
            "gps_duh" -> min(counters["gps"] ?: 0, 5) to 5
            "multitasker" -> min((multitaskMs / 60_000L).toInt(), 15) to 15
            "perfectionist" -> totalUnlocked to ACH_TOTAL
            else -> null
        }
    }

    /** The time Sprint Finish was won in, as shown in its art. */
    fun sprintSeconds(): Int = counters["sprint_s"] ?: 0

    /** The note a locked tile carries about mods (none on unlocked tiles). */
    fun modNote(def: AchDef): ModNote {
        if (isUnlocked(def.id) || !def.earnable) return ModNote.NONE
        return if (achOff || GameSettings.modMenu) {
            if (def.always) ModNote.STILL_EARNABLE else ModNote.DISABLED
        } else {
            if (def.needsMods) ModNote.NEEDS_CHEATING else ModNote.NONE
        }
    }

    // ------------------------------------------------------------------ unlocking

    private fun canEarn(def: AchDef) = !achOff || def.always

    fun unlock(id: String) {
        val def = BY_ID[id] ?: return
        if (!def.earnable || unlocked.containsKey(id) || !canEarn(def)) return
        unlocked[id] = System.currentTimeMillis()
        queue.add(def)
        persist()
        if (!def.bonus) checkBonus()
    }

    private fun checkBonus() {
        if (totalUnlocked >= ACH_TOTAL) unlock("perfectionist")
    }

    /** Counters only move in worlds where mods were not used. */
    private fun bump(key: String, by: Int = 1): Int {
        if (achOff) return counters[key] ?: 0
        val v = (counters[key] ?: 0) + by
        counters[key] = v
        persist()
        return v
    }

    fun setAchievementsOff(off: Boolean) {
        achOff = off
        world?.achOff = off
    }

    // ------------------------------------------------------------------ menu and settings events

    private var menuTaps = 0
    private var lastMenuTap = 0L
    private var menuToast: Toast? = null

    /** Seven quick taps on MENU: the "you are now a developer" joke. */
    fun menuButtonTapped(context: Context) {
        if (isUnlocked("developer_mode")) return
        val now = SystemClock.elapsedRealtime()
        menuTaps = if (now - lastMenuTap <= 1000L) menuTaps + 1 else 1
        lastMenuTap = now
        val message =
            when (menuTaps) {
                4 -> "You are now 3 steps away from being a developer."
                5 -> "You are now 2 steps away from being a developer."
                6 -> "You are now 1 step away from being a developer."
                7 -> "You have been officially made a fool!"
                else -> null
            }
        if (message != null) {
            menuToast?.cancel()
            menuToast = Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).also { it.show() }
        }
        if (menuTaps >= 7) {
            menuTaps = 0
            unlock("developer_mode")
        }
    }

    /** A regular Settings toggle was used. Tweak Freak wants every one of them touched. */
    fun settingTouched(key: String) {
        if (touchedSettings.add(key)) persist()
        if (touchedSettings.containsAll(TWEAK_KEYS)) unlock("tweak_freak")
    }

    fun referenceSavedWithEdits() = unlock("editor")

    fun referenceExported() = unlock("archivist")

    // ------------------------------------------------------------------ Multitasker (background time)

    private var inBackground = false
    private var backgroundLast = 0L

    /** The activity went to the background (or came back). The game loop sleeps there. */
    fun appBackgrounded(background: Boolean, u: Universe?) {
        if (background) {
            backgroundLast = SystemClock.elapsedRealtime()
            inBackground = true
        } else {
            if (inBackground) accountBackground(u)
            inBackground = false
        }
    }

    /** Called every few seconds from the activity while it is in the background. */
    fun backgroundTick(u: Universe) {
        if (inBackground) accountBackground(u)
    }

    private fun accountBackground(u: Universe?) {
        val now = SystemClock.elapsedRealtime()
        val delta = now - backgroundLast
        backgroundLast = now
        if (u == null || delta <= 0L) return
        val ws = world ?: return
        if (ws.universe !== u || achOff) return
        // autopilot on, game in the background, and the ship notification really up
        if (u.ship.autopilot?.enabled != true || !notificationUp()) return
        multitaskMs += delta
        persist()
        if (multitaskMs >= 15L * 60_000L) unlock("multitasker")
    }

    // ------------------------------------------------------------------ world start (load-time achievements)

    fun onWorldStart(u: Universe, achievementsOff: Boolean, plays: Int) {
        achOff = achievementsOff
        world = WorldState(u).also { it.achOff = achievementsOff }

        val seed = u.randomSeed

        // bookkeeping kept for the stats that are coming
        worldsPlayed.add(seed)
        counters["worlds"] = worldsPlayed.size
        if (seed < 10_000_000L) {
            randomSeeds.add(seed)
            counters["random_seeds"] = randomSeeds.size
        }
        counters["max_plays"] = max(counters["max_plays"] ?: 0, plays)

        // today's seed, for "any date other than today"
        val now = GregorianCalendar()
        val todaySeed =
            seedForDate(
                now.get(java.util.Calendar.YEAR),
                now.get(java.util.Calendar.MONTH),
                now.get(java.util.Calendar.DAY_OF_MONTH),
            )
        val isDate = seedToDateString(seed) != null
        val year = (seed / 10_000L).toInt()
        val month0 = ((seed / 100L) % 100L).toInt()
        val day = (seed % 100L).toInt()

        persist()

        if (isDate && seed != todaySeed) unlock("time_traveler")
        if (u.planets.size == 1) unlock("uno_planeta")

        // names
        if (u.namer.lastBaseName.contains("icecream", ignoreCase = true)) unlock("icecream")
        if (u.namer.lastBaseWasRare) {
            if (rareBases.add(u.namer.lastBaseName)) persist()
            if (rareBases.size >= 3) unlock("name_drop")
        }

        // seeds and dates
        if (seed == 7L) unlock("lucky_7")
        if (seed == 87L) unlock("cosmic_joke")
        if (seed == 0L) unlock("void")
        if (seed == 1L) unlock("first_dawn")
        if (seed == 404L) unlock("world_not_found")
        if (seed == seedForDate(2000, 0, 1)) unlock("y2k")
        if (seed == seedForDate(1000, 0, 1)) unlock("when_started")
        if (seed == seedForDate(9999, 11, 31)) unlock("when_ends")
        if (isDate && month0 == 8 && day == 25) unlock("happy_birthday")
        if (isDate && month0 == 1 && day == 29 && isLeap(year)) unlock("leap_of_faith")
    }

    private fun isLeap(y: Int) = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0

    // ------------------------------------------------------------------ gameplay events

    private fun isManual(u: Universe) = u.ship.autopilot?.enabled != true

    fun onReveal(u: Universe) {
        val ws = world ?: return
        if (ws.universe !== u) return
        ws.revealUsed = true
        if (u.planets.none { it.explored }) unlock("spoiler_alert")
    }

    /** AUTO was switched off. Doing it within 5 seconds of the blue line appearing is "GPS duh". */
    fun onAutopilotSwitchedOff(u: Universe) {
        val ws = world ?: return
        if (ws.universe !== u) return
        if (ws.gpsArmed && u.now - ws.gpsShownAt <= 5f) {
            if (bump("gps") >= 5) unlock("gps_duh")
        }
        ws.gpsArmed = false
        ws.gpsTarget = null
    }

    fun onLanded(u: Universe, planet: Planet, speed: Float, angleDiff: Float, activity: String) {
        val ws = world ?: return
        if (ws.universe !== u) return
        val manual = isManual(u)

        unlock("first_contact")
        ws.collisions++
        if (manual) ws.landingsManual++ else ws.landingsAuto++
        ws.orbitSum.clear()
        ws.orbitLast.clear()
        ws.slingActive = false

        // kept for the stats: planets explored in total (first landing per planet per world)
        if (ws.landedPlanets.add(planet) && !achOff) bump("planets")

        val planets = u.planets
        val all = planets.isNotEmpty() && planets.all { it.explored }
        if (all) unlock("cartographer")

        // landing style
        if (manual && ws.bouncesSinceApproach == 0) unlock("soft_touch")
        if (manual && angleDiff < 0.0873f) unlock("bullseye") // within 5 degrees
        if (manual && u.now - ws.launchTime <= 3f) unlock("stuck_landing")
        ws.bouncesSinceApproach = 0

        // systems with at least 3 planets
        if (planets.size >= 3) {
            if (all && u.now <= 600f && !isUnlocked("sprint_finish") && !achOff) {
                counters["sprint_s"] = u.now.toInt()
                persist()
            }
            if (all && u.now <= 600f) unlock("sprint_finish")
            if (all && !ws.revealUsed) unlock("purist")
            if (all && ws.landingsManual == 0) unlock("autopilot_passenger")
        }
        if (all && ws.revealUsed) unlock("overqualified")

        // landing jobs
        if (activity.equals("[REDACTED]", ignoreCase = true)) unlock("redacted")
    }

    fun onBounce(u: Universe, planet: Planet, speed: Float) {
        val ws = world ?: return
        if (ws.universe !== u) return

        // the very first thing that happens to you in this world is a bounce
        if (ws.collisions == 0) unlock("houston")
        ws.collisions++
        ws.bouncesSinceApproach++
        bump("bounces") // kept for the stats
        if (speed > 4000f) unlock("crash_test")

        ws.bounceTimes.addLast(u.now)
        while (ws.bounceTimes.isNotEmpty() && u.now - ws.bounceTimes.first() > 30f) ws.bounceTimes.removeFirst()
        if (ws.bounceTimes.size >= 10) unlock("pinball_wizard")
    }

    /** Called once per frame from the game loop. */
    fun tick(u: Universe) {
        val ws = world ?: return
        if (ws.universe !== u) return
        val dt = u.dt
        val ship = u.ship
        val manual = isManual(u)
        val landed = ship.landing != null

        // launches (kept for the stats)
        if (ws.wasLanded && !landed) {
            ws.launchTime = u.now
            ws.bouncesSinceApproach = 0
            bump("launches")
        }
        ws.wasLanded = landed

        // the blue autopilot line: note every time it latches onto a (new) target planet
        val ap = ship.autopilot
        val lineTarget = if (ap != null && ap.enabled && !landed) ap.target else null
        if (lineTarget != null) {
            if (ws.gpsTarget !== lineTarget) {
                ws.gpsTarget = lineTarget
                ws.gpsShownAt = u.now
                ws.gpsArmed = true
            }
        } else {
            ws.gpsTarget = null
            ws.gpsArmed = false
        }

        if (landed) {
            ws.thrustHeld = 0f
            return
        }

        val speed = ship.velocity.mag()

        // reach the speed limit (stock limit)
        if (manual && speed >= CRAFT_SPEED_LIMIT - 15f) unlock("full_send")

        // through the star
        if (ship.transit && !ws.inTransit) {
            ws.transits++
            unlock("sunbather")
            if (ws.transits >= 3) unlock("triple_transit")
        }
        ws.inTransit = ship.transit

        // edge of the universe
        val fromStar = ship.pos - u.star.pos
        val r = fromStar.mag()
        if (r >= UNIVERSE_RANGE - 120f) unlock("edge_lord")

        // thrust held without a break
        if (manual && ship.thrust != Vec2.Zero) {
            ws.thrustHeld += dt
            if (ws.thrustHeld >= 60f) unlock("hold_my_controller")
        } else {
            ws.thrustHeld = 0f
        }

        // a lap near the edge of the universe
        if (manual && r >= 0.85f * UNIVERSE_RANGE) {
            val ang = fromStar.angle()
            if (!ws.edgeValid) {
                ws.edgeValid = true
                ws.edgeLast = ang
                ws.edgeSum = 0f
            } else {
                ws.edgeSum += angleDelta(ang, ws.edgeLast)
                ws.edgeLast = ang
                if (abs(ws.edgeSum) >= TWO_PI) unlock("small_universe")
            }
        } else {
            ws.edgeValid = false
        }

        // planets: nearest surface, orbits, slingshot
        var nearAlt = Float.MAX_VALUE
        var nearest: Planet? = null
        for (p in u.planets) {
            val alt = (ship.pos - p.pos).mag() - p.radius
            if (alt < nearAlt) {
                nearAlt = alt
                nearest = p
            }
        }

        if (manual) {
            for (p in u.planets) {
                val rel = ship.pos - p.pos
                val alt = rel.mag() - p.radius
                if (alt < max(p.radius * 4f, 3000f)) {
                    val ang = rel.angle()
                    val last = ws.orbitLast[p]
                    if (last == null) {
                        ws.orbitLast[p] = ang
                        ws.orbitSum[p] = 0f
                    } else {
                        val sum = (ws.orbitSum[p] ?: 0f) + angleDelta(ang, last)
                        ws.orbitSum[p] = sum
                        ws.orbitLast[p] = ang
                        if (abs(sum) >= TWO_PI) unlock("orbiter")
                    }
                } else {
                    ws.orbitLast.remove(p)
                    ws.orbitSum.remove(p)
                }
            }
        }

        // slingshot: come out of a planet's pull (10,000 from its surface) at least 100 faster than
        // you went in, without touching the engines anywhere in between
        if (manual && nearest != null) {
            if (nearAlt < 10_000f) {
                if (!ws.slingActive) {
                    ws.slingActive = true
                    ws.slingEntry = speed
                    ws.slingBroken = false
                }
                if (ship.thrust != Vec2.Zero) ws.slingBroken = true
            } else if (ws.slingActive) {
                ws.slingActive = false
                if (!ws.slingBroken && speed - ws.slingEntry >= 100f) unlock("slingshot")
            }
        } else if (ws.slingActive) {
            ws.slingActive = false
        }
    }

    private fun angleDelta(a: Float, b: Float): Float {
        var d = a - b
        while (d > PIf) d -= TWO_PI
        while (d < -PIf) d += TWO_PI
        return d
    }

    private class WorldState(val universe: Universe) {
        var achOff = false
        var wasLanded = universe.ship.landing != null
        var launchTime = -1000f
        var bouncesSinceApproach = 0
        var collisions = 0
        val bounceTimes = ArrayDeque<Float>()
        var transits = 0
        var inTransit = false
        var thrustHeld = 0f
        var revealUsed = false
        var landingsAuto = 0
        var landingsManual = 0
        val landedPlanets = HashSet<Planet>()
        val orbitSum = HashMap<Planet, Float>()
        val orbitLast = HashMap<Planet, Float>()
        var slingActive = false
        var slingEntry = 0f
        var slingBroken = false
        var edgeSum = 0f
        var edgeLast = 0f
        var edgeValid = false
        var gpsTarget: Planet? = null
        var gpsShownAt = 0f
        var gpsArmed = false
    }
}

// ---------------------------------------------------------------------------------------------
// The unlock banner: drops in from the top with the menu's flicker, shows the real title.
// ---------------------------------------------------------------------------------------------

/** Cyan for every achievement; Perfectionist, set apart at the end of the list, is mint. */
internal fun sectionColor(def: AchDef) =
    if (def.bonus) Color(0xFFE8F5E9) else HiddenCyan

@Composable
fun AchievementBanner() {
    val current = Achievements.queue.firstOrNull()
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(current) {
        if (current != null) {
            shown = true
            delay(3600)
            shown = false
            delay(350)
            if (Achievements.queue.isNotEmpty()) Achievements.queue.removeAt(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeContent)) {
        AnimatedVisibility(
            visible = shown && current != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
            enter = expandVertically() + flickerFadeIn,
            exit = shrinkVertically() + fadeOut(),
        ) {
            if (current != null) {
                val color = sectionColor(current)
                Row(
                    modifier =
                        Modifier.widthIn(max = 340.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .background(Colors.Eigengrau)
                            .border(1.5.dp, color, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(46.dp)
                            .border(1.dp, color, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    ) {
                        AchievementArt(
                            id = current.id,
                            unlocked = true,
                            modifier = Modifier.fillMaxSize().padding(5.dp),
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            style = MenuText.copy(fontSize = 10.sp),
                            color = Colors.Eigengrau4,
                            text = "ACHIEVEMENT UNLOCKED",
                        )
                        Text(
                            style = MenuText.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = color,
                            text = current.title.uppercase(),
                        )
                    }
                }
            }
        }
    }
}
