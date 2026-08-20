package com.example.nargesapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.nargesapp.data.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items ORDER BY id DESC")
    fun getAllItems(): Flow<List<ShoppingItem>>

    @Insert
    suspend fun insert(item: ShoppingItem): Long

    @Insert
    suspend fun insertAll(items: List<ShoppingItem>)

    @Update
    suspend fun update(item: ShoppingItem)

    @Delete
    suspend fun delete(item: ShoppingItem)

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()
}