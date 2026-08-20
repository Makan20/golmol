package com.example.nargesapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.keyframes
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.border
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.rotate as rotateCanvas
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.nargesapp.data.model.Account
import com.example.nargesapp.data.model.DebtType
import com.example.nargesapp.data.model.TransactionType
import com.example.nargesapp.data.repository.DebtNotificationRepository
import com.example.nargesapp.data.repository.DebtRepository
import com.example.nargesapp.ui.theme.*
import com.example.nargesapp.ui.utils.PersianDateUtils
import com.example.nargesapp.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.*
import kotlin.math.absoluteValue

fun toPersianDigits(input: String): String {
    val persian = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return input.map { ch -> if (ch.isDigit()) persian[ch.digitToInt()] else ch }.joinToString("")
}

fun formatAmount(amount: Long): String {
    return toPersianDigits(NumberFormat.getInstance(Locale.US).format(amount))
}

fun formatPersianAmount(amount: Long): String {
    return if (amount < 0) "‎" + toPersianDigits(NumberFormat.getInstance(Locale.US).format(-amount)) + "-"
    else "‎" + toPersianDigits(NumberFormat.getInstance(Locale.US).format(amount))
}

@Composable
fun HomeScreen(navController: NavController, viewModel: TransactionViewModel) {
    val scrollState = rememberScrollState()
    val hazeState = remember { HazeState() }
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by com.example.nargesapp.data.repository.AccountRepository.accounts.collectAsStateWithLifecycle()
    val totalAccountBalance = accounts.sumOf { accountBalance(it, transactions) }

    var selectedPeriod by remember { mutableStateOf(HomePeriod.WEEK) }
    var periodOffset by remember { mutableStateOf(0) }

    // با تغییر نوع بازه (روز/هفته/ماه)، ناوبری به بازه‌ی جاری برمی‌گردد تا گیج‌کننده نباشد
    LaunchedEffect(selectedPeriod) { periodOffset = 0 }

    val periodDates = remember(selectedPeriod, periodOffset) {
        when (selectedPeriod) {
            HomePeriod.DAY -> listOf(PersianDateUtils.addDaysToToday(periodOffset))
            HomePeriod.WEEK -> PersianDateUtils.getWeekDates(periodOffset)
            HomePeriod.MONTH -> PersianDateUtils.getMonthInfo(periodOffset).dates
        }
    }
    val periodTransactions = remember(transactions, periodDates) {
        transactions.filter { it.date in periodDates }
    }
    val totalIncome = periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = periodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    val debts by DebtRepository.debts.collectAsStateWithLifecycle()
    val openDebts = debts.filter { !it.isSettled }
    val periodOpenDebts = openDebts.filter { debt ->
        debt.dueDate.isBlank() || debt.dueDate in periodDates || (PersianDateUtils.daysUntil(debt.dueDate) ?: -1) < 0
    }
    val totalDebtOwed = periodOpenDebts.filter { it.type == DebtType.PAYABLE }.sumOf { it.remainingAmount }
    val totalDebtReceivable = periodOpenDebts.filter { it.type == DebtType.RECEIVABLE }.sumOf { it.remainingAmount }

    // برای وام‌های قسطی، فقط اولین قسط تسویه‌نشده‌ی هر وام قابل‌اقدام است (به‌خاطر ترتیب اجباری پرداخت)
    val (loanDebts, regularOpenDebts) = openDebts.partition { it.loanGroupId != null }
    val nextActionableInstallments = loanDebts
        .groupBy { it.loanGroupId }
        .mapNotNull { (_, installments) ->
            installments.sortedBy { it.installmentNumber ?: 0 }.firstOrNull()
        }
    val actionableDebts = regularOpenDebts + nextActionableInstallments

    val dueSoonCount = actionableDebts.count { debt ->
        debt.dueDate.takeIf { it.isNotBlank() }?.let { date ->
            val diff = PersianDateUtils.daysUntil(date)
            diff != null && diff in 0..7
        } == true
    }

    Scaffold(
        topBar = { TopBarSection(navController) },
        containerColor = BackgroundLight
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            FlowerDecoration(modifier = Modifier.align(Alignment.TopEnd).padding(top = 115.dp, end = 4.dp).size(60.dp))
            FlowerDecoration(modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 100.dp, start = 4.dp).size(50.dp), color = ExpensePurple.copy(alpha = 0.08f))
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).hazeSource(state = hazeState).padding(padding).padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    GreetingSection()
                    Spacer(modifier = Modifier.height(16.dp))
                    BalanceCard(totalAccountBalance, accounts.isEmpty(), navController)
                    Spacer(modifier = Modifier.height(16.dp))

                    PeriodToggle(selected = selectedPeriod, onSelect = { selectedPeriod = it })
                    Spacer(modifier = Modifier.height(10.dp))
                    PeriodNavigator(
                        period = selectedPeriod,
                        offset = periodOffset,
                        onPrevious = { periodOffset -= 1 },
                        onNext = { if (periodOffset < 0) periodOffset += 1 }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (periodOpenDebts.isNotEmpty()) {
                        Box {
                            FlowerDecoration(
                                modifier = Modifier.align(Alignment.Center).size(22.dp),
                                color = PrimaryGreen.copy(alpha = 0.14f)
                            )
                            Column {
                                StatsRow(totalIncome, totalExpense)
                                Spacer(modifier = Modifier.height(12.dp))
                                DebtSummaryRow(
                                    totalDebtOwed = totalDebtOwed,
                                    totalDebtReceivable = totalDebtReceivable,
                                    onClick = { navController.navigate("debts") }
                                )
                            }
                        }
                    } else {
                        StatsRow(totalIncome, totalExpense)
                    }

                    if (dueSoonCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        DebtDueSoonCard(
                            count = dueSoonCount,
                            onClick = { navController.navigate("debts") }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    PeriodChartSection(period = selectedPeriod, periodDates = periodDates, transactions = transactions)
                    Spacer(modifier = Modifier.height(20.dp))

                    val debtCategories = setOf("بدهی", "طلب")
                    val regularTransactions = transactions.filter { it.category !in debtCategories }
                    val debtTransactions = transactions.filter { it.category in debtCategories }

                    RecentTransactions(regularTransactions, accounts, viewModel, navController)

                    if (debtTransactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        RecentDebtTransactions(debtTransactions, accounts, viewModel, navController)
                    }

                    Spacer(modifier = Modifier.height(130.dp))
                }
            }
            Box(
    modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .height(55.dp)
        .hazeEffect(
            state = hazeState,
            style = HazeStyle(
                tint = HazeTint(CardWhite.copy(alpha = 0f)),
                blurRadius = 9.dp
            )
        )
)
Box(modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 16.dp)) {
    BottomNavBar(navController, currentRoute = "home", isScrolling = scrollState.isScrollInProgress)
}
        }
    }
}

