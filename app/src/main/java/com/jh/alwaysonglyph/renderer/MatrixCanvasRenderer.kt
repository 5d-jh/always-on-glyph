package com.jh.alwaysonglyph.renderer

import android.graphics.Bitmap
import android.graphics.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object MatrixCanvasRenderer {

    // Nothing-style 4x6 Pixel Font for 25x25 Matrix clock
    private val FONT_4X6_NOTHING: Map<Char, Array<String>> = mapOf(
        '0' to arrayOf(
            "####",
            "#  #",
            "#  #",
            "#  #",
            "#  #",
            "####"
        ),
        '1' to arrayOf(
            "  ##",
            " ###",
            "  ##",
            "  ##",
            "  ##",
            "####"
        ),
        '2' to arrayOf(
            "####",
            "   #",
            "####",
            "#   ",
            "#   ",
            "####"
        ),
        '3' to arrayOf(
            "####",
            "   #",
            "####",
            "   #",
            "   #",
            "####"
        ),
        '4' to arrayOf(
            "#  #",
            "#  #",
            "####",
            "   #",
            "   #",
            "   #"
        ),
        '5' to arrayOf(
            "####",
            "#   ",
            "####",
            "   #",
            "   #",
            "####"
        ),
        '6' to arrayOf(
            "####",
            "#   ",
            "####",
            "#  #",
            "#  #",
            "####"
        ),
        '7' to arrayOf(
            "####",
            "   #",
            "  # ",
            " #  ",
            " #  ",
            " #  "
        ),
        '8' to arrayOf(
            "####",
            "#  #",
            "####",
            "#  #",
            "#  #",
            "####"
        ),
        '9' to arrayOf(
            "####",
            "#  #",
            "####",
            "   #",
            "   #",
            "####"
        )
    )

    // Standard 3x5 Pixel Font for 25x25 Matrix bottom text (battery/notification count)
    private val FONT_3X5: Map<Char, Array<String>> = mapOf(
        '0' to arrayOf("###", "# #", "# #", "# #", "###"),
        '1' to arrayOf(" # ", "## ", " # ", " # ", "###"),
        '2' to arrayOf("###", "  #", "###", "#  ", "###"),
        '3' to arrayOf("###", "  #", "###", "  #", "###"),
        '4' to arrayOf("# #", "# #", "###", "  #", "  #"),
        '5' to arrayOf("###", "#  ", "###", "  #", "###"),
        '6' to arrayOf("###", "#  ", "###", "# #", "###"),
        '7' to arrayOf("###", "  #", "  #", "  #", "  #"),
        '8' to arrayOf("###", "# #", "###", "# #", "###"),
        '9' to arrayOf("###", "# #", "###", "  #", "###"),
        '%' to arrayOf("# #", "  #", " # ", "#  ", "# #"),
        '·' to arrayOf("   ", "   ", " # ", "   ", "   ")
    )

    // Compact 2x6 Pixel Font for 13x13 Matrix Clock
    private val FONT_2X6: Map<Char, Array<String>> = mapOf(
        '0' to arrayOf("##", "# #", "# #", "# #", "# #", "##"),
        '1' to arrayOf(" #", "##", " #", " #", " #", "##"),
        '2' to arrayOf("##", " #", "##", "# ", "# ", "##"),
        '3' to arrayOf("##", " #", "##", " #", " #", "##"),
        '4' to arrayOf("# #", "# #", "##", " #", " #", " #"),
        '5' to arrayOf("##", "# ", "##", " #", " #", "##"),
        '6' to arrayOf("##", "# ", "##", "# #", "# #", "##"),
        '7' to arrayOf("##", " #", " #", " #", " #", " #"),
        '8' to arrayOf("##", "# #", "##", "# #", "# #", "##"),
        '9' to arrayOf("##", "# #", "##", " #", " #", "##")
    )

    // Compact 2x5 Pixel Font for 13x13 Matrix Bottom text
    private val FONT_2X5: Map<Char, Array<String>> = mapOf(
        '0' to arrayOf("##", "# #", "# #", "# #", "##"),
        '1' to arrayOf(" #", "##", " #", " #", "##"),
        '2' to arrayOf("##", " #", "##", "# ", "##"),
        '3' to arrayOf("##", " #", "##", " #", "##"),
        '4' to arrayOf("# #", "# #", "##", " #", " #"),
        '5' to arrayOf("##", "# ", "##", " #", "##"),
        '6' to arrayOf("##", "# ", "##", "# #", "##"),
        '7' to arrayOf("##", " #", " #", " #", " #"),
        '8' to arrayOf("##", "# #", "##", "# #", "##"),
        '9' to arrayOf("##", "# #", "##", " #", "##"),
        '%' to arrayOf("# #", " #", " #", "# #", "# #"),
        '·' to arrayOf("  ", "  ", " #", "  ", "  ")
    )

    /**
     * Renders a 1:1 pixel Bitmap for the Matrix.
     * @param matrixSize Size of matrix (25 for Phone (3), 13 for Phone (4a) Pro)
     * @param time HHmm LocalTime instance
     * @param batteryLevel Current battery percentage (0..100)
     * @param unreadNotifications Count of unread notifications
     * @param use24Hour true for 24-hour clock (HHmm), false for 12-hour clock (hhmm)
     */
    fun renderFrame(
        matrixSize: Int,
        time: LocalTime = LocalTime.now(),
        batteryLevel: Int = 100,
        unreadNotifications: Int = 0,
        use24Hour: Boolean = true
    ): Bitmap {
        val size = if (matrixSize <= 13) 13 else 25
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)

        val timePattern = if (use24Hour) "HHmm" else "hhmm"
        val timeStr = time.format(DateTimeFormatter.ofPattern(timePattern))

        if (size == 25) {
            // 25x25 Matrix Layout
            // Top: Nothing-style Clock (HHmm) at Y = 4 (Height = 6px)
            drawText(bitmap, timeStr, startY = 4, font = FONT_4X6_NOTHING)

            // Bottom: Battery (%) or Notification Count (· N) at Y = 18 (Height = 5px)
            val bottomText = if (unreadNotifications > 0) {
                "· $unreadNotifications"
            } else {
                "$batteryLevel%"
            }
            drawText(bitmap, bottomText, startY = 18, font = FONT_3X5)
        } else {
            // 13x13 Matrix Layout
            // Top: Clock (HHmm) at Y = 0 (Height = 6px)
            drawText(bitmap, timeStr, startY = 0, font = FONT_2X6)

            // Bottom: Battery or Notification at Y = 7 (Height = 5px)
            val bottomText = if (unreadNotifications > 0) {
                "·$unreadNotifications"
            } else {
                "$batteryLevel%"
            }
            drawText(bitmap, bottomText, startY = 7, font = FONT_2X5)
        }

        return bitmap
    }

    private fun drawText(
        bitmap: Bitmap,
        text: String,
        startY: Int,
        font: Map<Char, Array<String>>
    ) {
        var totalWidth = 0
        for (char in text) {
            val glyph = font[char]
            val width = glyph?.firstOrNull()?.length ?: 3
            totalWidth += width + 1
        }
        if (totalWidth > 0) totalWidth -= 1 // remove trailing gap

        var startX = (bitmap.width - totalWidth) / 2
        if (startX < 0) startX = 0

        var currentX = startX
        for (char in text) {
            val glyph = font[char] ?: font.values.first()
            val charWidth = glyph.first().length
            for (row in glyph.indices) {
                val line = glyph[row]
                for (col in line.indices) {
                    if (line[col] != ' ') {
                        val px = currentX + col
                        val py = startY + row
                        if (px in 0 until bitmap.width && py in 0 until bitmap.height) {
                            bitmap.setPixel(px, py, Color.WHITE)
                        }
                    }
                }
            }
            currentX += charWidth + 1
        }
    }
}
