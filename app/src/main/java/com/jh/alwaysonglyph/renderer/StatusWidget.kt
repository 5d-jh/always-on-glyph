package com.jh.alwaysonglyph.renderer

import android.content.Context
import com.jh.alwaysonglyph.R

/**
 * Localized display name for the widget.
 */
fun StatusWidget.displayName(context: Context): String = context.getString(
    when (this) {
        StatusWidget.NOTIFICATION -> R.string.widget_notification
        StatusWidget.BATTERY -> R.string.widget_battery
        StatusWidget.WATTAGE -> R.string.widget_wattage
        StatusWidget.TEMPERATURE -> R.string.widget_temperature
        StatusWidget.WEATHER -> R.string.widget_weather
    }
)

/**
 * Localized description for the widget.
 */
fun StatusWidget.description(context: Context): String = context.getString(
    when (this) {
        StatusWidget.NOTIFICATION -> R.string.widget_notification_desc
        StatusWidget.BATTERY -> R.string.widget_battery_desc
        StatusWidget.WATTAGE -> R.string.widget_wattage_desc
        StatusWidget.TEMPERATURE -> R.string.widget_temperature_desc
        StatusWidget.WEATHER -> R.string.widget_weather_desc
    }
)

/**
 * The mini status widget shown at the bottom of the matrix (digital & analog).
 *
 * The widget can be cycled by long-pressing the Glyph button. The available
 * options and their order are user-configurable (see EditWidgetsActivity).
 * [StatusWidget.NOTIFICATION] is only shown while there are unread
 * notifications, regardless of its position in the configured order.
 */
enum class StatusWidget { NOTIFICATION, BATTERY, WATTAGE, TEMPERATURE, WEATHER }

/**
 * Snapshot of the live values a [StatusWidget] may render.
 */
data class StatusData(
    val batteryLevel: Int = 100,
    val temperatureCelsius: Int = 25,
    val weatherCelsius: Int? = null,
    val unreadNotifications: Int = 0,
    val wattage: Int = 0,
)

/**
 * Shared widget module: resolves display text and rotation order for the
 * bottom status area, so digital and analog views render identically.
 */
object StatusWidgetModule {

    /**
     * Default rotation order. Matches the pre-customisation behaviour:
     * NOTIFICATION first (only while unread), then BATTERY, TEMPERATURE, WEATHER.
     */
    val DEFAULT_ORDER: List<StatusWidget> = listOf(
        StatusWidget.NOTIFICATION,
        StatusWidget.BATTERY,
        StatusWidget.TEMPERATURE,
        StatusWidget.WEATHER,
    )

    fun text(widget: StatusWidget, data: StatusData): String = when (widget) {
        StatusWidget.BATTERY -> "${data.batteryLevel}%"
        StatusWidget.WATTAGE -> "${data.wattage}W"
        StatusWidget.TEMPERATURE -> "${data.temperatureCelsius}°"
        StatusWidget.WEATHER -> data.weatherCelsius?.let { "${it}°" } ?: "--°"
        StatusWidget.NOTIFICATION -> "·${data.unreadNotifications}"
    }

    /**
     * The widgets available in the rotation, in configured order. The
     * notification widget is dropped while there is nothing unread, and is
     * bumped to the front whenever there is something unread.
     */
    fun availableWidgets(unreadCount: Int, widgets: List<StatusWidget> = DEFAULT_ORDER): List<StatusWidget> {
        val others = widgets.filter { it != StatusWidget.NOTIFICATION }
        return if (unreadCount > 0 && StatusWidget.NOTIFICATION in widgets) {
            listOf(StatusWidget.NOTIFICATION) + others
        } else {
            others
        }
    }

    fun nextWidget(current: StatusWidget, unreadCount: Int, widgets: List<StatusWidget> = DEFAULT_ORDER): StatusWidget {
        val available = availableWidgets(unreadCount, widgets)
        if (available.isEmpty()) return current
        val index = available.indexOf(current)
        val nextIndex = if (index >= 0) (index + 1) % available.size else 0
        return available[nextIndex]
    }

    /**
     * Resolves the widget actually displayed. Falls back to the first available
     * widget when the selected one is not in the rotation (e.g. the
     * notification widget is selected but there is nothing unread).
     */
    fun effectiveWidget(widget: StatusWidget, unreadCount: Int, widgets: List<StatusWidget> = DEFAULT_ORDER): StatusWidget {
        val available = availableWidgets(unreadCount, widgets)
        if (available.isEmpty()) return StatusWidget.BATTERY
        return if (widget in available) widget else available.first()
    }
}
