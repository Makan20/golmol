package com.example.nargesapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.nargesapp.data.model.DebtNotification
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
}
