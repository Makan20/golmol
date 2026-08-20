package com.example.nargesapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val isPurchased: Boolean = false,
    val createdDate: String,
    // Set once this item has been turned into a real Transaction, so the UI
    // can show it as "recorded" and prevent adding it a second time.
    val addedToTransactions: Boolean = false
)
