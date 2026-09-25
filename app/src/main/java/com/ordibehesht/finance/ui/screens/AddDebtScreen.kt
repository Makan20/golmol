package com.ordibehesht.finance.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ordibehesht.finance.data.model.Debt
import com.ordibehesht.finance.data.model.DebtType as DataDebtType
import com.ordibehesht.finance.data.repository.DebtRepository
import com.ordibehesht.finance.ui.theme.BackgroundLight
import com.ordibehesht.finance.ui.theme.CardWhite
import com.ordibehesht.finance.ui.theme.DividerColor
import com.ordibehesht.finance.ui.theme.ExpensePurple
import com.ordibehesht.finance.ui.theme.IncomeGreen
import com.ordibehesht.finance.ui.theme.TextPrimary
import com.ordibehesht.finance.ui.theme.TextSecondary
import com.ordibehesht.finance.ui.theme.TextTertiary
import com.ordibehesht.finance.ui.theme.Vazirmatn
import com.ordibehesht.finance.ui.utils.PersianDateUtils

private fun parseDebtAmount(value: String): Long = PersianDateUtils.parsePersianAmount(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtScreen(
    navController: NavController,
    initialType: String = "receivable",
    initialTitle: String = "",
    initialAmount: Long = 0L,
    initialDate: String = "",
    initialNote: String = ""
) {
    var debtType by remember(initialType) {
        mutableStateOf(if (initialType == "payable") DataDebtType.PAYABLE else DataDebtType.RECEIVABLE)
    }
    // اگر از صفحه‌ی ثبت تراکنش با نوع مشخص وارد شده باشیم، سوییچ تغییر نوع نمایش داده نمی‌شود
    val cameFromTransaction = initialTitle.isNotBlank() || initialAmount > 0L
    var personName by remember { mutableStateOf("") }
    var amount by remember(initialAmount) {
        mutableStateOf(if (initialAmount > 0L) PersianDateUtils.toPersianDigits(initialAmount.toString()) else "")
    }
    var dueDate by remember { mutableStateOf("") }
        var dueTime by remember { mutableStateOf("") }
    // تاریخ ثبت (createdDate) — پیش‌فرض امروز، ولی قابل ویرایش برای مواردی که چند روز
    // یادمان رفته همان روز ثبت کنیم. قبلاً این مقدار کاملاً پنهان بود و همیشه بی‌صدا امروز
    // در نظر گرفته می‌شد (مگر از مسیر «تراکنش → بدهی» که initialDate را از قبل می‌فرستد)
    var createdDateField by remember(initialDate) {
        mutableStateOf(initialDate.ifBlank { PersianDateUtils.getCurrentPersianDate() })
    }
    // ساعت ثبت (createdTime) — کنار تاریخ ثبت، برای همان مورد: وقتی چند روز یادمان رفته
    // ثبت کنیم، هم تاریخ و هم ساعتِ واقعیِ رویداد قابل تنظیم باشد، نه فقط لحظه‌ی ذخیره.
    // اگر کاربر چیزی انتخاب نکند، هنگام ذخیره خودِ ساعتِ الان جایگزین می‌شود (نه اینجا)
    var createdTimeField by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(true) }
    var submitStage by remember { mutableStateOf(DebtSubmitStage.IDLE) }
    val submitScope = rememberCoroutineScope()
    var isInstallment by remember { mutableStateOf(false) }
    var installmentCountText by remember { mutableStateOf("") }
    var installmentDayText by remember { mutableStateOf("") }
    var description by remember(initialNote) { mutableStateOf(initialNote) }

    val accentColor = if (debtType == DataDebtType.RECEIVABLE) IncomeGreen else ExpensePurple
    val parsedAmount = parseDebtAmount(amount)
    val title = if (debtType == DataDebtType.RECEIVABLE) "ثبت طلب" else "ثبت بدهی"

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontFamily = Vazirmatn) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "بازگشت", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            FlowerDecoration(Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 4.dp).size(60.dp))
            FlowerDecoration(
                Modifier.align(Alignment.BottomStart).padding(bottom = 32.dp, start = 4.dp).size(50.dp),
                color = ExpensePurple.copy(alpha = 0.08f)
            )
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Spacer(Modifier.height(8.dp))
                    if (!cameFromTransaction) {
                        DebtTypeToggle(selected = debtType, onSelect = { debtType = it })
                    }
                    AddDebtHeader(debtType)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                if (debtType == DataDebtType.RECEIVABLE) "اطلاعات طلب" else "اطلاعات بدهی",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Vazirmatn
                            )
                            DebtTextField(personName, { personName = it }, "نام شخص", "مثلاً پیمان", accentColor)
                            ReadOnlyAmount(amount, accentColor) { amount = it }

                            // تاریخ ثبت — برای مواردی که چند روز یادمان رفته همان روز ثبت
                            // کنیم؛ دقیقاً همان DebtDateInput فیلد سررسید، فقط با برچسب و
                            // state جدا، درست زیر فیلد مبلغ طبق درخواست
                            DebtDateInput(
                                createdDateField,
                                accentColor,
                                label = "انتخاب زمان ثبت",
                                placeholder = "انتخاب زمان ثبت",
                                clearOnDismissWithoutSelection = false
                            ) { createdDateField = it }
                            // ساعت ثبت — همراه فیلد تاریخ ثبت بالا، برای همان کاربرد: وقتی
                            // چند روز یادمان رفته ثبت کنیم. اگر انتخاب نشود، هنگام ذخیره
                            // ساعت واقعیِ لحظه‌ی ثبت جایگزین می‌شود (مثل رفتار فیلد ساعت در
                            // ثبت تراکنش)
                            DebtTimeInput(
                                createdTimeField,
                                accentColor,
                                label = "انتخاب ساعت ثبت",
                                placeholder = "اگر انتخاب نکنی، ساعت الان ثبت می‌شه"
                            ) { createdTimeField = it }

                            // اگر از صفحه‌ی ثبت تراکنش آمده باشیم، عنوان همان تراکنش را فقط
                            // برای اطلاع نشان می‌دهیم — جدا از فیلد توضیحات، تا کاربر مجبور
                            // نباشد یک متن ترکیبیِ «عنوان: ...\nتوضیحات: ...» را دستکاری کند
                            if (initialTitle.isNotBlank()) {
                                Text(
                                    "عنوان تراکنش: $initialTitle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontFamily = Vazirmatn,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            InstallmentToggleRow(
                                checked = isInstallment,
                                accentColor = accentColor,
                                onCheckedChange = { isInstallment = it }
                            )

                            if (isInstallment) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SimpleNumberField(
                                        value = installmentCountText,
                                        activeColor = accentColor,
                                        label = "تعداد اقساط",
                                        placeholder = "مثلاً ۱۰",
                                        dialogTitle = "تعداد اقساط",
                                        maxDigits = 2,
                                        modifier = Modifier.weight(1f),
                                        onValueChange = { installmentCountText = it }
                                    )
                                    SimpleNumberField(
                                        value = installmentDayText,
                                        activeColor = accentColor,
                                        label = "روز ثابت هر ماه",
                                        placeholder = "مثلاً ۵",
                                        dialogTitle = "روز ثابت هر ماه",
                                        maxDigits = 2,
                                        modifier = Modifier.weight(1f),
                                        onValueChange = { installmentDayText = it }
                                    )
                                }
                            }

                            DebtDateInput(dueDate, accentColor, label = if (isInstallment) "تاریخ سررسید قسط اول" else "تاریخ سررسید") { dueDate = it }
