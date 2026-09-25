package com.ordibehesht.finance.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ordibehesht.finance.data.model.Account
import com.ordibehesht.finance.data.model.Debt
import com.ordibehesht.finance.data.model.DebtPayment
import com.ordibehesht.finance.data.model.DebtType
import com.ordibehesht.finance.data.model.Transaction
import com.ordibehesht.finance.data.model.TransactionType
import com.ordibehesht.finance.data.repository.AccountRepository
import com.ordibehesht.finance.data.repository.DebtPaymentRepository
import com.ordibehesht.finance.data.repository.DebtRepository
import com.ordibehesht.finance.data.repository.TransactionRepository
import com.ordibehesht.finance.ui.theme.*
import com.ordibehesht.finance.ui.utils.PersianDateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private fun parseSettleAmount(value: String): Long = PersianDateUtils.parsePersianAmount(value)

// نمایش مبلغ با جداکننده‌ی سه‌رقمی و ارقام فارسی (فقط برای نمایش؛ مقدار خام دست نمی‌خورد)
private fun groupedPersianDigits(amount: Long): String =
    PersianDateUtils.toPersianDigits(NumberFormat.getInstance(Locale.US).format(amount))

private enum class DebtDeleteStage { IDLE, LOADING, SUCCESS }
private enum class DebtSettleStage { IDLE, LOADING, SUCCESS }
private enum class DebtEditStage { IDLE, LOADING, SUCCESS }

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
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedPayment by remember { mutableStateOf<DebtPayment?>(null) }
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
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Outlined.Edit, "ویرایش", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        }
    ) { padding ->
        // گل‌های تزئینی پس‌زمینه — دقیقاً همان الگوی HomeScreen/TransactionScreen: دو گل کم‌رنگ
        // در بالا-راست و پایین-چپ صفحه، پشت محتوای اصلی. این صفحه قبلاً این تزئین را نداشت
        Box(modifier = Modifier.fillMaxSize()) {
            FlowerDecoration(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 100.dp, end = 4.dp).size(56.dp)
            )
            FlowerDecoration(
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 60.dp, start = 4.dp).size(44.dp),
                color = ExpensePurple.copy(alpha = 0.08f)
            )

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

                    DebtSummaryHeaderCard(
                        personName = debt.personName,
                        statusLabel = relation,
                        amount = debt.amount,
                        amountStatusLabel = if (debt.isSettled) "تسویه شده" else "باز مانده",
                        accentColor = accentColor,
                        showProgress = !debt.isSettled,
                        progressFraction = if (debt.amount > 0L) debt.paidAmount.toFloat() / debt.amount.toFloat() else 0f,
                        paidLabel = "پرداخت‌شده: ${detailAmount(debt.paidAmount)}",
                        remainingLabel = "باقی‌مانده: ${detailAmount(debt.remainingAmount)}"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DetailRow("تاریخ ثبت", PersianDateUtils.toPersianDigits(debt.createdDate), Icons.Outlined.CalendarMonth)
                        if (debt.dueDate.isNotBlank()) {
                            DetailRow("تاریخ سررسید", PersianDateUtils.toPersianDigits(debt.dueDate), Icons.Outlined.CalendarMonth)

                            // ساعت سررسید همیشه مقدار دارد (پیش‌فرض ۱۰:۰۰ اگر کاربر انتخاب نکرده باشد —
                            // ر.ک. AddDebtScreen)، پس نمایشش هم‌زمان با تاریخ سررسید منطقی است
                            if (debt.dueTime.isNotBlank()) {
                                DetailRow("ساعت سررسید", PersianDateUtils.toPersianDigits(debt.dueTime), Icons.Outlined.Schedule)
                            }

                            val dueDateNotPassed = (PersianDateUtils.daysUntil(debt.dueDate) ?: -1L) >= 0L
                            if (!debt.isSettled && dueDateNotPassed) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.NotificationsActive, null, tint = TextTertiary, modifier = Modifier.size(17.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            "یادآوری سررسید",
                                            color = TextSecondary,
                                            fontFamily = Vazirmatn,
                                            fontSize = 13.sp
                                        )
                                    }
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
                        // عنوان تراکنش مبدا (اگر این بدهی از صفحه‌ی «ثبت تراکنش» ساخته شده باشد)
                        // یک سطر کاملاً مجزا و فقط‌خواندنی است — هرگز داخل «توضیحات» تزریق نمی‌شود،
                        // پس ویرایش توضیحات همیشه فقط متن خودِ کاربر را نشان می‌دهد
                        debt.sourceTransactionTitle?.takeIf { it.isNotBlank() }?.let {
                            DetailRow("برگرفته از تراکنش", it)
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
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedPayment = payment }
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    // اگر ساعت ثبت نشده باشد (پرداخت‌های قدیمی‌تر از این فیلد)،
                                    // فقط تاریخ نمایش داده می‌شود، بدون یک ساعت جعلی/گمراه‌کننده
                                    val dateTimeText = if (payment.time.isNotBlank()) {
                                        "${PersianDateUtils.toPersianDigits(payment.date)} - ${PersianDateUtils.toPersianDigits(payment.time)}"
                                    } else {
                                        PersianDateUtils.toPersianDigits(payment.date)
                                    }
                                    Text(
                                        "پرداخت $ordinal: ${detailAmount(payment.amount)} در تاریخ $dateTimeText",
                                        color = TextSecondary,
                                        fontFamily = Vazirmatn,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )
                                    // «از کارت: X» و «شماره تراکنش: Y» (اگر توضیحات اختیاری
                                    // ثبت شده باشد) با یک خط جداکننده «|» کنار هم — دقیقاً طبق
                                    // فرمت خواسته‌شده. اگر هیچ‌کدام نبود، این سطر اصلاً نمایش
                                    // داده نمی‌شود
                                    val subtitleParts = listOfNotNull(
                                        accountName?.let { "از کارت: $it" },
                                        payment.note.takeIf { it.isNotBlank() }?.let { "شماره تراکنش: $it" }
                                    )
                                    if (subtitleParts.isNotEmpty()) {
                                        Text(
                                            subtitleParts.joinToString(" | "),
                                            color = TextTertiary,
                                            fontFamily = Vazirmatn,
                                            fontSize = 11.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }
                            }
                            // راهنما + نقطه‌ی رنگی جلوی متن — انتهای بخش تاریخچه
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "برای اصلاح یا حذف هر پرداخت، روی آن بزنید",
                                    color = TextTertiary,
                                    fontFamily = Vazirmatn,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Right
                                )
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
                    shape = RoundedCornerShape(16.dp),
                    // پیش‌فرض OutlinedButton پس‌زمینه‌ی شفاف دارد؛ روی این صفحه که گل‌های
                    // تزئینی پشت محتوا هستند، این شفافیت باعث می‌شد گل از پشت دکمه دیده شود.
                    // بوردر پیش‌فرض هم رنگ تیره/مشکی دارد؛ با DividerColor (خاکستری کم‌رنگِ
                    // همان‌طور که در بقیه‌ی اپ استفاده می‌شود) جایگزین شد
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CardWhite),
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = ExpensePurple, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("حذف این مورد", color = ExpensePurple, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
    }

    if (showEditDialog) {
        EditDebtDialog(
            debt = debt,
            accentColor = accentColor,
            onDismiss = { showEditDialog = false }
        )
    }

    selectedPayment?.let { payment ->
        PaymentActionsDialog(
            payment = payment,
            debt = debt,
            accentColor = accentColor,
            onDismiss = { selectedPayment = null }
        )
    }

    if (showDeleteDialog) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
            AlertDialog(
                onDismissRequest = {
                    if (deleteStage == DebtDeleteStage.IDLE) showDeleteDialog = false
                },
                containerColor = CardWhite,
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

                                    // توجه: حذف تراکنش‌های مرتبط اینجا لازم نیست — خودِ
                                    // DebtRepository.deleteDebt برای هر بدهی این کار را
                                    // با deleteByDebtId انجام می‌دهد
                                    debtsToDelete.forEach { DebtRepository.deleteDebt(it) }

                                    deleteStage = DebtDeleteStage.SUCCESS
                                    delay(1200)
                                    showDeleteDialog = false
                                    deleteStage = DebtDeleteStage.IDLE

                                    // اگر قسط بود و از صفحه‌ی وام آمده بودیم، صفحه‌ی وام هم بسته شود
                                    val cameFromLoanDetail = isInstallment &&
                                        navController.previousBackStackEntry?.destination?.route?.startsWith("loan_detail") == true
                                    navController.popBackStack()
                                    if (cameFromLoanDetail) navController.popBackStack()
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
            onConfirm = { settleAmount, accountId, note, paymentDate, paymentTime ->
                val newPaidAmount = (debt.paidAmount + settleAmount).coerceAtMost(debt.amount)
                val nowSettled = newPaidAmount >= debt.amount
                // ساعتِ واقعیِ انتخاب‌شده در دیالوگ (اگر کاربر انتخاب نکرده باشد، ساعتِ
                // همین لحظه‌ی تأیید جایگزین می‌شود) — نه همیشه «الان»، دقیقاً برای مواردی
                // که کاربر یادش رفته همان لحظه ثبت کند
                val now = paymentTime.ifBlank { PersianDateUtils.getCurrentTime() }

                DebtRepository.updateDebt(
                    debt.copy(paidAmount = newPaidAmount, isSettled = nowSettled)
                )

                // ابتدا رکورد پرداخت درج می‌شود تا id واقعی‌اش به دست بیاید، سپس تراکنش با همان
                // id ساخته می‌شود — این پیوند دقیق و یکتا (به‌جای match با amount+date) باعث می‌شود
                // ویرایش/حذف بعدی این پرداخت همیشه دقیقاً تراکنش درست را پیدا کند
                val paymentId = DebtPaymentRepository.addPaymentAndGetId(
                    DebtPayment(
                        debtId = debt.id,
                        amount = settleAmount,
                        date = paymentDate,
                        accountId = accountId,
                        time = now,
                        note = note
                    )
                )

                TransactionRepository.addTransaction(
                    Transaction(
                        title = if (isReceivable) "وصول طلب از ${debt.personName}" else "پرداخت بدهی به ${debt.personName}",
                        amount = settleAmount,
                        type = if (isReceivable) TransactionType.INCOME else TransactionType.EXPENSE,
                        category = if (isReceivable) "طلب" else "بدهی",
                        date = paymentDate,
                        time = now,
                        note = debt.note,
                        accountId = accountId,
                        debtId = debt.id,
                        debtPaymentId = paymentId
                    )
                )

                showSettleDialog = false
            }
        )
    }
}

