package com.example.calorietracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.example.calorietracker.domain.CyclicProgressPolicy
import kotlin.math.roundToInt

@Composable
fun CyclicProgressBar(
    progress: Float,
    colors: List<Color>,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = colors.ifEmpty { listOf(Color.Transparent) }
    val state = remember(progress, palette.size) {
        CyclicProgressPolicy.resolve(progress, palette.size)
    }
    val percentage = remember(progress) {
        progress
            .takeIf { it.isFinite() && it > 0f }
            ?.times(100f)
            ?.roundToInt()
            ?: 0
    }

    Canvas(
        modifier = modifier
            .semantics { stateDescription = "已完成 $percentage%" }
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        state.completedCycleColorIndex?.let { colorIndex ->
            drawRect(color = palette[colorIndex])
        }

        if (state.activeFraction > 0f) {
            drawRect(
                color = palette[state.activeCycleColorIndex],
                topLeft = Offset.Zero,
                size = Size(
                    width = size.width * state.activeFraction,
                    height = size.height
                )
            )
        }
    }
}
