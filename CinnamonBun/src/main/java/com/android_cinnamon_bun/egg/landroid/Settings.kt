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

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import kotlin.math.ln
import kotlin.math.pow

// ---------------------------------------------------------------------------------------------
// MOD: persistent settings. Every value is Compose state, so the UI and the game react at once.
// ---------------------------------------------------------------------------------------------

/** Left end of the FOV slider (most zoomed in). The right end is God's Eye. */
const val FOV_NEAR_ZOOM = 3f

const val ENGINE_POWER_MIN = 0.5f
const val ENGINE_POWER_MAX = 3f
const val SPEED_LIMIT_MIN = 1_000f
const val SPEED_LIMIT_MAX = 20_000f

object GameSettings {
    private var prefs: SharedPreferences? = null

    // --- gameplay ---
    var showGrid by mutableStateOf(true)
    var showHud by mutableStateOf(true)
    var keepScreenOn by mutableStateOf(true)
    var shipNotification by mutableStateOf(true)

    /** On: the ship's trail keeps 100,000 points. Off: the stock 10,000 (lighter on weak phones). */
    var extendedTrail by mutableStateOf(true)

    /** The view zooms out as the ship speeds up. */
    var dynamicZoom by mutableStateOf(false)

    /** The ship's trail wears the colour of the player's rank. Off: the original green. */
    var dynamicTrail by mutableStateOf(true)

    // --- sound (the phone's own volume buttons are the master volume) ---
    var musicVolume by mutableFloatStateOf(0.5f)
    var effectsVolume by mutableFloatStateOf(0.7f)
    var engineVolume by mutableFloatStateOf(0.3f)

    // --- reality bending (everything below only applies while [modMenu] is on) ---
    var modMenu by mutableStateOf(false)
    var revealButton by mutableStateOf(false)
    var fovZoom by mutableFloatStateOf(DEFAULT_CAMERA_ZOOM)
    var fovGods by mutableStateOf(false)
    var enginePower by mutableFloatStateOf(1f)
    var speedLimit by mutableFloatStateOf(CRAFT_SPEED_LIMIT)

    /** Zoom at which the whole universe fits the screen. Updated by the game canvas, not saved. */
    var godZoom by mutableFloatStateOf(0.0025f)

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("landroid_settings", Context.MODE_PRIVATE)
        prefs = p
        showGrid = p.getBoolean("showGrid", true)
        showHud = p.getBoolean("showHud", true)
        keepScreenOn = p.getBoolean("keepScreenOn", true)
        shipNotification = p.getBoolean("shipNotification", true)
        extendedTrail = p.getBoolean("extendedTrail", true)
        dynamicZoom = p.getBoolean("dynamicZoom", false)
        dynamicTrail = p.getBoolean("dynamicTrail", true)
        musicVolume = p.getFloat("musicVolume", 0.5f)
        effectsVolume = p.getFloat("effectsVolume", 0.7f)
        engineVolume = p.getFloat("engineVolume", 0.3f)
        // the engine hum's default dropped to 30%: move earlier installs over once
        if (p.getInt("audioDefaults", 0) < 2) {
            engineVolume = 0.3f
            p.edit().putInt("audioDefaults", 2).putFloat("engineVolume", 0.3f).apply()
        }
        modMenu = p.getBoolean("modMenu", false)
        revealButton = p.getBoolean("revealButton", false)
        fovZoom = p.getFloat("fovZoom", DEFAULT_CAMERA_ZOOM)
        fovGods = p.getBoolean("fovGods", false)
        enginePower = p.getFloat("enginePower", 1f)
        speedLimit = p.getFloat("speedLimit", CRAFT_SPEED_LIMIT)
    }

    fun save() {
        prefs
            ?.edit()
            ?.putBoolean("showGrid", showGrid)
            ?.putBoolean("showHud", showHud)
            ?.putBoolean("keepScreenOn", keepScreenOn)
            ?.putBoolean("shipNotification", shipNotification)
            ?.putBoolean("extendedTrail", extendedTrail)
            ?.putBoolean("dynamicZoom", dynamicZoom)
            ?.putBoolean("dynamicTrail", dynamicTrail)
            ?.putFloat("musicVolume", musicVolume)
            ?.putFloat("effectsVolume", effectsVolume)
            ?.putFloat("engineVolume", engineVolume)
            ?.putBoolean("modMenu", modMenu)
            ?.putBoolean("revealButton", revealButton)
            ?.putFloat("fovZoom", fovZoom)
            ?.putBoolean("fovGods", fovGods)
            ?.putFloat("enginePower", enginePower)
            ?.putFloat("speedLimit", speedLimit)
            ?.apply()
    }

    // --- effective values: reality bending only counts while its master toggle is on ---

    fun effectiveReveal(): Boolean = modMenu && revealButton

    fun isGodsEye(): Boolean = modMenu && fovGods

    fun baseZoom(): Float =
        if (!modMenu) DEFAULT_CAMERA_ZOOM else if (fovGods) godZoom else fovZoom

    fun effectiveEngine(): Float = if (modMenu) enginePower else 1f

    fun effectiveSpeedLimit(): Float = if (modMenu) speedLimit else CRAFT_SPEED_LIMIT
}

