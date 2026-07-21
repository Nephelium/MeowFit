package com.example.calorietracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.calorietracker.ui.theme.PixelInk

@Composable
fun PixelBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    val dotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    Box(modifier = modifier.background(background)) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 24.dp.toPx()
            val pixel = 2.dp.toPx()
            var y = 12.dp.toPx()
            var row = 0
            while (y < size.height) {
                var x = if (row % 2 == 0) 12.dp.toPx() else 24.dp.toPx()
                while (x < size.width) {
                    drawRect(dotColor, Offset(x, y), androidx.compose.ui.geometry.Size(pixel, pixel))
                    x += step
                }
                row += 1
                y += step
            }
        }
        content()
    }
}

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    contentPadding: PaddingValues = PaddingValues(MeowFitUi.cardPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .padding(end = 2.dp, bottom = 2.dp)
            .background(PixelInk.copy(alpha = 0.22f), CutCornerShape(8.dp))
    ) {
        Surface(
            modifier = Modifier.padding(end = 2.dp, bottom = 2.dp),
            color = containerColor,
            shape = CutCornerShape(8.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        modifier = modifier.border(1.dp, PixelInk.copy(alpha = 0.55f), CutCornerShape(4.dp)),
        enabled = enabled,
        shape = CutCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = PixelInk)
    ) {
        Text(text, fontWeight = FontWeight.Black)
    }
}

@Composable
fun PixelBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = PixelInk
) {
    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = CutCornerShape(3.dp),
        border = BorderStroke(1.dp, PixelInk.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

fun Modifier.pixelBorder(
    color: Color = PixelInk.copy(alpha = 0.55f),
    width: Dp = 2.dp
): Modifier = border(width, color, CutCornerShape(6.dp))
