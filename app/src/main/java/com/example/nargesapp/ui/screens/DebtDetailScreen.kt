package com.example.nargesapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.nargesapp.data.model.Account
import com.example.nargesapp.data.model.Debt
import com.example.nargesapp.data.model.DebtPayment
import com.example.nargesapp.data.model.DebtType
import com.example.nargesapp.data.model.Transaction
import com.example.nargesapp.data.model.TransactionType
import com.example.nargesapp.data.repository.AccountRepository
import com.example.nargesapp.data.repository.DebtPaymentRepository
import com.example.nargesapp.data.repository.DebtRepository
import com.example.nargesapp.data.repository.TransactionRepository
import com.example.nargesapp.ui.theme.*
import com.example.nargesapp.ui.utils.PersianDateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private fun parseSettleAmount(value: String): Long {
    val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
    val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
    val englishDigits = value.map { char ->
        when {
            char in persianDigits -> ('0'.code + persianDigits.indexOf(char)).toChar()
            char in arabicDigits -> ('0'.code + arabicDigits.indexOf(char)).toChar()
            else -> char
        }
    }.joinToString("")
    return englishDigits.filter { it.isDigit() }.toLongOrNull() ?: 0L
}

private enum class DebtDeleteStage { IDLE, LOADING, SUCCESS }
private enum class DebtSettleStage { IDLE, LOADING, SUCCESS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(navController: NavController, debtId: Int) {
    val debts by DebtRepository.debts.collectAsStateWithLifecycle()

    val currentDebt = debts.find { it.id == debtId }

    var lastDebt by remember(debtId) {
        mutableStateOf<Debt?>(null)
    }

    LaunchedEffect(currentDebt) {
        if (currentDebt != null) {
            lastDebt = currentDebt
        }
    }

    val debt = currentDebt ?: lastDebt

