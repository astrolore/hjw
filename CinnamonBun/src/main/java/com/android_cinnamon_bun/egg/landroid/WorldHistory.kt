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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// MOD: seed helpers
//
// A world is fully determined by its seed. Date worlds use the same formula the original
// "daily" world used (year * 10000 + zeroBasedMonth * 100 + day), so picking today's date in the
// calendar gives you exactly the world everybody gets by default today.
// Random worlds use seeds below 10,000,000, which can never collide with a date seed
// (the earliest selectable date, year 1000, is already 10,000,000 or more).
// ---------------------------------------------------------------------------------------------

const val MIN_CALENDAR_YEAR = 1000
const val MAX_CALENDAR_YEAR = 9999
private const val DATE_SEED_FLOOR = 10_000_000L

/** [month0] is zero-based (January = 0), matching [java.util.Calendar.MONTH]. */
fun seedForDate(year: Int, month0: Int, day: Int): Long =
    year * 10_000L + month0 * 100L + day

/** A fresh random world seed (always below 10,000,000, so it is never a date seed). */
fun newRandomWorldSeed(): Long = Random.Default.nextLong(1L, DATE_SEED_FLOOR)

/** "2026-10-26" if [seed] came from a calendar date, null if it is a random-world seed. */
fun seedToDateString(seed: Long): String? {
    if (seed < DATE_SEED_FLOOR) return null
    val year = seed / 10_000L
    val month0 = (seed / 100L) % 100L
    val day = seed % 100L
    if (month0 !in 0L..11L || day !in 1L..31L) return null
    return "%04d-%02d-%02d".format(year, month0 + 1, day)
}

// ---------------------------------------------------------------------------------------------
// MOD: world history
// ---------------------------------------------------------------------------------------------

data class WorldRecord(
    val seed: Long,
    val name: String,
    val planets: Int,
    val starClass: String,
    /** Most planets landed on so far in any visit to this world. */
    val explored: Int,
    val firstPlayed: Long,
    val lastPlayed: Long,
    val plays: Int,
    /** MOD: true once mods were used in this world; achievements stay off for it. */
    val achDisabled: Boolean = false,
) {
    val dateString: String?
        get() = seedToDateString(seed)

    fun lastPlayedString(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(lastPlayed))
}

/** Persists every world you've played, keyed by seed, in plain SharedPreferences. */
object WorldHistory {
    private const val PREFS = "landroid_world_history"
    private const val KEY = "worlds"
    private const val MAX_RECORDS = 500

    /** All played worlds, most recently played first. */
    fun load(context: Context): List<WorldRecord> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length())
                .map { arr.getJSONObject(it) }
                .map {
                    WorldRecord(
                        seed = it.getLong("seed"),
                        name = it.getString("name"),
                        planets = it.getInt("planets"),
                        starClass = it.optString("starClass", "?"),
                        explored = it.optInt("explored", 0),
                        firstPlayed = it.optLong("firstPlayed", 0L),
                        lastPlayed = it.optLong("lastPlayed", 0L),
                        plays = it.optInt("plays", 1),
                        achDisabled = it.optBoolean("achDisabled", false),
                    )
                }
                .sortedByDescending { it.lastPlayed }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save(context: Context, list: List<WorldRecord>) {
        val arr = JSONArray()
        list.sortedByDescending { it.lastPlayed }.take(MAX_RECORDS).forEach {
            arr.put(
                JSONObject()
                    .put("seed", it.seed)
                    .put("name", it.name)
                    .put("planets", it.planets)
                    .put("starClass", it.starClass)
                    .put("explored", it.explored)
                    .put("firstPlayed", it.firstPlayed)
                    .put("lastPlayed", it.lastPlayed)
                    .put("plays", it.plays)
                    .put("achDisabled", it.achDisabled)
            )
        }
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .apply()
    }

    /** Call when a world is started (or restarted). Adds it, or bumps its play count. */
    fun recordPlay(context: Context, universe: Universe) {
        val now = System.currentTimeMillis()
        val all = load(context).toMutableList()
        val idx = all.indexOfFirst { it.seed == universe.randomSeed }
        if (idx >= 0) {
            val old = all[idx]
            all[idx] = old.copy(lastPlayed = now, plays = old.plays + 1)
        } else {
            all.add(
                WorldRecord(
                    seed = universe.randomSeed,
                    name = universe.star.name,
                    planets = universe.planets.size,
                    starClass = universe.star.cls.name,
                    explored = 0,
                    firstPlayed = now,
                    lastPlayed = now,
                    plays = 1,
                )
            )
        }
        save(context, all)
    }

    /** Call when the number of landed-on planets may have changed. Keeps the best count. */
    fun updateProgress(context: Context, universe: Universe) {
        val explored = universe.planets.count { it.explored }
        val all = load(context).toMutableList()
        val idx = all.indexOfFirst { it.seed == universe.randomSeed }
        if (idx < 0 || explored <= all[idx].explored) return
        all[idx] = all[idx].copy(explored = explored)
        save(context, all)
    }

    fun isAchievementsDisabled(context: Context, seed: Long): Boolean =
        load(context).any { it.seed == seed && it.achDisabled }

    /** Marks a world as achievements-disabled (mods were used in it). Permanent until [clear]. */
    fun markAchievementsDisabled(context: Context, seed: Long) {
        val all = load(context).toMutableList()
        val idx = all.indexOfFirst { it.seed == seed }
        if (idx < 0 || all[idx].achDisabled) return
        all[idx] = all[idx].copy(achDisabled = true)
        save(context, all)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
