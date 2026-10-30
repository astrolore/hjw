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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------------------------
// REFERENCE: view mode renders the markdown; EDIT switches to a raw markdown editor.
//   view:  [EDIT] [CLOSE]        edit:  [RESET] [SAVE]
//   hidden: long-press EDIT saves the current Reference to the Downloads folder.
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
                    ConsoleButtonLong(
                        color = Colors.Console,
                        bgColor = Color.Transparent,
                        borderColor = Colors.Console,
                        text = "EDIT",
                        onLongClick = {
                            val ok = ReferenceStore.exportToDownloads(context, saved)
                            Toast.makeText(
                                    context,
                                    if (ok) "\uD83D\uDCBE saved reference"
                                    else "could not save reference",
                                    Toast.LENGTH_SHORT,
                                )
                                .show()
                        },
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
                        textStyle = MenuText,
                        color = Colors.Android.Chartreuse,
                        bgColor = Color.Transparent,
                        borderColor = Colors.Android.Chartreuse,
                        text = "RESET",
                    ) {
                        confirmReset = true
                    }
                    Spacer(Modifier.width(8.dp))
                    ConsoleButton(
                        textStyle = MenuText,
                        color = Colors.Eigengrau,
                        bgColor = Colors.Autopilot,
                        borderColor = Colors.Autopilot,
                        text = "SAVE",
                    ) {
                        ReferenceStore.save(context, draft)
                        saved = draft
                        editing = false
                    }
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
                    MarkdownView(saved)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        if (confirmReset) {
            ThemedPrompt(
                title = "RESET REFERENCE?",
                message = "This throws away your edits and brings back the built-in Reference.",
                buttons =
                    listOf(
                        "RESET" to
                            {
                                ReferenceStore.reset(context)
                                saved = ReferenceStore.loadOriginal(context)
                                draft = saved
                                confirmReset = false
                            },
                        "CANCEL" to { confirmReset = false },
                    ),
            )
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

// A deliberately small markdown renderer: headings, rules, tables, bullets, numbered lines,
// paragraphs, and inline **bold** / `code`. That's everything the reference uses.

private val RefBody =
    TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.5.sp,
    )

private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val end = text.indexOf("**", i + 2)
            if (end >= 0) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Colors.Android.Mint)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        if (text[i] == '`') {
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
        append(text[i])
        i++
    }
}

private fun splitTableRow(line: String): List<String> =
    line.trim().trim('|').split('|').map { it.trim() }

private val NUMBERED = Regex("""^\d+\.\s.*""")

@Composable
private fun MarkdownView(source: String) {
    val lines = source.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> i++
            trimmed == "---" -> {
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Colors.Eigengrau3))
                Spacer(Modifier.height(10.dp))
                i++
            }
            trimmed.startsWith("### ") -> {
                Spacer(Modifier.height(10.dp))
                Text(
                    style = RefBody.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = Colors.Console,
                    text = inlineMarkdown(trimmed.removePrefix("### ")),
                )
                Spacer(Modifier.height(4.dp))
                i++
            }
            trimmed.startsWith("## ") -> {
                Spacer(Modifier.height(14.dp))
                Text(
                    style = RefBody.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = Colors.Android.Green,
                    text = inlineMarkdown(trimmed.removePrefix("## ")),
                )
                Spacer(Modifier.height(6.dp))
                i++
            }
            trimmed.startsWith("# ") -> {
                Text(
                    style = RefBody.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
                    color = Colors.Android.Mint,
                    text = inlineMarkdown(trimmed.removePrefix("# ")),
                )
                Spacer(Modifier.height(6.dp))
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
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                val indent = line.length - line.trimStart().length
                Row(modifier = Modifier.padding(start = (indent * 6).dp, top = 2.dp)) {
                    Text(style = RefBody, color = Colors.Android.Green, text = "• ")
                    Text(
                        style = RefBody,
                        color = Colors.Console,
                        text = inlineMarkdown(trimmed.substring(2)),
                    )
                }
                i++
            }
            NUMBERED.matches(trimmed) -> {
                val num = trimmed.substringBefore('.')
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    Text(style = RefBody, color = Colors.Android.Green, text = "$num. ")
                    Text(
                        style = RefBody,
                        color = Colors.Console,
                        text = inlineMarkdown(trimmed.substringAfter(". ")),
                    )
                }
                i++
            }
            else -> {
                // paragraph: gather consecutive plain lines
                val sb = StringBuilder(trimmed)
                i++
                while (i < lines.size) {
                    val t = lines[i].trim()
                    if (
                        t.isEmpty() ||
                            t == "---" ||
                            t.startsWith("#") ||
                            t.startsWith("|") ||
                            t.startsWith("- ") ||
                            t.startsWith("* ") ||
                            NUMBERED.matches(t)
                    )
                        break
                    sb.append(' ').append(t)
                    i++
                }
                Text(
                    modifier = Modifier.padding(vertical = 3.dp),
                    style = RefBody,
                    color = Colors.Console,
                    text = inlineMarkdown(sb.toString()),
                )
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
