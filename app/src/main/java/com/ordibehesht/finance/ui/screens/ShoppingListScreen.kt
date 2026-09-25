package com.ordibehesht.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ordibehesht.finance.data.model.Account
import com.ordibehesht.finance.data.model.ShoppingItem
import com.ordibehesht.finance.data.model.Transaction
import com.ordibehesht.finance.data.model.TransactionType
import com.ordibehesht.finance.data.repository.AccountRepository
import com.ordibehesht.finance.data.repository.ShoppingItemRepository
import com.ordibehesht.finance.data.repository.TransactionRepository
import com.ordibehesht.finance.ui.theme.*
import com.ordibehesht.finance.ui.utils.PersianDateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(navController: NavController) {
    val items by ShoppingItemRepository.items.collectAsStateWithLifecycle()
    val accounts by AccountRepository.accounts.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val undoScope = rememberCoroutineScope()

    var newTitle by remember { mutableStateOf("") }
    var convertingItem by remember { mutableStateOf<ShoppingItem?>(null) }

    // برای امکان «برگردون» بعد از حذف آیتم لیست خرید — قبلاً برخلاف حذف تراکنش/حساب،
    // این حذف بدون هیچ فرصتی برای جبران قطعی بود
    var pendingDelete by remember { mutableStateOf<PendingUndo<ShoppingItem>?>(null) }

    fun deleteItemWithUndo(item: ShoppingItem) {
        ShoppingItemRepository.deleteItem(item)
        pendingDelete = PendingUndo(item, message = "آیتم حذف شد")
    }

    fun submitNewItem() {
        if (newTitle.isBlank()) return
        ShoppingItemRepository.addItem(
            ShoppingItem(
                title = newTitle.trim(),
                createdDate = PersianDateUtils.getCurrentPersianDate()
            )
        )
        newTitle = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "لیست خرید",
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
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 100.dp, end = 4.dp).size(56.dp)
            )
            FlowerDecoration(
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 60.dp, start = 4.dp).size(44.dp),
                color = ExpensePurple.copy(alpha = 0.08f)
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "چیزهایی که باید بخری رو اضافه کن. بعد از خرید تیک بزن و در صورت نیاز به تراکنش‌ها اضافه‌اش کن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = Vazirmatn
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "آیتم جدید",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            placeholder = {
                                Text(
                                    "مثلاً نان",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right,
                                    color = TextTertiary,
                                    fontSize = 14.sp,
                                    fontFamily = Vazirmatn
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, fontSize = 16.sp, fontFamily = Vazirmatn),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ExpensePurple,
                                unfocusedBorderColor = DividerColor,
                                focusedContainerColor = Color(0xFFFDFDFD),
                                unfocusedContainerColor = Color(0xFFFDFDFD)
                            ),
                            trailingIcon = {
                                IconButton(
                                    onClick = { submitNewItem() },
                                    enabled = newTitle.isNotBlank()
                                ) {
                                    Icon(
    imageVector = Icons.Outlined.Add,
    contentDescription = "افزودن",
    tint = if (newTitle.isBlank()) TextTertiary else ExpensePurple,
    modifier = Modifier.size(17.dp)
)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (items.isEmpty()) {
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
                                    "هنوز چیزی به لیست خرید اضافه نکردی",
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
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                items.forEachIndexed { index, item ->
                                    ShoppingItemRow(
                                        item = item,
                                        onToggle = { ShoppingItemRepository.updateItem(item.copy(isPurchased = !item.isPurchased)) },
                                        onConvert = { convertingItem = item },
                                        onDelete = { deleteItemWithUndo(item) }
                                    )
                                    if (index != items.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            color = DividerColor.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (items.isNotEmpty()) {
    Spacer(modifier = Modifier.height(12.dp))

    Text(
        "پس از خرید در افزودن به تراکنش‌ها ثبت کنید",
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
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 16.dp)) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !scrollState.canScrollForward,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    BottomNavBar(navController, currentRoute = "shopping", isScrolling = scrollState.isScrollInProgress)
                }
            }

            UndoDeleteSnackbar(
                pending = pendingDelete,
                // هم‌تراز با TransactionScreen/HomeScreen/CardsScreen (bottom = 112.dp) —
                // قبلاً 90.dp بود که پایین‌تر از بقیه‌ی صفحات می‌نشست
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 112.dp),
                onExpired = { pendingDelete = null },
                onUndo = { item ->
                    // id صفر می‌شود تا Room برای رکورد بازگردانده‌شده یک id جدید تولید کند —
                    // چون id قبلی دیگر در جدول وجود ندارد و منطقاً هم نیازی به همان id نیست.
                    // اگر این آیتم قبلاً به تراکنش تبدیل شده بود (addedToTransactions=true)،
                    // آن تراکنش هنوز با id قدیمی (که دیگر وجود ندارد) لینک است — پس بعد از
                    // ساخت رکورد جدید، shoppingItemId آن تراکنش را به id تازه اصلاح می‌کنیم تا
                    // پیوندشان بعد از Undo قطع نشود
                    val oldItemId = item.id
                    undoScope.launch {
                        val newItemId = withContext(Dispatchers.IO) {
                            ShoppingItemRepository.addItemAndGetId(item.copy(id = 0))
                        }
                        if (item.addedToTransactions) {
                            TransactionRepository.relinkShoppingItem(oldItemId, newItemId)
                        }
                    }
                    pendingDelete = null
                }
            )
        }
    }

    convertingItem?.let { item ->
        ConvertToTransactionDialog(
            item = item,
            accounts = accounts.filter { !it.isArchived },
            navController = navController,
            onDismiss = { convertingItem = null },
            onConfirm = { amount, accountId, category ->
                TransactionRepository.addTransaction(
                    Transaction(
                        title = item.title,
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        category = category,
                        date = PersianDateUtils.getCurrentPersianDate(),
                        time = PersianDateUtils.getCurrentTime(),
                        accountId = accountId,
                        shoppingItemId = item.id
                    )
                )
                ShoppingItemRepository.updateItem(item.copy(addedToTransactions = true))
                convertingItem = null
            }
        )
    }
}

