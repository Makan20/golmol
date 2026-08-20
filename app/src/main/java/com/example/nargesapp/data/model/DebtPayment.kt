package com.example.nargesapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// تاریخچه‌ی هر تسویه (جزئی یا کامل) روی یک آیتم طلب/بدهی
@Entity(tableName = "debt_payments")
data class DebtPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val debtId: Int,
    val amount: Long,
    val date: String,
    val accountId: Int? = null
)
