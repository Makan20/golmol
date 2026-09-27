package com.ordibehesht.finance.ui.screens

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
import com.ordibehesht.finance.data.model.Debt
import com.ordibehesht.finance.data.model.DebtPayment
import com.ordibehesht.finance.data.model.DebtType
import com.ordibehesht.finance.ui.theme.ExpensePurple
import com.ordibehesht.finance.ui.theme.IncomeGreen
import com.ordibehesht.finance.ui.theme.PrimaryGreen
import com.ordibehesht.finance.ui.theme.TextPrimary
import com.ordibehesht.finance.ui.theme.TextSecondary
import com.ordibehesht.finance.ui.theme.TextTertiary
import com.ordibehesht.finance.ui.theme.Vazirmatn
import com.ordibehesht.finance.ui.utils.PersianDateUtils
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

/**
 * دو گل خاکستریِ کم‌رنگ در گوشه‌های مخالف کارت رسید — عیناً همان الگوی [DongReceiptCornerFlower]
 * موجود در ماژول دونگ (شش گلبرگ دایره‌ای دور یک مرکز)، اینجا جداگانه تعریف شده چون این کارت
 * در پکیج/ماژول متفاوتی (طلب/بدهی، نه دونگ) قرار دارد و به آن وابسته نیست.
 */
