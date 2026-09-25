package com.ordibehesht.finance.dong

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * موجودیت‌های Room برای ماژول دونگ.
 *
 * چون DongGroup در لایه‌ی UI (DongModels.kt) شامل لیست‌های تودرتو است و Room از رابطه‌های
 * تودرتو در یک جدول پشتیبانی نمی‌کند، هر بخش را در جدول مسطح (flat) جدای خودش نگه می‌داریم
 * و DongRepository مسئول جمع‌کردن آن‌ها به یک DongGroup کامل برای UI است.
 */

@Entity(tableName = "dong_groups")
data class DongGroupEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isArchived: Boolean = false,
    val createdDate: String = ""
)

@Entity(tableName = "dong_participants")
data class DongParticipantEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val name: String,
    val manualShare: Long? = null
)

@Entity(tableName = "dong_expenses")
data class DongExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val amount: Long,
    val payerId: String,
    val colorIndex: Int
)

@Entity(tableName = "dong_settlements")
data class DongSettlementEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val fromId: String,
    val toId: String,
    val amount: Long,
    val date: String,
    val time: String
)
