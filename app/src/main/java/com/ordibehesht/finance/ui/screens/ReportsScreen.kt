package com.ordibehesht.finance.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ordibehesht.finance.data.model.Account
import com.ordibehesht.finance.data.model.Debt
import com.ordibehesht.finance.data.model.DebtType
import com.ordibehesht.finance.data.model.Transaction
import com.ordibehesht.finance.data.model.TransactionType
import com.ordibehesht.finance.data.repository.DebtRepository
import com.ordibehesht.finance.ui.theme.*
import com.ordibehesht.finance.ui.utils.PersianDateUtils
import com.ordibehesht.finance.ui.utils.ReportPreferences
import com.ordibehesht.finance.ui.viewmodel.TransactionViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val chartPalette = listOf(
    ExpensePurple,
    Color(0xFFE0A458),
    Color(0xFF5B9BD5),
    Color(0xFFE0678C),
    Color(0xFF6FCF97),
    Color(0xFFB0855A)
)

private val incomeChartPalette = listOf(
    IncomeGreen,
    PrimaryGreen,
    Color(0xFF5B9BD5),
    Color(0xFFE0A458),
    Color(0xFF6FCF97),
    Color(0xFFB0855A)
)

private fun topCategoriesWithOther(byCategory: Map<String, Long>, maxSlices: Int = 5): List<Pair<String, Long>> {
    val sorted = byCategory.toList().sortedByDescending { it.second }
    if (sorted.size <= maxSlices) return sorted
    val top = sorted.take(maxSlices - 1)
    val otherSum = sorted.drop(maxSlices - 1).sumOf { it.second }
    return top + ("سایر" to otherSum)
}

private enum class BreakdownTab { EXPENSE, INCOME }

