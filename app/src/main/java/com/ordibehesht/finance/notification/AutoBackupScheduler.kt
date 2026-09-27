package com.ordibehesht.finance.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ordibehesht.finance.MainActivity

/**
 * زمان‌بندی پشتیبان‌گیری خودکار هفتگی با AlarmManager — همان مکانیزم دقیق
 * DebtReminderScheduler (setAlarmClock به‌جای setExactAndAllowWhileIdle) تا حتی در حالت
 * Doze یا روی گوشی‌هایی با مدیریت پس‌زمینه‌ی سخت‌گیرانه (MIUI و مشابه) هم دقیقاً در زمان
 * تعیین‌شده اجرا شود.
 */
object AutoBackupScheduler {

    private const val REQUEST_CODE = 9001

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AutoBackupAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildShowIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** یک هفته بعد از همین لحظه، ساعت ۳ بامداد (کم‌ترین احتمال تداخل با استفاده‌ی فعال کاربر) */
    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)

        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 7)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 3)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val triggerAtMillis = calendar.timeInMillis

        try {
            val showIntent = buildShowIntent(context)
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
    }
}
