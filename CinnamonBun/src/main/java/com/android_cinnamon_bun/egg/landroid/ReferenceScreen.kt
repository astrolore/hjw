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

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// REFERENCE: view mode renders the markdown; EDIT switches to a raw markdown editor.
//   view:  [EDIT] [CLOSE]        edit:  [RESET] [SAVE]
//   hidden: while editing, hold SAVE to write a backup file (achievements, numbers, Reference) to
//   Downloads; in view mode, hold EDIT to pick a backup file and restore it.
// ---------------------------------------------------------------------------------------------

@Composable
internal fun ReferenceScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var saved by remember { mutableStateOf(ReferenceStore.load(context)) }
    var draft by remember { mutableStateOf(saved) }
    var editing by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val dirty = editing && draft != saved

    // hold EDIT: pick a backup file and restore it
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val ok = Backup.restoreFrom(context, uri)
                if (ok) {
                    saved = ReferenceStore.load(context)
                    draft = saved
                }
                Toast.makeText(context, if (ok) "backup restored" else "could not restore that file", Toast.LENGTH_SHORT).show()
            }
        }

    // the confirm state folds back after a few seconds, like the history's CLEAR button
    LaunchedEffect(confirmReset) {
        if (confirmReset) {
            delay(3000)
            confirmReset = false
        }
    }

    // RTFM: the Reference has to stay open (and the game in front) for two minutes in one go
    LaunchedEffect(Unit) {
        var seconds = 0
        while (seconds < 120) {
            delay(1000)
            if (AppState.foreground) seconds++
        }
        Achievements.unlock("rtfm")
    }

    // takes priority over the menu's own back handling while editing
    BackHandler(enabled = editing || confirmReset || confirmDiscard) {
        when {
            confirmReset -> confirmReset = false
            confirmDiscard -> confirmDiscard = false
            dirty -> confirmDiscard = true
            else -> editing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OverlayScreen(
            title = "REFERENCE",
            onClose = onClose,
            headerActions = {
                if (!editing) {
                    // tap: edit the Reference; hold: restore a backup file
                    ConsoleButtonLong(
                        color = Colors.Console,
                        bgColor = Color.Transparent,
                        borderColor = Colors.Console,
                        text = "EDIT",
                        onLongClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        onClick = {
                            draft = saved
                            editing = true
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    ConsoleButton(
                        textStyle = MenuText,
                        color = Colors.Console,
                        bgColor = Color.Transparent,
                        borderColor = Colors.Console,
                        text = "CLOSE",
                        onClick = onClose,
                    )
                } else {
                    ConsoleButton(
                        textStyle = if (confirmReset) MenuText.copy(fontSize = 10.sp, letterSpacing = 0.5.sp) else MenuText,
                        color = if (confirmReset) Colors.Eigengrau else WarningRed,
                        bgColor = if (confirmReset) WarningRed else Color.Transparent,
                        borderColor = WarningRed,
                        text = if (confirmReset) "TAP AGAIN TO RESET" else "RESET",
                    ) {
                        if (confirmReset) {
                            ReferenceStore.reset(context)
                            saved = ReferenceStore.loadOriginal(context)
                            draft = saved
                            confirmReset = false
                        } else {
                            confirmReset = true
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    // tap: save your edits; hold: also save the text as a .md file in Downloads
                    ConsoleButtonLong(
                        color = Colors.Eigengrau,
                        bgColor = Colors.Autopilot,
                        borderColor = Colors.Autopilot,
                        text = "SAVE",
                        onLongClick = {
                            // the backup also carries the Reference text, so save the edits first
                            ReferenceStore.save(context, draft)
                            val name = Backup.exportToDownloads(context)
                            if (name != null) Achievements.referenceExported()
                            Toast.makeText(
                                    context,
                                    if (name != null) "\uD83D\uDCBE backup saved: $name" else "could not save the backup",
                                    Toast.LENGTH_LONG,
                                )
                                .show()
                        },
                        onClick = {
                            ReferenceStore.save(context, draft)
                            if (draft != ReferenceStore.loadOriginal(context)) Achievements.referenceSavedWithEdits()
                            saved = draft
                            editing = false
                        },
                    )
                }
            },
        ) {
            if (editing) {
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Colors.Eigengrau3)
                            .verticalScroll(rememberScrollState())
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        textStyle = RefBody.copy(color = Colors.Console),
                        cursorBrush = SolidColor(Colors.Android.Chartreuse),
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    )
                }
            } else {
                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    ExplorerCard()
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Colors.Eigengrau3))
                    Spacer(Modifier.height(10.dp))
                    MarkdownView(saved)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        if (confirmDiscard) {
            ThemedPrompt(
                title = "UNSAVED CHANGES",
                message = "You have edits that are not saved yet.",
                buttons =
                    listOf(
                        "SAVE" to
                            {
                                ReferenceStore.save(context, draft)
                                saved = draft
                                confirmDiscard = false
                                editing = false
                            },
                        "DISCARD" to
                            {
                                draft = saved
                                confirmDiscard = false
                                editing = false
                            },
                        "KEEP EDITING" to { confirmDiscard = false },
                    ),
            )
        }
    }
}

// A small markdown renderer: headings (# to ######), rules (---, -----, ***), tables, bullets and
// numbered lines (with wrapped continuation lines), paragraphs (hard-wrapped lines are joined; a
// trailing backslash or two spaces is a line break), and inline **bold**, *italic*, `code` and
// backslash escapes such as \[ and \]. Long documents are fine: everything scrolls in one column.

private val RefBody =
    TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.5.sp,
    )