private fun monthlyTrend(
    transactions: List<Transaction>,
    endYear: Int,
    endMonth: Int,
    count: Int = 6
): List<Triple<String, Long, Long>> {
    val result = mutableListOf<Triple<String, Long, Long>>()
    var y = endYear
    var m = endMonth
    repeat(count) {
        val monthTx = transactions.filter { t ->
            val parts = t.date.split("/")
            parts.getOrNull(0)?.toIntOrNull() == y && parts.getOrNull(1)?.toIntOrNull() == m && t.transferGroupId == null
        }
        val income = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val label = PersianDateUtils.persianMonthNames.getOrElse(m - 1) { "" }
        result.add(Triple(label, income, expense))
        m -= 1
        if (m == 0) {
            m = 12
            y -= 1
        }
    }
    return result.reversed()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController, viewModel: TransactionViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by com.ordibehesht.finance.data.repository.AccountRepository.accounts.collectAsStateWithLifecycle()

    val showAmountInReports = remember { mutableStateOf(ReportPreferences.isAmountModeEnabled(context)) }

    var periodType by remember { mutableStateOf(PeriodType.MONTH) }
    val currentYearMonth = remember {
        val parts = PersianDateUtils.getCurrentPersianDate().split("/")
        (parts.getOrNull(0)?.toIntOrNull() ?: 1404) to (parts.getOrNull(1)?.toIntOrNull() ?: 1)
    }
    var selectedYear by remember { mutableStateOf(currentYearMonth.first) }
    var selectedMonth by remember { mutableStateOf(currentYearMonth.second) }
    // ناوبری برای حالت‌های «هفته» و «روز» — مشابه selectedYear/selectedMonth برای ماه/سال؛
    // ۰ یعنی هفته/روز جاری، منفی یعنی به عقب (هماهنگ با PersianDateUtils.getWeekDates/addDaysToToday)
    var weekOffset by remember { mutableStateOf(0) }
    var dayOffset by remember { mutableStateOf(0) }

    // گزارش‌های درآمد/هزینه شامل تراکنش‌های خودکارِ تسویه‌ی بدهی/طلب هم می‌شوند —
    // چون این‌ها تراکنش مالی واقعی‌اند (پول واقعاً جابه‌جا شده)
    val periodFiltered = filterByPeriod(transactions, periodType, selectedYear, selectedMonth, weekOffset, dayOffset)
    // انتقال بین کارت‌های خودِ کاربر (transferGroupId != null) جابه‌جایی داخلی است، نه
    // درآمد/هزینه‌ی واقعی — از تمام آمار گزارش‌ها کنار گذاشته می‌شود (مشابه HomeScreen)
    val periodIncome = periodFiltered.filter { it.type == TransactionType.INCOME && it.transferGroupId == null }.sumOf { it.amount }
    val periodExpense = periodFiltered.filter { it.type == TransactionType.EXPENSE && it.transferGroupId == null }.sumOf { it.amount }

    val previousPeriodFiltered = when (periodType) {
        PeriodType.MONTH -> {
            val prevYear = if (selectedMonth == 1) selectedYear - 1 else selectedYear
            val prevMonth = if (selectedMonth == 1) 12 else selectedMonth - 1
            filterByPeriod(transactions, PeriodType.MONTH, prevYear, prevMonth)
        }
        PeriodType.YEAR -> filterByPeriod(transactions, PeriodType.YEAR, selectedYear - 1, selectedMonth)
        PeriodType.WEEK -> filterByPeriod(transactions, PeriodType.WEEK, selectedYear, selectedMonth, weekOffset - 1)
        PeriodType.DAY -> filterByPeriod(transactions, PeriodType.DAY, selectedYear, selectedMonth, dayOffset = dayOffset - 1)
    }
    val previousIncome = previousPeriodFiltered.filter { it.type == TransactionType.INCOME && it.transferGroupId == null }.sumOf { it.amount }
    val previousExpense = previousPeriodFiltered.filter { it.type == TransactionType.EXPENSE && it.transferGroupId == null }.sumOf { it.amount }

    val expenseByCategory = remember(periodFiltered) {
        val grouped = periodFiltered
            .filter { it.type == TransactionType.EXPENSE && it.transferGroupId == null }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        topCategoriesWithOther(grouped)
    }
    val incomeByCategory = remember(periodFiltered) {
        val grouped = periodFiltered
            .filter { it.type == TransactionType.INCOME && it.transferGroupId == null }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        topCategoriesWithOther(grouped)
    }

    // باگ رفع‌شده: این سه تابع (accountAmountBreakdown/accountAmountByName/accountMinMaxBreakdown)
    // بر خلاف بقیه‌ی محاسبات این صفحه (periodIncome/periodExpense/expenseByCategory/...)
    // فیلتر transferGroupId == null را اعمال نمی‌کردند، پس یک انتقال بین کارت‌های خودِ کاربر هم
    // در «مبلغ بر اساس کارت» و در «بیشترین/کمترین تراکنش هر کارت» لحاظ می‌شد — و عدد «کل
    // هزینه‌ها/درآمدها»ی وسط نمودار دایره‌ای می‌توانست با کارت خلاصه‌ی بالای همین صفحه ناهماهنگ
    // باشد. حالا همان لیست‌های بدون-انتقالِ periodFiltered/previousPeriodFiltered که بقیه‌ی
    // بخش‌های این صفحه هم استفاده می‌کنند، اینجا هم پاس داده می‌شود.
    val periodFilteredNoTransfer = periodFiltered.filter { it.transferGroupId == null }
    val previousPeriodFilteredNoTransfer = previousPeriodFiltered.filter { it.transferGroupId == null }

    val expenseByAccount = remember(periodFilteredNoTransfer, accounts) {
        accountAmountBreakdown(periodFilteredNoTransfer, TransactionType.EXPENSE, accounts)
    }
    val incomeByAccount = remember(periodFilteredNoTransfer, accounts) {
        accountAmountBreakdown(periodFilteredNoTransfer, TransactionType.INCOME, accounts)
    }
    // برای نمایش «نسبت به ماه قبل بیشتر/کمتر» کنار هر کارت در AccountAmountBreakdownCard
    val previousExpenseByAccount = remember(previousPeriodFilteredNoTransfer, accounts) {
        accountAmountByName(previousPeriodFilteredNoTransfer, TransactionType.EXPENSE, accounts)
    }
    val previousIncomeByAccount = remember(previousPeriodFilteredNoTransfer, accounts) {
        accountAmountByName(previousPeriodFilteredNoTransfer, TransactionType.INCOME, accounts)
    }
    val accountExtremes = remember(periodFilteredNoTransfer, accounts) {
        accountMinMaxBreakdown(periodFilteredNoTransfer, accounts)
    }
    val trendData = remember(transactions, selectedYear, selectedMonth) {
        monthlyTrend(transactions, selectedYear, selectedMonth)
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "گزارش‌ها",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            FlowerDecoration(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 4.dp).size(60.dp)
            )
            FlowerDecoration(
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 100.dp, start = 4.dp).size(50.dp),
                color = ExpensePurple.copy(alpha = 0.08f)
            )

            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    stickyHeader {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundLight)
                                .padding(bottom = 12.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            PeriodFilterRow(
                                periodType = periodType,
                                accentColor = PrimaryGreen,
                                onPeriodSelected = { periodType = it }
                            )
                            if (periodType == PeriodType.MONTH) {
                                Spacer(modifier = Modifier.height(10.dp))
                                MonthNavigator(
                                    year = selectedYear,
                                    month = selectedMonth,
                                    onPrevious = {
                                        if (selectedMonth == 1) {
                                            selectedMonth = 12
                                            selectedYear -= 1
                                        } else {
                                            selectedMonth -= 1
                                        }
                                    },
                                    onNext = {
                                        if (selectedMonth == 12) {
                                            selectedMonth = 1
                                            selectedYear += 1
                                        } else {
                                            selectedMonth += 1
                                        }
                                    }
                                )
                            } else if (periodType == PeriodType.YEAR) {
                                Spacer(modifier = Modifier.height(10.dp))
                                YearNavigator(
                                    year = selectedYear,
                                    onPrevious = { selectedYear -= 1 },
                                    onNext = { selectedYear += 1 }
                                )
                            } else if (periodType == PeriodType.WEEK) {
                                Spacer(modifier = Modifier.height(10.dp))
                                WeekNavigator(
                                    weekOffset = weekOffset,
                                    onPrevious = { weekOffset -= 1 },
                                    onNext = { if (weekOffset < 0) weekOffset += 1 }
                                )
                            } else if (periodType == PeriodType.DAY) {
                                Spacer(modifier = Modifier.height(10.dp))
                                DayNavigator(
                                    dayOffset = dayOffset,
                                    onPrevious = { dayOffset -= 1 },
                                    onNext = { if (dayOffset < 0) dayOffset += 1 }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        PeriodSummaryCard(periodIncome, periodExpense, periodType)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        CategoryBreakdownCard(
                            expenseCategories = expenseByCategory,
                            incomeCategories = incomeByCategory,
                            totalExpense = periodExpense,
                            totalIncome = periodIncome,
                            previousExpense = previousExpense,
                            previousIncome = previousIncome,
                            periodType = periodType,
                            showAmount = showAmountInReports.value
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        val hasAnyTopEntries = remember(periodFiltered) {
                            periodFiltered.any { it.debtId == null && (it.type == TransactionType.EXPENSE || it.type == TransactionType.INCOME) }
                        }
                        if (hasAnyTopEntries) {
                            TopExpensesCard(
                                transactions = periodFiltered,
                                showAmount = showAmountInReports.value
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    IncomeExpenseComparison(
                                        income = periodIncome,
                                        expense = periodExpense,
                                        showAmount = showAmountInReports.value
                                    )
                                }
                                FlowerDecoration(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(40.dp),
                                    color = ExpensePurple.copy(alpha = 0.10f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        AccountAmountBreakdownCard(
                            expenseBreakdown = expenseByAccount,
                            incomeBreakdown = incomeByAccount,
                            previousExpenseByName = previousExpenseByAccount,
                            previousIncomeByName = previousIncomeByAccount,
                            periodType = periodType,
                            showAmount = showAmountInReports.value
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        AccountMinMaxCard(accountExtremes, showAmount = showAmountInReports.value)
                        Spacer(modifier = Modifier.height(16.dp))
                        DebtBreakdownCard(showAmount = showAmountInReports.value)
                        Spacer(modifier = Modifier.height(8.dp))

                        // این راهنما فقط به کارت‌های درصدی/مبلغی بالا (دسته‌بندی، مبلغ بر
                        // اساس کارت، بیشترین/کمترین، طلب و بدهی) مربوط است، نه به نمودار
                        // روند ۶ ماهه که زیرش می‌آید — پس همین‌جا، بلافاصله بعد از
                        // DebtBreakdownCard (آخرین کارت درصدی) قرار گرفت
                        Text(
                            "برای تغییر درصد به مبلغ از تنظیمات اقدام کنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontFamily = Vazirmatn,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // «روند ۶ ماه اخیر» عمداً به انتهای صفحه منتقل شد (قبلاً دومین کارت
                        // بعد از خلاصه‌ی بالای صفحه بود و کاربر تازه‌وارد ممکن بود این نمودار
                        // را با محاسبات بازه‌ی انتخابی اشتباه بگیرد، چون این نمودار همیشه بر
                        // پایه‌ی ۶ ماه اخیر است، نه بازه‌ی روز/هفته/ماه/سالِ فیلترشده). این
                        // کارت با درصد/مبلغ کاری ندارد، پس عمداً بعد از متن راهنمای بالا آمد.
                        TrendChartCard(trendData)
                        Spacer(modifier = Modifier.height(8.dp))
                        // راهنمای تعاملی زیر همین نمودار — دقیقاً هم‌الگو با راهنماهای دیگر
                        // صفحه (مثلاً «برای ویرایش به راست و برای حذف به چپ بکشید» در
                        // TransactionScreen)؛ چون این نمودار با تپ روی هر میله جزئیات همان
                        // ماه را در چیپ‌های بالا نشان می‌دهد (ر.ک. TrendChartCard/TrendBarChart)
                        // و این قابلیت بدون یک راهنمای کوچک، برای کاربر قابل کشف نیست
                        Text(
                            "برای دیدن جزئیات روی نمودار بزنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontFamily = Vazirmatn,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(130.dp))
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 16.dp)) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !listState.canScrollForward,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    BottomNavBar(navController, currentRoute = "reports", isScrolling = listState.isScrollInProgress)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PrimaryGreen)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontFamily = Vazirmatn
        )
    }
}

@Composable
private fun DebtBreakdownCard(showAmount: Boolean) {
    val debts by DebtRepository.debts.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(BreakdownTab.EXPENSE) }

    // در این کارت: تب "هزینه" یعنی بدهی من، تب "درآمد" یعنی طلب من
    val debtType = if (tab == BreakdownTab.EXPENSE) DebtType.PAYABLE else DebtType.RECEIVABLE
    val accentColor = if (tab == BreakdownTab.EXPENSE) ExpensePurple else IncomeGreen
    val filteredDebts = debts.filter { it.type == debtType }

    val settledAmount = filteredDebts.sumOf { it.paidAmount }
    val remainingAmount = filteredDebts.sumOf { it.remainingAmount }
    val total = settledAmount + remainingAmount

    val breakdown = listOf(
        Triple("باقی‌مانده", remainingAmount, accentColor),
        Triple("تسویه‌شده", settledAmount, accentColor.copy(alpha = 0.35f))
    ).filter { it.second > 0L }

    val centerLabel = if (tab == BreakdownTab.EXPENSE) "کل بدهی" else "کل طلب"
    val emptyMessage = if (tab == BreakdownTab.EXPENSE) "بدهی‌ای ثبت نشده" else "طلبی ثبت نشده"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "طلب و بدهی",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DividerColor.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    BreakdownTabButton(
                        text = "بدهی",
                        selected = tab == BreakdownTab.EXPENSE,
                        color = ExpensePurple,
                        onClick = { tab = BreakdownTab.EXPENSE }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BreakdownTabButton(
                        text = "طلب",
                        selected = tab == BreakdownTab.INCOME,
                        color = IncomeGreen,
                        onClick = { tab = BreakdownTab.INCOME }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (breakdown.isEmpty() || total <= 0L) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FlowerDecoration(
                            modifier = Modifier.size(52.dp),
                            color = IncomeGreen.copy(alpha = 0.14f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            } else {
                val categories = breakdown.map { it.first to it.second }
                val colors = breakdown.map { it.third }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        breakdown.forEach { (name, amount, color) ->
                            CategoryLegendRow(name = name, amount = amount, total = total, color = color, showAmount = showAmount)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CategoryDonutChart(categories, colors, total, centerLabel = centerLabel)
                }
            }
        }
    }
}

@Composable
fun PeriodSummaryCard(income: Long, expense: Long, periodType: PeriodType) {
    val net = income - expense
    val periodLabel = when (periodType) {
        PeriodType.WEEK -> "این هفته"
        PeriodType.MONTH -> "این ماه"
        PeriodType.YEAR -> "امسال"
PeriodType.DAY -> "امروز"
    }
    val netColor = if (net >= 0) IncomeGreen else ExpensePurple

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = netColor.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "خالص $periodLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontFamily = Vazirmatn
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatPersianAmount(net),
                    style = MaterialTheme.typography.headlineMedium,
                    color = netColor,
                    fontFamily = Vazirmatn
                )
                Text(
                    "تومان",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontFamily = Vazirmatn
                )
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(netColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (net >= 0) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                    null,
                    tint = netColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

// نمودار روند با همان جلوه‌های HomeScreen: چیپ‌های تعاملی درآمد/هزینه (با انیمیشن عدد و
// قابلیت کلیک برای دیدن جزئیات یک بازه‌ی خاص)، خط‌چین‌های راهنما، و گل‌های تزئینی پشت نمودار
@Composable
private fun TrendChartCard(data: List<Triple<String, Long, Long>>) {
    val hasData = data.any { it.second > 0 || it.third > 0 }

    var selectedIndex by remember(data) { mutableStateOf<Int?>(null) }

    val totalIncome = data.sumOf { it.second }
    val totalExpense = data.sumOf { it.third }
    val chipIncome = selectedIndex?.let { data.getOrNull(it)?.second } ?: totalIncome
    val chipExpense = selectedIndex?.let { data.getOrNull(it)?.third } ?: totalExpense

    val animatedChipIncome by animateFloatAsState(
        targetValue = chipIncome.toFloat(),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "reportsChipIncomeAnim"
    )
    val displayChipIncome = if (animatedChipIncome == chipIncome.toFloat()) chipIncome else animatedChipIncome.toLong()
    val animatedChipExpense by animateFloatAsState(
        targetValue = chipExpense.toFloat(),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "reportsChipExpenseAnim"
    )
    val displayChipExpense = if (animatedChipExpense == chipExpense.toFloat()) chipExpense else animatedChipExpense.toLong()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "روند ۶ ماه اخیر",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )
                Row {
                    ChartLegend("درآمد", BarIncomeGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    ChartLegend("هزینه", BarExpensePurple)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasData) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FlowerDecoration(
                            modifier = Modifier.size(52.dp),
                            color = ExpensePurple.copy(alpha = 0.14f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "داده‌ای برای این بازه نیست",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            } else {
                // چیپ‌های تعاملی درآمد/هزینه — با کلیک روی یک میله، عدد این چیپ‌ها
                // به مقدار همان ماه/بازه عوض می‌شود (دقیقاً مثل PeriodChartSection در HomeScreen)
                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(visible = selectedIndex != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(TextPrimary.copy(alpha = 0.88f))
                                        .clickable { selectedIndex = null }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        (data.getOrNull(selectedIndex ?: -1)?.first ?: "") + "  ✕",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontFamily = Vazirmatn,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(IncomeGreen.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FlowerDecoration(modifier = Modifier.size(15.dp), color = IncomeGreen.copy(alpha = 0.45f))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("درآمد", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontFamily = Vazirmatn)
                                }
                                Text(formatPersianAmount(displayChipIncome), style = MaterialTheme.typography.labelMedium, color = IncomeGreen, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ExpensePurple.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FlowerDecoration(modifier = Modifier.size(15.dp), color = ExpensePurple.copy(alpha = 0.45f))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("هزینه", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontFamily = Vazirmatn)
                                }
                                Text(formatPersianAmount(displayChipExpense), style = MaterialTheme.typography.labelMedium, color = ExpensePurple, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                    TrendBarChart(
                        data = data,
                        selectedIndex = selectedIndex,
                        onIndexSelected = { index -> selectedIndex = if (selectedIndex == index) null else index }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        data.forEachIndexed { index, (label, _, _) ->
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedIndex == index) TextPrimary else TextTertiary,
                                fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = Vazirmatn,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// نمودار میله‌ای «روند ۶ ماه اخیر» با خط‌چین‌های راهنما و گل‌های تزئینی، دقیقاً هماهنگ با
// WeeklyBarChart در HomeScreen — با این تفاوت که تعداد گروه‌ها ثابت (طول data) است، نه ۸ بازه‌ی ثابت
@Composable
private fun TrendBarChart(
    data: List<Triple<String, Long, Long>>,
    selectedIndex: Int?,
    onIndexSelected: (Int) -> Unit
) {
    val density = LocalDensity.current
    val cornerPx = with(density) { 4.dp.toPx() }
    val groupCount = data.size
    val maxValue = data.maxOf { maxOf(it.second, it.third) }.let { if (it <= 0L) 1L else it }

    val incomeFractions: List<Float> = data.mapIndexed { index, triple ->
        val anim = remember { Animatable(0f) }
        val target = triple.second.toFloat() / maxValue.toFloat()
        LaunchedEffect(target, selectedIndex) {
            anim.snapTo(0f)
            delay(index * 35L)
            anim.animateTo(target, animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing))
        }
        anim.value
    }

    val expenseFractions: List<Float> = data.mapIndexed { index, triple ->
        val anim = remember { Animatable(0f) }
        val target = triple.third.toFloat() / maxValue.toFloat()
        LaunchedEffect(target, selectedIndex) {
            anim.snapTo(0f)
            delay(index * 35L)
            anim.animateTo(target, animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing))
        }
        anim.value
    }

    val currentOnIndexSelected by rememberUpdatedState(onIndexSelected)

    Box(modifier = Modifier.fillMaxSize()) {
        // ── گل سبز (بالا-چپ) ──
        WeeklyChartGreenFlower(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 2.dp, start = 2.dp)
                .size(96.dp),
            color = IncomeGreen
        )

        // ── گل بنفش (پایین-راست) ──
        WeeklyChartPurpleFlower(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 0.dp, end = 4.dp)
                .size(90.dp),
            color = ExpensePurple
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(groupCount) {
                    detectTapGestures { offset ->
                        val groupWidth = size.width / groupCount
                        val rawIndex = (offset.x / groupWidth).toInt().coerceIn(0, groupCount - 1)
                        currentOnIndexSelected(rawIndex)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val groupWidth = width / groupCount
            val barWidth = groupWidth * 0.16f
            val pairGap = 2.dp.toPx()
            val totalPairWidth = barWidth * 2 + pairGap

            // ── خط‌چین‌های راهنما (پشت میله‌ها) ──
            val gridLineCount = 4
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f)
            for (i in 1..gridLineCount) {
                val y = height * i / (gridLineCount + 1)
                drawLine(
                    color = DividerColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            if (selectedIndex != null) {
                val groupCenter = selectedIndex * groupWidth + groupWidth / 2
                drawRect(
                    color = TextPrimary.copy(alpha = 0.04f),
                    topLeft = Offset(groupCenter - groupWidth / 2, 0f),
                    size = Size(groupWidth, height)
                )
            }

            data.forEachIndexed { index, _ ->
                val incomeFraction = incomeFractions[index]
                val expenseFraction = expenseFractions[index]
                val barAlpha = if (selectedIndex == null || selectedIndex == index) 1f else 0.35f
                val groupCenter = index * groupWidth + groupWidth / 2
                val startLeft = groupCenter - totalPairWidth / 2

                val incomeHeight = incomeFraction * height
                drawRoundedTopBar(startLeft, height - incomeHeight, barWidth, incomeHeight, cornerPx, BarIncomeGreen.copy(alpha = barAlpha))

                val expenseLeft = startLeft + barWidth + pairGap
                val expenseHeight = expenseFraction * height
                drawRoundedTopBar(expenseLeft, height - expenseHeight, barWidth, expenseHeight, cornerPx, BarExpensePurple.copy(alpha = barAlpha))
            }
        }
    }
}

@Composable
fun PeriodComparisonNote(current: Long, previous: Long, periodType: PeriodType) {
    // مقایسه با «قبلی» فقط برای ماه و سال معنی روشنی دارد؛ برای هفته و روز نمایش داده نمی‌شود
    if (periodType == PeriodType.WEEK || periodType == PeriodType.DAY || previous <= 0L) return
    val diffPercent = (((current - previous).toFloat() / previous.toFloat()) * 100).roundToInt()
    if (diffPercent == 0) return
    val periodLabel = if (periodType == PeriodType.MONTH) "ماه قبل" else "سال قبل"
    val isIncrease = diffPercent > 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (isIncrease) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
            null,
            tint = TextTertiary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "نسبت به $periodLabel ${PersianDateUtils.toPersianDigits(kotlin.math.abs(diffPercent).toString())}٪ ${if (isIncrease) "بیشتر" else "کمتر"}",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontFamily = Vazirmatn
        )
    }
}

@Composable
private fun CategoryBreakdownCard(
    expenseCategories: List<Pair<String, Long>>,
    incomeCategories: List<Pair<String, Long>>,
    totalExpense: Long,
    totalIncome: Long,
    previousExpense: Long,
    previousIncome: Long,
    periodType: PeriodType,
    showAmount: Boolean
) {
    var tab by remember { mutableStateOf(BreakdownTab.EXPENSE) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "نمودار دسته‌بندی",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DividerColor.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    BreakdownTabButton(
                        text = "هزینه",
                        selected = tab == BreakdownTab.EXPENSE,
                        color = ExpensePurple,
                        onClick = { tab = BreakdownTab.EXPENSE }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BreakdownTabButton(
                        text = "درآمد",
                        selected = tab == BreakdownTab.INCOME,
                        color = IncomeGreen,
                        onClick = { tab = BreakdownTab.INCOME }
                    )
                }
            }

            val categories = if (tab == BreakdownTab.EXPENSE) expenseCategories else incomeCategories
            val total = if (tab == BreakdownTab.EXPENSE) totalExpense else totalIncome
            val previous = if (tab == BreakdownTab.EXPENSE) previousExpense else previousIncome
            val palette = if (tab == BreakdownTab.EXPENSE) chartPalette else incomeChartPalette
            val centerLabel = if (tab == BreakdownTab.EXPENSE) "کل هزینه‌ها" else "کل درآمدها"
            val topPrefix = if (tab == BreakdownTab.EXPENSE) "بیشترین هزینه" else "بیشترین درآمد"
            val emptyMessage = if (tab == BreakdownTab.EXPENSE) "هزینه‌ای در این بازه ثبت نشده" else "درآمدی در این بازه ثبت نشده"

            if (categories.isNotEmpty() && total > 0L) {
                val topCategory = categories.first()
                val topPercent = ((topCategory.second.toFloat() / total.toFloat()) * 100).roundToInt()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "$topPrefix: ${topCategory.first} (${PersianDateUtils.toPersianDigits(topPercent.toString())}٪)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontFamily = Vazirmatn
                )
                Spacer(modifier = Modifier.height(4.dp))
                PeriodComparisonNote(total, previous, periodType)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categories.isEmpty() || total <= 0L) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FlowerDecoration(
                            modifier = Modifier.size(52.dp),
                            color = IncomeGreen.copy(alpha = 0.14f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        categories.forEachIndexed { index, pair ->
                            CategoryLegendRow(
                                name = pair.first,
                                amount = pair.second,
                                total = total,
                                color = palette[index % palette.size],
                                showAmount = showAmount
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CategoryDonutChart(categories, palette, total, centerLabel = centerLabel)
                }
            }
        }
    }
}

// «بزرگ‌ترین تراکنش‌های این ماه» — دقیقاً هم‌الگو با «نمودار دسته‌بندی» (سوییچ هزینه/درآمد +
// نمودار دایره‌ای + لیست کنارش)، با این تفاوت که به‌جای جمع‌بندی بر اساس دسته، ۳ تراکنش
// منفرد با بزرگ‌ترین مبلغ در هر تب رتبه‌بندی می‌شوند — برای شناسایی سریع خرج‌ها/دریافت‌های
// غیرعادی، نه فقط جمع کل هر دسته. طلب/بدهی و اقساط شامل نمی‌شود چون بخش جدای «طلب و بدهی»
// گزارش‌ها را دارند
@Composable
private fun TopExpensesCard(transactions: List<Transaction>, showAmount: Boolean) {
    var tab by remember { mutableStateOf(BreakdownTab.EXPENSE) }

    val topExpenses = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.EXPENSE && it.debtId == null && it.transferGroupId == null }
            .sortedByDescending { it.amount }
            .take(3)
            .map { it.title to it.amount }
    }
    val topIncomes = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.INCOME && it.debtId == null && it.transferGroupId == null }
            .sortedByDescending { it.amount }
            .take(3)
            .map { it.title to it.amount }
    }

    if (topExpenses.isEmpty() && topIncomes.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "بزرگ‌ترین تراکنش‌ها",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DividerColor.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    BreakdownTabButton(
                        text = "هزینه",
                        selected = tab == BreakdownTab.EXPENSE,
                        color = ExpensePurple,
                        onClick = { tab = BreakdownTab.EXPENSE }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BreakdownTabButton(
                        text = "درآمد",
                        selected = tab == BreakdownTab.INCOME,
                        color = IncomeGreen,
                        onClick = { tab = BreakdownTab.INCOME }
                    )
                }
            }

            val entries = if (tab == BreakdownTab.EXPENSE) topExpenses else topIncomes
            val total = entries.sumOf { it.second }
            val palette = if (tab == BreakdownTab.EXPENSE) chartPalette else incomeChartPalette
            val centerLabel = if (tab == BreakdownTab.EXPENSE) "مجموع هزینه‌ها" else "مجموع درآمدها"
            val emptyMessage = if (tab == BreakdownTab.EXPENSE) "هزینه‌ای در این بازه ثبت نشده" else "درآمدی در این بازه ثبت نشده"

            Spacer(modifier = Modifier.height(16.dp))

            if (entries.isEmpty() || total <= 0L) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FlowerDecoration(
                            modifier = Modifier.size(52.dp),
                            color = ExpensePurple.copy(alpha = 0.14f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        entries.forEachIndexed { index, pair ->
                            CategoryLegendRow(
                                name = pair.first,
                                amount = pair.second,
                                total = total,
                                color = palette[index % palette.size],
                                showAmount = showAmount
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CategoryDonutChart(entries, palette, total, centerLabel = centerLabel)
                }
            }
        }
    }
}

@Composable
private fun BreakdownTabButton(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) color else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = Vazirmatn
        )
    }
}

