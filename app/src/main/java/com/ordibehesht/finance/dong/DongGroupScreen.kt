package com.ordibehesht.finance.dong

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ordibehesht.finance.ui.screens.AmountKeypadDialog
import com.ordibehesht.finance.ui.screens.CategoryDonutChart
import com.ordibehesht.finance.ui.screens.CategoryLegendRow
import com.ordibehesht.finance.ui.screens.FlowerDecoration
import com.ordibehesht.finance.ui.screens.cardColorPalette
import com.ordibehesht.finance.ui.theme.BackgroundLight
import com.ordibehesht.finance.ui.theme.CardWhite
import com.ordibehesht.finance.ui.theme.DividerColor
import com.ordibehesht.finance.ui.theme.ExpensePurple
import com.ordibehesht.finance.ui.theme.IncomeGreen
import com.ordibehesht.finance.ui.theme.LightGreen
import com.ordibehesht.finance.ui.theme.PrimaryGreen
import com.ordibehesht.finance.ui.theme.TextPrimary
import com.ordibehesht.finance.ui.theme.TextSecondary
import com.ordibehesht.finance.ui.theme.TextTertiary
import com.ordibehesht.finance.ui.theme.Vazirmatn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DongGroupScreen(navController: NavController, groupId: String) {
    val groups by DongRepository.groups.collectAsStateWithLifecycle()
    val group = groups.find { it.id == groupId }

    if (group == null) {
        // گروه حذف شده یا پیدا نشد؛ به لیست برگرد
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    var expensesExpanded by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    var showAddParticipant by remember { mutableStateOf(false) }
    var editingShareFor by remember { mutableStateOf<DongParticipant?>(null) }
    var participantPendingDelete by remember { mutableStateOf<DongParticipant?>(null) }
    var expensePendingDelete by remember { mutableStateOf<DongExpense?>(null) }
    val scrollState = rememberScrollState()

    val total = DongCalculator.totalExpense(group)
    val statuses = DongCalculator.calculateStatuses(group)
    val perPersonShareLabel = if (group.participants.isNotEmpty() && group.participants.all { it.manualShare == null }) {
        DongFormat.toman(total / group.participants.size.coerceAtLeast(1))
    } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        group.title,
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
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                FlowerDecoration(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 80.dp, start = 4.dp)
                        .size(60.dp)
                )
                FlowerDecoration(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 100.dp, end = 4.dp)
                        .size(50.dp),
                    color = ExpensePurple.copy(alpha = 0.08f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                ) {
                Spacer(Modifier.height(12.dp))

                // --- کارت خلاصه ---
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        SummaryStat(
                            modifier = Modifier.weight(1f),
                            label = "هزینه کل",
                            value = "${DongFormat.toman(total)} تومان",
                            valueColor = ExpensePurple
                        )
                        Spacer(Modifier.width(8.dp))
                        SummaryStat(
                            modifier = Modifier.weight(1f),
                            label = if (group.participants.isEmpty()) "سهم هرنفر" else "سهم هرنفر (${group.participants.size} نفر)",
                            value = if (perPersonShareLabel != null) "$perPersonShareLabel تومان" else "متفاوت",
                            valueColor = PrimaryGreen
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // --- نوار خلاصه‌ی وضعیت تسویه ---
                if (statuses.isNotEmpty() && group.expenses.isNotEmpty()) {
                    val settledCount = statuses.count { it.balance == 0L }
                    val allSettled = settledCount == statuses.size
                    // آخرین تسویه‌ی ثبت‌شده — وقتی همه تسویه باشند، همین رکورد دقیقاً همان
                    // لحظه‌ای است که آخرین بدهی گروه صاف شده
                    val lastSettlement = remember(group.settlements) {
                        group.settlements.maxWithOrNull(
                            compareBy<DongSettlementRecord> { it.date }.thenBy { it.time }
                        )
                    }
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (allSettled) LightGreen.copy(alpha = 0.5f) else CardWhite
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "وضعیت کلی تسویه: $settledCount از ${statuses.size} نفر تسویه کامل",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (allSettled) PrimaryGreen else TextSecondary,
                                    fontWeight = if (allSettled) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = Vazirmatn
                                )
                                if (allSettled) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (allSettled && lastSettlement != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "آخرین تسویه: ${DongFormat.toPersianDigits(lastSettlement.date)} - ${DongFormat.toPersianDigits(lastSettlement.time)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    fontFamily = Vazirmatn
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // --- آکاردئون هزینه‌ها ---
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expensesExpanded = !expensesExpanded }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Receipt,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "هزینه‌ها (${group.expenses.size} مورد)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Vazirmatn
                                )
                            }
                            Icon(
                                Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(if (expensesExpanded) 180f else 0f)
                            )
                        }

                        AnimatedVisibility(
                            visible = expensesExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                                if (group.expenses.isEmpty()) {
                                    Text(
                                        "هنوز هزینه‌ای ثبت نشده",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextTertiary,
                                        fontFamily = Vazirmatn,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                } else {
                                    group.expenses.forEach { expense ->
                                        val payerName = group.participants.find { it.id == expense.payerId }?.name ?: "؟"
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
                                                        .background(cardColorPalette[expense.colorIndex % cardColorPalette.size].copy(alpha = 0.7f))
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        expense.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = Vazirmatn
                                                    )
                                                    Text(
                                                        "${DongFormat.toman(expense.amount)} تومان",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextTertiary,
                                                        fontFamily = Vazirmatn
                                                    )
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    payerName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = PrimaryGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = Vazirmatn
                                                )
                                                IconButton(
                                                    onClick = { expensePendingDelete = expense },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        contentDescription = "حذف هزینه",
                                                        tint = ExpensePurple,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                TextButton(
                                    onClick = { showAddExpense = true },
                                    enabled = group.participants.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.Add, null, tint = ExpensePurple, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("افزودن هزینه جدید", color = ExpensePurple, fontFamily = Vazirmatn, fontSize = 13.sp)
                                }
                                if (group.participants.isEmpty()) {
                                    Text(
                                        "اول باید نفرات گروه را اضافه کنی",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary,
                                        fontFamily = Vazirmatn,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // --- جدول سهم و وضعیت هرکس ---
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "سهم و وضعیت هر نفر",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Vazirmatn
                            )
                            TextButton(onClick = { showAddParticipant = true }) {
                                Icon(Icons.Outlined.PersonAdd, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("افزودن نفر", color = PrimaryGreen, fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                        }

                        if (statuses.isEmpty()) {
                            Text(
                                "هنوز کسی به گروه اضافه نشده",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                fontFamily = Vazirmatn,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Spacer(Modifier.height(8.dp))
                            statuses.forEachIndexed { index, status ->
                                if (index > 0) {
                                    Spacer(Modifier.height(8.dp))
                                }
                                SwipeableDongParticipantItem(
                                    status = status,
                                    hasExpenses = group.expenses.isNotEmpty(),
                                    onEdit = { editingShareFor = status.participant },
                                    onDelete = { participantPendingDelete = status.participant }
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "برای تسویه حساب به سمت راست و برای حذف به سمت چپ بکشید",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontFamily = Vazirmatn,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(14.dp))
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LegendDot(color = IncomeGreen)
                                    Spacer(Modifier.width(4.dp))
                                    Text("طلبکار", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontFamily = Vazirmatn)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LegendDot(color = ExpensePurple)
                                    Spacer(Modifier.width(4.dp))
                                    Text("بدهکار", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontFamily = Vazirmatn)
                                }
                            }
                        }
                    }
                }

                if (group.participants.isEmpty() || group.expenses.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ابتدا افراد گروه را اضافه کنید سپس هزینه‌ها را ثبت کنید",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontFamily = Vazirmatn,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }

                // --- گزارش تسویه‌ها ---
                if (group.settlements.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "گزارش تسویه‌ها",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Vazirmatn
                            )
                            Spacer(Modifier.height(8.dp))
                            val sortedSettlements = remember(group.settlements) {
                                group.settlements.sortedWith(
                                    compareByDescending<DongSettlementRecord> { it.date }
                                        .thenByDescending { it.time }
                                )
                            }
                            sortedSettlements.forEachIndexed { index, s ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .background(DividerColor)
                                            .padding(vertical = 4.dp)
                                    )
                                }
                                val fromName = group.participants.find { it.id == s.fromId }?.name ?: "؟"
                                val toName = group.participants.find { it.id == s.toId }?.name ?: "؟"
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "$fromName با $toName تسویه کرد",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = Vazirmatn
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "${DongFormat.toPersianDigits(s.date)} · ${DongFormat.toPersianDigits(s.time)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextTertiary,
                                            fontFamily = Vazirmatn
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${DongFormat.toman(s.amount)} تومان",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PrimaryGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = Vazirmatn
                                        )
                                        IconButton(
                                            onClick = { DongRepository.deleteSettlement(group.id, s.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "حذف تسویه",
                                                tint = TextTertiary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (statuses.isNotEmpty() && total > 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "هنوز تسویه‌ای ثبت نشده",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            fontFamily = Vazirmatn
                        )
                    }
                }

                if (statuses.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    DongCreditDebtChartCard(statuses = statuses)

                    Spacer(Modifier.height(12.dp))
                    DongPaidByChartCard(statuses = statuses)
                }

                Spacer(Modifier.height(90.dp))
                }
            }
        }
    }

    if (showAddParticipant) {
        DongAddParticipantDialog(
            onDismiss = { showAddParticipant = false },
            onConfirm = { name ->
                DongRepository.addParticipant(group.id, name)
                showAddParticipant = false
            }
        )
    }

    if (showAddExpense) {
        DongAddExpenseDialog(
            participants = group.participants,
            onDismiss = { showAddExpense = false },
            onConfirm = { title, amount, payerId ->
                DongRepository.addExpense(group.id, title, amount, payerId)
                showAddExpense = false
            }
        )
    }

    editingShareFor?.let { participant ->
        DongSettleUpDialog(
            group = group,
            fromParticipant = participant,
            onDismiss = { editingShareFor = null },
            onConfirmSettlement = { toId, amount ->
                DongRepository.addSettlement(group.id, participant.id, toId, amount)
                editingShareFor = null
            }
        )
    }

    participantPendingDelete?.let { participant ->
        DongConfirmDialog(
            title = "حذف نفر",
            message = "«${participant.name}» از گروه حذف شود؟ هزینه‌هایی که او پرداخت کرده هم حذف می‌شوند.",
            confirmLabel = "حذف",
            onDismiss = { participantPendingDelete = null },
            onConfirm = {
                DongRepository.removeParticipant(group.id, participant.id)
                participantPendingDelete = null
            }
        )
    }

    expensePendingDelete?.let { expense ->
        DongConfirmDialog(
            title = "حذف هزینه",
            message = "هزینه‌ی «${expense.title}» حذف شود؟",
            confirmLabel = "حذف",
            onDismiss = { expensePendingDelete = null },
            onConfirm = {
                DongRepository.deleteExpense(group.id, expense.id)
                expensePendingDelete = null
            }
        )
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun SwipeableDongParticipantItem(
    status: DongParticipantStatus,
    hasExpenses: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val swipeThreshold = with(density) { 80.dp.toPx() }
    var offsetX by remember(status.participant.id) { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val animatedOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = offsetX,
        label = "dongSwipeOffset"
    )

    val swipeDirection = when {
        animatedOffset > swipeThreshold -> "edit"
        animatedOffset < -swipeThreshold -> "delete"
        else -> "none"
    }

    val rowBackgroundColor = when (swipeDirection) {
        "edit" -> Color(0xFFE8F5E9)
        "delete" -> Color(0xFFFFEBEE)
        else -> BackgroundLight
    }

    val icon = when (swipeDirection) {
        "edit" -> Icons.Outlined.Edit
        "delete" -> Icons.Outlined.Delete
        else -> null
    }

    val iconColor = when (swipeDirection) {
        "edit" -> IncomeGreen
        "delete" -> Color(0xFFE53935)
        else -> Color.Transparent
    }

    // detectHorizontalDragGestures همیشه جابه‌جایی فیزیکی خام گزارش می‌دهد (مستقل از جهت
    // صفحه)، پس جهت را Ltr نگه می‌داریم تا انگشت، اسلاید و آیکون هماهنگ بمانند — دقیقاً
    // مطابق همین منطق در SwipeableTransactionItem (HomeScreen.kt)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(rowBackgroundColor)
        ) {
            if (icon != null && swipeDirection != "none") {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                        .align(
                            when (swipeDirection) {
                                "edit" -> Alignment.CenterStart
                                "delete" -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { androidx.compose.ui.unit.IntOffset(animatedOffset.toInt(), 0) }
                    .pointerInput(status.participant.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    offsetX > swipeThreshold -> {
                                        onEdit()
                                        offsetX = 0f
                                    }
                                    offsetX < -swipeThreshold -> {
                                        onDelete()
                                        offsetX = 0f
                                    }
                                    else -> offsetX = 0f
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount
                            }
                        )
                    }
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBackgroundColor)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LegendDot(color = if (status.balance >= 0) IncomeGreen else ExpensePurple)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                status.participant.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Vazirmatn
                            )
                        }
                        Text(
                            if (hasExpenses && status.balance == 0L) "تسویه شده" else "${DongFormat.toman(kotlin.math.abs(status.balance))} تومان",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status.balance >= 0) IncomeGreen else ExpensePurple,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(modifier: Modifier = Modifier, label: String, value: String, valueColor: Color = TextPrimary) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BackgroundLight)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontFamily = Vazirmatn)
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            fontFamily = Vazirmatn
        )
    }
}

