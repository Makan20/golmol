package com.example.nargesapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
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
import androidx.fragment.app.FragmentActivity
import com.example.nargesapp.ui.theme.*
import com.example.nargesapp.ui.utils.BiometricAuthHelper

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    fun triggerAuth() {
        BiometricAuthHelper.showBiometricPrompt(
            activity = activity,
            title = "ورود با اثر انگشت",
            subtitle = "برای مشاهده اطلاعات مالی هویت خود را تأیید کنید",
            negativeButtonText = "انصراف",
            onSuccess = { onUnlocked() },
            onError = { },
            onFailed = { }
        )
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
                    "برای مشاهده اطلاعات مالی هویت خود را احراز کنید",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontFamily = Vazirmatn,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

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
    }
}