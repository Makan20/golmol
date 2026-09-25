package com.ordibehesht.finance.data.model

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
        val time: String = "",
    val note: String = "",
    val accountId: Int? = null,
    // اگر این تراکنش نتیجه‌ی تسویه‌ی یک بدهی/طلب/قسط باشد، شناسه‌ی همان مورد اینجا ذخیره می‌شود
    val debtId: Int? = null,
    // اگر این تراکنش نتیجه‌ی یک پرداخت مشخص در تاریخچه‌ی بدهی باشد، id همان DebtPayment اینجا
    // ذخیره می‌شود تا ویرایش/حذف پرداخت بتواند تراکنش دقیق مرتبط را (نه با amount+date) پیدا کند
    val debtPaymentId: Int? = null,
    // اگر این تراکنش نتیجه‌ی تبدیل یک آیتم لیست خرید به تراکنش باشد، id همان ShoppingItem
    // اینجا ذخیره می‌شود تا اگر بعداً کاربر همین تراکنش را حذف کرد، بشود addedToTransactions
    // آن آیتم را به false برگرداند و آیتم دوباره قابل ثبت/ویرایش شود
    val shoppingItemId: Int? = null,
    // اگر این تراکنش نتیجه‌ی انتقال بین دو کارت خودِ کاربر باشد (نه یک درآمد/هزینه‌ی واقعی)،
    // یک شناسه‌ی مشترک اینجا ذخیره می‌شود که دو طرف انتقال (خروج از مبدأ، ورود به مقصد) را
    // به هم وصل می‌کند. این دو تراکنش در تاریخچه‌ی هر کارت و صفحه‌ی تراکنش‌ها دیده می‌شوند
    // (چون واقعاً بخشی از تاریخچه‌ی همان کارت‌اند) اما از جمع کل درآمد/هزینه در HomeScreen و
    // گزارش‌ها کنار گذاشته می‌شوند، چون جابه‌جایی داخلی پول کاربر است، نه درآمد/هزینه‌ی واقعی
    val transferGroupId: String? = null
)
