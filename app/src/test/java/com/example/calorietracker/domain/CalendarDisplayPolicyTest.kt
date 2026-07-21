package com.example.calorietracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarDisplayPolicyTest {
    @Test
    fun missingPreferenceShowsEveryMetric() {
        assertEquals(CalendarMetricId.entries.toSet(), CalendarDisplayPolicy.normalizeVisibleMetrics(null))
    }

    @Test
    fun unknownOrEmptyPreferenceFallsBackToNetMetric() {
        assertEquals(
            setOf(CalendarMetricId.NET),
            CalendarDisplayPolicy.normalizeVisibleMetrics(setOf("unknown"))
        )
        assertEquals(
            setOf(CalendarMetricId.NET),
            CalendarDisplayPolicy.normalizeVisibleMetrics(emptySet())
        )
    }

    @Test
    fun explicitSubsetIsPreserved() {
        assertEquals(
            setOf(CalendarMetricId.WATER, CalendarMetricId.WEIGHT),
            CalendarDisplayPolicy.normalizeVisibleMetrics(setOf("water", "weight"))
        )
    }

    @Test
    fun compactLabelsAreUsedOnlyWhenTextWouldOverflow() {
        val textWidths = listOf(30, 32, 58)

        assertFalse(
            CalendarDisplayPolicy.shouldUseCompactLabels(
                availableWidthPx = 168,
                labelWidthsPx = textWidths,
                horizontalPaddingPx = 8
            )
        )
        assertTrue(
            CalendarDisplayPolicy.shouldUseCompactLabels(
                availableWidthPx = 167,
                labelWidthsPx = textWidths,
                horizontalPaddingPx = 8
            )
        )
    }
}
