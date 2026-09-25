package com.ordibehesht.finance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val cardNumber: String = "",
    val startingBalance: Long,
    val colorIndex: Int = 0,
    val startingBalanceDate: String = "",
    val bankKey: String? = null,
    // کارت آرشیوشده در لیست کارت‌های فعال (CardsScreen) نشان داده نمی‌شود و برای انتخاب در
    // تراکنش جدید/انتقال پیشنهاد نمی‌شود، اما تاریخچه‌اش (تراکنش‌های قبلی مرتبط با آن) و
    // موجودی محاسبه‌شده‌اش دست‌نخورده باقی می‌ماند — جایگزین امن‌تری برای حذف کامل کارت‌های
    // قدیمی که کاربر دیگر استفاده نمی‌کند ولی نمی‌خواهد سابقه‌اش از بین برود
    val isArchived: Boolean = false
)
