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
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.json.JSONObject

// ---------------------------------------------------------------------------------------------
// Backup: hold SAVE in the Reference editor to write a backup file (achievements, numbers on the
// card, the nickname and birthday, and the Reference text) to Downloads; hold EDIT in the
// Reference to pick such a file and restore it.
// ---------------------------------------------------------------------------------------------

object Backup {
    fun build(context: Context): String {
        val o = JSONObject()
        o.put("app", "landroid")
        o.put("format", 1)
        o.put("achievements", Achievements.exportState())
        o.put("profile", Profile.exportState())
        o.put("reference", ReferenceStore.load(context))
        return o.toString(2)
    }

    /** Writes the backup to the phone's Downloads folder. Returns the file name, or null on failure. */
    fun exportToDownloads(context: Context): String? {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        val name = "landroid_backup_$stamp.json"
        val text = build(context)
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values =
                    ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, name)
                        put(MediaStore.Downloads.MIME_TYPE, "application/json")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri == null) {
                    null
                } else {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                    name
                }
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                if (dir == null) {
                    null
                } else {
                    dir.mkdirs()
                    File(dir, name).writeText(text)
                    name
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Reads a backup file and restores it. Returns true on success. */
    fun restoreFrom(context: Context, uri: Uri): Boolean =
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            val o = JSONObject(text)
            if (o.optString("app") != "landroid") {
                false
            } else {
                o.optJSONObject("achievements")?.let { Achievements.importState(it) }
                o.optJSONObject("profile")?.let { p ->
                    Profile.save(p.optString("nickname", ""), p.optInt("birthYear", 2000), p.optInt("birthMonth", 0), p.optInt("birthDay", 1))
                }
                if (o.has("reference")) ReferenceStore.save(context, o.getString("reference"))
                true
            }
        } catch (e: Exception) {
            false
        }
}
