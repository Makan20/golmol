package com.ordibehesht.finance.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.ordibehesht.finance.ui.theme.DividerColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ordibehesht.finance.data.model.DebtType
import com.ordibehesht.finance.data.repository.DebtRepository
import com.ordibehesht.finance.ui.theme.BackgroundLight
import com.ordibehesht.finance.ui.theme.CardWhite
import com.ordibehesht.finance.ui.theme.ExpensePurple
import com.ordibehesht.finance.ui.theme.IncomeGreen
import com.ordibehesht.finance.ui.theme.PrimaryGreen
import com.ordibehesht.finance.ui.theme.TextPrimary
import com.ordibehesht.finance.ui.theme.TextSecondary
import com.ordibehesht.finance.ui.theme.TextTertiary
import com.ordibehesht.finance.ui.theme.Vazirmatn
import com.ordibehesht.finance.ui.theme.WarningAmber
import com.ordibehesht.finance.ui.utils.PersianDateUtils
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

// توجه: DebtType از data.model وارد می‌شود (نه یک enum محلی جدا)
// تا با مدل اصلی دیتابیس همیشه یکسان بماند
enum class DebtFilter { ALL, RECEIVABLES, PAYABLES, SETTLED }

data class DebtUiItem(
    val id: Int,
    val personName: String,
    val type: DebtType,
    val originalAmount: Long,
    val remainingAmount: Long,
    val dueDate: String? = null,
    val isSettled: Boolean = false,
    // اگر این آیتم نماینده‌ی یک وام قسطی باشد (نه یک بدهی/طلب ساده)
    val loanGroupId: String? = null,
    val paidInstallments: Int = 0,
    val totalInstallments: Int = 0,
    // ساعت ثبت (HH:mm) — برای وام، ساعت ثبت نخستین قسط. اگر خالی باشد (رکوردهای قدیمی‌تر
    // از این فیلد)، در کارت نمایش داده نمی‌شود
    val createdTime: String = ""
) {
    val isLoan: Boolean get() = loanGroupId != null
}

