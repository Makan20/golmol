package com.example.nargesapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DebtType {
    RECEIVABLE,
    PAYABLE
}

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val personName: String,
    val amount: Long,
    val type: DebtType,
    val dueDate: String = "",
    val note: String = "",
    val createdDate: String,
    val isSettled: Boolean = false,
    // مجموع مبلغی که تا الان به‌صورت جزئی یا کامل تسویه شده است
    val paidAmount: Long = 0L,
    // آیا یادآوری اعلان برای سررسید این مورد فعال باشد
    val reminderEnabled: Boolean = true,
    // شناسه‌ی مشترک بین تمام اقساط یک وام (اگر این مورد بخشی از یک وام قسطی باشد)
    val loanGroupId: String? = null,
    // شماره‌ی این قسط در وام (۱ تا تعداد کل اقساط)
    val installmentNumber: Int? = null,
    // تعداد کل اقساط وام (روی همه‌ی اقساط یک وام یکسان است، برای نمایش «قسط X از Y»)
    val totalInstallments: Int? = null
) {
    // مبلغ باقی‌مانده = مبلغ کل منهای مجموع تسویه‌شده (هیچ‌وقت منفی نمی‌شود)
    val remainingAmount: Long
        get() = (amount - paidAmount).coerceAtLeast(0L)

    // آیا این مورد بخشی از یک وام قسطی است
    val isInstallment: Boolean
        get() = loanGroupId != null
}