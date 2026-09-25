package com.ordibehesht.finance.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ordibehesht.finance.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// یک نمونه از هر آیتم طلب/بدهی، دو یادآوری متفاوت (۳ روز قبل و روز سررسید) دارد؛
// این کلید مشخص می‌کند این زنگ مربوط به کدام مرحله است
const val KEY_DEBT_ID = "debt_id"
const val KEY_IS_DUE_TODAY = "is_due_today"

/**
 * دریافت‌کننده‌ی زنگ هشدار دقیق (AlarmManager). این مسیر اصلی و دقیق زمان‌بندی یادآوری‌هاست —
 * AlarmManager.setAlarmClock حتی در حالت Doze هم این زنگ را دقیقاً در زمان تعیین‌شده فعال
 * می‌کند. برای گوشی‌هایی که با وجود این تضمین سیستمی، باز هم زنگ را (به‌خاطر محدودیت‌های
 * سخت‌گیرانه‌ی مخصوص OEM مثل MIUI) از کار می‌اندازند، [NotificationFallbackWorker] به‌صورت
 * دوره‌ای همین وضعیت را دوباره چک می‌کند — ر.ک. توضیحات آن فایل.
 */
class DebtReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val debtId = intent.getIntExtra(KEY_DEBT_ID, -1)
        if (debtId == -1) return
        val isDueToday = intent.getBooleanExtra(KEY_IS_DUE_TODAY, false)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val debtDao = AppDatabase.getInstance(context).debtDao()
                val debt = debtDao.getById(debtId) ?: return@launch
                DebtReminderNotifier.notifyIfNeeded(context, debt, isDueToday)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
