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

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------------------------
// The explorer's profile: a nickname and a birthday, asked once when the game is opened for the
// first time. The nickname goes on the card in the Reference; the birthday is the Happy Birthday
// world.
// ---------------------------------------------------------------------------------------------

object Profile {
    private var prefs: SharedPreferences? = null

    var nickname by mutableStateOf("")
        private set

    /** Month is 0-based (0 = January), like the calendar. */
    var birthMonth by mutableIntStateOf(-1)
        private set

    var birthDay by mutableIntStateOf(-1)
        private set

    var birthYear by mutableIntStateOf(-1)
        private set

    /** False until the first-launch question has been answered. */
    var done by mutableStateOf(true)
        private set

    val hasBirthday: Boolean
        get() = birthMonth in 0..11 && birthDay in 1..31

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("landroid_profile", Context.MODE_PRIVATE)
        prefs = p
        nickname = p.getString("nickname", "") ?: ""
        birthYear = p.getInt("birthYear", -1)
        birthMonth = p.getInt("birthMonth", -1)
        birthDay = p.getInt("birthDay", -1)
        done = p.getBoolean("done", false)
    }

    fun save(name: String, year: Int, month0: Int, day: Int) {
        nickname = name.trim().take(24)
        birthYear = year
        birthMonth = month0
        birthDay = day
        done = true
        prefs
            ?.edit()
            ?.putString("nickname", nickname)
            ?.putInt("birthYear", year)
            ?.putInt("birthMonth", month0)
            ?.putInt("birthDay", day)
            ?.putBoolean("done", true)
            ?.apply()
    }

    fun exportState(): org.json.JSONObject =
        org.json.JSONObject()
            .put("nickname", nickname)
            .put("birthYear", birthYear)
            .put("birthMonth", birthMonth)
            .put("birthDay", birthDay)

    /** True when a calendar date (any year) is the explorer's birthday. */
    fun isBirthday(month0: Int, day: Int): Boolean = hasBirthday && month0 == birthMonth && day == birthDay
}

/** The first-launch question. There is no way past it except answering. */
@Composable
internal fun ProfilePopup() {
    var name by remember { mutableStateOf("") }
    var y by remember { mutableIntStateOf(2000) }
    var m by remember { mutableIntStateOf(0) }
    var d by remember { mutableIntStateOf(1) }

    BackHandler(enabled = true) {} // no skipping

    OverlayScreen(
        title = "give us your sweet data",
        onClose = {},
        scroll = true,
        headerActions = {},
    ) {
        Text(style = MenuText, color = Colors.Console, text = "Enter your nickname")
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = name,
            onValueChange = { v -> name = v.replace("\n", "").take(24) },
            singleLine = true,
            textStyle = MenuText.copy(fontSize = 15.sp, color = Colors.Console),
            cursorBrush = SolidColor(Colors.Android.Chartreuse),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().border(1.dp, Colors.Console).padding(10.dp)) {
                    if (name.isEmpty()) {
                        Text(style = MenuText.copy(fontSize = 15.sp), color = Colors.Eigengrau3, text = "nickname")
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            style = MenuText.copy(fontSize = 11.sp),
            color = Colors.Eigengrau4,
            text = "whatever whichever you like",
        )

        Spacer(Modifier.height(22.dp))
        Text(style = MenuText, color = Colors.Console, text = "select your birthdate")
        Spacer(Modifier.height(8.dp))
        DatePicker(selY = y, selM = m, selD = d) { ny, nm, nd ->
            y = ny
            m = nm
            d = nd
        }
        Spacer(Modifier.height(8.dp))
        Text(
            style = MenuText,
            color = Colors.Console,
            text = "%04d%02d%02d".format(y, m + 1, d),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            style = MenuText.copy(fontSize = 11.sp),
            color = Colors.Eigengrau4,
            text = "two truths n one lie works as well",
        )

        Spacer(Modifier.height(18.dp))
        ConsoleButton(
            textStyle = MenuText,
            color = Colors.Eigengrau,
            bgColor = Colors.Autopilot,
            borderColor = Colors.Autopilot,
            text = "CONTINUE",
        ) {
            Profile.save(name, y, m, d)
        }
        Spacer(Modifier.height(24.dp))
    }
}