@Composable
private fun DongAddParticipantDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("افزودن نفر", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = Vazirmatn)
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        placeholder = { Text("مثلاً امیر", color = TextTertiary, fontFamily = Vazirmatn) },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, fontSize = 15.sp, fontFamily = Vazirmatn),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DividerColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                            Text("افزودن", color = if (name.isNotBlank()) PrimaryGreen else TextTertiary, fontWeight = FontWeight.Bold, fontFamily = Vazirmatn)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("انصراف", color = TextTertiary, fontFamily = Vazirmatn)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DongAddExpenseDialog(
    participants: List<DongParticipant>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Long, payerId: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf(TextFieldValue("")) }
    var showKeypad by remember { mutableStateOf(false) }
    var payerId by remember { mutableStateOf(participants.firstOrNull()?.id ?: "") }

    val parsedAmount = DongFormat.parseAmount(amountText.text)
    val amountHasValue = parsedAmount > 0
    val isValid = title.isNotBlank() && amountHasValue && payerId.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("افزودن هزینه جدید", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = Vazirmatn)
                    Spacer(Modifier.height(16.dp))

                    Text("عنوان هزینه", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontFamily = Vazirmatn, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        placeholder = { Text("مثلاً شام رستوران", color = TextTertiary, fontFamily = Vazirmatn) },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, fontSize = 15.sp, fontFamily = Vazirmatn),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ExpensePurple,
                            unfocusedBorderColor = DividerColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("مبلغ", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontFamily = Vazirmatn, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Spacer(Modifier.height(4.dp))
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
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = Vazirmatn),
                            placeholder = { Text("۰", color = TextTertiary, fontFamily = Vazirmatn) },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color.Transparent,
                                disabledContainerColor = Color(0xFFFDFDFD),
                                disabledTextColor = TextPrimary
                            ),
                            suffix = { Text("تومان", color = TextSecondary, fontSize = 13.sp, fontFamily = Vazirmatn) }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showKeypad = true })
                    }

                    if (showKeypad) {
                        AmountKeypadDialog(
                            initialAmount = amountText.text,
                            accentColor = ExpensePurple,
                            onDismiss = { showKeypad = false },
                            onConfirm = { newAmount ->
                                amountText = TextFieldValue(newAmount, selection = TextRange(newAmount.length))
                                showKeypad = false
                            }
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("چه کسی پرداخت کرده؟", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontFamily = Vazirmatn, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        participants.forEach { p ->
                            val selected = p.id == payerId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) ExpensePurple else BackgroundLight)
                                    .clickable { payerId = p.id }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    p.name,
                                    color = if (selected) Color.White else TextPrimary,
                                    fontFamily = Vazirmatn,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(
                            onClick = { if (isValid) onConfirm(title.trim(), parsedAmount, payerId) },
                            enabled = isValid
                        ) {
                            Text("ثبت هزینه", color = if (isValid) ExpensePurple else TextTertiary, fontWeight = FontWeight.Bold, fontFamily = Vazirmatn)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("انصراف", color = TextTertiary, fontFamily = Vazirmatn)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DongSettleUpDialog(
    group: DongGroup,
    fromParticipant: DongParticipant,
    onDismiss: () -> Unit,
    onConfirmSettlement: (toId: String, amount: Long) -> Unit
) {
    val statuses = remember(group) { DongCalculator.calculateStatuses(group) }
    val fromStatus = statuses.find { it.participant.id == fromParticipant.id }
    val fromBalance = fromStatus?.balance ?: 0L
    val isDebtor = fromBalance < 0
    // فقط کسانی که الان طلبکارند (balance مثبت) مجاز به دریافت تسویه‌اند — این لیست با هر
    // تغییر در هزینه‌ها یا تسویه‌های قبلی خودکار به‌روز می‌شود چون از calculateStatuses می‌آید
    val creditors = remember(statuses, fromParticipant.id) {
        statuses.filter { it.balance > 0 && it.participant.id != fromParticipant.id }
    }
    val canSettle = isDebtor && creditors.isNotEmpty()

    var selectedToId by remember(creditors) { mutableStateOf(creditors.firstOrNull()?.participant?.id ?: "") }
    var amountText by remember { mutableStateOf(TextFieldValue("")) }
    var showKeypad by remember { mutableStateOf(false) }
    val parsedAmount = DongFormat.parseAmount(amountText.text)
    val amountHasValue = parsedAmount > 0
    val selectedCreditorBalance = creditors.find { it.participant.id == selectedToId }?.balance ?: 0L
    // سقف مجاز پرداخت: هم نباید از بدهی خودِ فرستنده بیشتر شود و هم نباید از طلبِ گیرنده —
    // وگرنه دوباره طلب/بدهیِ برعکس ایجاد می‌شود (دقیقاً همان چیزی که کاربر نگرانش بود)
    val maxAllowed = minOf(kotlin.math.abs(fromBalance), selectedCreditorBalance)
    val exceedsMax = amountHasValue && parsedAmount > maxAllowed
    val isValid = canSettle && selectedToId.isNotBlank() && amountHasValue && !exceedsMax

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "تسویه حساب «${fromParticipant.name}»",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )

                    if (!canSettle) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (!isDebtor) {
                                "«${fromParticipant.name}» الان بدهکار نیست، پس چیزی برای تسویه ندارد."
                            } else {
                                "کسی طلبکار نیست"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Vazirmatn
                        )
                        Spacer(Modifier.height(18.dp))
                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text("بستن", color = TextTertiary, fontFamily = Vazirmatn)
                        }
                    } else {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "بدهی فعلی: ${DongFormat.toman(kotlin.math.abs(fromBalance))} تومان",
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpensePurple,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Vazirmatn
                        )

                        Spacer(Modifier.height(16.dp))
                        Text(
                            "به چه کسی پرداخت کرده؟",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontFamily = Vazirmatn
                        )
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            creditors.forEach { creditorStatus ->
                                val selected = creditorStatus.participant.id == selectedToId
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) PrimaryGreen else BackgroundLight)
                                        .clickable { selectedToId = creditorStatus.participant.id }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        creditorStatus.participant.name,
                                        color = if (selected) Color.White else TextPrimary,
                                        fontFamily = Vazirmatn,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "چقدر پرداخت کرده؟",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                fontFamily = Vazirmatn
                            )
                            Text(
                                "حداکثر ${DongFormat.toman(maxAllowed)} تومان",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                fontFamily = Vazirmatn
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = {},
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (amountHasValue) 2.dp else 1.dp,
                                        color = when {
                                            exceedsMax -> Color(0xFFE53935)
                                            amountHasValue -> PrimaryGreen
                                            else -> DividerColor
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = Vazirmatn),
                                placeholder = { Text("۰", color = TextTertiary, fontFamily = Vazirmatn) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = Color.Transparent,
                                    disabledContainerColor = Color(0xFFFDFDFD),
                                    disabledTextColor = TextPrimary
                                ),
                                suffix = { Text("تومان", color = TextSecondary, fontSize = 13.sp, fontFamily = Vazirmatn) }
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { showKeypad = true })
                        }

                        if (showKeypad) {
                            AmountKeypadDialog(
                                initialAmount = amountText.text,
                                accentColor = PrimaryGreen,
                                onDismiss = { showKeypad = false },
                                onConfirm = { newAmount ->
                                    amountText = TextFieldValue(newAmount, selection = TextRange(newAmount.length))
                                    showKeypad = false
                                }
                            )
                        }

                        if (exceedsMax) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "مبلغ نمی‌تواند بیشتر از ${DongFormat.toman(maxAllowed)} تومان باشد",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE53935),
                                fontFamily = Vazirmatn
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(
                                onClick = { if (isValid) onConfirmSettlement(selectedToId, parsedAmount) },
                                enabled = isValid
                            ) {
                                Text("ثبت تسویه", color = if (isValid) PrimaryGreen else TextTertiary, fontWeight = FontWeight.Bold, fontFamily = Vazirmatn)
                            }
                            TextButton(onClick = onDismiss) {
                                Text("انصراف", color = TextTertiary, fontFamily = Vazirmatn)
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class DongBreakdownTab { CREDITOR, DEBTOR }

/** پالت رنگ محلی برای نمودارهای دایره‌ای دونگ — چون chartPalette در ReportsScreen پرایوت است. */
private val dongChartPalette = listOf(
    ExpensePurple,
    Color(0xFFE0A458),
    Color(0xFF5B9BD5),
    Color(0xFFE0678C),
    Color(0xFF6FCF97),
    Color(0xFFB0855A)
)

@Composable
private fun DongBreakdownTabButton(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) color else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = Vazirmatn
        )
    }
}