private const val ESCAPABLE = "\\`*_{}[]()#+-.!|<>~"

private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val c = text[i]
        // backslash escape: \[ shows [
        if (c == '\\' && i + 1 < text.length && ESCAPABLE.indexOf(text[i + 1]) >= 0) {
            append(text[i + 1])
            i += 2
            continue
        }
        if (text.startsWith("**", i)) {
            val end = text.indexOf("**", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                    append(inlineMarkdown(text.substring(i + 2, end)))
                }
                i = end + 2
                continue
            }
        }
        if (c == '`') {
            val end = text.indexOf('`', i + 1)
            if (end >= 0) {
                withStyle(
                    SpanStyle(
                        color = Colors.Android.Chartreuse,
                        background = Colors.Eigengrau2,
                        fontFamily = FontFamily.Monospace,
                    )
                ) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        if (c == '*' && i + 1 < text.length && text[i + 1] != '*' && text[i + 1] != ' ') {
            var end = -1
            var j = i + 2
            while (j < text.length) {
                if (text[j] == '*' && text[j - 1] != ' ' && text[j - 1] != '\\' &&
                    (j + 1 >= text.length || text[j + 1] != '*')) {
                    end = j
                    break
                }
                j++
            }
            if (end > 0) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(inlineMarkdown(text.substring(i + 1, end)))
                }
                i = end + 1
                continue
            }
        }
        append(c)
        i++
    }
}

private fun splitTableRow(line: String): List<String> =
    line.trim().trim('|').split('|').map { it.trim() }

private val NUMBERED = Regex("""^\d+[.)]\s+.*""")
private val BULLET = Regex("""^[-*+]\s+.*""")
private val RULE = Regex("""^(-{3,}|\*{3,}|_{3,})$""")
private val HEADING = Regex("""^#{1,6}\s+.*""")

/** True when this (trimmed) line starts a new block, so it can not continue a paragraph or item. */
private fun startsBlock(t: String): Boolean =
    t.isEmpty() ||
        RULE.matches(t) ||
        HEADING.matches(t) ||
        t.startsWith("|") ||
        BULLET.matches(t) ||
        NUMBERED.matches(t)

