package com.example.calorietracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.calorietracker.ui.screens.calculatePerceivedLuminance

/** 热量缺口/达标绿：深色主题下用更亮的绿保证可读性。 */
@Composable
fun deficitColor(): Color = if (isSystemInDarkTheme()) DeficitGreenDark else DeficitGreen

/** 超标红：优先使用 colorScheme.error（深浅色主题均已适配）。 */
@Composable
fun surplusColor(): Color = MaterialTheme.colorScheme.error

@Composable
fun onCardColor(cardColor: Color, isDarkTheme: Boolean): Color {
    return remember(cardColor, isDarkTheme) {
        if (isDarkTheme) Color.White
        else if (calculatePerceivedLuminance(cardColor) > 0.5f) Color(0xFF1E1E1E)
        else Color(0xFFF4F4F4)
    }
}
