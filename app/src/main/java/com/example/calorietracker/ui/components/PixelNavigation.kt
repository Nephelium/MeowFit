package com.example.calorietracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

enum class PixelNavDestination {
    TODAY,
    STATS,
    CALENDAR,
    ANALYSIS,
    SETTINGS
}

@Composable
fun PixelNavIcon(
    destination: PixelNavDestination,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val grid = 18
        val cell = minOf(size.width, size.height) / grid
        val ox = (size.width - grid * cell) / 2f
        val oy = (size.height - grid * cell) / 2f
        fun px(x: Int, y: Int, w: Int = 1, h: Int = 1) {
            drawRect(
                color = color,
                topLeft = Offset(ox + x * cell, oy + y * cell),
                size = Size(w * cell, h * cell)
            )
        }

        when (destination) {
            PixelNavDestination.TODAY -> {
                px(8, 2, 2, 2)
                px(6, 4, 6, 2)
                px(4, 6, 10, 2)
                px(3, 8, 12, 2)
                px(5, 10, 8, 6)
                px(8, 12, 3, 4)
            }
            PixelNavDestination.STATS -> {
                px(2, 5, 3, 8)
                px(5, 7, 2, 4)
                px(7, 8, 4, 2)
                px(11, 7, 2, 4)
                px(13, 5, 3, 8)
                px(1, 7, 2, 4)
                px(16, 7, 1, 4)
            }
            PixelNavDestination.CALENDAR -> {
                px(4, 3, 2, 3)
                px(12, 3, 2, 3)
                px(3, 5, 12, 2)
                px(3, 7, 2, 8)
                px(13, 7, 2, 8)
                px(5, 13, 8, 2)
                px(6, 9, 2, 2)
                px(10, 9, 2, 2)
            }
            PixelNavDestination.ANALYSIS -> {
                px(3, 13, 12, 2)
                px(3, 10, 2, 3)
                px(7, 8, 2, 5)
                px(11, 5, 2, 8)
                px(14, 2, 2, 2)
                px(13, 3, 4, 1)
                px(15, 1, 1, 5)
                px(5, 8, 2, 2)
                px(8, 6, 3, 2)
                px(12, 4, 2, 2)
            }
            PixelNavDestination.SETTINGS -> {
                px(7, 2, 4, 3)
                px(7, 13, 4, 3)
                px(2, 7, 3, 4)
                px(13, 7, 3, 4)
                px(4, 4, 3, 3)
                px(11, 4, 3, 3)
                px(4, 11, 3, 3)
                px(11, 11, 3, 3)
                px(6, 6, 6, 2)
                px(6, 10, 6, 2)
                px(6, 8, 2, 2)
                px(10, 8, 2, 2)
            }
        }
    }
}
