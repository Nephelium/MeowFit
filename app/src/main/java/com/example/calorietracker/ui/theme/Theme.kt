package com.example.calorietracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenLight.copy(alpha = 0.3f),
    onPrimaryContainer = BrandGreenDark,
    
    secondary = BrandOrange,
    onSecondary = Color.White,
    secondaryContainer = BrandOrangeLight.copy(alpha = 0.3f),
    onSecondaryContainer = BrandOrangeDark,
    
    tertiary = BrandBlue,
    
    background = LightBackground,
    onBackground = LightTextPrimary,
    
    surface = LightSurface,
    onSurface = LightTextPrimary,
    
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    
    outline = LightDivider,
    error = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreenLight, // Lighter color for dark mode
    onPrimary = BrandGreenDark,
    primaryContainer = BrandGreen.copy(alpha = 0.2f),
    onPrimaryContainer = BrandGreenLight,
    
    secondary = BrandOrangeLight,
    onSecondary = BrandOrangeDark,
    secondaryContainer = BrandOrange.copy(alpha = 0.2f),
    onSecondaryContainer = BrandOrangeLight,
    
    tertiary = BrandBlue,
    
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    
    outline = DarkDivider,
    error = ErrorRedLight
)

@Composable
fun CalorieTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // We stick to our brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // Blend status bar with background
            // Use light status bar icons if not dark theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
