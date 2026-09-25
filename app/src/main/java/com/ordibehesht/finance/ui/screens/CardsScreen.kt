package com.ordibehesht.finance.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ordibehesht.finance.data.model.Account
import com.ordibehesht.finance.data.model.Bank
import com.ordibehesht.finance.data.model.BankCatalog
import com.ordibehesht.finance.data.model.Transaction
import com.ordibehesht.finance.data.model.TransactionType
import com.ordibehesht.finance.data.repository.AccountRepository
import com.ordibehesht.finance.ui.theme.*
import com.ordibehesht.finance.ui.utils.PersianDateUtils
import com.ordibehesht.finance.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val cardColorPalette = listOf(
    PrimaryGreen,
    ExpensePurple,
    IncomeGreen,
    Color(0xFF4C8577),
    Color(0xFF6B5B95),
    Color(0xFFD4A24C),
    Color(0xFF5B9BD5),
    Color(0xFFD97D8E),
    Color(0xFF3C7A89),
    Color(0xFFC77B58)
)

fun accountBalance(account: Account, transactions: List<Transaction>): Long {
    val income = transactions
        .filter { it.accountId == account.id && it.type == TransactionType.INCOME }
        .sumOf { it.amount }
    val expense = transactions
        .filter { it.accountId == account.id && it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }
    return account.startingBalance + income - expense
}

private fun isCardDigit(char: Char): Boolean {
    return char.isDigit() || char in '۰'..'۹'
}

