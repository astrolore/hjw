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

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment.Left
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import java.lang.Float.max
import java.lang.Float.min
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random
import com.dede.basic.utils.DessertUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RandomSeedType {
    Fixed,
    Daily,
    Evergreen,
}

const val TEST_UNIVERSE = false

val RANDOM_SEED_TYPE = RandomSeedType.Daily

const val FIXED_RANDOM_SEED = 5038L
const val DEFAULT_CAMERA_ZOOM = 1f
const val MIN_CAMERA_ZOOM = 250f / UNIVERSE_RANGE // 0.0025f
const val MAX_CAMERA_ZOOM = 5f
var TOUCH_CAMERA_PAN = false
var TOUCH_CAMERA_ZOOM = false
var DYNAMIC_ZOOM = false

fun dailySeed(): Long {
    val today = GregorianCalendar()
    return today.get(Calendar.YEAR) * 10_000L +
        today.get(Calendar.MONTH) * 100L +
        today.get(Calendar.DAY_OF_MONTH)
}

fun randomSeed(): Long {
    return when (RANDOM_SEED_TYPE) {
        RandomSeedType.Fixed -> FIXED_RANDOM_SEED
        RandomSeedType.Daily -> dailySeed()
        else -> Random.Default.nextLong().mod(10_000_000).toLong()
    }.absoluteValue
}

fun getDessertCode(): String = DessertUtils.getDessertCode()

fun getSystemDesignation(universe: Universe): String {
    return "${getDessertCode()}-${universe.randomSeed % 100_000}"
}

val DEBUG_TEXT = mutableStateOf("Hello Universe")
const val SHOW_DEBUG_TEXT = false

@Composable
fun DebugText(text: MutableState<String>) {
    if (SHOW_DEBUG_TEXT) {
        Text(
            modifier = Modifier.fillMaxWidth().border(0.5.dp, color = Color.Yellow).padding(2.dp),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            color = Color.Yellow,
            text = text.value,
        )
    }
}