/**
 * کارت «طلبکار / بدهکار» — دقیقاً هم‌الگو با «نمودار دسته‌بندی» در گزارش‌ها (سوییچ + نمودار
 * دایره‌ای + لیست کنارش)، با این تفاوت که به‌جای دسته‌بندی هزینه، اسم نفرات نشان داده می‌شود
 * و به‌جای «هزینه/درآمد» سوییچ «طلبکار/بدهکار» است.
 */
@Composable
private fun DongCreditDebtChartCard(statuses: List<DongParticipantStatus>) {
    var tab by remember { mutableStateOf(DongBreakdownTab.DEBTOR) }

    // رنگ هر نفر بر اساس ایندکس او در کل لیست شرکت‌کنندگان گروه تعیین می‌شود، نه در این
    // لیست فیلترشده — تا اسم هرکس در این کارت و کارت «مبلغ هزینه پرداختی هر نفر» همیشه
    // یک رنگ ثابت داشته باشد (چه ترتیب فیلترشده‌ی این دو کارت متفاوت باشد چه نباشد)
    val colorByParticipantId = statuses.mapIndexed { index, s -> s.participant.id to dongChartPalette[index % dongChartPalette.size] }.toMap()

    val creditors = statuses.filter { it.balance > 0 }
    val debtors = statuses.filter { it.balance < 0 }
    val activeList = if (tab == DongBreakdownTab.CREDITOR) creditors else debtors
    val entries = activeList.map { it.participant.name to (if (tab == DongBreakdownTab.CREDITOR) it.balance else -it.balance) }
    val colors = activeList.map { colorByParticipantId[it.participant.id] ?: dongChartPalette[0] }
    val total = entries.sumOf { it.second }
    val centerLabel = if (tab == DongBreakdownTab.CREDITOR) "جمع طلب" else "جمع بدهی"
    val emptyMessage = if (tab == DongBreakdownTab.CREDITOR) "کسی طلبکار نیست" else "کسی بدهکار نیست"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "طلبکار و بدهکار",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DividerColor.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    DongBreakdownTabButton(
                        text = "طلبکار",
                        selected = tab == DongBreakdownTab.CREDITOR,
                        color = IncomeGreen,
                        onClick = { tab = DongBreakdownTab.CREDITOR }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    DongBreakdownTabButton(
                        text = "بدهکار",
                        selected = tab == DongBreakdownTab.DEBTOR,
                        color = ExpensePurple,
                        onClick = { tab = DongBreakdownTab.DEBTOR }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (entries.isEmpty() || total <= 0L) {
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
                            emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        entries.forEachIndexed { index, pair ->
                            CategoryLegendRow(
                                name = pair.first,
                                amount = pair.second,
                                total = total,
                                color = colors[index],
                                showAmount = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CategoryDonutChart(entries, colors, total, centerLabel = centerLabel)
                }
            }
        }
    }
}

/**
 * کارت «مبلغ هزینه پرداختی هر نفر» — بدون سوییچ، چون فقط یک حالت دارد.
 */
@Composable
private fun DongPaidByChartCard(statuses: List<DongParticipantStatus>) {
    // همان قاعده‌ی رنگ‌دهی کارت «طلبکار و بدهکار»: رنگ هر نفر از روی ایندکس او در کل لیست
    // شرکت‌کنندگان گروه تعیین می‌شود تا در هر دو کارت یک رنگ ثابت داشته باشد
    val colorByParticipantId = statuses.mapIndexed { index, s -> s.participant.id to dongChartPalette[index % dongChartPalette.size] }.toMap()

    val activeList = statuses.filter { it.paid > 0 }
    val entries = activeList.map { it.participant.name to it.paid }
    val colors = activeList.map { colorByParticipantId[it.participant.id] ?: dongChartPalette[0] }
    val total = entries.sumOf { it.second }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "هزینه پرداختی هر نفر",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = Vazirmatn
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "مبلغ پرداخت‌شده اولیه",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontFamily = Vazirmatn
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (entries.isEmpty() || total <= 0L) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FlowerDecoration(
                            modifier = Modifier.size(52.dp),
                            color = IncomeGreen.copy(alpha = 0.14f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "هنوز کسی هزینه‌ای پرداخت نکرده",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        entries.forEachIndexed { index, pair ->
                            CategoryLegendRow(
                                name = pair.first,
                                amount = pair.second,
                                total = total,
                                color = colors[index],
                                showAmount = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CategoryDonutChart(entries, colors, total, centerLabel = "پرداخت‌شده")
                }
            }
        }
    }
}
