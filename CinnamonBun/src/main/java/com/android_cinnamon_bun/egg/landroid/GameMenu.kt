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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.GregorianCalendar
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// MOD: in-game menu (top right), with a dropdown that opens into:
//   CHOOSE WORLD (calendar, seed box, random world), HISTORY (replay old worlds),
//   SETTINGS (gameplay + mod menu), REFERENCE (the manual, editable)
// ---------------------------------------------------------------------------------------------

private enum class MenuScreen {
    None,
    Calendar,
    History,
    Settings,
    Reference,
}

private class MenuEntry(val label: String, val action: () -> Unit)

private val MENU_WIDTH = 176.dp

@Composable
fun GameMenu(
    universe: Universe,
    achievementsOff: Boolean,
    onPlayWorld: (Long) -> Unit,
    onModConfirmed: () -> Unit,
    onHistoryCleared: () -> Unit,
) {
    var buttonVisible by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf(MenuScreen.None) }

    LaunchedEffect("menu") {
        delay(1000)
        buttonVisible = true
    }

    BackHandler(enabled = screen != MenuScreen.None || menuOpen) {
        if (screen != MenuScreen.None) screen = MenuScreen.None else menuOpen = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Tap anywhere outside the dropdown to fold it back up.
        if (menuOpen) {
            Box(
                modifier =
                    Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { menuOpen = false } }
            )
        }

        Column(
            modifier =
                Modifier.align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeContent)
                    .padding(6.dp),
            horizontalAlignment = Alignment.End,
        ) {
            AnimatedVisibility(visible = buttonVisible, enter = flickerFadeInAfterDelay(500)) {
                ConsoleButton(
                    textStyle = MenuText,
                    color = Colors.Console,
                    bgColor = if (menuOpen) Colors.Autopilot else Colors.Eigengrau,
                    borderColor = Colors.Console,
                    text = if (menuOpen) "MENU ^" else "MENU v",
                ) {
                    menuOpen = !menuOpen
                }
            }

            val entries = buildList {
                // when the HUD is hidden, this is the way back
                if (!GameSettings.showHud) {
                    add(
                        MenuEntry("> SHOW HUD") {
                            GameSettings.showHud = true
                            GameSettings.save()
                            menuOpen = false
                        }
                    )
                }
                add(MenuEntry("> CHOOSE WORLD") { screen = MenuScreen.Calendar; menuOpen = false })
                add(MenuEntry("> HISTORY") { screen = MenuScreen.History; menuOpen = false })
                add(MenuEntry("> SETTINGS") { screen = MenuScreen.Settings; menuOpen = false })
                add(MenuEntry("> REFERENCE") { screen = MenuScreen.Reference; menuOpen = false })
            }

            // Dropdown: each row unrolls one after the other.
            AnimatedVisibility(
                visible = menuOpen,
                enter = expandVertically(tween(200), expandFrom = Alignment.Top),
                exit = shrinkVertically(tween(120), shrinkTowards = Alignment.Top) + fadeOut(tween(120)),
            ) {
                Text(
                    style = MenuText.copy(fontSize = 10.sp),
                    color = Colors.Eigengrau4,
                    modifier =
                        Modifier.width(MENU_WIDTH)
                            .background(Colors.Eigengrau)
                            .border(1.dp, Colors.Eigengrau3)
                            .padding(6.dp),
                    text =
                        buildAnnotatedString {
                            append(worldLabel(universe))
                            append("\n")
                            withStyle(SpanStyle(color = Colors.Android.Chartreuse)) {
                                append(
                                    if (achievementsOff) "ACHIEVEMENTS DISABLED"
                                    else "ACHIEVEMENTS ENABLED"
                                )
                            }
                        },
                )
            }
            entries.forEachIndexed { index, entry ->
                val delayMs = (index + 1) * 70
                AnimatedVisibility(
                    visible = menuOpen,
                    enter =
                        expandVertically(tween(180, delayMillis = delayMs), expandFrom = Alignment.Top) +
                            flickerFadeInAfterDelay(delayMs),
                    exit =
                        shrinkVertically(tween(120), shrinkTowards = Alignment.Top) +
                            fadeOut(tween(120)),
                ) {
                    ConsoleButton(
                        modifier = Modifier.width(MENU_WIDTH),
                        textStyle = MenuText,
                        color = Colors.Console,
                        bgColor = Colors.Eigengrau,
                        borderColor = Colors.Console,
                        text = entry.label,
                        onClick = entry.action,
                    )
                }
            }
        }

        when (screen) {
            MenuScreen.Calendar ->
                CalendarScreen(
                    onClose = { screen = MenuScreen.None },
                    onPlay = { seed ->
                        screen = MenuScreen.None
                        onPlayWorld(seed)
                    },
                )
            MenuScreen.History ->
                HistoryScreen(
                    currentSeed = universe.randomSeed,
                    onClose = { screen = MenuScreen.None },
                    onPlay = { seed ->
                        screen = MenuScreen.None
                        onPlayWorld(seed)
                    },
                    onCleared = onHistoryCleared,
                )
            MenuScreen.Settings ->
                SettingsScreen(
                    onClose = { screen = MenuScreen.None },
                    onModConfirmed = onModConfirmed,
                )
            MenuScreen.Reference -> ReferenceScreen(onClose = { screen = MenuScreen.None })
            MenuScreen.None -> {}
        }
    }
}

