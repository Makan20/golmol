package com.ordibehesht.finance.dong

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ordibehesht.finance.ui.theme.Vazirmatn
import java.io.File
import java.io.FileOutputStream

/**
 * دو گل خاکستریِ کم‌رنگ در گوشه‌های مخالف کارت رسید — هم‌الگو با FlowerDecoration خودِ اپ
 * (شش گلبرگ دایره‌ای دور یک مرکز)، فقط برای بافت/تزئین پس‌زمینه، بدون هیچ معنای دیگر.
 * مرکز هر گل کمی به‌سمت نوکِ همان گوشه جابه‌جا شده تا لبه‌ی کارت نیمی از گل را ببرد.
 */
@Composable
private fun DongReceiptCornerFlower(modifier: Modifier, centerOffsetFraction: androidx.compose.ui.geometry.Offset) {
    val color = Color(0xFF9E9E9E)
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val center = androidx.compose.ui.geometry.Offset(
            size.width * centerOffsetFraction.x,
            size.height * centerOffsetFraction.y
        )
        val petalRadius = size.width * 0.16f
        val centerRadius = size.width * 0.10f
        for (i in 0 until 6) {
            val angle = i * 60f * (Math.PI / 180f).toFloat()
            val petalCenter = androidx.compose.ui.geometry.Offset(
                center.x + kotlin.math.cos(angle) * centerRadius * 1.2f,
                center.y + kotlin.math.sin(angle) * centerRadius * 1.2f
            )
            drawCircle(color = color.copy(alpha = 0.13f), radius = petalRadius, center = petalCenter)
        }
        drawCircle(color = color.copy(alpha = 0.18f), radius = centerRadius * 0.6f, center = center)
    }
}

/**
 * کارت خروجیِ اشتراک‌گذاری برای یک گروه دونگ: خلاصه‌ای دیداری از وضعیت نهایی هر نفر
 * (طلبکار/بدهکار) به سبک رسید/فاکتور، که به‌صورت عکس در اپ‌های دیگر (تلگرام، واتساپ و...)
 * قابل اشتراک است.
 *
 * فقط طراحی و متن است؛ منطق «چه کسی چقدر طلبکار/بدهکار است» از همان
 * [DongCalculator.calculateStatuses] موجود می‌آید — چیزی دوباره محاسبه نمی‌شود.
 */
@Composable
fun DongSettlementShareCard(group: DongGroup, statuses: List<DongParticipantStatus>) {
    val total = DongCalculator.totalExpense(group)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            // گل گوشه‌ی بالا-راست، مرکزش به‌سمت نوک همان گوشه جابه‌جا شده
            DongReceiptCornerFlower(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-35).dp),
                centerOffsetFraction = androidx.compose.ui.geometry.Offset(0.62f, 0.38f)
            )
            // گل گوشه‌ی پایین-چپ، مرکزش به‌سمت نوک همان گوشه جابه‌جا شده
            DongReceiptCornerFlower(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (10).dp, y = 33.dp),
                centerOffsetFraction = androidx.compose.ui.geometry.Offset(0.38f, 0.62f)
            )

            Column(modifier = Modifier.padding(22.dp)) {
                // --- سربرگ رسید ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        group.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = com.ordibehesht.finance.ui.theme.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${DongFormat.toPersianDigits(group.participants.size.toString())} نفر · ${DongFormat.toman(total)} تومان",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.ordibehesht.finance.ui.theme.TextTertiary,
                        fontFamily = Vazirmatn
                    )
                }
                HorizontalDivider(
                    color = Color(0xFFE0E0E0),
                    modifier = Modifier.dashedBorderLine()
                )

                // --- ردیف‌های وضعیت هر نفر ---
                statuses.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            status.participant.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.ordibehesht.finance.ui.theme.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Vazirmatn
                        )
                        val isSettled = status.balance == 0L
                        // ترتیب: اول متن (بدهکار/طلبکار)، بعد مبلغ با علامت -/+ چسبیده به آن —
                        // طبق تصمیم نهایی، جای این دو عوض نمی‌شود.
                        val label = when {
                            isSettled -> "تسویه"
                            status.balance > 0 -> "طلبکار +${DongFormat.toman(status.balance)}"
                            else -> "بدهکار -${DongFormat.toman(kotlin.math.abs(status.balance))}"
                        }
                        val color = when {
                            isSettled -> com.ordibehesht.finance.ui.theme.TextTertiary
                            status.balance > 0 -> com.ordibehesht.finance.ui.theme.PrimaryGreen
                            else -> com.ordibehesht.finance.ui.theme.ExpensePurple
                        }
                        // بدون این CompositionLocalProvider، علامت +/- چون داخل یک والد Rtl
                        // است به انتهای بصری متن منتقل می‌شود، نه چسبیده به اولین رقم.
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = color,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Vazirmatn,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Left
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = Color(0xFFE0E0E0),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .dashedBorderLine()
                )
                Spacer(Modifier.height(12.dp))

                // --- پاورقی رسید ---
                // آخرین تسویه‌ی ثبت‌شده (اگر باشد) یعنی زمانی که گروه کامل تسویه شده؛ وگرنه
                // تاریخ ایجاد خودِ گروه نمایش داده می‌شود.
                val lastSettlement = group.settlements.maxByOrNull { "${it.date} ${it.time}" }
                val timestampLabel = if (lastSettlement != null) {
                    "تسویه‌شده در ${DongFormat.toPersianDigits(lastSettlement.date)} - ${DongFormat.toPersianDigits(lastSettlement.time)}"
                } else if (group.createdDate.isNotBlank()) {
                    "تاریخ ایجاد گروه: ${DongFormat.toPersianDigits(group.createdDate)} - ${DongFormat.toPersianDigits(group.createdTime)}"
                } else null

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🌿 ساخته‌شده با NargesApp",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.ordibehesht.finance.ui.theme.PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                    if (timestampLabel != null) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            timestampLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = com.ordibehesht.finance.ui.theme.TextTertiary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            }
        }
    }
}

