package com.example.nargesapp.data.repository

object AppResetManager {
    fun resetAllData() {
        TransactionRepository.clearAll()
        AccountRepository.clearAll()
        ShoppingItemRepository.clearAll()
        DebtRepository.clearAll()
    }
}