DebtTimeInput(dueTime, accentColor, label = if (isInstallment) "ساعت سررسید اقساط (پیش‌فرض ۱۰ صبح)" else "ساعت سررسید (پیش‌فرض ۱۰ صبح)") { dueTime = it }
                            // سررسید «امروز» هم مجاز است (مثلاً برای ثبت بدهی‌ای که همین الان
                            // سررسید شده)؛ فقط تاریخ‌های گذشته (قبل از امروز) رد می‌شوند
                            if (dueDate.isNotBlank() && (PersianDateUtils.daysUntil(dueDate) ?: 0L) < 0L) {
                                Text(
                                    "تاریخ سررسید نمی‌تواند قبل از امروز باشد",
                                    color = ExpensePurple,
                                    fontFamily = Vazirmatn,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (dueDate.isNotBlank() && (PersianDateUtils.daysUntil(dueDate) ?: -1L) >= 0L) {
                                ReminderToggleRow(
                                    checked = reminderEnabled,
                                    accentColor = accentColor,
                                    label = if (isInstallment) "یادآوری سررسید (برای همه‌ی اقساط)" else "یادآوری سررسید",
                                    onCheckedChange = { reminderEnabled = it }
                                )
                            }
                            DebtTextField(
                                description,
                                { description = it },
                                "توضیحات (اختیاری)",
                                "مثلاً قرض برای خرید",
                                accentColor,
                                singleLine = false
                            )
                        }
                    }
                    // سررسید «امروز» (daysUntil == 0) مجاز است؛ فقط تاریخ گذشته رد می‌شود —
                    // هماهنگ با DebtDetailScreen که در ویرایش هم همین قانون را دارد (بدون این
                    // محدودیت، تا امروز مجاز باشد)
                    val dueDateIsValid = dueDate.isBlank() || (PersianDateUtils.daysUntil(dueDate) ?: 0L) >= 0L
                    val installmentCount = installmentCountText.toIntOrNull() ?: 0
                    val installmentDay = installmentDayText.toIntOrNull() ?: 0
                    val installmentFieldsValid = !isInstallment ||
                        (installmentCount in 2..60 && installmentDay in 1..31)


                    // یادداشت نهایی فقط متن خودِ کاربر است — دیگر هیچ‌وقت با عنوان تراکنش مبدا
                    // ترکیب نمی‌شود، نه در لحظه‌ی ثبت و نه بعداً. عنوان تراکنش مبدا (اگر بود) در
                    // فیلد مستقل sourceTransactionTitle ذخیره می‌شود و در DebtDetailScreen به‌صورت
                    // یک سطر مجزا و فقط‌خواندنی نمایش داده می‌شود — پس اگر کاربر بعداً از آن صفحه
                    // «توضیحات» را ویرایش کند، فقط متن خودش را می‌بیند، نه یک ساختار تزریق‌شده
                    val finalNote = description.trim()
                    val sourceTitle = initialTitle.takeIf { it.isNotBlank() }
                    // ساعت واقعیِ همین لحظه‌ی ثبت — به‌عنوان مقدار پیش‌فرض وقتی کاربر از
                    // فیلد «انتخاب ساعت ثبت» بالا چیزی انتخاب نکرده باشد؛ اگر initialDate از
                    // امروز متفاوت باشد (یعنی کاربر تاریخ ثبت را دستی عوض کرده) باز هم این
                    // مقدار پیش‌فرض همان ساعت واقعی الان است، نه چیز دیگری
                    val nowTime = PersianDateUtils.getCurrentTime()

                    Button(
                        onClick = {
                            if (submitStage == DebtSubmitStage.IDLE) {
                                submitScope.launch {
                                    submitStage = DebtSubmitStage.LOADING
                                    delay(600)
                                    if (isInstallment) {
                                        DebtRepository.addLoan(
                                            personName = personName.trim(),
                                            totalAmount = parsedAmount,
                                            type = debtType,
                                            installmentCount = installmentCount,
                                            firstDueDate = dueDate,
                                            // اگر کاربر ساعتی انتخاب نکند، پیش‌فرض ثابت ۱۰:۰۰ صبح
                                            // استفاده می‌شود — نه ساعت لحظه‌ی ثبت، چون آن می‌تواند
                                            // هر ساعتی از شبانه‌روز باشد (مثلاً نیمه‌شب) و یادآوری
                                            // را در همان ساعت نامناسب زمان‌بندی می‌کرد
                                            dueTime = dueTime.ifBlank { "10:00" },
                                            installmentDayOfMonth = installmentDay,
                                            note = finalNote,
                                            createdDate = createdDateField,
                                            reminderEnabled = reminderEnabled,
                                            sourceTransactionTitle = sourceTitle,
                                            createdTime = createdTimeField.ifBlank { nowTime }
                                        )
                                    } else {
                                        DebtRepository.addDebt(
                                            Debt(
                                                personName = personName.trim(),
                                                amount = parsedAmount,
                                                type = debtType,
                                                dueDate = dueDate,
                                                // همان دلیل بالا: پیش‌فرض ثابت ۱۰:۰۰ صبح، نه ساعت ثبت
                                                dueTime = dueTime.ifBlank { "10:00" },
                                                note = finalNote,
                                                createdDate = createdDateField,
                                                reminderEnabled = reminderEnabled,
                                                sourceTransactionTitle = sourceTitle,
                                                createdTime = createdTimeField.ifBlank { nowTime }
                                            )
                                        )
                                    }
                                    submitStage = DebtSubmitStage.SUCCESS
                                    delay(900)
                                    navController.navigate("debts") {
                                        // popUpTo باید route pattern کامل همان‌طور که در
                                        // composable تعریف شده را بگیرد، نه فقط رشته‌ی خام
                                        // "debts" — وگرنه با نمونه‌ی واقعی
                                        // "debts?filterDate={filterDate}" در پشته تطبیق پیدا
                                        // نمی‌کند و آن نمونه در پشته باقی می‌ماند
                                        popUpTo("debts?filterDate={filterDate}") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        },
                        enabled = personName.isNotBlank() && parsedAmount > 0L && dueDateIsValid &&
                            createdDateField.isNotBlank() &&
                            (!isInstallment || dueDate.isNotBlank()) && installmentFieldsValid &&
                            submitStage == DebtSubmitStage.IDLE,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            disabledContainerColor = if (submitStage != DebtSubmitStage.IDLE) accentColor else accentColor.copy(alpha = 0.35f)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            when (submitStage) {
                    DebtSubmitStage.LOADING -> {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("در حال ثبت ...", color = Color.White, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    DebtSubmitStage.SUCCESS -> {
                        Text("ثبت شد", color = Color.White, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    DebtSubmitStage.IDLE -> {
                        Text(
                            title,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            fontFamily = Vazirmatn,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun DebtTypeToggle(selected: DataDebtType, onSelect: (DataDebtType) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(6.dp).height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TypeToggleButton(
                "طلب من",
                Icons.AutoMirrored.Outlined.TrendingUp,
                selected == DataDebtType.RECEIVABLE,
                IncomeGreen,
                { onSelect(DataDebtType.RECEIVABLE) },
                Modifier.weight(1f)
            )
            TypeToggleButton(
                "بدهی من",
                Icons.AutoMirrored.Outlined.TrendingDown,
                selected == DataDebtType.PAYABLE,
                ExpensePurple,
                { onSelect(DataDebtType.PAYABLE) },
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AddDebtHeader(debtType: DataDebtType) {
    val color = if (debtType == DataDebtType.RECEIVABLE) IncomeGreen else ExpensePurple
    val title = if (debtType == DataDebtType.RECEIVABLE) "ثبت طلب جدید" else "ثبت بدهی جدید"
    val subtitle = if (debtType == DataDebtType.RECEIVABLE) "مبلغی که دیگران باید به شما پرداخت کنند" else "مبلغی که باید به دیگران پرداخت کنید"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)), Alignment.Center) {
                Icon(Icons.Outlined.AccountBalanceWallet, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = Vazirmatn)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontFamily = Vazirmatn)
            }
        }
    }
}

@Composable
private fun SimpleNumberField(value: String, activeColor: Color, label: String, placeholder: String, dialogTitle: String, maxDigits: Int, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    var showKeypad by remember { mutableStateOf(false) }
    val hasValue = value.isNotBlank()

    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, fontFamily = Vazirmatn, color = if (hasValue) activeColor else TextTertiary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (hasValue) {
                        Modifier.border(width = 2.dp, color = activeColor, shape = RoundedCornerShape(14.dp))
                    } else {
                        Modifier
                    }
                )
        ) {
            OutlinedTextField(
                value = if (hasValue) PersianDateUtils.toPersianDigits(value) else "",
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                placeholder = {
                    Text(placeholder, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, color = TextTertiary, fontSize = 14.sp, fontFamily = Vazirmatn)
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Right, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = Vazirmatn),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = if (hasValue) Color.Transparent else DividerColor,
                    disabledContainerColor = Color(0xFFFDFDFD),
                    disabledTextColor = TextPrimary
                )
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
                title = dialogTitle,
                unitLabel = null,
                maxDigits = maxDigits,
                onDismiss = { showKeypad = false },
                onConfirm = { newValue ->
                    onValueChange(newValue)
                    showKeypad = false
                }
            )
        }
    }
}

@Composable
private fun ReadOnlyAmount(value: String, activeColor: Color, onValueChange: (String) -> Unit) {
    var showKeypad by remember { mutableStateOf(false) }
    val hasValue = value.isNotBlank()

    Column {
        Text("مبلغ", fontSize = 12.sp, fontFamily = Vazirmatn, color = if (hasValue) activeColor else TextTertiary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (hasValue) {
                        Modifier.border(width = 2.dp, color = activeColor, shape = RoundedCornerShape(14.dp))
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
                placeholder = {
                    Text("۰", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, color = TextTertiary, fontSize = 14.sp, fontFamily = Vazirmatn)
                },
                suffix = { Text("تومان", color = TextSecondary, fontFamily = Vazirmatn, fontSize = 13.sp) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Right, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = Vazirmatn),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = if (hasValue) Color.Transparent else DividerColor,
                    disabledContainerColor = Color(0xFFFDFDFD),
                    disabledTextColor = TextPrimary
                )
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
private fun DebtTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, accentColor: Color, icon: ImageVector? = null, keyboardType: KeyboardType = KeyboardType.Text, suffix: String? = null, singleLine: Boolean = true, modifier: Modifier = Modifier) {
    val hasValue = value.trim().isNotEmpty()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isActive = hasValue || isFocused
    val borderColor by animateColorAsState(
        targetValue = if (isActive) accentColor else DividerColor,
        animationSpec = tween(durationMillis = 200),
        label = "debtFieldBorderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isActive) 2.dp else 1.dp,
        animationSpec = tween(durationMillis = 200),
        label = "debtFieldBorderWidth"
    )
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, fontFamily = Vazirmatn, color = if (isActive) accentColor else TextTertiary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
        ) {
            OutlinedTextField(
                value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(),
                interactionSource = interactionSource,
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Right, fontSize = 16.sp, fontFamily = Vazirmatn),
                placeholder = {
    Text(
        text = placeholder,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Right,
        color = TextTertiary,
        fontSize = 14.sp,
        fontFamily = Vazirmatn
    )
},
                leadingIcon = icon?.let { imageVector -> { Icon(imageVector, null, tint = if (isActive) accentColor else TextTertiary) } },
                trailingIcon = suffix?.let { text -> { Text(text, color = TextSecondary, fontFamily = Vazirmatn) } },
                singleLine = singleLine,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color(0xFFFDFDFD), unfocusedContainerColor = Color(0xFFFDFDFD))
            )
        }
    }
}

@Composable
private fun DebtDateInput(
    value: String,
    activeColor: Color,
    label: String = "تاریخ سررسید (اختیاری)",
    placeholder: String = "انتخاب تاریخ سررسید",
    // اگر کاربر دیالوگ انتخاب تاریخ را بدون انتخاب چیزی ببندد، آیا مقدار فعلی باید خالی شود؟
    // برای سررسید (اختیاری) بله — رفتار قبلی حفظ شده. برای «تاریخ ثبت» (که همیشه یک مقدار
    // پیش‌فرض معتبر دارد) نه، وگرنه یک انصراف تصادفی، تاریخ امروزِ درست را پاک می‌کرد
    clearOnDismissWithoutSelection: Boolean = true,
    onValueChange: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val hasSelectedDate = value.isNotBlank()
    Column {
        Text(label, fontSize = 12.sp, fontFamily = Vazirmatn, color = if (hasSelectedDate) activeColor else TextTertiary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().then(if (hasSelectedDate) Modifier.border(2.dp, activeColor, RoundedCornerShape(14.dp)) else Modifier)) {
            OutlinedTextField(
                value = if (value.isBlank()) "" else PersianDateUtils.toPersianDigits(value), onValueChange = {}, enabled = false,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                placeholder = {
    Text(
        text = placeholder,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Right,
        color = TextTertiary,
        fontSize = 14.sp,
        fontFamily = Vazirmatn
    )
},
                trailingIcon = { Icon(Icons.Outlined.CalendarMonth, null, tint = if (hasSelectedDate) activeColor else TextTertiary, modifier = Modifier.size(20.dp)) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Right, fontSize = 15.sp, fontFamily = Vazirmatn, color = TextPrimary),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = if (hasSelectedDate) Color.Transparent else DividerColor, disabledContainerColor = Color(0xFFFDFDFD), disabledTextColor = TextPrimary)
            )
            Box(Modifier.matchParentSize().clickable { showPicker = true })
        }
    }
    if (showPicker) {
        PersianDatePickerDialog(
            initialDate = value,
            accentColor = activeColor,
            onDismiss = {
                showPicker = false
                if (clearOnDismissWithoutSelection) onValueChange("")
            },
            onConfirm = { newDate -> onValueChange(newDate); showPicker = false }
        )
    }
}

