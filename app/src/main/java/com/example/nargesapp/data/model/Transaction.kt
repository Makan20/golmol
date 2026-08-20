package com.example.nargesapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType { INCOME, EXPENSE }

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Long,
    val type: TransactionType,
    val category: String,
    val date: String,
    val note: String = "",
    val accountId: Int? = null,
    // اگر این تراکنش نتیجه‌ی تسویه‌ی یک بدهی/طلب/قسط باشد، شناسه‌ی همان مورد اینجا ذخیره می‌شود
    val debtId: Int? = null
)
