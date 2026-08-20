package com.example.nargesapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.nargesapp.data.model.Transaction
import com.example.nargesapp.data.model.TransactionType
import com.example.nargesapp.ui.theme.*
import com.example.nargesapp.ui.utils.PersianDateUtils
import com.example.nargesapp.ui.viewmodel.TransactionViewModel

enum class PeriodType { WEEK, MONTH, YEAR }

fun filterByPeriod(transactions: List<Transaction>, periodType: PeriodType, year: Int, month: Int): List<Transaction> {
    return when (periodType) {
        PeriodType.WEEK -> {
            val weekDates = PersianDateUtils.getCurrentWeekDates()
            transactions.filter { it.date in weekDates }
        }
        PeriodType.MONTH -> transactions.filter { t ->
            val parts = t.date.split("/")
            parts.getOrNull(0)?.toIntOrNull() == year && parts.getOrNull(1)?.toIntOrNull() == month
        }
        PeriodType.YEAR -> transactions.filter { t ->
            t.date.split("/").getOrNull(0)?.toIntOrNull() == year
        }
    }
}

enum class SortOrder { ASCENDING, DESCENDING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(navController: NavController, viewModel: TransactionViewModel) {
    val listState = rememberLazyListState()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by com.example.nargesapp.data.repository.AccountRepository.accounts.collectAsStateWithLifecycle()
    var filterType by remember { mutableStateOf<TransactionType?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf<SortOrder?>(null) }

    var periodType by remember { mutableStateOf(PeriodType.MONTH) }
    val currentYearMonth = remember {
        val parts = PersianDateUtils.getCurrentPersianDate().split("/")
        (parts.getOrNull(0)?.toIntOrNull() ?: 1404) to (parts.getOrNull(1)?.toIntOrNull() ?: 1)
    }
    var selectedYear by remember { mutableStateOf(currentYearMonth.first) }
    var selectedMonth by remember { mutableStateOf(currentYearMonth.second) }

    val periodFiltered = filterByPeriod(transactions, periodType, selectedYear, selectedMonth)

    val filtered = when {
        searchQuery.isNotBlank() -> {
            transactions.filter { t ->
                val matchesSearch = t.title.contains(searchQuery, ignoreCase = true) ||
                    t.category.contains(searchQuery, ignoreCase = true) ||
                    (accounts.find { it.id == t.accountId }?.name?.contains(searchQuery, ignoreCase = true) == true)
                matchesSearch && (filterType == null || t.type == filterType)
            }
        }
        filterType != null -> periodFiltered.filter { it.type == filterType }
        else -> periodFiltered
    }.let { list ->
        when (sortOrder) {
            SortOrder.ASCENDING -> list.sortedBy { it.amount }
            SortOrder.DESCENDING -> list.sortedBy { it.amount }
            null -> list
        }
    }

    val emptyMessage = when {
        searchQuery.isNotBlank() -> "نتیجه‌ای پیدا نشد"
        transactions.isEmpty() -> "هنوز تراکنشی ثبت نشده"
        else -> "تراکنشی در این بازه نیست"
    }

