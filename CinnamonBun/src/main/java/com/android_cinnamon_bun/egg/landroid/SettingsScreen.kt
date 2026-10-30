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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// ---------------------------------------------------------------------------------------------
// MOD: settings page. Two sections: GAMEPLAY and MOD MENU. Everything applies instantly.
// ---------------------------------------------------------------------------------------------

@Composable
private fun SectionHeader(text: String, color: Color) {
    Spacer(Modifier.height(14.dp))
    Text(
        style = MenuText.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
        color = color,
        text = "// $text",
    )
    Spacer(Modifier.height(4.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(color.copy(alpha = 0.5f)))
    Spacer(Modifier.height(4.dp))
}

/** A label with a live value on the right, an optional description, then the slider below. */
@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    description: String?,
    enabled: Boolean,
    accent: Color,
    t: Float,
    snapT: Float?,
    onDragging: (Boolean) -> Unit = {},
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.35f).padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(modifier = Modifier.weight(1f), style = MenuText, color = accent, text = label)
            Text(style = MenuText, color = accent, text = valueText)
        }
        if (description != null) {
            Text(
                style = MenuText.copy(fontSize = 10.sp, lineHeight = 13.sp),
                color = Colors.Eigengrau4,
                text = description,
            )
        }
        ConsoleSlider(
            t = t,
            snapT = snapT,
            enabled = enabled,
            accent = accent,
            onDragging = onDragging,
            onChange = onChange,
        )
    }
}

