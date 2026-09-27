package com.jh.alwaysonglyph.renderer

/**
 * The mini status widget shown at the bottom of the matrix (digital & analog).
 *
 * The widget can be cycled by long-pressing the Glyph button. The available
 * options depend on whether there are unread notifications:
 *  - no notifications: BATTERY <-> TEMPERATURE <-> WEATHER
 *  - with notifications: NOTIFICATION -> BATTERY -> TEMPERATURE -> WEATHER
 */
enum class StatusWidget { NOTIFICATION, BATTERY, TEMPERATURE, WEATHER }

/**
 * Snapshot of the live values a [StatusWidget] may render.
 */
data class StatusData(
    val batteryLevel: Int = 100,
    val temperatureCelsius: Int = 25,
    val weatherCelsius: Int? = null,
    val unreadNotifications: Int = 0,
)

/**
 * Shared widget module: resolves display text and rotation order for the
 * bottom status area, so digital and analog views render identically.
 */
object StatusWidgetModule {

    fun text(widget: StatusWidget, data: StatusData): String = when (widget) {
        StatusWidget.BATTERY -> "${data.batteryLevel}%"
        StatusWidget.TEMPERATURE -> "${data.temperatureCelsius}°"
        StatusWidget.WEATHER -> data.weatherCelsius?.let { "${it}°" } ?: "--°"
        StatusWidget.NOTIFICATION -> "·${data.unreadNotifications}"
    }

    fun availableWidgets(unreadCount: Int): List<StatusWidget> =
        if (unreadCount > 0) {
            listOf(StatusWidget.NOTIFICATION, StatusWidget.BATTERY, StatusWidget.TEMPERATURE, StatusWidget.WEATHER)
        } else {
            listOf(StatusWidget.BATTERY, StatusWidget.TEMPERATURE, StatusWidget.WEATHER)
        }

    fun nextWidget(current: StatusWidget, unreadCount: Int): StatusWidget {
        val available = availableWidgets(unreadCount)
        val index = available.indexOf(current)
        val nextIndex = if (index >= 0) (index + 1) % available.size else 0
        return available[nextIndex]
    }

    /**
     * Falls back to [StatusWidget.BATTERY] when the notification widget is
     * selected but there is nothing unread to display.
     */
    fun effectiveWidget(widget: StatusWidget, unreadCount: Int): StatusWidget =
        if (widget == StatusWidget.NOTIFICATION && unreadCount <= 0) StatusWidget.BATTERY else widget
}