@Composable
fun Telemetry(universe: Universe, showControls: Boolean) {
    var topVisible by remember { mutableStateOf(false) }
    var bottomVisible by remember { mutableStateOf(false) }

    var catalogFontSize by remember { mutableStateOf(9.sp) }

    val textStyle =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            lineHeight = 12.sp,
        )

    LaunchedEffect("blah") {
        delay(1000)
        bottomVisible = true
        topVisible = true
    }

    val explored = universe.planets.filter { it.known }

    // TODO: Narrow the scope of invalidation here to the specific data needed;
    // the behavior below mimics the previous implementation of a snapshot ticker value
    val recomposeScope = currentRecomposeScope
    Telescope(universe) { recomposeScope.invalidate() }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize().padding(6.dp).windowInsetsPadding(WindowInsets.safeContent)
    ) {
        val wide = maxWidth > maxHeight
        Column(
            modifier =
                Modifier.align(if (wide) Alignment.BottomEnd else Alignment.BottomStart)
                    .fillMaxWidth(if (wide) 0.45f else 1.0f)
        ) {
            val autopilotEnabled = universe.ship.autopilot?.enabled == true
            if (autopilotEnabled) {
                universe.ship.autopilot?.let { autopilot ->
                    AnimatedVisibility(
                        modifier = Modifier,
                        visible = bottomVisible,
                        enter = flickerFadeIn,
                    ) {
                        Text(
                            style = textStyle,
                            color = Colors.Autopilot,
                            modifier = Modifier.align(Left),
                            text = autopilot.telemetry,
                        )
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 6.dp)) {
                AnimatedVisibility(
                    modifier = Modifier.weight(1f),
                    visible = bottomVisible,
                    enter = flickerFadeIn,
                ) {
                    Text(
                        style = textStyle,
                        color = Colors.Console,
                        text =
                            with(universe.ship) {
                                val closest = universe.closestPlanet()
                                val distToClosest =
                                    ((closest.pos - pos).mag() - closest.radius).toInt()
                                listOfNotNull(
                                        landing?.let {
                                            "LND: ${it.planet.name.uppercase()}\n" +
                                                "JOB: ${it.text.uppercase()}"
                                        }
                                            ?: if (distToClosest < 10_000) {
                                                "ALT: $distToClosest"
                                            } else null,
                                        "THR: %.0f%%".format(thrust.mag() * 100f),
                                        "POS: %s".format(pos.str("%+7.0f")),
                                        "VEL: %.0f".format(velocity.mag()),
                                    )
                                    .joinToString("\n")
                            },
                    )
                }

                if (showControls) {
                    AnimatedVisibility(
                        visible = bottomVisible,
                        enter = flickerFadeInAfterDelay(500),
                    ) {
                        if (GameSettings.effectiveReveal()) {
                            val revealOn = universe.revealed
                            ConsoleButton(
                                textStyle = textStyle,
                                color = Colors.Console,
                                bgColor = if (revealOn) Colors.Autopilot else Color.Transparent,
                                borderColor = Colors.Console,
                                text = if (revealOn) "HIDE" else "REVEAL",
                            ) {
                                universe.setRevealAll(!universe.revealed)
                            }
                        } else {
                            val autoOn = universe.ship.autopilot?.enabled == true
                            ConsoleButton(
                                textStyle = textStyle,
                                color = Colors.Console,
                                bgColor = if (autoOn) Colors.Autopilot else Color.Transparent,
                                borderColor = Colors.Console,
                                text = "AUTO",
                            ) {
                                universe.ship.autopilot?.let { it.enabled = !it.enabled }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopStart),
            visible = topVisible,
            enter = flickerFadeInAfterDelay(1000),
        ) {
            Text(
                style = textStyle,
                fontSize = catalogFontSize,
                lineHeight = catalogFontSize,
                letterSpacing = 1.sp,
                color = Colors.Console,
                onTextLayout = { textLayoutResult ->
                    if (textLayoutResult.didOverflowHeight) {
                        catalogFontSize = 8.sp
                    }
                },
                text =
                    (with(universe.star) {
                            listOf(
                                "  STAR: $name (${getSystemDesignation(universe)})",
                                " CLASS: ${cls.name}",
                                "RADIUS: ${radius.toInt()}",
                                "  MASS: %.3g".format(mass),
                                "BODIES: ${explored.size} / ${universe.planets.size}",
                                "",
                            )
                        } +
                            explored
                                .map {
                                    listOf(
                                        "  BODY: ${it.name}",
                                        "  TYPE: ${it.description.replaceFirstChar { c -> c.uppercase() }}",
                                        "  ATMO: ${it.atmosphere.replaceFirstChar { c -> c.uppercase() }}",
                                        " FAUNA: ${it.fauna.replaceFirstChar { c -> c.uppercase() }}",
                                        " FLORA: ${it.flora.replaceFirstChar { c -> c.uppercase() }}",
                                        "",
                                    )
                                }
                                .flatten())
                        .joinToString("\n"),

                // TODO: different colors, highlight latest discovery
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    private var notifier: UniverseProgressNotifier? = null
    private var foldState = mutableStateOf<FoldingFeature?>(null)
    private lateinit var universeState: MutableState<Universe>

    /** MOD: true when achievements are disabled for the current world (mods were used in it). */
    private val achOffState = mutableStateOf(false)

    /** MOD: pending "mods are on" prompt. [seed] is the world waiting to load, or null at startup. */
    private class ModPrompt(val seed: Long?)

    private val modPrompt = mutableStateOf<ModPrompt?>(null)

    /** Builds the world for [seed]. The same seed always gives the same world. */
    private fun buildUniverse(seed: Long): Universe {
        val universe = Universe(namer = Namer(resources), randomSeed = seed)

        if (TEST_UNIVERSE) {
            universe.initTest()
        } else {
            universe.initRandom()
        }

        // the autopilot is kept (disabled) so the HUD/notification code keeps working
        val autopilot = Autopilot(universe.ship, universe)
        universe.ship.autopilot = autopilot
        universe.add(autopilot)
        autopilot.enabled = false
        return universe
    }

    /** Switches to the world for [seed] (menu: choose world / random world / history). */
    private fun startWorld(seed: Long) {
        notifier?.cancel()
        notifier = null
        DYNAMIC_ZOOM = false
        val universe = buildUniverse(seed)
        WorldHistory.recordPlay(applicationContext, universe)
        achOffState.value = WorldHistory.isAchievementsDisabled(applicationContext, seed)
        if (GameSettings.shipNotification) notifier = UniverseProgressNotifier(this, universe)
        if (::universeState.isInitialized) {
            universeState.value = universe
        } else {
            universeState = mutableStateOf(universe)
        }
    }

    /** Loads a world, asking first if the mod menu is on (loading then disables achievements). */
    private fun requestWorld(seed: Long) {
        if (GameSettings.modMenu) modPrompt.value = ModPrompt(seed) else startWorld(seed)
    }

    /** Mods were used in the current world: its achievements are off for good. */
    private fun flagCurrentWorld() {
        WorldHistory.markAchievementsDisabled(applicationContext, universeState.value.randomSeed)
        achOffState.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GameSettings.init(applicationContext)

        onWindowLayoutInfoChange()

        enableEdgeToEdge()

        com.android_cinnamon_bun.egg.ComponentActivationActivity.lockUnlockComponents(applicationContext)

        // Android 13+: ask once so the "ship status" notification can show
        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        // start in today's world; the menu can switch to any other
        startWorld(randomSeed())
        // mods left on from last time: same prompt as loading a world with mods on
        if (GameSettings.modMenu) modPrompt.value = ModPrompt(null)

        setContent {
            val universe = universeState.value
            val view = LocalView.current
            val ctx = LocalContext.current

            // settings that act on the game as a whole
            SideEffect {
                view.keepScreenOn = GameSettings.keepScreenOn
                Tuning.engineScale = GameSettings.effectiveEngine()
                Tuning.speedLimit = GameSettings.effectiveSpeedLimit()
                if (!GameSettings.effectiveReveal() && universe.revealed) universe.setRevealAll(false)
            }
            LaunchedEffect(Unit) {
                snapshotFlow { GameSettings.shipNotification }
                    .collect { on ->
                        if (on) {
                            if (notifier == null)
                                notifier = UniverseProgressNotifier(this@MainActivity, universeState.value)
                        } else {
                            notifier?.cancel()
                            notifier = null
                        }
                    }
            }
            BackHandler(enabled = modPrompt.value != null) {
                // cancelling a pending load is fine; the startup prompt can't be dodged
                if (modPrompt.value?.seed != null) modPrompt.value = null
            }

            // everything below belongs to one world; a new world gets a fresh composition
            key(universe) {
                // keep the history's "explored" count up to date
                LaunchedEffect(universe) {
                    var saved = 0
                    while (true) {
                        delay(1500)
                        val n = universe.planets.count { it.explored }
                        if (n != saved) {
                            saved = n
                            WorldHistory.updateProgress(applicationContext, universe)
                        }
                    }
                }

                Spaaaace(modifier = Modifier.fillMaxSize(), u = universe, foldState = foldState)
                DebugText(DEBUG_TEXT)

                val minRadius = 50.dp.toLocalPx()
                val maxRadius = 100.dp.toLocalPx()
                FlightStick(
                    modifier = Modifier.fillMaxSize(),
                    minRadius = minRadius,
                    maxRadius = maxRadius,
                    color = Color.Green,
                ) { vec ->
                    (universe.follow as? Spacecraft)?.let { ship ->
                        val wasThrusting = ship.thrust != Vec2.Zero
                        if (vec == Vec2.Zero) {
                            ship.thrust = Vec2.Zero
                        } else {
                            val a = vec.angle()
                            ship.angle = a

                            val m = vec.mag()
                            if (m < minRadius) {
                                // within this radius, just reorient
                                ship.thrust = Vec2.Zero
                            } else {
                                ship.thrust =
                                    Vec2.makeWithAngleMag(
                                        a,
                                        lexp(minRadius, maxRadius, m).coerceIn(0f, 1f),
                                    )
                            }
                        }
                        if (!wasThrusting && ship.thrust != Vec2.Zero) Haptics.pulse(ctx, 14, 90)
                    }
                }
                if (GameSettings.showHud) Telemetry(universe, true)
                GameMenu(
                    universe = universe,
                    achievementsOff = achOffState.value,
                    onPlayWorld = { seed -> requestWorld(seed) },
                    onModConfirmed = { flagCurrentWorld() },
                    onHistoryCleared = { achOffState.value = false },
                )
            }

            // loading a world (or starting) while the mod menu is on
            modPrompt.value?.let { prompt ->
                ThemedPrompt(
                    title = "MODS ARE ON",
                    message =
                        "You have mod settings turned on. Continuing will disable achievements for this world.",
                    buttons =
                        listOf(
                            "CONTINUE" to
                                {
                                    modPrompt.value = null
                                    prompt.seed?.let { startWorld(it) }
                                    flagCurrentWorld()
                                },
                            "TURN OFF MODS" to
                                {
                                    GameSettings.modMenu = false
                                    GameSettings.save()
                                    modPrompt.value = null
                                    prompt.seed?.let { startWorld(it) }
                                },
                        ),
                )
            }
        }
    }

    private fun onWindowLayoutInfoChange() {
        val windowInfoTracker = WindowInfoTracker.getOrCreate(this@MainActivity)

        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                windowInfoTracker.windowLayoutInfo(this@MainActivity).collect { layoutInfo ->
                    foldState.value =
                        layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                    Log.v("Landroid", "fold updated: $foldState")
                }
            }
        }
    }

    override fun onDestroy() {
        notifier?.cancel()
        DYNAMIC_ZOOM = false
        super.onDestroy()
    }
}

@Preview(name = "phone", device = Devices.PHONE)
@Preview(name = "fold", device = Devices.FOLDABLE)
@Preview(name = "tablet", device = Devices.TABLET)
@Composable
fun MainActivityPreview() {
    val universe = Universe(namer = Namer(Resources.getSystem()), randomSeed = randomSeed())

    universe.initTest()

    Spaaaace(modifier = Modifier.fillMaxSize(), universe)
    DebugText(DEBUG_TEXT)
    Telemetry(universe, true)
}

@Composable
fun FlightStick(
    modifier: Modifier,
    minRadius: Float = 0f,
    maxRadius: Float = 1000f,
    color: Color = Color.Green,
    onStickChanged: (vector: Vec2) -> Unit,
) {
    val origin = remember { mutableStateOf(Vec2.Zero) }
    val target = remember { mutableStateOf(Vec2.Zero) }

    Box(
        modifier =
            modifier
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            // ACTION_DOWN
                            val down = awaitFirstDown(requireUnconsumed = false)
                            origin.value = down.position
                            target.value = down.position

                            do {
                                // ACTION_MOVE
                                val event: PointerEvent = awaitPointerEvent()
                                target.value = event.changes[0].position

                                onStickChanged(target.value - origin.value)
                            } while (
                                !event.changes.any { it.isConsumed } &&
                                    event.changes.count { it.pressed } == 1
                            )

                            // ACTION_UP / CANCEL
                            target.value = Vec2.Zero
                            origin.value = Vec2.Zero

                            onStickChanged(Vec2.Zero)
                        }
                    }
                }
                .drawBehind {
                    if (origin.value != Vec2.Zero) {
                        val delta = target.value - origin.value
                        val mag = min(maxRadius, delta.mag())
                        val r = max(minRadius, mag)
                        val a = delta.angle()
                        drawCircle(
                            color = color,
                            center = origin.value,
                            radius = r,
                            style =
                                Stroke(
                                    width = 2f,
                                    pathEffect =
                                        if (mag < minRadius)
                                            PathEffect.dashPathEffect(
                                                floatArrayOf(this.density * 1f, this.density * 2f)
                                            )
                                        else null,
                                ),
                        )
                        drawLine(
                            color = color,
                            start = origin.value,
                            end = origin.value + Vec2.makeWithAngleMag(a, mag),
                            strokeWidth = 2f,
                        )
                    }
                }
    )
}

@Composable
fun Spaaaace(
    modifier: Modifier,
    u: Universe,
    foldState: MutableState<FoldingFeature?> = mutableStateOf(null),
) {
    LaunchedEffect(u) {
        while (true) withInfiniteAnimationFrameNanos { frameTimeNanos -> u.step(frameTimeNanos) }
    }

    var cameraZoom by remember { mutableFloatStateOf(DEFAULT_CAMERA_ZOOM) }
    var cameraOffset by remember { mutableStateOf(Offset.Zero) }

    // MOD: camera shake / zoom kick, and haptics, driven by landings and bounces
    val fx = remember(u) { CameraFx() }
    val context = LocalContext.current
    remember(u) {
        u.onLanded = { speed ->
            fx.impulse(landing = true, speed = speed)
            Haptics.pulse(context, 45, 160)
        }
        u.onBounce = { speed ->
            fx.impulse(landing = false, speed = speed)
            Haptics.pulse(context, 28, 110)
        }
        Unit
    }

    val transformableState =
        rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            if (TOUCH_CAMERA_PAN) cameraOffset += offsetChange / cameraZoom
            if (TOUCH_CAMERA_ZOOM)
                cameraZoom = clamp(cameraZoom * zoomChange, MIN_CAMERA_ZOOM, MAX_CAMERA_ZOOM)
        }

    var canvasModifier = modifier

    if (TOUCH_CAMERA_PAN || TOUCH_CAMERA_ZOOM) {
        canvasModifier = canvasModifier.transformable(transformableState)
    }

    val halfFolded = foldState.value?.let { it.state == FoldingFeature.State.HALF_OPENED } ?: false
    val horizontalFold =
        foldState.value?.let { it.orientation == FoldingFeature.Orientation.HORIZONTAL } ?: false

    val centerFracX: Float by
        animateFloatAsState(if (halfFolded && !horizontalFold) 0.25f else 0.5f, label = "centerX")
    val centerFracY: Float by
        animateFloatAsState(if (halfFolded && horizontalFold) 0.25f else 0.5f, label = "centerY")

    UniverseCanvas(u, canvasModifier) { u ->
        drawRect(Colors.Eigengrau, Offset.Zero, size)

        val closest = u.closestPlanet()
        val distToNearestSurf = max(0f, (u.ship.pos - closest.pos).mag() - closest.radius * 1.2f)
        //        val normalizedDist = clamp(distToNearestSurf, 50f, 50_000f) / 50_000f

        // MOD: zoom that makes the whole universe fit the screen ("God's Eye")
        val godZoom = (min(size.width, size.height) * 0.5f * 0.94f) / UNIVERSE_RANGE
        GameSettings.godZoom = godZoom
        val gods = GameSettings.isGodsEye()
        val baseZoom = GameSettings.baseZoom()

        val targetZoom =
            if (DYNAMIC_ZOOM) {
                // screensaver / autopilot mode: the original zoom by distance to the nearest surface
                clamp(500f / distToNearestSurf, MIN_CAMERA_ZOOM, MAX_CAMERA_ZOOM)
            } else if (GameSettings.dynamicCamera && !gods) {
                clamp(
                    baseZoom * fx.zoomFactor(u.ship, distToNearestSurf),
                    min(MIN_CAMERA_ZOOM, baseZoom),
                    MAX_CAMERA_ZOOM,
                )
            } else {
                baseZoom
            }
        if (!TOUCH_CAMERA_ZOOM) {
            cameraZoom = expSmooth(cameraZoom, targetZoom, dt = u.dt, speed = if (gods) 4f else 1.5f)
        }

        // MOD: shake in screen pixels
        val shake =
            if (GameSettings.dynamicCamera && !gods)
                fx.shakeOffset(u.dt, u.ship.thrust.mag(), GameSettings.shakeStrength)
            else Offset.Zero

        if (!TOUCH_CAMERA_PAN) cameraOffset = (u.follow?.pos ?: Vec2.Zero) * -1f

        // cameraZoom: metersToPixels
        // visibleSpaceSizeMeters: meters
        // cameraOffset: meters ≈ vector pointing from ship to (0,0) (e.g. -pos)
        val visibleSpaceSizeMeters = size / cameraZoom // meters x meters
        val visibleSpaceRectMeters =
            Rect(
                -cameraOffset -
                    Offset(
                        visibleSpaceSizeMeters.width * centerFracX,
                        visibleSpaceSizeMeters.height * centerFracY,
                    ),
                visibleSpaceSizeMeters,
            )

        var gridStep = 1000f
        while (gridStep * cameraZoom < 32.dp.toPx()) gridStep *= 10

        DEBUG_TEXT.value =
            ("SIMULATION //\n" +
                // "normalizedDist=${normalizedDist} \n" +
                "entities: ${u.entities.size} // " +
                "zoom: ${"%.4f".format(cameraZoom)}x // " +
                "fps: ${"%3.0f".format(1f / u.dt)} " +
                "dt: ${u.dt}\n" +
                ((u.follow as? Spacecraft)?.let {
                    "ship: p=%s v=%7.2f a=%6.3f t=%s\n"
                        .format(
                            it.pos.str("%+7.1f"),
                            it.velocity.mag(),
                            it.angle,
                            it.thrust.str("%+5.2f"),
                        )
                } ?: "") +
                "star: '${u.star.name}' designation=UDC-${u.randomSeed % 100_000} " +
                "class=${u.star.cls.name} r=${u.star.radius.toInt()} m=${u.star.mass}\n" +
                "planets: ${u.planets.size}\n" +
                u.planets.joinToString("\n") {
                    val range = (u.ship.pos - it.pos).mag()
                    val vorbit = sqrt(GRAVITATION * it.mass / range)
                    val vescape = sqrt(2 * GRAVITATION * it.mass / it.radius)
                    " * ${it.name}:\n" +
                        if (it.explored) {
                            "   TYPE:  ${it.description.replaceFirstChar { c -> c.uppercase() }}\n" +
                                "   ATMO:  ${it.atmosphere.replaceFirstChar { c -> c.uppercase() }}\n" +
                                "   FAUNA: ${it.fauna.replaceFirstChar { c -> c.uppercase() }}\n" +
                                "   FLORA: ${it.flora.replaceFirstChar { c -> c.uppercase() }}\n"
                        } else {
                            "   (Unexplored)\n"
                        } +
                        "   orbit=${(it.pos - it.orbitCenter).mag().toInt()}" +
                        " radius=${it.radius.toInt()}" +
                        " mass=${"%g".format(it.mass)}" +
                        " vel=${(it.speed).toInt()}" +
                        " // range=${"%.0f".format(range)}" +
                        " vorbit=${vorbit.toInt()} vescape=${vescape.toInt()}"
                })

        zoom(cameraZoom) {
            // All coordinates are space coordinates now.

            translate(
                -visibleSpaceRectMeters.center.x + size.width * 0.5f + shake.x / cameraZoom,
                -visibleSpaceRectMeters.center.y + size.height * 0.5f + shake.y / cameraZoom,
            ) {
                // debug outer frame
                // drawRect(
                //     Colors.Eigengrau2,
                //     visibleSpaceRectMeters.topLeft,
                //     visibleSpaceRectMeters.size,
                //     style = Stroke(width = 10f / cameraZoom)
                // )

                var x = floor(visibleSpaceRectMeters.left / gridStep) * gridStep
                if (!GameSettings.showGrid) x = visibleSpaceRectMeters.right
                while (x < visibleSpaceRectMeters.right) {
                    drawLine(
                        color = Colors.Eigengrau2,
                        start = Offset(x, visibleSpaceRectMeters.top),
                        end = Offset(x, visibleSpaceRectMeters.bottom),
                        strokeWidth = (if ((x % (gridStep * 10) == 0f)) 3f else 1.5f) / cameraZoom,
                    )
                    x += gridStep
                }

                var y = floor(visibleSpaceRectMeters.top / gridStep) * gridStep
                if (!GameSettings.showGrid) y = visibleSpaceRectMeters.bottom
                while (y < visibleSpaceRectMeters.bottom) {
                    drawLine(
                        color = Colors.Eigengrau2,
                        start = Offset(visibleSpaceRectMeters.left, y),
                        end = Offset(visibleSpaceRectMeters.right, y),
                        strokeWidth = (if ((y % (gridStep * 10) == 0f)) 3f else 1.5f) / cameraZoom,
                    )
                    y += gridStep
                }

                this@zoom.drawUniverse(u)

                // MOD: far-out view (God's Eye and low zooms)
                if (cameraZoom < 0.05f) {
                    // the edge of the universe
                    drawCircle(
                        color = Colors.Eigengrau3,
                        radius = UNIVERSE_RANGE,
                        center = Offset.Zero,
                        style = Stroke(width = 1.5.dp.toPx() / cameraZoom),
                    )
                    // bodies too small to see still get a dot
                    val minR = 2.5.dp.toPx() / cameraZoom
                    u.planets.forEach { p ->
                        if (p.radius < minR)
                            drawCircle(
                                if (p.known) p.color else Colors.Eigengrau4,
                                radius = minR,
                                center = p.pos,
                            )
                    }
                    if (u.star.radius < minR) drawCircle(u.star.color, radius = minR, center = u.star.pos)
                }
                // the ship becomes a pulsing red dot with a ring when it is too small to see
                if (cameraZoom < 0.3f && u.ship.radius * cameraZoom < 1.5.dp.toPx()) {
                    val pulse = 0.5f + 0.5f * kotlin.math.sin(u.now * 6f)
                    val dot = (3.dp.toPx() + 1.5.dp.toPx() * pulse) / cameraZoom
                    drawCircle(Color(0xFFFF3B30), radius = dot, center = u.ship.pos)
                    drawCircle(
                        Color(0xFFFF3B30),
                        radius = dot * 2.6f,
                        center = u.ship.pos,
                        style = Stroke(width = 1.5.dp.toPx() / cameraZoom),
                    )
                }
            }
        }
    }
}

/** MOD: camera shake, zoom kick and speed-based zoom for the dynamic camera. */
class CameraFx {
    private var kick = 0f // zoom punch on landing, decays
    private var shake = 0f // impulse shake in pixels, decays

    fun impulse(landing: Boolean, speed: Float) {
        val s = (speed / 1500f).coerceIn(0.15f, 1f)
        shake = max(shake, (if (landing) 10f else 18f) * s)
        if (landing) kick = 0.10f
    }

    fun zoomFactor(ship: Spacecraft, distToSurface: Float): Float {
        val speedFrac = (ship.velocity.mag() / Tuning.speedLimit).coerceIn(0f, 1f)
        var f = 1f - 0.55f * speedFrac // zoom out as you speed up
        if (distToSurface < 4000f) f *= 1f + 0.45f * (1f - distToSurface / 4000f) // ease in near a surface
        return f * (1f + kick)
    }

    fun shakeOffset(dt: Float, thrustMag: Float, strength: Float): Offset {
        val decay = exp(-dt * 7f)
        shake *= decay
        kick *= exp(-dt * 4f)
        val amp = (shake + thrustMag * 3f) * strength
        if (amp < 0.05f) return Offset.Zero
        return Offset((Random.nextFloat() - 0.5f) * 2f * amp, (Random.nextFloat() - 0.5f) * 2f * amp)
    }
}
