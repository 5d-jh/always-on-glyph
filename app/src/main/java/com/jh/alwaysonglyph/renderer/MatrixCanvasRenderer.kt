package com.jh.alwaysonglyph.renderer

import android.graphics.Bitmap
import android.graphics.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object MatrixCanvasRenderer {

    /**
     * Monospace pixel (bitmap) font definition for low-resolution LED matrices.
     * Glyphs are stored as rows of characters where '#' is a lit pixel and
     * ' ' is off. [width] and [height] are the nominal character cell size,
     * [spacing] is the blank column(s) inserted between characters.
     */
    data class PixelFont(
        val glyphs: Map<Char, Array<String>>,
        val width: Int,
        val height: Int,
        val spacing: Int = 1
    )

    /**
     * Nothing-style dot-matrix font for the 25x25 Glyph Matrix clock.
     * 4 columns + 1 blank spacing, 7 rows tall. Horizontal bars are 2px wide,
     * vertical strokes are 1px, and the middle row is blank unless the digit
     * has a middle bar. ("HHmm" = 4 * 4 + 3 gaps = 19px, fits 25px).
     */
    internal val FONT_5X7: PixelFont = PixelFont(
        width = 4,
        height = 7,
        glyphs = mapOf(
            '0' to arrayOf(
                " ## ",
                "#  #",
                "#  #",
                "    ",
                "#  #",
                "#  #",
                " ## "
            ),
            '1' to arrayOf(
                "  # ",
                "  # ",
                "  # ",
                "    ",
                "  # ",
                "  # ",
                "  # "
            ),
            '2' to arrayOf(
                " ## ",
                "   #",
                "   #",
                " ## ",
                "#   ",
                "#   ",
                " ## "
            ),
            '3' to arrayOf(
                " ## ",
                "   #",
                "   #",
                " ## ",
                "   #",
                "   #",
                " ## "
            ),
            '4' to arrayOf(
                "#  #",
                "#  #",
                "#  #",
                " ## ",
                "   #",
                "   #",
                "   #"
            ),
            '5' to arrayOf(
                " ## ",
                "#   ",
                "#   ",
                " ## ",
                "   #",
                "   #",
                " ## "
            ),
            '6' to arrayOf(
                " ## ",
                "#   ",
                "#   ",
                " ## ",
                "#  #",
                "#  #",
                " ## "
            ),
            '7' to arrayOf(
                " ## ",
                "   #",
                "  # ",
                "    ",
                " #  ",
                " #  ",
                " #  "
            ),
            '8' to arrayOf(
                " ## ",
                "#  #",
                "#  #",
                " ## ",
                "#  #",
                "#  #",
                " ## "
            ),
            '9' to arrayOf(
                " ## ",
                "#  #",
                "#  #",
                " ## ",
                "   #",
                "   #",
                " ## "
            ),
            ':' to arrayOf(
                "    ",
                " ## ",
                " ## ",
                "    ",
                " ## ",
                " ## ",
                "    "
            )
        )
    )

    // 3x5 pixel font for the Matrix bottom status (battery / notification count).
    // 3-wide cell, 5 rows tall, 1px spacing between characters.
    internal val FONT_3X5: PixelFont = PixelFont(
        width = 3,
        height = 5,
        spacing = 1,
        glyphs = mapOf(
            '0' to arrayOf(
                " # ",
                "# #",
                "# #",
                "# #",
                " # "),
            '1' to arrayOf(
                "  #",
                " ##",
                "  #",
                "  #",
                "  #"),
            '2' to arrayOf(
                "###",
                "  #",
                "###",
                "#  ",
                "###"),
            '3' to arrayOf(
                "###",
                "  #",
                "###",
                "  #",
                "###"),
            '4' to arrayOf(
                "# #",
                "# #",
                "###",
                "  #",
                "  #"),
            '5' to arrayOf(
                "###",
                "#  ",
                "###",
                "  #",
                "###"),
            '6' to arrayOf(
                "###",
                "#  ",
                "###",
                "# #",
                "###"),
            '7' to arrayOf(
                "###",
                "  #",
                "  #",
                "  #",
                "  #"),
            '8' to arrayOf(
                "###",
                "# #",
                "###",
                "# #",
                "###"),
            '9' to arrayOf(
                "###",
                "# #",
                "###",
                "  #",
                "###"),
            '%' to arrayOf("# #", "  #", " # ", "#  ", "# #"),
            '·' to arrayOf("   ", "   ", " # ", "   ", "   ")
        )
    )

    /**
     * Renders a 1:1 pixel Bitmap for the 25x25 Glyph Matrix (Nothing Phone (3)).
     * @param time HHmm LocalTime instance
     * @param batteryLevel Current battery percentage (0..100)
     * @param unreadNotifications Count of unread notifications
     * @param use24Hour true for 24-hour clock (HHmm), false for 12-hour clock (hhmm)
     */
    fun renderFrame(
        time: LocalTime = LocalTime.now(),
        batteryLevel: Int = 100,
        unreadNotifications: Int = 0,
        use24Hour: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)

        val timePattern = if (use24Hour) "HHmm" else "hhmm"
        val timeStr = time.format(DateTimeFormatter.ofPattern(timePattern))

        // Top: dot-matrix clock (HHmm) at Y = 6 (Height = 7px)
        drawText(bitmap, timeStr, startY = 6, font = FONT_5X7)

        // Bottom: Battery (%) or Notification Count (· N) at Y = 17 (Height = 5px)
        val bottomText = if (unreadNotifications > 0) {
            "·$unreadNotifications"
        } else {
            "$batteryLevel%"
        }
        drawText(bitmap, bottomText, startY = 17, font = FONT_3X5)

        return bitmap
    }

    private fun drawText(
        bitmap: Bitmap,
        text: String,
        startY: Int,
        font: PixelFont
    ) {
        val totalWidth = text.length * font.width + (text.length - 1) * font.spacing
        var startX = (bitmap.width - totalWidth) / 2
        if (startX < 0) startX = 0

        var currentX = startX
        for (char in text) {
            val glyph = font.glyphs[char]
            if (glyph != null) {
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
            }
            currentX += font.width + font.spacing
        }
    }
}
