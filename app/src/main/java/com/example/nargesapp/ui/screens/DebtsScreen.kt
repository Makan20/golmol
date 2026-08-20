package com.example.nargesapp.ui.screens

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
import androidx.compose.material.icons.outlined.Add
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
import com.example.nargesapp.ui.theme.DividerColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.nargesapp.data.repository.DebtRepository
import com.example.nargesapp.ui.theme.BackgroundLight
import com.example.nargesapp.ui.theme.CardWhite
import com.example.nargesapp.ui.theme.ExpensePurple
import com.example.nargesapp.ui.theme.IncomeGreen
import com.example.nargesapp.ui.theme.PrimaryGreen
import com.example.nargesapp.ui.theme.TextPrimary
import com.example.nargesapp.ui.theme.TextSecondary
import com.example.nargesapp.ui.theme.TextTertiary
import com.example.nargesapp.ui.theme.Vazirmatn
import com.example.nargesapp.ui.theme.WarningAmber
import com.example.nargesapp.ui.utils.PersianDateUtils
import java.text.NumberFormat
import java.util.Locale

enum class DebtType { RECEIVABLE, PAYABLE }
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
    val totalInstallments: Int = 0
) {
    val isLoan: Boolean get() = loanGroupId != null
}

private val PrimaryFilterColor = Color(0xFF6B8E5A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(navController: NavController) {
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
            type = if (first.type.name == "RECEIVABLE") DebtType.RECEIVABLE else DebtType.PAYABLE,
            originalAmount = sorted.sumOf { it.amount },
            remainingAmount = sorted.sumOf { it.remainingAmount },
            dueDate = nextDueDate,
            isSettled = allSettled,
            loanGroupId = groupId,
            paidInstallments = sorted.count { it.isSettled },
            totalInstallments = sorted.size
        )
    }

    val debts = loanItems + regularDebts.map { debt ->
        DebtUiItem(
            id = debt.id,
            personName = debt.personName,
            type = if (debt.type.name == "RECEIVABLE") {
                DebtType.RECEIVABLE
            } else {
                DebtType.PAYABLE
            },
            originalAmount = debt.amount,
            remainingAmount = debt.remainingAmount,
            dueDate = debt.dueDate.takeIf { it.isNotBlank() },
            isSettled = debt.isSettled
        )
    }

    var selectedFilter by remember { mutableStateOf(DebtFilter.ALL) }
    val listState = rememberLazyListState()
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val receivables = debts.filter {
        it.type == DebtType.RECEIVABLE && !it.isSettled
    }
    val payables = debts.filter {
        it.type == DebtType.PAYABLE && !it.isSettled
    }
    val totalReceivable = receivables.sumOf { it.remainingAmount }
    val totalPayable = payables.sumOf { it.remainingAmount }

    val filteredDebts = when (selectedFilter) {
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
                        text = "طلب و بدهی",
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

                    if (showSearch) {
                        item {
                            DebtSearchField(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    item {
                        DebtSummary(
                            totalReceivable = totalReceivable,
                            totalPayable = totalPayable
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        DebtFilterRow(
                            selected = selectedFilter,
                            onSelect = { selectedFilter = it }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

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
                        AddDebtButton(
                            onClick = { navController.navigate("add_debt") }
                        )
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
private fun DebtSummary(
    totalReceivable: Long,
    totalPayable: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryAmountCard(
                    title = "طلب من",
                    amount = totalReceivable,
                    color = IncomeGreen,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                SummaryAmountCard(
                    title = "بدهی من",
                    amount = totalPayable,
                    color = ExpensePurple,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = ExpensePurple,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val net = totalReceivable - totalPayable

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "خالص طلب و بدهی: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontFamily = Vazirmatn
                )

                Text(
    text = debtAmount(net),
    style = MaterialTheme.typography.bodySmall,
    color = if (net >= 0) IncomeGreen else ExpensePurple,
    fontWeight = FontWeight.Bold,
    fontFamily = Vazirmatn
)
            }
        }
    }
}

@Composable
private fun SummaryAmountCard(
    title: String,
    amount: Long,
    color: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.09f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontFamily = Vazirmatn
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = debtAmount(amount),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            fontFamily = Vazirmatn
        )
    }
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
                    Modifier.border(2.dp, WarningAmber, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (debt.isLoan) Icons.Outlined.CreditCard else Icons.Outlined.Person,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

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
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = Vazirmatn
                    )
                }

                Text(
                    text = debtAmount(debt.remainingAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn,
                    textAlign = TextAlign.Left
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (debt.isLoan) {
                        "${PersianDateUtils.toPersianDigits(debt.paidInstallments.toString())} از ${PersianDateUtils.toPersianDigits(debt.totalInstallments.toString())} قسط پرداخت شده"
                    } else {
                        "${PersianDateUtils.toPersianDigits((progress * 100).toInt().toString())}٪ تسویه شده"
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
            }
        }
    }
}

@Composable
private fun AddDebtButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PrimaryFilterColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "ثبت طلب یا بدهی",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = Vazirmatn
            )
        }
    }
}

private fun debtAmount(amount: Long): String {
    val absolute = NumberFormat.getInstance(Locale.US)
        .format(kotlin.math.abs(amount))

    val sign = if (amount < 0) "−" else ""

    return "\u2066$sign${PersianDateUtils.toPersianDigits(absolute)}\u2069 تومان"
}