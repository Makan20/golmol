package com.example.nargesapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.nargesapp.data.model.Account
import com.example.nargesapp.data.model.Transaction
import com.example.nargesapp.data.model.TransactionType
import com.example.nargesapp.data.repository.AccountRepository
import com.example.nargesapp.ui.theme.*
import com.example.nargesapp.ui.utils.PersianDateUtils
import com.example.nargesapp.ui.viewmodel.TransactionViewModel

fun persianToEnglish(input: String): String {
    val persian = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return input.map { ch ->
        val idx = persian.indexOf(ch)
        if (idx >= 0) idx.toString() else ch
    }.joinToString("")
}

fun englishToPersian(input: String): String {
    val english = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val persian = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return input.map { ch ->
        val idx = english.indexOf(ch)
        if (idx >= 0) persian[idx] else ch
    }.joinToString("")
}

fun parsePersianAmount(input: String): Long {
    val clean = persianToEnglish(input).filter { it.isDigit() }
    return if (clean.isEmpty()) 0 else clean.toLong()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    viewModel: TransactionViewModel,
    transactionId: Int = -1,
    initialType: String = "expense"
) {
    val isEditMode = transactionId != -1
    val existingTransaction = if (isEditMode) {
        viewModel.getTransactionById(transactionId)
    } else null

    var selectedType by remember {
        mutableStateOf(existingTransaction?.type ?: if (initialType == "income") TransactionType.INCOME else TransactionType.EXPENSE)
    }

    var title by remember { mutableStateOf(existingTransaction?.title ?: "") }
    var amount by remember { mutableStateOf(existingTransaction?.amount?.toString()?.let { formatAmountInput(it) } ?: "") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var date by remember { mutableStateOf(existingTransaction?.date ?: PersianDateUtils.getCurrentPersianDate()) }
var time by remember { mutableStateOf(existingTransaction?.time ?: "") }
    var note by remember { mutableStateOf(existingTransaction?.note ?: "") }
    var selectedAccountId by remember { mutableStateOf(existingTransaction?.accountId) }

    // حالت ثبت بدهی / ثبت طلب
    var isDebtMode by remember { mutableStateOf(false) }

    var submitStage by remember { mutableStateOf(TransactionSubmitStage.IDLE) }
    val submitScope = rememberCoroutineScope()

    val accounts by AccountRepository.accounts.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val activeColor = if (selectedType == TransactionType.INCOME) IncomeGreen else ExpensePurple
    val categories = if (selectedType == TransactionType.EXPENSE) expenseCategories else incomeCategories

    // اگر در حالت ویرایش هستیم، دسته‌ی موجود را ست کنیم
    LaunchedEffect(existingTransaction, selectedType) {
        if (selectedCategory == null && existingTransaction != null) {
            val matching = categories.find { it.name == existingTransaction.category }
            selectedCategory = matching
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "ویرایش تراکنش" else "ثبت تراکنش",
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

            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    TypeToggle(selectedType, activeColor) {
                        selectedType = it
                        selectedCategory = null
                        isDebtMode = false
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            TitleInput(title, activeColor) { title = it }
                            Spacer(modifier = Modifier.height(14.dp))
                            AmountInput(amount, activeColor) { amount = it }
                            Spacer(modifier = Modifier.height(14.dp))

                            // دسته‌بندی — فقط در حالت بدهی/طلب غیرفعال می‌شود
                            val categoryEnabled = !isDebtMode
                            CategorySection(
                                categories = categories,
                                selectedCategory = selectedCategory,
                                activeColor = activeColor,
                                enabled = categoryEnabled
                            ) { if (categoryEnabled) selectedCategory = it }

                            Spacer(modifier = Modifier.height(14.dp))

                            DateInput(date, activeColor) { date = it }
                            Spacer(modifier = Modifier.height(14.dp))
TimeInput(time, activeColor) { time = it }
Spacer(modifier = Modifier.height(14.dp))

                            // انتخاب کارت — در حالت بدهی/طلب دیده می‌شود ولی غیرفعال است
                            val accountEnabled = !isDebtMode
                            AccountPicker(
                                accounts = accounts,
                                selectedAccountId = selectedAccountId,
                                navController = navController,
                                activeColor = activeColor,
                                enabled = accountEnabled
                            ) { if (accountEnabled) selectedAccountId = it }

                            Spacer(modifier = Modifier.height(14.dp))
                            NoteInput(note, activeColor) { note = it }

                            Spacer(modifier = Modifier.height(16.dp))

                            // کادر ویژه ثبت بدهی / ثبت طلب
                            DebtOrClaimSection(
                                selectedType = selectedType,
                                isDebtMode = isDebtMode,
                                activeColor = activeColor,
                                onToggle = { isDebtMode = it; if (it) {
                                    // انتخاب خودکار دسته بدهی/طلب
                                    selectedCategory = if (selectedType == TransactionType.EXPENSE) {
                                        expenseCategories.find { it.name == "بدهی" }
                                    } else {
                                        incomeCategories.find { it.name == "طلب" }
                                    }
                                    // کارت را روی null بگذاریم تا در مرحله ثبت بدهی/طلب انتخاب شود
                                    selectedAccountId = null
                                } }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val isNormalModeValid =
                                title.isNotBlank() && amount.isNotBlank() && selectedCategory != null && selectedAccountId != null
                            val isDebtModeValid =
                                title.isNotBlank() && amount.isNotBlank() && selectedCategory != null

                            SubmitButton(
                                activeColor = activeColor,
                                enabled = if (isDebtMode) isDebtModeValid else isNormalModeValid,
                                isEditMode = isEditMode,
                                isDebtMode = isDebtMode,
                                selectedType = selectedType,
                                submitStage = submitStage
                            ) {
    val parsedAmount = parsePersianAmount(amount)
    val finalTime = time.ifBlank { PersianDateUtils.getCurrentTime() }
    if (parsedAmount > 0) {
                                    if (isDebtMode) {
    val debtType = if (selectedType == TransactionType.EXPENSE) {
        "payable"
    } else {
        "receivable"
    }

    val safeTitle = android.net.Uri.encode(title)
    val safeDate = android.net.Uri.encode(date)
    val safeNote = android.net.Uri.encode(note)

    navController.navigate(
        "add_debt/$debtType/$safeTitle/$parsedAmount/$safeDate/$safeNote"
    )
} else {
                                        if (isEditMode) {
                                            viewModel.updateTransaction(
                                                Transaction(
                                                    id = transactionId,
                                                    title = title,
                                                    amount = parsedAmount,
                                                    type = selectedType,
                                                    category = selectedCategory!!.name,
                                                    date = date,
                                                                                                        time = finalTime,
                                                    note = note,
                                                    accountId = selectedAccountId
                                                )
                                            )
                                            navController.popBackStack()
                                        } else {
                                            submitScope.launch {
                                                submitStage = TransactionSubmitStage.LOADING
                                                delay(600)
                                                viewModel.addTransaction(
                                                    Transaction(
                                                        title = title,
                                                        amount = parsedAmount,
                                                        type = selectedType,
                                                        category = selectedCategory!!.name,
                                                        date = date,
                                                                                                                time = finalTime,
                                                        note = note,
                                                        accountId = selectedAccountId
                                                    )
                                                )
                                                submitStage = TransactionSubmitStage.SUCCESS
                                                delay(900)
                                                navController.navigate("home") {
                                                    popUpTo("home") { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

enum class TransactionSubmitStage { IDLE, LOADING, SUCCESS }

// --------- مدل دسته‌بندی و لیست‌ها ---------
data class Category(val name: String, val icon: ImageVector, val bgColor: Color)

val expenseCategories = listOf(
    Category("سوپرمارکت", Icons.Outlined.ShoppingCart, Color(0xFFF3E5F5)),
    Category("کافه", Icons.Outlined.Coffee, Color(0xFFF3E5F5)),
    Category("حمل‌ونقل", Icons.Outlined.DirectionsBus, Color(0xFFF3E5F5)),
    Category("سلامت", Icons.Outlined.HealthAndSafety, Color(0xFFF3E5F5)),
    Category("خرید", Icons.Outlined.ShoppingBag, Color(0xFFF3E5F5)),
    Category("آموزش", Icons.Outlined.School, Color(0xFFF3E5F5)),
    Category("سفر", Icons.Outlined.Flight, Color(0xFFF3E5F5)),
    Category("بدهی", Icons.Outlined.MoneyOff, Color(0xFFF3E5F5)),
    Category("قبض", Icons.Outlined.ReceiptLong, Color(0xFFF3E5F5)),
    Category("هدیه", Icons.Outlined.CardGiftcard, Color(0xFFF3E5F5)),
    Category("تفریح", Icons.Outlined.SportsEsports, Color(0xFFF3E5F5)),
    Category("دیگر", Icons.Outlined.MoreHoriz, Color(0xFFF3E5F5))
)

val incomeCategories = listOf(
    Category("حقوق", Icons.Outlined.Work, Color(0xFFE8F5E9)),
    Category("فریلنس", Icons.Outlined.Laptop, Color(0xFFE8F5E9)),
    Category("متفرقه", Icons.Outlined.MonetizationOn, Color(0xFFE8F5E9)),
    Category("سود", Icons.AutoMirrored.Outlined.TrendingUp, Color(0xFFE8F5E9)),
    Category("انعام", Icons.Outlined.RequestQuote, Color(0xFFE8F5E9)),
    Category("مغازه", Icons.Outlined.Store, Color(0xFFE8F5E9)),
    Category("طلب", Icons.Outlined.RequestQuote, Color(0xFFE8F5E9)),
    Category("فروش", Icons.Outlined.PointOfSale, Color(0xFFE8F5E9)),
    Category("هدیه", Icons.Outlined.CardGiftcard, Color(0xFFE8F5E9)),
    Category("پاداش", Icons.Outlined.EmojiEvents, Color(0xFFE8F5E9)),
    Category("سرمایه‌گذاری", Icons.Outlined.ShowChart, Color(0xFFE8F5E9)),
    Category("دیگر", Icons.Outlined.MoreHoriz, Color(0xFFE8F5E9))
)

// --------- کامپوننت‌ها ---------
@Composable
fun TypeToggle(selected: TransactionType, activeColor: Color, onSelect: (TransactionType) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TypeToggleButton(
                "درآمد",
                Icons.AutoMirrored.Outlined.TrendingUp,
                selected == TransactionType.INCOME,
                IncomeGreen,
                { onSelect(TransactionType.INCOME) },
                Modifier.weight(1f)
            )
            TypeToggleButton(
                "هزینه",
                Icons.AutoMirrored.Outlined.TrendingDown,
                selected == TransactionType.EXPENSE,
                ExpensePurple,
                { onSelect(TransactionType.EXPENSE) },
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TypeToggleButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) selectedColor else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                tint = if (selected) Color.White else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text,
                color = if (selected) Color.White else TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = Vazirmatn,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun TitleInput(value: String, activeColor: Color, onValueChange: (String) -> Unit) {
    Column {
        Text(
            "عنوان تراکنش",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontFamily = Vazirmatn,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = activeColor,
                unfocusedBorderColor = DividerColor,
                focusedContainerColor = Color(0xFFFDFDFD),
                unfocusedContainerColor = Color(0xFFFDFDFD)
            ),
            placeholder = {
                Text(
                    "مثلاً خرید لباس",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    color = TextTertiary,
                    fontSize = 14.sp,
                    fontFamily = Vazirmatn
                )
            },
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Right,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = Vazirmatn
            ),
            singleLine = true
        )
    }
}

fun formatAmountInput(raw: String): String {
    val digitsOnly = persianToEnglish(raw).filter { it.isDigit() }.take(12)
    if (digitsOnly.isEmpty()) return ""
    val withCommas = digitsOnly.reversed().chunked(3).joinToString("٬").reversed()
    return PersianDateUtils.toPersianDigits(withCommas)
}

private fun isDigitChar(c: Char) = c.isDigit() || c in '۰'..'۹'

fun formatAmountFieldValue(newValue: TextFieldValue): TextFieldValue {
    val rawText = newValue.text
    val cursorPos = newValue.selection.end.coerceIn(0, rawText.length)
    val digitsBeforeCursor = rawText.take(cursorPos).count { isDigitChar(it) }
    val formatted = formatAmountInput(rawText)

    var digitCount = 0
    var newCursorPos = formatted.length
    if (digitsBeforeCursor == 0) {
        newCursorPos = 0
    } else {
        for ((index, char) in formatted.withIndex()) {
            if (isDigitChar(char)) {
                digitCount++
                if (digitCount == digitsBeforeCursor) {
                    newCursorPos = index + 1
                    break
                }
            }
        }
    }

    return TextFieldValue(text = formatted, selection = TextRange(newCursorPos))
}

@Composable
fun AmountInput(value: String, activeColor: Color, onValueChange: (String) -> Unit) {
    var showKeypad by remember { mutableStateOf(false) }

    Column {
        Text(
            "مبلغ",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontFamily = Vazirmatn,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (value.isNotBlank()) {
                        Modifier.border(
                            width = 2.dp,
                            color = activeColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = if (value.isNotBlank()) {
                        Color.Transparent
                    } else {
                        DividerColor
                    },
                    disabledContainerColor = Color(0xFFFDFDFD),
                    disabledTextColor = TextPrimary
                ),
                placeholder = {
                    Text(
                        "۰",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right,
                        color = TextTertiary,
                        fontSize = 14.sp,
                        fontFamily = Vazirmatn
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Right,
                    fontSize = 20.sp,
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
                },
                singleLine = true
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showKeypad = true }
            )
        }

        if (showKeypad) {
            AmountKeypadDialog(
                initialAmount = value,
                accentColor = activeColor,
                onDismiss = { showKeypad = false },
                onConfirm = { newAmount ->
                    onValueChange(newAmount)
                    showKeypad = false
                }
            )
        }
    }
}

@Composable
fun CategorySection(
    categories: List<Category>,
    selectedCategory: Category?,
    activeColor: Color,
    enabled: Boolean = true,
    onSelect: (Category) -> Unit
) {
    Column {
        Text(
            "دسته‌بندی",
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) TextSecondary else TextTertiary,
            fontFamily = Vazirmatn,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowItems.forEach { category ->
                        CategoryChip(
                            category = category,
                            selected = selectedCategory?.name == category.name,
                            activeColor = activeColor,
                            enabled = enabled,
                            onClick = { if (enabled) onSelect(category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: Category,
    selected: Boolean,
    activeColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = when {
        selected -> activeColor
        isPressed && enabled -> activeColor.copy(alpha = 0.12f)
        else -> category.bgColor
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(bgColor)
                .then(
                    if (enabled) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (selected) activeColor else category.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    category.icon,
                    null,
                    tint = if (selected) Color.White else activeColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))
        Text(
            category.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) activeColor else if (enabled) TextSecondary else TextTertiary,
            fontFamily = Vazirmatn,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DateInput(value: String, activeColor: Color, onValueChange: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    var hasSelectedDate by remember(value) {
        mutableStateOf(value != PersianDateUtils.getCurrentPersianDate())
    }

    Column {
        Text(
            "تاریخ",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontFamily = Vazirmatn,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (hasSelectedDate) {
                        Modifier.border(
                            width = 2.dp,
                            color = activeColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            OutlinedTextField(
                value = PersianDateUtils.toPersianDigits(value),
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = if (hasSelectedDate) {
                        Color.Transparent
                    } else {
                        DividerColor
                    },
                    disabledContainerColor = Color(0xFFFDFDFD),
                    disabledTextColor = TextPrimary
                ),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Right,
                    fontSize = 15.sp,
                    fontFamily = Vazirmatn
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = activeColor,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showPicker = true }
            )
        }

        if (showPicker) {
            PersianDatePickerDialog(
                initialDate = value,
                accentColor = activeColor,
                onDismiss = { showPicker = false },
                onConfirm = { newDate ->
                    hasSelectedDate = true
                    onValueChange(newDate)
                    showPicker = false
                }
            )
        }
    }
}

@Composable
fun NoteInput(value: String, activeColor: Color, onValueChange: (String) -> Unit) {
    Column {
        Text(
            "توضیحات",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontFamily = Vazirmatn,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = activeColor,
                unfocusedBorderColor = DividerColor,
                focusedContainerColor = Color(0xFFFDFDFD),
                unfocusedContainerColor = Color(0xFFFDFDFD)
            ),
            placeholder = {
                Text(
                    "توضیحات تراکنش...",
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
            singleLine = true
        )
    }
}

@Composable
fun AccountPicker(
    accounts: List<Account>,
    selectedAccountId: Int?,
    navController: NavController,
    activeColor: Color,
    enabled: Boolean = true,
    onSelect: (Int?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val fieldColor = selectedAccount?.let {
        cardColorPalette[it.colorIndex % cardColorPalette.size]
    }

    Column {
        Text(
            "پرداخت از کارت",
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) TextSecondary else TextTertiary,
            fontFamily = Vazirmatn,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (accounts.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LightGreen.copy(alpha = 0.5f))
                    .clickable(enabled = enabled) { navController.navigate("cards") }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "هنوز کارتی نساختی — اول یکی بساز",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )
                Icon(Icons.Outlined.Add, null, tint = PrimaryGreen)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(fieldColor?.copy(alpha = 0.08f) ?: CardWhite)
                    .border(
                        1.dp,
                        fieldColor ?: DividerColor,
                        RoundedCornerShape(14.dp)
                    )
                    .then(
                        if (enabled) {
                            Modifier.clickable { showPicker = true }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (fieldColor != null) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(fieldColor)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = selectedAccount?.name ?: "یک کارت انتخاب کن",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedAccount != null) TextPrimary else TextTertiary,
                        fontWeight = if (selectedAccount != null) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = Vazirmatn
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.CreditCard,
                    contentDescription = null,
                    tint = fieldColor ?: TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showPicker && accounts.isNotEmpty()) {
            AccountPickerDialog(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                actionColor = activeColor,
                onDismiss = { showPicker = false },
                onConfirm = { accountId ->
                    onSelect(accountId)
                    showPicker = false
                },
                onAddNew = {
                    showPicker = false
                    navController.navigate("cards")
                }
            )
        }
    }
}

@Composable
fun DebtOrClaimSection(
    selectedType: TransactionType,
    isDebtMode: Boolean,
    activeColor: Color,
    onToggle: (Boolean) -> Unit
) {
    val title = if (selectedType == TransactionType.EXPENSE) "این هزینه را به‌عنوان بدهی ثبت کنم؟" else "این درآمد را به‌عنوان طلب ثبت کنم؟"
    val description = if (selectedType == TransactionType.EXPENSE) {
    "در این حالت، فقط بدهی ثبت می‌شود و بعداً که واقعاً پرداخت شد، از بخش بدهی‌ها تسویه‌اش می‌کنی.\n\nاز بخش طلب و بدهی قابل مشاهده است."
} else {
    "در این حالت، فقط طلب ثبت می‌شود و بعداً که واقعاً دریافت شد، از بخش طلب‌ها تسویه‌اش می‌کنی.\n\nاز بخش طلب و بدهی قابل مشاهده است."
}

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FF))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontFamily = Vazirmatn
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = Vazirmatn
                    )
                }

                Switch(
    checked = isDebtMode,
    onCheckedChange = { onToggle(it) },
    modifier = Modifier.scale(0.70f),
    colors = SwitchDefaults.colors(
        checkedTrackColor = activeColor.copy(alpha = 0.3f),
        checkedThumbColor = activeColor,
        uncheckedTrackColor = DividerColor,
        uncheckedThumbColor = Color.White
    )
)
            }
        }
    }
}

@Composable
fun SubmitButton(
    activeColor: Color,
    enabled: Boolean,
    isEditMode: Boolean = false,
    isDebtMode: Boolean = false,
    selectedType: TransactionType,
    submitStage: TransactionSubmitStage = TransactionSubmitStage.IDLE,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.96f else 1f
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) activeColor.copy(alpha = 0.85f) else activeColor,
        label = "buttonColor"
    )

    val label = when {
        isEditMode && !isDebtMode -> "ذخیره تغییرات"
        isDebtMode && selectedType == TransactionType.EXPENSE -> "ادامه برای ثبت بدهی"
        isDebtMode && selectedType == TransactionType.INCOME -> "ادامه برای ثبت طلب"
        else -> "ثبت تراکنش"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && submitStage == TransactionSubmitStage.IDLE,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (submitStage) {
                TransactionSubmitStage.LOADING -> {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("...در حال ثبت", color = Color.White, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                TransactionSubmitStage.SUCCESS -> {
                    Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ثبت شد", color = Color.White, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                TransactionSubmitStage.IDLE -> {
                    Text(
                        label,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        fontFamily = Vazirmatn,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
@Composable
fun TimeInput(value: String, activeColor: Color, onValueChange: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val hasSelectedTime = value.isNotBlank()

    Column {
        Text(
            "ساعت (اختیاری)",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontFamily = Vazirmatn,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (hasSelectedTime) Modifier.border(
                        width = 2.dp,
                        color = activeColor,
                        shape = RoundedCornerShape(14.dp)
                    ) else Modifier
                )
        ) {
            OutlinedTextField(
                value = if (hasSelectedTime) PersianDateUtils.toPersianDigits(value) else "",
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = if (hasSelectedTime) Color.Transparent else DividerColor,
                    disabledContainerColor = Color(0xFFFDFDFD),
                    disabledTextColor = TextPrimary
                ),
                placeholder = {
                    Text(
                        "اگر انتخاب نکنی، ساعت الان ثبت می‌شه",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right,
                        color = TextTertiary,
                        fontSize = 13.sp,
                        fontFamily = Vazirmatn
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Right,
                    fontSize = 15.sp,
                    fontFamily = Vazirmatn
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = if (hasSelectedTime) activeColor else TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true
            )
            Box(modifier = Modifier.matchParentSize().clickable { showPicker = true })
        }
    }

    if (showPicker) {
        TimePickerDialog(
            initialTime = value,
            accentColor = activeColor,
            onDismiss = { showPicker = false },
            onConfirm = { newTime ->
                onValueChange(newTime)
                showPicker = false
            }
        )
    }
}
