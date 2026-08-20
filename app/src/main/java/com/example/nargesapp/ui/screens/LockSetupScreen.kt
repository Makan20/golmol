package com.example.nargesapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.example.nargesapp.ui.theme.*
import com.example.nargesapp.ui.utils.BiometricAuthHelper
import com.example.nargesapp.ui.utils.LockPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockSetupScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var isLockEnabled by remember { mutableStateOf(LockPreferences.isLockEnabled(context)) }

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

                    Spacer(modifier = Modifier.weight(1f))

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
                                "قفل اثر انگشت فعال است",
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
                        Button(
                            onClick = {
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
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text("فعال‌سازی قفل اثر انگشت", fontFamily = Vazirmatn, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("فعلاً نه، بعداً", color = TextTertiary, fontFamily = Vazirmatn)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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