@Composable
fun CategoryDonutChart(
    categories: List<Pair<String, Long>>,
    colors: List<Color>,
    total: Long,
    centerLabel: String = "کل هزینه‌ها"
) {
    Box(
        modifier = Modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = size.minDimension * 0.22f
            var startAngle = -90f
            categories.forEachIndexed { index, pair ->
                val amount = pair.second
                val sweep = if (total > 0) (amount.toFloat() / total.toFloat()) * 360f else 0f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt),
                    topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                    size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontFamily = Vazirmatn
            )
            Text(
                formatAmount(total),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = Vazirmatn
            )
            Text(
                "تومان",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontFamily = Vazirmatn
            )
        }
    }
}

@Composable
fun CategoryLegendRow(
    name: String,
    amount: Long,
    total: Long,
    color: Color,
    showAmount: Boolean = false
) {
    val percent = if (total > 0) ((amount.toFloat() / total.toFloat()) * 100).roundToInt() else 0
    val trailingText = if (showAmount) {
        "${PersianDateUtils.toPersianDigits(formatAmount(amount))} تومان"
    } else {
        "${PersianDateUtils.toPersianDigits(percent.toString())}٪"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontFamily = Vazirmatn,
            modifier = Modifier.weight(1f)
        )
        Text(
            trailingText,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontFamily = Vazirmatn
        )
    }
}

