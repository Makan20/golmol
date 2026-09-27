package com.ordibehesht.finance.ui.screens

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import com.ordibehesht.finance.ui.theme.CardWhite
import com.ordibehesht.finance.ui.theme.TextPrimary
import com.ordibehesht.finance.ui.theme.TextSecondary
import com.ordibehesht.finance.ui.theme.TextTertiary
import com.ordibehesht.finance.ui.theme.Vazirmatn
import com.ordibehesht.finance.ui.utils.PersianDateUtils
import kotlinx.coroutines.launch

// نکته (رفع code-smell): قبلاً اینجا یک نسخه‌ی جداگانه از الگوریتم کبیسه‌ی جلالی
// (isPersianLeapYear/daysInPersianMonth) تکرار شده بود که دقیقاً همان منطق
// PersianDateUtils.isLeapJalaliYear/daysInJalaliMonth را داشت. چون این دو نسخه از هم
// مستقل بودند، اگر در آینده الگوریتم کبیسه یک‌جا اصلاح می‌شد (مثلاً fix باگ آخرین روز
// اسفند در PersianDateUtils.gregorianToJalali)، این نسخه‌ی محلی خودکار sync نمی‌شد.
// حالا مستقیماً از تابع عمومی PersianDateUtils.daysInJalaliMonth استفاده می‌شود.

@Composable
fun PersianDatePickerDialog(
    initialDate: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    // وقتی از داخل یک دیالوگ دیگر باز می‌شود false بگذارید تا پرده‌ی تیره دوبل نشود (بدون پرش)
    dimBehind: Boolean = true
) {
    val parts = initialDate.split("/")
    val todayParts = PersianDateUtils.getCurrentPersianDate().split("/")
    val currentYear = todayParts[0].toInt()
    val initYear = parts.getOrNull(0)?.toIntOrNull() ?: currentYear
    val initMonth = parts.getOrNull(1)?.toIntOrNull() ?: todayParts[1].toInt()
    val initDay = parts.getOrNull(2)?.toIntOrNull() ?: todayParts[2].toInt()

    // باگ رفع‌شده: قبلاً این بازه به‌صورت ثابت (1404..1410) نوشته شده بود. برای هر تاریخِ
    // اولیه‌ای که سالش بیرون از آن بازه بود (مثلاً یک تراکنش قدیمی‌تر از ۱۴۰۴)،
    // WheelColumn.initialIndex با items.indexOf(selected)=-1 و coerceAtLeast(0) به‌طور
    // خاموش روی اولین سالِ لیست (نه سال واقعی) می‌نشست، و همان لحظه‌ی باز شدن، بدون هیچ
    // لمسی از کاربر، LaunchedEffect(isScrollInProgress) در WheelColumn سال انتخاب‌شده را
    // واقعاً به همان سالِ اشتباه تغییر می‌داد. حالا بازه هم نسبت به سال جاری پویاست (تا سال‌ها
    // بعد هم کار می‌کند) و هم همیشه شامل initYear است (هر چقدر قدیمی یا جدید باشد)، تا سال
    // واقعیِ تاریخِ ورودی همیشه در چرخ لیست وجود داشته باشد.
    val years = remember(initYear, currentYear) {
        val lowerBound = minOf(initYear, currentYear - 5)
        val upperBound = maxOf(initYear, currentYear + 5)
        (lowerBound..upperBound).toList()
    }

    var selectedYear by remember { mutableStateOf(initYear) }
    var selectedMonth by remember { mutableStateOf(initMonth) }
    var selectedDay by remember { mutableStateOf(initDay) }

    val maxDay = PersianDateUtils.daysInJalaliMonth(selectedYear, selectedMonth)
    LaunchedEffect(maxDay) {
        if (selectedDay > maxDay) selectedDay = maxDay
    }

    Dialog(onDismissRequest = onDismiss) {
        if (!dimBehind) {
            val view = LocalView.current
            SideEffect {
                (view.parent as? DialogWindowProvider)?.window
                    ?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "انتخاب تاریخ",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val itemHeight = 40.dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight * 4)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .height(itemHeight)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                        )

                        Row(modifier = Modifier.fillMaxSize()) {
                            WheelColumn(
                                items = (1..maxDay).toList(),
                                selected = selectedDay,
                                accentColor = accentColor,
                                itemHeight = itemHeight,
                                modifier = Modifier.weight(1f),
                                label = { PersianDateUtils.toPersianDigits(it.toString()) },
                                onSelected = { selectedDay = it }
                            )
                            WheelColumn(
                                items = (1..12).toList(),
                                selected = selectedMonth,
                                accentColor = accentColor,
                                itemHeight = itemHeight,
                                modifier = Modifier.weight(1.3f),
                                label = { PersianDateUtils.persianMonthNames[it - 1] },
                                onSelected = { selectedMonth = it }
                            )
                            WheelColumn(
                                items = years,
                                selected = selectedYear,
                                accentColor = accentColor,
                                itemHeight = itemHeight,
                                modifier = Modifier.weight(1f),
                                label = { PersianDateUtils.toPersianDigits(it.toString()) },
                                onSelected = { selectedYear = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "انصراف",
                                fontFamily = Vazirmatn,
                                color = TextSecondary
                            )
                        }

                        Button(
                            onClick = {
                                val dateStr = String.format(
                                    "%d/%02d/%02d", selectedYear, selectedMonth, selectedDay
                                )
                                onConfirm(dateStr)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "تایید",
                                fontFamily = Vazirmatn,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> WheelColumn(
    items: List<T>,
    selected: T,
    accentColor: Color,
    itemHeight: Dp,
    modifier: Modifier = Modifier,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    val initialIndex = items.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val index = listState.firstVisibleItemIndex
            items.getOrNull(index)?.let { onSelected(it) }
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
                    label(item),
                    fontFamily = Vazirmatn,
                    fontSize = if (isSelected) 17.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) accentColor else TextTertiary
                )
            }
        }
    }
}