@Composable
private fun ReminderToggleRow(checked: Boolean, accentColor: Color, label: String = "یادآوری سررسید", onCheckedChange: (Boolean) -> Unit) {
    val borderColor by animateColorAsState(
        targetValue = if (checked) accentColor else DividerColor,
        animationSpec = tween(durationMillis = 200),
        label = "reminderToggleBorderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (checked) 2.dp else 1.dp,
        animationSpec = tween(durationMillis = 200),
        label = "reminderToggleBorderWidth"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFDFDFD))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                color = TextPrimary,
                fontFamily = Vazirmatn,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "۳ روز قبل و روز سررسید اعلان دریافت می‌کنید",
                color = TextTertiary,
                fontFamily = Vazirmatn,
                fontSize = 11.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InstallmentToggleRow(checked: Boolean, accentColor: Color, onCheckedChange: (Boolean) -> Unit) {
    val borderColor by animateColorAsState(
        targetValue = if (checked) accentColor else DividerColor,
        animationSpec = tween(durationMillis = 200),
        label = "installmentToggleBorderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (checked) 2.dp else 1.dp,
        animationSpec = tween(durationMillis = 200),
        label = "installmentToggleBorderWidth"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFDFDFD))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "تبدیل به وام قسطی",
                color = TextPrimary,
                fontFamily = Vazirmatn,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "مبلغ به‌صورت خودکار بین چند قسط ماهانه تقسیم می‌شود",
                color = TextTertiary,
                fontFamily = Vazirmatn,
                fontSize = 11.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private enum class DebtSubmitStage { IDLE, LOADING, SUCCESS }

