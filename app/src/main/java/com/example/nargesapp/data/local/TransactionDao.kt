package com.example.nargesapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.nargesapp.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY id ASC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Insert
    suspend fun insertAll(transactions: List<Transaction>)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("UPDATE transactions SET accountId = NULL WHERE accountId = :accountId")
    suspend fun clearAccountReferences(accountId: Int)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}