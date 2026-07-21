package com.example.calorietracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.floor

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
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val grid = 16
        val cell = floor(minOf(size.width, size.height) / grid).coerceAtLeast(1f)
        val ox = (size.width - grid * cell) / 2f
        val oy = (size.height - grid * cell) / 2f
        val highlight = Color.White.copy(alpha = if (selected) 0.42f else 0f)
        fun px(x: Int, y: Int, w: Int = 1, h: Int = 1, fill: Color = color) {
            drawRect(
                color = fill,
                topLeft = Offset(ox + x * cell, oy + y * cell),
                size = Size(w * cell, h * cell)
            )
        }

        when (destination) {
            PixelNavDestination.TODAY -> {
                // 猫爪贴纸
                px(2, 4, 2, 3)
                px(5, 1, 3, 3)
                px(9, 1, 3, 3)
                px(13, 4, 2, 3)
                if (selected) {
                    px(5, 7, 6, 2)
                    px(3, 9, 10, 3)
                    px(5, 12, 6, 2)
                    px(6, 8, 2, 2, highlight)
                } else {
                    px(5, 7, 6, 1)
                    px(3, 8, 2, 4)
                    px(11, 8, 2, 4)
                    px(5, 12, 6, 2)
                }
            }
            PixelNavDestination.STATS -> {
                // 哑铃贴纸
                px(1, 5, 2, 6)
                px(3, 3, 3, 10)
                px(6, 7, 4, 2)
                px(10, 3, 3, 10)
                px(13, 5, 2, 6)
                if (selected) px(4, 4, 1, 3, highlight)
            }
            PixelNavDestination.CALENDAR -> {
                // 台历贴纸
                px(3, 2, 2, 4)
                px(11, 2, 2, 4)
                px(2, 4, 12, 2)
                px(2, 6, 2, 8)
                px(12, 6, 2, 8)
                px(4, 12, 8, 2)
                if (selected) {
                    px(4, 7, 2, 2)
                    px(7, 7, 2, 2)
                    px(10, 7, 2, 2)
                    px(4, 10, 2, 2)
                    px(7, 10, 2, 2)
                    px(10, 10, 2, 2)
                    px(3, 4, 3, 1, highlight)
                } else {
                    px(5, 8, 2, 2)
                    px(9, 8, 2, 2)
                }
            }
            PixelNavDestination.ANALYSIS -> {
                // 柱状图与闪光贴纸
                px(2, 13, 12, 2)
                px(3, 9, 3, 4)
                px(7, 6, 3, 7)
                px(11, 3, 3, 10)
                px(14, 1, 1, 4)
                px(12, 3, 4, 1)
                if (selected) {
                    px(4, 9, 1, 2, highlight)
                    px(8, 6, 1, 3, highlight)
                    px(12, 3, 1, 3, highlight)
                }
            }
            PixelNavDestination.SETTINGS -> {
                // 三组滑杆贴纸，比齿轮更清爽
                px(2, 3, 12, 2)
                px(2, 7, 12, 2)
                px(2, 11, 12, 2)
                px(5, 1, 3, 6)
                px(10, 5, 3, 6)
                px(4, 9, 3, 6)
                if (selected) {
                    px(6, 2, 1, 2, highlight)
                    px(11, 6, 1, 2, highlight)
                    px(5, 10, 1, 2, highlight)
                }
            }
        }
    }
}
