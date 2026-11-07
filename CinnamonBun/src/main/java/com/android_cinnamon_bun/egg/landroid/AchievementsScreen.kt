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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// The Achievements page: one list of rounded console tiles (no sections), Perfectionist last.
//   locked tiles are red with a glitching ???, unlocked ones cyan with their colourful art
// ---------------------------------------------------------------------------------------------

private enum class AchSort(val label: String) {
    DEFAULT("DEFAULT"),
    CLOSEST("CLOSEST TO UNLOCKING"),
    AZ("A TO Z"),
}

private val TILE_SHAPE = RoundedCornerShape(15.dp)
private val ART_SHAPE = RoundedCornerShape(10.dp)
private val BAR_TRACK = Color(0xFF2B2B2B)

/** The chosen sort is remembered between visits (the filters are not). */
private fun loadSort(context: Context): AchSort {
    val name = context.getSharedPreferences("landroid_ui", Context.MODE_PRIVATE).getString("ach_sort", null)
    return AchSort.values().firstOrNull { it.name == name } ?: AchSort.DEFAULT
}

private fun saveSort(context: Context, sort: AchSort) {
    context.getSharedPreferences("landroid_ui", Context.MODE_PRIVATE).edit().putString("ach_sort", sort.name).apply()
}

/** What locked titles and ??? turn into: obscure symbols that keep changing. */
private const val GLITCH_CHARS =
    "\u2206\u00A7\u03C0\u00B6\u03A9\u266A\u2260\u039E\u03A8\u03A3\u039B\u03A6\u2202\u2211\u220F\u221A\u221E\u2248\u2261\u222B\u2318\u2325\u233D\u2297\u2295\u2298\u22C8\u25C6\u25C7\u25B2\u25BC\u25A3\u2591\u2592\u2593\u00A4\u00A5\u00A3\u00BF\u00A1\u2021\u2020\u203B\u203D\u2042\u2127\u0278\u03DE\u03FE\u2234\u2235\u29EB\u22A5"

private fun glitchChar(rnd: Random): Char = GLITCH_CHARS[rnd.nextInt(GLITCH_CHARS.length)]

/** Obfuscated text: every letter keeps turning into a random obscure symbol, very fast. */
@Composable
private fun GlitchText(text: String, tick: IntState, color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    val t = tick.intValue
    val glitched =
        remember(t, text) {
            val rnd = Random(t * 7919L + text.hashCode())
            buildString { for (c in text) append(if (c == ' ') ' ' else glitchChar(rnd)) }
        }
    Text(modifier = modifier, style = style, color = color, text = glitched)
}

/** A glitching ???: mostly steady, with bursts of jitter, colour-split ghosts and swapped symbols. */
@Composable
private fun GlitchQuestion(tick: IntState, color: Color) {
    val t = tick.intValue
    val style = MenuText.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    val burst = t % 11 == 0 || t % 29 < 3
    val rnd = remember(t) { Random(t * 31L + 5L) }
    val dx = if (burst) rnd.nextInt(-5, 6) else 0
    val dy = if (burst) rnd.nextInt(-3, 4) else 0
    val shown =
        remember(t) {
            if (t % 7 == 0 || burst) {
                buildString { repeat(3) { append(if (rnd.nextInt(100) < 45) glitchChar(rnd) else '?') } }
            } else {
                "???"
            }
        }
    Box(contentAlignment = Alignment.Center) {
        if (burst) {
            Text(
                modifier = Modifier.offset(x = (dx - 2).dp, y = dy.dp),
                style = style,
                color = Color(0xCC00E5FF),
                text = shown,
            )
            Text(
                modifier = Modifier.offset(x = (dx + 2).dp, y = (-dy).dp),
                style = style,
                color = Color(0xCCFF1744),
                text = shown,
            )
        }
        Text(modifier = Modifier.offset(x = (dx / 2).dp), style = style, color = color, text = shown)
    }
}

/** A padlock, drawn in the game's line style. */
@Composable
private fun LockIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 2.5.dp.toPx()
        // shackle
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.3f, h * 0.12f),
            size = Size(w * 0.4f, h * 0.4f),
            style = Stroke(sw, cap = StrokeCap.Round),
        )
        drawLine(color, Offset(w * 0.3f, h * 0.32f), Offset(w * 0.3f, h * 0.46f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.7f, h * 0.32f), Offset(w * 0.7f, h * 0.46f), sw, StrokeCap.Round)
        // body
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.18f, h * 0.44f),
            size = Size(w * 0.64f, h * 0.44f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
        )
        // keyhole
        drawCircle(Colors.Eigengrau, radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.62f))
        drawLine(Colors.Eigengrau, Offset(w * 0.5f, h * 0.62f), Offset(w * 0.5f, h * 0.76f), sw, StrokeCap.Round)
    }
}

