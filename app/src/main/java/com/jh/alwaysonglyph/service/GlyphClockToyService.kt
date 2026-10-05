package com.jh.alwaysonglyph.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import android.os.PowerManager
import android.util.Log
import com.jh.alwaysonglyph.prefs.ClockPreferences
import com.jh.alwaysonglyph.prefs.ClockStyle
import com.jh.alwaysonglyph.receiver.BatteryStateReceiver
import com.jh.alwaysonglyph.renderer.MatrixCanvasRenderer
import com.jh.alwaysonglyph.renderer.StatusData
import com.jh.alwaysonglyph.renderer.StatusWidget
import com.jh.alwaysonglyph.renderer.StatusWidgetModule
import com.jh.alwaysonglyph.renderer.WidgetPriority
import com.jh.alwaysonglyph.weather.WeatherRepository
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphToy
import java.time.LocalTime

class GlyphClockToyService : Service() {

    private var glyphMatrixManager: GlyphMatrixManager? = null
    private var isConnected = false

    private val serviceHandler = Handler(Looper.getMainLooper()) { msg ->
        if (msg.what == GlyphToy.MSG_GLYPH_TOY) {
            val bundle: Bundle? = msg.data
            val event = bundle?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
            Log.d(TAG, "GlyphToy Event received: $event")

            when (event) {
                GlyphToy.EVENT_AOD -> {
                    if (ClockPreferences.isPriorityEnabled(applicationContext)) {
                        WidgetPriority.advance()
                    }
                    refreshAod()
                }
                GlyphToy.EVENT_CHANGE -> {
                    if (ClockPreferences.isPriorityEnabled(applicationContext)) {
                        WidgetPriority.advance()
                        refreshAod()
                    } else {
                        rotateStatusWidget()
                    }
                }
            }
        }
        true
    }

    private val serviceMessenger = Messenger(serviceHandler)

