package com.ordibehesht.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ordibehesht.finance.data.model.Account
import com.ordibehesht.finance.data.model.Transaction
import com.ordibehesht.finance.data.model.TransactionType
import com.ordibehesht.finance.data.repository.TransactionRepository
import com.ordibehesht.finance.ui.theme.CardWhite
import com.ordibehesht.finance.ui.theme.ExpensePurple
import com.ordibehesht.finance.ui.theme.IncomeGreen
import com.ordibehesht.finance.ui.theme.PrimaryGreen
import com.ordibehesht.finance.ui.theme.TextPrimary
import com.ordibehesht.finance.ui.theme.TextSecondary
import com.ordibehesht.finance.ui.theme.TextTertiary
import com.ordibehesht.finance.ui.theme.Vazirmatn
import com.ordibehesht.finance.ui.utils.PersianDateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private enum class TransferStage { IDLE, LOADING, SUCCESS }

/**
 * انتقال پول بین دو کارت خودِ کاربر — دو تراکنش با یک transferGroupId مشترک می‌سازد:
 * یک EXPENSE روی کارت مبدأ و یک INCOME روی کارت مقصد، هر دو با عنوان ثابت «انتقال بین
 * کارت‌ها» و دسته‌ی «انتقال». این دو تراکنش در تاریخچه‌ی هر کارت دیده می‌شوند (چون واقعاً
 * موجودی همان کارت را تغییر می‌دهند) اما چون transferGroupId دارند، از جمع کل درآمد/هزینه
 * در HomeScreen و ReportsScreen کنار گذاشته می‌شوند.
 *
 * initialFromAccountId: وقتی این دیالوگ از داخل یک کارت خاص باز می‌شود (نه از دکمه‌ی کلی
 * صفحه‌ی کارت‌ها)، همان کارت از قبل به‌عنوان مبدأ انتخاب شده است.
 */