@Composable
private fun CircularCheckbox(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(
                if (checked) PrimaryGreen else Color.Transparent
            )
            .border(
                1.5.dp,
                if (checked) PrimaryGreen else TextTertiary.copy(alpha = 0.5f),
                CircleShape
            )
            .clickable(
                enabled = enabled,
                onClick = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}
@Composable
private fun ShoppingItemRow(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onConvert: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularCheckbox(
    checked = item.isPurchased,
    enabled = !item.addedToTransactions,
    onCheckedChange = onToggle
)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.isPurchased) TextTertiary else TextPrimary,
            textDecoration = if (item.isPurchased) TextDecoration.LineThrough else null,
            fontWeight = FontWeight.Bold,
            fontFamily = Vazirmatn,
            modifier = Modifier.weight(1f)
        )
        when {
            item.addedToTransactions -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = PrimaryGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ثبت شد", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen, fontFamily = Vazirmatn)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            item.isPurchased -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryGreen.copy(alpha = 0.1f))
                        .clickable { onConvert() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        "افزودن به تراکنش‌ها",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Outlined.Close, "حذف", tint = TextTertiary, modifier = Modifier.size(16.dp))
        }
    }
}

private enum class ConvertToTxnStage { IDLE, LOADING, SUCCESS }

@Composable
private fun ConvertToTransactionDialog(
    item: ShoppingItem,
    accounts: List<Account>,
    navController: NavController,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, accountId: Int?, category: String) -> Unit
) {
    var amountText by remember { mutableStateOf(TextFieldValue("")) }
    var showAmountKeypad by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf<Int?>(accounts.firstOrNull()?.id) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var submitStage by remember { mutableStateOf(ConvertToTxnStage.IDLE) }
    val scope = rememberCoroutineScope()

    val parsedAmount = parsePersianAmount(amountText.text)
    val amountHasValue = amountText.text.isNotBlank()
    val isValid = parsedAmount > 0 && selectedCategory != null && selectedAccountId != null

    Dialog(onDismissRequest = { if (submitStage == ConvertToTxnStage.IDLE) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "افزودن «${item.title}» به تراکنش‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        "مبلغ",
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
                                    color = if (amountHasValue) ExpensePurple else DividerColor,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Right,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = Vazirmatn
                            ),
                            placeholder = {
                                Text(
                                    "۰",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right,
                                    color = TextTertiary,
                                    fontFamily = Vazirmatn
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color.Transparent,
                                disabledContainerColor = Color(0xFFFDFDFD),
                                disabledTextColor = TextPrimary
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
                            accentColor = ExpensePurple,
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

                    Spacer(modifier = Modifier.height(16.dp))

                    AccountPicker(
                        accounts,
                        selectedAccountId,
                        navController,
                        ExpensePurple
                    ) {
                        selectedAccountId = it
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    CategorySection(
                        expenseCategories,
                        selectedCategory,
                        ExpensePurple
                    ) {
                        selectedCategory = it
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDismiss, enabled = submitStage == ConvertToTxnStage.IDLE) {
                            Text(
                                "انصراف",
                                color = TextTertiary,
                                fontFamily = Vazirmatn
                            )
                        }

                        TextButton(
                            onClick = {
                                if (submitStage == ConvertToTxnStage.IDLE) {
                                    scope.launch {
                                        submitStage = ConvertToTxnStage.LOADING
                                        delay(700)
                                        submitStage = ConvertToTxnStage.SUCCESS
                                        delay(900)
                                        onConfirm(
                                            parsedAmount,
                                            selectedAccountId,
                                            selectedCategory!!.name
                                        )
                                    }
                                }
                            },
                            enabled = isValid && submitStage == ConvertToTxnStage.IDLE
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (submitStage) {
                                    ConvertToTxnStage.LOADING -> {
                                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = PrimaryGreen)
                                        Spacer(Modifier.width(6.dp))
                                        Text("در حال ثبت ...", color = PrimaryGreen, fontFamily = Vazirmatn, fontSize = 13.sp)
                                    }
                                    ConvertToTxnStage.SUCCESS -> {
                                        Icon(Icons.Outlined.CheckCircle, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("ثبت شد", color = PrimaryGreen, fontFamily = Vazirmatn, fontSize = 13.sp)
                                    }
                                    ConvertToTxnStage.IDLE -> {
                                        Text(
                                            "ثبت تراکنش",
                                            color = if (isValid) PrimaryGreen else TextTertiary,
                                            fontWeight = FontWeight.Bold,
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
    }
}