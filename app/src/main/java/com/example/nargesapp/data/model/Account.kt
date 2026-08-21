package com.example.nargesapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val cardNumber: String = "",
    val startingBalance: Long,
    val colorIndex: Int = 0,
    val startingBalanceDate: String = "",
    val bankKey: String? = null
)