@Composable
fun IncomeExpenseComparison(income: Long, expense: Long, showAmount: Boolean = false) {
    Column {
        Text(
            "مقایسه درآمد و هزینه",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontFamily = Vazirmatn
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (income <= 0L && expense <= 0L) {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FlowerDecoration(
                        modifier = Modifier.size(52.dp),
                        color = ExpensePurple.copy(alpha = 0.14f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "داده‌ای برای این بازه نیست",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    CategoryLegendRow("درآمد", income, income + expense, IncomeGreen, showAmount)
                    CategoryLegendRow("هزینه", expense, income + expense, ExpensePurple, showAmount)
                }
                Spacer(modifier = Modifier.width(16.dp))
                ComparisonDonutChart(income, expense)
            }
        }
    }
}

@Composable
fun ComparisonDonutChart(income: Long, expense: Long) {
    val total = income + expense
    val net = income - expense
    Box(
        modifier = Modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = size.minDimension * 0.22f
            var startAngle = -90f
            val slices = listOf(income to IncomeGreen, expense to ExpensePurple)
            slices.forEach { pair ->
                val amount = pair.first
                val color = pair.second
                val sweep = if (total > 0) (amount.toFloat() / total.toFloat()) * 360f else 0f
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt),
                    topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                    size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "خالص",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontFamily = Vazirmatn
            )
            Text(
                formatPersianAmount(net),
                style = MaterialTheme.typography.bodyMedium,
                color = if (net >= 0) IncomeGreen else ExpensePurple,
                fontWeight = FontWeight.Bold,
                fontFamily = Vazirmatn
            )
            Text(
                "تومان",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontFamily = Vazirmatn
            )
        }
    }
}

