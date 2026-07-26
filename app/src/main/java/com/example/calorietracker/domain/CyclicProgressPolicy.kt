package com.example.calorietracker.domain

import kotlin.math.floor

data class CyclicProgressState(
    val completedCycles: Int,
    val completedCycleColorIndex: Int?,
    val activeCycleColorIndex: Int,
    val activeFraction: Float
)

object CyclicProgressPolicy {
    fun resolve(rawProgress: Float, paletteSize: Int): CyclicProgressState {
        val safePaletteSize = paletteSize.coerceAtLeast(1)
        val safeProgress = rawProgress
            .takeIf { it.isFinite() && it > 0f }
            ?.coerceAtMost(Int.MAX_VALUE.toFloat())
            ?: 0f
        val completedCycles = floor(safeProgress.toDouble()).toInt()
        val activeFraction = (safeProgress - completedCycles)
            .coerceIn(0f, 1f)

        return CyclicProgressState(
            completedCycles = completedCycles,
            completedCycleColorIndex = if (completedCycles > 0) {
                (completedCycles - 1) % safePaletteSize
            } else {
                null
            },
            activeCycleColorIndex = completedCycles % safePaletteSize,
            activeFraction = activeFraction
        )
    }
}
