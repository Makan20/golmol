package com.example.nargesapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.nargesapp.data.model.DebtNotification
import com.example.nargesapp.data.repository.DebtNotificationRepository
import com.example.nargesapp.ui.theme.*
import com.example.nargesapp.ui.utils.PersianDateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController) {
    val notifications by DebtNotificationRepository.notifications.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val unreadCount = notifications.count { !it.isRead }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "اعلان‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontFamily = Vazirmatn
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "بازگشت", tint = TextPrimary)
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        IconButton(onClick = { DebtNotificationRepository.markAllAsRead() }) {
                            Icon(Icons.Outlined.DoneAll, "همه را خواندم", tint = PrimaryGreen)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            FlowerDecoration(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 100.dp, end = 4.dp).size(56.dp)
            )
            FlowerDecoration(
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 100.dp, start = 4.dp).size(50.dp),
                color = ExpensePurple.copy(alpha = 0.08f)
            )

            if (notifications.isEmpty()) {
                EmptyNotificationsState(modifier = Modifier.padding(padding))
            } else {
                val today = PersianDateUtils.getCurrentPersianDate()
                val yesterday = PersianDateUtils.getYesterdayPersianDate()
                val grouped = notifications.groupBy { it.date }
                val sortedEntries = grouped.entries.sortedWith(
                    compareBy<Map.Entry<String, List<DebtNotification>>> { (date, _) ->
                        when (date) {
                            today -> 0
                            yesterday -> 1
                            else -> 2
                        }
                    }.thenByDescending { (date, _) -> date }
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    sortedEntries.forEach { (date, items) ->
                        val dayLabel = when (date) {
                            today -> "امروز"
                            yesterday -> "دیروز"
                            else -> PersianDateUtils.toPersianDigits(date)
                        }

                        item(key = "header_$date") {
                            Text(
                                dayLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary,
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        }

                        items(items, key = { it.id }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onClick = {
                                    if (!notification.isRead) {
                                        DebtNotificationRepository.markAsRead(notification)
                                    }
                                },
                                onDelete = { DebtNotificationRepository.delete(notification) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "با سویپ به چپ و راست پیام حذف می‌شود",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontFamily = Vazirmatn,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(LightGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.NotificationsNone, null, tint = PrimaryGreen, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "اعلانی وجود ندارد",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "یادآوری‌ها اینجا نمایش داده می‌شوند",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontFamily = Vazirmatn,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NotificationCard(notification: DebtNotification, onClick: () -> Unit, onDelete: () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val swipeThreshold = with(density) { 80.dp.toPx() }
    var offsetX by remember(notification.id) { mutableFloatStateOf(0f) }
    val animatedOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = offsetX,
        label = "notificationSwipeOffset"
    )

    val isSwiping = kotlin.math.abs(animatedOffset) > swipeThreshold
    val backgroundColor = if (isSwiping) Color(0xFFFFEBEE) else CardWhite

    // detectHorizontalDragGestures همیشه جابه‌جایی فیزیکی خام گزارش می‌دهد (مستقل از جهت صفحه)،
    // پس جهت را Ltr نگه می‌داریم تا انگشت، اسلاید، و آیکون هماهنگ بمانند
    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(backgroundColor)
        ) {
            if (isSwiping) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 18.dp)
                        .align(if (animatedOffset > 0) Alignment.CenterStart else Alignment.CenterEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.DeleteOutline, null, tint = Color(0xFFE53935), modifier = Modifier.size(22.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { androidx.compose.ui.unit.IntOffset(animatedOffset.toInt(), 0) }
                    .pointerInput(notification.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (kotlin.math.abs(offsetX) > swipeThreshold) {
                                    onDelete()
                                }
                                offsetX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount
                            }
                        )
                    }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onClick),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    val isReceivableNotification = notification.title.contains("طلب")
                    val notificationAccentColor = if (isReceivableNotification) IncomeGreen else ExpensePurple
                    val notificationBgColor = if (isReceivableNotification) LightGreen else Color(0xFFF3E5F5)

                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(notificationBgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.CalendarMonth, null, tint = notificationAccentColor, modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        notification.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        fontFamily = Vazirmatn,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        PersianDateUtils.toPersianDigits(notification.time),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary,
                                        fontFamily = Vazirmatn
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    notification.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontFamily = Vazirmatn,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            if (!notification.isRead) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpensePurple))
                            } else {
                                Box(modifier = Modifier.size(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