private fun formatCardNumberFieldValue(newValue: TextFieldValue): TextFieldValue {
    val rawText = newValue.text
    val cursorPosition = newValue.selection.end.coerceIn(0, rawText.length)
    val digitsBeforeCursor = rawText.take(cursorPosition).count(::isCardDigit)
    val digitsOnly = rawText.filter(::isCardDigit).take(16)
    val formatted = digitsOnly.chunked(4).joinToString("-")

    if (digitsBeforeCursor == 0) {
        return TextFieldValue(formatted, TextRange(0))
    }

    var seenDigits = 0
    var newCursorPosition = formatted.length

    for ((index, char) in formatted.withIndex()) {
        if (isCardDigit(char)) seenDigits++
        if (seenDigits == digitsBeforeCursor) {
            newCursorPosition = index + 1
            break
        }
    }

    return TextFieldValue(formatted, TextRange(newCursorPosition))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(navController: NavController, viewModel: TransactionViewModel) {
    val scrollState = rememberScrollState()
    val accounts by AccountRepository.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var transferFromAccountId by remember { mutableStateOf<Int?>(null) }

    // برای امکان «برگردون» بعد از حذف کارت با سوایپ — یک سوایپ اشتباه نباید بدون هیچ
    // فرصتی برای جبران، کارت را برای همیشه از بین ببرد (دقیقاً هم‌الگو با حذف تراکنش در
    // HomeScreen). توجه: چون AccountRepository.deleteAccount ارجاع تراکنش‌های قدیمی به این
    // کارت را پاک می‌کند، «برگردون» فقط خودِ کارت را بازمی‌گرداند، نه پیوند تراکنش‌های قدیمی
    var pendingDeleteAccount by remember { mutableStateOf<PendingUndo<Account>?>(null) }

    fun deleteAccountWithUndo(account: Account) {
        AccountRepository.deleteAccount(account)
        pendingDeleteAccount = PendingUndo(account, message = "کارت حذف شد")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "کارت‌های من",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontFamily = Vazirmatn
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showTransferDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.CompareArrows,
                            contentDescription = "انتقال بین کارت‌ها",
                            tint = TextPrimary
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
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 4.dp)
                    .size(56.dp)
            )
            FlowerDecoration(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 60.dp, start = 4.dp)
                    .size(44.dp),
                color = ExpensePurple.copy(alpha = 0.08f)
            )

            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "کارت دلخواهتان را برای ثبت تراکنش انتخاب کنید. برای ویرایش، روی کارت بزنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = Vazirmatn
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val activeAccounts = accounts.filter { !it.isArchived }
                    val archivedAccounts = accounts.filter { it.isArchived }

                    if (activeAccounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FlowerDecoration(
                                    modifier = Modifier.size(52.dp),
                                    color = ExpensePurple.copy(alpha = 0.14f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "هنوز کارتی اضافه نکردی",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontFamily = Vazirmatn
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                activeAccounts.forEachIndexed { index, account ->
                                    if (index > 0) {
                                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                                    }
                                    SwipeableAccountCard(
                                        account = account,
                                        balance = accountBalance(account, transactions),
                                        cardColor = cardColorPalette[
                                            account.colorIndex % cardColorPalette.size
                                        ],
                                        onClick = { editingAccount = account },
                                        onDelete = { deleteAccountWithUndo(account) },
                                        onArchive = { AccountRepository.updateAccount(account.copy(isArchived = true)) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    AddCardButton(onClick = { showAddDialog = true })
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "برای آرشیو کارت به راست و برای حذف به چپ بکشید.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontFamily = Vazirmatn,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (archivedAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        ArchivedAccountsSection(
                            archivedAccounts = archivedAccounts,
                            transactions = transactions,
                            onUnarchive = { account ->
                                AccountRepository.updateAccount(account.copy(isArchived = false))
                            },
                            onDelete = { account -> AccountRepository.deleteAccount(account) }
                        )
                    }

                    if (activeAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        StartingBalancesCard(activeAccounts, transactions)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            UndoDeleteSnackbar(
                pending = pendingDeleteAccount,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 112.dp),
                onExpired = { pendingDeleteAccount = null },
                onUndo = { account ->
                    // id صفر می‌شود تا Room برای رکورد بازگردانده‌شده یک id جدید تولید کند —
                    // چون id قبلی دیگر در جدول وجود ندارد و منطقاً هم نیازی به همان id نیست
                    AccountRepository.addAccount(account.copy(id = 0))
                    pendingDeleteAccount = null
                }
            )
        }
    }

    if (showAddDialog) {
        AddCardDialog(
            title = "افزودن کارت جدید",
            confirmLabel = "ثبت",
            initialName = "",
            initialCardNumber = "",
            initialAmountDisplay = "",
            initialColorIndex = 0,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, cardNumber, typedAmount, colorIndex, bankKey ->
                AccountRepository.addAccount(
                    Account(
                        name = name,
                        cardNumber = cardNumber,
                        startingBalance = typedAmount,
                        colorIndex = colorIndex,
                        startingBalanceDate = PersianDateUtils.getCurrentPersianDate(),
                        bankKey = bankKey
                    )
                )
                showAddDialog = false
            }
        )
    }

    val accountBeingEdited = editingAccount
    if (accountBeingEdited != null) {
        val currentBalance = accountBalance(accountBeingEdited, transactions)
        AddCardDialog(
            title = "ویرایش کارت",
            confirmLabel = "ذخیره",
            initialName = accountBeingEdited.name,
            initialCardNumber = accountBeingEdited.cardNumber,
            initialAmountDisplay = formatAmountInput(kotlin.math.abs(currentBalance).toString()),
            initialIsNegative = currentBalance < 0,
            initialColorIndex = accountBeingEdited.colorIndex,
            initialBankKey = accountBeingEdited.bankKey,
            onDismiss = { editingAccount = null },
            onConfirm = { name, cardNumber, typedAmount, colorIndex, bankKey ->
                val alreadyAppliedDelta =
                    currentBalance - accountBeingEdited.startingBalance
                val newStartingBalance = typedAmount - alreadyAppliedDelta
                // برچسب «تاریخ ثبت موجودی اولیه» فقط وقتی به امروز به‌روزرسانی می‌شود که
                // کاربر واقعاً مبلغ موجودی را عوض کرده باشد؛ صرفاً ویرایش نام یا شماره‌ی
                // کارت (بدون تغییر موجودی) دیگر این تاریخ را گمراه‌کننده جابه‌جا نمی‌کند
                val balanceChanged = typedAmount != currentBalance
                AccountRepository.updateAccount(
                    accountBeingEdited.copy(
                        name = name,
                        cardNumber = cardNumber,
                        startingBalance = newStartingBalance,
                        colorIndex = colorIndex,
                        startingBalanceDate = if (balanceChanged)
                            PersianDateUtils.getCurrentPersianDate()
                        else
                            accountBeingEdited.startingBalanceDate,
                        bankKey = bankKey
                    )
                )
                editingAccount = null
            },
            onTransfer = {
                transferFromAccountId = accountBeingEdited.id
                editingAccount = null
                showTransferDialog = true
            }
        )
    }

    if (showTransferDialog) {
        TransferDialog(
            accounts = accounts,
            initialFromAccountId = transferFromAccountId,
            navController = navController,
            onDismiss = {
                showTransferDialog = false
                transferFromAccountId = null
            },
            onDone = {
                showTransferDialog = false
                transferFromAccountId = null
            }
        )
    }
}

