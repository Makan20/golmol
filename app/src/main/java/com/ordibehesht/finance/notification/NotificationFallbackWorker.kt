package com.ordibehesht.finance.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.ordibehesht.finance.data.local.AppDatabase
import com.ordibehesht.finance.ui.utils.PersianDateUtils
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * لایه‌ی پشتیبان (نه جایگزین) برای [DebtReminderAlarmReceiver]. مسیر اصلی
 * (AlarmManager.setAlarmClock) طبق مستندات اندروید باید همیشه دقیقاً در زمان تعیین‌شده فعال
 * شود، اما در عمل روی برخی گوشی‌ها (مشاهده‌شده مشخصاً روی MIUI، حتی با تمام مجوزهای لازم —
 * باتری بدون محدودیت، Autostart، و مجوز Alarms & reminders — همگی فعال) این تضمین سیستمی
 * زیر پا گذاشته می‌شود و زنگ اصلاً فایر نمی‌شود، بدون هیچ خطا یا نشانه‌ای.
 *
 * این Worker هر ۱۵ دقیقه یک‌بار (کمترین بازه‌ی مجاز WorkManager) اجرا می‌شود و برای هر بدهی/طلب
 * باز با یادآوری فعال، چک می‌کند که آیا «امروز» یکی از دو روز یادآوری آن (۳ روز قبل از سررسید،
 * یا خودِ روز سررسید) هست یا نه و ساعتش گذشته یا نه؛ اگر بود، تلاش می‌کند یادآوری را از طریق
 * [DebtReminderNotifier.notifyIfNeeded] نمایش دهد — که خودش (نه این فایل) چک می‌کند که آیا
 * امروز برای همین بدهی قبلاً نوتیفیکیشنی ثبت شده یا نه (یعنی AlarmManager آن را از دست داده یا
 * نه) و در صورت تکراری بودن، از نمایش دوباره صرف‌نظر می‌کند.
 *
 * چون این Worker خودش هم مشمول همان محدودیت‌های پس‌زمینه‌ی OEM است، فوری/دقیق نیست — ولی
 * چون AlarmManager معمولاً کار می‌کند (این فقط برای حالت‌های نادر شکست آن است) و بازه‌ی ۱۵
 * دقیقه‌ای به‌اندازه‌ی کافی کوتاه است، برای این هدف کافی است.
 */
class NotificationFallbackWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val debtDao = database.debtDao()

            val allDebts = debtDao.getAllDebts().first()
            val candidates = allDebts.filter { debt ->
                !debt.isSettled && debt.reminderEnabled && debt.dueDate.isNotBlank()
            }

            for (debt in candidates) {
                val daysUntilDue = PersianDateUtils.daysUntil(debt.dueDate) ?: continue
                if (daysUntilDue < 0) continue

                // همان دو روز یادآوری منطق اصلی (DebtReminderScheduler): خود روز سررسید،
                // و در صورتی که سررسید بیش از ۳ روز دیگر بود، ۳ روز مانده به آن
                val isDueToday = daysUntilDue == 0L
                val isThreeDaysBefore = daysUntilDue == 3L

                if (!isDueToday && !isThreeDaysBefore) continue

                // نکته‌ی حیاتی: «امروز روز یادآوری است» به‌تنهایی کافی نیست — باید ساعتِ
                // یادآوری (dueTime، یا برای حالت «۳ روز قبل» همیشه ساعت ۱۰ صبح، طبق
                // DebtReminderScheduler) هم واقعاً گذشته باشد. بدون این چک، اگر ساعت الان
                // (مثلاً ۰۷:۰۳) زودتر از ساعت سررسید (مثلاً ۱۱:۰۲) باشد، این Worker به‌اشتباه
                // یادآوری را زودهنگام نشان می‌دهد، در حالی که AlarmManager بعداً هم آن را در
                // زمان درستش (۱۱:۰۲) دوباره نشان خواهد داد — یعنی یک یادآوری، دو بار
                val (reminderHour, reminderMinute) = if (isDueToday) {
                    DebtReminderScheduler.parseDueTime(debt.dueTime)
                } else {
                    10 to 0
                }
                if (!hasTimePassedToday(reminderHour, reminderMinute)) continue

                // نکته (رفع code-smell + باگ #۴ race condition): چک «آیا امروز برای همین بدهی
                // قبلاً یادآوری ثبت شده» قبلاً اینجا (فقط برای مسیر Worker) انجام می‌شد. حالا
                // این چک به داخل خودِ DebtReminderNotifier.notifyIfNeeded منتقل شده تا هم مسیر
                // اصلی (AlarmManager) و هم این مسیر پشتیبان به‌طور یکسان از آن بهره‌مند باشند —
                // به کامنت بالای notifyIfNeeded مراجعه کنید.
                DebtReminderNotifier.notifyIfNeeded(applicationContext, debt, isDueToday)
            }

            Result.success()
        } catch (e: Exception) {
            // اجرای بعدی (۱۵ دقیقه دیگر) خودش دوباره تلاش می‌کند، پس شکست این یک بار مشکلی
            // در تداوم یادآوری‌ها ایجاد نمی‌کند
            Result.failure()
        }
    }

    /** آیا ساعت/دقیقه‌ی داده‌شده از لحظه‌ی الان (همین امروز) گذشته است؟ */
    private fun hasTimePassedToday(hour: Int, minute: Int): Boolean {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return now.timeInMillis >= target.timeInMillis
    }
}

object NotificationFallbackScheduler {
    private const val WORK_NAME = "debt_reminder_fallback_check"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // ۱۵ دقیقه کمترین بازه‌ی مجاز خودِ WorkManager است (کمتر از این را سیستم نمی‌پذیرد).
        // این عدد فقط سقف فاصله‌ی *بین چک‌ها* را کوچک می‌کند تا اگر AlarmManager یادآوری‌ای را
        // از دست داد، این لایه‌ی پشتیبان با کمترین تأخیر ممکن جایگزینش کند — تضمین اجرای دقیقاً
        // سرِ زمان نیست (چون WorkManager هم مثل AlarmManager تابع محدودیت‌های پس‌زمینه‌ی گوشی/OEM
        // است)، فقط فاصله‌ی احتمالی تأخیر را به‌جای چند ساعت به چند دقیقه کاهش می‌دهد
        val request = PeriodicWorkRequestBuilder<NotificationFallbackWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        // UPDATE (نه KEEP): تنظیمات این کار دوره‌ای (ازجمله بازه‌ی زمانی) را در هر بار اجرای
        // اپ به‌روزرسانی می‌کند، بدون این‌که چرخه‌ی زمانی از نو شروع شود — یعنی اگر بازه در
        // نسخه‌ای بعدی دوباره تغییر کند، لازم نیست کاربر اپ را پاک‌نصب کند
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