/**
 * یک خط‌چین ساده (dashed) روی HorizontalDivider — چون Compose خط‌چین آماده ندارد،
 * با PathEffect.dashPathEffect روی Canvas رسم می‌شود؛ برای حس «رسید بریده‌شده».
 */
private fun Modifier.dashedBorderLine(): Modifier = this.then(
    Modifier.drawBehind {
        val dashWidth = 6.dp.toPx()
        val gapWidth = 5.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = Color(0xFFE0E0E0),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset((x + dashWidth).coerceAtMost(size.width), 0f),
                strokeWidth = 2.dp.toPx()
            )
            x += dashWidth + gapWidth
        }
    }
)

/**
 * کارت [DongSettlementShareCard] را در یک ComposeView موقتِ خارج از صفحه اندازه‌گیری،
 * layout و روی یک Bitmap رسم می‌کند، سپس آن را در cache ذخیره و با Intent.ACTION_SEND
 * (از طریق FileProvider، مطابق file_paths.xml) برای اشتراک‌گذاری باز می‌کند.
 *
 * از View.drawToBitmap به‌جای rememberGraphicsLayer استفاده شده چون این پروژه هنوز روی
 * Compose BOM قدیمی‌تر (2024.12.01) است و آن API جدیدتر در دسترس نیست؛ این روش با
 * minSdk=24 هم سازگار است.
 */
fun shareDongSettlementImage(context: Context, group: DongGroup, statuses: List<DongParticipantStatus>) {
    val activity = context.findActivity()
    if (activity == null) {
        android.widget.Toast.makeText(context, "امکان ساخت تصویر وجود ندارد", android.widget.Toast.LENGTH_SHORT).show()
        return
    }

    // یک ComposeView بدون هیچ LifecycleOwner/SavedStateRegistryOwner/ViewModelStoreOwner
    // نمی‌تواند اصلاً compose شود (کرش فوری هنگام setContent یا draw). چون این View هرگز
    // در XML یا در تصویر اصلی صفحه نیست، این سه owner را از خودِ Activity می‌گیریم — دقیقاً
    // همان owner هایی که MainActivity به صفحه‌ی اصلی هم می‌دهد.
    val lifecycleOwner = activity as? androidx.lifecycle.LifecycleOwner
    val savedStateOwner = activity as? androidx.savedstate.SavedStateRegistryOwner
    val viewModelStoreOwner = activity as? ViewModelStoreOwner
    if (lifecycleOwner == null || savedStateOwner == null || viewModelStoreOwner == null) {
        android.widget.Toast.makeText(context, "امکان ساخت تصویر وجود ندارد", android.widget.Toast.LENGTH_SHORT).show()
        return
    }

    val composeView = ComposeView(activity).apply {
        setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(savedStateOwner)
        setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        setContent {
            DongSettlementShareCard(group = group, statuses = statuses)
        }
    }

    // این View باید واقعاً attach شود تا Compose بتواند آن را measure/layout/draw کند —
    // مثل قبل که فقط composeView.measure/layout دستی صدا زده می‌شد، بدون attach واقعی،
    // رندر خالی یا کرش می‌داد. به‌جای ساختن یک پنجره‌ی سیستمی جدا (که به token معتبر پنجره
    // نیاز دارد و می‌تواند BadTokenException بدهد)، این View را با شفافیت صفر و اندازه‌ی صفر
    // به decorView خودِ Activity (که از قبل attach و visible است) اضافه می‌کنیم.
    val decorView = activity.window.decorView as android.view.ViewGroup
    val displayWidthPx = context.resources.displayMetrics.widthPixels
    composeView.alpha = 0f
    val layoutParams = android.view.ViewGroup.LayoutParams(
        displayWidthPx,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    )
    decorView.addView(composeView, layoutParams)

    // بعد از attach، دو فریم صبر می‌کنیم (نه یک فریم) تا مطمئن شویم هم Compose محتوا را
    // composed کرده و هم یک دور کامل measure/layout انجام شده — یک post تنها روی بعضی
    // دستگاه‌ها می‌تواند قبل از پایان layout واقعی اجرا شود و اندازه‌ی View هنوز صفر باشد.
    composeView.post {
        composeView.post {
        try {
            val bitmap: Bitmap = Bitmap.createBitmap(
                composeView.width.coerceAtLeast(1),
                composeView.height.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            composeView.draw(canvas)

            val shareDir = File(context.cacheDir, "dong_share").apply { mkdirs() }
            val file = File(shareDir, "dong_${group.id}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری تسویه گروه"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "خطا در ساخت تصویر اشتراک‌گذاری", android.widget.Toast.LENGTH_SHORT).show()
        } finally {
            decorView.removeView(composeView)
        }
        }
    }
}

/** نزدیک‌ترین Activity والدِ این Context را پیدا می‌کند (چون LocalContext.current در Compose
 * معمولاً یک ContextWrapper است، نه خودِ Activity). */
private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}