@Composable
internal fun SettingsScreen(onClose: () -> Unit, onModConfirmed: () -> Unit) {
    var previewing by remember { mutableStateOf(false) }
    var showModWarning by remember { mutableStateOf(false) }

    val mod = GameSettings.modMenu
    val hidable = Modifier.alpha(if (previewing) 0f else 1f)
    val modColor = Colors.Android.Chartreuse

    Box(modifier = Modifier.fillMaxSize()) {
        OverlayScreen(
            title = "SETTINGS",
            onClose = onClose,
            scroll = true,
            transparentBg = previewing,
        ) {
            // ------------------------------------------------------------ GAMEPLAY
            Column(modifier = hidable) {
                SectionHeader("GAMEPLAY", Colors.Console)

                ConsoleToggle(
                    label = "DYNAMIC CAMERA",
                    description = "Zoom follows your speed. Shake and kick on thrust and landing.",
                    checked = GameSettings.dynamicCamera,
                ) {
                    GameSettings.dynamicCamera = it
                    GameSettings.save()
                }
                SliderRow(
                    label = "CAMERA SHAKE",
                    valueText = "${(GameSettings.shakeStrength * 100).toInt()}%",
                    description = null,
                    enabled = GameSettings.dynamicCamera,
                    accent = Colors.Console,
                    t = GameSettings.shakeStrength,
                    snapT = null,
                    onDragging = { if (!it) GameSettings.save() },
                ) {
                    GameSettings.shakeStrength = it
                }
                ConsoleToggle(
                    label = "HAPTICS",
                    description = "Vibration on landing, bounces and thruster start.",
                    checked = GameSettings.haptics,
                ) {
                    GameSettings.haptics = it
                    GameSettings.save()
                }
                ConsoleToggle(
                    label = "WORLD GRID",
                    description = "The coordinate grid behind everything.",
                    checked = GameSettings.showGrid,
                ) {
                    GameSettings.showGrid = it
                    GameSettings.save()
                }
                ConsoleToggle(
                    label = "ORBIT LINES",
                    description = "The circles planets travel on.",
                    checked = GameSettings.showOrbits,
                ) {
                    GameSettings.showOrbits = it
                    GameSettings.save()
                }
                ConsoleToggle(
                    label = "SHOW HUD",
                    description = "Hides all telemetry. SHOW HUD returns to the top of the menu.",
                    checked = GameSettings.showHud,
                ) {
                    GameSettings.showHud = it
                    GameSettings.save()
                }
                ConsoleToggle(
                    label = "KEEP SCREEN AWAKE",
                    description = "Stops the screen from timing out while you fly.",
                    checked = GameSettings.keepScreenOn,
                ) {
                    GameSettings.keepScreenOn = it
                    GameSettings.save()
                }
                ConsoleToggle(
                    label = "SHIP NOTIFICATION",
                    description = "A status notification while you fly.",
                    checked = GameSettings.shipNotification,
                ) {
                    GameSettings.shipNotification = it
                    GameSettings.save()
                }
            }

            // ------------------------------------------------------------ MOD MENU
            Column(modifier = hidable) {
                SectionHeader("MOD MENU", modColor)

                ConsoleToggle(
                    label = "MOD MENU",
                    description =
                        "Unlocks everything below. Using it in a world turns achievements off for that world.",
                    checked = mod,
                    accent = modColor,
                ) { on ->
                    if (on) {
                        showModWarning = true
                    } else {
                        GameSettings.modMenu = false
                        GameSettings.save()
                    }
                }
                ConsoleToggle(
                    label = "REPLACE AUTO WITH REVEAL",
                    description = "Off shows the AUTO button. On shows REVEAL, which uncovers every planet.",
                    checked = GameSettings.revealButton,
                    enabled = mod,
                    accent = modColor,
                ) {
                    GameSettings.revealButton = it
                    GameSettings.save()
                }
            }

            // FOV stays visible while you drag it, so you can see the view behind the page.
            run {
                val godZoom = GameSettings.godZoom
                val t = fovSliderT(GameSettings.fovZoom, GameSettings.fovGods, godZoom)
                val defT = fovSliderT(DEFAULT_CAMERA_ZOOM, false, godZoom)
                val valueText =
                    when {
                        GameSettings.fovGods -> "GOD'S EYE"
                        abs(t - defT) < 0.002f -> "DEFAULT"
                        else -> "ZOOM %.3g".format(GameSettings.fovZoom)
                    }
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(
                                if (previewing) Colors.Eigengrau.copy(alpha = 0.85f) else Color.Transparent
                            )
                ) {
                    SliderRow(
                        label = "FOV",
                        valueText = valueText,
                        description = "How much of the universe you see. Drag to preview.",
                        enabled = mod,
                        accent = modColor,
                        t = t,
                        snapT = defT,
                        onDragging = {
                            previewing = it
                            if (!it) GameSettings.save()
                        },
                    ) { nt ->
                        if (nt >= 0.985f) {
                            GameSettings.fovGods = true
                        } else {
                            GameSettings.fovGods = false
                            GameSettings.fovZoom =
                                if (abs(nt - defT) < 0.0001f) DEFAULT_CAMERA_ZOOM
                                else fovZoomForT(nt, godZoom)
                        }
                    }
                }
            }

            Column(modifier = hidable) {
                val engineDefT = (1f - ENGINE_POWER_MIN) / (ENGINE_POWER_MAX - ENGINE_POWER_MIN)
                SliderRow(
                    label = "ENGINE POWER",
                    valueText = "${(GameSettings.enginePower * 100).toInt()}%",
                    description = "Thrust strength. 100% is the stock game.",
                    enabled = mod,
                    accent = modColor,
                    t = (GameSettings.enginePower - ENGINE_POWER_MIN) /
                        (ENGINE_POWER_MAX - ENGINE_POWER_MIN),
                    snapT = engineDefT,
                    onDragging = { if (!it) GameSettings.save() },
                ) { nt ->
                    GameSettings.enginePower =
                        if (abs(nt - engineDefT) < 0.0001f) 1f
                        else ENGINE_POWER_MIN + nt * (ENGINE_POWER_MAX - ENGINE_POWER_MIN)
                }

                val speedDefT = (CRAFT_SPEED_LIMIT - SPEED_LIMIT_MIN) / (SPEED_LIMIT_MAX - SPEED_LIMIT_MIN)
                SliderRow(
                    label = "SPEED LIMIT",
                    valueText = "%,d".format(GameSettings.speedLimit.toInt()),
                    description = "Top speed of the ship. 5,000 is the stock game.",
                    enabled = mod,
                    accent = modColor,
                    t = (GameSettings.speedLimit - SPEED_LIMIT_MIN) / (SPEED_LIMIT_MAX - SPEED_LIMIT_MIN),
                    snapT = speedDefT,
                    onDragging = { if (!it) GameSettings.save() },
                ) { nt ->
                    GameSettings.speedLimit =
                        if (abs(nt - speedDefT) < 0.0001f) CRAFT_SPEED_LIMIT
                        else SPEED_LIMIT_MIN + nt * (SPEED_LIMIT_MAX - SPEED_LIMIT_MIN)
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        if (showModWarning) {
            ThemedPrompt(
                title = "WARNING",
                message =
                    "Using the mod menu in a world will permanently disable the ability to earn achievements in that world.",
                buttons =
                    listOf(
                        "CONTINUE" to
                            {
                                GameSettings.modMenu = true
                                GameSettings.save()
                                showModWarning = false
                                onModConfirmed()
                            },
                        "GO BACK" to { showModWarning = false },
                    ),
            )
        }
    }
}
