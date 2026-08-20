package com.example.nargesapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
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
import com.example.nargesapp.data.model.Debt
import com.example.nargesapp.data.model.DebtType
import com.example.nargesapp.data.repository.DebtRepository
import com.example.nargesapp.ui.theme.*
import com.example.nargesapp.ui.utils.PersianDateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LoanDeleteStage { IDLE, LOADING, SUCCESS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(navController: NavController, loanGroupId: String) {
    val allDebts by DebtRepository.debts.collectAsStateWithLifecycle()
    val installments = allDebts
        .filter { it.loanGroupId == loanGroupId }
        .sortedBy { it.installmentNumber ?: 0 }

    if (installments.isEmpty()) {
        // این حالت معمولاً بلافاصله بعد از حذف وام رخ می‌دهد (که خودش popBackStack را صدا می‌زند)؛
        // این fallback فقط برای اطمینان است تا کاربر هرگز صفحه‌ی خالی نبیند
        LaunchedEffect(loanGroupId) {
            navController.popBackStack()
        }
        return
    }

    LoanDetailContent(navController, loanGroupId, installments)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanDetailContent(navController: NavController, loanGroupId: String, installments: List<Debt>) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteStage by remember { mutableStateOf(LoanDeleteStage.IDLE) }
    val scope = rememberCoroutineScope()

    val first = installments.first()
    val isReceivable = first.type == DebtType.RECEIVABLE
    val accentColor = if (isReceivable) IncomeGreen else ExpensePurple
    val title = if (isReceivable) "جزئیات وام (طلب)" else "جزئیات وام (بدهی)"
    val relation = if (isReceivable) "به شما بدهکار است" else "شما به او بدهکار هستید"

    val totalAmount = installments.sumOf { it.amount }
    val remainingAmount = installments.sumOf { it.remainingAmount }
    val paidCount = installments.count { it.isSettled }
    val allSettled = paidCount == installments.size
    val reminderEnabledForAll = installments.count { it.reminderEnabled } >= installments.size / 2 + 1

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
                            Icon(Icons.Outlined.CreditCard, null, tint = accentColor, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(first.personName, color = TextPrimary, fontFamily = Vazirmatn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(relation, color = TextSecondary, fontFamily = Vazirmatn, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                        Text(detailAmount(totalAmount), color = accentColor, fontFamily = Vazirmatn, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (allSettled) "تمام اقساط تسویه شد" else "باقی‌مانده: ${detailAmount(remainingAmount)}",
                            color = if (allSettled) IncomeGreen else TextTertiary,
                            fontFamily = Vazirmatn,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { (paidCount.toFloat() / installments.size.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = accentColor,
                            trackColor = accentColor.copy(alpha = 0.15f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${PersianDateUtils.toPersianDigits(paidCount.toString())} از ${PersianDateUtils.toPersianDigits(installments.size.toString())} قسط پرداخت شده",
                            color = TextSecondary,
                            fontFamily = Vazirmatn,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = reminderEnabledForAll,
                            onCheckedChange = { enabled ->
                                installments.forEach { installment ->
                                    DebtRepository.updateDebt(installment.copy(reminderEnabled = enabled))
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "یادآوری همه‌ی اقساط",
                                color = TextPrimary,
                                fontFamily = Vazirmatn,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "با تغییر این سوییچ، یادآوری تمام اقساط یکجا تنظیم می‌شود",
                                color = TextTertiary,
                                fontFamily = Vazirmatn,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Text(
                    "لیست اقساط",
                    color = TextPrimary,
                    fontFamily = Vazirmatn,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                installments.forEach { installment ->
                    InstallmentRow(
                        installment = installment,
                        accentColor = accentColor,
                        canSettle = DebtRepository.canSettleInstallment(installment),
                        onClick = { navController.navigate("debt_detail/${installment.id}") }
                    )
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = ExpensePurple, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("حذف این وام (همه‌ی اقساط)", color = ExpensePurple, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteDialog) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
            AlertDialog(
                onDismissRequest = {
                    if (deleteStage == LoanDeleteStage.IDLE) showDeleteDialog = false
                },
                title = {
                    Text("حذف وام", fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                },
                text = {
                    Text(
                        "تمام ${PersianDateUtils.toPersianDigits(installments.size.toString())} قسط این وام برای همیشه حذف می‌شوند.",
                        fontFamily = Vazirmatn,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (deleteStage == LoanDeleteStage.IDLE) {
                                scope.launch {
                                    deleteStage = LoanDeleteStage.LOADING
                                    delay(700)
                                    installments.forEach { DebtRepository.deleteDebt(it) }
                                    deleteStage = LoanDeleteStage.SUCCESS
                                    delay(1200)
                                    showDeleteDialog = false
                                    deleteStage = LoanDeleteStage.IDLE
                                    navController.popBackStack()
                                }
                            }
                        },
                        enabled = deleteStage == LoanDeleteStage.IDLE
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (deleteStage) {
                                LoanDeleteStage.LOADING -> {
                                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = ExpensePurple)
                                    Spacer(Modifier.width(6.dp))
                                    Text("...در حال حذف", color = ExpensePurple, fontFamily = Vazirmatn, fontSize = 13.sp)
                                }
                                LoanDeleteStage.SUCCESS -> {
                                    Icon(Icons.Outlined.CheckCircle, null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("حذف شد", color = IncomeGreen, fontFamily = Vazirmatn, fontSize = 13.sp)
                                }
                                LoanDeleteStage.IDLE -> {
                                    Text("حذف", color = ExpensePurple, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }, enabled = deleteStage == LoanDeleteStage.IDLE) {
                        Text("انصراف", color = TextSecondary, fontFamily = Vazirmatn, fontSize = 13.sp)
                    }
                }
            )
        }
    }
}

@Composable
private fun InstallmentRow(installment: Debt, accentColor: Color, canSettle: Boolean, onClick: () -> Unit) {
    val isDueSoon = !installment.isSettled && installment.dueDate.takeIf { it.isNotBlank() }?.let { date ->
        val diff = PersianDateUtils.daysUntil(date)
        diff != null && diff in 0..7
    } == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isDueSoon) Modifier.border(1.5.dp, WarningAmber, RoundedCornerShape(16.dp)) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val badgeColor = when {
                installment.isSettled -> IncomeGreen
                !canSettle -> TextTertiary
                isDueSoon -> WarningAmber
                else -> accentColor
            }
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(badgeColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    PersianDateUtils.toPersianDigits((installment.installmentNumber ?: 0).toString()),
                    color = badgeColor,
                    fontFamily = Vazirmatn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "قسط ${PersianDateUtils.toPersianDigits((installment.installmentNumber ?: 0).toString())}",
                    color = TextPrimary,
                    fontFamily = Vazirmatn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                if (installment.dueDate.isNotBlank()) {
                    Text(
                        PersianDateUtils.toPersianDigits(installment.dueDate),
                        color = TextTertiary,
                        fontFamily = Vazirmatn,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    detailAmount(installment.amount),
                    color = accentColor,
                    fontFamily = Vazirmatn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        installment.isSettled -> "پرداخت شد"
                        !canSettle -> "در انتظار"
                        isDueSoon -> "نزدیک سررسید"
                        else -> "باز"
                    },
                    color = badgeColor,
                    fontFamily = Vazirmatn,
                    fontSize = 11.sp
                )
            }
        }
    }
}