    private val weatherHandler = Handler(Looper.getMainLooper())
    private val weatherRefreshRunnable = object : Runnable {
        override fun run() {
            if (WeatherRepository.isStale()) {
                WeatherRepository.refresh(applicationContext) {
                    weatherHandler.post { refreshAod() }
                }
            }
            weatherHandler.postDelayed(this, WEATHER_REFRESH_INTERVAL_MS)
        }
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> refreshAod()
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    if (ClockPreferences.isTurnOffOnWakeEnabled(applicationContext)) {
                        turnOffMatrix()
                    }
                }
            }
        }
    }

    private val powerStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            syncChargingPriority()
            refreshAod()
        }
    }

    override fun onCreate() {
        super.onCreate()
        WeatherRepository.loadCache(applicationContext)
        registerScreenStateReceiver()
        registerPowerStateReceiver()
    }

    override fun onDestroy() {
        weatherHandler.removeCallbacks(weatherRefreshRunnable)
        unregisterScreenStateReceiver()
        unregisterPowerStateReceiver()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind called")
        initGlyphManager()
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind called")
        try {
            turnOffMatrix()
            glyphMatrixManager?.unInit()
        } catch (e: Exception) {
            Log.e(TAG, "Error during unInit", e)
        }
        glyphMatrixManager = null
        isConnected = false
        return false
    }

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        Log.d(TAG, "Screen state receiver registered")
    }

    private fun unregisterScreenStateReceiver() {
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering screen state receiver", e)
        }
    }

    private fun registerPowerStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(powerStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        Log.d(TAG, "Power state receiver registered")
    }

    private fun unregisterPowerStateReceiver() {
        try {
            unregisterReceiver(powerStateReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering power state receiver", e)
        }
    }

    private fun syncChargingPriority() {
        if (BatteryStateReceiver.isCharging(applicationContext)) {
            WidgetPriority.push(StatusWidget.BATTERY)
        } else {
            WidgetPriority.remove(StatusWidget.BATTERY)
        }
    }

    private fun turnOffMatrix() {
        val manager = glyphMatrixManager ?: return
        if (!isConnected) return
        try {
            manager.turnOff()
            Log.d(TAG, "Matrix turned off (all black)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn off matrix", e)
        }
    }

    private fun isScreenInteractive(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isInteractive ?: true
    }

    private fun refreshAod() {
        if (isScreenInteractive()) return
        if (isAodDisabledNow()) {
            turnOffMatrix()
        } else {
            updateMatrixDisplay()
        }
    }

    private fun isAodDisabledNow(): Boolean {
        if (!ClockPreferences.isAodDisabledEnabled(applicationContext)) return false
        val now = LocalTime.now()
        val nowMinutes = now.hour * 60 + now.minute
        val start = ClockPreferences.getAodDisabledStartMinutes(applicationContext)
        val end = ClockPreferences.getAodDisabledEndMinutes(applicationContext)
        return ClockPreferences.isAodDisabledAt(nowMinutes, start, end)
    }

    private fun initGlyphManager() {
        if (glyphMatrixManager != null) return

        val manager = GlyphMatrixManager.getInstance(applicationContext)
        glyphMatrixManager = manager

        manager.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: ComponentName?) {
                Log.d(TAG, "GlyphMatrixManager connected: $name")
                isConnected = true

                val targetDevice = Glyph.DEVICE_23112

                try {
                    manager.register(targetDevice)
                    syncChargingPriority()
                    refreshAod()
                    scheduleWeatherRefresh()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register target device $targetDevice", e)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d(TAG, "GlyphMatrixManager disconnected")
                isConnected = false
            }
        })
    }

    private fun rotateStatusWidget() {
        val unreadCount = NotificationAccess.getUnreadCount()
        val widgets = ClockPreferences.getActiveWidgets(applicationContext)
        val current = ClockPreferences.getStatusWidget(applicationContext)
        val next = StatusWidgetModule.nextWidget(current, unreadCount, widgets)
        ClockPreferences.setStatusWidget(applicationContext, next)
        updateMatrixDisplay()
    }

    private fun scheduleWeatherRefresh() {
        weatherHandler.removeCallbacks(weatherRefreshRunnable)
        if (WeatherRepository.isStale()) {
            WeatherRepository.refresh(applicationContext) {
                weatherHandler.post { refreshAod() }
            }
        }
        weatherHandler.postDelayed(weatherRefreshRunnable, WEATHER_REFRESH_INTERVAL_MS)
    }

    fun updateMatrixDisplay() {
        val manager = glyphMatrixManager ?: return
        if (!isConnected) return

        try {
            val time = LocalTime.now()
            val batteryLevel = BatteryStateReceiver.getBatteryPercentage(applicationContext)
            val temperature = BatteryStateReceiver.getTemperatureCelsius(applicationContext)
            val unreadCount = NotificationAccess.getUnreadCount()
            val weatherCelsius = WeatherRepository.currentTemperatureCelsius()

            val use24Hour = ClockPreferences.use24HourFormat(applicationContext)
            val style = ClockPreferences.getClockStyle(applicationContext)
            val priorityEnabled = ClockPreferences.isPriorityEnabled(applicationContext)
            val widget = if (priorityEnabled) {
                WidgetPriority.current
            } else {
                ClockPreferences.getStatusWidget(applicationContext)
            }
            val widgets = if (priorityEnabled) {
                StatusWidgetModule.DEFAULT_ORDER
            } else {
                ClockPreferences.getActiveWidgets(applicationContext)
            }
            val data = StatusData(
                batteryLevel = batteryLevel,
                temperatureCelsius = temperature,
                weatherCelsius = weatherCelsius,
                unreadNotifications = unreadCount
            )
            val bitmap = when (style) {
                ClockStyle.DIGITAL -> MatrixCanvasRenderer.renderFrame(
                    time = time,
                    use24Hour = use24Hour,
                    widget = widget,
                    data = data,
                    widgets = widgets
                )
                ClockStyle.ANALOG -> MatrixCanvasRenderer.renderAnalogFrame(
                    time = time,
                    use24Hour = use24Hour,
                    widget = widget,
                    data = data,
                    widgets = widgets
                )
            }

            val matrixObject = GlyphMatrixObject.Builder()
                .setImageSource(bitmap)
                .setPosition(0, 0)
                .setBrightness(ClockPreferences.getBrightness(applicationContext))
                .build()

            val frame = GlyphMatrixFrame.Builder()
                .addTop(matrixObject)
                .build(applicationContext)

            manager.setMatrixFrame(frame)
            Log.d(TAG, "Updated matrix display successfully: time=$time, batt=$batteryLevel%, temp=${temperature}C, weather=${weatherCelsius}C, unread=$unreadCount")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update matrix display", e)
        }
    }

    companion object {
        private const val TAG = "GlyphClockToyService"
        private const val WEATHER_REFRESH_INTERVAL_MS = 30 * 60 * 1000L
    }
}
