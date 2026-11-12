/*
 * Copyright (C) 2024 The Android Open Source Project
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

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class Autopilot(val ship: Spacecraft, val universe: Universe) : Entity {
    val BRAKING_TIME = 5f
    val SIGHTSEEING_TIME = 15f
    val LAUNCH_THRUST_TIME = 5f
    val STRATEGY_MIN_TIME = 0.5f

    var enabled = false

    var target: Planet? = null

    var landingAltitude = 0f

    var nextStrategyTime = 0f

    var brakingDistance = 0f

    // used by rendering
    var leadingPos = Vec2.Zero
    var leadingVector = Vec2.Zero

    val telemetry: String
        get() =
            if (enabled)
                listOf(
                        "---- AUTOPILOT ENGAGED ----",
                        "TGT: " + (target?.name?.uppercase() ?: "SELECTING..."),
                        "EXE: $strategy" + if (debug.isNotEmpty()) " ($debug)" else "",
                    )
                    .joinToString("\n")
            else ""

    var strategy: String = "NONE"
    private var debug: String = ""

    override fun update(sim: Simulator, dt: Float) {
        if (!enabled) return

        // MOD: nothing to fly to (for example a world with no planets)
        if (universe.planets.isEmpty()) {
            strategy = "NO TARGETS"
            debug = ""
            target = null
            if (ship.landing == null) ship.thrust = Vec2.Zero
            return
        }

        // launch burn / sightseeing hold
        if (sim.now < nextStrategyTime) {
            return
        }

        if (ship.landing != null) {
            if (target != null) {
                strategy = "LANDED"
                debug = ""
                // we just got here. see the sights.
                target = null
                landingAltitude = 0f
                nextStrategyTime = sim.now + SIGHTSEEING_TIME
            } else {
                // full power until we blast off
                ship.thrust = Vec2.makeWithAngleMag(ship.angle, 1f)

                strategy = "LAUNCHING"
                debug = ""
                nextStrategyTime = sim.now + LAUNCH_THRUST_TIME
            }
            return
        }

        // a target that is already explored (for example landed on by hand while the autopilot
        // was off) is dropped as long as unexplored planets remain
        target?.let { t ->
            if (t.explored && universe.planets.any { !it.explored }) target = null
        }

        // select new target
        if (target == null) {
            // target the nearest unexplored planet
            target =
                universe.planets
                    .sortedBy { (it.pos - ship.pos).mag() }
                    .firstOrNull { !it.explored }
            brakingDistance = 0f

            // if we've explored them all, pick one at random
            if (target == null) target = universe.planets.random()
        }

        target?.let { target -> // should be nonnull
            // MOD: the whole manoeuvre is flown in the target's own reference frame, so a planet
            // that keeps moving along its orbit is just a point we fly straight at. (The old
            // version braked along the ship's own velocity and flipped between "chasing" and
            // "approaching" over and over, because its braking distance shrank as it slowed.)
            val toTarget = target.pos - ship.pos
            val dist = max(toTarget.mag(), 1f)
            val dir = toTarget / dist
            val altitude = dist - target.radius

            landingAltitude = min(target.radius, 100f)

            val relV: Vec2 = ship.velocity - target.velocity
            val closing = relV.dot(dir) // positive: getting closer
            val accelMax = MAIN_ENGINE_ACCEL * Tuning.engineScale
            val timeToTarget = if (closing > 1f) altitude / closing else 1_000f

            // for the on-screen guide: where the target will be when we get there
            leadingPos = target.pos + target.velocity * min(timeToTarget, 30f)
            leadingVector = leadingPos - ship.pos
            brakingDistance =
                expSmooth(
                    brakingDistance,
                    if (closing > 0f) (closing * closing) / (accelMax) else 0f,
                    dt = sim.dt,
                    speed = 5f,
                )

            if (altitude < landingAltitude) {
                strategy = "LANDING"
                // Strategy: zero thrust, face away, prepare for landing
                ship.angle = (ship.pos - target.pos).angle() // point away from ground
                ship.thrust = Vec2.Zero
            } else {
                // The fastest we may close in at: never over the speed limit, never so fast that
                // we couldn't stop (using half the engine), and slow for the last stretch.
                val vMax = Tuning.speedLimit * 0.9f
                val vDes =
                    min(
                        vMax,
                        min(
                            sqrt(altitude * accelMax * 0.5f),
                            (200f + altitude * 0.15f) * sqrt(Tuning.engineScale),
                        ),
                    )

                // Steer the velocity (relative to the target) toward "vDes, straight at it".
                val error = dir * vDes - relV
                val errMag = error.mag()
                if (errMag < 2f) {
                    ship.thrust = Vec2.Zero
                } else {
                    ship.angle = error.angle()
                    ship.thrust =
                        Vec2.makeWithAngleMag(
                            ship.angle,
                            (errMag / (accelMax * 0.6f)).coerceIn(0f, 1f),
                        )
                }

                // label only (with some hysteresis so the HUD doesn't flicker)
                strategy =
                    when {
                        closing < vDes * 0.85f -> "CHASING"
                        closing > vDes * 1.05f -> "APPROACHING"
                        strategy == "CHASING" || strategy == "APPROACHING" -> strategy
                        else -> "CHASING"
                    }
            }
            debug = ("DV=%.0f D=%.0f T%+.1f").format(closing, altitude, timeToTarget)
        }
    }

    override fun postUpdate(sim: Simulator, dt: Float) {
        if (!enabled) return
    }
}
