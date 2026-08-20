package com.example.nargesapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.nargesapp.data.model.Debt
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY isSettled ASC, id DESC")
    fun getAllDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Debt?

    @Insert
    suspend fun insert(debt: Debt): Long

    @Insert
    suspend fun insertAll(debts: List<Debt>)

    @Update
    suspend fun update(debt: Debt)

    @Delete
    suspend fun delete(debt: Debt)

    @Query("DELETE FROM debts")
    suspend fun deleteAll()
}