package com.ordibehesht.finance.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.ordibehesht.finance.ui.theme.*
import com.ordibehesht.finance.ui.utils.BiometricAuthHelper
import com.ordibehesht.finance.ui.utils.LockPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockSetupScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var isLockEnabled by remember { mutableStateOf(LockPreferences.isLockEnabled(context)) }
    // وضعیت بیومتریک دستگاه؛ فقط یک‌بار در ورود به صفحه چک می‌شود (کاربر معمولاً وسط همین صفحه
    // دستگاهش را از حالت «بدون اثرانگشت ثبت‌شده» خارج نمی‌کند، ولی اگر برگردد این مقدار به‌روز می‌شود)
    var biometricStatus by remember { mutableStateOf(BiometricAuthHelper.checkStatus(activity)) }
    val isBiometricReady = biometricStatus == BiometricAuthHelper.BiometricStatus.AVAILABLE
    // اگر گوشی هیچ قفل صفحه‌ای (رمز/الگو/PIN گوشی) نداشته باشد، مسیر «خروج اضطراری» در
    // LockScreen هم کار نمی‌کند — یعنی فراموشی PIN اپ در این حالت واقعاً بدون راه برگشت است.
    // این را فقط یک‌بار موقع ورود به صفحه چک می‌کنیم تا هنگام تنظیم PIN هشدار بدهیم
    val isDeviceSecure = remember { BiometricAuthHelper.isDeviceSecure(activity) }

    var isPinSet by remember { mutableStateOf(LockPreferences.isPinSet(context)) }
    // جریان تنظیم PIN: مرحله‌ی اول وارد کردن ۴ رقم، مرحله‌ی دوم تکرار همان ۴ رقم برای تأیید.
    // اگر تکرار با مرحله‌ی اول نخواند، به مرحله‌ی اول برمی‌گردد (نه خطای گمراه‌کننده)
    var showPinSetupFlow by remember { mutableStateOf(false) }
    var pinSetupStage by remember { mutableStateOf(0) } // 0=وارد کردن، 1=تکرار
    var firstPinEntry by remember { mutableStateOf("") }
    var pinSetupInput by remember { mutableStateOf("") }
    var pinSetupError by remember { mutableStateOf(false) }
    var showRemovePinConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(pinSetupInput) {
        if (pinSetupInput.length == PIN_LENGTH) {
            kotlinx.coroutines.delay(120)
            if (pinSetupStage == 0) {
                firstPinEntry = pinSetupInput
                pinSetupInput = ""
                pinSetupStage = 1
            } else {
                if (pinSetupInput == firstPinEntry) {
                    // نکته: setPin از این نسخه به بعد شامل یک محاسبه‌ی PBKDF2 (عمداً کند) است؛
                    // با withContext از UI thread بیرون می‌بریم — مشابه checkPin در LockScreen.
                    withContext(Dispatchers.Default) {
                        LockPreferences.setPin(context, pinSetupInput)
                    }
                    isPinSet = true
                    // تنظیم رمز به‌تنهایی هم باید قفل برنامه را فعال کند — وگرنه رمزی که کاربر
                    // همین الان تنظیم کرد هیچ‌وقت واقعاً استفاده نمی‌شود، چون MainActivity فقط
                    // بر اساس isLockEnabled تصمیم می‌گیرد LockScreen نشان داده شود یا نه
                    if (!isLockEnabled) {
                        LockPreferences.setLockEnabled(context, true)
                        isLockEnabled = true
                    }
                    showPinSetupFlow = false
                    pinSetupStage = 0
                    firstPinEntry = ""
                    pinSetupInput = ""
                } else {
                    pinSetupError = true
                    pinSetupInput = ""
                    pinSetupStage = 0
                    firstPinEntry = ""
                }
            }
        }
    }

    LaunchedEffect(pinSetupError) {
        if (pinSetupError) {
            kotlinx.coroutines.delay(400)
            pinSetupError = false
        }
    }

    if (showPinSetupFlow) {
        PinSetupDialog(
            stage = pinSetupStage,
            pinInput = pinSetupInput,
            hasError = pinSetupError,
            isDeviceSecure = isDeviceSecure,
            onDigit = { digit -> if (pinSetupInput.length < PIN_LENGTH) pinSetupInput += digit },
            onBackspace = { if (pinSetupInput.isNotEmpty()) pinSetupInput = pinSetupInput.dropLast(1) },
            onDismiss = {
                showPinSetupFlow = false
                pinSetupStage = 0
                firstPinEntry = ""
                pinSetupInput = ""
            }
        )
    }

    if (showRemovePinConfirm) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
            AlertDialog(
                onDismissRequest = { showRemovePinConfirm = false },
                containerColor = CardWhite,
                title = {
                    Text("حذف رمز عبور", fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                },
                text = {
                    Text(
                        if (isBiometricReady)
                            "رمز عبور حذف می‌شود. ورود به اپ همچنان با اثر انگشت ممکن است."
                        else
                            "رمز عبور تنها راه ورود به اپ است. اگر حذفش کنی و اثر انگشت هم فعال نباشد، قفل برنامه به‌طور کامل غیرفعال می‌شود.",
                        fontFamily = Vazirmatn,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        LockPreferences.clearPin(context)
                        isPinSet = false
                        if (!isBiometricReady) {
                            LockPreferences.setLockEnabled(context, false)
                            isLockEnabled = false
                        }
                        showRemovePinConfirm = false
                    }) {
                        Text("حذف", color = ExpensePurple, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemovePinConfirm = false }) {
                        Text("انصراف", color = TextTertiary, fontFamily = Vazirmatn, fontSize = 13.sp)
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "قفل اثر انگشت",
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
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 90.dp, end = 4.dp).size(60.dp)
            )
            FlowerDecoration(
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 110.dp, start = 4.dp).size(50.dp),
                color = ExpensePurple.copy(alpha = 0.08f)
            )

            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    PulsingFingerprintBadge()

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "از اطلاعات مالی خود محافظت کنید",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontFamily = Vazirmatn,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "با فعال‌سازی قفل اثر انگشت، دسترسی سریع، امن و خصوصی به حساب خود داشته باشید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontFamily = Vazirmatn,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                            LockFeatureRow(Icons.Outlined.FlashOn, "دسترسی سریع و آسان", "ورود به برنامه تنها با لمس حسگر")
                            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 8.dp))
                            LockFeatureRow(Icons.Outlined.VerifiedUser, "امنیت بیشتر", "اطلاعات مالی شما فقط برای شما قابل مشاهده است")
                            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 8.dp))
                            LockFeatureRow(Icons.Outlined.Lock, "حریم خصوصی", "اثر انگشت شما ذخیره نمی‌شود و به جایی ارسال نمی‌شود")
                            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 8.dp))
                            LockFeatureRow(Icons.Outlined.CheckCircle, "راحت و مطمئن", "دیگر نیازی به وارد کردن رمز عبور نیست")
                        }
                    }

                    // دکمه‌ی فعال‌سازی/وضعیت قفل اثر انگشت — درست زیر همان کارت توضیحاتش، نه
                    // پایین صفحه کنار کارت PIN (که قبلاً به‌خاطر Spacer(weight(1f)) اونجا
                    // افتاده بود و گیج‌کننده بود)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isLockEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(LightGreen),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isBiometricReady) "قفل برنامه فعال است" else "قفل برنامه با رمز عبور فعال است",
                                color = PrimaryGreen,
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            LockPreferences.setLockEnabled(context, false)
                            isLockEnabled = false
                        }) {
                            Text("غیرفعال کردن قفل", color = TextTertiary, fontFamily = Vazirmatn)
                        }
                    } else {
                        if (!isBiometricReady) {
                            val message = when (biometricStatus) {
                                BiometricAuthHelper.BiometricStatus.NOT_ENROLLED ->
                                    "روی این دستگاه هنوز اثر انگشتی ثبت نشده. اول از تنظیمات گوشی، اثر انگشت اضافه کن، بعد برگرد اینجا."
                                BiometricAuthHelper.BiometricStatus.NO_HARDWARE ->
                                    "این دستگاه حسگر اثر انگشت ندارد، پس قفل اثر انگشت روی آن قابل استفاده نیست."
                                BiometricAuthHelper.BiometricStatus.HARDWARE_UNAVAILABLE ->
                                    "حسگر اثر انگشت در حال حاضر در دسترس نیست. کمی بعد دوباره امتحان کن."
                                else ->
                                    "قفل اثر انگشت روی این دستگاه در دسترس نیست."
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ExpensePurple.copy(alpha = 0.08f))
                                    .padding(14.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Info,
                                    null,
                                    tint = ExpensePurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    message,
                                    color = TextSecondary,
                                    fontFamily = Vazirmatn,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Right
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Button(
                            onClick = {
                                if (!isBiometricReady) {
                                    // وضعیت را دوباره چک می‌کنیم؛ شاید کاربر از تنظیمات برگشته باشد
                                    biometricStatus = BiometricAuthHelper.checkStatus(activity)
                                    return@Button
                                }
                                BiometricAuthHelper.showBiometricPrompt(
                                    activity = activity,
                                    title = "فعال‌سازی قفل اثر انگشت",
                                    subtitle = "برای فعال‌سازی، اثر انگشت خود را تأیید کنید",
                                    onSuccess = {
                                        LockPreferences.setLockEnabled(context, true)
                                        isLockEnabled = true
                                    },
                                    onError = { },
                                    onFailed = { }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBiometricReady) PrimaryGreen else TextTertiary
                            )
                        ) {
                            Text(
                                if (isBiometricReady) "فعال‌سازی قفل اثر انگشت" else "بررسی مجدد وضعیت اثر انگشت",
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("فعلاً نه، بعداً", color = TextTertiary, fontFamily = Vazirmatn)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(LightGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Password, null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "رمز عبور عددی",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Vazirmatn
                                    )
                                    Text(
                                        if (isPinSet) "به‌عنوان راه پشتیبان ورود، همیشه در دسترس است"
                                        else "یک رمز ۴ رقمی به‌عنوان پشتیبان اثر انگشت تنظیم کن",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary,
                                        fontFamily = Vazirmatn
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showPinSetupFlow = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CardWhite),
                                    border = BorderStroke(1.dp, DividerColor)
                                ) {
                                    Text(
                                        if (isPinSet) "تغییر رمز" else "تنظیم رمز",
                                        color = PrimaryGreen,
                                        fontFamily = Vazirmatn,
                                        fontSize = 13.sp
                                    )
                                }
                                if (isPinSet) {
                                    OutlinedButton(
                                        onClick = { showRemovePinConfirm = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardWhite),
                                        border = BorderStroke(1.dp, DividerColor)
                                    ) {
                                        Text("حذف رمز", color = ExpensePurple, fontFamily = Vazirmatn, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // فاصله‌ی ثابت پایانی — چون حالا کل ستون قابل‌اسکرول است (verticalScroll)،
                    // weight(1f) دیگر معنی ندارد (ستون قابل‌اسکرول ارتفاع نامحدود دارد)
                    Spacer(modifier = Modifier.height(28.dp))
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun PulsingFingerprintBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Box(
        modifier = Modifier.size(120.dp)
            .background(LightGreen, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .background(CardWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Fingerprint,
                null,
                tint = PrimaryGreen,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun LockFeatureRow(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(LightGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = Vazirmatn)
            Text(desc, style = MaterialTheme.typography.labelMedium, color = TextTertiary, fontFamily = Vazirmatn)
        }
    }
}

/**
 * دیالوگ تمام‌صفحه‌ی تنظیم رمز عبور — دو مرحله: وارد کردن ۴ رقم، سپس تکرار همان ۴ رقم برای
 * تأیید. اگر تکرار نخواند، به مرحله‌ی اول برمی‌گردد با یک پیام خطای واضح، نه سکوت گیج‌کننده.
 */
@Composable
private fun PinSetupDialog(
    stage: Int,
    pinInput: String,
    hasError: Boolean,
    isDeviceSecure: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier.fillMaxSize().background(BackgroundLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, "انصراف", tint = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                    Box(
                        modifier = Modifier.size(88.dp).clip(CircleShape).background(LightGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Password, null, tint = PrimaryGreen, modifier = Modifier.size(38.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        if (stage == 0) "رمز عبور جدید را وارد کنید" else "رمز عبور را دوباره وارد کنید",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontFamily = Vazirmatn,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (hasError) "رمزها یکسان نبودند — دوباره از اول امتحان کن"
                        else if (stage == 0) "یک رمز ۴ رقمی انتخاب کن"
                        else "برای تأیید، همان رمز را دوباره وارد کن",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasError) ExpensePurple else TextSecondary,
                        fontFamily = Vazirmatn,
                        textAlign = TextAlign.Center
                    )

                    // هشدار: اگر گوشی هیچ قفل صفحه‌ای نداشته باشد، مسیر «خروج اضطراری» هم کار
                    // نمی‌کند — پس فراموشی همین رمز واقعاً بدون راه برگشت است. فقط مرحله‌ی اول
                    // (نه مرحله‌ی تکرار) نمایش داده می‌شود تا حواس‌پرتی اضافه در تأیید نباشد
                    if (!isDeviceSecure && stage == 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ExpensePurple.copy(alpha = 0.08f))
                                .padding(14.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                null,
                                tint = ExpensePurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "گوشی‌ات قفل صفحه (رمز/الگو/اثرانگشت) ندارد. اگر این رمز را فراموش کنی، هیچ راهی برای بازیابی‌اش نیست. پیشنهاد می‌شود اول از تنظیمات گوشی یک قفل صفحه فعال کنی.",
                                color = TextSecondary,
                                fontFamily = Vazirmatn,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                    PinDotsRow(filledCount = pinInput.length, accentColor = PrimaryGreen, hasError = hasError)

                    Spacer(modifier = Modifier.height(32.dp))
                    PinKeypad(
                        onDigit = onDigit,
                        onBackspace = onBackspace,
                        enabled = true,
                        accentColor = PrimaryGreen
                    )
                }
            }
        }
    }
}