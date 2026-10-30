package com.dede.basic;

import android.content.Context;

/** Standalone stand-in for the host app's SpUtils. */
public final class SpUtils {
    private SpUtils() {}

    public static void putLong(Context context, String key, long value) {
        context.getSharedPreferences("landroid_prefs", Context.MODE_PRIVATE)
                .edit().putLong(key, value).apply();
    }
}
