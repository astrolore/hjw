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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val MenuText =
    TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        lineHeight = 16.sp,
    )

/** A console button that also reacts to a long press (used for a hidden shortcut). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ConsoleButtonLong(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MenuText,
    color: Color,
    bgColor: Color,
    borderColor: Color,
    text: String,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    Text(
        style = textStyle,
        color = color,
        modifier =
            modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .background(color = bgColor)
                .border(width = 1.dp, color = borderColor)
                .padding(6.dp)
                .minimumInteractiveComponentSize(),
        text = text,
    )
}

/**
 * A console button that keeps firing while it is held. [onStep] gets a running step count (0 for
 * the first tap, then 1, 2, 3...) so the caller can accelerate.
 */
@Composable
internal fun HoldButton(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Colors.Console,
    borderColor: Color = Colors.Eigengrau3,
    onStep: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val step by rememberUpdatedState(onStep)

    Text(
        style = MenuText,
        color = color,
        modifier =
            modifier
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        val job =
                            scope.launch {
                                var n = 0
                                step(n)
                                delay(450)
                                while (true) {
                                    n++
                                    step(n)
                                    // starts at ~8 steps per second, ends at ~40
                                    delay((130 - n * 4).coerceAtLeast(25).toLong())
                                }
                            }
                        waitForUpOrCancellation()
                        job.cancel()
                    }
                }
                .border(width = 1.dp, color = borderColor)
                .padding(6.dp)
                .minimumInteractiveComponentSize(),
        text = text,
    )
}

/** A themed on/off row: label, optional description, and a square-knob switch. */
@Composable
internal fun ConsoleToggle(
    label: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    accent: Color = Colors.Console,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .alpha(if (enabled) 1f else 0.35f)
                .clickable(enabled = enabled) { onChange(!checked) }
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(style = MenuText, color = accent, text = label)
            if (description != null) {
                Text(
                    style = MenuText.copy(fontSize = 10.sp, lineHeight = 13.sp),
                    color = Colors.Eigengrau4,
                    text = description,
                )
            }
        }
        Box(
            modifier =
                Modifier.size(width = 58.dp, height = 26.dp)
                    .background(if (checked) accent.copy(alpha = 0.22f) else Color.Transparent)
                    .border(1.dp, accent)
                    .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (checked) Arrangement.SpaceBetween else Arrangement.SpaceBetween,
            ) {
                if (checked) {
                    Text(style = MenuText.copy(fontSize = 10.sp), color = accent, text = " ON")
                    Box(Modifier.size(18.dp).background(accent))
                } else {
                    Box(Modifier.size(18.dp).background(accent.copy(alpha = 0.55f)))
                    Text(style = MenuText.copy(fontSize = 10.sp), color = accent, text = "OFF ")
                }
            }
        }
    }
}

/**
 * A themed horizontal slider. [t] runs from 0 to 1. A notch is drawn at [snapT] (if any) and the
 * thumb snaps to it when you get close. [onDragging] tells the caller when a drag starts and ends.
 */
@Composable
internal fun ConsoleSlider(
    modifier: Modifier = Modifier,
    t: Float,
    snapT: Float? = null,
    enabled: Boolean = true,
    accent: Color = Colors.Console,
    onDragging: (Boolean) -> Unit = {},
    onChange: (Float) -> Unit,
) {
    var widthPx by remember { mutableIntStateOf(0) }
    val currentOnChange by rememberUpdatedState(onChange)
    val currentOnDragging by rememberUpdatedState(onDragging)

    fun valueAt(x: Float, pad: Float): Float {
        val span = (widthPx - 2 * pad).coerceAtLeast(1f)
        var v = ((x - pad) / span).coerceIn(0f, 1f)
        if (snapT != null && abs(v - snapT) < 0.025f) v = snapT
        return v
    }

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp)
                .alpha(if (enabled) 1f else 0.35f)
                .onSizeChanged { widthPx = it.width }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pad = 12.dp.toPx()
                        currentOnDragging(true)
                        currentOnChange(valueAt(down.position.x, pad))
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            currentOnChange(valueAt(change.position.x, pad))
                            change.consume()
                            if (!change.pressed) break
                        }
                        currentOnDragging(false)
                    }
                }
    ) {
        val pad = 12.dp.toPx()
        val cy = size.height / 2f
        val left = pad
        val right = size.width - pad
        val x = left + (right - left) * t.coerceIn(0f, 1f)

        // track
        drawLine(accent.copy(alpha = 0.35f), Offset(left, cy), Offset(right, cy), strokeWidth = 2.dp.toPx())
        // filled part
        drawLine(accent, Offset(left, cy), Offset(x, cy), strokeWidth = 2.dp.toPx())
        // notch for the default value
        if (snapT != null) {
            val nx = left + (right - left) * snapT
            drawLine(
                Colors.Android.Chartreuse,
                Offset(nx, cy - 11.dp.toPx()),
                Offset(nx, cy + 11.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
            )
        }
        // thumb
        drawRect(
            color = accent,
            topLeft = Offset(x - 5.dp.toPx(), cy - 11.dp.toPx()),
            size = Size(10.dp.toPx(), 22.dp.toPx()),
        )
    }
}

/**
 * A full-screen themed warning: warning sign, message, and stacked buttons (each full width, one
 * above the other). Taps outside the box are swallowed.
 */
@Composable
internal fun ThemedPrompt(
    title: String,
    message: String,
    accent: Color = Colors.Android.Chartreuse,
    buttons: List<Pair<String, () -> Unit>>,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect("prompt") { shown = true }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Color(0xB0000000))
                .pointerInput(Unit) { detectTapGestures {} },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(visible = shown, enter = flickerFadeIn) {
            Column(
                modifier =
                    Modifier.fillMaxWidth(0.88f)
                        .widthIn(max = 380.dp)
                        .background(Colors.Eigengrau)
                        .border(1.dp, accent)
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WarningSign(accent)
                Spacer(Modifier.height(10.dp))
                Text(
                    style = MenuText.copy(fontSize = 14.sp, textAlign = TextAlign.Center),
                    color = accent,
                    text = title,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    style = MenuText.copy(textAlign = TextAlign.Center),
                    color = Colors.Console,
                    text = message,
                )
                Spacer(Modifier.height(14.dp))
                buttons.forEachIndexed { index, (label, action) ->
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    ConsoleButton(
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MenuText.copy(textAlign = TextAlign.Center),
                        color = if (index == 0) Colors.Eigengrau else Colors.Console,
                        bgColor = if (index == 0) accent else Color.Transparent,
                        borderColor = if (index == 0) accent else Colors.Console,
                        text = label,
                        onClick = action,
                    )
                }
            }
        }
    }
}

/** An outlined warning triangle with an exclamation mark, in the game's line style. */
@Composable
private fun WarningSign(color: Color) {
    Canvas(modifier = Modifier.size(54.dp)) {
        val w = size.width
        val h = size.height
        val path =
            Path().apply {
                moveTo(w * 0.5f, h * 0.08f)
                lineTo(w * 0.95f, h * 0.9f)
                lineTo(w * 0.05f, h * 0.9f)
                close()
            }
        drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
        // exclamation mark
        drawLine(color, Offset(w * 0.5f, h * 0.36f), Offset(w * 0.5f, h * 0.62f), strokeWidth = 3.dp.toPx())
        drawCircle(color, radius = 2.2.dp.toPx(), center = Offset(w * 0.5f, h * 0.76f))
    }
}
