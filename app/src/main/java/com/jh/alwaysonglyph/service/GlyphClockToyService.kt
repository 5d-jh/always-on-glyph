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
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.jh.alwaysonglyph.receiver.BatteryStateReceiver
import com.jh.alwaysonglyph.renderer.MatrixCanvasRenderer
import com.jh.alwaysonglyph.prefs.ClockPreferences
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
                GlyphToy.EVENT_AOD -> refreshAod()
                GlyphToy.EVENT_CHANGE -> updateMatrixDisplay()
            }
        }
        true
    }

    private val serviceMessenger = Messenger(serviceHandler)

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> turnOffMatrix()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerScreenStateReceiver()
    }

    override fun onDestroy() {
        unregisterScreenStateReceiver()
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

    private fun refreshAod() {
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
                    refreshAod()
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

    fun updateMatrixDisplay() {
        val manager = glyphMatrixManager ?: return
        if (!isConnected) return

        try {
            val time = LocalTime.now()
            val batteryLevel = BatteryStateReceiver.getBatteryPercentage(applicationContext)
            val unreadCount = UnreadNotificationListenerService.getUnreadCount()

            val use24Hour = ClockPreferences.use24HourFormat(applicationContext)
            val bitmap = MatrixCanvasRenderer.renderFrame(
                time = time,
                batteryLevel = batteryLevel,
                unreadNotifications = unreadCount,
                use24Hour = use24Hour
            )

            val matrixObject = GlyphMatrixObject.Builder()
                .setImageSource(bitmap)
                .setPosition(0, 0)
                .setBrightness(ClockPreferences.getBrightness(applicationContext))
                .build()

            val frame = GlyphMatrixFrame.Builder()
                .addTop(matrixObject)
                .build(applicationContext)

            manager.setMatrixFrame(frame)
            Log.d(TAG, "Updated matrix display successfully: time=$time, batt=$batteryLevel%, unread=$unreadCount")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update matrix display", e)
        }
    }

    companion object {
        private const val TAG = "GlyphClockToyService"
    }
}
