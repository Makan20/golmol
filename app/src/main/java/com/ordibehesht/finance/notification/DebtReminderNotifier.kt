package com.ordibehesht.finance.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.ordibehesht.finance.MainActivity
import com.ordibehesht.finance.R
import com.ordibehesht.finance.data.local.AppDatabase
import com.ordibehesht.finance.data.local.DebtNotificationDao
import com.ordibehesht.finance.data.model.Debt
import com.ordibehesht.finance.data.model.DebtNotification
import com.ordibehesht.finance.data.model.DebtType
import com.ordibehesht.finance.ui.utils.PersianDateUtils

private const val CHANNEL_ID = "debt_reminders"

/**
 * منطق مشترک ساخت متن و نمایش نوتیفیکیشن یادآوری بدهی/طلب — هم از [DebtReminderAlarmReceiver]
 * (مسیر اصلی، دقیق، بر پایه‌ی AlarmManager.setAlarmClock) و هم از [NotificationFallbackWorker]
 * (مسیر پشتیبان دوره‌ای، بر پایه‌ی WorkManager، برای گوشی‌هایی که به هر دلیلی — مثلاً
 * محدودیت‌های سفت‌وسخت OEM مثل MIUI — حتی setAlarmClock را هم گاهی از کار می‌اندازند)
 * استفاده می‌شود، تا هر دو مسیر دقیقاً یک رفتار و یک تاریخچه داشته باشند.
 */
object DebtReminderNotifier {

    /**
     * اگر شرایط نمایش برقرار باشد (بدهی هنوز باز و یادآوری‌اش فعال است، و امروز برای همین
     * بدهی هنوز یادآوری‌ای ثبت نشده)، نوتیفیکیشن را نشان می‌دهد و یک رکورد در تاریخچه‌ی
     * داخلی اپ ثبت می‌کند. برمی‌گرداند که آیا واقعاً نمایش داده شد.
     */
    suspend fun notifyIfNeeded(context: Context, debt: Debt, isDueToday: Boolean): Boolean {
        if (debt.isSettled || !debt.reminderEnabled) return false

        // باگ رفع‌شده (race condition نادر): این چک قبلاً فقط داخل NotificationFallbackWorker
        // (مسیر پشتیبان دوره‌ای) انجام می‌شد، نه اینجا. یعنی مسیر اصلی
        // (DebtReminderAlarmReceiver، بر پایه‌ی AlarmManager) هیچ محافظتی در برابر نمایش
        // دوباره‌ی همان یادآوری نداشت. اگر AlarmManager و این Worker تقریباً هم‌زمان اجرا
        // می‌شدند (هر دو قبل از ثبت رکورد تاریخچه توسط دیگری)، هر دو می‌توانستند یک یادآوری
        // را دوبار نشان دهند. حالا این چک اینجا، در نقطه‌ی مشترک هر دو مسیر، انجام می‌شود.
        val today = PersianDateUtils.getCurrentPersianDate()
        val notificationDao = AppDatabase.getInstance(context).debtNotificationDao()
        val alreadyNotified = notificationDao.countForDebtAndDate(debt.id, today) > 0
        if (alreadyNotified) return false

        val isReceivable = debt.type == DebtType.RECEIVABLE
        val title = if (isReceivable) "یادآوری طلب" else "یادآوری بدهی"
        val amountText = "%,d".format(debt.remainingAmount).replace(",", "٬")

        val body = if (isDueToday) {
            if (isReceivable)
                "امروز سررسید طلب شما از ${debt.personName} است — $amountText تومان"
            else
                "امروز سررسید بدهی شما به ${debt.personName} است — $amountText تومان"
        } else {
            if (isReceivable)
                "\u200F۳ روز تا سررسید طلب شما از ${debt.personName} مانده — $amountText تومان"
            else
                "\u200F۳ روز تا سررسید بدهی شما به ${debt.personName} مانده — $amountText تومان"
        }

        showNotification(context, debt.id, isDueToday, title, body)
        recordNotificationHistory(notificationDao, debt.id, today, title, body)
        return true
    }

    private suspend fun recordNotificationHistory(
        notificationDao: DebtNotificationDao,
        debtId: Int,
        today: String,
        title: String,
        body: String
    ) {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val time = "%02d:%02d".format(hour, minute)

        val entry = DebtNotification(
            debtId = debtId,
            title = title,
            body = body,
            date = today,
            time = time
        )
        notificationDao.insert(entry)
    }

    private fun showNotification(context: Context, debtId: Int, isDueToday: Boolean, title: String, body: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "یادآوری طلب و بدهی",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "یادآوری نزدیک شدن به تاریخ سررسید طلب و بدهی"
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "debt_detail/$debtId")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            debtId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(debtId * 10 + (if (isDueToday) 1 else 0), notification)
    }
}
