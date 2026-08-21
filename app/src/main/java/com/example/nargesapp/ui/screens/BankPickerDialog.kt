package com.example.nargesapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.nargesapp.data.model.Bank
import com.example.nargesapp.data.model.BankCatalog
import com.example.nargesapp.ui.theme.CardWhite
import com.example.nargesapp.ui.theme.DividerColor
import com.example.nargesapp.ui.theme.PrimaryGreen
import com.example.nargesapp.ui.theme.TextPrimary
import com.example.nargesapp.ui.theme.TextSecondary
import com.example.nargesapp.ui.theme.TextTertiary
import com.example.nargesapp.ui.theme.Vazirmatn

@Composable
fun BankPickerDialog(
    selectedBankKey: String?,
    onBankSelected: (Bank) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "بانک کارت رو انتخاب کن",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontFamily = Vazirmatn,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 380.dp)
                ) {
                    items(BankCatalog.banks) { bank ->
                        BankGridItem(
                            bank = bank,
                            isSelected = bank.key == selectedBankKey,
                            onClick = {
                                onBankSelected(bank)
                                onDismiss()
                            }
                        )
                    }
                    item {
                        NoBankGridItem(
                            isSelected = selectedBankKey == null,
                            onClick = {
                                onClear()
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "انصراف",
                    color = TextTertiary,
                    fontSize = 13.sp,
                    fontFamily = Vazirmatn,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onClear()   // انصراف = خالی شدن فیلد
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun BankGridItem(bank: Bank, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isSelected)
                    Modifier
                        .background(PrimaryGreen.copy(alpha = 0.08f))
                        .border(1.5.dp, PrimaryGreen, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        BankLogo(bank = bank, size = 44)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = bank.name.replace("بانک ", ""),
            fontSize = 10.sp,
            fontFamily = Vazirmatn,
            color = TextPrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun NoBankGridItem(isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isSelected)
                    Modifier
                        .background(PrimaryGreen.copy(alpha = 0.08f))
                        .border(1.5.dp, PrimaryGreen, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DividerColor),
            contentAlignment = Alignment.Center
        ) {
            Text("—", color = TextTertiary, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "بدون بانک",
            fontSize = 10.sp,
            fontFamily = Vazirmatn,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * لوگوی بانک — از assets لود می‌شه. توی گرید پیکر، فیلد دیالوگ و لیست کارت‌ها
 * همه از همین یک کامپوزبل استفاده می‌کنن.
 */
@Composable
fun BankLogo(bank: Bank, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, DividerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = "file:///android_asset/${bank.assetPath}",
            contentDescription = bank.name,
            modifier = Modifier.size((size * 0.72f).dp)
        )
    }
}
