package com.example.nargesapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nargesapp.ui.theme.BackgroundLight
import com.example.nargesapp.ui.theme.CardWhite
import com.example.nargesapp.ui.theme.DividerColor
import com.example.nargesapp.ui.theme.ExpensePurple
import com.example.nargesapp.ui.theme.LightGreen
import com.example.nargesapp.ui.theme.PrimaryGreen
import com.example.nargesapp.ui.theme.TextPrimary
import com.example.nargesapp.ui.theme.TextTertiary
import com.example.nargesapp.ui.theme.Vazirmatn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    val debts by com.example.nargesapp.data.repository.DebtRepository.debts.collectAsStateWithLifecycle()
    val hasDueSoonDebts = debts.any { debt ->
        !debt.isSettled && debt.dueDate.takeIf { it.isNotBlank() }?.let { date ->
            val diff = com.example.nargesapp.ui.utils.PersianDateUtils.daysUntil(date)
            diff != null && diff in 0..7
        } == true
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "بیشتر",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontFamily = Vazirmatn
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            FlowerDecoration(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 4.dp)
                    .size(60.dp)
            )

            FlowerDecoration(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 100.dp, start = 4.dp)
                    .size(50.dp),
                color = ExpensePurple.copy(alpha = 0.08f)
            )

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                            MoreMenuRow(
                                label = "کارت‌ها",
                                icon = Icons.Outlined.CreditCard,
                                enabled = true,
                                onClick = { navController.navigate("cards") }
                            )

                            HorizontalDivider(
                                color = DividerColor,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            MoreMenuRow(
                                label = "فعال‌سازی قفل",
                                icon = Icons.Outlined.Fingerprint,
                                enabled = true,
                                onClick = { navController.navigate("lock_setup") }
                            )

                            HorizontalDivider(
                                color = DividerColor,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            MoreMenuRow(
                                label = "دسته‌بندی‌ها",
                                icon = Icons.AutoMirrored.Outlined.List,
                                enabled = false,
                                onClick = { }
                            )

                            HorizontalDivider(
                                color = DividerColor,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            MoreMenuRow(
                                label = "طلب و بدهی",
                                icon = Icons.Outlined.CurrencyExchange,
                                enabled = true,
                                onClick = { navController.navigate("debts") },
                                showBadge = hasDueSoonDebts
                            )

                            HorizontalDivider(
                                color = DividerColor,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            MoreMenuRow(
                                label = "تنظیمات",
                                icon = Icons.Outlined.Settings,
                                enabled = true,
                                onClick = { navController.navigate("settings") }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !scrollState.canScrollForward,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    BottomNavBar(navController, currentRoute = "more", isScrolling = scrollState.isScrollInProgress)
                }
            }
        }
    }
}

@Composable
fun MoreMenuRow(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    showBadge: Boolean = false
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (enabled) LightGreen else DividerColor.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) PrimaryGreen else TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (showBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(ExpensePurple)
                            .border(1.5.dp, CardWhite, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) TextPrimary else TextTertiary,
                fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal,
                fontFamily = Vazirmatn
            )
        }

        if (enabled) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
