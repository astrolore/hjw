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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
// Achievement art: a small multi-colour drawing on a 0..1 square for every achievement. Locked
// tiles never draw it (they show a glitching ???), so these are always the unlocked colours.
// The ship in the art is the same shape as the ship in the game.
// ---------------------------------------------------------------------------------------------

private val Blue = Color(0xFF4285F4)
private val Green = Color(0xFF34A853)
private val Yellow = Color(0xFFFBBC05)
private val Orange = Color(0xFFFF9800)
private val Red = Color(0xFFEA4335)
private val Pink = Color(0xFFE08DE8)
private val Violet = Color(0xFF9C6BFF)
private val Teal = Color(0xFF26C6DA)
private val Grey = Color(0xFF9E9E9E)
private val White = Color(0xFFF2F2F7)
private val Brown = Color(0xFF8D6E63)
private val Ink = Color(0xFF16161D)

@Composable
fun AchievementArt(id: String, unlocked: Boolean, modifier: Modifier = Modifier) {
    if (id == "sprint_finish") {
        // the art is the time it took you, MM:SS (the world's clock starts when it loads)
        val s = Achievements.sprintSeconds()
        Box(
            modifier =
                modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0B2A14))
                    .border(1.dp, Colors.Track, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "%02d:%02d".format(s / 60, s % 60),
                color = Colors.Track,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.sp,
            )
        }
    } else {
        Canvas(modifier = modifier) { ArtPainter(this, minOf(size.width, size.height)).draw(id) }
    }
}

private class ArtPainter(private val ds: DrawScope, private val side: Float) {
    private fun p(x: Float, y: Float) = Offset(x * side, y * side)

    private fun pathOf(pts: FloatArray, close: Boolean): Path =
        Path().apply {
            moveTo(pts[0] * side, pts[1] * side)
            var i = 2
            while (i + 1 < pts.size) {
                lineTo(pts[i] * side, pts[i + 1] * side)
                i += 2
            }
            if (close) close()
        }

    private fun fc(c: Color, x: Float, y: Float, r: Float) = ds.drawCircle(c, r * side, p(x, y))

    private fun sc(c: Color, x: Float, y: Float, r: Float, w: Float = 0.045f) =
        ds.drawCircle(c, r * side, p(x, y), style = Stroke(w * side))

    private fun poly(c: Color, vararg pts: Float) = ds.drawPath(pathOf(pts, true), c)

    private fun polyS(c: Color, w: Float, vararg pts: Float) =
        ds.drawPath(pathOf(pts, true), c, style = Stroke(w * side, join = StrokeJoin.Round))

    private fun pl(c: Color, w: Float, vararg pts: Float) =
        ds.drawPath(pathOf(pts, false), c, style = Stroke(w * side, cap = StrokeCap.Round, join = StrokeJoin.Round))

    private fun ln(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, w: Float = 0.045f) =
        ds.drawLine(c, p(x1, y1), p(x2, y2), w * side, StrokeCap.Round)

    private fun rect(c: Color, x: Float, y: Float, w: Float, h: Float) =
        ds.drawRect(c, p(x, y), Size(w * side, h * side))

    private fun rr(c: Color, x: Float, y: Float, w: Float, h: Float, r: Float) =
        ds.drawRoundRect(c, p(x, y), Size(w * side, h * side), androidx.compose.ui.geometry.CornerRadius(r * side))

    private fun arc(c: Color, cx: Float, cy: Float, r: Float, start: Float, sweep: Float, w: Float = 0.045f) =
        ds.drawArc(
            c,
            start,
            sweep,
            false,
            Offset((cx - r) * side, (cy - r) * side),
            Size(2 * r * side, 2 * r * side),
            style = Stroke(w * side, cap = StrokeCap.Round),
        )

    private fun rays(c: Color, cx: Float, cy: Float, r1: Float, r2: Float, n: Int, w: Float = 0.04f) {
        for (i in 0 until n) {
            val a = (i * 2 * PI / n).toFloat()
            ln(c, cx + r1 * cos(a), cy + r1 * sin(a), cx + r2 * cos(a), cy + r2 * sin(a), w)
        }
    }

