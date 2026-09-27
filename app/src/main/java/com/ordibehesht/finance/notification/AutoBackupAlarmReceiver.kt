package com.ordibehesht.finance.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ordibehesht.finance.data.repository.BackupManager
import com.ordibehesht.finance.ui.utils.AutoBackupPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * هر بار که این زنگ فایر می‌شود: یک فایل پشتیبان جدید می‌سازد (با پیشوند auto_ تا از
 * پشتیبان‌های دستی کاربر متمایز باشد)، فایل‌های auto_ قدیمی‌تر از ۵ تای اخیر را پاک
 * می‌کند، و دوباره خودش را برای هفته‌ی بعد زمان‌بندی می‌کند (چون AlarmManager.setAlarmClock
 * یک‌بارمصرف است، نه تکرارشونده).
 */
class AutoBackupAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // اگر کاربر از تنظیمات این قابلیت را خاموش کرده باشد ولی به هر دلیلی (مثلاً یک
        // زنگِ زمان‌بندی‌شده‌ی قدیمی که لغو نشده) این گیرنده فایر شود، کاری انجام نمی‌دهیم
        if (!AutoBackupPreferences.isEnabled(context)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)
                val fileName = "auto_narges_backup_${sdf.format(Date())}.json"
                BackupManager.saveBackupToFileFromDb(context, fileName)
                BackupManager.pruneOldAutoBackups(context, keepCount = 5)
                AutoBackupPreferences.setLastBackupAt(context, System.currentTimeMillis())
            } catch (e: Exception) {
                // خطای پشتیبان‌گیری پس‌زمینه را بی‌صدا نادیده می‌گیریم — کاربر معمولاً برای
                // آن آنلاین نیست که پیام خطا ببیند؛ هفته‌ی بعد دوباره تلاش می‌شود
            } finally {
                // صرف‌نظر از موفقیت/خطا، حتماً برای هفته‌ی بعد دوباره زمان‌بندی می‌کنیم
                AutoBackupScheduler.scheduleNext(context.applicationContext)
                pendingResult.finish()
            }
        }
    }
}
