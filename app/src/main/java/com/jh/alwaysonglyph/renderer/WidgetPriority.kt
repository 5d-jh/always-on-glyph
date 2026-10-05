package com.jh.alwaysonglyph.renderer

/**
 * Priority spec for the status widget.
 *
 * A priority is an ordered list of [StatusWidget]s pushed by the application to
 * override the default (weather) widget. The last element has the highest
 * priority and is the one shown on the Glyph. Each [advance] moves the
 * displayed widget one step toward the first element, wrapping back to the
 * last. Elements can be removed from any position at any time. When the list
 * is empty, [StatusWidget.WEATHER] is shown.
 *
 * Long-pressing cycles through [rotation]: the priority array first, followed
 * by the default widgets that are not currently in the array, wrapping back
 * around.
 */
open class PriorityList {

    private val entries = mutableListOf<StatusWidget>()

    /** The widget currently selected for display, or null when the list is empty. */
    private var currentWidget: StatusWidget? = null

    /** The widget currently displayed, or [StatusWidget.WEATHER] when empty. */
    val current: StatusWidget
        get() = currentWidget ?: StatusWidget.WEATHER

    /** A snapshot of the priority array, highest priority last. */
    val list: List<StatusWidget>
        get() = entries.toList()

    /**
     * The full long-press rotation: the priority array (in priority order)
     * followed by the default widgets not currently in the array. Never empty,
     * falling back to [StatusWidgetModule.DEFAULT_ORDER] when the array is.
     */
    val rotation: List<StatusWidget>
        get() = entries + StatusWidgetModule.DEFAULT_ORDER.filter { it !in entries }

    val isEmpty: Boolean
        get() = entries.isEmpty()

    /**
     * Adds [widget] at the highest priority (end of the array) and makes it the
     * displayed widget. Re-adding an existing widget moves it to the top.
     */
    fun push(widget: StatusWidget): StatusWidget {
        entries.removeAll { it == widget }
        entries.add(widget)
        currentWidget = widget
        return widget
    }

    /**
     * Removes the topmost occurrence of [widget] from any position in the
     * array. If it was the displayed widget, the new last element is shown.
     */
    fun remove(widget: StatusWidget): Boolean {
        val removed = entries.remove(widget)
        if (removed && currentWidget == widget) {
            currentWidget = entries.lastOrNull()
        }
        return removed
    }

    /** Removes every occurrence of [widget] from the array. */
    fun removeAll(widget: StatusWidget): Boolean {
        val removed = entries.removeAll { it == widget }
        if (removed && currentWidget == widget) {
            currentWidget = entries.lastOrNull()
        }
        return removed
    }

    /**
     * Removes the element at [index]. Returns the removed widget, or null when
     * [index] is out of bounds.
     */
    fun removeAt(index: Int): StatusWidget? {
        if (index !in entries.indices) return null
        val removed = entries.removeAt(index)
        if (removed == currentWidget) {
            currentWidget = entries.lastOrNull()
        }
        return removed
    }

    /** Empties the priority array, leaving the display at weather. */
    fun clear() {
        entries.clear()
        currentWidget = null
    }

    /**
     * Advances the displayed widget one step toward the first element of the
     * [rotation], wrapping back to the last element. The priority array is
     * traversed first, then the default widgets outside the array. Returns the
     * newly displayed widget ([StatusWidget.WEATHER] when the rotation is
     * empty, which cannot happen in practice since it always contains the
     * default order).
     */
    fun advance(): StatusWidget {
        val rotation = rotation
        if (rotation.isEmpty()) {
            currentWidget = null
            return StatusWidget.WEATHER
        }
        val index = rotation.indexOf(currentWidget).takeIf { it >= 0 } ?: rotation.lastIndex
        val next = if (index - 1 < 0) rotation.lastIndex else index - 1
        currentWidget = rotation[next]
        return currentWidget!!
    }
}

/**
 * Application-wide priority instance shared between the Glyph toy service and
 * the notification listener.
 */
object WidgetPriority : PriorityList()
