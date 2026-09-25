package com.ordibehesht.finance.dong

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ordibehesht.finance.ui.screens.FlowerDecoration
import com.ordibehesht.finance.ui.theme.BackgroundLight
import com.ordibehesht.finance.ui.theme.CardWhite
import com.ordibehesht.finance.ui.theme.DividerColor
import com.ordibehesht.finance.ui.theme.ExpensePurple
import com.ordibehesht.finance.ui.theme.LightGreen
import com.ordibehesht.finance.ui.theme.PrimaryGreen
import com.ordibehesht.finance.ui.theme.TextPrimary
import com.ordibehesht.finance.ui.theme.TextSecondary
import com.ordibehesht.finance.ui.theme.TextTertiary
import com.ordibehesht.finance.ui.theme.Vazirmatn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DongListScreen(navController: NavController) {
    val allGroups by DongRepository.groups.collectAsStateWithLifecycle()
    val activeGroups = remember(allGroups) { allGroups.filterNot { it.isArchived } }
    val archivedGroups = remember(allGroups) { allGroups.filter { it.isArchived } }
    var showAddDialog by remember { mutableStateOf(false) }
    var groupPendingDelete by remember { mutableStateOf<DongGroup?>(null) }
    val scrollState = rememberScrollState()

    // دکمه در همان فریم اول (پیش از این‌که layout واقعاً اندازه‌گیری و maxValue محاسبه
    // شود) هم باید دیده شود، وگرنه چون ScrollState.canScrollForward در آن لحظه هنوز
    // نامعتبر است، دکمه یک لحظه مخفی می‌شود و با فید دوباره ظاهر می‌شود («پرش» ورودی).
    // بنابراین تا وقتی حداقل یک بار layout کامل شود، دکمه را همیشه نمایان نگه می‌داریم.
    //
    // نکته‌ی مهم دیگر: وقتی allGroups خالی شود (مثلاً همه‌ی گروه‌ها حذف شوند)، حالت خالی
    // یک Column جدا بدون verticalScroll(scrollState) رندر می‌شود، پس scrollState دیگر
    // هرگز recompute نمی‌شود و مقدار canScrollForward قدیمی (از قبل از حذف) روی آن
    // می‌ماند — که می‌تواند اشتباهاً true بماند و دکمه را برای همیشه مخفی نگه دارد. در
    // چنین حالتی هیچ‌چیز برای اسکرول نیست، پس دکمه باید همیشه نمایان باشد.
    var layoutSettled by remember { mutableStateOf(false) }
    val fabVisible = allGroups.isEmpty() || !layoutSettled || !scrollState.canScrollForward
    LaunchedEffect(scrollState.maxValue) {
        layoutSettled = true
    }

    // دقیقاً هم‌الگو با چرخش دکمه‌ی گلی BottomNavBar در HomeScreen: هر بار که اسکرول در
    // حال حرکت است، یک دور کامل (۳۶۰ درجه) با همان مدت و easing می‌چرخد
    val fabRotation = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            fabRotation.animateTo(
                targetValue = fabRotation.value + 360f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 1200,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "دونگ",
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

                if (allGroups.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Groups,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "هنوز گروهی نساختی",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontFamily = Vazirmatn
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "برای تقسیم هزینه‌ی یک سفر یا گردش گروهی، یک گروه جدید بساز",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            fontFamily = Vazirmatn,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(Modifier.height(12.dp))

                        if (activeGroups.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = CardWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    activeGroups.forEachIndexed { index, group ->
                                        if (index > 0) {
                                            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                                        }
                                        SwipeableDongGroupRow(
                                            group = group,
                                            onClick = { navController.navigate("dong_group/${group.id}") },
                                            onArchive = { DongRepository.setGroupArchived(group.id, true) },
                                            onDelete = { groupPendingDelete = group }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(
                                "برای آرشیو گروه به راست و برای حذف به چپ بکشید.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                fontFamily = Vazirmatn,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // همه‌ی گروه‌ها آرشیو شده‌اند — به‌جای فضای خالی و عجیب بالای
                            // بخش آرشیو، یک راهنمای کوچک برای ساخت گروه جدید نشان می‌دهیم
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                FlowerDecoration(
                                    modifier = Modifier.size(56.dp),
                                    color = PrimaryGreen.copy(alpha = 0.14f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "گروه فعالی نداری",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Vazirmatn
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "همه‌ی گروه‌ها آرشیو شده‌اند؛ برای شروع یک گروه جدید بساز\nیا یکی از گروه‌های آرشیوشده را برگردان",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary,
                                    fontFamily = Vazirmatn,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (archivedGroups.isNotEmpty()) {
                            Spacer(Modifier.height(20.dp))
                            ArchivedDongGroupsSection(
                                archivedGroups = archivedGroups,
                                onUnarchive = { group -> DongRepository.setGroupArchived(group.id, false) },
                                onDelete = { group -> groupPendingDelete = group }
                            )
                        }

                        Spacer(Modifier.height(90.dp))
                    }
                }

                // دقیقاً هم‌الگو با TransactionScreen/ReportsScreen: وقتی محتوا کوچک‌تر از
                // صفحه است یا اسکرول به انتها رسیده، دکمه نشان داده می‌شود؛ وسط اسکرول محو
                // می‌شود تا روی محتوا نیفتد.
                // موقعیت دقیقاً برابر جایگاه دکمه‌ی گلی BottomNavBar در HomeScreen است:
                // آن Box بیرونی خودش padding(horizontal=16.dp, vertical=16.dp) دارد، و
                // داخلش notchCenterXDp(44.dp)-25.5.dp افقی و (96-68-28=0 از بالای یک
                // Box با ارتفاع ۹۶dp، یعنی 96-50=46.dp از پایین آن) — جمع هر دو لایه:
                // 16+18.5=34.5.dp از لبه و 16+46=62.dp از پایین صفحه‌ی واقعی.
                //
                // نکته‌ی مهم: AnimatedVisibility عمداً این‌جا داخل یک Box جدا با align
                // گذاشته نشده — چون در فریم اولِ ظاهر شدن، قبل از این‌که اندازه‌گیری
                // نهایی انجام شود، یک لحظه با موقعیت (۰,۰) یعنی گوشه‌ی بالا-چپ صفحه
                // رندر می‌شود و بعد به‌جای درست می‌پرد (یک باگ شناخته‌شده‌ی Compose در
                // ترکیب AnimatedVisibility با Modifier.align در BoxScope‌های بزرگ).
                // به‌جایش align و padding مستقیم روی خود AnimatedVisibility اعمال می‌شود.
                androidx.compose.animation.AnimatedVisibility(
                    visible = fabVisible,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 34.5.dp, bottom = 62.dp)
                ) {
                    // عمداً از FloatingActionButton متریال استفاده نشده: shadow پیش‌فرض آن
                    // یک لایه‌ی گرافیکی جدا از محتواست و داخل AnimatedVisibility (fadeIn/
                    // fadeOut) به‌درستی با آلفای در حال تغییر هماهنگ نمی‌شود — همان چیزی که
                    // به‌شکل یک سایه‌ی مربعی محو زیر دکمه دیده می‌شد. دقیقاً هم‌الگو با دکمه‌ی
                    // گلی HomeScreen/BottomNavBar، یک Box دستی با shadow صریحِ CircleShape
                    // می‌سازیم که همه‌چیز (پس‌زمینه + سایه + کلیک) در یک Modifier chain است.
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(elevation = 4.dp, shape = CircleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.2f))
                            .clip(CircleShape)
                            .background(ExpensePurple)
                            .clickable { showAddDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        com.ordibehesht.finance.ui.screens.FabFlowerIcon(
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(fabRotation.value),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        DongAddGroupDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title ->
                val group = DongRepository.addGroup(title)
                showAddDialog = false
                navController.navigate("dong_group/${group.id}")
            }
        )
    }

    groupPendingDelete?.let { group ->
        DongConfirmDialog(
            title = "حذف گروه",
            message = "گروه «${group.title}» و همه‌ی هزینه‌های آن حذف شود؟",
            confirmLabel = "حذف",
            onDismiss = { groupPendingDelete = null },
            onConfirm = {
                DongRepository.deleteGroup(group.id)
                groupPendingDelete = null
            }
        )
    }
}

/** ردیف سویپ‌پذیر گروه‌های فعال: سویپ راست = آرشیو، سویپ چپ = حذف. */
@Composable
private fun SwipeableDongGroupRow(
    group: DongGroup,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val total = DongCalculator.totalExpense(group)
    val settled = DongCalculator.isFullySettled(group) && group.expenses.isNotEmpty()

    val density = androidx.compose.ui.platform.LocalDensity.current
    val swipeThreshold = with(density) { 90.dp.toPx() }
    var offsetX by remember(group.id) { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val animatedOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = offsetX,
        label = "dongGroupSwipeOffset"
    )

    val isRevealing = kotlin.math.abs(animatedOffset) > swipeThreshold * 0.25f

    // detectHorizontalDragGestures همیشه جابه‌جایی فیزیکی خام گزارش می‌دهد (مستقل از جهت
    // صفحه)، پس جهت را Ltr نگه می‌داریم تا انگشت، اسلاید و آیکون هماهنگ بمانند — دقیقاً
    // مطابق همین منطق در SwipeableAccountCard (CardsScreen.kt)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                            contentDescription = "آرشیو گروه",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "حذف گروه",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { androidx.compose.ui.unit.IntOffset(animatedOffset.toInt(), 0) }
                    .pointerInput(group.id) {
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
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    DongGroupRowContent(
                        group = group,
                        total = total,
                        settled = settled,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

/**
 * بخش «گروه‌های آرشیوشده» — جمع‌شونده، پیش‌فرض بسته، دقیقاً هم‌الگو با
 * ArchivedAccountsSection در CardsScreen.kt. هر گروه آرشیوشده هزینه‌ی کل و تعداد نفراتش
 * را نشان می‌دهد و یک دکمه‌ی «بازگردانی» برای برگرداندن به لیست فعال دارد — داده‌های گروه
 * هرگز پاک نمی‌شوند، فقط از دید مخفی می‌شوند.
 */
@Composable
private fun ArchivedDongGroupsSection(
    archivedGroups: List<DongGroup>,
    onUnarchive: (DongGroup) -> Unit,
    onDelete: (DongGroup) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                        "گروه‌های آرشیوشده (${DongFormat.toPersianDigits(archivedGroups.size.toString())})",
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
                archivedGroups.forEachIndexed { index, group ->
                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                    val total = DongCalculator.totalExpense(group)
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
                                    .background(PrimaryGreen.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    group.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Vazirmatn
                                )
                                Text(
                                    text = dongGroupSummaryLine(group, total),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    fontFamily = Vazirmatn
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onUnarchive(group) }) {
                                Text(
                                    "بازگردانی",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Vazirmatn
                                )
                            }
                            IconButton(
                                onClick = { onDelete(group) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "حذف کامل گروه",
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
}

/** محتوای مشترک یک ردیف گروه (آیکون + عنوان + خلاصه + برچسب تسویه)، بدون Card بیرونی،
 * چون نسخه‌ی سویپ‌پذیر خودش پس‌زمینه و شکل گرد را در لایه‌ی بیرونی مدیریت می‌کند. */
@Composable
private fun DongGroupRowContent(
    group: DongGroup,
    total: Long,
    settled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(LightGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Groups,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                group.title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = Vazirmatn
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = dongGroupSummaryLine(group, total),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontFamily = Vazirmatn
            )
        }

        if (settled) {
            Text(
                "تسویه شد",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryGreen,
                fontFamily = Vazirmatn,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(LightGreen)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DongAddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "گروه دونگ جدید",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        placeholder = {
                            Text(
                                "مثلاً سفر شمال",
                                color = TextTertiary,
                                fontFamily = Vazirmatn
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Right,
                            fontSize = 15.sp,
                            fontFamily = Vazirmatn
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ExpensePurple,
                            unfocusedBorderColor = DividerColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { if (title.isNotBlank()) onConfirm(title.trim()) },
                            enabled = title.isNotBlank()
                        ) {
                            Text(
                                "ایجاد",
                                color = if (title.isNotBlank()) ExpensePurple else TextTertiary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Vazirmatn
                            )
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
internal fun DongConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("انصراف", color = TextTertiary, fontFamily = Vazirmatn)
                        }
                        TextButton(onClick = onConfirm) {
                            Text(
                                confirmLabel,
                                color = ExpensePurple,
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

/**
 * خط خلاصه‌ی یک گروه در لیست: «X نفر · Y تومان · تاریخ ثبت گروه». اگر گروه هنوز نفری
 * نداشته باشد فقط «بدون شرکت‌کننده» نمایش داده می‌شود؛ تاریخ فقط وقتی موجود باشد اضافه می‌شود
 * (گروه‌های خیلی قدیمی‌تر از این فیچر ممکن است createdDate خالی داشته باشند).
 */
private fun dongGroupSummaryLine(group: DongGroup, total: Long): String {
    val base = if (group.participants.isEmpty()) {
        "بدون شرکت‌کننده"
    } else {
        "${group.participants.size} نفر · ${DongFormat.toman(total)} تومان"
    }
    return if (group.createdDate.isNotBlank()) {
        "$base · ${DongFormat.toPersianDigits(group.createdDate)}"
    } else {
        base
    }
}

/** ابزار کمکی فرمت/پارس مبلغ، هماهنگ با بقیه‌ی اپ (اعداد فارسی). */
object DongFormat {
    /** نمایش مبلغ با ارقام فارسی و جداکننده‌ی هزارگان — دقیقاً هم‌شکل formatPersianAmount در بقیه‌ی اپ. */
    fun toman(amount: Long): String =
        com.ordibehesht.finance.ui.screens.formatPersianAmount(amount)

    /**
     * رشته‌ی ورودیِ کیبورد مبلغ (AmountKeypadDialog) را به عدد تبدیل می‌کند.
     * از پیاده‌سازی یکتای پارس مبلغ خود پروژه استفاده می‌شود
     * (PersianDateUtils.parsePersianAmount) چون آن دیالوگ رقم‌ها را فارسی و
     * جداکننده‌ی هزار را با کاراکتر «٬» برمی‌گرداند، نه کاما و ارقام انگلیسی؛
     * یک toLongOrNull ساده روی چنین رشته‌ای همیشه null/۰ می‌داد.
     */
    fun parseAmount(text: String): Long =
        com.ordibehesht.finance.ui.utils.PersianDateUtils.parsePersianAmount(text)

    /** تبدیل رشته‌ی حاوی ارقام انگلیسی (مثل تاریخ yyyy/MM/dd یا ساعت HH:mm) به ارقام فارسی. */
    fun toPersianDigits(text: String): String =
        com.ordibehesht.finance.ui.utils.PersianDateUtils.toPersianDigits(text)
}