@Composable
fun SwipeableAccountCard(
    account: Account,
    balance: Long,
    cardColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit
) {
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 90.dp.toPx() }
    var offsetX by remember(account.id) { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        label = "cardSwipeOffset"
    )

    val isRevealing = kotlin.math.abs(animatedOffset) > swipeThreshold * 0.25f

    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        animatedOffset > 0f -> LightGreen
                        animatedOffset < 0f -> Color(0xFFFFEBEE)
                        else -> Color.Transparent
                    }
                )
        ) {
            if (isRevealing) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 22.dp)
                        .align(
                            if (animatedOffset > 0) {
                                Alignment.CenterStart
                            } else {
                                Alignment.CenterEnd
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // راست = آرشیو (سبز)، چپ = حذف (قرمز) — دقیقاً مطابق متن راهنمای پایین لیست
                    if (animatedOffset > 0) {
                        Icon(
                            Icons.Outlined.Archive,
                            contentDescription = "آرشیو کارت",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "حذف کارت",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        androidx.compose.ui.unit.IntOffset(animatedOffset.toInt(), 0)
                    }
                    .pointerInput(account.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX > swipeThreshold) {
                                    onArchive()
                                } else if (offsetX < -swipeThreshold) {
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
                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    AccountCard(
                        account = account,
                        balance = balance,
                        cardColor = cardColor,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
fun AccountCard(
    account: Account,
    balance: Long,
    cardColor: Color,
    onClick: () -> Unit
) {
    val bank = BankCatalog.byKey(account.bankKey)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ترتیب درخواستی: لوگو (راست‌ترین) - نقطه‌ی رنگی - اسم کارت
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (bank != null) {
                BankLogo(bank = bank, size = 26)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(cardColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    account.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )
                if (account.cardNumber.isNotBlank()) {
                    Text(
                        account.cardNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontFamily = Vazirmatn
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatPersianAmount(balance),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = Vazirmatn
            )
            Text(
                "تومان",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontFamily = Vazirmatn
            )
        }
    }
}

// بخش «کارت‌های آرشیوشده» — جمع‌شونده، پیش‌فرض بسته، چون این یک بخش کم‌کاربردتر است و
// نباید فضای زیادی از کارت‌های فعال بگیرد. هر کارت آرشیوشده موجودی محاسبه‌شده‌اش (بر اساس
// همان تاریخچه‌ی تراکنش‌های واقعی‌اش) را نشان می‌دهد، و یک دکمه‌ی «بازگردانی» برای برگرداندن
// به لیست فعال دارد — تاریخچه‌ی تراکنش‌ها هرگز پاک نمی‌شود، فقط از دید مخفی می‌شود
private enum class DeleteFromArchiveStage { IDLE, LOADING, SUCCESS }

@Composable
fun ArchivedAccountsSection(
    archivedAccounts: List<Account>,
    transactions: List<Transaction>,
    onUnarchive: (Account) -> Unit,
    onDelete: (Account) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var pendingDeleteFromArchive by remember { mutableStateOf<Account?>(null) }
    var deleteFromArchiveStage by remember { mutableStateOf(DeleteFromArchiveStage.IDLE) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Archive,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "کارت‌های آرشیوشده (${PersianDateUtils.toPersianDigits(archivedAccounts.size.toString())})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                archivedAccounts.forEachIndexed { index, account ->
                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BackgroundLight)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(cardColorPalette[account.colorIndex % cardColorPalette.size].copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    account.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Vazirmatn
                                )
                                Text(
                                    "${formatPersianAmount(accountBalance(account, transactions))} تومان",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    fontFamily = Vazirmatn
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onUnarchive(account) }) {
                                Text(
                                    "بازگردانی",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Vazirmatn
                                )
                            }
                            IconButton(
                                onClick = { pendingDeleteFromArchive = account },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "حذف کامل کارت",
                                    tint = ExpensePurple,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val accountToDelete = pendingDeleteFromArchive
    if (accountToDelete != null) {
        val linkedCount = transactions.count { it.accountId == accountToDelete.id }
        val scope = rememberCoroutineScope()
        AlertDialog(
            containerColor = CardWhite,
            onDismissRequest = { if (deleteFromArchiveStage == DeleteFromArchiveStage.IDLE) pendingDeleteFromArchive = null },
            title = {
                Text(
                    "حذف همیشگی «${accountToDelete.name}»",
                    fontFamily = Vazirmatn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    if (linkedCount > 0) {
                        // هشدار صریح تعداد تراکنش‌های وابسته — چون حذف کامل (برخلاف آرشیو)
                        // ارجاع همه‌ی این تراکنش‌ها به این کارت را برای همیشه پاک می‌کند و
                        // دیگر قابل بازگشت نیست، حتی با «برگردون» بلافاصله بعد از حذف
                        "$linkedCount تراکنش قدیمی به این کارت وصل است. با حذف کامل، پیوند همه‌ی آن‌ها به این کارت برای همیشه از بین می‌رود (خود تراکنش‌ها حذف نمی‌شوند). مطمئنی؟"
                    } else {
                        "این کارت برای همیشه حذف می‌شود. مطمئنی؟"
                    },
                    fontFamily = Vazirmatn,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleteFromArchiveStage == DeleteFromArchiveStage.IDLE) {
                            scope.launch {
                                deleteFromArchiveStage = DeleteFromArchiveStage.LOADING
                                delay(700)
                                onDelete(accountToDelete)
                                deleteFromArchiveStage = DeleteFromArchiveStage.SUCCESS
                                delay(900)
                                pendingDeleteFromArchive = null
                                deleteFromArchiveStage = DeleteFromArchiveStage.IDLE
                            }
                        }
                    },
                    enabled = deleteFromArchiveStage == DeleteFromArchiveStage.IDLE
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (deleteFromArchiveStage) {
                            DeleteFromArchiveStage.LOADING -> {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = ExpensePurple)
                                Spacer(Modifier.width(6.dp))
                                Text("...در حال حذف", color = ExpensePurple, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                            DeleteFromArchiveStage.SUCCESS -> {
                                Icon(Icons.Outlined.CheckCircle, null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("حذف شد", color = IncomeGreen, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                            DeleteFromArchiveStage.IDLE -> {
                                Text("حذف کامل", color = ExpensePurple, fontWeight = FontWeight.Bold, fontFamily = Vazirmatn)
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteFromArchive = null },
                    enabled = deleteFromArchiveStage == DeleteFromArchiveStage.IDLE
                ) {
                    Text("انصراف", fontFamily = Vazirmatn)
                }
            }
        )
    }
}

@Composable
fun StartingBalancesCard(accounts: List<Account>, transactions: List<Transaction>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "موجودی اولیه ثبت‌شده",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = Vazirmatn
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "همون عددیه که اول برای هر کارت نوشتی؛ با تراکنش‌ها تغییر نمی‌کنه، فقط با ویرایش یا حذف کارت.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontFamily = Vazirmatn
            )
            Spacer(modifier = Modifier.height(14.dp))

            accounts.forEachIndexed { index, account ->
                val netChange = accountBalance(account, transactions) - account.startingBalance
                val changeColor = if (netChange >= 0) IncomeGreen else ExpensePurple

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    cardColorPalette[
                                        account.colorIndex % cardColorPalette.size
                                    ]
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            account.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontFamily = Vazirmatn
                        )
                    }

                    CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CompositionLocalProvider(
                                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
                            ) {
                                Text(
                                    "${formatAmount(account.startingBalance)} تومان",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontFamily = Vazirmatn
                                )
                            }
                            if (account.startingBalanceDate.isNotBlank()) {
                                Text(
                                    " | ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontFamily = Vazirmatn
                                )
                                Text(
                                    PersianDateUtils.toPersianDigits(account.startingBalanceDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontFamily = Vazirmatn
                                )
                            }
                            if (netChange != 0L) {
                                Text(
                                    " | ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontFamily = Vazirmatn
                                )
                                Text(
                                    if (netChange > 0) "+" else "-",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = changeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Vazirmatn
                                )
                                Text(
                                    PersianDateUtils.toPersianDigits(
                                        formatAmount(kotlin.math.abs(netChange))
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = changeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Vazirmatn
                                )
                            }
                        }
                    }
                }
                if (index < accounts.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun AddCardButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f))
    ) {
        Text(
            "+ افزودن کارت جدید",
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryGreen,
            fontWeight = FontWeight.Bold,
            fontFamily = Vazirmatn,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

@Composable
fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) Modifier.border(2.dp, TextPrimary, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun AddCardDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialCardNumber: String,
    initialAmountDisplay: String,
    initialIsNegative: Boolean = false,
    initialColorIndex: Int,
    initialBankKey: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Int, String?) -> Unit,
    onTransfer: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var cardNumber by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialCardNumber,
                selection = TextRange(initialCardNumber.length)
            )
        )
    }
    var selectedBank by remember { mutableStateOf<Bank?>(BankCatalog.byKey(initialBankKey)) }
    var showBankPicker by remember { mutableStateOf(false) }
    var showCardNumberKeypad by remember { mutableStateOf(false) }
    var amountText by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialAmountDisplay,
                selection = TextRange(initialAmountDisplay.length)
            )
        )
    }
    var colorIndex by remember { mutableStateOf(initialColorIndex) }
    var showAmountKeypad by remember { mutableStateOf(false) }
    // چون کیبورد مبلغ (AmountKeypadDialog) فقط برای اعداد مثبت طراحی شده (همان کیبورد مبلغ
    // تراکنش)، علامت منفیِ موجودی را جدا نگه می‌داریم؛ در غیر این صورت وقتی موجودی واقعی
    // کارت منفی بود (هزینه بیشتر از درآمد)، فیلد آن را مثبت نشان می‌داد و اگر کاربر بدون
    // توجه به این فیلد فقط رنگ/بانک را عوض می‌کرد و ذخیره می‌زد، موجودی به‌اشتباه مثبت
    // می‌شد و کل حساب به‌هم می‌ریخت
    var isNegativeBalance by remember { mutableStateOf(initialIsNegative) }

    val nameHasValue = name.isNotBlank()
    val cardNumberHasValue = cardNumber.text.isNotBlank()
    val amountHasValue = amountText.text.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Vazirmatn
                        )
                        if (onTransfer != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onTransfer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.CompareArrows,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "انتقال",
                                    color = PrimaryGreen,
                                    fontFamily = Vazirmatn,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── فیلد بانک ──
                    Text(
                        "بانک",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedBank?.name ?: "",
                            onValueChange = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (selectedBank != null) 2.dp else 1.dp,
                                    color = if (selectedBank != null) PrimaryGreen else DividerColor,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color.Transparent,
                                disabledContainerColor = Color(0xFFFDFDFD),
                                disabledTextColor = TextPrimary
                            ),
                            placeholder = {
                                Text(
                                    "انتخاب بانک (اختیاری)",
                                    textAlign = TextAlign.Right,
                                    color = TextTertiary,
                                    fontSize = 14.sp,
                                    fontFamily = Vazirmatn,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            leadingIcon = {
                                selectedBank?.let { bank ->
                                    BankLogo(bank = bank, size = 26)
                                }
                            },
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Right,
                                fontSize = 15.sp,
                                fontFamily = Vazirmatn
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showBankPicker = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "اسم کارت",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (nameHasValue) 2.dp else 1.dp,
                                color = if (nameHasValue) PrimaryGreen else DividerColor,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        placeholder = {
                            Text(
                                "مثلاً کارت ملی",
                                textAlign = TextAlign.Right,
                                color = TextTertiary,
                                fontSize = 14.sp,
                                fontFamily = Vazirmatn,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Right,
                            fontSize = 15.sp,
                            fontFamily = Vazirmatn
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "شماره کارت (اختیاری)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // فیلد فقط نمایشی است؛ کیبورد سیستم باز نمی‌شود — با لمس آن، کی‌پد
                        // اختصاصی شماره کارت (CardNumberKeypadDialog) باز می‌شود، درست مثل
                        // رفتار فیلد مبلغ در همین دیالوگ
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (cardNumberHasValue) 2.dp else 1.dp,
                                    color = if (cardNumberHasValue) PrimaryGreen else DividerColor,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                disabledTextColor = TextPrimary
                            ),
                            placeholder = {
                                Text(
                                    "5892-1011-1213-1415",
                                    textAlign = TextAlign.Right,
                                    color = TextTertiary,
                                    fontSize = 14.sp,
                                    fontFamily = Vazirmatn,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Right,
                                fontSize = 15.sp,
                                fontFamily = Vazirmatn
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showCardNumberKeypad = true }
                        )
                    }

                    if (showCardNumberKeypad) {
                        CardNumberKeypadDialog(
                            initialCardNumber = cardNumber.text,
                            accentColor = PrimaryGreen,
                            onDismiss = { showCardNumberKeypad = false },
                            onConfirm = { newValue ->
                                cardNumber = formatCardNumberFieldValue(
                                    TextFieldValue(newValue, TextRange(newValue.length))
                                )
                                // تشخیص خودکار بانک از روی شماره کارت
                                val detected = BankCatalog.fromCardNumber(cardNumber.text)
                                if (detected != null && selectedBank == null) {
                                    selectedBank = detected
                                    if (name.isBlank()) {
                                        name = "کارت ${detected.name.replace("بانک ", "")}"
                                    }
                                }
                                showCardNumberKeypad = false
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "موجودی فعلی",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontFamily = Vazirmatn
                        )

                        // سوییچ صریح برای علامت منفی — چون کیبورد مبلغ فقط عدد مثبت
                        // می‌گیرد، این تنها راهی است که کاربر آگاهانه بگوید موجودی این
                        // کارت واقعاً منفی است (مثلاً کارتی که هزینه‌اش از درآمدش بیشتر شده)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isNegativeBalance = !isNegativeBalance }
                                .background(if (isNegativeBalance) ExpensePurple.copy(alpha = 0.12f) else Color(0xFFF3F3F3))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isNegativeBalance) ExpensePurple else Color.Transparent)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isNegativeBalance) ExpensePurple else TextTertiary,
                                        shape = RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isNegativeBalance) {
                                    Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "موجودی منفی است",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isNegativeBalance) ExpensePurple else TextSecondary,
                                fontFamily = Vazirmatn,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = amountText.copy(
                                text = if (isNegativeBalance && amountText.text.isNotBlank()) {
                                    // \u202A(LRE) ... \u202C(PDF): بخش عدد را به‌صورت یک بلوکِ
                                    // چپ‌به‌راست جدا محصور می‌کند تا علامت منفی همیشه به‌طور
                                    // قابل‌اعتماد به سمت چپِ کل عدد بچسبد — یک "-" ساده کنار
                                    // ارقام فارسی در بافت RTL از نظر جهت‌یابی دوجهته مبهم است و
                                    // ممکن است بسته به رندرر، جای دیگری نمایش داده شود
                                    "\u202A-${amountText.text}\u202C"
                                } else {
                                    amountText.text
                                }
                            ),
                            onValueChange = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (amountHasValue) 2.dp else 1.dp,
                                    color = if (amountHasValue) PrimaryGreen else DividerColor,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color.Transparent,
                                disabledContainerColor = Color(0xFFFDFDFD),
                                disabledTextColor = TextPrimary
                            ),
                            placeholder = {
                                Text(
                                    "۰",
                                    textAlign = TextAlign.Right,
                                    color = TextTertiary,
                                    fontSize = 14.sp,
                                    fontFamily = Vazirmatn,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Right,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = Vazirmatn
                            ),
                            suffix = {
                                Text(
                                    "تومان",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontFamily = Vazirmatn
                                )
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showAmountKeypad = true }
                        )
                    }

                    if (showAmountKeypad) {
                        AmountKeypadDialog(
                            initialAmount = amountText.text,
                            accentColor = PrimaryGreen,
                            onDismiss = { showAmountKeypad = false },
                            onConfirm = { newAmount ->
                                amountText = TextFieldValue(
                                    text = newAmount,
                                    selection = TextRange(newAmount.length)
                                )
                                showAmountKeypad = false
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "رنگ کارت",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (0..4).forEach { index ->
                            ColorDot(
                                color = cardColorPalette[index],
                                selected = colorIndex == index,
                                onClick = { colorIndex = index }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (5..9).forEach { index ->
                            ColorDot(
                                color = cardColorPalette[index],
                                selected = colorIndex == index,
                                onClick = { colorIndex = index }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PrimaryGreen)
                        ) {
                            TextButton(
                                onClick = {
                                    if (name.isNotBlank()) {
                                        val rawAmount = parsePersianAmount(amountText.text)
                                        onConfirm(
                                            name.trim(),
                                            cardNumber.text.trim(),
                                            if (isNegativeBalance) -rawAmount else rawAmount,
                                            colorIndex,
                                            selectedBank?.key
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    confirmLabel,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Vazirmatn
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DividerColor.copy(alpha = 0.5f))
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "لغو",
                                    color = TextSecondary,
                                    fontFamily = Vazirmatn
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBankPicker) {
        BankPickerDialog(
            selectedBankKey = selectedBank?.key,
            onBankSelected = { bank ->
                selectedBank = bank
                if (name.isBlank()) {
                    name = "کارت ${bank.name.replace("بانک ", "")}"
                }
            },
            onClear = { selectedBank = null },
            onDismiss = { showBankPicker = false }
        )
    }
}
