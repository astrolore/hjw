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

import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// Planet names. Every planet gets its own name instead of "<star> 1, 2, 3". A name is built from
// one of several recipes (a made-up word, a word from a pool, a prefix or a suffix, a number or a
// catalogue code, a delimiter) so that there are millions of different names. The generator has
// its own random numbers, so it never changes anything else about a world.
// ---------------------------------------------------------------------------------------------

object PlanetNames {
    private val onsets =
        arrayOf(
            "b", "d", "f", "g", "h", "k", "l", "m", "n", "p", "r", "s", "t", "v", "z", "w", "j", "br", "dr", "gr", "kr", "pr",
            "tr", "st", "sl", "sh", "th", "ch", "kl", "fl", "gl", "pl", "vr", "zh", "qu", "sk", "sn", "cr", "fr", "mn",
        )
    private val vowels = arrayOf("a", "e", "i", "o", "u", "a", "e", "i", "o", "ae", "ia", "io", "ei", "ou", "au", "y", "ee", "oo")
    private val codas = arrayOf("", "", "", "", "n", "r", "l", "s", "x", "th", "nd", "rn", "k", "m", "ph", "z", "rd", "lt", "sk")
    private val endings = arrayOf("a", "ia", "os", "us", "on", "is", "ar", "ion", "eth", "ara", "ix", "um", "ena", "or", "ys", "ael", "ine", "oth")

    private val pool =
        arrayOf(
            "Aurelia", "Borealis", "Calypso", "Dione", "Elara", "Fenwick", "Galatea", "Halcyon", "Iolite", "Juniper", "Kestrel", "Lumen",
            "Marrow", "Nyx", "Ondine", "Perihelion", "Quill", "Rhea", "Sable", "Tethys", "Umbra", "Vesper", "Wren", "Xanadu", "Yarrow",
            "Zephyr", "Amber", "Basalt", "Cinder", "Dusk", "Ember", "Flint", "Garnet", "Harbor", "Indigo", "Jasper", "Kelp", "Lantern",
            "Moss", "Nacre", "Obsidian", "Pebble", "Quartz", "Rust", "Sapphire", "Tundra", "Umber", "Velvet", "Willow", "Zinc", "Atlas",
            "Brume", "Cirrus", "Delta", "Echo", "Fable", "Glacier", "Hollow", "Isle", "Jade", "Keystone", "Lagoon", "Meridian", "Nimbus",
            "Opal", "Prism", "Quasar", "Ripple", "Solace", "Thistle", "Unity", "Vale", "Whisper", "Zenith", "Arbor", "Beacon", "Corona",
            "Dawn", "Eclipse", "Fathom", "Gossamer", "Horizon", "Ivory", "Jubilee", "Kindle", "Lyric", "Mirage", "Nomad", "Orchid",
            "Pilgrim", "Quiver", "Reverie", "Solstice", "Tempest", "Utopia", "Verdant", "Wanderer", "Yonder", "Zodiac", "Argent", "Bramble",
            "Cobalt", "Driftwood", "Evergreen", "Foxglove", "Gale", "Heather", "Iris", "Jessamine", "Kite", "Lilac", "Marigold", "Nettle",
            "Oleander", "Poppy", "Reed", "Sorrel", "Tansy", "Violet", "Wisteria", "Almond", "Brine", "Cedar", "Dune", "Estuary", "Fjord",
            "Glen", "Hearth", "Inlet", "Jetty", "Knoll", "Loch", "Mesa", "Narrows", "Outpost", "Pier", "Quay", "Ridge", "Strait", "Tarn",
        )
    private val prefixes = arrayOf("New", "Old", "Port", "Fort", "Mount", "Lake", "Cape", "Saint", "Nova", "Mare", "Terra", "Great", "Little", "Lost", "High", "Far", "Deep", "Free", "Last", "First")
    private val suffixes = arrayOf("Prime", "Major", "Minor", "Reach", "Haven", "Deep", "Landing", "Rest", "Hollow", "Gate", "Drift", "Bloom", "Crossing", "Verge", "Anchor", "Station", "Shore", "Fall", "Wake", "Cradle", "Spire", "Basin", "Terrace")
    private val greek = arrayOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Sigma", "Omega")
    private val roman = arrayOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")
    private val delimiters = arrayOf(" ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", "-", "-", "-", "_", "/", ".")
    private const val LETTERS = "ABCDEFGHJKLMNPRSTVWXYZ"

    private fun cap(s: String) = if (s.isEmpty()) s else s[0].uppercaseChar() + s.substring(1)

    /** A made-up, pronounceable word. */
    private fun word(r: Random): String {
        val syl = 2 + r.nextInt(3)
        val sb = StringBuilder()
        for (i in 0 until syl) {
            sb.append(onsets[r.nextInt(onsets.size)])
            sb.append(vowels[r.nextInt(vowels.size)])
            if (i < syl - 1 && r.nextFloat() < 0.35f) sb.append(codas[r.nextInt(codas.size)])
        }
        if (r.nextFloat() < 0.45f) sb.append(endings[r.nextInt(endings.size)])
        var w = sb.toString()
        // sometimes a break in the middle: Kel'tara, Vo-ruun
        if (w.length > 6 && r.nextFloat() < 0.14f) {
            val at = 2 + r.nextInt(w.length - 4)
            w = w.substring(0, at) + (if (r.nextBoolean()) "'" else "-") + w.substring(at)
        }
        return cap(w).split("-", "'").joinToString(if (w.contains("'")) "'" else if (w.contains("-")) "-" else "") { cap(it) }
    }

    private fun core(r: Random): String = if (r.nextFloat() < 0.65f) word(r) else pool[r.nextInt(pool.size)]

    private fun designation(r: Random): String =
        when (r.nextInt(6)) {
            0 -> roman[r.nextInt(roman.size)]
            1 -> greek[r.nextInt(greek.size)]
            2 -> "" + LETTERS[r.nextInt(LETTERS.length)] + "-" + (10 + r.nextInt(990))
            3 -> "" + (1 + r.nextInt(9)) + LETTERS[r.nextInt(LETTERS.length)]
            4 -> "" + (100 + r.nextInt(900))
            else -> "" + ('b' + r.nextInt(7))
        }

    /** The name of planet [idx] of the world with [seed]; [system] is the star's own name. */
    fun name(seed: Long, idx: Int, system: String): String {
        val r = Random(seed * 1_000_003L + idx * 7919L + 17L)
        val d = delimiters[r.nextInt(delimiters.size)]
        val roll = r.nextFloat()
        return when {
            roll < 0.30f -> core(r)
            roll < 0.42f -> prefixes[r.nextInt(prefixes.size)] + d + core(r)
            roll < 0.58f -> core(r) + d + suffixes[r.nextInt(suffixes.size)]
            roll < 0.70f -> core(r) + d + designation(r)
            roll < 0.78f -> prefixes[r.nextInt(prefixes.size)] + " " + core(r) + " " + suffixes[r.nextInt(suffixes.size)]
            roll < 0.86f -> system.substringBefore(' ') + d + designation(r)
            roll < 0.92f -> core(r) + "'s " + suffixes[r.nextInt(suffixes.size)]
            roll < 0.97f -> "" + LETTERS[r.nextInt(LETTERS.length)] + LETTERS[r.nextInt(LETTERS.length)] + d + (100 + r.nextInt(9000)) + ('a' + r.nextInt(8))
            else -> "The " + core(r)
        }
    }
}