private fun worldLabel(universe: Universe): String {
    val date = seedToDateString(universe.randomSeed)
    return "WORLD: ${universe.star.name}\n" +
        (if (date != null) "DATE:  $date\n" else "MODE:  RANDOM\n") +
        "SEED:  ${universe.randomSeed}"
}

// ---------------------------------------------------------------------------------------------
// Shared full-screen "console page"
// ---------------------------------------------------------------------------------------------

/**
 * A full-screen themed page. [headerActions] replaces the default CLOSE button. With
 * [transparentBg] the page turns see-through so the game shows behind it (used by the FOV
 * slider preview). With [scroll] the content scrolls.
 */
@Composable
internal fun OverlayScreen(
    title: String,
    onClose: () -> Unit,
    scroll: Boolean = false,
    transparentBg: Boolean = false,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect("overlay") { shown = true }

    val bgAlpha by animateFloatAsState(if (transparentBg) 0f else 1f, tween(150), label = "bg")
    val chromeAlpha by animateFloatAsState(if (transparentBg) 0f else 1f, tween(150), label = "chrome")

    AnimatedVisibility(visible = shown, enter = flickerFadeIn) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .background(Colors.Eigengrau.copy(alpha = bgAlpha))
                    .pointerInput(Unit) { detectTapGestures {} } // swallow taps: don't fly the ship
                    .windowInsetsPadding(WindowInsets.safeContent)
                    .imePadding()
                    .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().alpha(chromeAlpha),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    style = MenuText.copy(fontSize = 14.sp),
                    color = Colors.Console,
                    text = "// $title",
                )
                if (headerActions != null) {
                    headerActions()
                } else {
                    ConsoleButton(
                        textStyle = MenuText,
                        color = Colors.Console,
                        bgColor = Color.Transparent,
                        borderColor = Colors.Console,
                        text = "CLOSE",
                        onClick = onClose,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Colors.Eigengrau3).alpha(chromeAlpha))
            Spacer(Modifier.height(10.dp))
            if (scroll) {
                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// CHOOSE WORLD: calendar + seed box
// ---------------------------------------------------------------------------------------------

private val MONTH_NAMES =
    listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

private fun isLeapYear(y: Int) = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0

private fun daysInMonth(year: Int, month0: Int): Int =
    when (month0) {
        1 -> if (isLeapYear(year)) 29 else 28
        3,
        5,
        8,
        10 -> 30
        else -> 31
    }

/** 0 = Sunday. Proleptic Gregorian (Sakamoto's method), so every month renders consistently. */
private fun dayOfWeek(year: Int, month0: Int, day: Int): Int {
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val y = if (month0 < 2) year - 1 else year
    return (y + y / 4 - y / 100 + y / 400 + t[month0] + day) % 7
}

/** Years to jump per step while a year button is held: 1, then 5, 25, 100, 400... */
private fun yearJump(step: Int): Int =
    when {
        step < 6 -> 1
        step < 14 -> 5
        step < 26 -> 25
        step < 40 -> 100
        else -> 400
    }

@Composable
private fun CalendarScreen(onClose: () -> Unit, onPlay: (Long) -> Unit) {
    val today = remember { GregorianCalendar() }
    val todayY = today.get(java.util.Calendar.YEAR)
    val todayM = today.get(java.util.Calendar.MONTH)
    val todayD = today.get(java.util.Calendar.DAY_OF_MONTH)

    var viewYear by remember { mutableIntStateOf(todayY) }
    var viewMonth by remember { mutableIntStateOf(todayM) }
    var selY by remember { mutableIntStateOf(todayY) }
    var selM by remember { mutableIntStateOf(todayM) }
    var selD by remember { mutableIntStateOf(todayD) }
    var seedText by remember { mutableStateOf("") }

    fun shiftMonth(delta: Int) {
        var m = viewMonth + delta
        var y = viewYear
        while (m < 0) {
            m += 12
            y -= 1
        }
        while (m > 11) {
            m -= 12
            y += 1
        }
        if (y in MIN_CALENDAR_YEAR..MAX_CALENDAR_YEAR) {
            viewYear = y
            viewMonth = m
        }
    }

    fun shiftYear(delta: Int) {
        viewYear = (viewYear + delta).coerceIn(MIN_CALENDAR_YEAR, MAX_CALENDAR_YEAR)
    }

    OverlayScreen(title = "CHOOSE WORLD", onClose = onClose, scroll = true) {
        Text(
            style = MenuText,
            color = Colors.Eigengrau4,
            text = "EVERY DATE IS ITS OWN WORLD.\nPICK A DAY, TYPE A SEED, OR ROLL ONE.\nHOLD << OR >> TO SPEED THROUGH THE YEARS.",
        )
        Spacer(Modifier.height(12.dp))

        // month / year navigation (hold to keep going, faster and faster)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HoldButton(text = "<<") { n -> shiftYear(-yearJump(n)) }
            HoldButton(text = "<") { _ -> shiftMonth(-1) }
            Text(
                style = MenuText.copy(fontSize = 15.sp),
                color = Colors.Console,
                textAlign = TextAlign.Center,
                text = "${MONTH_NAMES[viewMonth]} $viewYear",
            )
            HoldButton(text = ">") { _ -> shiftMonth(1) }
            HoldButton(text = ">>") { n -> shiftYear(yearJump(n)) }
        }
        Spacer(Modifier.height(8.dp))

        // weekday header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA").forEach {
                Text(
                    modifier = Modifier.weight(1f),
                    style = MenuText,
                    color = Colors.Eigengrau4,
                    textAlign = TextAlign.Center,
                    text = it,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // day grid (always 6 rows so the layout never jumps)
        val firstDow = dayOfWeek(viewYear, viewMonth, 1)
        val monthDays = daysInMonth(viewYear, viewMonth)
        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = week * 7 + col - firstDow + 1
                    if (day in 1..monthDays) {
                        val selected = viewYear == selY && viewMonth == selM && day == selD
                        val isToday = viewYear == todayY && viewMonth == todayM && day == todayD
                        Box(
                            modifier =
                                Modifier.weight(1f)
                                    .padding(1.dp)
                                    .height(38.dp)
                                    .background(if (selected) Colors.Autopilot else Color.Transparent)
                                    .border(
                                        1.dp,
                                        when {
                                            isToday -> Colors.Android.Chartreuse
                                            selected -> Colors.Autopilot
                                            else -> Colors.Eigengrau3
                                        },
                                    )
                                    .clickable {
                                        selY = viewYear
                                        selM = viewMonth
                                        selD = day
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                style = MenuText,
                                color = if (selected) Colors.Eigengrau else Colors.Console,
                                text = "$day",
                            )
                        }
                    } else {
                        Spacer(Modifier.weight(1f).padding(1.dp).height(38.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        val seed = seedForDate(selY, selM, selD)
        Text(
            style = MenuText,
            color = Colors.Console,
            text = "SELECTED: %04d-%02d-%02d\nSEED:     %d".format(selY, selM + 1, selD, seed),
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConsoleButton(
                textStyle = MenuText,
                color = Colors.Eigengrau,
                bgColor = Colors.Autopilot,
                borderColor = Colors.Autopilot,
                text = "LAUNCH",
            ) {
                onPlay(seed)
            }
            ConsoleButton(
                textStyle = MenuText,
                color = Colors.Console,
                bgColor = Color.Transparent,
                borderColor = Colors.Console,
                text = "TODAY",
            ) {
                selY = todayY
                selM = todayM
                selD = todayD
                viewYear = todayY
                viewMonth = todayM
            }
            ConsoleButton(
                textStyle = MenuText,
                color = Colors.Android.Chartreuse,
                bgColor = Color.Transparent,
                borderColor = Colors.Android.Chartreuse,
                text = "RANDOM WORLD",
            ) {
                onPlay(newRandomWorldSeed())
            }
        }

        // ----- type a seed -----
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Colors.Eigengrau3))
        Spacer(Modifier.height(12.dp))
        Text(style = MenuText, color = Colors.Console, text = "OR TYPE A SEED")
        Spacer(Modifier.height(6.dp))

        val typedSeed: Long? = seedText.toLongOrNull()
        val tooLarge = seedText.isNotEmpty() && typedSeed == null

        BasicTextField(
            value = seedText,
            onValueChange = { v -> seedText = v.filter { it.isDigit() }.take(19) },
            singleLine = true,
            textStyle = MenuText.copy(fontSize = 15.sp, color = Colors.Console),
            cursorBrush = SolidColor(Colors.Android.Chartreuse),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .border(1.dp, if (tooLarge) Colors.Android.Chartreuse else Colors.Console)
                            .padding(10.dp)
                ) {
                    if (seedText.isEmpty()) {
                        Text(style = MenuText.copy(fontSize = 15.sp), color = Colors.Eigengrau3, text = "0000000")
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            style = MenuText.copy(fontSize = 11.sp),
            color = if (tooLarge) Colors.Android.Chartreuse else Colors.Eigengrau4,
            text =
                when {
                    seedText.isEmpty() -> "ANY NUMBER FROM 0 TO 9223372036854775807"
                    tooLarge -> "SEED TOO LARGE"
                    else ->
                        seedToDateString(typedSeed!!)?.let { "DATE WORLD $it" } ?: "RANDOM-WORLD SEED"
                },
        )
        Spacer(Modifier.height(10.dp))
        ConsoleButton(
            textStyle = MenuText,
            color = if (typedSeed != null) Colors.Eigengrau else Colors.Eigengrau3,
            bgColor = if (typedSeed != null) Colors.Autopilot else Color.Transparent,
            borderColor = if (typedSeed != null) Colors.Autopilot else Colors.Eigengrau3,
            text = "LAUNCH SEED",
        ) {
            if (typedSeed != null) onPlay(typedSeed)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// HISTORY
// ---------------------------------------------------------------------------------------------

@Composable
private fun HistoryScreen(
    currentSeed: Long,
    onClose: () -> Unit,
    onPlay: (Long) -> Unit,
    onCleared: () -> Unit,
) {
    val context: Context = LocalContext.current
    var records by remember { mutableStateOf(WorldHistory.load(context)) }
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(confirmClear) {
        if (confirmClear) {
            delay(3000)
            confirmClear = false
        }
    }

    OverlayScreen(title = "HISTORY", onClose = onClose) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                style = MenuText,
                color = Colors.Eigengrau4,
                text = "${records.size} WORLD${if (records.size == 1) "" else "S"} LOGGED",
            )
            if (records.isNotEmpty()) {
                ConsoleButton(
                    textStyle = MenuText,
                    color = if (confirmClear) Colors.Eigengrau else Colors.Console,
                    bgColor = if (confirmClear) Colors.Android.Chartreuse else Color.Transparent,
                    borderColor = Colors.Eigengrau3,
                    text = if (confirmClear) "TAP AGAIN TO ERASE" else "CLEAR",
                ) {
                    if (confirmClear) {
                        WorldHistory.clear(context)
                        records = emptyList()
                        confirmClear = false
                        onCleared()
                    } else {
                        confirmClear = true
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (records.isEmpty()) {
            Text(
                style = MenuText,
                color = Colors.Console,
                text = "NO WORLDS ON RECORD YET.\nFLY SOMEWHERE.",
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(records, key = { it.seed }) { w ->
                    val current = w.seed == currentSeed
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, if (current) Colors.Autopilot else Colors.Eigengrau3)
                                .clickable { onPlay(w.seed) }
                                .padding(10.dp)
                    ) {
                        Text(
                            style = MenuText.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = Colors.Console,
                            text = w.name,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            style = MenuText.copy(fontSize = 11.sp, lineHeight = 15.sp),
                            color = Colors.Console,
                            text =
                                "PLANETS:  ${w.planets}   STAR CLASS: ${w.starClass}\n" +
                                    "EXPLORED: ${w.explored} / ${w.planets}\n" +
                                    (w.dateString?.let { "DATE:     $it\n" } ?: "MODE:     RANDOM\n") +
                                    "SEED:     ${w.seed}\n" +
                                    "LAST:     ${w.lastPlayedString()}   VISITS: ${w.plays}",
                        )
                        if (w.achDisabled) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                style = MenuText.copy(fontSize = 11.sp),
                                color = Colors.Android.Chartreuse,
                                text = "ACHIEVEMENTS DISABLED FOR WORLD",
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            style = MenuText.copy(fontSize = 10.sp),
                            color = if (current) Colors.Autopilot else Colors.Android.Chartreuse,
                            text = if (current) "[ CURRENT WORLD - TAP TO RESTART ]" else "[ TAP TO REPLAY ]",
                        )
                    }
                }
            }
        }
    }
}