@Composable
fun FlowerDecoration(modifier: Modifier = Modifier, color: Color = ExpensePurple.copy(alpha = 0.10f)) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val petalRadius = size.width * 0.18f
        val centerRadius = size.width * 0.12f
        for (i in 0 until 6) {
            val angle = i * 60f * (Math.PI / 180f).toFloat()
            val petalCenter = Offset(center.x + kotlin.math.cos(angle) * centerRadius * 1.2f, center.y + kotlin.math.sin(angle) * centerRadius * 1.2f)
            drawCircle(color = color, radius = petalRadius, center = petalCenter)
        }
        drawCircle(color = color.copy(alpha = 0.5f), radius = centerRadius * 0.6f, center = center)
    }
}

@Composable
fun FabFlowerIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val petalLength = size.width * 0.40f
        val petalWidth = size.width * 0.22f
        val petalDistance = size.width * 0.24f
        val petalCount = 5

        for (i in 0 until petalCount) {
            val angleDeg = i * (360f / petalCount)
            rotateCanvas(degrees = angleDeg, pivot = center) {
                val petalCenter = Offset(center.x, center.y - petalDistance)
                drawOval(
                    color = color,
                    topLeft = Offset(petalCenter.x - petalWidth / 2, petalCenter.y - petalLength / 2),
                    size = Size(petalWidth, petalLength)
                )
            }
        }
        drawCircle(color = color.copy(alpha = (color.alpha + 0.15f).coerceAtMost(1f)), radius = size.width * 0.09f, center = center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarSection(navController: NavController) {
    val notifications by DebtNotificationRepository.notifications.collectAsStateWithLifecycle()
    val hasUnreadNotifications = notifications.any { !it.isRead }

    TopAppBar(
        title = { },
        navigationIcon = {
            Icon(Icons.Outlined.Eco, "Logo", tint = PrimaryGreen, modifier = Modifier.padding(start = 16.dp).size(28.dp))
        },
        actions = {
            IconButton(onClick = { navController.navigate("notifications") }) {
                Box {
                    Icon(Icons.Outlined.Notifications, "اعلان‌ها", tint = TextPrimary)
                    if (hasUnreadNotifications) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ExpensePurple)
                                .border(1.dp, BackgroundLight, CircleShape)
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
    )
}

@Composable
fun GreetingSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("سلام اردیبهشت جان", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("خوش اومدی، روزت قشنگ", style = MaterialTheme.typography.labelMedium, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun BalanceCard(balance: Long, noAccountsYet: Boolean, navController: NavController) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (noAccountsYet) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LightGreen.copy(alpha = 0.5f))
                        .clickable { navController.navigate("cards") }
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "هنوز کارتی نساختی — اول یکی بساز",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                    Icon(Icons.Outlined.Add, null, tint = PrimaryGreen)
                }
            } else {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(LightGreen), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.AccountBalanceWallet, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("موجودی کل", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                            Text("‎" + formatAmount(balance), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp), color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("تومان", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                    }
                }
                FlowerDecoration(modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 14.dp, start = 16.dp).size(48.dp), color = ExpensePurple.copy(alpha = 0.12f))
            }
        }
    }
}

