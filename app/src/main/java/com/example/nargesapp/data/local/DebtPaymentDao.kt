package com.example.nargesapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.nargesapp.data.model.DebtPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtPaymentDao {
    @Query("SELECT * FROM debt_payments ORDER BY id DESC")
    fun getAllPayments(): Flow<List<DebtPayment>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY id DESC")
    fun getPaymentsForDebt(debtId: Int): Flow<List<DebtPayment>>

    @Insert
    suspend fun insert(payment: DebtPayment): Long

    @Insert
    suspend fun insertAll(payments: List<DebtPayment>)

    @Update
    suspend fun update(payment: DebtPayment)

    @Delete
    suspend fun delete(payment: DebtPayment)

    @Query("DELETE FROM debt_payments WHERE debtId = :debtId")
    suspend fun deleteForDebt(debtId: Int)

    @Query("DELETE FROM debt_payments")
    suspend fun deleteAll()
}
