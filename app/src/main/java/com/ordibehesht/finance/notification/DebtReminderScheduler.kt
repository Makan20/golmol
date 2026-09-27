package com.ordibehesht.finance.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ordibehesht.finance.MainActivity
import com.ordibehesht.finance.data.model.Debt
import com.ordibehesht.finance.ui.utils.PersianDateUtils

/**
 * زمان‌بندی و لغو یادآوری‌های اعلان برای طلب/بدهی با استفاده از AlarmManager.
 * برخلاف WorkManager که تضمینی برای اجرای دقیق در زمان مشخص ندارد (به‌خصوص روی گوشی‌هایی
 * با مدیریت پس‌زمینه‌ی سخت‌گیرانه مثل MIUI)، از AlarmManager.setAlarmClock استفاده می‌شود —
 * که برخلاف setExactAndAllowWhileIdle (که فقط «تقریباً دقیق» است و محدودیت throttling دارد:
 * طبق مستندات Doze/Standby، حداکثر یک بار در هر ۹ دقیقه توسط یک اپ قابل فایر شدن است)،
 * تضمین می‌کند که زنگ دقیقاً در لحظه‌ی موردنظر فعال شود، صرف‌نظر از حالت Doze یا اینکه چند
 * زنگ دیگر هم نزدیک به همان زمان زمان‌بندی شده باشند. این دقیقاً همان مکانیزمی است که
 * اپ‌های ساعت/زنگ‌دار سیستمی استفاده می‌کنند، پس محدودیت‌های battery-saving معمول رویش
 * اعمال نمی‌شود.
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

    /** PendingIntent که فقط برای نمایش هنگام کلیک کاربر روی آیکون زنگ در نوار وضعیت
     * استفاده می‌شود (پارامتر الزامی AlarmClockInfo)؛ صرفاً اپ را باز می‌کند */
    private fun buildShowIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
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

        // یادآوری ۳ روز قبل از سررسید، همیشه ساعت ۱۰ صبح (یک یادآوری زودهنگام و عمومی است)
        if (daysUntilDue > 3) {
            scheduleOne(alarmManager, context, debt.id, isDueToday = false, daysFromNow = daysUntilDue - 3, hour = 10, minute = 0)
        }

        // یادآوری روز سررسید: اگر کاربر ساعت مشخصی برای سررسید انتخاب کرده باشد (Debt.dueTime)
        // همان ساعت استفاده می‌شود، وگرنه پیش‌فرض ساعت ۱۰ صبح
        val (dueHour, dueMinute) = parseDueTime(debt.dueTime)
        scheduleOne(alarmManager, context, debt.id, isDueToday = true, daysFromNow = daysUntilDue, hour = dueHour, minute = dueMinute)
    }

    /** dueTime با فرمت "HH:mm" را می‌خواند؛ اگر خالی یا نامعتبر بود، ۱۰:۰۰ پیش‌فرض برمی‌گردد.
     * internal (نه private) تا NotificationFallbackWorker هم از همین پیاده‌سازی استفاده کند و
     * دو نسخه‌ی جدا از یک منطق به‌مرور از هم منحرف نشوند. */
    internal fun parseDueTime(dueTime: String): Pair<Int, Int> {
        val parts = dueTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()
        val minute = parts.getOrNull(1)?.toIntOrNull()
        return if (hour != null && hour in 0..23 && minute != null && minute in 0..59) {
            hour to minute
        } else {
            10 to 0
        }
    }

    private fun scheduleOne(
        alarmManager: AlarmManager,
        context: Context,
        debtId: Int,
        isDueToday: Boolean,
        daysFromNow: Long,
        hour: Int,
        minute: Int
    ) {
        val triggerAtMillis = triggerTimeMillis(daysFromNow, hour, minute)
        val pendingIntent = buildPendingIntent(context, debtId, isDueToday)

        // minSdk این پروژه ۲۴ است، یعنی همیشه بالاتر از API 21 (جایی که setAlarmClock معرفی
        // شد)، پس نیازی به شاخه‌ی جداگانه برای نسخه‌های خیلی قدیمی نیست
        try {
            val showIntent = buildShowIntent(context)
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: SecurityException) {
            // در صورت نبود مجوز زنگ دقیق (مثلاً کاربر آن را از تنظیمات لغو کرده)،
            // به زنگ غیردقیق سقوط می‌کنیم تا حداقل با کمی تأخیر اعلان برسد
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /** زمان مطلق (میلی‌ثانیه از epoch) برای اعلان، در ساعت/دقیقه‌ی مشخص‌شده از روز موردنظر */
    private fun triggerTimeMillis(daysFromNow: Long, hour: Int, minute: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, daysFromNow.toInt())
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
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
