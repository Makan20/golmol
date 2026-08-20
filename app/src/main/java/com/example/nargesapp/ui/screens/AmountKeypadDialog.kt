package com.example.nargesapp.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nargesapp.ui.theme.CardWhite
import com.example.nargesapp.ui.theme.TextPrimary
import com.example.nargesapp.ui.theme.TextSecondary
import com.example.nargesapp.ui.theme.TextTertiary
import com.example.nargesapp.ui.theme.Vazirmatn

@Composable
fun AmountKeypadDialog(
    initialAmount: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String = "مبلغ تراکنش",
    unitLabel: String? = "تومان",
    maxDigits: Int? = null
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

                    Text(
                        text = if (amount.isBlank()) "۰" else com.example.nargesapp.ui.utils.PersianDateUtils.toPersianDigits(amount),
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
                            text = "تأیید",
                            modifier = Modifier.weight(1f),
                            backgroundColor = accentColor,
                            contentColor = Color.White,
                            onClick = { onConfirm(amount) }
                        )

                        KeypadButton(
                            text = "۰",
                            modifier = Modifier.weight(1f),
                            backgroundColor = Color(0xFFF5F5F5),
                            contentColor = TextPrimary,
                            onClick = { addDigit("0") }
                        )

                        KeypadButton(
                            text = "پاک",
                            modifier = Modifier.weight(1f),
                            backgroundColor = Color(0xFFF5F5F5),
                            contentColor = TextSecondary,
                            onClick = { removeLastDigit() }
                        )
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