// دیالوگ اکشن‌های هر پرداخت: اصلاح مبلغ یا حذف — هر دو با به‌روزرسانی اتمیک
// payment + paidAmount بدهی + تراکنش لینک‌شده
@Composable
private fun PaymentActionsDialog(
    payment: DebtPayment,
    debt: Debt,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    var showEditKeypad by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (!showEditKeypad && !showDeleteConfirm) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = CardWhite,
                title = {
                    Text(
                        "پرداخت ${detailAmount(payment.amount)}",
                        fontFamily = Vazirmatn,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        "تاریخ: ${PersianDateUtils.toPersianDigits(payment.date)}",
                        color = TextSecondary,
                        fontFamily = Vazirmatn,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showEditKeypad = true }) {
                        Text("اصلاح مبلغ", color = accentColor, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Text("حذف پرداخت", color = accentColor, fontFamily = Vazirmatn, fontSize = 13.sp)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("انصراف", color = TextSecondary, fontFamily = Vazirmatn, fontSize = 13.sp)
                        }
                    }
                }
            )
        }
    }

    if (showEditKeypad) {
        EditPaymentAmountDialog(
            payment = payment,
            debt = debt,
            accentColor = accentColor,
            onDismiss = { showEditKeypad = false },
            onDone = onDismiss
        )
    }

    if (showDeleteConfirm) {
        DeletePaymentDialog(
            payment = payment,
            debt = debt,
            accentColor = accentColor,
            onDismiss = { showDeleteConfirm = false },
            onDone = onDismiss
        )
    }
}