/** Slider position (0 = most zoomed in, 1 = God's Eye) for a zoom level. */
fun fovSliderT(zoom: Float, gods: Boolean, godZoom: Float): Float {
    if (gods) return 1f
    if (godZoom >= FOV_NEAR_ZOOM) return 0f
    return (ln(FOV_NEAR_ZOOM / zoom) / ln(FOV_NEAR_ZOOM / godZoom)).coerceIn(0f, 1f)
}

/** Zoom level for a slider position; logarithmic, so every notch feels the same. */
fun fovZoomForT(t: Float, godZoom: Float): Float =
    FOV_NEAR_ZOOM * (godZoom / FOV_NEAR_ZOOM).pow(t.coerceIn(0f, 1f))

// ---------------------------------------------------------------------------------------------
// MOD: the Reference document (built-in asset, with an optional edited copy)
// ---------------------------------------------------------------------------------------------

object ReferenceStore {
    private const val FILE = "landroid_reference.md"

    fun loadOriginal(context: Context): String =
        try {
            context.assets.open(FILE).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "# Reference unavailable\n\nCould not load landroid_reference.md from the app assets."
        }

    /** Bumped whenever the built-in Reference is replaced by a new default. */
    private const val DEFAULT_VERSION = 2

    /**
     * The first time a new built-in Reference ships, an old saved copy would hide it. Set the old
     * copy aside (kept as landroid_reference_old.md) so the new default shows; later edits stick.
     */
    private fun migrate(context: Context) {
        val meta = context.getSharedPreferences("landroid_reference_meta", Context.MODE_PRIVATE)
        if (meta.getInt("default_version", 0) >= DEFAULT_VERSION) return
        val f = File(context.filesDir, FILE)
        if (f.exists()) {
            try {
                f.copyTo(File(context.filesDir, "landroid_reference_old.md"), overwrite = true)
            } catch (e: Exception) {
                // ignore
            }
            try {
                f.delete()
            } catch (e: Exception) {
                // ignore
            }
        }
        meta.edit().putInt("default_version", DEFAULT_VERSION).apply()
    }

    /** The edited copy if you saved one, otherwise the built-in text. */
    fun load(context: Context): String {
        migrate(context)
        val f = File(context.filesDir, FILE)
        if (f.exists()) {
            try {
                return f.readText()
            } catch (e: Exception) {
                // fall through to the built-in copy
            }
        }
        return loadOriginal(context)
    }

    fun save(context: Context, text: String) {
        try {
            File(context.filesDir, FILE).writeText(text)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun reset(context: Context) {
        try {
            File(context.filesDir, FILE).delete()
        } catch (e: Exception) {
            // ignore
        }
    }

    /** Writes the document to the phone's Downloads folder. Returns true on success. */
    fun exportToDownloads(context: Context, text: String): Boolean =
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values =
                    ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, FILE)
                        put(MediaStore.Downloads.MIME_TYPE, "text/markdown")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                val uri =
                    context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri == null) {
                    false
                } else {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                    true
                }
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                if (dir == null) {
                    false
                } else {
                    dir.mkdirs()
                    File(dir, FILE).writeText(text)
                    true
                }
            }
        } catch (e: Exception) {
            false
        }
}
