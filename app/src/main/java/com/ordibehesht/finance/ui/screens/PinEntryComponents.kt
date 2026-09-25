package com.ordibehesht.finance.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ordibehesht.finance.ui.theme.*

const val PIN_LENGTH = 4

/**
 * ردیف ۴ نقطه‌ای که پیشرفت واردکردن PIN را نشان می‌دهد. نقاط پرشده با رنگ accentColor
 * (سبز برای تأیید، بنفش برای خطا) پر می‌شوند.
 */
@Composable
fun PinDotsRow(filledCount: Int, accentColor: Color, hasError: Boolean = false) {
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(hasError) {
        if (hasError) {
            // یک لرزش کوتاه افقی، همان الگوی رایج فیدبک بصری خطای PIN
            val values = listOf(0f, -12f, 12f, -8f, 8f, -4f, 4f, 0f)
            for (v in values) {
                shakeOffset.animateTo(v, animationSpec = tween(35))
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.graphicsLayer(translationX = shakeOffset.value)
    ) {
        repeat(PIN_LENGTH) { index ->
            val filled = index < filledCount
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            hasError -> ExpensePurple
                            filled -> accentColor
                            else -> DividerColor
                        }
                    )
            )
        }
    }
}

/**
 * صفحه‌کلید عددی ۰ تا ۹ + دکمه‌ی پاک‌کردن، به شکل شبکه‌ی ۳ ستونه — دقیقاً همان الگویی که
 * برای AmountKeypadDialog در بقیه‌ی اپ استفاده می‌شود.
 */
@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    enabled: Boolean,
    accentColor: Color
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9')
    )

    // صفحه‌کلید عددی همیشه چپ‌به‌راست (۱ ۲ ۳ از چپ به راست) نمایش داده می‌شود، حتی داخل
    // صفحه‌ی RTL — دقیقاً مثل صفحه‌کلید تلفن، صرف‌نظر از جهت‌نگارش بقیه‌ی صفحه
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    row.forEach { digit ->
                        PinKeypadKey(text = digit.toString(), enabled = enabled, accentColor = accentColor) {
                            onDigit(digit)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Spacer(modifier = Modifier.size(64.dp))
                PinKeypadKey(text = "0", enabled = enabled, accentColor = accentColor) { onDigit('0') }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .then(if (enabled) Modifier.clickable { onBackspace() } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = "پاک کردن",
                        tint = if (enabled) TextSecondary else TextTertiary.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKeypadKey(text: String, enabled: Boolean, accentColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.headlineSmall,
            color = if (enabled) TextPrimary else TextTertiary.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            fontFamily = Vazirmatn
        )
    }
}
