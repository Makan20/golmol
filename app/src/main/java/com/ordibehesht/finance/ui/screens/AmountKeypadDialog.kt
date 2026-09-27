package com.ordibehesht.finance.ui.screens

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import com.ordibehesht.finance.ui.theme.CardWhite
import com.ordibehesht.finance.ui.theme.TextPrimary
import com.ordibehesht.finance.ui.theme.TextSecondary
import com.ordibehesht.finance.ui.theme.TextTertiary
import com.ordibehesht.finance.ui.theme.Vazirmatn
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AmountKeypadDialog(
    initialAmount: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String = "مبلغ تراکنش",
    unitLabel: String? = "تومان",
    // باگ رفع‌شده: قبلاً پیش‌فرض null بود. وقتی caller این پارامتر را پاس نمی‌داد (مثلاً برای
    // مبلغ تراکنش در AddTransactionScreen)، محافظتِ خط پایین‌تر («اگر currentDigits.length >=
    // maxDigits دیگر رقم نگیر») اصلاً فعال نمی‌شد، و formatAmountInput (که خودش دیگر truncate
    // نمی‌کند — به کامنت آن تابع مراجعه کنید) هر رشته‌ای — هرچقدر طولانی، از جمله یک مقدار
    // از پیش موجود مثل موجودی یک کارت که مستقیم پاس داده می‌شود — را کامل و بدون کوتاه‌شدن
    // نمایش می‌داد؛ تنها لایه‌ی safety، truncate خاموشِ .take(12) داخل formatAmountInput بود که
    // برای یک مقدار از پیش موجود، رقم‌های آخر (کم‌ارزش) را حذف می‌کرد و عدد را عملاً ~۱۰ برابر
    // کوچک‌تر نشان می‌داد. حالا پیش‌فرض ۱۲ است، همان سقفی که قبلاً فقط داخل formatAmountInput
    // اعمال می‌شد، اما اینجا به‌صورت «رد کردن رقمِ اضافه» عمل می‌کند نه «کوتاه کردن بعد از قبول».
    maxDigits: Int? = 12,
    // وقتی از داخل یک دیالوگ دیگر باز می‌شود false بگذارید تا پرده‌ی تیره دوبل نشود (بدون پرش)
    dimBehind: Boolean = true
) {
    var amount by remember(initialAmount) {
        mutableStateOf(initialAmount)
    }

    fun addDigit(digit: String) {
        val currentDigits = persianToEnglish(amount)
            .filter { it in '0'..'9' }

        if (maxDigits != null && currentDigits.length >= maxDigits) return

        amount = if (unitLabel != null) {
            formatAmountInput(currentDigits + digit)
        } else {
            currentDigits + digit
        }
    }

    fun removeLastDigit() {
        val currentDigits = persianToEnglish(amount)
            .filter { it in '0'..'9' }

        amount = if (unitLabel != null) {
            formatAmountInput(currentDigits.dropLast(1))
        } else {
            currentDigits.dropLast(1)
        }
    }

    // نمایش همیشه سه‌رقم‌سه‌رقم — حتی برای مقدار اولیه‌ای که خام پاس داده شده
    val displayAmount = persianToEnglish(amount)
        .filter { it in '0'..'9' }
        .toLongOrNull()
        ?.let { NumberFormat.getInstance(Locale.US).format(it) }
        ?.let { com.ordibehesht.finance.ui.utils.PersianDateUtils.toPersianDigits(it) }
        ?: "۰"

    Dialog(onDismissRequest = onDismiss) {
        if (!dimBehind) {
            val view = LocalView.current
            SideEffect {
                (view.parent as? DialogWindowProvider)?.window
                    ?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }

        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardWhite
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontFamily = Vazirmatn,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (amount.isBlank()) "۰" else displayAmount,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = if (amount.isBlank()) TextTertiary else TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )

                    if (unitLabel != null) {
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = unitLabel,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontFamily = Vazirmatn
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // شبکه‌ی صفحه‌کلید همیشه چپ‌به‌راست (۱ سمت چپ، مثل صفحه‌کلید عددی
                    // تلفن) نمایش داده می‌شود — دقیقاً همان الگوی PinKeypad — حتی داخل
                    // دیالوگی که خودش RTL است، چون این عرف واردکردن رقم فارسی‌ست
                    CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        Column {
                            KeypadRow(
                                first = "۱",
                                second = "۲",
                                third = "۳",
                                accentColor = accentColor,
                                onFirst = { addDigit("1") },
                                onSecond = { addDigit("2") },
                                onThird = { addDigit("3") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            KeypadRow(
                                first = "۴",
                                second = "۵",
                                third = "۶",
                                accentColor = accentColor,
                                onFirst = { addDigit("4") },
                                onSecond = { addDigit("5") },
                                onThird = { addDigit("6") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            KeypadRow(
                                first = "۷",
                                second = "۸",
                                third = "۹",
                                accentColor = accentColor,
                                onFirst = { addDigit("7") },
                                onSecond = { addDigit("8") },
                                onThird = { addDigit("9") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                KeypadButton(
                                    text = "پاک",
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = Color(0xFFF5F5F5),
                                    contentColor = TextSecondary,
                                    onClick = { removeLastDigit() }
                                )

                                KeypadButton(
                                    text = "۰",
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = Color(0xFFF5F5F5),
                                    contentColor = TextPrimary,
                                    onClick = { addDigit("0") }
                                )

                                KeypadButton(
                                    text = "تأیید",
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = accentColor,
                                    contentColor = Color.White,
                                    onClick = { onConfirm(amount) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "انصراف",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            }
        }
    }
}

private const val CARD_NUMBER_LENGTH = 16

/**
 * فرمت خام رقم‌های شماره کارت را به شکل گروه‌بندی‌شده‌ی ۴تایی برای نمایش تبدیل
 * می‌کند، مثلاً «5892101112131415» → «۵۸۹۲  ۱۰۱۱  ۱۲۱۳  ۱۴۱۵». عمداً به‌جای خط‌تیره
 * از فاصله استفاده شده، چون گلیف خط‌تیره در این فونت پایین‌تر از ارتفاع رقم‌ها و
 * خطوط پلیس‌هولدر می‌نشیند و ناهم‌تراز دیده می‌شود.
 */
private fun formatCardNumberDisplay(digits: String): String {
    val persianDigits = com.ordibehesht.finance.ui.utils.PersianDateUtils.toPersianDigits(digits)
    return persianDigits.chunked(4).joinToString("   ")
}

/**
 * کی‌پد اختصاصی برای واردکردن شماره کارت (حداکثر ۱۶ رقم)، هم‌سبک با AmountKeypadDialog:
 * همان کارت گرد و دکمه‌های شبکه‌ای، اما بدون واحد و بدون فرمت جداکننده‌ی هزارگان. مثل
 * صفحه‌کلید مبلغ، شبکه‌ی اعداد همیشه چپ‌به‌راست (۱ سمت چپ) نمایش داده می‌شود.
 */
@Composable
fun CardNumberKeypadDialog(
    initialCardNumber: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String = "شماره کارت"
) {
    var digits by remember(initialCardNumber) {
        mutableStateOf(initialCardNumber.filter { it in '0'..'9' }.take(CARD_NUMBER_LENGTH))
    }

    fun addDigit(digit: String) {
        if (digits.length >= CARD_NUMBER_LENGTH) return
        digits += digit
    }

    fun removeLastDigit() {
        digits = digits.dropLast(1)
    }

    val displayValue = if (digits.isBlank()) null else formatCardNumberDisplay(digits)

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardWhite
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontFamily = Vazirmatn,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // شماره کارت همیشه یک بلوکِ چپ‌به‌راست است (مثل شماره تلفن) تا رقم‌ها
                    // و خط‌تیره‌های جداکننده جابه‌جا یا معکوس نمایش داده نشوند
                    CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        Text(
                            text = displayValue ?: "----   ----   ----   ----",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = if (displayValue == null) TextTertiary else TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Vazirmatn
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // شبکه‌ی صفحه‌کلید همیشه چپ‌به‌راست (۱ سمت چپ) — همان الگوی
                    // AmountKeypadDialog/PinKeypad
                    CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        Column {
                            KeypadRow(
                                first = "۱",
                                second = "۲",
                                third = "۳",
                                accentColor = accentColor,
                                onFirst = { addDigit("1") },
                                onSecond = { addDigit("2") },
                                onThird = { addDigit("3") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            KeypadRow(
                                first = "۴",
                                second = "۵",
                                third = "۶",
                                accentColor = accentColor,
                                onFirst = { addDigit("4") },
                                onSecond = { addDigit("5") },
                                onThird = { addDigit("6") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            KeypadRow(
                                first = "۷",
                                second = "۸",
                                third = "۹",
                                accentColor = accentColor,
                                onFirst = { addDigit("7") },
                                onSecond = { addDigit("8") },
                                onThird = { addDigit("9") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                KeypadButton(
                                    text = "پاک",
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = Color(0xFFF5F5F5),
                                    contentColor = TextSecondary,
                                    onClick = { removeLastDigit() }
                                )

                                KeypadButton(
                                    text = "۰",
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = Color(0xFFF5F5F5),
                                    contentColor = TextPrimary,
                                    onClick = { addDigit("0") }
                                )

                                KeypadButton(
                                    text = "تأیید",
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = accentColor,
                                    contentColor = Color.White,
                                    onClick = { onConfirm(digits) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "انصراف",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontFamily = Vazirmatn
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadRow(
    first: String,
    second: String,
    third: String,
    accentColor: Color,
    onFirst: () -> Unit,
    onSecond: () -> Unit,
    onThird: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KeypadButton(
            text = first,
            modifier = Modifier.weight(1f),
            backgroundColor = accentColor.copy(alpha = 0.10f),
            contentColor = accentColor,
            onClick = onFirst
        )

        KeypadButton(
            text = second,
            modifier = Modifier.weight(1f),
            backgroundColor = accentColor.copy(alpha = 0.10f),
            contentColor = accentColor,
            onClick = onSecond
        )

        KeypadButton(
            text = third,
            modifier = Modifier.weight(1f),
            backgroundColor = accentColor.copy(alpha = 0.10f),
            contentColor = accentColor,
            onClick = onThird
        )
    }
}

@Composable
private fun KeypadButton(
    text: String,
    modifier: Modifier,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Vazirmatn
        )
    }
}
