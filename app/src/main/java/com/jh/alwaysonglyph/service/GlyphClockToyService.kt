package com.jh.alwaysonglyph.service

import android.app.Service
import android.content.ComponentName
import android.content.Intent
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
import com.nothing.ketchum.Common
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
                GlyphToy.EVENT_AOD, GlyphToy.EVENT_CHANGE -> {
                    updateMatrixDisplay()
                }
            }
        }
        true
    }

    private val serviceMessenger = Messenger(serviceHandler)

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind called")
        initGlyphManager()
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind called")
        try {
            glyphMatrixManager?.unInit()
        } catch (e: Exception) {
            Log.e(TAG, "Error during unInit", e)
        }
        glyphMatrixManager = null
        isConnected = false
        return false
    }

    private fun initGlyphManager() {
        if (glyphMatrixManager != null) return

        val manager = GlyphMatrixManager.getInstance(applicationContext)
        glyphMatrixManager = manager

        manager.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: ComponentName?) {
                Log.d(TAG, "GlyphMatrixManager connected: $name")
                isConnected = true

                val matrixLength = Common.getDeviceMatrixLength()
                val targetDevice = if (matrixLength <= 13) {
                    Glyph.DEVICE_25111p
                } else {
                    Glyph.DEVICE_23112
                }

                try {
                    manager.register(targetDevice)
                    updateMatrixDisplay()
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
            val matrixLength = Common.getDeviceMatrixLength()
            val time = LocalTime.now()
            val batteryLevel = BatteryStateReceiver.getBatteryPercentage(applicationContext)
            val unreadCount = UnreadNotificationListenerService.getUnreadCount()

            val use24Hour = ClockPreferences.use24HourFormat(applicationContext)
            val bitmap = MatrixCanvasRenderer.renderFrame(
                matrixSize = matrixLength,
                time = time,
                batteryLevel = batteryLevel,
                unreadNotifications = unreadCount,
                use24Hour = use24Hour
            )

            val matrixObject = GlyphMatrixObject.Builder()
                .setImageSource(bitmap)
                .setPosition(0, 0)
                .setBrightness(200)
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
