package com.example.nargesapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nargesapp.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * زنگ‌های AlarmManager بعد از ری‌استارت گوشی حذف می‌شوند، پس باید تمام یادآوری‌های
 * فعال (بدهی/طلب‌های تسویه‌نشده با یادآوری روشن) را دوباره زمان‌بندی کنیم.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val debtDao = AppDatabase.getInstance(context).debtDao()
                val allDebts = debtDao.getAllDebts().first()
                allDebts.forEach { debt ->
                    DebtReminderScheduler.scheduleFor(context.applicationContext, debt)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