    val grouped = filtered.groupBy { it.date }
    val today = PersianDateUtils.getCurrentPersianDate()
    val yesterday = PersianDateUtils.getYesterdayPersianDate()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "تراکنش‌ها",
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
                    IconButton(
                        onClick = {
                            showSearch = !showSearch
                            if (!showSearch) {
                                searchQuery = ""
                                sortOrder = null
                            }
                        }
                    ) {
                        Icon(
                            if (showSearch) Icons.Outlined.Close else Icons.Outlined.Search,
                            if (showSearch) "بستن جستجو" else "جستجو",
                            tint = if (showSearch) PrimaryGreen else TextPrimary
                        )
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
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 60.dp, start = 4.dp).size(44.dp),
                color = ExpensePurple.copy(alpha = 0.08f)
            )
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    if (showSearch) {
                        SearchField(
                            query = searchQuery,
                            onQueryChange = { newQuery ->
                                if (searchQuery.isBlank() && newQuery.isNotBlank()) {
                                    filterType = null
                                }
                                if (newQuery.isBlank()) {
                                    sortOrder = null
                                }
                                searchQuery = newQuery
                            },
                            sortOrder = sortOrder,
                            onSortOrderChange = { newSort ->
                                sortOrder = newSort
                                when (newSort) {
                                    SortOrder.ASCENDING -> filterType = TransactionType.INCOME
                                    SortOrder.DESCENDING -> filterType = TransactionType.EXPENSE
                                    null -> Unit
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Filter Chips
                    FilterChips(
                        selectedType = filterType,
                        onTypeSelected = { newType ->
                            filterType = newType
                            val mismatch = when (sortOrder) {
                                SortOrder.ASCENDING -> newType != TransactionType.INCOME
                                SortOrder.DESCENDING -> newType != TransactionType.EXPENSE
                                null -> false
                            }
                            if (mismatch) sortOrder = null
                        },
                        periodType = periodType,
                        onPeriodSelected = { periodType = it },
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth,
                        onPreviousMonth = {
                            if (selectedMonth == 1) {
                                selectedMonth = 12
                                selectedYear -= 1
                            } else {
                                selectedMonth -= 1
                            }
                        },
                        onNextMonth = {
                            if (selectedMonth == 12) {
                                selectedMonth = 1
                                selectedYear += 1
                            } else {
                                selectedMonth += 1
                            }
                        },
                        onPreviousYear = { selectedYear -= 1 },
                        onNextYear = { selectedYear += 1 }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
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
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 130.dp)
                        ) {
                            if (sortOrder != null) {
                                // Sort is active: show one continuous list ordered by amount,
                                // bypassing date-based grouping so the sort is visible across all results.
                                item(key = "sorted_flat_list") {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            filtered.forEachIndexed { index, transaction ->
                                                SwipeableTransactionItem(
                                                    transactionId = transaction.id,
                                                    title = transaction.title,
                                                    date = transaction.date,
                                                    note = transaction.note,
                                                    amount = transaction.amount,
                                                    type = transaction.type,
                                                    category = transaction.category,
                                                    onDelete = { viewModel.deleteTransaction(transaction) },
                                                    onEdit = { navController.navigate("edit_transaction/${transaction.id}") },
                                                    accountName = accounts.find { it.id == transaction.accountId }?.name
                                                )
                                                if (index < filtered.size - 1) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(vertical = 8.dp),
                                                        color = DividerColor.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Sort grouped entries: today first, then yesterday, then others
                                val sortedEntries = grouped.entries.sortedWith(
                                    compareBy<Map.Entry<String, List<Transaction>>> { (date, _) ->
                                        when (date) {
                                            today -> 0
                                            yesterday -> 1
                                            else -> 2
                                        }
                                    }.thenByDescending { (date, _) -> date }
                                )

                                sortedEntries.forEach { (date, items) ->
                                    val dayLabel = when (date) {
                                        today -> "امروز"
                                        yesterday -> "دیروز"
                                        else -> ""
                                    }

                                    item(key = "header_$date") {
                                        DateSectionHeader(date, dayLabel, items)
                                    }
                                    item(key = "card_$date") {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = CardWhite),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                items.forEachIndexed { index, transaction ->
                                                    SwipeableTransactionItem(
                                                        transactionId = transaction.id,
                                                        title = transaction.title,
                                                        date = transaction.date,
                                                        note = transaction.note,
                                                        amount = transaction.amount,
                                                        type = transaction.type,
                                                        category = transaction.category,
                                                        onDelete = { viewModel.deleteTransaction(transaction) },
                                                        onEdit = { navController.navigate("edit_transaction/${transaction.id}") },
                                                        accountName = accounts.find { it.id == transaction.accountId }?.name
                                                    )
                                                    if (index < items.size - 1) {
                                                        HorizontalDivider(
                                                            modifier = Modifier.padding(vertical = 8.dp),
                                                            color = DividerColor.copy(alpha = 0.5f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "برای ویرایش به راست و برای حذف به چپ بکشید",
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
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 16.dp)) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !listState.canScrollForward,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    BottomNavBar(navController, currentRoute = "transactions", isScrolling = listState.isScrollInProgress)
                }
            }
        }
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    sortOrder: SortOrder?,
    onSortOrderChange: (SortOrder?) -> Unit
) {
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
                "جستجو در عنوان، دسته‌بندی یا کارت...",
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    SortDotButton(
                        icon = Icons.Outlined.ArrowUpward,
                        contentDescription = "مرتب‌سازی از کم به زیاد",
                        color = IncomeGreen,
                        active = sortOrder == SortOrder.ASCENDING,
                        onClick = {
                            onSortOrderChange(if (sortOrder == SortOrder.ASCENDING) null else SortOrder.ASCENDING)
                        }
                    )
                    SortDotButton(
                        icon = Icons.Outlined.ArrowDownward,
                        contentDescription = "مرتب‌سازی از زیاد به کم",
                        color = ExpensePurple,
                        active = sortOrder == SortOrder.DESCENDING,
                        onClick = {
                            onSortOrderChange(if (sortOrder == SortOrder.DESCENDING) null else SortOrder.DESCENDING)
                        }
                    )
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, "پاک کردن", tint = TextTertiary)
                    }
                }
            }
        }
    )
}

@Composable
fun SortDotButton(
    icon: ImageVector,
    contentDescription: String,
    color: Color,
    active: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (active) color else color.copy(alpha = 0.12f),
        label = "sortDotBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) Color.White else color,
        label = "sortDotIcon"
    )

    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = iconTint, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun FilterChips(
    selectedType: TransactionType?,
    onTypeSelected: (TransactionType?) -> Unit,
    periodType: PeriodType,
    onPeriodSelected: (PeriodType) -> Unit,
    selectedYear: Int,
    selectedMonth: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit
) {
    val accentColor = when (selectedType) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpensePurple
        null -> PrimaryGreen
    }