@Composable
private fun DebtReceiptCornerFlower(modifier: Modifier, centerOffsetFraction: androidx.compose.ui.geometry.Offset) {
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

private fun debtShareAmount(amount: Long): String {
    val formatted = NumberFormat.getInstance(Locale.US).format(amount)
    return "${PersianDateUtils.toPersianDigits(formatted)} تومان"
}

/**
 * کارت خروجیِ اشتراک‌گذاری برای یک طلب/بدهیِ تسویه‌شده (غیرقسطی): خلاصه‌ای دیداری از مبلغ
 * کل و تاریخچه‌ی پرداخت‌ها (هرکدام با تاریخ، ساعت و نام کارت پرداخت‌کننده) به سبک همان کارت
 * رسیدِ دونگ، که به‌صورت عکس در اپ‌های دیگر قابل اشتراک است.
 *
 * فقط طراحی و متن است؛ داده از همان [Debt] و لیست [DebtPayment] موجود می‌آید.
 */
@Composable
fun DebtSettlementShareCard(
    debt: Debt,
    payments: List<DebtPayment>,
    accountName: (Int?) -> String?
) {
    val accentColor = if (debt.type == DebtType.RECEIVABLE) IncomeGreen else ExpensePurple
    val relationLabel = when {
        debt.isSettled && debt.type == DebtType.RECEIVABLE -> "به من بدهکار نیست"
        debt.isSettled -> "من تسویه کردم"
        debt.type == DebtType.RECEIVABLE -> "به من بدهکار است"
        else -> "من بدهکارم"
    }
    val totalCaption = if (debt.type == DebtType.RECEIVABLE) "مبلغ کل طلب" else "مبلغ کل بدهی"
    val remainingCaption = if (debt.type == DebtType.RECEIVABLE) "طلب باقی‌مانده" else "بدهی باقی‌مانده"

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            DebtReceiptCornerFlower(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-35).dp),
                centerOffsetFraction = androidx.compose.ui.geometry.Offset(0.62f, 0.38f)
            )
            DebtReceiptCornerFlower(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 10.dp, y = 33.dp),
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
                        debt.personName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (debt.isSettled) "✓ $relationLabel" else relationLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                    Spacer(Modifier.height(10.dp))
                    if (debt.isSettled) {
                        // تسویه‌شده: فقط مبلغ کل، مثل قبل — چون دیگر مبلغ باقیمانده‌ای وجود ندارد
                        Text(
                            debtShareAmount(debt.amount),
                            style = MaterialTheme.typography.titleLarge,
                            color = accentColor,
                            fontWeight = FontWeight.Black,
                            fontFamily = Vazirmatn
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            totalCaption,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontFamily = Vazirmatn
                        )
                    } else {
                        // تسویه‌نشده: هم مبلغ کل و هم مبلغ باقیمانده نمایش داده می‌شود
                        Text(
                            debtShareAmount(debt.remainingAmount),
                            style = MaterialTheme.typography.titleLarge,
                            color = accentColor,
                            fontWeight = FontWeight.Black,
                            fontFamily = Vazirmatn
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            remainingCaption,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontFamily = Vazirmatn
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${totalCaption}: ${debtShareAmount(debt.amount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
                HorizontalDivider(
                    color = Color(0xFFE0E0E0),
                    modifier = Modifier.dashedBorderLine()
                )

                // --- تاریخچه‌ی پرداخت‌ها ---
                if (payments.isNotEmpty()) {
                    Text(
                        "تاریخچه‌ی پرداخت‌ها",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.ordibehesht.finance.ui.theme.TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                    val paymentOrdinals = listOf("اول", "دوم", "سوم", "چهارم", "پنجم", "ششم", "هفتم", "هشتم", "نهم", "دهم")
                    payments.sortedBy { it.id }.forEachIndexed { index, payment ->
                        val ordinal = paymentOrdinals.getOrNull(index) ?: "${index + 1}ام"
                        val dateTimeText = if (payment.time.isNotBlank()) {
                            "${PersianDateUtils.toPersianDigits(payment.date)} - ${PersianDateUtils.toPersianDigits(payment.time)}"
                        } else {
                            PersianDateUtils.toPersianDigits(payment.date)
                        }
                        val cardName = accountName(payment.accountId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "پرداخت $ordinal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Vazirmatn
                                )
                                Text(
                                    dateTimeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    fontFamily = Vazirmatn
                                )
                                if (cardName != null) {
                                    Text(
                                        "از کارت: $cardName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary,
                                        fontFamily = Vazirmatn
                                    )
                                }
                            }
                            // مبلغ هر پرداخت باید همیشه از چپ به راست خوانده شود (مثل کارت
                            // دونگ)، وگرنه داخل والد Rtl به‌هم می‌ریزد.
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    debtShareAmount(payment.amount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Vazirmatn,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Left
                                )
                            }
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
                // برای موارد تسویه‌شده: آخرین پرداخت ثبت‌شده همان لحظه‌ی تسویه‌ی نهایی است.
                // برای موارد تسویه‌نشده: صرفاً تاریخ/ساعت ثبت خودِ طلب/بدهی نمایش داده می‌شود،
                // حتی اگر پرداخت جزئی هم ثبت شده باشد — چون هنوز کامل تسویه نشده.
                val lastPayment = payments.maxByOrNull { "${it.date} ${it.time}" }
                val timestampLabel = if (debt.isSettled && lastPayment != null) {
                    "تسویه‌شده در ${PersianDateUtils.toPersianDigits(lastPayment.date)} - ${PersianDateUtils.toPersianDigits(lastPayment.time)}"
                } else if (debt.createdDate.isNotBlank()) {
                    val timePart = if (debt.createdTime.isNotBlank()) " - ${PersianDateUtils.toPersianDigits(debt.createdTime)}" else ""
                    "تاریخ ثبت: ${PersianDateUtils.toPersianDigits(debt.createdDate)}$timePart"
                } else null

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🌿 ساخته‌شده با NargesApp",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )
                    if (timestampLabel != null) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            timestampLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            }
        }
    }
}

/**
 * یک خط‌چین ساده (dashed) روی HorizontalDivider — عیناً همان تابع کمکی موجود در ماژول دونگ،
 * چون Compose خط‌چین آماده ندارد و اینجا هم لازم است.
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
 * کارت [DebtSettlementShareCard] را در یک ComposeView موقتِ خارج از صفحه رسم و به Bitmap
 * تبدیل می‌کند، سپس با Intent.ACTION_SEND (از طریق FileProvider) برای اشتراک‌گذاری باز
 * می‌کند — عیناً همان مکانیزم shareDongSettlementImage در ماژول دونگ.
 */
fun shareDebtSettlementImage(
    context: Context,
    debt: Debt,
    payments: List<DebtPayment>,
    accountName: (Int?) -> String?
) {
    val activity = context.findDebtShareActivity()
    if (activity == null) {
        android.widget.Toast.makeText(context, "امکان ساخت تصویر وجود ندارد", android.widget.Toast.LENGTH_SHORT).show()
        return
    }

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
            DebtSettlementShareCard(debt = debt, payments = payments, accountName = accountName)
        }
    }

    val decorView = activity.window.decorView as android.view.ViewGroup
    val displayWidthPx = context.resources.displayMetrics.widthPixels
    composeView.alpha = 0f
    val layoutParams = android.view.ViewGroup.LayoutParams(
        displayWidthPx,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    )
    decorView.addView(composeView, layoutParams)

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

                val shareDir = File(context.cacheDir, "debt_share").apply { mkdirs() }
                val file = File(shareDir, "debt_${debt.id}_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری تسویه"))
            } catch (e: Exception) {
                android.util.Log.e("DebtShareCard", "خطا در ساخت تصویر اشتراک‌گذاری", e)
                android.widget.Toast.makeText(context, "خطا: ${e.javaClass.simpleName} - ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            } finally {
                decorView.removeView(composeView)
            }
        }
    }
}

private tailrec fun Context.findDebtShareActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findDebtShareActivity()
    else -> null
}
