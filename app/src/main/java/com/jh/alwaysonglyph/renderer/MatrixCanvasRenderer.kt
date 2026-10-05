package com.jh.alwaysonglyph.renderer

import android.graphics.Bitmap
import android.graphics.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object MatrixCanvasRenderer {

    private val HOUR_MARKER_COLOR = 0xFF555555.toInt()

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
                "   #",
                "    ",
                "  # ",
                "  # ",
                "  # "
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
            '·' to arrayOf("   ", " # ", "# #", " # ", "   "),
            '°' to arrayOf(" # ", "# #", " # ", "   ", "   "),
            '-' to arrayOf("   ", "   ", "###", "   ", "   "),
            'W' to arrayOf(
                "# # #",
                "# # #",
                " # # ",
                "    ",
                "    ")
        )
    )

    /**
     * Renders a 1:1 pixel Bitmap for the 25x25 Glyph Matrix (Nothing Phone (3)).
     * @param time HHmm LocalTime instance
     * @param use24Hour true for 24-hour clock (HHmm), false for 12-hour clock (hhmm)
     * @param widget the mini status widget to show in the bottom area
     * @param data live values backing the status widget
     */
    fun renderFrame(
        time: LocalTime = LocalTime.now(),
        use24Hour: Boolean = true,
        widget: StatusWidget = StatusWidget.BATTERY,
        data: StatusData = StatusData(),
        widgets: List<StatusWidget> = StatusWidgetModule.DEFAULT_ORDER
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)

        val timePattern = if (use24Hour) "HHmm" else "hhmm"
        val timeStr = time.format(DateTimeFormatter.ofPattern(timePattern))

        // Top: dot-matrix clock (HHmm) at Y = 6 (Height = 7px)
        drawText(bitmap, timeStr, startY = 6, font = FONT_5X7)

        // Bottom: status widget at Y = 17 (Height = 5px)
        drawText(bitmap, statusText(widget, data, widgets), startY = 17, font = FONT_3X5)

        return bitmap
    }

    /**
     * Renders an analog-style clock for the 25x25 Glyph Matrix.
     * The matrix is a 25x25 circle of LEDs (radius 12.5, 489 LEDs).
     *  - hour markers around the edge (12 o'clock = downward triangle,
     *    3/6/9 o'clock = 2px line, other hours = 1px line), drawn dimmer;
     *    a marker lights up fully when the minute dot reaches it
     *  - minute dot placed on the edge
     *  - hour (HH) centered in the upper half
     *  - status widget centered in the lower half
     */
    fun renderAnalogFrame(
        time: LocalTime = LocalTime.now(),
        use24Hour: Boolean = true,
        widget: StatusWidget = StatusWidget.BATTERY,
        data: StatusData = StatusData(),
        widgets: List<StatusWidget> = StatusWidgetModule.DEFAULT_ORDER,
        showMinuteDot: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)

        val cx = 12
        val cy = 12

        // 1. Hour markers around the edge (dimmed; lights up when the minute dot reaches it)
        for (hour in 0..11) {
            val lit = time.minute == hour * 5
            val color = if (lit) Color.WHITE else HOUR_MARKER_COLOR
            when (hour) {
                0 -> drawTriangleDown(bitmap, cx, color)
                3, 6, 9 -> drawRadialTick(bitmap, cx, cy, hour, length = 2, color = color)
                else -> drawRadialTick(bitmap, cx, cy, hour, length = 1, color = color)
            }
        }

        // 2. Minute dot on the edge (blinks on/off every 3s)
        if (showMinuteDot) {
            val (mdx, mdy) = pointOnRing(cx, cy, 12, time.minute)
            setPixel(bitmap, mdx, mdy, Color.WHITE)
        }

        // 3. Hour (HH), horizontally centered
        val hour = if (use24Hour) {
            time.hour
        } else {
            time.hour % 12
        }.let { if (it == 0) 12 else it }
        val hourText = hour.toString().padStart(2, '0')
        drawTextCentered(bitmap, hourText, centerY = 8, font = FONT_5X7)

        // 4. Status widget, centered
        drawTextCentered(bitmap, statusText(widget, data, widgets), centerY = 16, font = FONT_3X5)

        return bitmap
    }

    /**
     * Shared status widget text for the bottom area, resolving the effective
     * widget (e.g. notification falls back to battery when nothing is unread).
     */
    private fun statusText(widget: StatusWidget, data: StatusData, widgets: List<StatusWidget>): String =
        StatusWidgetModule.text(
            StatusWidgetModule.effectiveWidget(widget, data.unreadNotifications, widgets),
            data
        )

    private fun drawTextCentered(
        bitmap: Bitmap,
        text: String,
        centerY: Int,
        font: PixelFont,
        color: Int = Color.WHITE
    ) {
        val startY = centerY - font.height / 2
        drawText(bitmap, text, startY, font, color)
    }

    private fun drawRadialTick(
        bitmap: Bitmap,
        cx: Int,
        cy: Int,
        hour: Int,
        length: Int,
        color: Int = Color.WHITE
    ) {
        val (ox, oy) = pointOnRing(cx, cy, 12, hour * 5)
        var x = ox
        var y = oy
        val dx = abs(cx - ox)
        val dy = -abs(cy - oy)
        val sx = if (ox < cx) 1 else -1
        val sy = if (oy < cy) 1 else -1
        var err = dx + dy
        var drawn = 0
        while (drawn < length) {
            setPixel(bitmap, x, y, color)
            drawn++
            if (drawn >= length) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                x += sx
            }
            if (e2 <= dx) {
                err += dx
                y += sy
            }
        }
    }

    private fun drawTriangleDown(bitmap: Bitmap, cx: Int, color: Int = Color.WHITE) {
        setPixel(bitmap, cx - 1, 0, color)
        setPixel(bitmap, cx, 0, color)
        setPixel(bitmap, cx + 1, 0, color)
        setPixel(bitmap, cx, 1, color)
    }

    private fun pointOnRing(cx: Int, cy: Int, radius: Int, minute: Int): Pair<Int, Int> {
        val angleRad = Math.toRadians(minute * 6.0)
        val x = cx + (radius * sin(angleRad)).roundToInt()
        val y = cy - (radius * cos(angleRad)).roundToInt()
        return x to y
    }

    private fun setPixel(bitmap: Bitmap, x: Int, y: Int, color: Int) {
        if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
            bitmap.setPixel(x, y, color)
        }
    }

    private fun drawText(
        bitmap: Bitmap,
        text: String,
        startY: Int,
        font: PixelFont,
        color: Int = Color.WHITE
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
                                bitmap.setPixel(px, py, color)
                            }
                        }
                    }
                }
            }
            currentX += font.width + font.spacing
        }
    }
}