    Column {
        TypeFilterRow(selectedType = selectedType, onTypeSelected = onTypeSelected)

        Spacer(modifier = Modifier.height(14.dp))

        PeriodFilterRow(
            periodType = periodType,
            accentColor = accentColor,
            onPeriodSelected = onPeriodSelected
        )

        if (periodType == PeriodType.MONTH) {
            Spacer(modifier = Modifier.height(10.dp))
            MonthNavigator(
                year = selectedYear,
                month = selectedMonth,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth
            )
        } else if (periodType == PeriodType.YEAR) {
            Spacer(modifier = Modifier.height(10.dp))
            YearNavigator(
                year = selectedYear,
                onPrevious = onPreviousYear,
                onNext = onNextYear
            )
        }
    }
}

@Composable
fun TypeFilterRow(
    selectedType: TransactionType?,
    onTypeSelected: (TransactionType?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TypeFilterItem(
            label = "درآمدها",
            icon = Icons.AutoMirrored.Outlined.TrendingUp,
            color = IncomeGreen,
            selected = selectedType == TransactionType.INCOME,
            onClick = { onTypeSelected(TransactionType.INCOME) }
        )
        TypeFilterItem(
            label = "هزینه‌ها",
            icon = Icons.AutoMirrored.Outlined.TrendingDown,
            color = ExpensePurple,
            selected = selectedType == TransactionType.EXPENSE,
            onClick = { onTypeSelected(TransactionType.EXPENSE) }
        )
        TypeFilterItem(
            label = "همه",
            icon = Icons.AutoMirrored.Outlined.List,
            color = PrimaryGreen,
            selected = selectedType == null,
            onClick = { onTypeSelected(null) }
        )
    }
}

