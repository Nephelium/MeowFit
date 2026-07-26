package com.example.calorietracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CyclicProgressPolicyTest {
    @Test
    fun zeroProgressStartsOnFirstColorWithEmptyTrack() {
        val state = CyclicProgressPolicy.resolve(rawProgress = 0f, paletteSize = 4)

        assertEquals(0, state.completedCycles)
        assertNull(state.completedCycleColorIndex)
        assertEquals(0, state.activeCycleColorIndex)
        assertEquals(0f, state.activeFraction, 0.0001f)
    }

    @Test
    fun partialFirstCycleUsesFirstColor() {
        val state = CyclicProgressPolicy.resolve(rawProgress = 0.75f, paletteSize = 4)

        assertEquals(0, state.completedCycles)
        assertNull(state.completedCycleColorIndex)
        assertEquals(0, state.activeCycleColorIndex)
        assertEquals(0.75f, state.activeFraction, 0.0001f)
    }

    @Test
    fun exactCycleLeavesCompletedColorAcrossTheWholeTrack() {
        val state = CyclicProgressPolicy.resolve(rawProgress = 1f, paletteSize = 4)

        assertEquals(1, state.completedCycles)
        assertEquals(0, state.completedCycleColorIndex)
        assertEquals(1, state.activeCycleColorIndex)
        assertEquals(0f, state.activeFraction, 0.0001f)
    }

    @Test
    fun overflowStartsNextColorFromTheBeginning() {
        val state = CyclicProgressPolicy.resolve(rawProgress = 1.3f, paletteSize = 4)

        assertEquals(1, state.completedCycles)
        assertEquals(0, state.completedCycleColorIndex)
        assertEquals(1, state.activeCycleColorIndex)
        assertEquals(0.3f, state.activeFraction, 0.0001f)
    }

    @Test
    fun laterCyclesKeepWrappingThroughPalette() {
        val exactSecondCycle = CyclicProgressPolicy.resolve(rawProgress = 2f, paletteSize = 4)
        val partialThirdCycle = CyclicProgressPolicy.resolve(rawProgress = 2.3f, paletteSize = 4)
        val wrappedFifthCycle = CyclicProgressPolicy.resolve(rawProgress = 4.25f, paletteSize = 4)

        assertEquals(1, exactSecondCycle.completedCycleColorIndex)
        assertEquals(2, exactSecondCycle.activeCycleColorIndex)
        assertEquals(0f, exactSecondCycle.activeFraction, 0.0001f)

        assertEquals(1, partialThirdCycle.completedCycleColorIndex)
        assertEquals(2, partialThirdCycle.activeCycleColorIndex)
        assertEquals(0.3f, partialThirdCycle.activeFraction, 0.0001f)

        assertEquals(3, wrappedFifthCycle.completedCycleColorIndex)
        assertEquals(0, wrappedFifthCycle.activeCycleColorIndex)
        assertEquals(0.25f, wrappedFifthCycle.activeFraction, 0.0001f)
    }

    @Test
    fun invalidProgressAndPaletteAreNormalizedSafely() {
        listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { progress ->
            val state = CyclicProgressPolicy.resolve(rawProgress = progress, paletteSize = 0)

            assertEquals(0, state.completedCycles)
            assertNull(state.completedCycleColorIndex)
            assertEquals(0, state.activeCycleColorIndex)
            assertEquals(0f, state.activeFraction, 0.0001f)
        }
    }
}
