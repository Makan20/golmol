package com.example.nargesapp.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.nargesapp.data.model.Account
import com.example.nargesapp.data.model.Transaction
import com.example.nargesapp.data.model.TransactionType
import com.example.nargesapp.data.repository.AccountRepository
import com.example.nargesapp.ui.theme.*
import com.example.nargesapp.ui.utils.PersianDateUtils
import com.example.nargesapp.ui.viewmodel.TransactionViewModel

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

private fun formatCardNumberInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(16)
    return digits.chunked(4).joinToString("-")
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

                    if (accounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "هنوز کارتی اضافه نکردی",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontFamily = Vazirmatn
                            )
                        }
                    } else {
                        accounts.forEach { account ->
                            SwipeableAccountCard(
                                account = account,
                                balance = accountBalance(account, transactions),
                                cardColor = cardColorPalette[
                                    account.colorIndex % cardColorPalette.size
                                ],
                                onClick = { editingAccount = account },
                                onDelete = { AccountRepository.deleteAccount(account) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    AddCardButton(onClick = { showAddDialog = true })
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "با سویپ به چپ یا راست، کارت حذف می‌شود.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontFamily = Vazirmatn,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (accounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        StartingBalancesCard(accounts, transactions)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
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
                    onConfirm = { name, cardNumber, typedAmount, colorIndex ->
                        AccountRepository.addAccount(
                            Account(
                                name = name,
                                cardNumber = cardNumber,
                                startingBalance = typedAmount,
                                colorIndex = colorIndex,
                                startingBalanceDate = PersianDateUtils.getCurrentPersianDate()
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
                    initialAmountDisplay = formatAmountInput(currentBalance.toString()),
                    initialColorIndex = accountBeingEdited.colorIndex,
                    onDismiss = { editingAccount = null },
                    onConfirm = { name, cardNumber, typedAmount, colorIndex ->
                        val alreadyAppliedDelta =
                            currentBalance - accountBeingEdited.startingBalance
                        val newStartingBalance = typedAmount - alreadyAppliedDelta
                        AccountRepository.updateAccount(
                            accountBeingEdited.copy(
                                name = name,
                                cardNumber = cardNumber,
                                startingBalance = newStartingBalance,
                                colorIndex = colorIndex,
                                startingBalanceDate = PersianDateUtils.getCurrentPersianDate()
                            )
                        )
                        editingAccount = null
                    }
                )
            }
        }
    }
}

@Composable
fun SwipeableAccountCard(
    account: Account,
    balance: Long,
    cardColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
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
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFFEBEE))
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
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "حذف کارت",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(22.dp)
                    )
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

@Composable
fun AccountCard(
    account: Account,
    balance: Long,
    cardColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        account.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                    if (account.cardNumber.isNotBlank()) {
                        Text(
                            account.cardNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            fontFamily = Vazirmatn
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatPersianAmount(balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                    Text(
                        "تومان",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = Vazirmatn
                    )
                }
            }
        }
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
    initialColorIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Int) -> Unit
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
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { newValue ->
                            cardNumber = formatCardNumberFieldValue(newValue)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (cardNumberHasValue) 2.dp else 1.dp,
                                color = if (cardNumberHasValue) PrimaryGreen else DividerColor,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "موجودی فعلی",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = amountText,
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
                                        onConfirm(
                                            name.trim(),
                                            cardNumber.text.trim(),
                                            parsePersianAmount(amountText.text),
                                            colorIndex
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
}