@Composable
fun PeriodFilterRow(
    periodType: PeriodType,
    accentColor: Color,
    onPeriodSelected: (PeriodType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TypeFilterItem(
            label = "هفته",
            icon = Icons.Outlined.DateRange,
            color = accentColor,
            selected = periodType == PeriodType.WEEK,
            onClick = { onPeriodSelected(PeriodType.WEEK) }
        )
        TypeFilterItem(
            label = "ماه",
            icon = Icons.Outlined.CalendarToday,
            color = accentColor,
            selected = periodType == PeriodType.MONTH,
            onClick = { onPeriodSelected(PeriodType.MONTH) }
        )
        TypeFilterItem(
            label = "سال",
            icon = Icons.Outlined.Event,
            color = accentColor,
            selected = periodType == PeriodType.YEAR,
            onClick = { onPeriodSelected(PeriodType.YEAR) }
        )
    }
}

@Composable
fun MonthNavigator(
    year: Int,
    month: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Declared first -> right side in RTL: go to the next month
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, "ماه بعد", tint = TextSecondary)
        }
        Text(
            "${PersianDateUtils.persianMonthNames.getOrElse(month - 1) { "" }} ${PersianDateUtils.toPersianDigits(year.toString())}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontFamily = Vazirmatn
        )
        // Declared last -> left side in RTL: go to the previous month
        IconButton(onClick = onPrevious) {
            Icon(Icons.Outlined.ChevronLeft, "ماه قبل", tint = TextSecondary)
        }
    }
}

@Composable
fun YearNavigator(
    year: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Declared first -> right side in RTL: go to the next year
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, "سال بعد", tint = TextSecondary)
        }
        Text(
            PersianDateUtils.toPersianDigits(year.toString()),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontFamily = Vazirmatn
        )
        // Declared last -> left side in RTL: go to the previous year
        IconButton(onClick = onPrevious) {
            Icon(Icons.Outlined.ChevronLeft, "سال قبل", tint = TextSecondary)
        }
    }
}

@Composable
fun TypeFilterItem(
    label: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) color else color.copy(alpha = 0.12f),
        label = "typeFilterBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) Color.White else color,
        label = "typeFilterIcon"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) color else TextSecondary,
            fontFamily = Vazirmatn,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DateSectionHeader(date: String, dayLabel: String, transactions: List<Transaction>) {
    val dayTotal = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } -
                   transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // RIGHT side: dayLabel (امروز / دیروز)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dayLabel.isNotEmpty()) {
                Text(
                    dayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )
                Spacer(modifier = Modifier.width(4.dp))
                FlowerDecoration(
                    modifier = Modifier.size(16.dp),
                    color = if (dayLabel == "امروز") PrimaryGreen.copy(alpha = 0.3f) else ExpensePurple.copy(alpha = 0.2f)
                )
            }
        }

        // LEFT side: Date (persian digits)
        Text(
            PersianDateUtils.toPersianDigits(date),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontFamily = Vazirmatn
        )
    }
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) IncomeGreen else ExpensePurple
    val sign = if (isIncome) "+" else ""
    val minusSuffix = if (isIncome) "" else " -"
    val iconBg = amountColor.copy(alpha = 0.1f)
    val categoryIcon = getCategoryIcon(transaction.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Right side: Icon + Title + Category
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    categoryIcon,
                    contentDescription = transaction.category,
                    tint = amountColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    transaction.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )
                Text(
                    transaction.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontFamily = Vazirmatn
                )
            }
        }

        // Left side: Amount (sign before, minus after for expense)
        Text(
            "${sign}${formatPersianAmount(transaction.amount)}${minusSuffix}",
            style = MaterialTheme.typography.bodyMedium,
            color = amountColor,
            fontWeight = FontWeight.Bold,
            fontFamily = Vazirmatn,
            textAlign = TextAlign.Left,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
