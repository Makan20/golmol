package com.ordibehesht.finance.ui.utils

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object PersianDateUtils {

    fun getCurrentTime(): String {
        val cal = Calendar.getInstance()
        return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    val persianMonthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    private val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

    private fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): Triple<Int, Int, Int> {
        var gy = gYear - 1600
        var gm = gMonth - 1
        var gd = gDay - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79
        var jNp = jDayNo / 12053
        jDayNo %= 12053
        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }
        // نکته‌ی مهم (باگ رفع‌شده): jDaysInMonth یک آرایه‌ی ثابت است که همیشه اسفند را ۲۹
        // روزه فرض می‌کند. در آخرین روز اسفندِ سال‌های کبیسه‌ی جلالی (jDayNo == 365، یعنی
        // روز ۳۶۶ام سال)، این حلقه به هیچ ماهی نمی‌رسید چون مجموع آرایه فقط ۳۶۵ روز است —
        // نتیجه jm=0, jd=0 نامعتبر بود (مثلاً دقیقاً ۲۰ اسفند/مارس هر ۴ سال، از جمله
        // ۱۴۰۳/۱۲/۳۰ = ۲۰ مارس ۲۰۲۵). همان‌طور که isLeapJalaliYear/daysInJalaliMonth پایین‌تر
        // در همین فایل درست مدیریت می‌کنند، اینجا هم باید طول اسفندِ همان jy را کبیسه لحاظ کرد.
        val jDaysInMonthForYear = if (isLeapJalaliYear(jy)) {
            jDaysInMonth.copyOf().also { it[11] = 30 }
        } else {
            jDaysInMonth
        }
        var jm = 0
        var jd = 0
        for (i in 0..11) {
            if (jDayNo >= jDaysInMonthForYear[i]) {
                jDayNo -= jDaysInMonthForYear[i]
            } else {
                jm = i + 1
                jd = jDayNo + 1
                break
            }
        }
        return Triple(jy, jm, jd)
    }

    fun getCurrentPersianDate(): String {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val gy = calendar.get(Calendar.YEAR)
        val gm = calendar.get(Calendar.MONTH) + 1
        val gd = calendar.get(Calendar.DAY_OF_MONTH)
        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        return String.format("%d/%02d/%02d", jy, jm, jd)
    }

    fun getCurrentPersianDatePersianDigits(): String {
        return toPersianDigits(getCurrentPersianDate())
    }

    fun getWeekDays(): List<String> {
        return listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
    }

    fun getCurrentWeekDates(): List<String> {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 7=Saturday
        // Map to Persian: Saturday=0, Sunday=1, ..., Friday=6
        val persianDayOfWeek = when (currentDayOfWeek) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }

        val result = mutableListOf<String>()
        for (i in 0..6) {
            val diff = i - persianDayOfWeek
            val tempCal = Calendar.getInstance(TimeZone.getDefault())
            tempCal.add(Calendar.DAY_OF_YEAR, diff)
            val gy = tempCal.get(Calendar.YEAR)
            val gm = tempCal.get(Calendar.MONTH) + 1
            val gd = tempCal.get(Calendar.DAY_OF_MONTH)
            val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
            result.add(String.format("%d/%02d/%02d", jy, jm, jd))
        }
        return result
    }

    fun getYesterdayPersianDate(): String {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val gy = calendar.get(Calendar.YEAR)
        val gm = calendar.get(Calendar.MONTH) + 1
        val gd = calendar.get(Calendar.DAY_OF_MONTH)
        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        return String.format("%d/%02d/%02d", jy, jm, jd)
    }

    fun toPersianDigits(input: String): String {
        val persian = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return input.map { ch -> if (ch.isDigit()) persian[ch.digitToInt()] else ch }.joinToString("")
    }

    // پیاده‌سازی یکتای parse مبلغ در کل اپ (قبلاً سه نسخه‌ی جدا از همین منطق در
    // AddDebtScreen/DebtDetailScreen/AddTransactionScreen وجود داشت). ارقام فارسی و
    // عربی هر دو پشتیبانی می‌شوند و همیشه toLongOrNull با پیش‌فرض ۰ استفاده می‌شود،
    // نه toLong() که روی ورودی نامعتبر/خیلی بزرگ کرش می‌کند
    fun parsePersianAmount(input: String): Long {
        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
        val englishDigits = input.map { char ->
            when {
                char in persianDigits -> ('0'.code + persianDigits.indexOf(char)).toChar()
                char in arabicDigits -> ('0'.code + arabicDigits.indexOf(char)).toChar()
                else -> char
            }
        }.joinToString("")
        return englishDigits.filter { it.isDigit() }.toLongOrNull() ?: 0L
    }

    /**
     * تبدیل تاریخ جلالی (yyyy/MM/dd) به تعداد روز مطلق، برای مقایسه‌ی راحت دو تاریخ.
     * بر پایه‌ی farvardinFirstInGregorian و daysInJalaliMonth (که خودشان از
     * gregorianToJalaliِ تست‌شده مشتق شده‌اند) محاسبه می‌شود — به‌جای فرمول تقریبی
     * قبلی که هم در تشخیص سال کبیسه (حول ۱۴۰۳/۱۴۰۴) و هم در آفست ماه‌های بعد از
     * مهر (چون طول ماه مهر را در جمع کل به‌اشتباه نادیده می‌گرفت) خطا داشت.
     */
    // internal (نه private) تا CalendarUtils/DebtCalendarCard هم بتوانند مستقیم از همین
    // پیاده‌سازی برای محاسبه‌ی روز هفته‌ی هر تاریخ دلخواه استفاده کنند، بدون تکرار منطق تبدیل
    internal fun jalaliToDayCount(year: Int, month: Int, day: Int): Long {
        val baseOrdinal = farvardinFirstInGregorian(year).timeInMillis / 86_400_000L
        var offsetDays = 0L
        for (m in 1 until month) {
            offsetDays += daysInJalaliMonth(year, m)
        }
        offsetDays += (day - 1)
        return baseOrdinal + offsetDays
    }

    /**
     * تعداد روزهای باقی‌مانده تا تاریخ سررسید (فرمت yyyy/MM/dd، ارقام انگلیسی).
     * عدد منفی یعنی تاریخ گذشته است. ورودی نامعتبر null برمی‌گرداند.
     */
    fun daysUntil(targetDate: String): Long? {
        val parts = targetDate.trim().split("/")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null

        val todayParts = getCurrentPersianDate().split("/")
        val todayYear = todayParts[0].toInt()
        val todayMonth = todayParts[1].toInt()
        val todayDay = todayParts[2].toInt()

        val targetCount = jalaliToDayCount(year, month, day)
        val todayCount = jalaliToDayCount(todayYear, todayMonth, todayDay)

        return targetCount - todayCount
    }

    /**
     * ایندکس روز هفته‌ی سال+ماه+روز جلالی داده‌شده، هم‌راستا با ترتیب [getWeekDays]
     * (۰=شنبه ... ۶=جمعه). با کالیبره‌کردن نسبت به «امروز» (که خودِ Calendar سیستم روز هفته‌ی
     * واقعی‌اش را می‌داند) محاسبه می‌شود، بدون نیاز به یک تبدیل کامل جلالی‌به‌میلادی جداگانه.
     */
    fun weekdayIndexOf(year: Int, month: Int, day: Int): Int {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val todayGregorianWeekday = calendar.get(Calendar.DAY_OF_WEEK)
        val todayPersianWeekday = when (todayGregorianWeekday) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
        val todayParts = getCurrentPersianDate().split("/")
        val todayCount = jalaliToDayCount(todayParts[0].toInt(), todayParts[1].toInt(), todayParts[2].toInt())
        val targetCount = jalaliToDayCount(year, month, day)
        val diff = (targetCount - todayCount).toInt()
        return ((todayPersianWeekday + diff) % 7 + 7) % 7
    }

    /** آیا تاریخ داده‌شده (فرمت yyyy/MM/dd) قبل از امروز است؟ */
    fun isPastDate(date: String): Boolean {
        val diff = daysUntil(date) ?: return false
        return diff < 0
    }

    /**
     * تبدیل اول فروردین یک سال جلالی به تاریخ میلادی، با استفاده از gregorianToJalali
     * (که خودش تست و تأیید شده) برای اسکن ۱ تا ۲۲ مارس همان بازه‌ی میلادی.
     * نوروز همیشه بین ۱۹ تا ۲۲ مارس میلادی می‌افتد.
     */
    private fun farvardinFirstInGregorian(jalaliYear: Int): Calendar {
        for (gYearCandidate in intArrayOf(jalaliYear + 621, jalaliYear + 622)) {
            for (day in 19..22) {
                val cal = Calendar.getInstance(TimeZone.getDefault())
                cal.clear()
                cal.set(gYearCandidate, Calendar.MARCH, day)
                val (jy, jm, jd) = gregorianToJalali(gYearCandidate, 3, day)
                if (jy == jalaliYear && jm == 1 && jd == 1) return cal
            }
        }
        // نباید هرگز به اینجا برسد؛ فقط برای اطمینان از عدم کرش
        val fallback = Calendar.getInstance(TimeZone.getDefault())
        fallback.clear()
        fallback.set(jalaliYear + 621, Calendar.MARCH, 21)
        return fallback
    }

    /**
     * آیا سال جلالی داده‌شده کبیسه است (اسفند ۳۰ روزه)؟
     * از الگوریتم چرخه‌ی ۳۳-ساله استفاده می‌کند (که در برابر الگوریتم دقیق jalaali-js
     * روی بازه‌ی ۱۲۰۶ تا ۱۵۰۰ صفر خطا داشت) — به‌جای فرمول قبلی (چرخه‌ی ۲۸۲۰-ساله) که
     * دقیقاً حول سال‌های ۱۴۰۳/۱۴۰۴ نتیجه‌ی برعکس می‌داد (۱۴۰۳ را عادی و ۱۴۰۴ را کبیسه
     * حساب می‌کرد، در حالی که طبق تقویم رسمی برعکس است).
     */
    private fun isLeapJalaliYear(year: Int): Boolean {
        val cycle = ((year % 33) + 33) % 33
        return cycle == 1 || cycle == 5 || cycle == 9 || cycle == 13 ||
            cycle == 17 || cycle == 22 || cycle == 26 || cycle == 30
    }

    /** تعداد روزهای یک ماه مشخص از سال جلالی */
    fun daysInJalaliMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            else -> if (isLeapJalaliYear(year)) 30 else 29
        }
    }

    /**
     * اضافه کردن n ماه به یک تاریخ جلالی، با نگه‌داشتن همان روز از ماه.
     * اگر ماه مقصد آن روز را نداشته باشد (مثلاً ۳۱ در مهر)، به آخرین روز موجود آن ماه سقوط می‌کند.
     */
    fun addMonthsToJalaliDate(date: String, monthsToAdd: Int, targetDay: Int? = null): String? {
        val parts = date.trim().split("/")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = targetDay ?: (parts[2].toIntOrNull() ?: return null)

        val totalMonths = (month - 1) + monthsToAdd
        val newYear = year + totalMonths.floorDiv(12)
        val newMonth = totalMonths.mod(12) + 1

        val maxDay = daysInJalaliMonth(newYear, newMonth)
        val newDay = day.coerceAtMost(maxDay)

        return "%d/%02d/%02d".format(newYear, newMonth, newDay)
    }

    /** تاریخ جلالی امروز به‌علاوه‌ی n روز (می‌تواند منفی هم باشد) */
    fun addDaysToToday(daysOffset: Int): String {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.add(Calendar.DAY_OF_YEAR, daysOffset)
        val gy = calendar.get(Calendar.YEAR)
        val gm = calendar.get(Calendar.MONTH) + 1
        val gd = calendar.get(Calendar.DAY_OF_MONTH)
        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        return String.format("%d/%02d/%02d", jy, jm, jd)
    }

    /** هفت روز هفته (شنبه تا جمعه)، برای هفته‌ای که weekOffset هفته نسبت به امروز جابه‌جا شده است (۰=این‌هفته، -۱=هفته‌ی قبل) */
    fun getWeekDates(weekOffset: Int): List<String> {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val persianDayOfWeek = when (currentDayOfWeek) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }

        val result = mutableListOf<String>()
        for (i in 0..6) {
            val diff = i - persianDayOfWeek + (weekOffset * 7)
            val tempCal = Calendar.getInstance(TimeZone.getDefault())
            tempCal.add(Calendar.DAY_OF_YEAR, diff)
            val gy = tempCal.get(Calendar.YEAR)
            val gm = tempCal.get(Calendar.MONTH) + 1
            val gd = tempCal.get(Calendar.DAY_OF_MONTH)
            val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
            result.add(String.format("%d/%02d/%02d", jy, jm, jd))
        }
        return result
    }

    data class MonthInfo(val year: Int, val month: Int, val dates: List<String>)

    /** اطلاعات ماهی که monthOffset ماه نسبت به ماه جاری جابه‌جا شده (۰=این‌ماه، -۱=ماه قبل)، شامل تمام تاریخ‌های آن ماه */
    fun getMonthInfo(monthOffset: Int): MonthInfo {
        val today = getCurrentPersianDate().split("/")
        val currentYear = today[0].toInt()
        val currentMonth = today[1].toInt()

        val totalMonths = (currentMonth - 1) + monthOffset
        val targetYear = currentYear + totalMonths.floorDiv(12)
        val targetMonth = totalMonths.mod(12) + 1

        val dayCount = daysInJalaliMonth(targetYear, targetMonth)
        val dates = (1..dayCount).map { day -> "%d/%02d/%02d".format(targetYear, targetMonth, day) }

        return MonthInfo(targetYear, targetMonth, dates)
    }
}