data class AccountExtreme(
    val category: String,
    val title: String,
    val amount: Long
)

data class AccountMinMaxEntry(
    val accountName: String,
    val color: Color,
    val maxExpense: AccountExtreme?,
    val minExpense: AccountExtreme?,
    val maxIncome: AccountExtreme?,
    val minIncome: AccountExtreme?
)

private fun accountMinMaxBreakdown(
    transactions: List<Transaction>,
    accounts: List<Account>
): List<AccountMinMaxEntry> {
    fun extreme(list: List<Transaction>, useMax: Boolean): AccountExtreme? {
        val picked = if (useMax) list.maxByOrNull { it.amount } else list.minByOrNull { it.amount }
        return picked?.let { AccountExtreme(it.category, it.title, it.amount) }
    }

    return accounts.map { account ->
        val accountTx = transactions.filter { it.accountId == account.id }
        val expenses = accountTx.filter { it.type == TransactionType.EXPENSE }
        val incomes = accountTx.filter { it.type == TransactionType.INCOME }

        AccountMinMaxEntry(
            accountName = account.name,
            color = cardColorPalette[account.colorIndex % cardColorPalette.size],
            maxExpense = extreme(expenses, useMax = true),
            minExpense = if (expenses.size > 1) extreme(expenses, useMax = false) else null,
            maxIncome = extreme(incomes, useMax = true),
            minIncome = if (incomes.size > 1) extreme(incomes, useMax = false) else null
        )
    }
}

