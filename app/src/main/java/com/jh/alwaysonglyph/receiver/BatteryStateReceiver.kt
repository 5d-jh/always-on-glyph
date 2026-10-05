package com.jh.alwaysonglyph.receiver

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.roundToInt

object BatteryStateReceiver {

    fun getBatteryPercentage(context: Context): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            100
        }
    }

    /**
     * Device temperature in degrees Celsius, sourced from the system battery
     * status ([BatteryManager.EXTRA_TEMPERATURE] is reported in tenths of a
     * degree). Returns 0 when unavailable.
     */
    fun getTemperatureCelsius(context: Context): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val tenths: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        return if (tenths > 0) (tenths / 10.0).roundToInt() else 0
    }

    /**
     * Whether the device is currently plugged in to a power source
     * ([BatteryManager.EXTRA_PLUGGED] is non-zero while charging).
     */
    fun isCharging(context: Context): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        return plugged != 0
    }
}
