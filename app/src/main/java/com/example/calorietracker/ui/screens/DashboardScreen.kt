package com.example.calorietracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.calorietracker.data.CalorieItemEntity
import com.example.calorietracker.data.DailyRecordEntity
import com.example.calorietracker.data.UserProfileEntity
import com.example.calorietracker.util.CalorieUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userProfile: UserProfileEntity?,
    dailyRecord: DailyRecordEntity?,
    items: List<CalorieItemEntity>,
    selectedDate: String,
    onDateChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onDeleteItem: (CalorieItemEntity) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("减肥日历") },
                actions = {
                    // Date Navigation
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        try {
                            cal.time = sdf.parse(selectedDate)!!
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                            onDateChange(sdf.format(cal.time))
                        } catch (e: Exception) {}
                    }) {
                        Icon(Icons.Default.ArrowBack, "Previous Day")
                    }
                    Text(
                        text = CalorieUtils.formatDate(selectedDate),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    IconButton(onClick = {
                         val cal = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        try {
                            cal.time = sdf.parse(selectedDate)!!
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            onDateChange(sdf.format(cal.time))
                        } catch (e: Exception) {}
                    }) {
                        Icon(Icons.Default.ArrowForward, "Next Day")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, "Add Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val target = userProfile?.dailyCalorieTarget ?: 2000
                    val intake = dailyRecord?.totalIntake ?: 0
                    val burned = dailyRecord?.totalBurned ?: 0
                    val net = intake - burned
                    val remaining = target - net

                    Text("今日目标: $target kcal", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("摄入", style = MaterialTheme.typography.bodySmall)
                            Text("$intake", style = MaterialTheme.typography.bodyLarge)
                        }
                        Column {
                            Text("运动", style = MaterialTheme.typography.bodySmall)
                            Text("$burned", style = MaterialTheme.typography.bodyLarge)
                        }
                        Column {
                            Text("剩余", style = MaterialTheme.typography.bodySmall)
                            Text("$remaining", style = MaterialTheme.typography.bodyLarge, color = if(remaining < 0) Color.Red else Color.Green)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = (net.toFloat() / target.toFloat()).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("今日记录", style = MaterialTheme.typography.titleMedium)
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(items) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text("${item.time} - ${if(item.type == "food") "食物" else "运动"}") },
                        trailingContent = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${if (item.type == "food") "+" else "-"}${item.calories} kcal")
                                IconButton(onClick = { onDeleteItem(item) }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color.Gray)
                                }
                            }
                        }
                    )
                    Divider()
                }
                if (items.isEmpty()) {
                    item {
                        Text("暂无记录", modifier = Modifier.padding(16.dp), color = Color.Gray)
                    }
                }
            }
        }
    }
}
