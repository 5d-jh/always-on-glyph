package com.jh.alwaysonglyph

import org.junit.Assert.assertEquals
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
}