private val PrimaryFilterColor = Color(0xFF6B8E5A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(navController: NavController, filterDate: String? = null) {
    val storedDebts by DebtRepository.debts.collectAsStateWithLifecycle()

    val (loanDebts, regularDebts) = storedDebts.partition { it.loanGroupId != null }
    val loanGroups = loanDebts.groupBy { it.loanGroupId }

    val loanItems = loanGroups.mapNotNull { (groupId, installments) ->
        if (groupId == null || installments.isEmpty()) return@mapNotNull null
        val sorted = installments.sortedBy { it.installmentNumber ?: 0 }
        val first = sorted.first()
        val allSettled = sorted.all { it.isSettled }
        // نزدیک‌ترین سررسید در میان اقساط تسویه‌نشده، برای نمایش و هشدار سررسید
        val nextDueDate = sorted.firstOrNull { !it.isSettled }?.dueDate?.takeIf { it.isNotBlank() }

        DebtUiItem(
            id = first.id,
            personName = first.personName,
            type = first.type,
            originalAmount = sorted.sumOf { it.amount },
            remainingAmount = sorted.sumOf { it.remainingAmount },
            dueDate = nextDueDate,
            isSettled = allSettled,
            loanGroupId = groupId,
            paidInstallments = sorted.count { it.isSettled },
            totalInstallments = sorted.size,
            createdTime = first.createdTime
        )
    }

    val debts = loanItems + regularDebts.map { debt ->
        DebtUiItem(
            id = debt.id,
            personName = debt.personName,
            type = debt.type,
            originalAmount = debt.amount,
            remainingAmount = debt.remainingAmount,
            dueDate = debt.dueDate.takeIf { it.isNotBlank() },
            isSettled = debt.isSettled,
            createdTime = debt.createdTime
        )
    }

    var selectedFilter by remember { mutableStateOf(DebtFilter.ALL) }
    val listState = rememberLazyListState()
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var calendarMonthOffset by remember { mutableStateOf(0) }

    val receivables = debts.filter {
        it.type == DebtType.RECEIVABLE && !it.isSettled
    }
    val payables = debts.filter {
        it.type == DebtType.PAYABLE && !it.isSettled
    }
    val totalReceivable = receivables.sumOf { it.remainingAmount }
    val totalPayable = payables.sumOf { it.remainingAmount }

    // حالت «فیلتر بر اساس یک روز خاص» — وقتی از تقویم روی روزی با چند مورد زده شود. در این
    // حالت selectedFilter/searchQuery نادیده گرفته می‌شوند تا فقط موارد همان روز دیده شوند
    val filteredDebts = if (filterDate != null) {
        debts.filter { it.dueDate == filterDate }
    } else {
        when (selectedFilter) {
            DebtFilter.ALL -> debts.filter { !it.isSettled }
            DebtFilter.RECEIVABLES -> receivables
            DebtFilter.PAYABLES -> payables
            DebtFilter.SETTLED -> debts.filter { it.isSettled }
        }.let { list ->
            if (searchQuery.isNotBlank()) {
                list.filter { it.personName.contains(searchQuery, ignoreCase = true) }
            } else {
                list
            }
        }
    }

    val hasDueSoonItems = filteredDebts.any { debt ->
        !debt.isSettled && debt.dueDate?.let { date ->
            val diff = PersianDateUtils.daysUntil(date)
            diff != null && diff in 0..7
        } == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (filterDate != null) {
                            "موارد ${PersianDateUtils.toPersianDigits(filterDate)}"
                        } else {
                            "طلب و بدهی"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontFamily = Vazirmatn
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // در حالت «فیلتر بر اساس یک روز» جستجو معنی ندارد — لیست از قبل به همان
                    // روز محدود شده است
                    if (filterDate == null) {
                        IconButton(
                            onClick = {
                                showSearch = !showSearch
                                if (!showSearch) searchQuery = ""
                            }
                        ) {
                            Icon(
                                if (showSearch) Icons.Outlined.Close else Icons.Outlined.Search,
                                if (showSearch) "بستن جستجو" else "جستجو",
                                tint = if (showSearch) PrimaryGreen else TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
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

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 130.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    if (filterDate == null && showSearch) {
                        item {
                            DebtSearchField(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // کادرهای خلاصه (نیم‌دایره) و تقویم فقط در حالت عادی نمایش داده می‌شوند —
                    // در حالت «فیلتر یک روز خاص» این‌ها بی‌ربط‌اند، چون کاربر همین الان از
                    // روی تقویم اصلی (صفحه‌ی قبلی) این‌جا آمده
                    if (filterDate == null) {
                        item {
                            DebtDonutSummaryCard(
                                totalReceivable = totalReceivable,
                                totalPayable = totalPayable
                            )
                        }

                        item { Spacer(modifier = Modifier.height(12.dp)) }

                        item {
                            DebtCalendarCard(
                                debts = debts,
                                monthOffset = calendarMonthOffset,
                                onPreviousMonth = { calendarMonthOffset -= 1 },
                                onNextMonth = { calendarMonthOffset += 1 },
                                onDayClick = { date, dayDebts ->
                                    if (dayDebts.size == 1) {
                                        val only = dayDebts.first()
                                        if (only.isLoan) {
                                            navController.navigate("loan_detail/${only.loanGroupId}")
                                        } else {
                                            navController.navigate("debt_detail/${only.id}")
                                        }
                                    } else if (dayDebts.size > 1) {
                                        navController.navigate("debts?filterDate=$date") {
                                            // این یک navigate از خودِ debts به خودش (فقط با
                                            // فیلتر متفاوت) است؛ بدون launchSingleTop، هر بار
                                            // یک نمونه‌ی جدید از همان route روی پشته push
                                            // می‌شود و آن‌ها با popUpTo(startDestination) در
                                            // ناوبری تب‌های پایین (BottomNavBar) به‌درستی
                                            // تداخل پیدا می‌کنند و می‌توانند به‌جای «بیشتر»،
                                            // دوباره همین صفحه را باز نگه دارند
                                            popUpTo("debts?filterDate={filterDate}") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(12.dp)) }

                        item {
                            DebtFilterRow(
                                selected = selectedFilter,
                                onSelect = { selectedFilter = it }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    if (filteredDebts.isEmpty()) {
                        item {
                            EmptyDebtState(
                                showSettled = selectedFilter == DebtFilter.SETTLED,
                                onAdd = { navController.navigate("add_debt") }
                            )
                        }
                    } else {
                        items(filteredDebts, key = { it.id }) { debt ->
                            DebtPersonCard(
                                debt = debt,
                                onClick = {
                                    if (debt.isLoan) {
                                        navController.navigate("loan_detail/${debt.loanGroupId}")
                                    } else {
                                        navController.navigate("debt_detail/${debt.id}")
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (hasDueSoonItems) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "بوردر نارنجی یعنی کمتر از ۷ روز تا سررسید باقی مانده است",
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

            // همون منوی پایین صفحه که تو خانه/تراکنش‌ها/گزارش‌ها هست، این‌جا هم اضافه شد —
            // فقط دکمه‌ی گلی وسطش به‌جای «ثبت تراکنش»، به «ثبت طلب و بدهی» می‌رود. دقیقاً
            // همون رفتار TransactionScreen: اول مخفی، وقتی کاربر تا انتهای لیست اسکرول کرد
            // (دیگر چیزی برای اسکرول‌کردن به پایین نمانده) ظاهر می‌شود
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 16.dp)) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !listState.canScrollForward,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    BottomNavBar(
                        navController,
                        currentRoute = "debts",
                        isScrolling = listState.isScrollInProgress,
                        fabDestination = "add_debt"
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtSearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryGreen,
            unfocusedBorderColor = DividerColor,
            focusedContainerColor = CardWhite,
            unfocusedContainerColor = CardWhite
        ),
        placeholder = {
            Text(
                "جستجو در طلب و بدهی بر اساس نام طرف حساب...",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                color = TextTertiary,
                fontSize = 14.sp,
                fontFamily = Vazirmatn
            )
        },
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Right,
            fontSize = 15.sp,
            fontFamily = Vazirmatn
        ),
        leadingIcon = {
            Icon(Icons.Outlined.Search, null, tint = TextTertiary)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, "پاک کردن", tint = TextTertiary)
                }
            }
        }
    )
}

@Composable
private fun DebtDonutSummaryCard(
    totalReceivable: Long,
    totalPayable: Long
) {
    // دقیقاً همون طراحی/الگوی ویجت «وضعیت طلب و بدهی» صفحه‌ی خانه (DebtBalanceWidget در
    // HomeScreen.kt) — طبق درخواست، به‌جای نیم‌دایره‌ی قبلی این‌جا هم عیناً همین کپی شد؛
    // فقط ردیف عنوان «وضعیت طلب و بدهی / مشاهده همه» حذف شده چون خودِ این صفحه همان
    // مقصد است. یک طراحی تازه‌تر برای این کارت بعداً جداگانه بررسی می‌شود.
    val total = totalReceivable + totalPayable
    val receivableFraction = if (total > 0L) totalReceivable.toFloat() / total.toFloat() else 0.5f
    val receivablePercent = (receivableFraction * 100).roundToInt()
    val owedPercent = 100 - receivablePercent
    val netBalance = totalReceivable - totalPayable

    val animatedFractionAnim = remember { Animatable(0f) }
    LaunchedEffect(receivableFraction) {
        animatedFractionAnim.snapTo(0f)
        animatedFractionAnim.animateTo(
            receivableFraction,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }
    val animatedFraction = animatedFractionAnim.value

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Spacer(modifier = Modifier.height(10.dp))

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IncomeGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("طلب من:", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontFamily = Vazirmatn)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${formatAmount(totalReceivable)} تومان",
                        style = MaterialTheme.typography.labelMedium,
                        color = IncomeGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpensePurple))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بدهی من:", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontFamily = Vazirmatn)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${formatAmount(totalPayable)} تومان",
                        style = MaterialTheme.typography.labelMedium,
                        color = ExpensePurple,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val dominantColor = if (totalReceivable >= totalPayable) IncomeGreen else ExpensePurple
            Box(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val trackHeight = 6.dp.toPx()
                    val trackY = size.height / 2 - trackHeight / 2
                    val cornerRadius = trackHeight / 2
                    val splitX = size.width * animatedFraction

                    drawRoundRect(
                        color = IncomeGreen,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, trackY),
                        size = androidx.compose.ui.geometry.Size(splitX.coerceAtLeast(cornerRadius), trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                    )
                    drawRoundRect(
                        color = ExpensePurple,
                        topLeft = androidx.compose.ui.geometry.Offset(splitX, trackY),
                        size = androidx.compose.ui.geometry.Size((size.width - splitX).coerceAtLeast(cornerRadius), trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                    )

                    drawCircle(color = Color.White, radius = 7.dp.toPx(), center = androidx.compose.ui.geometry.Offset(splitX, size.height / 2))
                    drawCircle(
                        color = dominantColor,
                        radius = 7.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(splitX, size.height / 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
                    )
                    drawCircle(color = dominantColor, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(splitX, size.height / 2))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("۰%", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontFamily = Vazirmatn, fontSize = 10.sp)
                    Text("۵۰%", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontFamily = Vazirmatn, fontSize = 10.sp)
                    Text("۱۰۰%", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontFamily = Vazirmatn, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            PersianDateUtils.toPersianDigits("$owedPercent% بدهی"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontFamily = Vazirmatn,
                            fontSize = 9.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    val netColor = if (netBalance >= 0) IncomeGreen else ExpensePurple
                    val sign = if (netBalance >= 0) "+" else "-"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "خالص حساب من : ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontFamily = Vazirmatn,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            formatAmount(kotlin.math.abs(netBalance)),
                            style = MaterialTheme.typography.labelSmall,
                            color = netColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Vazirmatn,
                            maxLines = 1,
                            softWrap = false
                        )
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr
                        ) {
                            Text(
                                sign,
                                style = MaterialTheme.typography.labelSmall,
                                color = netColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            " تومان",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontFamily = Vazirmatn,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            PersianDateUtils.toPersianDigits("$receivablePercent% طلب"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontFamily = Vazirmatn,
                            fontSize = 9.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtCalendarCard(
    debts: List<DebtUiItem>,
    monthOffset: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (date: String, dayDebts: List<DebtUiItem>) -> Unit
) {
    val monthInfo = remember(monthOffset) { PersianDateUtils.getMonthInfo(monthOffset) }
    // سرستون‌های کوتاه (یک حرفی) برای جمع‌وجورتر بودن تقویم؛ ترتیب هم‌راستا با weekdayIndexOf
    // (۰=شنبه ... ۶=جمعه)
    val weekDayLabels = remember { listOf("ش", "ی", "د", "س", "چ", "پ", "ج") }

    // نگاشت هر تاریخ سررسید (فقط تاریخ‌هایی که مقدار دارند) به لیست آیتم‌های همان روز —
    // برای نمایش سریع نقاط رنگی زیر هر روز از تقویم
    val debtsByDueDate = remember(debts) {
        debts.filter { !it.dueDate.isNullOrBlank() }.groupBy { it.dueDate!! }
    }

    val firstWeekdayIndex = remember(monthInfo) {
        PersianDateUtils.weekdayIndexOf(monthInfo.year, monthInfo.month, 1)
    }
    // تعداد خانه‌های خالیِ ابتدای جدول (روزهای ماه قبل که در همان هفته‌ی اول جا می‌گیرند)
    val leadingBlanks = firstWeekdayIndex
    val totalCells = leadingBlanks + monthInfo.dates.size
    val trailingBlanks = (7 - totalCells % 7).let { if (it == 7) 0 else it }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ناوبری ماه: چون این تقویم صرفاً برای مرور سررسیدهای ثبت‌شده است (نه ورود
            // تاریخ)، برخلاف MonthNavigator صفحه‌ی گزارش‌ها، رفتن به ماه‌های آینده هم مجاز
            // است — کاربر ممکن است بخواهد سررسیدهای از قبل برنامه‌ریزی‌شده‌ی چند ماه بعد را
            // (مثلاً اقساط یک وام بلندمدت) از قبل مرور کند
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "ماه بعد",
                        tint = TextSecondary
                    )
                }

                Text(
                    text = "${monthNamePersian(monthInfo.month)} ${PersianDateUtils.toPersianDigits(monthInfo.year.toString())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )

                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = "ماه قبل",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                weekDayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // خانه‌های ابتدا/انتهای ماه‌های مجاور (خاکستری، بدون کلیک) — remember همیشه
            // بدون‌قید‌وشرط صدا زده می‌شود (نه داخل یک if)، چون فراخوانی شرطی composable ها
            // می‌تواند ترتیب فراخوانی‌ها را بین recomposition ها به‌هم بریزد و کرش کند؛ فقط
            // *استفاده* از نتیجه‌اش (وقتی leadingBlanks/trailingBlanks صفر است) شرطی است
            val prevMonthInfo = remember(monthOffset) { PersianDateUtils.getMonthInfo(monthOffset - 1) }
            val nextMonthInfo = remember(monthOffset) { PersianDateUtils.getMonthInfo(monthOffset + 1) }

            val allCells = buildList {
                if (leadingBlanks > 0) {
                    val prevDays = prevMonthInfo.dates.takeLast(leadingBlanks)
                    prevDays.forEach { add(CalendarCell(it, isCurrentMonth = false)) }
                }
                monthInfo.dates.forEach { add(CalendarCell(it, isCurrentMonth = true)) }
                if (trailingBlanks > 0) {
                    val nextDays = nextMonthInfo.dates.take(trailingBlanks)
                    nextDays.forEach { add(CalendarCell(it, isCurrentMonth = false)) }
                }
            }

            val today = remember { PersianDateUtils.getCurrentPersianDate() }

            allCells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        val dayDebts = debtsByDueDate[cell.date].orEmpty()
                        CalendarDayCell(
                            cell = cell,
                            isToday = cell.date == today,
                            dayDebts = dayDebts,
                            onClick = {
                                if (cell.isCurrentMonth && dayDebts.isNotEmpty()) {
                                    onDayClick(cell.date, dayDebts)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CalendarLegendDot(color = IncomeGreen, label = "طلب")
                Spacer(modifier = Modifier.width(14.dp))
                CalendarLegendDot(color = ExpensePurple, label = "بدهی")
                Spacer(modifier = Modifier.width(14.dp))
                CalendarLegendDot(color = WarningAmber, label = "وام")
            }
        }
    }
}

private data class CalendarCell(val date: String, val isCurrentMonth: Boolean)

@Composable
private fun CalendarDayCell(
    cell: CalendarCell,
    isToday: Boolean,
    dayDebts: List<DebtUiItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayNumber = cell.date.substringAfterLast("/").toIntOrNull()?.toString() ?: "?"
    val hasReceivable = dayDebts.any { it.type == DebtType.RECEIVABLE && !it.isLoan }
    val hasPayable = dayDebts.any { it.type == DebtType.PAYABLE && !it.isLoan }
    val hasLoan = dayDebts.any { it.isLoan }

    Box(
        modifier = modifier
            .padding(vertical = 3.dp)
            .clip(CircleShape)
            .then(
                if (cell.isCurrentMonth && dayDebts.isNotEmpty()) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .then(
                if (isToday && cell.isCurrentMonth) {
                    Modifier.background(PrimaryGreen.copy(alpha = 0.12f))
                } else {
                    Modifier
                }
            )
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = PersianDateUtils.toPersianDigits(dayNumber),
                style = MaterialTheme.typography.bodySmall,
                color = if (cell.isCurrentMonth) TextPrimary else TextTertiary.copy(alpha = 0.5f),
                fontWeight = if (isToday && cell.isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                fontFamily = Vazirmatn
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (hasReceivable) DotIndicator(IncomeGreen)
                if (hasPayable) DotIndicator(ExpensePurple)
                if (hasLoan) DotIndicator(WarningAmber)
            }
        }
    }
}

@Composable
private fun DotIndicator(color: Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun CalendarLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontFamily = Vazirmatn
        )
    }
}

private fun monthNamePersian(month: Int): String = when (month) {
    1 -> "فروردین"
    2 -> "اردیبهشت"
    3 -> "خرداد"
    4 -> "تیر"
    5 -> "مرداد"
    6 -> "شهریور"
    7 -> "مهر"
    8 -> "آبان"
    9 -> "آذر"
    10 -> "دی"
    11 -> "بهمن"
    12 -> "اسفند"
    else -> ""
}

@Composable
private fun DebtFilterRow(
    selected: DebtFilter,
    onSelect: (DebtFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DebtFilterSelector(
            firstText = "طلب‌ها",
            secondText = "بدهی‌ها",
            firstFilter = DebtFilter.RECEIVABLES,
            secondFilter = DebtFilter.PAYABLES,
            selected = selected,
            firstColor = IncomeGreen,
            secondColor = ExpensePurple,
            modifier = Modifier.weight(1f),
            onSelect = onSelect
        )

        DebtFilterSelector(
            firstText = "همه",
            secondText = "تسویه شده",
            firstFilter = DebtFilter.ALL,
            secondFilter = DebtFilter.SETTLED,
            selected = selected,
            firstColor = PrimaryFilterColor,
            secondColor = TextSecondary,
            modifier = Modifier.weight(1f),
            onSelect = onSelect
        )
    }
}

@Composable
private fun DebtFilterSelector(
    firstText: String,
    secondText: String,
    firstFilter: DebtFilter,
    secondFilter: DebtFilter,
    selected: DebtFilter,
    firstColor: Color,
    secondColor: Color,
    modifier: Modifier = Modifier,
    onSelect: (DebtFilter) -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DividerColor.copy(alpha = 0.50f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DebtFilterButton(
            text = firstText,
            selected = selected == firstFilter,
            color = firstColor,
            modifier = Modifier.weight(1f),
            onClick = {
                onSelect(firstFilter)
            }
        )

        DebtFilterButton(
            text = secondText,
            selected = selected == secondFilter,
            color = secondColor,
            modifier = Modifier.weight(1f),
            onClick = {
                onSelect(secondFilter)
            }
        )
    }
}

@Composable
private fun DebtFilterButton(
    text: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) color else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else TextSecondary,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            fontFamily = Vazirmatn,
            textAlign = TextAlign.Center
        )
    }
}
@Composable
private fun DebtPersonCard(
    debt: DebtUiItem,
    onClick: () -> Unit
) {
    val accentColor = if (debt.type == DebtType.RECEIVABLE) {
        IncomeGreen
    } else {
        ExpensePurple
    }

    val relation = when {
    debt.isLoan && debt.isSettled -> {
        "تمام اقساط تسویه شد"
    }

    debt.isLoan -> {
        "قسط ${PersianDateUtils.toPersianDigits((debt.paidInstallments + 1).coerceAtMost(debt.totalInstallments).toString())} از ${PersianDateUtils.toPersianDigits(debt.totalInstallments.toString())}"
    }

    debt.isSettled &&
        debt.type == DebtType.RECEIVABLE -> {
        "به من بدهکار نیست"
    }

    debt.isSettled &&
        debt.type == DebtType.PAYABLE -> {
        "من تسویه کردم"
    }

    debt.type == DebtType.RECEIVABLE -> {
        "به من بدهکار است"
    }

    else -> {
        "من بدهکارم"
    }
}

    val paidAmount = (debt.originalAmount - debt.remainingAmount)
        .coerceAtLeast(0L)

    val progress = if (debt.originalAmount > 0) {
        paidAmount.toFloat() / debt.originalAmount
    } else {
        0f
    }

    // اگر تسویه نشده و کمتر از ۷ روز (و بیشتر از صفر روز، یعنی هنوز نگذشته) به سررسید مانده باشد
    val isDueSoon = !debt.isSettled && debt.dueDate?.let { date ->
        val diff = PersianDateUtils.daysUntil(date)
        diff != null && diff in 0..7
    } == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isDueSoon) {
                    Modifier.border(1.5.dp, WarningAmber, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (debt.isLoan) Icons.Outlined.CreditCard else Icons.Outlined.Person,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.personName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )

                    Text(
                        text = relation,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontFamily = Vazirmatn
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = debtAmount(debt.remainingAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn,
                        textAlign = TextAlign.Left
                    )
                    // ساعت ثبت — فقط اگر واقعاً ثبت شده باشد (رکوردهای قدیمی‌تر از این فیلد
                    // این مقدار را ندارند، پس چیزی نمایش داده نمی‌شود)
                    if (debt.createdTime.isNotBlank()) {
                        Text(
                            text = "ثبت ${PersianDateUtils.toPersianDigits(debt.createdTime)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (debt.isLoan) {
                        "${PersianDateUtils.toPersianDigits(debt.paidInstallments.toString())} از ${PersianDateUtils.toPersianDigits(debt.totalInstallments.toString())} قسط"
                    } else {
                        "${PersianDateUtils.toPersianDigits((progress * 100).toInt().toString())}٪ تسویه"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontFamily = Vazirmatn
                )

                debt.dueDate?.let { dueDate ->
                    Text(
                        text = "سررسید: ${PersianDateUtils.toPersianDigits(dueDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontFamily = Vazirmatn
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDebtState(
    showSettled: Boolean,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 30.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (showSettled) {
                            TextSecondary.copy(alpha = 0.10f)
                        } else {
                            IncomeGreen.copy(alpha = 0.10f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = if (showSettled) TextSecondary else IncomeGreen,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (showSettled) {
                    "مورد تسویه‌شده‌ای وجود ندارد"
                } else {
                    "هنوز طلب یا بدهی ثبت نشده است"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontFamily = Vazirmatn,
                textAlign = TextAlign.Center
            )

            if (!showSettled) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "برای مدیریت قرض‌ها و پرداخت‌ها، اولین مورد را ثبت کنید",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontFamily = Vazirmatn,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "طلب و بدهی‌های تسویه‌شده اینجا نمایش داده می‌شوند",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontFamily = Vazirmatn,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun debtAmount(amount: Long): String {
    val absolute = NumberFormat.getInstance(Locale.US)
        .format(kotlin.math.abs(amount))

    val sign = if (amount < 0) "−" else ""

    return "\u2066$sign${PersianDateUtils.toPersianDigits(absolute)}\u2069 تومان"
}