@Composable
fun TransferDialog(
    accounts: List<Account>,
    initialFromAccountId: Int? = null,
    navController: androidx.navigation.NavController? = null,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    val activeAccounts = remember(accounts) { accounts.filter { !it.isArchived } }

    var fromAccountId by remember { mutableStateOf(initialFromAccountId ?: activeAccounts.firstOrNull()?.id) }
    var toAccountId by remember {
        mutableStateOf(activeAccounts.firstOrNull { it.id != fromAccountId }?.id)
    }
    var amountText by remember { mutableStateOf("") }
    var showAmountKeypad by remember { mutableStateOf(false) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf(TransferStage.IDLE) }
    val scope = rememberCoroutineScope()

    val parsedAmount = parsePersianAmount(amountText)
    val fromAccount = activeAccounts.find { it.id == fromAccountId }
    val toAccount = activeAccounts.find { it.id == toAccountId }
    val isValid = parsedAmount > 0 && fromAccountId != null && toAccountId != null && fromAccountId != toAccountId

    fun submit() {
        if (!isValid || stage != TransferStage.IDLE) return
        scope.launch {
            stage = TransferStage.LOADING
            delay(600)
            val groupId = UUID.randomUUID().toString()
            val date = PersianDateUtils.getCurrentPersianDate()
            val time = PersianDateUtils.getCurrentTime()
            // تراکنش سمت مبدأ (هزینه) در توضیحاتش می‌گوید پول به کدام کارت رفته، و تراکنش
            // سمت مقصد (درآمد) می‌گوید پول از کدام کارت آمده — چون عنوان هر دو یکسان است
            // («انتقال بین کارت‌ها»)، این توضیح تنها راهی است که در لیست تراکنش‌ها بشود
            // فهمید طرف مقابل انتقال کدام کارت بوده
            val fromNote = "به کارت ${toAccount?.name ?: ""}"
            val toNote = "از کارت ${fromAccount?.name ?: ""}"

            TransactionRepository.addTransaction(
                Transaction(
                    title = "انتقال بین کارت‌ها",
                    amount = parsedAmount,
                    type = TransactionType.EXPENSE,
                    category = "انتقال",
                    date = date,
                    time = time,
                    note = fromNote,
                    accountId = fromAccountId,
                    transferGroupId = groupId
                )
            )
            TransactionRepository.addTransaction(
                Transaction(
                    title = "انتقال بین کارت‌ها",
                    amount = parsedAmount,
                    type = TransactionType.INCOME,
                    category = "انتقال",
                    date = date,
                    time = time,
                    note = toNote,
                    accountId = toAccountId,
                    transferGroupId = groupId
                )
            )

            stage = TransferStage.SUCCESS
            delay(800)
            onDone()
        }
    }

    Dialog(onDismissRequest = { if (stage == TransferStage.IDLE) onDismiss() }) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(PrimaryGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.CompareArrows, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "انتقال بین کارت‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Vazirmatn
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("از کارت", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontFamily = Vazirmatn)
                    Spacer(modifier = Modifier.height(6.dp))
                    TransferAccountRow(
                        account = fromAccount,
                        placeholder = "انتخاب کارت مبدأ",
                        accentColor = ExpensePurple,
                        enabled = stage == TransferStage.IDLE,
                        onClick = { showFromPicker = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // آیکون جهت‌نمای بصری بین دو کارت — صرفاً تزئینی، برای وضوح جهت انتقال
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Outlined.CompareArrows,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("به کارت", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontFamily = Vazirmatn)
                    Spacer(modifier = Modifier.height(6.dp))
                    TransferAccountRow(
                        account = toAccount,
                        placeholder = "انتخاب کارت مقصد",
                        accentColor = IncomeGreen,
                        enabled = stage == TransferStage.IDLE,
                        onClick = { showToPicker = true }
                    )

                    if (fromAccountId != null && toAccountId != null && fromAccountId == toAccountId) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "کارت مبدأ و مقصد نمی‌توانند یکی باشند",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExpensePurple,
                            fontFamily = Vazirmatn
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text("مبلغ", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontFamily = Vazirmatn)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF7F7F7))
                            .clickable(enabled = stage == TransferStage.IDLE) { showAmountKeypad = true }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (amountText.isBlank()) "مبلغ را وارد کن" else "${PersianDateUtils.toPersianDigits(formatAmountInput(amountText))} تومان",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (amountText.isBlank()) TextTertiary else TextPrimary,
                            fontWeight = if (amountText.isBlank()) FontWeight.Normal else FontWeight.Bold,
                            fontFamily = Vazirmatn
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDismiss, enabled = stage == TransferStage.IDLE) {
                            Text("انصراف", color = TextTertiary, fontFamily = Vazirmatn)
                        }

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isValid) PrimaryGreen else TextTertiary.copy(alpha = 0.3f))
                                .clickable(
                                    enabled = isValid && stage == TransferStage.IDLE,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { submit() }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (stage) {
                                    TransferStage.LOADING -> {
                                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                        Spacer(Modifier.width(6.dp))
                                        Text("در حال انتقال ...", color = Color.White, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    TransferStage.SUCCESS -> {
                                        Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("انجام شد", color = Color.White, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    TransferStage.IDLE -> {
                                        Text("انتقال", color = Color.White, fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFromPicker) {
        AccountPickerDialog(
            accounts = activeAccounts,
            selectedAccountId = fromAccountId,
            actionColor = ExpensePurple,
            onDismiss = { showFromPicker = false },
            onConfirm = {
                fromAccountId = it
                showFromPicker = false
            },
            onAddNew = {
                showFromPicker = false
                if (navController != null) {
                    onDismiss()
                    navController.navigate("cards")
                }
            }
        )
    }

    if (showToPicker) {
        AccountPickerDialog(
            accounts = activeAccounts,
            selectedAccountId = toAccountId,
            actionColor = IncomeGreen,
            onDismiss = { showToPicker = false },
            onConfirm = {
                toAccountId = it
                showToPicker = false
            },
            onAddNew = {
                showToPicker = false
                if (navController != null) {
                    onDismiss()
                    navController.navigate("cards")
                }
            }
        )
    }

    if (showAmountKeypad) {
        AmountKeypadDialog(
            initialAmount = amountText,
            accentColor = PrimaryGreen,
            onDismiss = { showAmountKeypad = false },
            onConfirm = {
                amountText = it
                showAmountKeypad = false
            },
            dimBehind = false
        )
    }
}

@Composable
private fun TransferAccountRow(
    account: Account?,
    placeholder: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF7F7F7))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (account != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    account.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )
            }
        } else {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                fontFamily = Vazirmatn
            )
        }
    }
}
