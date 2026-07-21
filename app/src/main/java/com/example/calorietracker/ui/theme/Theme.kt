package com.example.calorietracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PixelPink,
    onPrimary = PixelInk, // dark ink on candy pink for readable contrast
    primaryContainer = PixelPinkSoft,
    onPrimaryContainer = PixelInk,
    
    secondary = PixelMint,
    onSecondary = PixelInk,
    secondaryContainer = PixelMintSoft,
    onSecondaryContainer = PixelInk,
    
    tertiary = PixelLavender,
    
    background = PixelCream,
    onBackground = PixelInk,
    
    surface = PixelPaper,
    onSurface = PixelInk,
    
    surfaceVariant = PixelPinkSoft, // opaque: alpha variants drift over different backgrounds
    onSurfaceVariant = PixelInk.copy(alpha = 0.78f),
    
    outline = PixelInk.copy(alpha = 0.52f),
    error = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF93B6),
    onPrimary = PixelPlum,
    primaryContainer = Color(0xFF61374D),
    onPrimaryContainer = PixelPinkSoft,
    
    secondary = Color(0xFF74E2BE),
    onSecondary = PixelPlum,
    secondaryContainer = Color(0xFF244E46),
    onSecondaryContainer = PixelMintSoft,
    
    tertiary = Color(0xFFBCA9FF),
    
    background = PixelPlum,
    onBackground = PixelDarkText,
    
    surface = PixelPlumSurface,
    onSurface = PixelDarkText,
    
    surfaceVariant = Color(0xFF493751),
    onSurfaceVariant = PixelDarkText.copy(alpha = 0.78f),
    
    outline = PixelPinkSoft.copy(alpha = 0.45f),
    error = ErrorRedLight
)

private val PixelShapes = Shapes(
    extraSmall = CutCornerShape(2.dp),
    small = CutCornerShape(4.dp),
    medium = CutCornerShape(6.dp),
    large = CutCornerShape(10.dp),
    extraLarge = CutCornerShape(14.dp)
)

@Composable
fun CalorieTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // We stick to our brand colors
    fontMode: AppFontMode = AppFontMode.MEOW_FIT,
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
            window.statusBarColor = android.graphics.Color.TRANSPARENT // Transparent to extend background into status bar
            // Use light status bar icons if not dark theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = meowFitTypography(fontMode),
        shapes = PixelShapes,
        content = content
    )
}
