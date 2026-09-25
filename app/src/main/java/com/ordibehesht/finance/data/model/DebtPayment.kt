package com.ordibehesht.finance.data.model

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
    val accountId: Int? = null,
    // ساعت ثبت این پرداخت (HH:mm) — رکوردهای قدیمی‌تر از این فیلد مقدار خالی دارند و در UI
    // نمایش داده نمی‌شود
    val time: String = "",
    // توضیحات اختیاری کاربر هنگام ثبت پرداخت (مثلاً شماره‌ی تراکنش بانکی) — در تاریخچه‌ی
    // پرداخت‌ها بعد از «از کارت: ...» نمایش داده می‌شود، اگر خالی نباشد
    val note: String = ""
)
