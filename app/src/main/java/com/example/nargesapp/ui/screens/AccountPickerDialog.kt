package com.example.nargesapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nargesapp.data.model.Account
import com.example.nargesapp.ui.theme.CardWhite
import com.example.nargesapp.ui.theme.TextPrimary
import com.example.nargesapp.ui.theme.TextSecondary
import com.example.nargesapp.ui.theme.TextTertiary
import com.example.nargesapp.ui.theme.Vazirmatn
import kotlinx.coroutines.launch

@Composable
fun AccountPickerDialog(
    accounts: List<Account>,
    selectedAccountId: Int?,
    actionColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onAddNew: () -> Unit
) {
    var pendingAccountId by remember(selectedAccountId, accounts) {
        mutableStateOf(selectedAccountId ?: accounts.firstOrNull()?.id)
    }

    val pendingAccount = accounts.find { it.id == pendingAccountId }
    val selectedCardColor = pendingAccount?.let {
        cardColorPalette[it.colorIndex % cardColorPalette.size]
    } ?: actionColor

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "انتخاب کارت",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Vazirmatn
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val itemHeight = 42.dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight * 3)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .height(itemHeight)
                                .clip(RoundedCornerShape(11.dp))
                                .background(selectedCardColor.copy(alpha = 0.12f))
                        )

                        AccountWheelColumn(
                            accounts = accounts,
                            selectedAccountId = pendingAccountId,
                            selectedCardColor = selectedCardColor,
                            itemHeight = itemHeight,
                            onSelected = { pendingAccountId = it.id }
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        TextButton(
                            onClick = onAddNew,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = actionColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "افزودن کارت جدید",
                                color = actionColor,
                                fontFamily = Vazirmatn,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "انصراف",
                                color = TextSecondary,
                                fontFamily = Vazirmatn
                            )
                        }

                        Button(
                            onClick = { pendingAccountId?.let(onConfirm) },
                            enabled = pendingAccountId != null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = actionColor
                            )
                        ) {
                            Text(
                                text = "تایید",
                                color = Color.White,
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountWheelColumn(
    accounts: List<Account>,
    selectedAccountId: Int?,
    selectedCardColor: Color,
    itemHeight: Dp,
    onSelected: (Account) -> Unit
) {
    val initialIndex = accounts.indexOfFirst { it.id == selectedAccountId }
        .coerceAtLeast(0)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex
    )
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            accounts.getOrNull(listState.firstVisibleItemIndex)?.let(onSelected)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(vertical = itemHeight)
    ) {
        itemsIndexed(accounts) { index, account ->
            val isSelected = account.id == selectedAccountId

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable(
    indication = null,
    interactionSource = remember { MutableInteractionSource() }
) {
    onSelected(account)
    scope.launch {
        listState.animateScrollToItem(index)
    }
},
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = account.name,
                    color = if (isSelected) selectedCardColor else TextTertiary,
                    fontSize = if (isSelected) 16.sp else 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = Vazirmatn,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