@Composable
private fun DebtTimeInput(
    value: String,
    activeColor: Color,
    label: String = "ساعت سررسید (پیش‌فرض ۱۰ صبح)",
    placeholder: String = "اگر انتخاب نکنی، ساعت ۱۰ صبح ثبت می‌شه",
    onValueChange: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val hasSelectedTime = value.isNotBlank()
    Column {
        Text(label, fontSize = 12.sp, fontFamily = Vazirmatn, color = if (hasSelectedTime) activeColor else TextTertiary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().then(if (hasSelectedTime) Modifier.border(2.dp, activeColor, RoundedCornerShape(14.dp)) else Modifier)) {
            OutlinedTextField(
                value = if (value.isBlank()) "" else PersianDateUtils.toPersianDigits(value),
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                placeholder = {
                    Text(
                        text = placeholder,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right,
                        color = TextTertiary,
                        fontSize = 13.sp,
                        fontFamily = Vazirmatn
                    )
                },
                trailingIcon = { Icon(Icons.Outlined.Schedule, null, tint = if (hasSelectedTime) activeColor else TextTertiary, modifier = Modifier.size(20.dp)) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Right, fontSize = 15.sp, fontFamily = Vazirmatn, color = TextPrimary),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = if (hasSelectedTime) Color.Transparent else DividerColor, disabledContainerColor = Color(0xFFFDFDFD), disabledTextColor = TextPrimary)
            )
            Box(Modifier.matchParentSize().clickable { showPicker = true })
        }
    }
    if (showPicker) {
        TimePickerDialog(
            initialTime = value,
            accentColor = activeColor,
            onDismiss = { showPicker = false },
            onConfirm = { newTime -> onValueChange(newTime); showPicker = false }
        )
    }
}
