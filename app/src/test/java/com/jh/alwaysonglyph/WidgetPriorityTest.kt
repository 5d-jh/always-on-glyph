package com.jh.alwaysonglyph.renderer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPriorityTest {

    @Test
    fun emptyListShowsWeather() {
        val priority = PriorityList()
        assertTrue(priority.isEmpty)
        assertEquals(StatusWidget.WEATHER, priority.current)
    }

    @Test
    fun pushShowsLastElement() {
        val priority = PriorityList()
        priority.push(StatusWidget.NOTIFICATION)
        assertEquals(StatusWidget.NOTIFICATION, priority.current)

        priority.push(StatusWidget.BATTERY)
        assertEquals(StatusWidget.BATTERY, priority.current)
        assertEquals(
            listOf(StatusWidget.NOTIFICATION, StatusWidget.BATTERY),
            priority.list
        )
    }

    @Test
    fun advanceCyclesFromLastToFirstAndWraps() {
        val priority = PriorityList()
        priority.push(StatusWidget.NOTIFICATION)
        priority.push(StatusWidget.BATTERY)

        assertEquals(StatusWidget.BATTERY, priority.current)
        assertEquals(StatusWidget.NOTIFICATION, priority.advance())
        assertEquals(StatusWidget.BATTERY, priority.advance())
    }

    @Test
    fun advanceOnSingleElementStaysOnIt() {
        val priority = PriorityList()
        priority.push(StatusWidget.BATTERY)
        assertEquals(StatusWidget.BATTERY, priority.advance())
        assertEquals(StatusWidget.BATTERY, priority.current)
    }

    @Test
    fun advanceOnEmptyReturnsWeather() {
        val priority = PriorityList()
        assertEquals(StatusWidget.WEATHER, priority.advance())
        assertEquals(StatusWidget.WEATHER, priority.current)
    }

    @Test
    fun removeFromAnyPosition() {
        val priority = PriorityList()
        priority.push(StatusWidget.NOTIFICATION)
        priority.push(StatusWidget.BATTERY)
        priority.push(StatusWidget.WEATHER)

        assertEquals(StatusWidget.BATTERY, priority.removeAt(1))
        assertEquals(
            listOf(StatusWidget.NOTIFICATION, StatusWidget.WEATHER),
            priority.list
        )
        assertEquals(StatusWidget.WEATHER, priority.current)
    }

    @Test
    fun removeCurrentFallsBackToLast() {
        val priority = PriorityList()
        priority.push(StatusWidget.NOTIFICATION)
        priority.push(StatusWidget.BATTERY)

        assertTrue(priority.remove(StatusWidget.BATTERY))
        assertEquals(StatusWidget.NOTIFICATION, priority.current)
    }

    @Test
    fun removeLastLeavesWeatherWhenEmpty() {
        val priority = PriorityList()
        priority.push(StatusWidget.BATTERY)

        assertTrue(priority.remove(StatusWidget.BATTERY))
        assertTrue(priority.isEmpty)
        assertEquals(StatusWidget.WEATHER, priority.current)
    }

    @Test
    fun removeMissingWidgetReturnsFalse() {
        val priority = PriorityList()
        priority.push(StatusWidget.BATTERY)
        assertFalse(priority.remove(StatusWidget.NOTIFICATION))
    }

    @Test
    fun rePushMovesWidgetToTop() {
        val priority = PriorityList()
        priority.push(StatusWidget.NOTIFICATION)
        priority.push(StatusWidget.BATTERY)

        priority.push(StatusWidget.NOTIFICATION)

        assertEquals(
            listOf(StatusWidget.BATTERY, StatusWidget.NOTIFICATION),
            priority.list
        )
        assertEquals(StatusWidget.NOTIFICATION, priority.current)
    }

    @Test
    fun removeAtOutOfBoundsReturnsNull() {
        val priority = PriorityList()
        assertNull(priority.removeAt(0))
    }
}
