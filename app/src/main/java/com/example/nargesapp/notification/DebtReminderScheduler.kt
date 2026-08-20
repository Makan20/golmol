package com.example.nargesapp.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.nargesapp.data.model.Debt
import com.example.nargesapp.ui.utils.PersianDateUtils

/**
 * زمان‌بندی و لغو یادآوری‌های اعلان برای طلب/بدهی با استفاده از AlarmManager.
 * برخلاف WorkManager که تضمینی برای اجرای دقیق در زمان مشخص ندارد (به‌خصوص روی گوشی‌هایی
 * با مدیریت پس‌زمینه‌ی سخت‌گیرانه مثل MIUI)، AlarmManager.setExactAndAllowWhileIdle حتی
 * در حالت Doze هم زنگ را دقیقاً در لحظه‌ی موردنظر فعال می‌کند.
 *
 * هر آیتم (در صورت فعال بودن reminderEnabled و داشتن dueDate معتبر) دو یادآوری دارد:
 * یکی ۳ روز قبل از سررسید، یکی خود روز سررسید.
 */
object DebtReminderScheduler {

    private fun requestCode(debtId: Int, isDueToday: Boolean): Int =
        debtId * 10 + (if (isDueToday) 1 else 0)

    private fun buildPendingIntent(context: Context, debtId: Int, isDueToday: Boolean): PendingIntent {
        val intent = Intent(context, DebtReminderAlarmReceiver::class.java).apply {
            putExtra(KEY_DEBT_ID, debtId)
            putExtra(KEY_IS_DUE_TODAY, isDueToday)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(debtId, isDueToday),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleFor(context: Context, debt: Debt) {
        cancelFor(context, debt.id)

        if (!debt.reminderEnabled || debt.isSettled || debt.dueDate.isBlank()) return

        val daysUntilDue = PersianDateUtils.daysUntil(debt.dueDate) ?: return
        if (daysUntilDue < 0) return // سررسید گذشته، یادآوری معنی ندارد

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // یادآوری ۳ روز قبل از سررسید (فقط اگر هنوز به آن نرسیده‌ایم)
        if (daysUntilDue > 3) {
            scheduleOne(alarmManager, context, debt.id, isDueToday = false, daysFromNow = daysUntilDue - 3)
        }

        // یادآوری روز سررسید
        scheduleOne(alarmManager, context, debt.id, isDueToday = true, daysFromNow = daysUntilDue)
    }

    private fun scheduleOne(
        alarmManager: AlarmManager,
        context: Context,
        debtId: Int,
        isDueToday: Boolean,
        daysFromNow: Long
    ) {
        val triggerAtMillis = triggerTimeMillis(daysFromNow)
        val pendingIntent = buildPendingIntent(context, debtId, isDueToday)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    // اگر کاربر مجوز زنگ دقیق را نداده باشد، حداقل یک زنگ غیردقیق زمان‌بندی می‌شود
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            // در صورت نبود مجوز روی برخی دستگاه‌ها، به زنگ غیردقیق سقوط می‌کنیم
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /** زمان مطلق (میلی‌ثانیه از epoch) برای اعلان، ساعت ۱۰ صبح روز موردنظر */
    private fun triggerTimeMillis(daysFromNow: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, daysFromNow.toInt())
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 10)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        val triggerTime = calendar.timeInMillis
        // اگر زمان محاسبه‌شده (برای امروز) از الان گذشته باشد، همین الان اعلان بیاید نه فردا
        return if (triggerTime < System.currentTimeMillis()) System.currentTimeMillis() + 5_000L else triggerTime
    }

    fun cancelFor(context: Context, debtId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context, debtId, isDueToday = false))
        alarmManager.cancel(buildPendingIntent(context, debtId, isDueToday = true))
    }
}
