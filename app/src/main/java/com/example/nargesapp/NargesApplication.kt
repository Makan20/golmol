package com.example.nargesapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.example.nargesapp.data.local.AppDatabase
import com.example.nargesapp.data.repository.AccountRepository
import com.example.nargesapp.data.repository.DebtNotificationRepository
import com.example.nargesapp.data.repository.DebtPaymentRepository
import com.example.nargesapp.data.repository.DebtRepository
import com.example.nargesapp.data.repository.ShoppingItemRepository
import com.example.nargesapp.data.repository.TransactionRepository

class NargesApplication : Application(), ImageLoaderFactory {

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

    // معرفی دیکودر SVG به Coil — بدون این، لوگوهای بانک لود نمی‌شن
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
}
