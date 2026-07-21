package com.example.calorietracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calorietracker.R

object MeowFitUi {
    val pagePadding = 12.dp
    val sectionGap = 10.dp
    val cardPadding = 11.dp
    // Keep fields compact without imposing a maximum height. System fonts and user font-scale
    // settings can have taller ascent/descent metrics, so the field must remain free to grow.
    val compactFieldMinHeight = 60.dp
}

fun Modifier.compactInput(): Modifier = heightIn(
    min = MeowFitUi.compactFieldMinHeight
)

@Composable
fun PixelJournalBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val shape = CutCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomEnd = 14.dp, bottomStart = 4.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp)
            .clip(shape)
            .border(1.dp, accentColor.copy(alpha = 0.55f), shape)
    ) {
        Image(
            painter = painterResource(R.drawable.pixel_cat_journal_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop, // 图片在 drawable-nodpi，避免密度重采样模糊
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.52f to Color.Transparent,
                        1f to Color(0xCC3B2B3F) // PixelInk 渐变遮罩，与像素主题一致
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
fun PixelSectionLabel(
    title: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = accentColor,
            shape = CutCornerShape(2.dp),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.18f))
        ) {
            Text(
                "◆",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Box(
            Modifier
                .weight(0.55f)
                .height(1.dp)
                .background(accentColor.copy(alpha = 0.35f))
        )
    }
}