@Composable
private fun ConsoleCheck(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.clickable { onChange(!checked) }.padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp).border(1.dp, Colors.Console).padding(3.dp)) {
            if (checked) Box(Modifier.fillMaxSize().background(Colors.Console))
        }
        Spacer(Modifier.width(8.dp))
        Text(style = MenuText.copy(fontSize = 11.sp), color = Colors.Console, text = label)
    }
}

@Composable
private fun AchTile(def: AchDef, tick: IntState) {
    val unlocked = Achievements.isUnlocked(def.id)
    val note = Achievements.modNote(def)
    // locked is the warning red; unlocked wears the achievement colour
    val color = if (unlocked) sectionColor(def) else WarningRed
    val progress = Achievements.progressOf(def)

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(TILE_SHAPE)
                .background(Colors.Eigengrau)
                .border(1.5.dp, color, TILE_SHAPE)
                .padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // art: colourful when unlocked; a glitching ??? (or a padlock) while locked
        Box(
            Modifier.size(64.dp).clip(ART_SHAPE).border(1.dp, color, ART_SHAPE),
            contentAlignment = Alignment.Center,
        ) {
            when {
                unlocked -> AchievementArt(id = def.id, unlocked = true, modifier = Modifier.fillMaxSize().padding(6.dp))
                note == ModNote.DISABLED || note == ModNote.NEEDS_CHEATING ->
                    LockIcon(color = color, modifier = Modifier.fillMaxSize().padding(14.dp))
                else -> GlitchQuestion(tick = tick, color = color)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val titleStyle = MenuText.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
            if (unlocked) {
                Text(style = titleStyle, color = color, text = def.title.uppercase())
            } else {
                GlitchText(text = def.title.uppercase(), tick = tick, color = color, style = titleStyle)
            }
            Spacer(Modifier.height(4.dp))
            // the "not available yet" tag goes away on its own once an achievement can be earned
            Text(
                style = MenuText.copy(fontSize = 10.sp, lineHeight = 13.sp),
                color = Colors.Eigengrau4,
                text = (def.text + if (def.earnable) "" else " (Not available yet.)").uppercase(),
            )
            if (note != ModNote.NONE) {
                Spacer(Modifier.height(4.dp))
                Text(
                    style = MenuText.copy(fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold),
                    color =
                        when (note) {
                            ModNote.STILL_EARNABLE -> Colors.Track
                            ModNote.NEEDS_CHEATING -> Colors.Android.Chartreuse
                            else -> WarningRed
                        },
                    text =
                        when (note) {
                            ModNote.DISABLED -> "[ACHIEVEMENT DISABLED]"
                            ModNote.STILL_EARNABLE -> "[STILL EARNABLE]"
                            else -> "[SOME ACHIEVEMENTS REQUIRE CHEATING]"
                        },
                )
            }
            // a bar only while locked, and only where progress makes sense
            if (progress != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(BAR_TRACK)) {
                        val frac = if (progress.second > 0) (progress.first.toFloat() / progress.second).coerceIn(0f, 1f) else 0f
                        Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Colors.Track))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        style = MenuText.copy(fontSize = 11.sp),
                        color = color,
                        text = "${progress.first}/${progress.second}",
                    )
                }
            }
        }
    }
}

/** One of the little dimmed tiles in the empty-state picture. */
@Composable
private fun GhostTile(modifier: Modifier, rows: Int, hash: Boolean) {
    val dim = Colors.Eigengrau3
    Row(
        modifier =
            modifier
                .size(width = 190.dp, height = 46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Colors.Eigengrau)
                .border(2.dp, dim, RoundedCornerShape(10.dp))
                .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).border(1.5.dp, dim, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (hash) {
                Text(style = MenuText.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = Colors.Eigengrau4, text = "#")
            }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                style = MenuText.copy(fontSize = 9.sp, letterSpacing = 2.sp, lineHeight = 11.sp),
                color = dim,
                text = if (rows > 1) "* * * * * * * *\n* * * * * *" else "* * * * * * * *",
            )
        }
    }
}