    if (debt == null) {
        Scaffold(
            containerColor = BackgroundLight,
            topBar = {
                TopAppBar(
                    title = { Text("جزئیات طلب و بدهی", color = TextPrimary, fontFamily = Vazirmatn) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "بازگشت", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("این مورد پیدا نشد", color = TextSecondary, fontFamily = Vazirmatn)
            }
        }
        return
    }

    DebtDetailContent(navController, debt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtDetailContent(navController: NavController, debt: Debt) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteStage by remember { mutableStateOf(DebtDeleteStage.IDLE) }
    val allDebts by DebtRepository.debts.collectAsStateWithLifecycle()
    val isInstallment = debt.loanGroupId != null
    var showSettleDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val allPayments by DebtPaymentRepository.payments.collectAsStateWithLifecycle()
    val accounts by AccountRepository.accounts.collectAsStateWithLifecycle()
    val payments = allPayments.filter { it.debtId == debt.id }
    val isReceivable = debt.type == DebtType.RECEIVABLE
    val accentColor = if (isReceivable) IncomeGreen else ExpensePurple
    val title = if (isReceivable) "جزئیات طلب" else "جزئیات بدهی"
    val relation = if (isReceivable) "به شما بدهکار است" else "شما به او بدهکار هستید"

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(title, color = TextPrimary, fontFamily = Vazirmatn, style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "بازگشت", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        }
    ) { padding ->
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(accentColor.copy(alpha = .12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Person, null, tint = accentColor, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(debt.personName, color = TextPrimary, fontFamily = Vazirmatn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(relation, color = TextSecondary, fontFamily = Vazirmatn, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                        Text(detailAmount(debt.amount), color = accentColor, fontFamily = Vazirmatn, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(if (debt.isSettled) "تسویه شده" else "باز مانده", color = if (debt.isSettled) IncomeGreen else TextTertiary, fontFamily = Vazirmatn, style = MaterialTheme.typography.bodySmall)

                        if (debt.paidAmount > 0L && !debt.isSettled) {
                            Spacer(Modifier.height(14.dp))
                            LinearProgressIndicator(
                                progress = { (debt.paidAmount.toFloat() / debt.amount.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = accentColor,
                                trackColor = accentColor.copy(alpha = 0.15f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                                Text("پرداخت‌شده: ${detailAmount(debt.paidAmount)}", color = TextSecondary, fontFamily = Vazirmatn, style = MaterialTheme.typography.bodySmall)
                                Text("باقی‌مانده: ${detailAmount(debt.remainingAmount)}", color = accentColor, fontFamily = Vazirmatn, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DetailRow("تاریخ ثبت", PersianDateUtils.toPersianDigits(debt.createdDate))
                        if (debt.dueDate.isNotBlank()) {
                            DetailRow("تاریخ سررسید", PersianDateUtils.toPersianDigits(debt.dueDate), Icons.Outlined.CalendarMonth)

                            val dueDateNotPassed = (PersianDateUtils.daysUntil(debt.dueDate) ?: -1L) >= 0L
                            if (!debt.isSettled && dueDateNotPassed) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "یادآوری سررسید",
                                        color = TextSecondary,
                                        fontFamily = Vazirmatn,
                                        fontSize = 13.sp
                                    )
                                    Switch(
                                        checked = debt.reminderEnabled,
                                        onCheckedChange = { enabled ->
                                            val updated = debt.copy(reminderEnabled = enabled)
                                            DebtRepository.updateDebt(updated)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                                    )
                                }
                            }
                        }
                        if (debt.note.isNotBlank()) DetailRow("توضیحات", debt.note)

                        if (payments.isNotEmpty()) {
                            HorizontalDivider(color = DividerColor)
                            Text(
                                "تاریخچه‌ی پرداخت‌ها",
                                color = TextPrimary,
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                            val paymentOrdinals = listOf("اول", "دوم", "سوم", "چهارم", "پنجم", "ششم", "هفتم", "هشتم", "نهم", "دهم")
                            payments.sortedBy { it.id }.forEachIndexed { index, payment ->
                                val ordinal = paymentOrdinals.getOrNull(index) ?: "${index + 1}ام"
                                val accountName = accounts.find { it.id == payment.accountId }?.name
                                Column {
                                    Text(
                                        "پرداخت $ordinal: ${detailAmount(payment.amount)} در تاریخ ${PersianDateUtils.toPersianDigits(payment.date)}",
                                        color = TextSecondary,
                                        fontFamily = Vazirmatn,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )
                                    if (accountName != null) {
                                        Text(
                                            "از کارت: $accountName",
                                            color = TextTertiary,
                                            fontFamily = Vazirmatn,
                                            fontSize = 11.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!debt.isSettled) {
                    val canSettle = DebtRepository.canSettleInstallment(debt)
                    if (!canSettle) {
                        Text(
                            "ابتدا باید اقساط قبلی این وام تسویه شوند",
                            color = TextTertiary,
                            fontFamily = Vazirmatn,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Button(
                        onClick = { showSettleDialog = true },
                        enabled = canSettle,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            disabledContainerColor = accentColor.copy(alpha = 0.35f)
                        )
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isReceivable) "دریافت مبلغ" else "پرداخت مبلغ", color = Color.White, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = ExpensePurple, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("حذف این مورد", color = ExpensePurple, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteDialog) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
            AlertDialog(
                onDismissRequest = {
                    if (deleteStage == DebtDeleteStage.IDLE) showDeleteDialog = false
                },
                title = {
                    Text(
                        if (isInstallment) "حذف وام" else "حذف طلب یا بدهی",
                        fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        if (isInstallment)
                            "این قسط بخشی از وام «${debt.personName}» است. با حذف، کل وام، همهٔ اقساط و تراکنش‌های پرداخت‌شده برای همیشه حذف می‌شوند."
                        else
                            "مورد «${debt.personName}» برای همیشه حذف می‌شود.",
                        fontFamily = Vazirmatn, fontSize = 12.sp,
                        textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (deleteStage == DebtDeleteStage.IDLE) {
                                scope.launch {
                                    deleteStage = DebtDeleteStage.LOADING
                                    delay(700)

                                    val debtsToDelete = if (isInstallment)
                                        allDebts.filter { it.loanGroupId == debt.loanGroupId }
                                    else
                                        listOf(debt)

                                    val ids = debtsToDelete.map { it.id }.toSet()
                                    TransactionRepository.transactions.value
                                        .filter { it.debtId != null && it.debtId in ids }
                                        .forEach { TransactionRepository.deleteTransaction(it) }
                                    debtsToDelete.forEach { DebtRepository.deleteDebt(it) }

                                    deleteStage = DebtDeleteStage.SUCCESS
                                    delay(1200)
                                    showDeleteDialog = false
                                    deleteStage = DebtDeleteStage.IDLE
                                    navController.popBackStack()
                                }
                            }
                        },
                        enabled = deleteStage == DebtDeleteStage.IDLE
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (deleteStage) {
                                DebtDeleteStage.LOADING -> {
                                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = ExpensePurple)
                                    Spacer(Modifier.width(6.dp))
                                    Text("...در حال حذف", color = ExpensePurple, fontFamily = Vazirmatn, fontSize = 13.sp)
                                }
                                DebtDeleteStage.SUCCESS -> {
                                    Icon(Icons.Outlined.CheckCircle, null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("حذف شد", color = IncomeGreen, fontFamily = Vazirmatn, fontSize = 13.sp)
                                }
                                DebtDeleteStage.IDLE -> Text("حذف کن", color = ExpensePurple, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }, enabled = deleteStage == DebtDeleteStage.IDLE) {
                        Text("انصراف", color = TextSecondary, fontFamily = Vazirmatn, fontSize = 13.sp)
                    }
                }
            )
        }
    }

    if (showSettleDialog) {
        SettleDebtDialog(
            navController = navController,
            debt = debt,
            accentColor = accentColor,
            isReceivable = isReceivable,
            onDismiss = { showSettleDialog = false },
            onConfirm = { settleAmount, accountId ->
                val newPaidAmount = (debt.paidAmount + settleAmount).coerceAtMost(debt.amount)
                val nowSettled = newPaidAmount >= debt.amount
                val today = PersianDateUtils.getCurrentPersianDate()

                DebtRepository.updateDebt(
                    debt.copy(paidAmount = newPaidAmount, isSettled = nowSettled)
                )

                TransactionRepository.addTransaction(
                    Transaction(
                        title = if (isReceivable) "وصول طلب از ${debt.personName}" else "پرداخت بدهی به ${debt.personName}",
                        amount = settleAmount,
                        type = if (isReceivable) TransactionType.INCOME else TransactionType.EXPENSE,
                        category = if (isReceivable) "طلب" else "بدهی",
                        date = today,
                        time = PersianDateUtils.getCurrentTime(),
                        note = debt.note,
                        accountId = accountId,
                        debtId = debt.id
                    )
                )

                DebtPaymentRepository.addPayment(
                    DebtPayment(
                        debtId = debt.id,
                        amount = settleAmount,
                        date = today,
                        accountId = accountId
                    )
                )

                showSettleDialog = false
            }
        )
    }
}

@Composable
private fun SettleDebtDialog(
    navController: NavController,
    debt: Debt,
    accentColor: Color,
    isReceivable: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long, Int?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var showKeypad by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf<Int?>(null) }
    var settleStage by remember { mutableStateOf(DebtSettleStage.IDLE) }
    val scope = rememberCoroutineScope()
    val accounts by AccountRepository.accounts.collectAsStateWithLifecycle()
    val enteredAmount = parseSettleAmount(amountText)
    val exceedsRemaining = enteredAmount > debt.remainingAmount
    // انتخاب کارت اجباری است چون این تسویه باید در تراکنش‌های اصلی به یک کارت مشخص وصل شود
    val isValid = enteredAmount > 0L && !exceedsRemaining && selectedAccountId != null

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
        AlertDialog(
            onDismissRequest = { if (settleStage == DebtSettleStage.IDLE) onDismiss() },
            containerColor = CardWhite,
            title = {
                Text(
                    if (isReceivable) "دریافت مبلغ از ${debt.personName}" else "پرداخت مبلغ به ${debt.personName}",
                    fontFamily = Vazirmatn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column {
                    Text(
                        "باقی‌مانده: ${detailAmount(debt.remainingAmount)}",
                        color = TextSecondary,
                        fontFamily = Vazirmatn,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    val hasEnteredAmount = amountText.isNotBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFDFDFD))
                            .then(
                                if (hasEnteredAmount) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = if (exceedsRemaining) ExpensePurple else accentColor,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                } else {
                                    Modifier.border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                                }
                            )
                            .clickable { showKeypad = true }
                    ) {
                        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    if (hasEnteredAmount) amountText else "۰",
                                    color = if (hasEnteredAmount) TextPrimary else TextTertiary,
                                    fontFamily = Vazirmatn,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "تومان",
                                    color = TextSecondary,
                                    fontFamily = Vazirmatn,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    if (exceedsRemaining) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "مبلغ وارد شده از باقی‌مانده بیشتر است",
                            color = ExpensePurple,
                            fontFamily = Vazirmatn,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TextButton(
                        onClick = { amountText = PersianDateUtils.toPersianDigits(debt.remainingAmount.toString()) },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("تسویه‌ی کامل باقی‌مانده", color = accentColor, fontFamily = Vazirmatn, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(4.dp))
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                        AccountPicker(
                            accounts = accounts,
                            selectedAccountId = selectedAccountId,
                            navController = navController,
                            activeColor = accentColor,
                            onSelect = { selectedAccountId = it }
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (isReceivable)
                            "با تایید این مبلغ، یک تراکنش درآمد به‌صورت خودکار ثبت می‌شود و پس از آن قابل ویرایش یا لغو نیست."
                        else
                            "با تایید این مبلغ، یک تراکنش هزینه به‌صورت خودکار ثبت می‌شود و پس از آن قابل ویرایش یا لغو نیست.",
                        color = TextTertiary,
                        fontFamily = Vazirmatn,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (settleStage == DebtSettleStage.IDLE) {
                            scope.launch {
                                settleStage = DebtSettleStage.LOADING
                                delay(700)
                                settleStage = DebtSettleStage.SUCCESS
                                delay(900)
                                onConfirm(enteredAmount, selectedAccountId)
                                settleStage = DebtSettleStage.IDLE
                            }
                        }
                    },
                    enabled = isValid && settleStage == DebtSettleStage.IDLE
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (settleStage) {
                            DebtSettleStage.LOADING -> {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = accentColor)
                                Spacer(Modifier.width(6.dp))
                                Text("...در حال تسویه", color = accentColor, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                            DebtSettleStage.SUCCESS -> {
                                Icon(Icons.Outlined.CheckCircle, null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("تسویه شد", color = IncomeGreen, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                            DebtSettleStage.IDLE -> {
                                Text("تایید", color = if (isValid) accentColor else TextTertiary, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = settleStage == DebtSettleStage.IDLE) {
                    Text("انصراف", color = TextSecondary, fontFamily = Vazirmatn, fontSize = 13.sp)
                }
            }
        )
    }

    if (showKeypad) {
        AmountKeypadDialog(
            initialAmount = amountText,
            accentColor = accentColor,
            onDismiss = { showKeypad = false },
            onConfirm = { newAmount ->
                amountText = newAmount
                showKeypad = false
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(.42f)) {
            if (icon != null) {
                Icon(icon, null, tint = TextTertiary, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(label, color = TextSecondary, fontFamily = Vazirmatn, style = MaterialTheme.typography.bodySmall)
        }
        Text(value, color = TextPrimary, fontFamily = Vazirmatn, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Left, modifier = Modifier.weight(.58f))
    }
}

fun detailAmount(amount: Long): String {
    val formatted = NumberFormat.getInstance(Locale.US).format(amount)
    return "${PersianDateUtils.toPersianDigits(formatted)} تومان"
}
