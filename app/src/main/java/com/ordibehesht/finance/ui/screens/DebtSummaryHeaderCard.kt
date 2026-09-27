package com.ordibehesht.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ordibehesht.finance.ui.theme.CardWhite
import com.ordibehesht.finance.ui.theme.TextPrimary
import com.ordibehesht.finance.ui.theme.TextSecondary
import com.ordibehesht.finance.ui.theme.TextTertiary
import com.ordibehesht.finance.ui.theme.Vazirmatn

/**
 * کارت خلاصه‌ی جزئیات طلب/بدهی/وام — طرح جمع‌وجورشده: مبلغ اصلی سمت چپ با وضعیت زیرش،
 * نام و آیکون شخص سمت راست با برچسب رنگی وضعیت، و نوار پیشرفت + پرداخت‌شده/باقی‌مانده پایین.
 * جایگزین طرح قبلی (وسط‌چین و عمودی) که فضای زیادی اشغال می‌کرد.
 */
@Composable
fun DebtSummaryHeaderCard(
    personName: String,
    statusLabel: String,
    amount: Long,
    amountStatusLabel: String,
    accentColor: Color,
    icon: ImageVector = Icons.Outlined.Person,
    showProgress: Boolean,
    progressFraction: Float,
    paidLabel: String,
    remainingLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // سمت راست: آیکون (راست‌ترین) و بعدش اسم، زیرش وضعیت با همان سبک.
                    // نکته‌ی مهم: چون این بلوک داخل CompositionLocalProvider(LayoutDirection.Rtl)
                    // است، Alignment.End در اینجا معنی «چپِ فیزیکی» می‌دهد نه راست (چون در RTL
                    // جهت خواندن برعکس است) — همان دلیلی که باعث می‌شد ردیف کوتاه‌ترِ وضعیت با
                    // ردیف پهن‌ترِ اسم هم‌تراز چپ شوند و در نتیجه لبه‌ی راستش از لبه‌ی راست آیکون
                    // عقب بیفتد. Alignment.Start همان چیزی است که اینجا لازم داریم: راستِ فیزیکی.
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                personName,
                                color = TextPrimary,
                                fontFamily = Vazirmatn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(accentColor))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                statusLabel,
                                color = TextTertiary,
                                fontFamily = Vazirmatn,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Right
                            )
                        }
                    }

                    // سمت چپ: مبلغ اصلی بزرگ، زیرش وضعیت مبلغ (باز مانده / تسویه‌شده)
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            detailAmount(amount),
                            color = accentColor,
                            fontFamily = Vazirmatn,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Left
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            amountStatusLabel,
                            color = TextSecondary,
                            fontFamily = Vazirmatn,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Left
                        )
                    }
                }
            }

            if (showProgress) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            remainingLabel,
                            color = accentColor,
                            fontFamily = Vazirmatn,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            paidLabel,
                            color = TextSecondary,
                            fontFamily = Vazirmatn,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
