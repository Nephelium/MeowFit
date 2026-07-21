package com.example.calorietracker.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calorietracker.ui.theme.PixelInk
import com.example.calorietracker.ui.theme.PixelMint
import com.example.calorietracker.ui.theme.PixelPink
import com.example.calorietracker.ui.theme.PixelYellow

enum class PixelCatMood {
    HAPPY,
    PROUD,
    HUNGRY,
    SLEEPY,
    ENCOURAGING
}

data class PixelCatStatus(
    val mood: PixelCatMood,
    val title: String,
    val message: String,
    val badge: String
)

fun resolvePixelCatStatus(
    intake: Int,
    burned: Int,
    target: Int,
    water: Int,
    sleepMinutes: Int
): PixelCatStatus {
    val net = intake - burned
    return when {
        sleepMinutes in 1 until 360 -> PixelCatStatus(
            PixelCatMood.SLEEPY,
            "猫猫有点困",
            "昨晚睡得偏少，今天给自己留一点慢下来的时间吧。",
            "Z Z Z"
        )
        intake == 0 -> PixelCatStatus(
            PixelCatMood.HUNGRY,
            "等你开饭喵~",
            "还没有饮食记录。把营养标签抄进来，我会帮你算好当前份量！",
            "MEAL?"
        )
        target > 0 && net <= target && water >= 1500 -> PixelCatStatus(
            PixelCatMood.PROUD,
            "今天很稳！",
            "净热量在目标内，补水也很认真。猫猫已经把小红花别在你身上啦。",
            "NICE!"
        )
        target > 0 && net > target -> PixelCatStatus(
            PixelCatMood.ENCOURAGING,
            "没关系，继续走",
            "今天比目标多 ${net - target} kcal。不需要惩罚自己，下一餐正常吃、多走一会儿就好。",
            "GO!"
        )
        else -> PixelCatStatus(
            PixelCatMood.HAPPY,
            "记录中喵~",
            "今天的小进度正在积攒。再喝一杯水，我们继续慢慢完成。",
            "KEEP"
        )
    }
}

@Composable
fun PixelCatStatusCard(
    status: PixelCatStatus,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    PixelCard(
        modifier = modifier,
        containerColor = containerColor,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelCat(status.mood, Modifier.size(90.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        status.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    PixelBadge(status.badge, color = moodColor(status.mood))
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    status.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f)
                )
            }
        }
    }
}

@Composable
fun PixelCat(mood: PixelCatMood, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pixel-cat")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), repeatMode = RepeatMode.Restart),
        label = "cat-phase"
    )
    val blink = phase > 0.91f
    val tailUp = phase > 0.5f
    val fur = when (mood) {
        PixelCatMood.PROUD -> PixelYellow
        PixelCatMood.SLEEPY -> Color(0xFFB8A7E8)
        PixelCatMood.HUNGRY -> Color(0xFFFFB16D)
        PixelCatMood.ENCOURAGING -> PixelMint
        PixelCatMood.HAPPY -> PixelPink
    }
    val cheek = Color(0xFFFF7FA8)
    val white = Color(0xFFFFF8EC)

    Canvas(modifier) {
        val grid = 18f
        val cell = minOf(size.width, size.height) / grid
        val ox = (size.width - cell * grid) / 2f
        val oy = (size.height - cell * grid) / 2f
        fun pixel(x: Int, y: Int, color: Color, w: Int = 1, h: Int = 1) {
            drawRect(color, Offset(ox + x * cell, oy + y * cell), Size(w * cell, h * cell))
        }

        // Tail, body, ears and head use integer cells to keep hard pixel edges.
        pixel(if (tailUp) 14 else 15, if (tailUp) 9 else 12, PixelInk, 2, 5)
        pixel(if (tailUp) 15 else 14, if (tailUp) 8 else 13, fur, 2, 4)
        pixel(4, 10, PixelInk, 10, 6)
        pixel(5, 9, fur, 8, 6)
        pixel(4, 2, PixelInk, 4, 5)
        pixel(10, 2, PixelInk, 4, 5)
        pixel(5, 3, fur, 3, 4)
        pixel(10, 3, fur, 3, 4)
        pixel(4, 5, PixelInk, 10, 7)
        pixel(5, 5, fur, 8, 6)
        pixel(6, 6, white, 6, 4)

        if (blink || mood == PixelCatMood.SLEEPY) {
            pixel(6, 7, PixelInk, 2, 1)
            pixel(10, 7, PixelInk, 2, 1)
        } else {
            pixel(7, 7, PixelInk)
            pixel(10, 7, PixelInk)
        }
        pixel(5, 9, cheek)
        pixel(12, 9, cheek)
        pixel(8, 8, PixelInk, 2, 1)
        when (mood) {
            PixelCatMood.HUNGRY -> pixel(8, 10, PixelInk, 2, 2)
            PixelCatMood.PROUD, PixelCatMood.HAPPY -> {
                pixel(7, 9, PixelInk)
                pixel(10, 9, PixelInk)
            }
            else -> pixel(8, 10, PixelInk, 2, 1)
        }
        pixel(5, 15, PixelInk, 3, 1)
        pixel(10, 15, PixelInk, 3, 1)
    }
}

private fun moodColor(mood: PixelCatMood): Color = when (mood) {
    PixelCatMood.PROUD -> PixelYellow
    PixelCatMood.SLEEPY -> Color(0xFFB8A7E8)
    PixelCatMood.HUNGRY -> Color(0xFFFFB16D)
    PixelCatMood.ENCOURAGING -> PixelMint
    PixelCatMood.HAPPY -> PixelPink
}
