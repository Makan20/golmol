package com.ordibehesht.finance.dong

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DongDao {
    @Query("SELECT * FROM dong_groups ORDER BY id DESC")
    fun getAllGroups(): Flow<List<DongGroupEntity>>

    @Query("SELECT * FROM dong_participants")
    fun getAllParticipants(): Flow<List<DongParticipantEntity>>

    @Query("SELECT * FROM dong_expenses")
    fun getAllExpenses(): Flow<List<DongExpenseEntity>>

    @Query("SELECT * FROM dong_settlements")
    fun getAllSettlements(): Flow<List<DongSettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: DongGroupEntity)

    @Update
    suspend fun updateGroup(group: DongGroupEntity)

    @Query("UPDATE dong_groups SET isArchived = :archived WHERE id = :groupId")
    suspend fun setGroupArchived(groupId: String, archived: Boolean)

    @Query("DELETE FROM dong_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    // با حذف یک گروه، هرچه به آن گروه وابسته است هم پاک می‌شود تا داده‌ی یتیم نماند —
    // Room رابطه‌ی foreign-key ندارد پس این پاک‌سازی را دستی انجام می‌دهیم
    @Query("DELETE FROM dong_participants WHERE groupId = :groupId")
    suspend fun deleteParticipantsForGroup(groupId: String)

    @Query("DELETE FROM dong_expenses WHERE groupId = :groupId")
    suspend fun deleteExpensesForGroup(groupId: String)

    @Query("DELETE FROM dong_settlements WHERE groupId = :groupId")
    suspend fun deleteSettlementsForGroup(groupId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: DongParticipantEntity)

    @Update
    suspend fun updateParticipant(participant: DongParticipantEntity)

    @Query("DELETE FROM dong_participants WHERE id = :participantId")
    suspend fun deleteParticipant(participantId: String)

    @Query("DELETE FROM dong_expenses WHERE payerId = :participantId")
    suspend fun deleteExpensesByPayer(participantId: String)

    @Query("DELETE FROM dong_settlements WHERE fromId = :participantId OR toId = :participantId")
    suspend fun deleteSettlementsInvolving(participantId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: DongExpenseEntity)

    @Query("DELETE FROM dong_expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: DongSettlementEntity)

    @Query("DELETE FROM dong_settlements WHERE id = :settlementId")
    suspend fun deleteSettlement(settlementId: String)

    // برای «پاک کردن کل دیتا»ی اپ — چهار جدول دونگ هم باید مثل بقیه‌ی داده‌ها پاک شوند
    @Query("DELETE FROM dong_groups")
    suspend fun deleteAllGroups()

    @Query("DELETE FROM dong_participants")
    suspend fun deleteAllParticipants()

    @Query("DELETE FROM dong_expenses")
    suspend fun deleteAllExpenses()

    @Query("DELETE FROM dong_settlements")
    suspend fun deleteAllSettlements()
}
