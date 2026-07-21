package com.example.calorietracker.domain

enum class CalendarMetricId(val storageKey: String) {
    SLEEP("sleep"),
    WATER("water"),
    NET("net"),
    INTAKE("intake"),
    BURNED("burned"),
    WEIGHT("weight"),
    MEDS("meds");

    companion object {
        fun fromStorageKey(value: String): CalendarMetricId? =
            entries.firstOrNull { it.storageKey == value }
    }
}

object CalendarDisplayPolicy {
    fun normalizeVisibleMetrics(storedKeys: Set<String>?): Set<CalendarMetricId> {
        if (storedKeys == null) return CalendarMetricId.entries.toSet()

        val visible = storedKeys.mapNotNull(CalendarMetricId::fromStorageKey).toSet()
        return visible.ifEmpty { setOf(CalendarMetricId.NET) }
    }

    fun shouldUseCompactLabels(
        availableWidthPx: Int,
        labelWidthsPx: List<Int>,
        horizontalPaddingPx: Int
    ): Boolean {
        val requiredWidth = labelWidthsPx.sumOf { width ->
            width + horizontalPaddingPx * 2
        }
        return requiredWidth > availableWidthPx
    }
}
