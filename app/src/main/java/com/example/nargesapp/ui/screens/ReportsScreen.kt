package com.example.nargesapp.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.nargesapp.data.model.Account
import com.example.nargesapp.data.model.Debt
import com.example.nargesapp.data.model.DebtType
import com.example.nargesapp.data.model.Transaction
import com.example.nargesapp.data.model.TransactionType
import com.example.nargesapp.data.repository.DebtRepository
import com.example.nargesapp.ui.theme.*
import com.example.nargesapp.ui.utils.PersianDateUtils
import com.example.nargesapp.ui.utils.ReportPreferences
import com.example.nargesapp.ui.viewmodel.TransactionViewModel
import kotlin.math.roundToInt

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
            parts.getOrNull(0)?.toIntOrNull() == y && parts.getOrNull(1)?.toIntOrNull() == m
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
    val accounts by com.example.nargesapp.data.repository.AccountRepository.accounts.collectAsStateWithLifecycle()

    val showAmountInReports = remember { mutableStateOf(ReportPreferences.isAmountModeEnabled(context)) }

    var periodType by remember { mutableStateOf(PeriodType.MONTH) }
    val currentYearMonth = remember {
        val parts = PersianDateUtils.getCurrentPersianDate().split("/")
        (parts.getOrNull(0)?.toIntOrNull() ?: 1404) to (parts.getOrNull(1)?.toIntOrNull() ?: 1)
    }
    var selectedYear by remember { mutableStateOf(currentYearMonth.first) }
    var selectedMonth by remember { mutableStateOf(currentYearMonth.second) }

    val periodFiltered = filterByPeriod(transactions, periodType, selectedYear, selectedMonth)
    val periodIncome = periodFiltered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val periodExpense = periodFiltered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    val previousPeriodFiltered = when (periodType) {
        PeriodType.MONTH -> {
            val prevYear = if (selectedMonth == 1) selectedYear - 1 else selectedYear
            val prevMonth = if (selectedMonth == 1) 12 else selectedMonth - 1
            filterByPeriod(transactions, PeriodType.MONTH, prevYear, prevMonth)
        }
        PeriodType.YEAR -> filterByPeriod(transactions, PeriodType.YEAR, selectedYear - 1, selectedMonth)
        PeriodType.WEEK -> emptyList()
PeriodType.DAY -> emptyList()
    }
    val previousIncome = previousPeriodFiltered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val previousExpense = previousPeriodFiltered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    val expenseByCategory = remember(periodFiltered) {
        val grouped = periodFiltered
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        topCategoriesWithOther(grouped)
    }
    val incomeByCategory = remember(periodFiltered) {
        val grouped = periodFiltered
            .filter { it.type == TransactionType.INCOME }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        topCategoriesWithOther(grouped)
    }

    val expenseByAccount = remember(periodFiltered, accounts) {
        accountAmountBreakdown(periodFiltered, TransactionType.EXPENSE, accounts)
    }
    val incomeByAccount = remember(periodFiltered, accounts) {
        accountAmountBreakdown(periodFiltered, TransactionType.INCOME, accounts)
    }
    val accountExtremes = remember(periodFiltered, accounts) {
        accountMinMaxBreakdown(periodFiltered, accounts)
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
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        PeriodSummaryCard(periodIncome, periodExpense, periodType)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        MonthlyTrendCard(trendData)
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
                            showAmount = showAmountInReports.value
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
    AccountMinMaxCard(accountExtremes, showAmount = showAmountInReports.value)
    Spacer(modifier = Modifier.height(16.dp))
    DebtBreakdownCard(showAmount = showAmountInReports.value)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "برای تغییر درصد به مبلغ از تنظیمات اقدام کنید",
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
                    Text(
                        emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn
                    )
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

@Composable
private fun MonthlyTrendCard(data: List<Triple<String, Long, Long>>) {
    val density = LocalDensity.current
    val cornerPx = with(density) { 4.dp.toPx() }
    val hasData = data.any { it.second > 0 || it.third > 0 }

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
                    Text(
                        "داده‌ای برای این بازه نیست",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn
                    )
                }
            } else {
                val maxValue = data.maxOf { maxOf(it.second, it.third) }.let { if (it <= 0L) 1L else it }

                Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val groupCount = data.size
                        val groupWidth = width / groupCount
                        val barWidth = groupWidth * 0.16f
                        val pairGap = with(density) { 2.dp.toPx() }
                        val totalPairWidth = barWidth * 2 + pairGap

                        data.forEachIndexed { index, (_, income, expense) ->
                            val groupCenter = index * groupWidth + groupWidth / 2
                            val startLeft = groupCenter - totalPairWidth / 2

                            val incomeHeight = if (maxValue > 0) (income.toFloat() / maxValue.toFloat()) * height else 0f
                            drawRoundedTopBar(
                                startLeft, height - incomeHeight, barWidth, incomeHeight, cornerPx, BarIncomeGreen
                            )

                            val expenseLeft = startLeft + barWidth + pairGap
                            val expenseHeight = if (maxValue > 0) (expense.toFloat() / maxValue.toFloat()) * height else 0f
                            drawRoundedTopBar(
                                expenseLeft, height - expenseHeight, barWidth, expenseHeight, cornerPx, BarExpensePurple
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        data.forEach { (label, _, _) ->
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                fontWeight = FontWeight.Medium,
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

@Composable
fun PeriodComparisonNote(current: Long, previous: Long, periodType: PeriodType) {
    if (periodType == PeriodType.WEEK || previous <= 0L) return
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
                    Text(
                        emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn
                    )
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
                Text(
                    "داده‌ای برای این بازه نیست",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontFamily = Vazirmatn
                )
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

@Composable
private fun AccountAmountBreakdownCard(
    expenseBreakdown: List<Triple<String, Long, Color>>,
    incomeBreakdown: List<Triple<String, Long, Color>>,
    showAmount: Boolean
) {
    var tab by remember { mutableStateOf(BreakdownTab.EXPENSE) }

    val breakdown = if (tab == BreakdownTab.EXPENSE) expenseBreakdown else incomeBreakdown
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
                    Text(
                        emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn
                    )
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
                    Text(
                        if (tab == BreakdownTab.EXPENSE) "هزینه‌ای ثبت نشده" else "درآمدی ثبت نشده",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn
                    )
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
