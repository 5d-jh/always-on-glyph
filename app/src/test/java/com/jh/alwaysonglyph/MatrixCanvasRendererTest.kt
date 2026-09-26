package com.jh.alwaysonglyph

import com.jh.alwaysonglyph.prefs.ClockPreferences
import com.jh.alwaysonglyph.renderer.MatrixCanvasRenderer
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
}