    /** The game's ship (a pointed arrow with a notch), size [s], pointing along [ang]. */
    private fun ship(x: Float, y: Float, s: Float, ang: Float, body: Color = White, flame: Boolean = false) {
        fun rx(px: Float, py: Float) = x + s * (px * cos(ang) - py * sin(ang))
        fun ry(px: Float, py: Float) = y + s * (px * sin(ang) + py * cos(ang))
        if (flame) {
            poly(Orange, rx(-0.5f, -0.2f), ry(-0.5f, -0.2f), rx(-1.4f, 0f), ry(-1.4f, 0f), rx(-0.5f, 0.2f), ry(-0.5f, 0.2f))
        }
        poly(body, rx(1f, 0f), ry(1f, 0f), rx(-0.7f, 0.65f), ry(-0.7f, 0.65f), rx(-0.35f, 0f), ry(-0.35f, 0f), rx(-0.7f, -0.65f), ry(-0.7f, -0.65f))
    }

    private fun star4(c: Color, x: Float, y: Float, r: Float) =
        poly(c, x, y - r, x + r * 0.3f, y - r * 0.3f, x + r, y, x + r * 0.3f, y + r * 0.3f, x, y + r, x - r * 0.3f, y + r * 0.3f, x - r, y, x - r * 0.3f, y - r * 0.3f)

