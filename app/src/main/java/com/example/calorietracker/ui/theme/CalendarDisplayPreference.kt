package com.example.calorietracker.ui.theme

import android.content.Context
import com.example.calorietracker.domain.CalendarDisplayPolicy
import com.example.calorietracker.domain.CalendarMetricId

object CalendarDisplayPreferences {
    private const val PREFS_NAME = "ui_preferences"
    private const val KEY_VISIBLE_METRICS = "calendar_visible_metrics"

    fun read(context: Context): Set<CalendarMetricId> {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_VISIBLE_METRICS, null)
            ?.toSet()
        return CalendarDisplayPolicy.normalizeVisibleMetrics(stored)
    }

    fun write(context: Context, visibleMetrics: Set<CalendarMetricId>) {
        val normalized = CalendarDisplayPolicy.normalizeVisibleMetrics(
            visibleMetrics.mapTo(mutableSetOf()) { it.storageKey }
        )
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_VISIBLE_METRICS, normalized.mapTo(mutableSetOf()) { it.storageKey })
            .apply()
    }
}