/** Joins hard-wrapped lines starting at [first]; returns the text and the index after the last line. */
private fun gatherLines(lines: List<String>, first: Int, firstText: String): Pair<String, Int> {
    val sb = StringBuilder()
    var i = first
    var t = firstText
    while (true) {
        val hardBreak = t.endsWith("\\") || lines[i].endsWith("  ")
        sb.append(if (t.endsWith("\\")) t.dropLast(1).trimEnd() else t)
        i++
        if (i >= lines.size) break
        val next = lines[i].trim()
        if (startsBlock(next)) break
        sb.append(if (hardBreak) "\n" else " ")
        t = next
    }
    return sb.toString() to i
}

@Composable
private fun MarkdownView(source: String) {
    val lines = source.replace("\r\n", "\n").lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> i++
            RULE.matches(trimmed) -> {
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Colors.Eigengrau3))
                Spacer(Modifier.height(10.dp))
                i++
            }
            HEADING.matches(trimmed) -> {
                val level = trimmed.takeWhile { it == '#' }.length
                val body = trimmed.drop(level).trim()
                when (level) {
                    1 -> {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            style = RefBody.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
                            color = Color.White,
                            text = inlineMarkdown(body),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    2 -> {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            style = RefBody.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
                            color = Color.White,
                            text = inlineMarkdown(body),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    else -> {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            style = RefBody.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = Color.White,
                            text = inlineMarkdown(body),
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                i++
            }
            trimmed.startsWith("|") -> {
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    val cells = splitTableRow(lines[i])
                    // skip the |---|---| separator row
                    if (!cells.all { c -> c.isNotEmpty() && c.all { ch -> ch == '-' || ch == ':' } }) {
                        rows.add(cells)
                    }
                    i++
                }
                MarkdownTable(rows)
                Spacer(Modifier.height(8.dp))
            }
            BULLET.matches(trimmed) -> {
                val indent = line.length - line.trimStart().length
                val first = trimmed.drop(1).trim()
                val (text, next) = gatherLines(lines, i, first)
                Row(modifier = Modifier.padding(start = (indent * 6).dp, top = 3.dp)) {
                    Text(style = RefBody, color = Colors.Android.Green, text = "• ")
                    Text(
                        modifier = Modifier.weight(1f),
                        style = RefBody,
                        color = Colors.Console,
                        text = inlineMarkdown(text),
                    )
                }
                i = next
            }
            NUMBERED.matches(trimmed) -> {
                val num = trimmed.takeWhile { it.isDigit() }
                val first = trimmed.drop(num.length + 1).trim()
                val (text, next) = gatherLines(lines, i, first)
                Row(modifier = Modifier.padding(top = 3.dp)) {
                    Text(style = RefBody, color = Colors.Android.Green, text = "$num. ")
                    Text(
                        modifier = Modifier.weight(1f),
                        style = RefBody,
                        color = Colors.Console,
                        text = inlineMarkdown(text),
                    )
                }
                i = next
            }
            else -> {
                val (text, next) = gatherLines(lines, i, trimmed)
                Text(
                    modifier = Modifier.padding(vertical = 3.dp),
                    style = RefBody,
                    color = Colors.Console,
                    text = inlineMarkdown(text),
                )
                i = next
            }
        }
    }
}

@Composable
private fun MarkdownTable(rows: List<List<String>>) {
    if (rows.isEmpty()) return
    val cols = rows.maxOf { it.size }
    // Column widths follow content length, clamped so nothing gets crushed or hogs the row.
    val weights =
        (0 until cols).map { c ->
            rows.maxOf { (it.getOrNull(c) ?: "").length }.coerceIn(6, 40).toFloat()
        }
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { r, row ->
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                for (c in 0 until cols) {
                    Box(
                        modifier =
                            Modifier.weight(weights[c])
                                .fillMaxHeight()
                                .background(if (r == 0) Colors.Eigengrau2 else Color.Transparent)
                                .border(0.5.dp, Colors.Eigengrau3)
                                .padding(4.dp)
                    ) {
                        Text(
                            style =
                                RefBody.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = if (r == 0) FontWeight.Bold else FontWeight.Normal,
                                ),
                            color = if (r == 0) Colors.Android.Mint else Colors.Console,
                            text = inlineMarkdown(row.getOrNull(c) ?: ""),
                        )
                    }
                }
            }
        }
    }
}