@Composable
fun StatsRow(totalIncome: Long, totalExpense: Long) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("درآمد", totalIncome, Icons.Outlined.AddCard, Color(0xFFE8F5E9), IncomeGreen, Modifier.weight(1f))
        StatCard("هزینه", totalExpense, Icons.Outlined.Wallet, Color(0xFFF3E5F5), ExpensePurple, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(title: String, amount: Long, icon: androidx.compose.ui.graphics.vector.ImageVector, iconBg: Color, iconTint: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Text("‎" + formatAmount(amount), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("تومان", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun DebtSummaryRow(totalDebtOwed: Long, totalDebtReceivable: Long, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("طلب من", totalDebtReceivable, Icons.Outlined.RequestQuote, Color(0xFFE8F5E9), IncomeGreen, Modifier.weight(1f))
        StatCard("بدهی من", totalDebtOwed, Icons.Outlined.MoneyOff, Color(0xFFF3E5F5), ExpensePurple, Modifier.weight(1f))
    }
}

@Composable
fun DebtDueSoonCard(count: Int, onClick: () -> Unit) {
    // افکت: تیک‌تاک ساعت که کامل یک دور (۳۶۰ درجه) می‌زند - نسخه آرام‌تر (۲ برابر کندتر)
    val infiniteTransition = rememberInfiniteTransition(label = "tick")
    val tickRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 7200
                0f at 0
                0f at 800
                60f at 1100 using FastOutSlowInEasing
                60f at 1900
                120f at 2200 using FastOutSlowInEasing
                120f at 3000
                180f at 3300 using FastOutSlowInEasing
                180f at 4100
                240f at 4400 using FastOutSlowInEasing
                240f at 5200
                300f at 5500 using FastOutSlowInEasing
                300f at 6300
                360f at 6600 using FastOutSlowInEasing
                360f at 7200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "tickRotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ExpensePurple.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = ExpensePurple,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(tickRotation)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "$count مورد طلب/بدهی به سررسید نزدیک شده",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontFamily = Vazirmatn,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

enum class HomePeriod { DAY, WEEK, MONTH }

@Composable
fun PeriodToggle(selected: HomePeriod, onSelect: (HomePeriod) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(5.dp)) {
            PeriodToggleButton("روز", selected == HomePeriod.DAY, Modifier.weight(1f)) { onSelect(HomePeriod.DAY) }
            PeriodToggleButton("هفته", selected == HomePeriod.WEEK, Modifier.weight(1f)) { onSelect(HomePeriod.WEEK) }
            PeriodToggleButton("ماه", selected == HomePeriod.MONTH, Modifier.weight(1f)) { onSelect(HomePeriod.MONTH) }
        }
    }
}

@Composable
private fun PeriodToggleButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) LightGreen else Color.Transparent,
        label = "periodToggleBg"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) PrimaryGreen else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = Vazirmatn
        )
    }
}

@Composable
fun PeriodNavigator(period: HomePeriod, offset: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    val label = remember(period, offset) { periodLabel(period, offset) }
    val isAtPresent = offset >= 0

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, "بازه‌ی قبل", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = Vazirmatn)
            IconButton(onClick = onNext, enabled = !isAtPresent) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBackIos,
                    "بازه‌ی بعد",
                    tint = if (isAtPresent) TextTertiary.copy(alpha = 0.4f) else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun periodLabel(period: HomePeriod, offset: Int): String {
    return when (period) {
        HomePeriod.DAY -> {
            val date = PersianDateUtils.addDaysToToday(offset)
            when (offset) {
                0 -> "امروز"
                -1 -> "دیروز"
                else -> PersianDateUtils.toPersianDigits(date)
            }
        }
        HomePeriod.WEEK -> {
            val dates = PersianDateUtils.getWeekDates(offset)
            val first = dates.first().split("/")
            val last = dates.last().split("/")
            if (offset == 0) {
                "این هفته"
            } else {
                "${PersianDateUtils.toPersianDigits(first[2])} تا ${PersianDateUtils.toPersianDigits(last[2])} ${persianMonthName(last[1].toInt())}"
            }
        }
        HomePeriod.MONTH -> {
            val info = PersianDateUtils.getMonthInfo(offset)
            if (offset == 0) {
                "این ماه"
            } else {
                "${persianMonthName(info.month)} ${PersianDateUtils.toPersianDigits(info.year.toString())}"
            }
        }
    }
}

