package com.example.calorietracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.calorietracker.ui.screens.calculatePerceivedLuminance

@Composable
fun onCardColor(cardColor: Color, isDarkTheme: Boolean): Color {
    return remember(cardColor, isDarkTheme) {
        if (isDarkTheme) Color.White
        else if (calculatePerceivedLuminance(cardColor) > 0.5f) Color(0xFF1E1E1E)
        else Color(0xFFF4F4F4)
    }
}
