package com.jh.alwaysonglyph.prefs

import android.content.Context
import android.content.SharedPreferences

object ClockPreferences {
    private const val PREFS_NAME = "clock_prefs"
    private const val KEY_USE_24_HOUR = "use_24_hour"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun use24HourFormat(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USE_24_HOUR, true)

    fun setUse24HourFormat(context: Context, use24Hour: Boolean) {
        prefs(context).edit().putBoolean(KEY_USE_24_HOUR, use24Hour).apply()
    }
}
