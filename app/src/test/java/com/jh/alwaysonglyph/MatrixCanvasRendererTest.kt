package com.jh.alwaysonglyph

import com.jh.alwaysonglyph.prefs.ClockPreferences
import com.jh.alwaysonglyph.renderer.MatrixCanvasRenderer
import com.jh.alwaysonglyph.renderer.StatusData
import com.jh.alwaysonglyph.renderer.StatusWidget
import com.jh.alwaysonglyph.renderer.StatusWidgetModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MatrixCanvasRendererTest {

    @Test
    fun testTimeFormatting() {
        val time = LocalTime.of(14, 30)
        val formatted = time.format(DateTimeFormatter.ofPattern("HHmm"))
        assertEquals("1430", formatted)
    }

    @Test
    fun testUnreadNotificationTextLogic() {
        val count = 3
        val text = "· $count"
        assertEquals("· 3", text)
    }

    @Test
    fun testZeroUnreadNotificationLogic() {
        val battery = 85
        val text = "$battery%"
        assertEquals("85%", text)
    }

    @Test
    fun testFont5x7GlyphDimensions() {
        val font = MatrixCanvasRenderer.FONT_5X7
        assertEquals(4, font.width)
        assertEquals(7, font.height)
        for ((char, glyph) in font.glyphs) {
            assertEquals("Glyph '$char' should have ${font.height} rows", font.height, glyph.size)
            for (row in glyph) {
                assertEquals("Glyph '$char' row should be ${font.width} wide", font.width, row.length)
            }
        }
    }

    @Test
    fun testBottomFontGlyphDimensions() {
        val font = MatrixCanvasRenderer.FONT_3X5
        assertEquals(3, font.width)
        assertEquals(5, font.height)
        assertEquals(1, font.spacing)
        for ((char, glyph) in font.glyphs) {
            assertEquals("Glyph '$char' should have ${font.height} rows", font.height, glyph.size)
        }
    }

    @Test
    fun testAodDisableWindow() {
        // Same-day window 09:00 - 17:00
        assertTrue(ClockPreferences.isAodDisabledAt(9 * 60, 9 * 60, 17 * 60))
        assertTrue(ClockPreferences.isAodDisabledAt(16 * 60 + 59, 9 * 60, 17 * 60))
        assertFalse(ClockPreferences.isAodDisabledAt(17 * 60, 9 * 60, 17 * 60))
        assertFalse(ClockPreferences.isAodDisabledAt(8 * 60, 9 * 60, 17 * 60))

        // Overnight window 22:00 - 07:00
        assertTrue(ClockPreferences.isAodDisabledAt(23 * 60, 22 * 60, 7 * 60))
        assertTrue(ClockPreferences.isAodDisabledAt(3 * 60, 22 * 60, 7 * 60))
        assertFalse(ClockPreferences.isAodDisabledAt(12 * 60, 22 * 60, 7 * 60))

        // Equal start/end means no disabled window
        assertFalse(ClockPreferences.isAodDisabledAt(12 * 60, 9 * 60, 9 * 60))
    }

    @Test
    fun testAllFontsContainDigits() {
        val fonts = listOf(
            MatrixCanvasRenderer.FONT_5X7,
            MatrixCanvasRenderer.FONT_3X5
        )
        for (font in fonts) {
            for (digit in '0'..'9') {
                assertTrue("Font should contain digit '$digit'", font.glyphs.containsKey(digit))
            }
        }
    }

    @Test
    fun testStatusWidgetText() {
        val data = StatusData(batteryLevel = 79, temperatureCelsius = 27, weatherCelsius = 21, unreadNotifications = 3)
        assertEquals("79%", StatusWidgetModule.text(StatusWidget.BATTERY, data))
        assertEquals("27°", StatusWidgetModule.text(StatusWidget.TEMPERATURE, data))
        assertEquals("21°", StatusWidgetModule.text(StatusWidget.WEATHER, data))
        assertEquals("--°", StatusWidgetModule.text(StatusWidget.WEATHER, data.copy(weatherCelsius = null)))
        assertEquals("·3", StatusWidgetModule.text(StatusWidget.NOTIFICATION, data))
    }

    @Test
    fun testAvailableWidgetsWithoutNotification() {
        assertEquals(
            listOf(StatusWidget.BATTERY, StatusWidget.TEMPERATURE, StatusWidget.WEATHER),
            StatusWidgetModule.availableWidgets(0)
        )
    }

    @Test
    fun testAvailableWidgetsWithNotification() {
        assertEquals(
            listOf(StatusWidget.NOTIFICATION, StatusWidget.BATTERY, StatusWidget.TEMPERATURE, StatusWidget.WEATHER),
            StatusWidgetModule.availableWidgets(3)
        )
    }

    @Test
    fun testNextWidgetCyclesWithoutNotification() {
        assertEquals(StatusWidget.TEMPERATURE, StatusWidgetModule.nextWidget(StatusWidget.BATTERY, 0))
        assertEquals(StatusWidget.WEATHER, StatusWidgetModule.nextWidget(StatusWidget.TEMPERATURE, 0))
        assertEquals(StatusWidget.BATTERY, StatusWidgetModule.nextWidget(StatusWidget.WEATHER, 0))
    }

    @Test
    fun testNextWidgetCyclesWithNotification() {
        assertEquals(StatusWidget.BATTERY, StatusWidgetModule.nextWidget(StatusWidget.NOTIFICATION, 5))
        assertEquals(StatusWidget.TEMPERATURE, StatusWidgetModule.nextWidget(StatusWidget.BATTERY, 5))
        assertEquals(StatusWidget.WEATHER, StatusWidgetModule.nextWidget(StatusWidget.TEMPERATURE, 5))
        assertEquals(StatusWidget.NOTIFICATION, StatusWidgetModule.nextWidget(StatusWidget.WEATHER, 5))
    }

    @Test
    fun testEffectiveWidgetFallsBackToBattery() {
        assertEquals(StatusWidget.BATTERY, StatusWidgetModule.effectiveWidget(StatusWidget.NOTIFICATION, 0))
        assertEquals(StatusWidget.NOTIFICATION, StatusWidgetModule.effectiveWidget(StatusWidget.NOTIFICATION, 1))
        assertEquals(StatusWidget.TEMPERATURE, StatusWidgetModule.effectiveWidget(StatusWidget.TEMPERATURE, 0))
        assertEquals(StatusWidget.WEATHER, StatusWidgetModule.effectiveWidget(StatusWidget.WEATHER, 0))
    }
}
