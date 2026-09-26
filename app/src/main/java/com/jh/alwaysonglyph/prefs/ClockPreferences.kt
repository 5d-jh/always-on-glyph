package com.jh.alwaysonglyph.prefs

import android.content.Context
import android.content.SharedPreferences

object ClockPreferences {
    private const val PREFS_NAME = "clock_prefs"
    private const val KEY_USE_24_HOUR = "use_24_hour"
    private const val KEY_BRIGHTNESS = "brightness"
    private const val KEY_DISABLE_ENABLED = "disable_enabled"
    private const val KEY_DISABLE_START_MINUTES = "disable_start_minutes"
    private const val KEY_DISABLE_END_MINUTES = "disable_end_minutes"

    private const val DEFAULT_BRIGHTNESS = 200
    private const val DEFAULT_DISABLE_START_MINUTES = 22 * 60
    private const val DEFAULT_DISABLE_END_MINUTES = 7 * 60

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun use24HourFormat(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USE_24_HOUR, true)

    fun setUse24HourFormat(context: Context, use24Hour: Boolean) {
        prefs(context).edit().putBoolean(KEY_USE_24_HOUR, use24Hour).apply()
    }

    fun getBrightness(context: Context): Int =
        prefs(context).getInt(KEY_BRIGHTNESS, DEFAULT_BRIGHTNESS)

    fun setBrightness(context: Context, brightness: Int) {
        prefs(context).edit().putInt(KEY_BRIGHTNESS, brightness.coerceIn(0, 255)).apply()
    }

    fun isAodDisabledEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DISABLE_ENABLED, false)

    fun setAodDisabledEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DISABLE_ENABLED, enabled).apply()
    }

    fun getAodDisabledStartMinutes(context: Context): Int =
        prefs(context).getInt(KEY_DISABLE_START_MINUTES, DEFAULT_DISABLE_START_MINUTES)

    fun getAodDisabledEndMinutes(context: Context): Int =
        prefs(context).getInt(KEY_DISABLE_END_MINUTES, DEFAULT_DISABLE_END_MINUTES)

    fun setAodDisabledStartMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_DISABLE_START_MINUTES, minutes.coerceIn(0, 1439)).apply()
    }

    fun setAodDisabledEndMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_DISABLE_END_MINUTES, minutes.coerceIn(0, 1439)).apply()
    }

    /**
     * Returns true when [nowMinutes] falls inside the disabled window defined by
     * [startMinutes] and [endMinutes]. Supports overnight ranges (start > end).
     */
    fun isAodDisabledAt(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
        return when {
            startMinutes < endMinutes -> nowMinutes in startMinutes until endMinutes
            startMinutes > endMinutes -> nowMinutes >= startMinutes || nowMinutes < endMinutes
            else -> false
        }
    }

    fun minutesToTimeString(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    fun timeStringToMinutes(value: String): Int? {
        val parts = value.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }
}