/** Shown when the filters leave nothing to list: the achievement version of an empty channel list. */
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 36.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(width = 250.dp, height = 150.dp)) {
            GhostTile(Modifier.offset(x = 0.dp, y = 0.dp), rows = 1, hash = false)
            GhostTile(Modifier.offset(x = 56.dp, y = 50.dp), rows = 2, hash = false)
            GhostTile(Modifier.offset(x = 14.dp, y = 100.dp), rows = 1, hash = true)
            // a few sparkles, like the original picture
            Text(style = MenuText.copy(fontSize = 12.sp), color = Colors.Eigengrau3, text = "+", modifier = Modifier.offset(x = 232.dp, y = 8.dp))
            Text(style = MenuText.copy(fontSize = 12.sp), color = Colors.Eigengrau3, text = "o", modifier = Modifier.offset(x = 206.dp, y = 22.dp))
            Text(style = MenuText.copy(fontSize = 12.sp), color = Colors.Eigengrau3, text = ".:", modifier = Modifier.offset(x = 214.dp, y = 118.dp))
            Text(style = MenuText.copy(fontSize = 12.sp), color = Colors.Eigengrau3, text = "+", modifier = Modifier.offset(x = 2.dp, y = 62.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(
            style = MenuText.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            color = Colors.Eigengrau4,
            text = "NO ACHIEVEMENTS TO DISPLAY",
        )
        Spacer(Modifier.height(6.dp))
        Text(
            style = MenuText.copy(fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.Center),
            color = Colors.Eigengrau4,
            text =
                "You find yourself in a strange place. None of the achievement tiles can get through " +
                    "the currently set filters or achievements don't exist.",
        )
    }
}

@Composable
internal fun AchievementsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var sort by remember { mutableStateOf(loadSort(context)) }
    var hideUnlocked by remember { mutableStateOf(false) }
    var hideLocked by remember { mutableStateOf(false) }

    // one shared clock drives every glitch
    val tick = remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(55)
            tick.intValue = tick.intValue + 1
        }
    }

    fun fraction(def: AchDef): Float {
        val p = Achievements.progressOf(def) ?: return -1f
        return if (p.second > 0) p.first.toFloat() / p.second else 0f
    }

    fun visible(def: AchDef): Boolean {
        val u = Achievements.isUnlocked(def.id)
        return !(hideUnlocked && u) && !(hideLocked && !u)
    }

    val regular = ALL_ACHIEVEMENTS.filter { !it.bonus && visible(it) }
    val bonus = ALL_ACHIEVEMENTS.filter { it.bonus && visible(it) }

    val ordered =
        when (sort) {
            // newest unlock first, then everything still locked in list order (normal and legendary mixed)
            AchSort.DEFAULT ->
                regular.filter { Achievements.isUnlocked(it.id) }.sortedByDescending { Achievements.unlocked[it.id] ?: 0L } +
                    regular.filter { !Achievements.isUnlocked(it.id) }
            AchSort.CLOSEST ->
                regular.sortedWith(
                    compareBy<AchDef>(
                        { if (Achievements.isUnlocked(it.id)) 2 else if (fraction(it) >= 0f) 0 else 1 },
                        { -fraction(it) },
                    )
                )
            AchSort.AZ -> regular.sortedBy { it.title.lowercase() }
        }
    // tiles that cannot be earned right now (mods) always sink to the end of the list
    val list =
        ordered.sortedBy {
            val n = Achievements.modNote(it)
            if (n == ModNote.DISABLED || n == ModNote.NEEDS_CHEATING) 1 else 0
        }

    OverlayScreen(title = "ACHIEVEMENTS ${Achievements.totalUnlocked}/$ACH_TOTAL", onClose = onClose) {
        ConsoleButton(
            textStyle = MenuText,
            color = Colors.Console,
            bgColor = Color.Transparent,
            borderColor = Colors.Console,
            text = "SORT: ${sort.label}",
        ) {
            sort = AchSort.values()[(sort.ordinal + 1) % AchSort.values().size]
            saveSort(context, sort)
        }
        Spacer(Modifier.height(4.dp))
        Row {
            ConsoleCheck("HIDE UNLOCKED", hideUnlocked) { hideUnlocked = it }
            Spacer(Modifier.width(14.dp))
            ConsoleCheck("HIDE LOCKED", hideLocked) { hideLocked = it }
        }

        if (list.isEmpty() && bonus.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(list, key = { it.id }) { AchTile(it, tick) }
                if (bonus.isNotEmpty()) {
                    item(key = "line_end") {
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE8F5E9).copy(alpha = 0.5f)))
                        Spacer(Modifier.height(6.dp))
                    }
                    items(bonus, key = { it.id }) { AchTile(it, tick) }
                }
                item(key = "end") { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
