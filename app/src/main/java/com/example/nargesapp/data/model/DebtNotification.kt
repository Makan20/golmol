package com.example.nargesapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// تاریخچه‌ی واقعی اعلان‌هایی که برای طلب/بدهی نمایش داده شده‌اند
@Entity(tableName = "debt_notifications")
data class DebtNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val debtId: Int,
    val title: String,
    val body: String,
    // تاریخ به فرمت جلالی yyyy/MM/dd
    val date: String,
    // ساعت به فرمت HH:mm
    val time: String,
    val isRead: Boolean = false
)
