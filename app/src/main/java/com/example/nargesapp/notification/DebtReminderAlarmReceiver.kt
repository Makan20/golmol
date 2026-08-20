package com.example.nargesapp.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.nargesapp.MainActivity
import com.example.nargesapp.R
import com.example.nargesapp.data.local.AppDatabase
import com.example.nargesapp.data.model.DebtNotification
import com.example.nargesapp.data.model.DebtType
import com.example.nargesapp.ui.utils.PersianDateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "debt_reminders"

// یک نمونه از هر آیتم طلب/بدهی، دو یادآوری متفاوت (۳ روز قبل و روز سررسید) دارد؛
// این کلید مشخص می‌کند این زنگ مربوط به کدام مرحله است
const val KEY_DEBT_ID = "debt_id"
const val KEY_IS_DUE_TODAY = "is_due_today"

/**
 * دریافت‌کننده‌ی زنگ هشدار دقیق (AlarmManager). برخلاف WorkManager معمولی که تضمینی برای
 * اجرای دقیق در زمان مشخص ندارد (به‌خصوص روی گوشی‌هایی با مدیریت پس‌زمینه‌ی سخت‌گیرانه مثل MIUI)،
 * AlarmManager.setExactAndAllowWhileIdle حتی در حالت Doze هم این زنگ را دقیقاً در زمان تعیین‌شده فعال می‌کند.
 */
class DebtReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val debtId = intent.getIntExtra(KEY_DEBT_ID, -1)
        if (debtId == -1) return
        val isDueToday = intent.getBooleanExtra(KEY_IS_DUE_TODAY, false)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleReminder(context, debtId, isDueToday)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReminder(context: Context, debtId: Int, isDueToday: Boolean) {
        val debtDao = AppDatabase.getInstance(context).debtDao()
        val debt = debtDao.getById(debtId) ?: return

        if (debt.isSettled || !debt.reminderEnabled) return

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

        showNotification(context, debtId, isDueToday, title, body)
        recordNotificationHistory(context, debtId, title, body)
    }

    private suspend fun recordNotificationHistory(context: Context, debtId: Int, title: String, body: String) {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val time = "%02d:%02d".format(hour, minute)

        val notificationDao = AppDatabase.getInstance(context).debtNotificationDao()
        val entry = DebtNotification(
            debtId = debtId,
            title = title,
            body = body,
            date = PersianDateUtils.getCurrentPersianDate(),
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
