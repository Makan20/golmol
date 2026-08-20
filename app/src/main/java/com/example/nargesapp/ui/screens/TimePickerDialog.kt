package com.example.nargesapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nargesapp.ui.theme.*
import com.example.nargesapp.ui.utils.PersianDateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun TimePickerDialog(
    initialTime: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val now = Calendar.getInstance()
    val parts = initialTime.split(":")
    var selectedHour by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull() ?: now.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(parts.getOrNull(1)?.toIntOrNull() ?: now.get(Calendar.MINUTE)) }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "انتخاب ساعت",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val itemHeight = 40.dp
                    Box(modifier = Modifier.fillMaxWidth().height(itemHeight * 4)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .height(itemHeight)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                        )
                        Row(
    modifier = Modifier.fillMaxSize(),
    verticalAlignment = Alignment.CenterVertically
) {
    TimeWheelColumn(
        items = (0..59).toList(),
        selected = selectedMinute,
        accentColor = accentColor,
        itemHeight = itemHeight,
        modifier = Modifier.weight(1f),
        onSelected = { selectedMinute = it }
    )
    Text(
        ":",
        fontFamily = Vazirmatn,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = accentColor
    )
    TimeWheelColumn(
        items = (0..23).toList(),
        selected = selectedHour,
        accentColor = accentColor,
        itemHeight = itemHeight,
        modifier = Modifier.weight(1f),
        onSelected = { selectedHour = it }
    )
}
                    }

                    TextButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            selectedHour = c.get(Calendar.HOUR_OF_DAY)
                            selectedMinute = c.get(Calendar.MINUTE)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("الان", fontFamily = Vazirmatn, color = accentColor, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("انصراف", fontFamily = Vazirmatn, color = TextSecondary)
                        }
                        Button(
                            onClick = { onConfirm(String.format("%02d:%02d", selectedHour, selectedMinute)) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تایید", fontFamily = Vazirmatn, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeWheelColumn(
    items: List<Int>,
    selected: Int,
    accentColor: Color,
    itemHeight: Dp,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    val initialIndex = items.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            items.getOrNull(listState.firstVisibleItemIndex)?.let { onSelected(it) }
        }
    }
    // وقتی از بیرون (دکمه «الان») مقدار عوض شد، چرخه اسکرول شود
    LaunchedEffect(selected) {
        val idx = items.indexOf(selected)
        if (idx >= 0 && listState.firstVisibleItemIndex != idx) {
            listState.animateScrollToItem(idx)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier.fillMaxHeight(),
        contentPadding = PaddingValues(vertical = itemHeight * 1.5f)
    ) {
        itemsIndexed(items) { index, item ->
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable {
                        onSelected(item)
                        scope.launch { listState.animateScrollToItem(index) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    PersianDateUtils.toPersianDigits(String.format("%02d", item)),
                    fontFamily = Vazirmatn,
                    fontSize = if (isSelected) 17.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) accentColor else TextTertiary
                )
            }
        }
    }
}