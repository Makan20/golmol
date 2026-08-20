package com.example.nargesapp.ui.utils

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
        var jm = 0
        var jd = 0
        for (i in 0..11) {
            if (jDayNo >= jDaysInMonth[i]) {
                jDayNo -= jDaysInMonth[i]
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

    // تبدیل تاریخ جلالی (yyyy/MM/dd) به تعداد روز مطلق، برای مقایسه‌ی راحت دو تاریخ
    private fun jalaliToDayCount(year: Int, month: Int, day: Int): Long {
        val epochBaseYear = 475
        val cycleYears = 2820
        val cycleDays = 1029983L

        var y = year - epochBaseYear
        val cycle = if (y >= 0) y / cycleYears else y / cycleYears - 1
        var yearInCycle = y - cycle * cycleYears

        val a = if (yearInCycle < 2346) yearInCycle else yearInCycle - 2346
        val leapCount = (a * 682) / 2816
        val yearDayNo = 365L * yearInCycle + leapCount + (if (yearInCycle >= 2346) 1029982L else 0L)

        var totalDays = cycle * cycleDays + yearDayNo

        totalDays += if (month <= 7) (month - 1) * 31 else 186 + (month - 8) * 30
        totalDays += (day - 1)

        return totalDays
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

    /** آیا تاریخ داده‌شده (فرمت yyyy/MM/dd) قبل از امروز است؟ */
    fun isPastDate(date: String): Boolean {
        val diff = daysUntil(date) ?: return false
        return diff < 0
    }

    /** آیا سال جلالی داده‌شده کبیسه است (اسفند ۳۰ روزه) */
    private fun isLeapJalaliYear(year: Int): Boolean {
        val remainder = ((year - (if (year > 0) 474 else 473)) % 2820 + 2820) % 2820
        val cycle = (remainder + 474) % 2820
        return ((cycle + 38) * 682) % 2816 < 682
    }

    /** تعداد روزهای یک ماه مشخص از سال جلالی */
    private fun daysInJalaliMonth(year: Int, month: Int): Int {
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
