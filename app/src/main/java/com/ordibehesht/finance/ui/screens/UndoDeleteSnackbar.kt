package com.ordibehesht.finance.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ordibehesht.finance.ui.theme.Vazirmatn

/**
 * وضعیت یک درخواست حذف در انتظار — نگه‌دارنده‌ی خودِ آیتم حذف‌شده (برای برگرداندنش
 * در صورت "برگردون") به همراه پیام مناسب برای نمایش.
 */
data class PendingUndo<T>(
    val item: T,
    val message: String = "تراکنش حذف شد"
)

/**
 * نوار پایین سفارشی برای حذف با قابلیت برگشت (Undo) — یک دایره‌ی شمارشی معکوس (۵ ثانیه)
 * که وقتی کامل می‌شود یعنی حذف قطعی شده، و یک دکمه‌ی «برگردون» که تا قبل از تمام‌شدن
 * تایمر، آیتم را به حالت اول برمی‌گرداند.
 *
 * @param pending آیتم در انتظار حذف؛ وقتی null است چیزی نمایش داده نمی‌شود
 * @param durationMillis مدت زمان مهلت برای برگرداندن (پیش‌فرض ۵ ثانیه)
 * @param onExpired وقتی تایمر کامل می‌شود و حذف قطعی تلقی می‌شود صدا زده می‌شود
 * @param onUndo وقتی کاربر «برگردون» را می‌زند صدا زده می‌شود
 */
@Composable
fun <T> UndoDeleteSnackbar(
    pending: PendingUndo<T>?,
    modifier: Modifier = Modifier,
    durationMillis: Int = 5000,
    onExpired: () -> Unit,
    onUndo: (T) -> Unit
) {
    AnimatedVisibility(
        visible = pending != null,
        enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 2 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(220)) { it / 2 },
        modifier = modifier
    ) {
        // key(pending) هر بار که یک آیتم جدید حذف می‌شود، تایمر و انیمیشن حلقه را از نو
        // شروع می‌کند — بدون این کار، اگر کاربر سریع پشت‌سرهم چند آیتم را حذف کند، تایمر
        // آیتم قبلی همچنان در حال شمارش می‌ماند و رفتار غیرمنتظره‌ای پیش می‌آید
        val current = pending
        if (current != null) {
            androidx.compose.runtime.key(current) {
                val progress = remember { Animatable(1f) }

                LaunchedEffect(current) {
                    progress.snapTo(1f)
                    progress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
                    )
                    onExpired()
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF2E2E33))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // دایره‌ی شمارش معکوس: حلقه‌ی کامل که به‌مرور در ۵ ثانیه خالی می‌شود
                            Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
                                    val strokeWidth = 2.2.dp.toPx()
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.25f),
                                        radius = size.minDimension / 2 - strokeWidth / 2,
                                        style = Stroke(width = strokeWidth)
                                    )
                                    drawArc(
                                        color = Color.White,
                                        startAngle = -90f,
                                        sweepAngle = 360f * progress.value,
                                        useCenter = false,
                                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                        size = androidx.compose.ui.geometry.Size(
                                            size.width - strokeWidth,
                                            size.height - strokeWidth
                                        ),
                                        style = Stroke(width = strokeWidth)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                current.message,
                                color = Color.White,
                                fontFamily = Vazirmatn,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                onUndo(current.item)
                            }
                        ) {
                            Text(
                                "برگردون",
                                color = Color(0xFFB39DDB),
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.AutoMirrored.Outlined.Undo,
                                contentDescription = "برگردون",
                                tint = Color(0xFFB39DDB),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