private fun persianMonthName(month: Int): String {
    val names = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )
    return names.getOrElse(month - 1) { "" }
}

@Composable
fun PeriodChartSection(period: HomePeriod, periodDates: List<String>, transactions: List<com.example.nargesapp.data.model.Transaction>) {
    val periodTransactions = remember(transactions, periodDates) {
        transactions.filter { it.date in periodDates }
    }
    val totalPeriodIncome = periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalPeriodExpense = periodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    val chartTitle = when (period) {
        HomePeriod.DAY -> "نمودار روز"
        HomePeriod.WEEK -> "نمودار هفتگی"
        HomePeriod.MONTH -> "نمودار ماهانه"
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(chartTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Row {
                ChartLegend("درآمد", IncomeGreen)
                Spacer(modifier = Modifier.width(12.dp))
                ChartLegend("هزینه", ExpensePurple)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (periodTransactions.isNotEmpty()) {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(IncomeGreen.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("درآمد", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontFamily = Vazirmatn)
                        Text(formatPersianAmount(totalPeriodIncome), style = MaterialTheme.typography.labelMedium, color = IncomeGreen, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ExpensePurple.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("هزینه", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontFamily = Vazirmatn)
                        Text(formatPersianAmount(totalPeriodExpense), style = MaterialTheme.typography.labelMedium, color = ExpensePurple, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Card(modifier = Modifier.fillMaxWidth().height(240.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
            if (periodTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("تراکنشی در این بازه ثبت نشده", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            } else if (period == HomePeriod.DAY) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    DayDonutChart(income = totalPeriodIncome, expense = totalPeriodExpense)
                }
            } else {
                var selectedIndex by remember(periodDates) { mutableStateOf<Int?>(null) }

                val labels: List<String>
                val incomeByBucket: List<Long>
                val expenseByBucket: List<Long>

                if (period == HomePeriod.WEEK) {
                    labels = PersianDateUtils.getWeekDays()
                    incomeByBucket = periodDates.map { date -> periodTransactions.filter { it.type == TransactionType.INCOME && it.date == date }.sumOf { it.amount } }
                    expenseByBucket = periodDates.map { date -> periodTransactions.filter { it.type == TransactionType.EXPENSE && it.date == date }.sumOf { it.amount } }
                } else {
                    // برای ماه: هر هفته یک میله (نه هر روز، تا نمودار شلوغ نشود)
                    val weeksInMonth = periodDates.chunked(7)
                    labels = weeksInMonth.mapIndexed { index, _ -> "هفته ${PersianDateUtils.toPersianDigits((index + 1).toString())}" }
                    incomeByBucket = weeksInMonth.map { week -> periodTransactions.filter { it.type == TransactionType.INCOME && it.date in week }.sumOf { it.amount } }
                    expenseByBucket = weeksInMonth.map { week -> periodTransactions.filter { it.type == TransactionType.EXPENSE && it.date in week }.sumOf { it.amount } }
                }

                val maxValue = remember(incomeByBucket, expenseByBucket) {
                    val max = (incomeByBucket + expenseByBucket).maxOrNull() ?: 0L
                    if (max > 0) max else 1
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        WeeklyBarChart(
                            incomeData = incomeByBucket,
                            expenseData = expenseByBucket,
                            maxValue = maxValue.toFloat(),
                            selectedDayIndex = selectedIndex,
                            onDaySelected = { index -> selectedIndex = if (selectedIndex == index) null else index }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        labels.forEachIndexed { index, label ->
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = if (period == HomePeriod.MONTH) 10.sp else 14.sp),
                                color = if (selectedIndex == index) TextPrimary else TextTertiary,
                                fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayDonutChart(income: Long, expense: Long) {
    val total = income + expense
    val incomeAngle = if (total > 0) (income.toFloat() / total) * 360f else 0f

    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 26.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = androidx.compose.ui.geometry.Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

            if (total <= 0L) {
                drawArc(
                    color = DividerColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            } else {
                drawArc(
                    color = ExpensePurple,
                    startAngle = -90f,
                    sweepAngle = 360f - incomeAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                drawArc(
                    color = IncomeGreen,
                    startAngle = -90f + (360f - incomeAngle),
                    sweepAngle = incomeAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text("خالص روز", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontFamily = Vazirmatn)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                formatPersianAmount(income - expense),
                style = MaterialTheme.typography.titleMedium,
                color = if (income >= expense) IncomeGreen else ExpensePurple,
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun WeeklyBarChart(
    incomeData: List<Long>,
    expenseData: List<Long>,
    maxValue: Float,
    selectedDayIndex: Int? = null,
    onDaySelected: (Int) -> Unit = {}
) {
    val density = LocalDensity.current
    val cornerPx = with(density) { 4.dp.toPx() }
    val barCount = incomeData.size

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(barCount) {
                    detectTapGestures { offset ->
                        val groupWidth = size.width / barCount
                        val tappedIndex = (offset.x / groupWidth).toInt().coerceIn(0, barCount - 1)
                        onDaySelected(tappedIndex)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val groupWidth = width / barCount
            val barWidth = groupWidth * 0.16f
            val pairGap = with(density) { 2.dp.toPx() }
            val totalPairWidth = barWidth * 2 + pairGap

            if (selectedDayIndex != null) {
                val groupCenter = selectedDayIndex * groupWidth + groupWidth / 2
                drawRect(
                    color = TextPrimary.copy(alpha = 0.04f),
                    topLeft = androidx.compose.ui.geometry.Offset(groupCenter - groupWidth / 2, 0f),
                    size = androidx.compose.ui.geometry.Size(groupWidth, height)
                )
            }

            incomeData.forEachIndexed { index, value ->
                val groupCenter = index * groupWidth + groupWidth / 2
                val startLeft = groupCenter - totalPairWidth / 2
                val barHeight = if (maxValue > 0) (value / maxValue) * height else 0f
                val barTop = height - barHeight

                drawRoundedTopBar(startLeft, barTop, barWidth, barHeight, cornerPx, BarIncomeGreen)
            }

            expenseData.forEachIndexed { index, value ->
                val groupCenter = index * groupWidth + groupWidth / 2
                val startLeft = groupCenter - totalPairWidth / 2
                val barLeft = startLeft + barWidth + pairGap
                val barHeight = if (maxValue > 0) (value / maxValue) * height else 0f
                val barTop = height - barHeight

                drawRoundedTopBar(barLeft, barTop, barWidth, barHeight, cornerPx, BarExpensePurple)
            }
        }

        if (selectedDayIndex != null) {
            val groupFraction = (selectedDayIndex + 0.5f) / barCount
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val tooltipOffsetX = (maxWidth * groupFraction) - 58.dp
                Column(
                    modifier = Modifier
                        .offset(x = tooltipOffsetX.coerceIn(0.dp, (maxWidth - 116.dp).coerceAtLeast(0.dp)), y = 2.dp)
                        .width(116.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp), clip = false)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardWhite)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BarIncomeGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            formatPersianAmount(incomeData[selectedDayIndex]),
                            color = TextPrimary,
                            fontFamily = Vazirmatn,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BarExpensePurple))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            formatPersianAmount(expenseData[selectedDayIndex]),
                            color = TextPrimary,
                            fontFamily = Vazirmatn,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun DrawScope.drawRoundedTopBar(left: Float, top: Float, width: Float, height: Float, radius: Float, color: Color) {
    if (height <= 0 || width <= 0) return
    val r = radius.coerceAtMost(width / 2).coerceAtMost(height / 2)
    val path = Path().apply {
        moveTo(left, top + height)
        lineTo(left, top + r)
        quadraticTo(left, top, left + r, top)
        lineTo(left + width - r, top)
        quadraticTo(left + width, top, left + width, top + r)
        lineTo(left + width, top + height)
        close()
    }
    drawPath(path, color = color)
}

@Composable
fun ChartLegend(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

@Composable
fun RecentTransactions(transactions: List<com.example.nargesapp.data.model.Transaction>, accounts: List<Account>, viewModel: TransactionViewModel, navController: androidx.navigation.NavController) {
    Column {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("transactions") },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("تراکنش‌های اخیر", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("مشاهده همه", style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp), color = PrimaryGreen)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("هنوز تراکنشی ثبت نشده", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    transactions.take(5).forEachIndexed { index, transaction ->
                        if (index > 0) {
                            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                        }
                        SwipeableTransactionItem(
                            transactionId = transaction.id,
                            title = transaction.title,
                            date = transaction.date,
                            note = transaction.note,
                            amount = transaction.amount,
                            type = transaction.type,
                            category = transaction.category,
                            onDelete = { viewModel.deleteTransaction(transaction) },
                            onEdit = { navController.navigate("edit_transaction/${transaction.id}") },
                            accountName = accounts.find { it.id == transaction.accountId }?.name
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentDebtTransactions(debtTransactions: List<com.example.nargesapp.data.model.Transaction>, accounts: List<Account>, viewModel: TransactionViewModel, navController: androidx.navigation.NavController) {
    val allDebts by DebtRepository.debts.collectAsStateWithLifecycle()

    Column {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("debts") },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("تراکنش‌های طلب و بدهی", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("مشاهده همه", style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp), color = PrimaryGreen)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
            Column(modifier = Modifier.padding(16.dp)) {
                debtTransactions.take(5).forEachIndexed { index, transaction ->
                    if (index > 0) {
                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    val relatedDebt = allDebts.find { it.id == transaction.debtId }
                    val accentColor = if (transaction.type == com.example.nargesapp.data.model.TransactionType.INCOME) IncomeGreen else ExpensePurple

                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when {
                                        relatedDebt?.loanGroupId != null -> navController.navigate("loan_detail/${relatedDebt.loanGroupId}")
                                        relatedDebt != null -> navController.navigate("debt_detail/${relatedDebt.id}")
                                        else -> navController.navigate("debts")
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (relatedDebt?.loanGroupId != null) Icons.Outlined.CreditCard else Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(transaction.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold)
                                Text(
                                    PersianDateUtils.toPersianDigits(transaction.date),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    fontFamily = Vazirmatn
                                )
                            }
                            Text(
                                formatPersianAmount(transaction.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = accentColor,
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeableTransactionItem(
    transactionId: Int,
    title: String,
    date: String,
    note: String,
    amount: Long,
    type: TransactionType,
    category: String,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    accountName: String? = null
) {
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 80.dp.toPx() }
    var offsetX by remember(transactionId) { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        label = "swipeOffset"
    )

    val swipeDirection = when {
        animatedOffset > swipeThreshold -> "edit"
        animatedOffset < -swipeThreshold -> "delete"
        else -> "none"
    }

    val backgroundColor = when (swipeDirection) {
        "edit" -> Color(0xFFE8F5E9)
        "delete" -> Color(0xFFFFEBEE)
        else -> CardWhite
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

    // detectHorizontalDragGestures always reports raw/physical drag deltas
    // (unaffected by layout direction), but Modifier.offset{} and
    // Alignment.CenterStart/CenterEnd auto-mirror in RTL. Forcing Ltr here
    // keeps the finger, the slide, and the revealed icon in sync no matter
    // what LayoutDirection the screen around this item applies.
    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(vertical = 4.dp)
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
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { androidx.compose.ui.unit.IntOffset(animatedOffset.toInt(), 0) }
                .pointerInput(transactionId) {
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
                                else -> {
                                    offsetX = 0f
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount
                        }
                    )
                }
        ) {
            TransactionItem(title, date, note, amount, type, category, accountName)
        }
    }
    }
}

@Composable
fun TransactionItem(title: String, date: String, note: String, amount: Long, type: TransactionType, category: String, accountName: String? = null) {
    val icon = getCategoryIcon(category)
    val iconBg = if (type == TransactionType.INCOME) Color(0xFFE8F5E9) else Color(0xFFF3E5F5)
    val iconTint = if (type == TransactionType.INCOME) IncomeGreen else ExpensePurple
    val amountText = if (type == TransactionType.EXPENSE) -amount else amount
    val persianDate = toPersianDigits(date)

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = Modifier.fillMaxWidth().background(CardWhite),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.weight(1f)
            ) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(persianDate, style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                        if (!accountName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("|", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(accountName, style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                        }
                        if (note.isNotBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("|", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(note, style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                        }
                    }
                }
            }
            Text(
                formatPersianAmount(amountText),
                style = MaterialTheme.typography.bodyMedium,
                color = if (type == TransactionType.EXPENSE) ExpensePurple else IncomeGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "سوپرمارکت" -> Icons.Outlined.ShoppingCart
        "کافه" -> Icons.Outlined.Coffee
        "حمل‌ونقل" -> Icons.Outlined.DirectionsBus
        "سلامت" -> Icons.Outlined.HealthAndSafety
        "خرید" -> Icons.Outlined.ShoppingBag
        "آموزش" -> Icons.Outlined.School
        "سفر" -> Icons.Outlined.Flight
        "بدهی" -> Icons.Outlined.MoneyOff
        "طلب" -> Icons.Outlined.RequestQuote
        "انعام" -> Icons.Outlined.VolunteerActivism
             "متفرقه" -> Icons.Outlined.MonetizationOn
        "قبض" -> Icons.Outlined.ReceiptLong
        "تفریح" -> Icons.Outlined.SportsEsports
        "حقوق" -> Icons.Outlined.Work
        "فریلنس" -> Icons.Outlined.Laptop
        "هدیه" -> Icons.Outlined.CardGiftcard
        "طلب" -> Icons.Outlined.AccountBalanceWallet
        "سود" -> Icons.AutoMirrored.Outlined.TrendingUp
        "اجاره" -> Icons.Outlined.House
        "مغازه" -> Icons.Outlined.Store
        "فروش" -> Icons.Outlined.PointOfSale
        "پاداش" -> Icons.Outlined.EmojiEvents
        "سرمایه‌گذاری" -> Icons.Outlined.ShowChart
        else -> Icons.Outlined.MoreHoriz
    }
}
private var _homeAndGardenIcon: ImageVector? = null
val HomeAndGardenIcon: ImageVector
    get() {
        if (_homeAndGardenIcon != null) return _homeAndGardenIcon!!
        _homeAndGardenIcon = ImageVector.Builder(
            name = "home_and_garden",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(4f, 20f)
                verticalLineTo(10.63f)
                lineTo(2.2f, 12f)
                lineTo(1.03f, 10.43f)
                lineTo(12f, 2f)
                lineToRelative(11f, 8.4f)
                lineTo(21.8f, 12f)
                lineTo(12f, 4.5f)
                lineTo(6f, 9.1f)
                verticalLineTo(18f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(2f)
                horizontalLineTo(4f)
                close()
                moveToRelative(13.5f, 2.38f)
                quadToRelative(-1.05f, 0.73f, -2.31f, 0.61f)
                reflectiveQuadTo(13.03f, 21.98f)
                reflectiveQuadTo(12.01f, 19.81f)
                reflectiveQuadTo(12.63f, 17.5f)
                quadTo(11.9f, 16.45f, 12.01f, 15.19f)
                reflectiveQuadToRelative(1.01f, -2.16f)
                quadToRelative(0.9f, -0.9f, 2.16f, -1.01f)
                reflectiveQuadToRelative(2.31f, 0.61f)
                quadToRelative(1.05f, -0.73f, 2.31f, -0.61f)
                reflectiveQuadToRelative(2.16f, 1.01f)
                quadToRelative(0.9f, 0.9f, 1.01f, 2.16f)
                reflectiveQuadTo(22.38f, 17.5f)
                quadToRelative(0.73f, 1.05f, 0.61f, 2.31f)
                reflectiveQuadToRelative(-1.01f, 2.16f)
                reflectiveQuadToRelative(-2.16f, 1.01f)
                reflectiveQuadTo(17.5f, 22.38f)
                close()
                moveToRelative(0f, -2.45f)
                lineToRelative(1.15f, 0.8f)
                quadTo(19.1f, 21.05f, 19.63f, 21f)
                reflectiveQuadToRelative(0.93f, -0.45f)
                reflectiveQuadTo(21f, 19.63f)
                reflectiveQuadTo(20.73f, 18.65f)
                lineTo(19.93f, 17.5f)
                lineToRelative(0.8f, -1.15f)
                quadTo(21.05f, 15.9f, 21f, 15.38f)
                reflectiveQuadTo(20.55f, 14.45f)
                reflectiveQuadTo(19.63f, 14f)
                reflectiveQuadToRelative(-0.97f, 0.27f)
                lineToRelative(-1.15f, 0.8f)
                lineToRelative(-1.15f, -0.8f)
                quadTo(15.9f, 13.95f, 15.38f, 14f)
                reflectiveQuadToRelative(-0.92f, 0.45f)
                reflectiveQuadTo(14f, 15.38f)
                reflectiveQuadToRelative(0.28f, 0.98f)
                lineToRelative(0.8f, 1.15f)
                lineToRelative(-0.8f, 1.15f)
                quadTo(13.95f, 19.1f, 14f, 19.63f)
                reflectiveQuadToRelative(0.45f, 0.93f)
                reflectiveQuadTo(15.38f, 21f)
                reflectiveQuadToRelative(0.98f, -0.27f)
                lineToRelative(1.15f, -0.8f)
                close()
                moveToRelative(0.89f, -1.54f)
                quadToRelative(0.36f, -0.36f, 0.36f, -0.89f)
                reflectiveQuadTo(18.39f, 16.61f)
                quadTo(18.03f, 16.25f, 17.5f, 16.25f)
                quadToRelative(-0.52f, 0f, -0.89f, 0.36f)
                reflectiveQuadTo(16.25f, 17.5f)
                reflectiveQuadToRelative(0.36f, 0.89f)
                reflectiveQuadToRelative(0.89f, 0.36f)
                quadToRelative(0.53f, 0f, 0.89f, -0.36f)
                close()
                moveTo(12f, 12.25f)
                close()
                moveToRelative(5.5f, 5.25f)
                close()
            }
        }.build()
        return _homeAndGardenIcon!!
    }

@Composable
fun BottomNavBar(navController: NavController, currentRoute: String = "home", isScrolling: Boolean = false) {
    val flowerRotation = remember { Animatable(0f) }
    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            flowerRotation.animateTo(
                targetValue = flowerRotation.value + 360f,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
            )
        }
    }

    val debts by DebtRepository.debts.collectAsStateWithLifecycle()
    val hasDueSoonDebts = debts.any { debt ->
        !debt.isSettled && debt.dueDate.takeIf { it.isNotBlank() }?.let { date ->
            val diff = PersianDateUtils.daysUntil(date)
            diff != null && diff in 0..7
        } == true
    }

    val notchCenterXDp = 44.dp
    val notchRadiusDp = 29.dp
    val barTopLeftDp = 24.dp
    val barTopRightDp = 24.dp
    val barBottomRightDp = 24.dp
    val barBottomLeftDp = 24.dp

    Box(
        modifier = Modifier.fillMaxWidth().height(96.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = NotchedBarShape(notchCenterXDp, notchRadiusDp, barTopLeftDp, barTopRightDp, barBottomRightDp, barBottomLeftDp),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.15f),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(NotchedBarShape(notchCenterXDp, notchRadiusDp, barTopLeftDp, barTopRightDp, barBottomRightDp, barBottomLeftDp))
                .background(CardWhite.copy(alpha = 0.88f))
        ) {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(end = 78.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(Icons.Outlined.MoreVert, "بیشتر", currentRoute == "more", showBadge = hasDueSoonDebts) { navController.navigate("more") }
                    BottomNavItem(Icons.AutoMirrored.Outlined.ReceiptLong, "تراکنش‌ها", currentRoute == "transactions") { navController.navigate("transactions") }
                    BottomNavItem(HomeAndGardenIcon, "خانه", currentRoute == "home") { navController.navigate("home") }
                    BottomNavItem(Icons.Outlined.BarChart, "گزارش‌ها", currentRoute == "reports") { navController.navigate("reports") }
                    BottomNavItem(Icons.Outlined.ShoppingBag, "لیست خرید", currentRoute == "shopping") { navController.navigate("shopping") }
                }
            }
        }
        // FAB icon sits centered on the notch position, overlapping above the bar's top edge.
        // ADJUST FAB SIZE HERE:
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = notchCenterXDp - 25.5.dp, y = (96 - 68 - 28).dp)
                .size(50.dp)
                .shadow(elevation = 4.dp, shape = CircleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.2f))
                .clip(CircleShape)
                .background(PrimaryGreen)
                .clickable { navController.navigate("add_transaction") },
            contentAlignment = Alignment.Center
        ) {
            FabFlowerIcon(modifier = Modifier.size(28.dp).rotate(flowerRotation.value), color = Color.White)
        }
        // Label positioned independently so it lines up with the other items' label row,
        // regardless of how high the icon above sits.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = notchCenterXDp - 39.dp)
                .width(78.dp)
                .padding(bottom = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "افزودن تراکنش",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontFamily = Vazirmatn,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

class NotchedBarShape(
    private val notchCenterX: Dp,
    private val notchRadius: Dp,
    private val topLeftDp: Dp,
    private val topRightDp: Dp,
    private val bottomRightDp: Dp,
    private val bottomLeftDp: Dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val topLeftPx = with(density) { topLeftDp.toPx() }
        val topRightPx = with(density) { topRightDp.toPx() }
        val bottomRightPx = with(density) { bottomRightDp.toPx() }
        val bottomLeftPx = with(density) { bottomLeftDp.toPx() }
        val notchRadiusPx = with(density) { notchRadius.toPx() }
        val notchCenterXPx = with(density) { notchCenterX.toPx() }

        val barPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    topLeft = CornerRadius(topLeftPx, topLeftPx),
                    topRight = CornerRadius(topRightPx, topRightPx),
                    bottomRight = CornerRadius(bottomRightPx, bottomRightPx),
                    bottomLeft = CornerRadius(bottomLeftPx, bottomLeftPx)
                )
            )
        }
        val notchPath = Path().apply {
            addOval(
                Rect(
                    left = notchCenterXPx - notchRadiusPx,
                    top = 0f - notchRadiusPx,
                    right = notchCenterXPx + notchRadiusPx,
                    bottom = 0f + notchRadiusPx
                )
            )
        }
        val resultPath = Path.combine(path1 = barPath, path2 = notchPath, operation = PathOperation.Difference)
        return Outline.Generic(resultPath)
    }
}

@Composable
fun CloverFlowerIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val petalRadius = size.minDimension * 0.24f
        val offset = size.minDimension * 0.32f
        val center = Offset(size.width / 2, size.height / 2)
        listOf(
            Offset(0f, -offset),
            Offset(offset, 0f),
            Offset(0f, offset),
            Offset(-offset, 0f)
        ).forEach { petalOffset ->
            drawCircle(color = color, radius = petalRadius, center = center + petalOffset)
        }
        drawCircle(color = color, radius = petalRadius * 0.55f, center = center)
    }
}

@Composable
fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, showBadge: Boolean = false, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = androidx.compose.ui.Modifier.clickable(onClick = onClick)
    ) {
        Box {
            Icon(icon, label, tint = if (selected) androidx.compose.ui.graphics.Color(0xFF6B8E5A) else TextSecondary, modifier = androidx.compose.ui.Modifier.size(24.dp))
            if (showBadge) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .align(androidx.compose.ui.Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ExpensePurple)
                        .border(1.dp, CardWhite, CircleShape)
                )
            }
        }
        androidx.compose.material3.Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = if (selected) androidx.compose.ui.graphics.Color(0xFF6B8E5A) else TextSecondary, modifier = androidx.compose.ui.Modifier.padding(top = 2.dp))
    }
}
