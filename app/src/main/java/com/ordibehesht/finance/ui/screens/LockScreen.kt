package com.ordibehesht.finance.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.ordibehesht.finance.ui.theme.*
import com.ordibehesht.finance.ui.utils.BiometricAuthHelper
import com.ordibehesht.finance.ui.utils.LockPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    // اگر بیومتریک اصلاً روی دستگاه در دسترس نباشد (سخت‌افزار ندارد، یا همه‌ی
    // اثرانگشت‌ها از تنظیمات گوشی پاک شده‌اند)، تلاش برای auth بی‌فایده است و
    // کاربر باید بلافاصله مسیر غیرفعال‌سازی قفل را ببیند، نه یک صفحه‌ی بن‌بست
    var biometricStatus by remember { mutableStateOf(BiometricAuthHelper.checkStatus(activity)) }
    val isBiometricReady = biometricStatus == BiometricAuthHelper.BiometricStatus.AVAILABLE
    val isDeviceSecure = remember { BiometricAuthHelper.isDeviceSecure(activity) }
    // PIN اگر تنظیم شده باشد، همیشه به‌عنوان راه ورود جایگزین/پشتیبان بیومتریک در دسترس است —
    // برخلاف بیومتریک که ممکن است هر لحظه از کار بیفتد (مثلاً اثرانگشت پاک شود)، PIN مستقل
    // از سخت‌افزار گوشی همیشه کار می‌کند
    val isPinSet = remember { LockPreferences.isPinSet(context) }

    // اگر احراز هویت چند بار پشت‌سرهم شکست بخورد (نه صرفاً انصراف کاربر)،
    // گزینه‌ی خروج اضطراری نمایش داده می‌شود تا کاربر برای همیشه از برنامه‌اش قفل نشود
    var failedAttempts by remember { mutableStateOf(0) }
    var showEmergencyUnlock by remember { mutableStateOf(false) }
    var showNoDeviceCredentialWarning by remember { mutableStateOf(false) }

    // اگر PIN تنظیم شده، همیشه به‌عنوان صفحه‌ی پیش‌فرض نمایش داده می‌شود (چون همیشه در
    // دسترس است)؛ بیومتریک (اگر آماده باشد) به‌عنوان یک میان‌بر سریع‌تر کنارش پیشنهاد می‌شود
    var showPinEntry by remember { mutableStateOf(isPinSet) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var pinFailCount by remember { mutableStateOf(0) }
    var lockoutRemainingMillis by remember { mutableStateOf(LockPreferences.remainingLockoutMillis(context)) }

    // شمارش معکوس دوره‌ی قفل موقت — هر ثانیه به‌روزرسانی می‌شود تا کاربر بداند چقدر باید صبر کند
    LaunchedEffect(lockoutRemainingMillis > 0) {
        while (lockoutRemainingMillis > 0) {
            kotlinx.coroutines.delay(1000)
            lockoutRemainingMillis = LockPreferences.remainingLockoutMillis(context)
        }
    }


    // به‌جای یک دیالوگ تأیید ساده، غیرفعال‌سازی قفل حالا خودش یک عامل احراز هویت واقعی
    // می‌طلبد: رمز/الگو/PIN خودِ قفل‌صفحه‌ی گوشی (نه بیومتریک اپ، که همین الان از دسترس
    // خارج شده). این یعنی فقط کسی که قفل خودِ گوشی را می‌داند می‌تواند مسیر اضطراری را
    // فعال کند، نه هرکسی که صرفاً گوشی را در دست دارد
    val confirmDeviceCredentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            LockPreferences.setLockEnabled(context, false)
            onUnlocked()
        }
        // اگر کاربر انصراف داد یا رمز غلط بود، هیچ اتفاقی نمی‌افتد — قفل همچنان فعال می‌ماند
    }

    fun launchDisableLockFlow() {
        if (!isDeviceSecure) {
            // گوشی اصلاً رمز/الگو/PIN ندارد، پس هیچ عامل دومی برای تأیید هویت وجود ندارد؛
            // به‌جای دور زدن این مرحله، به کاربر توضیح می‌دهیم چرا نمی‌شود
            showNoDeviceCredentialWarning = true
            return
        }
        val intent = BiometricAuthHelper.createConfirmDeviceCredentialIntent(
            activity = activity,
            title = "تأیید هویت برای غیرفعال‌سازی قفل",
            subtitle = "رمز، الگو یا PIN گوشی خود را وارد کنید"
        )
        if (intent != null) {
            confirmDeviceCredentialLauncher.launch(intent)
        } else {
            showNoDeviceCredentialWarning = true
        }
    }

    fun triggerAuth() {
        BiometricAuthHelper.showBiometricPrompt(
            activity = activity,
            title = "ورود با اثر انگشت",
            subtitle = "برای مشاهده اطلاعات مالی هویت خود را تأیید کنید",
            negativeButtonText = "انصراف",
            onSuccess = { onUnlocked() },
            onError = { message ->
                // خطاهای «قفل موقت» یا «سخت‌افزار در دسترس نیست» و مشابه، یعنی این مسیر
                // دیگر جواب نمی‌دهد؛ وضعیت را دوباره چک می‌کنیم و اگر واقعاً از دسترس خارج
                // شده، مسیر خروج اضطراری را نشان می‌دهیم
                biometricStatus = BiometricAuthHelper.checkStatus(activity)
                failedAttempts += 1
                if (!isBiometricReady || failedAttempts >= 3) {
                    showEmergencyUnlock = true
                }
            },
            onFailed = {
                failedAttempts += 1
                if (failedAttempts >= 3) showEmergencyUnlock = true
            }
        )
    }

    // وقتی رقم چهارم PIN وارد شد، بلافاصله چک می‌شود (نیازی به دکمه‌ی تأیید جدا نیست، طبق
    // الگوی رایج PIN entry). موفقیت یعنی ورود؛ شکست یعنی ثبت تلاش ناموفق (که بعد از ۳ بار
    // قفل موقت ۳۰ ثانیه‌ای فعال می‌کند) + پاک کردن ورودی برای تلاش بعدی
    //
    // نکته: verifyPin از این نسخه به بعد PBKDF2 (عمداً کند) استفاده می‌کند، پس آن را با
    // withContext(Dispatchers.Default) از UI thread بیرون می‌بریم تا حتی چند فریم jank هم
    // ایجاد نشود — قبلاً (با SHA-256 ساده و سریع) این تفاوتی نمی‌کرد.
    suspend fun checkPin() {
        val isCorrect = withContext(Dispatchers.Default) {
            LockPreferences.verifyPin(context, pinInput)
        }
        if (isCorrect) {
            LockPreferences.clearFailedAttempts(context)
            onUnlocked()
        } else {
            pinError = true
            LockPreferences.recordFailedPinAttempt(context)
            lockoutRemainingMillis = LockPreferences.remainingLockoutMillis(context)
            pinFailCount += 1
            // بعد از ۶ بار اشتباه (یعنی حدوداً دو دوره‌ی کامل قفل موقت)، گزینه‌ی اضطراری هم
            // نمایش داده می‌شود — شاید کاربر واقعاً PIN را فراموش کرده باشد
            if (pinFailCount >= 6) showEmergencyUnlock = true
            pinInput = ""
        }
    }

    LaunchedEffect(pinInput) {
        if (pinInput.length == PIN_LENGTH) {
            kotlinx.coroutines.delay(120) // نمایش کوتاه نقطه‌ی چهارم قبل از چک کردن
            checkPin()
        }
    }

    LaunchedEffect(pinError) {
        if (pinError) {
            kotlinx.coroutines.delay(400) // مدت زمان انیمیشن لرزش در PinDotsRow
            pinError = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(BackgroundLight)
    ) {
        FlowerDecoration(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 50.dp, end = 4.dp).size(50.dp)
        )
        FlowerDecoration(
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 90.dp, start = 4.dp).size(45.dp),
            color = ExpensePurple.copy(alpha = 0.08f)
        )

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showPinEntry) {
                    PinLockContent(
                        pinInput = pinInput,
                        pinError = pinError,
                        lockoutRemainingMillis = lockoutRemainingMillis,
                        isBiometricReady = isBiometricReady,
                        onDigit = { digit ->
                            if (lockoutRemainingMillis <= 0L && pinInput.length < PIN_LENGTH) {
                                pinInput += digit
                            }
                        },
                        onBackspace = {
                            if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                        },
                        onUseBiometric = {
                            showPinEntry = false
                            triggerAuth()
                        }
                    )
                } else {

                Box(
                    modifier = Modifier.size(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(158.dp)
                            .clip(CircleShape)
                            .border(1.dp, ExpensePurple.copy(alpha = 0.10f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(142.dp)
                            .clip(CircleShape)
                            .border(1.dp, ExpensePurple.copy(alpha = 0.18f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .clip(CircleShape)
                            .border(1.dp, ExpensePurple.copy(alpha = 0.28f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(LightGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Fingerprint,
                            null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "ورود با اثر انگشت",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontFamily = Vazirmatn,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (isBiometricReady)
                        "برای مشاهده اطلاعات مالی هویت خود را احراز کنید"
                    else
                        "اثر انگشتی روی این دستگاه ثبت نشده یا در دسترس نیست",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontFamily = Vazirmatn,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (isBiometricReady) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(LightGreen)
                            .clickable { triggerAuth() }
                            .padding(vertical = 12.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "ورود با اثر انگشت",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryGreen,
                            fontFamily = Vazirmatn,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            Icons.Outlined.Fingerprint,
                            null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                }

                // مسیر خروج اضطراری فقط وقتی نمایش داده می‌شود که واقعاً به بن‌بست خورده باشیم:
                // - در نمای بیومتریک: بیومتریک در دسترس نیست یا چندبار شکست خورده، و PIN هم تنظیم نشده
                // - در نمای PIN: چندبار پشت‌سرهم اشتباه زده شده (showEmergencyUnlock از منطق
                //   قفل موقت PIN هم ست می‌شود)
                // اگر PIN تنظیم شده و کاربر تازه وارد این نما شده، نیازی به گزینه‌ی اضطراری نیست
                // چون خودِ PIN مسیر عادی و همیشه در دسترس ورود است
                val showEmergencyOption = if (showPinEntry) {
                    showEmergencyUnlock
                } else {
                    (!isBiometricReady && !isPinSet) || showEmergencyUnlock
                }
                if (showEmergencyOption) {
                    Spacer(modifier = Modifier.height(if (isBiometricReady || showPinEntry) 20.dp else 0.dp))
                    TextButton(onClick = { launchDisableLockFlow() }) {
                        Text(
                            "دسترسی به قفل ممکن نیست؟ غیرفعال‌سازی قفل",
                            color = ExpensePurple,
                            fontFamily = Vazirmatn,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(LightGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.VerifiedUser, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "اطلاعات اثر انگشت فقط روی دستگاه شما ذخیره می‌شود و هرگز از دستگاه خارج نمی‌شود.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary,
                        fontFamily = Vazirmatn
                    )
                }
            }
        }

        // این حالت فقط وقتی نمایش داده می‌شود که گوشی اصلاً رمز/الگو/PIN نداشته باشد — یعنی
        // هیچ عامل دومی برای تأیید هویت قبل از غیرفعال‌سازی قفل وجود ندارد. اینجا دیگر
        // نمی‌شود قفل را «غیرفعال» کرد بدون احراز هویت؛ فقط راهنمایی می‌کنیم
        if (showNoDeviceCredentialWarning) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AlertDialog(
                    onDismissRequest = { showNoDeviceCredentialWarning = false },
                    title = {
                        Text(
                            "قفل صفحه‌ی گوشی تنظیم نشده",
                            fontFamily = Vazirmatn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Text(
                            "برای غیرفعال‌سازی قفل برنامه، باید هویت خود را با رمز، الگو یا PIN گوشی تأیید کنید. چون گوشی شما هیچ‌کدام از این‌ها را ندارد، ابتدا از تنظیمات گوشی یک قفل صفحه فعال کنید و دوباره تلاش کنید.",
                            fontFamily = Vazirmatn,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showNoDeviceCredentialWarning = false }) {
                            Text("متوجه شدم", color = ExpensePurple, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                )
            }
        }
    }
}

/**
 * محتوای نمای PIN — عنوان، ردیف نقاط، پیام خطا/قفل موقت، صفحه‌کلید عددی، و در صورت آماده
 * بودن بیومتریک، یک میان‌بر برای سوییچ سریع به نمای اثرانگشت.
 */
@Composable
private fun PinLockContent(
    pinInput: String,
    pinError: Boolean,
    lockoutRemainingMillis: Long,
    isBiometricReady: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onUseBiometric: () -> Unit
) {
    val isLockedOut = lockoutRemainingMillis > 0L

    Box(
        modifier = Modifier.size(88.dp).clip(CircleShape).background(LightGreen),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Lock, null, tint = PrimaryGreen, modifier = Modifier.size(38.dp))
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        "رمز عبور را وارد کنید",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontFamily = Vazirmatn,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        if (isLockedOut)
            "چند بار اشتباه وارد شد — ${(lockoutRemainingMillis / 1000L) + 1} ثانیه صبر کنید"
        else
            "برای مشاهده اطلاعات مالی، رمز ۴ رقمی خود را وارد کنید",
        style = MaterialTheme.typography.bodyMedium,
        color = if (isLockedOut) ExpensePurple else TextSecondary,
        fontFamily = Vazirmatn,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(28.dp))
    PinDotsRow(filledCount = pinInput.length, accentColor = PrimaryGreen, hasError = pinError)

    Spacer(modifier = Modifier.height(32.dp))
    PinKeypad(
        onDigit = onDigit,
        onBackspace = onBackspace,
        enabled = !isLockedOut,
        accentColor = PrimaryGreen
    )

    if (isBiometricReady) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.clickable(onClick = onUseBiometric),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Fingerprint, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "ورود با اثر انگشت",
                color = PrimaryGreen,
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}