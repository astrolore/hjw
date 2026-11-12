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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------------------------
// The explorer card at the top of the Reference: credit-card shaped, rounded like the
// achievement tiles. The picture is the ship leaving a planet; its trail and the card's border
// wear the colour of the explorer's rank.
// ---------------------------------------------------------------------------------------------

class Rank(val title: String, val color: Color)

internal val RANKS =
    listOf(
        Rank("NOOB", Color(0xFF9E9E9E)),
        Rank("APPRENTICE", Color(0xFF34A853)),
        Rank("EXPLORER", Color(0xFF4285F4)),
        Rank("NAVIGATOR", Color(0xFF26C6DA)),
        Rank("PILOT", Color(0xFF9C6BFF)),
        Rank("VETERAN", Color(0xFFFF9800)),
        Rank("LEGEND", Color(0xFFFFD54F)),
    )

/** Index into [RANKS] from how many of the achievements that can be earned are unlocked. */
fun rankIndex(): Int {
    val earnable = ALL_ACHIEVEMENTS.filter { it.earnable }
    val got = earnable.count { Achievements.isUnlocked(it.id) }
    val f = if (earnable.isEmpty()) 0f else got.toFloat() / earnable.size
    return when {
        f >= 1f -> 6
        f >= 0.80f -> 5
        f >= 0.60f -> 4
        f >= 0.40f -> 3
        f >= 0.25f -> 2
        f >= 0.10f -> 1
        else -> 0
    }
}

fun currentRank(): Rank = RANKS[rankIndex()]

/** The colour of the ship's trail: the rank's colour, or the original green when DYNAMIC TRAIL is off. */
fun trailColor(): Color = if (GameSettings.dynamicTrail) currentRank().color else Colors.Track

/** The ship leaving a planet, with stars. [trail] colours the outer flame. */
@Composable
internal fun CardArt(trail: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        fun x(v: Float) = v * s
        // night sky
        drawRect(Color(0xFF202125), Offset.Zero, Size(s, s))
        // square stars
        val stars =
            floatArrayOf(
                55f, 225f, 10f, 207f, 229f, 10f, 250f, 251f, 20f, 129f, 331f, 20f, 202f, 351f, 20f,
                145f, 404f, 20f, 283f, 419f, 10f, 145f, 529f, 10f, 525f, 356f, 10f, 562f, 384f, 20f,
                645f, 404f, 20f, 614f, 443f, 10f, 457f, 454f, 10f, 661f, 206f, 10f, 679f, 294f, 20f,
            )
        var i = 0
        while (i + 2 < stars.size) {
            val sz = stars[i + 2] / 753f
            drawRect(Color.White, Offset(x(stars[i] / 753f), x(stars[i + 1] / 753f)), Size(x(sz), x(sz)))
            i += 3
        }
        // the outer flame (rank colour) and the white core
        val outer =
            Path().apply {
                moveTo(x(0.494f), x(0.350f))
                quadraticBezierTo(x(0.455f), x(0.62f), x(0.21f), x(0.80f))
                lineTo(x(0.77f), x(0.80f))
                quadraticBezierTo(x(0.535f), x(0.62f), x(0.494f), x(0.350f))
                close()
            }
        drawPath(outer, trail)
        val core =
            Path().apply {
                moveTo(x(0.494f), x(0.352f))
                quadraticBezierTo(x(0.478f), x(0.60f), x(0.40f), x(0.76f))
                lineTo(x(0.585f), x(0.76f))
                quadraticBezierTo(x(0.51f), x(0.60f), x(0.494f), x(0.352f))
                close()
            }
        drawPath(core, Color.White)
        drawLine(Color.White, Offset(x(0.494f), x(0.345f)), Offset(x(0.494f), x(0.375f)), x(0.022f), StrokeCap.Round)
        // the planet with its two antennae
        drawLine(Color(0xFF5F6368), Offset(x(0.30f), x(0.665f)), Offset(x(0.352f), x(0.75f)), x(0.068f), StrokeCap.Round)
        drawLine(Color(0xFF5F6368), Offset(x(0.70f), x(0.665f)), Offset(x(0.648f), x(0.75f)), x(0.068f), StrokeCap.Round)
        drawCircle(Color(0xFF5F6368), x(0.396f), Offset(x(0.5f), x(1.113f)))
        // the ship's pod
        drawRoundRect(
            Color(0xFFE8F0EB),
            Offset(x(0.455f), x(0.248f)),
            Size(x(0.09f), x(0.092f)),
            androidx.compose.ui.geometry.CornerRadius(x(0.045f)),
        )
        drawRoundRect(
            Color(0xFF202125),
            Offset(x(0.476f), x(0.308f)),
            Size(x(0.048f), x(0.034f)),
            androidx.compose.ui.geometry.CornerRadius(x(0.012f)),
        )
    }
}

@Composable
internal fun ExplorerCard() {
    // the numbers tick in real time while the card is on screen
    var ticker by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            ticker++
        }
    }
    val stats = remember(ticker) { Achievements.statsLines() }
    val rank = currentRank()
    val context = LocalContext.current
    val version =
        remember {
            try {
                "v" + (context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?")
            } catch (e: Exception) {
                "v?"
            }
        }
    val shape = RoundedCornerShape(15.dp)

    Box(
        modifier =
            Modifier.fillMaxWidth()
                .clip(shape)
                .background(Colors.Eigengrau)
                .border(2.dp, rank.color, shape)
                .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val artShape = RoundedCornerShape(12.dp)
                CardArt(
                    trail = rank.color,
                    modifier = Modifier.size(96.dp).clip(artShape).border(1.5.dp, rank.color, artShape),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    style = MenuText.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = rank.color,
                    text = "[${rank.title}]",
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    style = MenuText.copy(fontSize = 19.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    text = Profile.nickname.ifBlank { "unnamed" },
                )
                Text(
                    style = MenuText.copy(fontSize = 9.sp, lineHeight = 12.sp),
                    color = Colors.Eigengrau4,
                    text = "microverse IR-002S explorer-13",
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    style = MenuText.copy(fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold),
                    color = Colors.Console,
                    text = "STATS:",
                )
                stats.forEach { (label, value) ->
                    Text(
                        style = MenuText.copy(fontSize = 9.sp, lineHeight = 12.sp),
                        color = Colors.Console,
                        text = "$label: $value",
                    )
                }
            }
        }
        Text(
            modifier = Modifier.align(Alignment.BottomEnd),
            style = MenuText.copy(fontSize = 11.sp),
            color = Colors.Eigengrau4,
            text = version,
        )
    }
}
