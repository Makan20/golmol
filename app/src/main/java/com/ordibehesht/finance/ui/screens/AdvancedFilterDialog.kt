package com.ordibehesht.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ordibehesht.finance.data.model.Account
import com.ordibehesht.finance.ui.theme.CardWhite
import com.ordibehesht.finance.ui.theme.LightGreen
import com.ordibehesht.finance.ui.theme.PrimaryGreen
import com.ordibehesht.finance.ui.theme.TextPrimary
import com.ordibehesht.finance.ui.theme.TextSecondary
import com.ordibehesht.finance.ui.theme.Vazirmatn

@Composable
fun AdvancedFilterDialog(
    accounts: List<Account>,
    initialFilter: AdvancedFilter,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onConfirm: (AdvancedFilter) -> Unit
) {
    var mode by remember { mutableStateOf(initialFilter.mode) }
    var selectedAccountIds by remember { mutableStateOf(initialFilter.selectedAccountIds) }
    var selectedCategories by remember { mutableStateOf(initialFilter.selectedCategories) }

    // دسته‌بندی‌های هزینه و درآمد با هم، بدون تکرار اسم مشترک (مثلاً «هدیه» که هم در
    // هزینه‌ها و هم در درآمدها وجود دارد) — چون در پایگاه‌داده فقط نام دسته ذخیره می‌شود،
    // نه نوعش، فیلتر بر اساس اسم عمل می‌کند و انتخاب یک اسم هم هزینه‌ها و هم درآمدهای
    // هم‌نام را شامل می‌شود
    val allCategoryNames = remember {
        (expenseCategories.map { it.name } + incomeCategories.map { it.name }).distinct()
    }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "فیلتر پیشرفته",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // سوییچ حالت: کارت یا دسته‌بندی — این دو حالت متقابلاً انحصاری‌اند،
                    // پس با تعویض حالت، انتخاب‌های حالت دیگر پاک می‌شوند تا هیچ فیلتر
                    // نامرئی و گیج‌کننده‌ای از حالت قبلی باقی نماند
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3F3F3))
                            .padding(4.dp)
                    ) {
                        AdvancedFilterModeButton(
                            text = "کارت",
                            selected = mode == AdvancedFilterMode.ACCOUNT,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                mode = AdvancedFilterMode.ACCOUNT
                                selectedCategories = emptySet()
                            }
                        )
                        AdvancedFilterModeButton(
                            text = "دسته‌بندی",
                            selected = mode == AdvancedFilterMode.CATEGORY,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                mode = AdvancedFilterMode.CATEGORY
                                selectedAccountIds = emptySet()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (mode) {
                        AdvancedFilterMode.ACCOUNT -> {
                            if (accounts.isEmpty()) {
                                Text(
                                    "هنوز کارتی ثبت نشده",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontFamily = Vazirmatn,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 260.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(accounts, key = { it.id }) { account ->
                                        AdvancedFilterCheckRow(
                                            label = account.name,
                                            checked = account.id in selectedAccountIds,
                                            onToggle = {
                                                selectedAccountIds = if (account.id in selectedAccountIds) {
                                                    selectedAccountIds - account.id
                                                } else {
                                                    selectedAccountIds + account.id
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        AdvancedFilterMode.CATEGORY -> {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 260.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allCategoryNames.forEach { categoryName ->
                                    AdvancedFilterCheckRow(
                                        label = categoryName,
                                        checked = categoryName in selectedCategories,
                                        onToggle = {
                                            selectedCategories = if (categoryName in selectedCategories) {
                                                selectedCategories - categoryName
                                            } else {
                                                selectedCategories + categoryName
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onClear,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "پاک کردن",
                                color = TextSecondary,
                                fontFamily = Vazirmatn
                            )
                        }

                        Button(
                            onClick = {
                                onConfirm(
                                    AdvancedFilter(
                                        mode = mode,
                                        selectedAccountIds = selectedAccountIds,
                                        selectedCategories = selectedCategories
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text(
                                text = "تایید",
                                color = Color.White,
                                fontFamily = Vazirmatn,
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
private fun AdvancedFilterModeButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) CardWhite else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) PrimaryGreen else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = Vazirmatn
        )
    }
}

@Composable
private fun AdvancedFilterCheckRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val animatedRowBackground by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) LightGreen.copy(alpha = 0.5f) else Color(0xFFF7F7F7),
        label = "filterCheckRowBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(animatedRowBackground)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (checked) PrimaryGreen else TextPrimary,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
            fontFamily = Vazirmatn
        )
        val animatedCircleColor by androidx.compose.animation.animateColorAsState(
            targetValue = if (checked) PrimaryGreen else Color(0xFFE0E0E0),
            label = "filterCheckCircle"
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(animatedCircleColor),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text("✓", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}