private fun accountAmountBreakdown(
    transactions: List<Transaction>,
    type: TransactionType,
    accounts: List<Account>
): List<Triple<String, Long, Color>> {
    val grouped = transactions
        .filter { it.type == type && it.accountId != null }
        .groupBy { it.accountId }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
    return accounts
        .mapNotNull { account ->
            grouped[account.id]?.let { amount ->
                Triple(account.name, amount, cardColorPalette[account.colorIndex % cardColorPalette.size])
            }
        }
        .sortedByDescending { it.second }
}

// مبلغ هر کارت در دوره‌ی قبل، بر اساس نام کارت (نه id) — چون AccountAmountBreakdownCard
// با نام کار می‌کند، برای مقایسه‌ی «نسبت به ماه قبل» هم نیاز به همین کلید داریم
private fun accountAmountByName(
    transactions: List<Transaction>,
    type: TransactionType,
    accounts: List<Account>
): Map<String, Long> {
    val grouped = transactions
        .filter { it.type == type && it.accountId != null }
        .groupBy { it.accountId }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
    return accounts.mapNotNull { account ->
        grouped[account.id]?.let { amount -> account.name to amount }
    }.toMap()
}

@Composable
private fun AccountAmountBreakdownCard(
    expenseBreakdown: List<Triple<String, Long, Color>>,
    incomeBreakdown: List<Triple<String, Long, Color>>,
    previousExpenseByName: Map<String, Long>,
    previousIncomeByName: Map<String, Long>,
    periodType: PeriodType,
    showAmount: Boolean
) {
    var tab by remember { mutableStateOf(BreakdownTab.EXPENSE) }

    val breakdown = if (tab == BreakdownTab.EXPENSE) expenseBreakdown else incomeBreakdown
    val previousByName = if (tab == BreakdownTab.EXPENSE) previousExpenseByName else previousIncomeByName
    val total = breakdown.sumOf { it.second }
    val centerLabel = if (tab == BreakdownTab.EXPENSE) "کل هزینه‌ها" else "کل درآمدها"
    val emptyMessage = if (tab == BreakdownTab.EXPENSE) "هزینه‌ای برای کارتی ثبت نشده" else "درآمدی برای کارتی ثبت نشده"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "مبلغ بر اساس کارت",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DividerColor.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    BreakdownTabButton(
                        text = "هزینه",
                        selected = tab == BreakdownTab.EXPENSE,
                        color = ExpensePurple,
                        onClick = { tab = BreakdownTab.EXPENSE }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BreakdownTabButton(
                        text = "درآمد",
                        selected = tab == BreakdownTab.INCOME,
                        color = IncomeGreen,
                        onClick = { tab = BreakdownTab.INCOME }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (breakdown.isEmpty() || total <= 0L) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FlowerDecoration(
                            modifier = Modifier.size(52.dp),
                            color = IncomeGreen.copy(alpha = 0.14f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            } else {
                val categories = breakdown.map { it.first to it.second }
                val colors = breakdown.map { it.third }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        breakdown.forEach { (name, amount, color) ->
                            CategoryLegendRow(name = name, amount = amount, total = total, color = color, showAmount = showAmount)
                            val previousAmount = previousByName[name]
                            if (previousAmount != null && previousAmount > 0L) {
                                Spacer(modifier = Modifier.height(2.dp))
                                PeriodComparisonNote(amount, previousAmount, periodType)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CategoryDonutChart(categories, colors, total, centerLabel = centerLabel)
                }
            }
        }
    }
}

@Composable
fun AccountMinMaxCard(entries: List<AccountMinMaxEntry>, showAmount: Boolean) {
    var tab by remember { mutableStateOf(BreakdownTab.EXPENSE) }
    var selectedIndex by remember { mutableStateOf(0) }

    val accentColor = if (tab == BreakdownTab.EXPENSE) ExpensePurple else IncomeGreen
    val lightAccentColor = accentColor.copy(alpha = 0.4f)

    val visibleEntries = remember(entries, tab) {
        entries.mapNotNull { entry ->
            val max = if (tab == BreakdownTab.EXPENSE) entry.maxExpense else entry.maxIncome
            val min = if (tab == BreakdownTab.EXPENSE) entry.minExpense else entry.minIncome
            if (max == null) null else Triple(entry, min, max)
        }
    }

    LaunchedEffect(visibleEntries) {
        if (selectedIndex >= visibleEntries.size) selectedIndex = 0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "بیشترین و کمترین مبلغ",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DividerColor.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    BreakdownTabButton(
                        text = "هزینه",
                        selected = tab == BreakdownTab.EXPENSE,
                        color = ExpensePurple,
                        onClick = { tab = BreakdownTab.EXPENSE }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BreakdownTabButton(
                        text = "درآمد",
                        selected = tab == BreakdownTab.INCOME,
                        color = IncomeGreen,
                        onClick = { tab = BreakdownTab.INCOME }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (visibleEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FlowerDecoration(
                            modifier = Modifier.size(52.dp),
                            color = ExpensePurple.copy(alpha = 0.14f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (tab == BreakdownTab.EXPENSE) "هزینه‌ای ثبت نشده" else "درآمدی ثبت نشده",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            } else {
                val hasMultiple = visibleEntries.size > 1
                val (currentEntry, _, _) = visibleEntries[selectedIndex]

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasMultiple) DividerColor.copy(alpha = 0.45f)
                                else DividerColor.copy(alpha = 0.20f)
                            )
                            .clickable(enabled = hasMultiple) {
                                selectedIndex =
                                    (selectedIndex - 1 + visibleEntries.size) % visibleEntries.size
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "کارت قبلی",
                            tint = if (hasMultiple) TextTertiary else TextTertiary.copy(alpha = 0.30f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(currentEntry.color)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        currentEntry.accountName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasMultiple) DividerColor.copy(alpha = 0.45f)
                                else DividerColor.copy(alpha = 0.20f)
                            )
                            .clickable(enabled = hasMultiple) {
                                selectedIndex = (selectedIndex + 1) % visibleEntries.size
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = "کارت بعدی",
                            tint = if (hasMultiple) TextTertiary else TextTertiary.copy(alpha = 0.30f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                run {
                    val (_, min, max) = visibleEntries[selectedIndex]

                    if (min == null || min.amount == max.amount) {
                        val label = "${max.category} - ${max.title}"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                CategoryLegendRow(
                                    name = label,
                                    amount = max.amount,
                                    total = max.amount,
                                    color = accentColor,
                                    showAmount = showAmount
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            CategoryDonutChart(
                                categories = listOf(label to max.amount),
                                colors = listOf(accentColor),
                                total = max.amount,
                                centerLabel = "بیشترین"
                            )
                        }
                    } else {
                        val maxLabel = "${max.category} - ${max.title}"
                        val minLabel = "${min.category} - ${min.title}"
                        val total = max.amount + min.amount
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                CategoryLegendRow(
                                    name = maxLabel,
                                    amount = max.amount,
                                    total = total,
                                    color = accentColor,
                                    showAmount = showAmount
                                )
                                CategoryLegendRow(
                                    name = minLabel,
                                    amount = min.amount,
                                    total = total,
                                    color = lightAccentColor,
                                    showAmount = showAmount
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            CategoryDonutChart(
                                categories = listOf(maxLabel to max.amount, minLabel to min.amount),
                                colors = listOf(accentColor, lightAccentColor),
                                total = total,
                                centerLabel = "مجموع"
                            )
                        }
                    }
                }
            }
        }
    }
}
