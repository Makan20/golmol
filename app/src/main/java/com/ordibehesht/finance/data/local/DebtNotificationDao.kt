package com.ordibehesht.finance.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ordibehesht.finance.data.model.DebtNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtNotificationDao {
    @Query("SELECT * FROM debt_notifications ORDER BY id DESC")
    fun getAll(): Flow<List<DebtNotification>>

    @Insert
    suspend fun insert(notification: DebtNotification): Long

    @Insert
    suspend fun insertAll(notifications: List<DebtNotification>)

    @Update
    suspend fun update(notification: DebtNotification)

    @Delete
    suspend fun delete(notification: DebtNotification)

    @Query("UPDATE debt_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM debt_notifications")
    suspend fun deleteAll()

    @Query("DELETE FROM debt_notifications WHERE debtId = :debtId")
    suspend fun deleteForDebt(debtId: Int)

    // برای WorkManager پشتیبان (NotificationFallbackWorker) — قبل از نمایش یادآوری، چک
    // می‌کند آیا برای همین بدهی و همین تاریخ (یعنی «امروز») قبلاً یادآوری‌ای ثبت شده یا نه،
    // تا یادآوری تکراری در هر بار اجرای دوره‌ای Worker نمایش داده نشود
    @Query("SELECT COUNT(*) FROM debt_notifications WHERE debtId = :debtId AND date = :date")
    suspend fun countForDebtAndDate(debtId: Int, date: String): Int
}
