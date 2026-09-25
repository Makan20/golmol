package com.ordibehesht.finance

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.ordibehesht.finance.data.local.AppDatabase
import com.ordibehesht.finance.data.repository.AccountRepository
import com.ordibehesht.finance.data.repository.DebtNotificationRepository
import com.ordibehesht.finance.data.repository.DebtPaymentRepository
import com.ordibehesht.finance.data.repository.DebtRepository
import com.ordibehesht.finance.data.repository.ShoppingItemRepository
import com.ordibehesht.finance.data.repository.TransactionRepository
import com.ordibehesht.finance.dong.DongRepository
import com.ordibehesht.finance.notification.NotificationFallbackScheduler

class NargesApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        TransactionRepository.init(database.transactionDao())
        AccountRepository.init(database.accountDao(), database.transactionDao())
        ShoppingItemRepository.init(database.shoppingItemDao())
        DongRepository.init(database.dongDao())

        // این ریپازیتوری‌ها باید همیشه (حتی بدون باز بودن اکتیویتی) در دسترس باشند
        // چون سیستم یادآوری (AlarmManager در DebtReminderScheduler) مستقل از UI اجرا می‌شود
        DebtRepository.init(database.debtDao(), this)
        DebtPaymentRepository.init(database.debtPaymentDao())
        DebtNotificationRepository.init(database.debtNotificationDao())

        // لایه‌ی پشتیبان یادآوری‌ها — ر.ک. توضیحات کامل در NotificationFallbackWorker.kt.
        // enqueueUniquePeriodicWork با KEEP باعث می‌شود این فراخوانی در هر بار باز شدن اپ
        // بی‌خطر باشد و چرخه‌ی ۶ ساعته‌ی از قبل ثبت‌شده را دوباره ریست نکند
        NotificationFallbackScheduler.schedule(this)
    }

    // معرفی دیکودر SVG به Coil — بدون این، لوگوهای بانک لود نمی‌شن
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
}