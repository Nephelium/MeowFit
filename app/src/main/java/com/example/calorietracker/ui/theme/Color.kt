package com.example.calorietracker.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Colors
val BrandGreen = Color(0xFF4CAF50)
val BrandGreenLight = Color(0xFF80E27E)
val BrandGreenDark = Color(0xFF087F23)

val BrandOrange = Color(0xFFFF9800)
val BrandOrangeLight = Color(0xFFFFC947)
val BrandOrangeDark = Color(0xFFC66900)

val BrandBlue = Color(0xFF2196F3)

// Light Theme Colors
val LightBackground = Color(0xFFF2F5F8) // Cool Greyish White
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F2F5)
val LightTextPrimary = Color(0xFF1A1C1E)
val LightTextSecondary = Color(0xFF70797F)
val LightDivider = Color(0xFFE0E0E0)

// Dark Theme Colors
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)
val DarkTextPrimary = Color(0xFFE1E2E6)
val DarkTextSecondary = Color(0xFFA0A0A0)
val DarkDivider = Color(0xFF333333)

// Heatmap Colors (Light)
val HeatmapLight0 = Color(0xFFEBEDF0)
val HeatmapLight1 = Color(0xFF9BE9A8)
val HeatmapLight2 = Color(0xFF40C463)
val HeatmapLight3 = Color(0xFF30A14E)
val HeatmapLight4 = Color(0xFF216E39)

// Heatmap Colors (Dark)
val HeatmapDark0 = Color(0xFF2D333B) // Dark grey for empty
val HeatmapDark1 = Color(0xFF0E4429)
val HeatmapDark2 = Color(0xFF006D32)
val HeatmapDark3 = Color(0xFF26A641)
val HeatmapDark4 = Color(0xFF39D353) // Bright green for max

// Functional Colors
val ErrorRed = Color(0xFFBA1A1A)
val ErrorRedLight = Color(0xFFFFB4AB)

// Semantic colors: single source of truth for status/functional colors across screens.
// Light-theme values; use deficitColor()/surplusColor() helpers in ThemeUtils for theme-aware picks.
val DeficitGreen = Color(0xFF2E7D32)      // 热量缺口/达标（浅色主题）
val DeficitGreenDark = Color(0xFF81C784)  // 热量缺口/达标（深色主题）
val SurplusRed = Color(0xFFD32F2F)        // 超标（浅色主题；深色主题用 colorScheme.error）
val SurplusRedSoft = Color(0xFFE57373)    // 超标（热力图/大块填充等柔和场景）
val OfficialGreen = Color(0xFF4CAF50)     // “官方数据”等认证标记
val InfoBlue = Color(0xFF2196F3)          // 信息/计时等中性强调
val WarnAmber = Color(0xFFF9A825)         // 警告

// Macronutrient colors (蛋白/碳水/脂肪)
val MacroProtein = Color(0xFF69F0AE)
val MacroCarb = Color(0xFF40C4FF)
val MacroFat = Color(0xFFFF8A80)

// Pixel-pet palette: warm candy colors with a dark ink outline.
val PixelInk = Color(0xFF3B2B3F)
val PixelCream = Color(0xFFFFF7E8)
val PixelPaper = Color(0xFFFFFCF5)
val PixelPink = Color(0xFFFF7FA8)
val PixelPinkSoft = Color(0xFFFFD5E2)
val PixelMint = Color(0xFF58C9A3)
val PixelMintSoft = Color(0xFFCFF4E8)
val PixelLavender = Color(0xFF9B83E6)
val PixelSky = Color(0xFF6DBCEB)
val PixelYellow = Color(0xFFFFCD64)
val PixelPlum = Color(0xFF241B2C)
val PixelPlumSurface = Color(0xFF35283F)
val PixelDarkText = Color(0xFFFFF4FA)