    fun draw(id: String) {
        when (id) {
            // ------------------------------------------------------------ the first dozen
            "first_contact" -> {
                fc(Blue, 0.42f, 0.66f, 0.28f)
                fc(Green, 0.32f, 0.72f, 0.08f)
                fc(Grey.copy(alpha = 0.7f), 0.74f, 0.36f, 0.05f)
                fc(Grey.copy(alpha = 0.5f), 0.84f, 0.26f, 0.04f)
                ship(0.62f, 0.46f, 0.09f, -0.78f)
            }
            "cartographer" -> {
                poly(Yellow, 0.08f, 0.22f, 0.36f, 0.3f, 0.64f, 0.2f, 0.92f, 0.28f, 0.92f, 0.8f, 0.64f, 0.72f, 0.36f, 0.82f, 0.08f, 0.74f)
                ln(Brown, 0.36f, 0.3f, 0.36f, 0.82f, 0.025f)
                ln(Brown, 0.64f, 0.2f, 0.64f, 0.72f, 0.025f)
                ln(Red, 0.7f, 0.42f, 0.84f, 0.58f, 0.05f)
                ln(Red, 0.84f, 0.42f, 0.7f, 0.58f, 0.05f)
                fc(Pink, 0.28f, 0.64f, 0.025f)
                fc(Pink, 0.4f, 0.58f, 0.025f)
                fc(Pink, 0.52f, 0.5f, 0.025f)
                fc(Pink, 0.62f, 0.52f, 0.025f)
                ship(0.17f, 0.5f, 0.07f, -0.5f)
            }
            "full_send" -> {
                ln(Orange, 0.08f, 0.34f, 0.48f, 0.34f)
                ln(Yellow, 0.04f, 0.5f, 0.5f, 0.5f)
                ln(Orange, 0.08f, 0.66f, 0.48f, 0.66f)
                ship(0.72f, 0.5f, 0.22f, 0f, flame = true)
            }
            "orbiter" -> {
                fc(Blue, 0.5f, 0.5f, 0.2f)
                fc(Green, 0.45f, 0.45f, 0.06f)
                ds.drawCircle(Teal, 0.36f * side, p(0.5f, 0.5f), style = Stroke(0.025f * side))
                ship(0.5f + 0.36f * cos(-0.8f), 0.5f + 0.36f * sin(-0.8f), 0.09f, -0.8f + 1.57f)
            }
            "sunbather" -> {
                rays(Orange, 0.5f, 0.5f, 0.3f, 0.44f, 8)
                fc(Yellow, 0.5f, 0.5f, 0.24f)
                ship(0.5f, 0.5f, 0.1f, -0.6f)
            }
            "edge_lord" -> {
                ds.drawCircle(Violet, 0.4f * side, p(0.5f, 0.5f), style = Stroke(0.05f * side))
                fc(Violet.copy(alpha = 0.18f), 0.5f, 0.5f, 0.37f)
                ship(0.5f + 0.4f * cos(-0.6f), 0.5f + 0.4f * sin(-0.6f), 0.1f, -0.6f + 1.57f)
            }
            "time_traveler" -> {
                fc(Pink, 0.55f, 0.52f, 0.32f)
                ln(White, 0.55f, 0.52f, 0.55f, 0.3f)
                ln(White, 0.55f, 0.52f, 0.7f, 0.58f)
                arc(White, 0.5f, 0.52f, 0.4f, 150f, 120f, 0.05f)
                poly(White, 0.06f, 0.4f, 0.2f, 0.34f, 0.18f, 0.5f)
            }
            "soft_touch" -> {
                fc(Green, 0.5f, 1.0f, 0.5f)
                fc(Blue.copy(alpha = 0.5f), 0.36f, 0.82f, 0.05f)
                poly(White, 0.28f, 0.52f, 0.52f, 0.2f, 0.74f, 0.3f, 0.6f, 0.52f)
                ln(Grey, 0.3f, 0.56f, 0.7f, 0.24f, 0.03f)
            }
            "slingshot" -> {
                fc(Violet, 0.5f, 0.55f, 0.2f)
                arc(Orange, 0.5f, 0.55f, 0.34f, 170f, 200f, 0.05f)
                poly(Orange, 0.78f, 0.22f, 0.92f, 0.34f, 0.76f, 0.38f)
                ship(0.14f, 0.55f, 0.07f, -1.57f)
            }
            "uno_planeta" -> {
                fc(Green, 0.5f, 0.5f, 0.38f)
                pl(White, 0.07f, 0.42f, 0.38f, 0.54f, 0.3f, 0.54f, 0.7f)
                ln(White, 0.42f, 0.7f, 0.66f, 0.7f, 0.07f)
            }
            "creator" -> {
                fc(Blue, 0.38f, 0.64f, 0.24f)
                fc(Green, 0.3f, 0.7f, 0.07f)
                poly(Yellow, 0.52f, 0.5f, 0.62f, 0.4f, 0.9f, 0.16f, 0.84f, 0.1f)
                poly(Pink, 0.52f, 0.5f, 0.5f, 0.58f, 0.58f, 0.52f)
            }
            // ------------------------------------------------------------ legendary
            "redacted" -> {
                ln(White, 0.1f, 0.24f, 0.9f, 0.24f, 0.05f)
                ln(White, 0.1f, 0.42f, 0.7f, 0.42f, 0.05f)
                ln(White, 0.1f, 0.78f, 0.8f, 0.78f, 0.05f)
                rr(Grey, 0.08f, 0.5f, 0.84f, 0.2f, 0.03f)
            }
            "icecream" -> {
                poly(Orange, 0.3f, 0.52f, 0.7f, 0.52f, 0.5f, 0.94f)
                ln(Brown, 0.36f, 0.62f, 0.62f, 0.62f, 0.025f)
                ln(Brown, 0.42f, 0.74f, 0.58f, 0.74f, 0.025f)
                fc(Pink, 0.37f, 0.46f, 0.15f)
                fc(Teal, 0.63f, 0.46f, 0.15f)
                fc(Yellow, 0.5f, 0.3f, 0.16f)
                fc(Red, 0.5f, 0.13f, 0.05f)
            }
            "spoiler_alert" -> {
                poly(White, 0.06f, 0.55f, 0.3f, 0.34f, 0.5f, 0.3f, 0.7f, 0.34f, 0.94f, 0.55f, 0.7f, 0.76f, 0.5f, 0.8f, 0.3f, 0.76f)
                fc(Blue, 0.5f, 0.55f, 0.15f)
                fc(Ink, 0.5f, 0.55f, 0.07f)
                ln(Red, 0.84f, 0.08f, 0.84f, 0.24f, 0.06f)
                fc(Red, 0.84f, 0.32f, 0.035f)
            }
            "purist" -> {
                fc(White, 0.5f, 0.5f, 0.38f)
                pl(Green, 0.09f, 0.32f, 0.52f, 0.46f, 0.66f, 0.7f, 0.36f)
            }
            "lucky_7" -> {
                fc(Green, 0.36f, 0.36f, 0.22f)
                fc(Green, 0.64f, 0.36f, 0.22f)
                fc(Green, 0.36f, 0.64f, 0.22f)
                fc(Green, 0.64f, 0.64f, 0.22f)
                pl(White, 0.07f, 0.4f, 0.34f, 0.62f, 0.34f, 0.5f, 0.7f)
            }
            "rtfm" -> {
                poly(Blue, 0.04f, 0.26f, 0.5f, 0.34f, 0.96f, 0.26f, 0.96f, 0.8f, 0.5f, 0.88f, 0.04f, 0.8f)
                poly(White, 0.1f, 0.22f, 0.48f, 0.3f, 0.48f, 0.82f, 0.1f, 0.74f)
                poly(White, 0.9f, 0.22f, 0.52f, 0.3f, 0.52f, 0.82f, 0.9f, 0.74f)
                ln(Grey, 0.18f, 0.4f, 0.4f, 0.45f, 0.03f)
                ln(Grey, 0.18f, 0.52f, 0.4f, 0.57f, 0.03f)
                ln(Grey, 0.6f, 0.45f, 0.82f, 0.4f, 0.03f)
            }
            "editor" -> {
                rr(White, 0.14f, 0.1f, 0.6f, 0.8f, 0.04f)
                ln(Grey, 0.24f, 0.3f, 0.62f, 0.3f, 0.03f)
                ln(Grey, 0.24f, 0.44f, 0.62f, 0.44f, 0.03f)
                poly(Yellow, 0.46f, 0.7f, 0.76f, 0.34f, 0.9f, 0.46f, 0.6f, 0.82f)
                poly(Pink, 0.46f, 0.7f, 0.6f, 0.82f, 0.4f, 0.88f)
            }
            "archivist" -> {
                poly(Blue, 0.1f, 0.1f, 0.76f, 0.1f, 0.9f, 0.24f, 0.9f, 0.9f, 0.1f, 0.9f)
                rect(White, 0.22f, 0.5f, 0.56f, 0.4f)
                rect(Grey, 0.3f, 0.1f, 0.34f, 0.22f)
                ln(Blue, 0.3f, 0.64f, 0.7f, 0.64f, 0.03f)
                ln(Blue, 0.3f, 0.76f, 0.7f, 0.76f, 0.03f)
            }
            "y2k" -> {
                rr(Grey, 0.1f, 0.12f, 0.8f, 0.58f, 0.05f)
                rr(Ink, 0.17f, 0.19f, 0.66f, 0.44f, 0.03f)
                rect(Grey, 0.4f, 0.7f, 0.2f, 0.1f)
                rect(Grey, 0.28f, 0.8f, 0.44f, 0.07f)
                fc(Red, 0.5f, 0.42f, 0.08f)
                ln(Red, 0.38f, 0.34f, 0.44f, 0.38f, 0.03f)
                ln(Red, 0.62f, 0.34f, 0.56f, 0.38f, 0.03f)
                ln(Red, 0.38f, 0.5f, 0.44f, 0.46f, 0.03f)
                ln(Red, 0.62f, 0.5f, 0.56f, 0.46f, 0.03f)
            }
            "leap_of_faith" -> {
                rr(White, 0.1f, 0.18f, 0.8f, 0.7f, 0.05f)
                rect(Red, 0.1f, 0.18f, 0.8f, 0.16f)
                arc(Green, 0.5f, 0.78f, 0.28f, 190f, 160f, 0.055f)
                poly(Green, 0.76f, 0.62f, 0.9f, 0.74f, 0.7f, 0.78f)
            }
            "cosmic_joke" -> {
                fc(Blue, 0.34f, 0.34f, 0.22f)
                fc(Blue, 0.66f, 0.34f, 0.22f)
                poly(Blue, 0.13f, 0.44f, 0.87f, 0.44f, 0.5f, 0.88f)
                fc(White.copy(alpha = 0.55f), 0.28f, 0.28f, 0.05f)
            }
            "triple_transit" -> {
                fc(Yellow, 0.5f, 0.5f, 0.3f)
                ln(White, 0.04f, 0.3f, 0.96f, 0.3f, 0.04f)
                ln(White, 0.04f, 0.5f, 0.96f, 0.5f, 0.04f)
                ln(White, 0.04f, 0.7f, 0.96f, 0.7f, 0.04f)
            }
            "bullseye" -> {
                fc(Red, 0.5f, 0.5f, 0.4f)
                fc(White, 0.5f, 0.5f, 0.28f)
                fc(Red, 0.5f, 0.5f, 0.16f)
                fc(White, 0.5f, 0.5f, 0.06f)
                ln(Yellow, 0.5f, 0.5f, 0.9f, 0.1f, 0.05f)
                poly(Yellow, 0.82f, 0.06f, 0.94f, 0.06f, 0.94f, 0.18f)
            }
            "crash_test" -> {
                ship(0.46f, 0.56f, 0.24f, -0.78f, body = White)
                star4(Yellow, 0.82f, 0.2f, 0.14f)
                star4(Yellow, 0.16f, 0.3f, 0.09f)
                star4(Teal, 0.2f, 0.82f, 0.08f)
                star4(Pink, 0.86f, 0.74f, 0.07f)
            }
            "pinball_wizard" -> {
                fc(Pink, 0.26f, 0.3f, 0.17f)
                fc(Pink, 0.74f, 0.3f, 0.17f)
                fc(White, 0.5f, 0.74f, 0.1f)
                ln(Yellow, 0.1f, 0.62f, 0.2f, 0.56f, 0.035f)
                ln(Yellow, 0.9f, 0.62f, 0.8f, 0.56f, 0.035f)
                ln(Teal, 0.5f, 0.52f, 0.5f, 0.6f, 0.03f)
            }
            "houston" -> {
                ln(Grey, 0.5f, 0.2f, 0.26f, 0.9f, 0.05f)
                ln(Grey, 0.5f, 0.2f, 0.74f, 0.9f, 0.05f)
                ln(Grey, 0.36f, 0.56f, 0.64f, 0.56f, 0.04f)
                ln(Grey, 0.31f, 0.72f, 0.69f, 0.72f, 0.04f)
                fc(Grey, 0.5f, 0.16f, 0.07f)
                arc(Grey, 0.5f, 0.16f, 0.16f, 200f, 140f, 0.03f)
                pl(Red, 0.04f, 0.64f, 0.38f, 0.5f, 0.5f, 0.56f, 0.62f, 0.5f, 0.74f)
            }
            "hold_my_controller" -> {
                ln(Grey, 0.4f, 0.74f, 0.4f, 0.4f, 0.07f)
                fc(Red, 0.4f, 0.34f, 0.15f)
                rr(Grey, 0.12f, 0.74f, 0.56f, 0.16f, 0.04f)
                sc(White, 0.78f, 0.3f, 0.14f, 0.04f)
                ln(White, 0.78f, 0.3f, 0.78f, 0.22f, 0.035f)
                ln(White, 0.78f, 0.3f, 0.84f, 0.32f, 0.035f)
            }
            "overqualified" -> {
                poly(Ink, 0.5f, 0.2f, 0.94f, 0.42f, 0.5f, 0.64f, 0.06f, 0.42f)
                polyS(White, 0.03f, 0.5f, 0.2f, 0.94f, 0.42f, 0.5f, 0.64f, 0.06f, 0.42f)
                pl(White, 0.03f, 0.26f, 0.54f, 0.26f, 0.72f, 0.5f, 0.82f, 0.74f, 0.72f, 0.74f, 0.54f)
                ln(Yellow, 0.88f, 0.46f, 0.88f, 0.74f, 0.035f)
                fc(Yellow, 0.88f, 0.78f, 0.05f)
            }
            "autopilot_passenger" -> {
                rr(Blue, 0.08f, 0.2f, 0.26f, 0.62f, 0.06f)
                rr(Blue, 0.08f, 0.62f, 0.5f, 0.2f, 0.05f)
                sc(White, 0.72f, 0.42f, 0.2f, 0.05f)
                fc(White, 0.72f, 0.42f, 0.04f)
                ln(White, 0.72f, 0.62f, 0.72f, 0.82f, 0.05f)
            }
            "gps_duh" -> {
                ln(Blue, 0.1f, 0.84f, 0.66f, 0.36f, 0.035f)
                poly(Red, 0.72f, 0.1f, 0.88f, 0.28f, 0.72f, 0.5f, 0.56f, 0.28f)
                fc(White, 0.72f, 0.26f, 0.06f)
                rr(White, 0.12f, 0.66f, 0.2f, 0.22f, 0.06f)
                ln(White, 0.16f, 0.66f, 0.16f, 0.58f, 0.04f)
                ln(White, 0.22f, 0.66f, 0.22f, 0.56f, 0.04f)
                ln(White, 0.28f, 0.66f, 0.28f, 0.58f, 0.04f)
            }
            "name_drop" -> {
                rr(Teal, 0.1f, 0.1f, 0.64f, 0.24f, 0.05f)
                rr(Pink, 0.26f, 0.38f, 0.64f, 0.24f, 0.05f)
                rr(Yellow, 0.1f, 0.66f, 0.64f, 0.24f, 0.05f)
                ln(White, 0.18f, 0.22f, 0.6f, 0.22f, 0.04f)
                ln(White, 0.34f, 0.5f, 0.76f, 0.5f, 0.04f)
                ln(White, 0.18f, 0.78f, 0.6f, 0.78f, 0.04f)
            }
            "stuck_landing" -> {
                fc(Blue, 0.5f, 1.05f, 0.4f)
                ln(Green, 0.34f, 0.62f, 0.34f, 0.12f, 0.06f)
                poly(Green, 0.24f, 0.22f, 0.34f, 0.06f, 0.44f, 0.22f)
                ln(Green, 0.66f, 0.1f, 0.66f, 0.56f, 0.06f)
                poly(Green, 0.56f, 0.46f, 0.66f, 0.64f, 0.76f, 0.46f)
            }
            "developer_mode" -> {
                pl(Violet, 0.07f, 0.32f, 0.26f, 0.1f, 0.5f, 0.32f, 0.74f)
                pl(Violet, 0.07f, 0.68f, 0.26f, 0.9f, 0.5f, 0.68f, 0.74f)
                ln(Pink, 0.58f, 0.2f, 0.42f, 0.8f, 0.07f)
                rect(Yellow, 0.3f, 0.86f, 0.4f, 0.05f)
            }
            "petridish" -> {
                fc(Teal.copy(alpha = 0.18f), 0.5f, 0.5f, 0.38f)
                sc(White, 0.5f, 0.5f, 0.38f, 0.055f)
                fc(Green, 0.5f, 0.5f, 0.12f)
                fc(Blue, 0.46f, 0.46f, 0.04f)
                fc(White.copy(alpha = 0.7f), 0.3f, 0.34f, 0.03f)
            }
            "happy_birthday" -> {
                rr(Pink, 0.14f, 0.52f, 0.72f, 0.36f, 0.05f)
                rect(White, 0.14f, 0.64f, 0.72f, 0.06f)
                rect(Yellow, 0.46f, 0.28f, 0.08f, 0.24f)
                poly(Orange, 0.5f, 0.1f, 0.58f, 0.22f, 0.5f, 0.28f, 0.42f, 0.22f)
            }
            "cheater" -> {
                poly(White, 0.06f, 0.3f, 0.5f, 0.2f, 0.94f, 0.3f, 0.88f, 0.62f, 0.64f, 0.84f, 0.5f, 0.74f, 0.36f, 0.84f, 0.12f, 0.62f)
                poly(Red, 0.2f, 0.4f, 0.4f, 0.42f, 0.36f, 0.54f, 0.2f, 0.52f)
                poly(Red, 0.8f, 0.4f, 0.6f, 0.42f, 0.64f, 0.54f, 0.8f, 0.52f)
            }
            "void" -> {
                fc(White, 0.5f, 0.5f, 0.4f)
                fc(Ink, 0.5f, 0.5f, 0.3f)
            }
            "when_started" -> {
                rays(Yellow, 0.5f, 0.5f, 0.16f, 0.44f, 12, 0.045f)
                fc(White, 0.5f, 0.5f, 0.1f)
            }
            "when_ends" -> {
                poly(White, 0.28f, 0.14f, 0.72f, 0.14f, 0.54f, 0.5f, 0.72f, 0.86f, 0.28f, 0.86f, 0.46f, 0.5f)
                rect(Violet, 0.2f, 0.08f, 0.6f, 0.07f)
                rect(Violet, 0.2f, 0.85f, 0.6f, 0.07f)
                ln(Grey, 0.4f, 0.8f, 0.6f, 0.8f, 0.03f)
            }
            "small_universe" -> {
                ds.drawCircle(Violet, 0.42f * side, p(0.5f, 0.5f), style = Stroke(0.05f * side))
                ds.drawCircle(Teal, 0.28f * side, p(0.5f, 0.5f), style = Stroke(0.03f * side))
                fc(Yellow, 0.5f, 0.5f, 0.06f)
                ship(0.5f + 0.28f * cos(-0.5f), 0.5f + 0.28f * sin(-0.5f), 0.08f, -0.5f + 1.57f)
            }
            "first_dawn" -> {
                rays(Yellow, 0.5f, 0.62f, 0.26f, 0.38f, 9, 0.04f)
                fc(Yellow, 0.5f, 0.62f, 0.22f)
                fc(Green, 0.5f, 1.0f, 0.42f)
                fc(Blue.copy(alpha = 0.6f), 0.4f, 0.74f, 0.05f)
            }
            "world_not_found" -> {
                poly(White, 0.16f, 0.08f, 0.62f, 0.08f, 0.84f, 0.3f, 0.84f, 0.6f, 0.7f, 0.54f, 0.58f, 0.66f, 0.46f, 0.6f, 0.34f, 0.72f, 0.16f, 0.66f)
                ln(Red, 0.34f, 0.3f, 0.64f, 0.5f, 0.06f)
                ln(Red, 0.64f, 0.3f, 0.34f, 0.5f, 0.06f)
                poly(Grey, 0.62f, 0.08f, 0.62f, 0.3f, 0.84f, 0.3f)
            }
            "end_to_ending" -> {
                rr(Blue, 0.18f, 0.1f, 0.64f, 0.8f, 0.05f)
                rect(White, 0.74f, 0.14f, 0.06f, 0.72f)
                poly(Yellow, 0.46f, 0.3f, 0.52f, 0.46f, 0.68f, 0.48f, 0.55f, 0.58f, 0.6f, 0.74f, 0.46f, 0.65f, 0.32f, 0.74f, 0.37f, 0.58f, 0.24f, 0.48f, 0.4f, 0.46f)
            }
            "cat_astrophe" -> {
                poly(Orange, 0.2f, 0.16f, 0.4f, 0.32f, 0.2f, 0.46f)
                poly(Orange, 0.8f, 0.16f, 0.6f, 0.32f, 0.8f, 0.46f)
                fc(Orange, 0.5f, 0.56f, 0.32f)
                sc(White, 0.5f, 0.56f, 0.38f, 0.05f)
                rr(Blue.copy(alpha = 0.55f), 0.26f, 0.44f, 0.48f, 0.24f, 0.1f)
                fc(White, 0.4f, 0.54f, 0.03f)
                fc(White, 0.6f, 0.54f, 0.03f)
                ln(Ink, 0.5f, 0.6f, 0.5f, 0.64f, 0.03f)
            }
            "full_stack" -> {
                fc(Blue, 0.5f, 0.78f, 0.19f)
                fc(Green, 0.5f, 0.58f, 0.17f)
                fc(Yellow, 0.5f, 0.4f, 0.15f)
                fc(Red, 0.5f, 0.24f, 0.12f)
            }
            "multitasker" -> {
                poly(Yellow, 0.2f, 0.74f, 0.28f, 0.42f, 0.38f, 0.24f, 0.5f, 0.18f, 0.62f, 0.24f, 0.72f, 0.42f, 0.8f, 0.74f)
                rect(Yellow, 0.14f, 0.72f, 0.72f, 0.07f)
                fc(Orange, 0.5f, 0.86f, 0.07f)
                fc(Yellow, 0.5f, 0.14f, 0.04f)
            }
            "tweak_freak" -> {
                ln(Grey, 0.12f, 0.24f, 0.88f, 0.24f, 0.045f)
                fc(Teal, 0.62f, 0.24f, 0.09f)
                ln(Grey, 0.12f, 0.5f, 0.88f, 0.5f, 0.045f)
                fc(Pink, 0.3f, 0.5f, 0.09f)
                ln(Grey, 0.12f, 0.76f, 0.88f, 0.76f, 0.045f)
                fc(Yellow, 0.74f, 0.76f, 0.09f)
            }
            "perfectionist" -> {
                poly(Yellow, 0.1f, 0.74f, 0.06f, 0.28f, 0.3f, 0.46f, 0.5f, 0.16f, 0.7f, 0.46f, 0.94f, 0.28f, 0.9f, 0.74f)
                rect(Yellow, 0.1f, 0.74f, 0.8f, 0.1f)
                fc(Red, 0.3f, 0.62f, 0.045f)
                fc(Blue, 0.5f, 0.56f, 0.05f)
                fc(Green, 0.7f, 0.62f, 0.045f)
            }
            else -> fc(Grey, 0.5f, 0.5f, 0.3f)
        }
    }
}
