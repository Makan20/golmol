package com.example.nargesapp

import android.app.Application
import com.example.nargesapp.data.local.AppDatabase
import com.example.nargesapp.data.repository.AccountRepository
import com.example.nargesapp.data.repository.DebtNotificationRepository
import com.example.nargesapp.data.repository.DebtPaymentRepository
import com.example.nargesapp.data.repository.DebtRepository
import com.example.nargesapp.data.repository.ShoppingItemRepository
import com.example.nargesapp.data.repository.TransactionRepository

class NargesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        TransactionRepository.init(database.transactionDao())
        AccountRepository.init(database.accountDao(), database.transactionDao())
        ShoppingItemRepository.init(database.shoppingItemDao())

        // این ریپازیتوری‌ها باید همیشه (حتی بدون باز بودن اکتیویتی) در دسترس باشند
        // چون سیستم یادآوری (WorkManager) مستقل از UI اجرا می‌شود
        DebtRepository.init(database.debtDao(), this)
        DebtPaymentRepository.init(database.debtPaymentDao())
        DebtNotificationRepository.init(database.debtNotificationDao())
    }
}
