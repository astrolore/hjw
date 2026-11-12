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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
// The void outside the universe. Instead of pitch black it is a deep, slowly shifting night: soft
// clouds of the game's colours and three layers of square stars (the same stars as in the
// explorer card) drifting at different speeds. Like an end portal, it is drawn in screen space:
// it does not move with the ship or the camera, so the edge feels like a window onto something
// endless behind the world.
// ---------------------------------------------------------------------------------------------

private val VOID_STAR_COLORS =
    arrayOf(
        Color(0xFF26C6DA), // teal
        Color(0xFF9C6BFF), // violet
        Color(0xFF34A853), // green
        Color(0xFFF2F2F7), // white
        Color(0xFFE08DE8), // pink
    )

private fun voidHash(x: Int, y: Int, s: Int): Int {
    var n = x * 374761393 + y * 668265263 + s * 1274126177
    n = (n xor (n ushr 13)) * 1274126177
    return n xor (n ushr 16)
}

private fun DrawScope.voidStars(
    time: Float,
    shiftX: Float,
    shiftY: Float,
    blend: Float,
    cell: Float,
    vx: Float,
    vy: Float,
    starSize: Float,
    chance: Float,
    salt: Int,
    twinkle: Float,
    alpha: Float,
) {
    val ox = time * vx - shiftX
    val oy = time * vy - shiftY
    val gx0 = floor(ox / cell).toInt()
    val gy0 = floor(oy / cell).toInt()
    val nx = (size.width / cell).toInt() + 2
    val ny = (size.height / cell).toInt() + 2
    for (j in 0..ny) {
        for (i in 0..nx) {
            val gx = gx0 + i
            val gy = gy0 + j
            val h = voidHash(gx, gy, salt)
            if ((h and 0xFF) / 255f > chance) continue
            val px = gx * cell - ox + ((h shr 8) and 0xFF) / 255f * cell
            val py = gy * cell - oy + ((h shr 16) and 0xFF) / 255f * cell
            val color = VOID_STAR_COLORS[(h ushr 24) % VOID_STAR_COLORS.size]
            val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(time * twinkle + (h and 0xFFFF) * 0.001f))
            drawRect(color.copy(alpha = tw * alpha * blend), Offset(px, py), Size(starSize, starSize))
        }
    }
}

/** Draws the void over the whole canvas; the universe's own disc is painted on top of it. */
/**
 * [camera] is the camera offset (the opposite of the ship's position): the layers slide with it at
 * different rates, so flying through the void feels like moving. [blend] fades the whole effect.
 */
fun DrawScope.drawEndVoid(time: Float, camera: Offset = Offset.Zero, blend: Float = 1f) {
    val w = size.width
    val h = size.height
    val unit = min(w, h) / 360f
    val big = max(w, h)

    drawRect(Color(0xFF05060B).copy(alpha = blend), Offset.Zero, size)

    // slow clouds of colour, deep behind the stars
    fun cloud(color: Color, cx: Float, cy: Float, r: Float) {
        val c = Offset(cx, cy)
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.20f * blend), Color.Transparent), center = c, radius = r),
            radius = r,
            center = c,
        )
    }
    val cx = camera.x * 0.0006f
    val cy = camera.y * 0.0006f
    cloud(Color(0xFF26C6DA), w * (0.3f + 0.18f * sin(time * 0.05f)) + cx, h * (0.35f + 0.12f * cos(time * 0.04f)) + cy, big * 0.65f)
    cloud(Color(0xFF9C6BFF), w * (0.72f + 0.16f * cos(time * 0.045f)) + cx * 1.6f, h * (0.62f + 0.14f * sin(time * 0.035f)) + cy * 1.6f, big * 0.7f)
    cloud(Color(0xFF34A853), w * (0.5f + 0.25f * sin(time * 0.03f + 2f)) + cx * 0.7f, h * (0.85f + 0.08f * cos(time * 0.05f)) + cy * 0.7f, big * 0.5f)

    // three layers of square stars, far to near
    voidStars(time, camera.x * 0.0015f, camera.y * 0.0015f, blend, 58f * unit, 4f * unit, 2f * unit, 2f * unit, 0.40f, 1, 1.3f, 0.65f)
    voidStars(time, camera.x * 0.0040f, camera.y * 0.0040f, blend, 74f * unit, -7f * unit, 5f * unit, 3f * unit, 0.30f, 2, 1.9f, 0.85f)
    voidStars(time, camera.x * 0.0100f, camera.y * 0.0100f, blend, 120f * unit, 12f * unit, -8f * unit, 5f * unit, 0.16f, 3, 2.6f, 1f)
}