@Composable
private fun EditPaymentAmountDialog(
    payment: DebtPayment,
    debt: Debt,
    accentColor: Color,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    var amountText by remember { mutableStateOf(PersianDateUtils.toPersianDigits(payment.amount.toString())) }
    var showKeypad by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf(DebtEditStage.IDLE) }
    val scope = rememberCoroutineScope()

    val enteredAmount = parseSettleAmount(amountText)
    // حداکثر مجاز: باقی‌مانده + مبلغ همین پرداخت (یعنی paidAmount جدید از کل بدهی بیشتر نشود)
    val maxAllowed = debt.remainingAmount + payment.amount
    val exceedsMax = enteredAmount > maxAllowed
    val isValid = enteredAmount > 0L && !exceedsMax && enteredAmount != payment.amount

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
        AlertDialog(
            onDismissRequest = { if (stage == DebtEditStage.IDLE) onDismiss() },
            containerColor = CardWhite,
            // وقتی کیپد باز است، محتوای والد نامرئی می‌شود ولی پنجره و پرده زنده می‌مانند
            modifier = Modifier.alpha(if (showKeypad) 0f else 1f),
            title = {
                Text(
                    "اصلاح مبلغ پرداخت",
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
                        "مبلغ فعلی: ${detailAmount(payment.amount)}",
                        color = TextSecondary,
                        fontFamily = Vazirmatn,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    val hasAmount = amountText.isNotBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFDFDFD))
                            .border(
                                width = if (hasAmount) 2.dp else 1.dp,
                                color = if (hasAmount) accentColor else DividerColor,
                                shape = RoundedCornerShape(14.dp)
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
                                    if (hasAmount) groupedPersianDigits(enteredAmount) else "۰",
                                    color = if (hasAmount) TextPrimary else TextTertiary,
                                    fontFamily = Vazirmatn,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("تومان", color = TextSecondary, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                        }
                    }
                    if (exceedsMax) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "حداکثر مبلغ مجاز: ${detailAmount(maxAllowed)}",
                            color = TextTertiary,
                            fontFamily = Vazirmatn,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (stage == DebtEditStage.IDLE) {
                            scope.launch {
                                stage = DebtEditStage.LOADING
                                delay(700)

                                // ۱) به‌روزرسانی رکورد پرداخت
                                DebtPaymentRepository.updatePayment(payment.copy(amount = enteredAmount))

                                // ۲) به‌روزرسانی بدهی (paidAmount + وضعیت تسویه)
                                // کلمپ مستقیم روی خود محاسبه (نه فقط تکیه به isValid/exceedsMax
                                // که دکمه را قفل می‌کند) — دفاع دولایه، هم‌سطح با DeletePaymentDialog
                                val newPaidAmount = (debt.paidAmount - payment.amount + enteredAmount)
                                    .coerceIn(0L, debt.amount)
                                DebtRepository.updateDebt(
                                    debt.copy(
                                        paidAmount = newPaidAmount,
                                        isSettled = newPaidAmount >= debt.amount
                                    )
                                )

                                // ۳) اصلاح تراکنش لینک‌شده (اگر پیدا شود) — ابتدا با شناسه‌ی یکتای
                                // پرداخت (debtPaymentId) که همیشه دقیق است؛ اگر پرداخت قدیمی‌تر از
                                // این پچ باشد و این شناسه را نداشته باشد، به روش قدیمی (amount+date)
                                // به‌عنوان fallback می‌رویم
                                val linkedTxn = TransactionRepository.transactions.value.find {
                                    it.debtPaymentId == payment.id
                                } ?: TransactionRepository.transactions.value.find {
                                    it.debtId == debt.id && it.debtPaymentId == null &&
                                        it.amount == payment.amount && it.date == payment.date
                                }
                                linkedTxn?.let {
                                    TransactionRepository.updateTransaction(it.copy(amount = enteredAmount))
                                }

                                stage = DebtEditStage.SUCCESS
                                delay(900)
                                stage = DebtEditStage.IDLE
                                onDone()
                            }
                        }
                    },
                    enabled = isValid && stage == DebtEditStage.IDLE
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (stage) {
                            DebtEditStage.LOADING -> {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = accentColor)
                                Spacer(Modifier.width(6.dp))
                                Text("...در حال اصلاح", color = accentColor, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                            DebtEditStage.SUCCESS -> {
                                Icon(Icons.Outlined.CheckCircle, null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("اصلاح شد", color = IncomeGreen, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                            DebtEditStage.IDLE -> {
                                Text(
                                    "ذخیره",
                                    color = if (isValid) accentColor else TextTertiary,
                                    fontFamily = Vazirmatn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = stage == DebtEditStage.IDLE) {
                    Text("انصراف", color = TextSecondary, fontFamily = Vazirmatn, fontSize = 13.sp)
                }
            }
        )
    }

    // کیپد بدون پرده‌ی دوم روی همین دیالوگ باز می‌شود — بدون پرش
    if (showKeypad) {
        AmountKeypadDialog(
            initialAmount = amountText,
            accentColor = accentColor,
            onDismiss = { showKeypad = false },
            onConfirm = { newAmount ->
                // کلمپ مستقیم: اگر مبلغ واردشده از سقف مجاز (باقی‌مانده + مبلغ همین پرداخت)
                // بیشتر باشد، همان‌جا به سقف محدود می‌شود؛ محافظت فقط با غیرفعال‌بودن دکمه
                // کافی نیست چون مقدار نامعتبر همچنان در state و روی صفحه می‌ماند
                val parsed = parseSettleAmount(newAmount)
                amountText = if (parsed > maxAllowed) {
                    PersianDateUtils.toPersianDigits(maxAllowed.toString())
                } else {
                    newAmount
                }
                showKeypad = false
            },
            dimBehind = false
        )
    }
}

@Composable
private fun DeletePaymentDialog(
    payment: DebtPayment,
    debt: Debt,
    accentColor: Color,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    var stage by remember { mutableStateOf(DebtEditStage.IDLE) }
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
        AlertDialog(
            onDismissRequest = { if (stage == DebtEditStage.IDLE) onDismiss() },
            containerColor = CardWhite,
            title = {
                Text(
                    "حذف پرداخت",
                    fontFamily = Vazirmatn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    "پرداخت ${detailAmount(payment.amount)} حذف می‌شود، از مبلغ پرداخت‌شده کم می‌شود و تراکنش مرتبطش هم پاک می‌شود. مطمئنی؟",
                    fontFamily = Vazirmatn,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (stage == DebtEditStage.IDLE) {
                            scope.launch {
                                stage = DebtEditStage.LOADING
                                delay(700)

                                // ۱) حذف رکورد پرداخت
                                DebtPaymentRepository.deletePayment(payment)

                                // ۲) کم کردن از paidAmount بدهی
                                val newPaidAmount = (debt.paidAmount - payment.amount).coerceAtLeast(0L)
                                DebtRepository.updateDebt(
                                    debt.copy(
                                        paidAmount = newPaidAmount,
                                        isSettled = newPaidAmount >= debt.amount
                                    )
                                )

                                // ۳) حذف تراکنش لینک‌شده (اگر پیدا شود) — ابتدا با شناسه‌ی یکتای پرداخت،
                                // با fallback به amount+date برای پرداخت‌های قدیمی‌تر از این پچ
                                val linkedTxn = TransactionRepository.transactions.value.find {
                                    it.debtPaymentId == payment.id
                                } ?: TransactionRepository.transactions.value.find {
                                    it.debtId == debt.id && it.debtPaymentId == null &&
                                        it.amount == payment.amount && it.date == payment.date
                                }
                                linkedTxn?.let { TransactionRepository.deleteTransaction(it) }

                                stage = DebtEditStage.SUCCESS
                                delay(900)
                                stage = DebtEditStage.IDLE
                                onDone()
                            }
                        }
                    },
                    enabled = stage == DebtEditStage.IDLE
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (stage) {
                            DebtEditStage.LOADING -> {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = accentColor)
                                Spacer(Modifier.width(6.dp))
                                Text("...در حال حذف", color = accentColor, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                            DebtEditStage.SUCCESS -> {
                                Icon(Icons.Outlined.CheckCircle, null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("حذف شد", color = IncomeGreen, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                            DebtEditStage.IDLE -> {
                                Text("حذف کن", color = accentColor, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = stage == DebtEditStage.IDLE) {
                    Text("انصراف", color = TextSecondary, fontFamily = Vazirmatn, fontSize = 13.sp)
                }
            }
        )
    }
}

// فیلد متنی دیالوگ ویرایش: خاکستری تا وقتی مقدارش عوض نشده؛
// بعد از اولین تغییر، سبز/بنفش پررنگ (چه فوکوس داشته باشه چه نه)
@Composable
private fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    originalValue: String,
    accentColor: Color,
    singleLine: Boolean = true
) {
    val changed = value != originalValue
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isActive = changed || isFocused
    val borderColor by animateColorAsState(
        targetValue = if (isActive) accentColor else DividerColor,
        label = "editFieldBorder"
    )

    Column {
        Text(
            label,
            fontSize = 12.sp,
            fontFamily = Vazirmatn,
            color = if (isActive) accentColor else TextTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .border(if (isActive) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                interactionSource = interactionSource,
                singleLine = singleLine,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, fontFamily = Vazirmatn, fontSize = 15.sp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = accentColor,
                    focusedContainerColor = Color(0xFFFDFDFD),
                    unfocusedContainerColor = Color(0xFFFDFDFD)
                )
            )
        }
    }
}

// ویرایش مستقل بدهی/طلب/قسط: نام، مبلغ (با شرط حداقل = پرداخت‌شده)، سررسید و توضیحات.
// نوع (طلب/بدهی) و ساختار وام قابل ویرایش نیستند — برای آن‌ها حذف و ثبت مجدد.
@Composable
private fun EditDebtDialog(
    debt: Debt,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    var nameText by remember { mutableStateOf(debt.personName) }
    var amountText by remember { mutableStateOf(PersianDateUtils.toPersianDigits(debt.amount.toString())) }
    // تاریخ ثبت — برای مواردی که چند روز یادمان رفته درست ثبتش کنیم
    var createdDateText by remember { mutableStateOf(debt.createdDate) }
    var showCreatedDatePicker by remember { mutableStateOf(false) }
    // ساعت ثبت — همراه فیلد تاریخ ثبت بالا، برای همان کاربرد: وقتی چند روز یادمان
    // رفته ثبت کنیم، هم تاریخ و هم ساعتِ واقعیِ رویداد قابل ویرایش باشد
    var createdTimeText by remember { mutableStateOf(debt.createdTime) }
    var showCreatedTimePicker by remember { mutableStateOf(false) }
    var dueDateText by remember { mutableStateOf(debt.dueDate) }
    var dueTimeText by remember { mutableStateOf(debt.dueTime) }
    var noteText by remember { mutableStateOf(debt.note) }
    var showAmountKeypad by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editStage by remember { mutableStateOf(DebtEditStage.IDLE) }
    val scope = rememberCoroutineScope()

    val enteredAmount = parseSettleAmount(amountText)
    // قانون: مبلغ جدید نباید از مبلغ پرداخت‌شده کمتر باشد
    val belowPaid = enteredAmount < debt.paidAmount
    val isValid = nameText.isNotBlank() && enteredAmount > 0L && !belowPaid && createdDateText.isNotBlank()

    // وضعیت «تغییر کرده» برای هر فیلد — تا عوض نشده، بوردر خاکستری می‌ماند
    val nameChanged = nameText.trim() != debt.personName
    val amountChanged = enteredAmount != debt.amount
    val createdDateChanged = createdDateText != debt.createdDate
    val createdTimeChanged = createdTimeText != debt.createdTime
    val dateChanged = dueDateText != debt.dueDate
    val timeChanged = dueTimeText != debt.dueTime
    val noteChanged = noteText.trim() != debt.note
    val hasChanges = nameChanged || amountChanged || createdDateChanged || createdTimeChanged ||
        dateChanged || timeChanged || noteChanged

    // وقتی کیپد یا تقویم باز است، محتوای والد نامرئی می‌شود ولی پنجره و پرده زنده می‌مانند
    val childOpen = showAmountKeypad || showDatePicker || showTimePicker || showCreatedDatePicker ||
        showCreatedTimePicker

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
        AlertDialog(
            onDismissRequest = { if (editStage == DebtEditStage.IDLE) onDismiss() },
            containerColor = CardWhite,
            modifier = Modifier.alpha(if (childOpen) 0f else 1f),
            title = {
                // Rtl تا گیومه‌ها («») درست رندر شوند
                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        "ویرایش «${debt.personName}»",
                        fontFamily = Vazirmatn,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            text = {
                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column {
                        EditField(
                            value = nameText,
                            onValueChange = { nameText = it },
                            label = "نام شخص",
                            originalValue = debt.personName,
                            accentColor = accentColor
                        )

                        Spacer(Modifier.height(10.dp))

                        // مبلغ (با کیپد اختصاصی) — نمایش سه‌رقم‌سه‌رقم + لیبل
                        Text(
                            "مبلغ",
                            fontSize = 12.sp,
                            fontFamily = Vazirmatn,
                            color = if (amountChanged) accentColor else TextTertiary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFDFDFD))
                                .border(
                                    width = if (amountChanged) 2.dp else 1.dp,
                                    color = if (amountChanged) accentColor else DividerColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { showAmountKeypad = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    groupedPersianDigits(enteredAmount),
                                    color = TextPrimary,
                                    fontFamily = Vazirmatn,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("تومان", color = TextSecondary, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                        }
                        if (belowPaid) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "نمی‌تواند کمتر از مبلغ پرداخت‌شده (${detailAmount(debt.paidAmount)}) باشد",
                                color = TextTertiary,
                                fontFamily = Vazirmatn,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // تاریخ ثبت + لیبل — دقیقاً همان الگوی بصری «تاریخ سررسید» زیر همین
                        // فیلد، طبق درخواست، درست زیر فیلد مبلغ
                        Text(
                            "انتخاب زمان ثبت",
                            fontSize = 12.sp,
                            fontFamily = Vazirmatn,
                            color = if (createdDateChanged) accentColor else TextTertiary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFDFDFD))
                                .border(
                                    width = if (createdDateChanged) 2.dp else 1.dp,
                                    color = if (createdDateChanged) accentColor else DividerColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { showCreatedDatePicker = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.CalendarMonth, null,
                                    tint = if (createdDateChanged) accentColor else TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (createdDateText.isNotBlank()) PersianDateUtils.toPersianDigits(createdDateText)
                                    else "انتخاب زمان ثبت",
                                    color = if (createdDateText.isBlank()) TextTertiary else TextPrimary,
                                    fontFamily = Vazirmatn,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // ساعت ثبت + لیبل — همراه فیلد تاریخ ثبت بالا (اگر خالی بماند، هنگام
                        // ذخیره ساعت واقعیِ لحظه‌ی ثبت به‌جایش می‌نشیند، نه اینکه خالی بماند)
                        Text(
                            "انتخاب ساعت ثبت",
                            fontSize = 12.sp,
                            fontFamily = Vazirmatn,
                            color = if (createdTimeChanged) accentColor else TextTertiary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFDFDFD))
                                .border(
                                    width = if (createdTimeChanged) 2.dp else 1.dp,
                                    color = if (createdTimeChanged) accentColor else DividerColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { showCreatedTimePicker = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Schedule, null,
                                    tint = if (createdTimeChanged) accentColor else TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (createdTimeText.isNotBlank()) PersianDateUtils.toPersianDigits(createdTimeText)
                                    else "انتخاب ساعت ثبت",
                                    color = if (createdTimeText.isBlank()) TextTertiary else TextPrimary,
                                    fontFamily = Vazirmatn,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // تاریخ سررسید + لیبل (بدون دکمه‌ی حذف — اگر عوض نشود، همان تاریخ اصلی می‌ماند)
                        Text(
                            "تاریخ سررسید",
                            fontSize = 12.sp,
                            fontFamily = Vazirmatn,
                            color = if (dateChanged) accentColor else TextTertiary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFDFDFD))
                                .border(
                                    width = if (dateChanged) 2.dp else 1.dp,
                                    color = if (dateChanged) accentColor else DividerColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.CalendarMonth, null,
                                    tint = if (dateChanged) accentColor else TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (dueDateText.isNotBlank()) PersianDateUtils.toPersianDigits(dueDateText)
                                    else "بدون سررسید",
                                    color = if (dueDateText.isBlank()) TextTertiary else TextPrimary,
                                    fontFamily = Vazirmatn,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // ساعت سررسید + لیبل (اگر خالی بماند، یادآوری ساعت ۱۰ صبح فعال می‌شود)
                        Text(
                            "ساعت سررسید (پیش‌فرض ۱۰ صبح)",
                            fontSize = 12.sp,
                            fontFamily = Vazirmatn,
                            color = if (timeChanged) accentColor else TextTertiary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFDFDFD))
                                .border(
                                    width = if (timeChanged) 2.dp else 1.dp,
                                    color = if (timeChanged) accentColor else DividerColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { showTimePicker = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Schedule, null,
                                    tint = if (timeChanged) accentColor else TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (dueTimeText.isNotBlank()) PersianDateUtils.toPersianDigits(dueTimeText)
                                    else "بدون ساعت مشخص",
                                    color = if (dueTimeText.isBlank()) TextTertiary else TextPrimary,
                                    fontFamily = Vazirmatn,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        EditField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = "توضیحات",
                            originalValue = debt.note,
                            accentColor = accentColor,
                            singleLine = false
                        )

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "اگر مبلغ به اندازه‌ی پرداخت‌شده یا کمتر برسد، این مورد تسویه‌شده علامت می‌خورد.",
                            color = TextTertiary,
                            fontFamily = Vazirmatn,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editStage == DebtEditStage.IDLE) {
                            scope.launch {
                                editStage = DebtEditStage.LOADING
                                delay(700)
                                DebtRepository.updateDebt(
                                    debt.copy(
                                        personName = nameText.trim(),
                                        amount = enteredAmount,
                                        createdDate = createdDateText,
                                        // اگر کاربر ساعت ثبت را خالی بگذارد، ساعتِ واقعیِ همین
                                        // لحظه‌ی ذخیره جایگزین می‌شود — هماهنگ با رفتار پیش‌فرض
                                        // فیلد ساعت ثبت در AddDebtScreen
                                        createdTime = createdTimeText.ifBlank { PersianDateUtils.getCurrentTime() },
                                        dueDate = dueDateText,
                                        // اگر کاربر ساعت را خالی بگذارد، پیش‌فرض ثابت ۱۰:۰۰ صبح
                                        // ذخیره می‌شود — هماهنگ با همین منطق در AddDebtScreen،
                                        // تا مقدار dueTime در دیتابیس همیشه صریح باشد نه خالی
                                        dueTime = dueTimeText.ifBlank { "10:00" },
                                        note = noteText.trim(),
                                        isSettled = debt.paidAmount >= enteredAmount
                                    )
                                )
                                editStage = DebtEditStage.SUCCESS
                                delay(900)
                                editStage = DebtEditStage.IDLE
                                onDismiss()
                            }
                        }
                    },
                    enabled = isValid && hasChanges && editStage == DebtEditStage.IDLE
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (editStage) {
                            DebtEditStage.LOADING -> {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = accentColor)
                                Spacer(Modifier.width(6.dp))
                                Text("...در حال ذخیره", color = accentColor, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                            DebtEditStage.SUCCESS -> {
                                Icon(Icons.Outlined.CheckCircle, null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ذخیره شد", color = IncomeGreen, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                            DebtEditStage.IDLE -> {
                                Text(
                                    "ذخیره",
                                    color = if (isValid && hasChanges) accentColor else TextTertiary,
                                    fontFamily = Vazirmatn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = editStage == DebtEditStage.IDLE) {
                    Text("انصراف", color = TextSecondary, fontFamily = Vazirmatn, fontSize = 13.sp)
                }
            }
        )
    }

    // کیپد بدون پرده‌ی دوم روی همین دیالوگ باز می‌شود — بدون پرش
    if (showAmountKeypad) {
        AmountKeypadDialog(
            initialAmount = amountText,
            accentColor = accentColor,
            onDismiss = { showAmountKeypad = false },
            onConfirm = { newAmount ->
                amountText = newAmount
                showAmountKeypad = false
            },
            dimBehind = false
        )
    }

    // تقویم هم بدون پرده‌ی دوم روی همین دیالوگ باز می‌شود — بدون پرش
    if (showDatePicker) {
        PersianDatePickerDialog(
            initialDate = dueDateText,
            accentColor = accentColor,
            onDismiss = { showDatePicker = false },
            onConfirm = { newDate ->
                dueDateText = newDate
                showDatePicker = false
            },
            dimBehind = false
        )
    }

    if (showCreatedDatePicker) {
        PersianDatePickerDialog(
            initialDate = createdDateText,
            accentColor = accentColor,
            onDismiss = { showCreatedDatePicker = false },
            onConfirm = { newDate ->
                createdDateText = newDate
                showCreatedDatePicker = false
            },
            dimBehind = false
        )
    }

    if (showCreatedTimePicker) {
        TimePickerDialog(
            initialTime = createdTimeText,
            accentColor = accentColor,
            onDismiss = { showCreatedTimePicker = false },
            onConfirm = { newTime ->
                createdTimeText = newTime
                showCreatedTimePicker = false
            }
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialTime = dueTimeText,
            accentColor = accentColor,
            onDismiss = { showTimePicker = false },
            onConfirm = { newTime ->
                dueTimeText = newTime
                showTimePicker = false
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
    onConfirm: suspend (Long, Int?, String, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var showKeypad by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf<Int?>(null) }
    // توضیحات اختیاری (مثلاً شماره‌ی تراکنش بانکی) — کاملاً اختیاری، در isValid شرط نیست
    var noteText by remember { mutableStateOf("") }
    // تاریخ واقعیِ دریافت/پرداخت — پیش‌فرض امروز، ولی قابل تغییر برای پرداخت‌هایی که همان روز
    // ثبت نشده‌اند (مثلاً چند روز بعد یادمان افتاده که ثبتش کنیم)
    var settleDate by remember { mutableStateOf(PersianDateUtils.getCurrentPersianDate()) }
    // ساعت واقعیِ همین پرداخت/دریافت — کنار تاریخ بالا، برای همان مورد؛ اگر کاربر چیزی
    // انتخاب نکند، هنگام تأیید خودِ ساعتِ الان جایگزین می‌شود (نه اینجا)
    var settleTime by remember { mutableStateOf("") }
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
            // وقتی کیپد باز است، محتوای والد نامرئی می‌شود ولی پنجره و پرده زنده می‌مانند
            modifier = Modifier.alpha(if (showKeypad) 0f else 1f),
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
                            selectableAccounts = accounts.filter { !it.isArchived },
                            onSelect = { selectedAccountId = it }
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    // تاریخ واقعی این پرداخت/دریافت — پیش‌فرض امروز، ولی قابل ویرایش برای
                    // موردی که همان روز ثبت نشده (مثلاً چند روز بعد یادمان افتاده). دقیقاً
                    // همان DateInput استفاده‌شده در AddTransactionScreen، برای یکدستی کامل
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                        DateInput(
                            value = settleDate,
                            activeColor = accentColor,
                            onValueChange = { settleDate = it }
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    // ساعت واقعی این پرداخت/دریافت — کنار تاریخ بالا، برای همان کاربرد؛
                    // دقیقاً همان TimeInput استفاده‌شده در AddTransactionScreen. اگر انتخاب
                    // نشود، هنگام تأیید ساعت واقعیِ لحظه‌ی ثبت جایگزین می‌شود
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                        TimeInput(
                            value = settleTime,
                            activeColor = accentColor,
                            onValueChange = { settleTime = it }
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    // فیلد کاملاً اختیاری — نه در isValid شرط است، نه اجباری برای تسویه.
                    // برای مواردی مثل ثبت شماره‌ی تراکنش بانکی که کاربر می‌خواهد بعداً از
                    // تاریخچه‌ی پرداخت‌ها پیدایش کند
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "توضیحات (اختیاری) — مثلاً شماره تراکنش",
                                    color = TextTertiary,
                                    fontSize = 12.sp,
                                    fontFamily = Vazirmatn
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Right,
                                fontSize = 13.sp,
                                fontFamily = Vazirmatn
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = DividerColor
                            )
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
                                onConfirm(enteredAmount, selectedAccountId, noteText.trim(), settleDate, settleTime)
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

    // کیپد بدون پرده‌ی دوم روی همین دیالوگ باز می‌شود — بدون پرش
    if (showKeypad) {
        AmountKeypadDialog(
            initialAmount = amountText,
            accentColor = accentColor,
            onDismiss = { showKeypad = false },
            onConfirm = { newAmount ->
                amountText = newAmount
                showKeypad = false
            },
            dimBehind = false
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
