package com.ordibehesht.finance.dong

import java.util.UUID

/**
 * ماژول «دونگ»: تقسیم هزینه‌ی گروهی بین چند نفر (مثلاً یک سفر یا گردش دسته‌جمعی).
 *
 * این ماژول کاملاً مستقل از بقیه‌ی اپ (تراکنش‌ها، حساب‌ها، بدهی‌ها) است.
 *
 * توضیح مستندسازی (رفع تناقض قبلی): این فایل مدل‌های «ساده»ی در-حافظه
 * (DongGroup/DongParticipant/DongExpense/DongSettlement) را تعریف می‌کند که
 * UI و DongCalculator مستقیماً با آن‌ها کار می‌کنند. این کامنت قبلاً می‌گفت
 * ماژول دونگ «هیچ داده‌ای در Room ذخیره نمی‌کند... موقتی است»، که دیگر درست
 * نیست: از یک نسخه‌ی قبل‌تر، پایداری کامل با Room اضافه شده — جدول‌های واقعی
 * (DongGroupEntity/DongParticipantEntity/...) در DongEntities.kt تعریف و در
 * DongRepository.kt نگه‌داری می‌شوند (به کامنت بالای DongRepository مراجعه
 * کنید)، و DongRepository این Entityها را به مدل‌های همین فایل تبدیل می‌کند.
 * یعنی گروه‌های دونگ با بستن اپ از بین نمی‌روند.
 */

/** یک شرکت‌کننده در گروه دونگ. */
data class DongParticipant(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    /**
     * سهم دستیِ این فرد از هزینه‌ی کل، اگر کاربر آن را تغییر داده باشد.
     * اگر null باشد یعنی سهم به صورت خودکار و مساوی محاسبه می‌شود.
     */
    val manualShare: Long? = null
)

/** یک آیتم هزینه که توسط یکی از شرکت‌کنندگان پرداخت شده است. */
data class DongExpense(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Long,
    val payerId: String,
    /** ایندکس رنگ (از همان پالت رنگ‌های افزودن کارت جدید) برای نقطه‌ی کنار این هزینه در لیست. */
    val colorIndex: Int = (0..9).random()
)

/**
 * یک رکورد واقعی تسویه‌ی حساب بین دو نفر — مثلاً «گندم ۱۰۰,۰۰۰ تومان به سارا پرداخت کرد».
 * فقط از بدهکار به طلبکار مجاز است (repository این قاعده را هنگام ثبت اجرا می‌کند).
 */
data class DongSettlementRecord(
    val id: String = UUID.randomUUID().toString(),
    val fromId: String,
    val toId: String,
    val amount: Long,
    /** تاریخ شمسی به‌صورت yyyy/MM/dd، دقیقاً هم‌فرمت بقیه‌ی اپ (PersianDateUtils). */
    val date: String,
    /** ساعت به‌صورت HH:mm. */
    val time: String
)

/** یک گروه دونگ (مثلاً «سفر شمال»). */
data class DongGroup(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val participants: List<DongParticipant> = emptyList(),
    val expenses: List<DongExpense> = emptyList(),
    val settlements: List<DongSettlementRecord> = emptyList(),
    /** آرشیوشده یعنی از لیست فعال کنار رفته، ولی داده‌هایش دست‌نخورده و قابل بازگردانی است. */
    val isArchived: Boolean = false,
    /** تاریخ شمسی ساخت گروه (yyyy/MM/dd)، به‌صورت خودکار در لحظه‌ی ایجاد ثبت می‌شود. */
    val createdDate: String = ""
)

/** خلاصه‌ی وضعیت مالی یک نفر در گروه. */
data class DongParticipantStatus(
    val participant: DongParticipant,
    /** جمع هزینه‌هایی که این فرد پرداخت‌کننده‌ی آن‌ها بوده (بدون احتساب تسویه‌ها). */
    val paid: Long,
    val share: Long,
    /**
     * مانده‌ی نهایی این فرد پس از احتساب هزینه‌ها و همه‌ی تسویه‌های ثبت‌شده.
     * مثبت = طلبکار، منفی = بدهکار، صفر = تسویه.
     */
    val balance: Long
)

object DongCalculator {

    /** جمع کل هزینه‌های ثبت‌شده در گروه. */
    fun totalExpense(group: DongGroup): Long =
        group.expenses.sumOf { it.amount }

    /**
     * سهم هر نفر را محاسبه می‌کند.
     * پیش‌فرض: تقسیم مساوی جمع کل بین همه‌ی نفرات.
     * اگر برخی افراد سهم دستی داشته باشند، ابتدا مجموع سهم‌های دستی از کل کم می‌شود
     * و باقیمانده به صورت مساوی بین بقیه (بدون سهم دستی) تقسیم می‌شود.
     * در صورتی که مجموع سهم‌های دستی از کل هزینه بیشتر شود یا کسی برای تقسیم باقیمانده
     * وجود نداشته باشد، افراد بدون سهم دستی صفر می‌گیرند (حالت نامعتبر ورودی کاربر).
     */
    fun calculateShares(group: DongGroup): Map<String, Long> {
        val participants = group.participants
        if (participants.isEmpty()) return emptyMap()

        val total = totalExpense(group)
        val manualParticipants = participants.filter { it.manualShare != null }
        val autoParticipants = participants.filter { it.manualShare == null }

        val manualSum = manualParticipants.sumOf { it.manualShare ?: 0L }
        val remaining = (total - manualSum).coerceAtLeast(0L)

        val result = mutableMapOf<String, Long>()
        manualParticipants.forEach { result[it.id] = it.manualShare ?: 0L }

        if (autoParticipants.isNotEmpty()) {
            val baseShare = remaining / autoParticipants.size
            val remainder = remaining % autoParticipants.size
            // ریال باقیمانده از تقسیم صحیح را برای دقیق بودن جمع کل، به نفرات اول اختصاص می‌دهیم
            autoParticipants.forEachIndexed { index, p ->
                result[p.id] = baseShare + if (index < remainder) 1L else 0L
            }
        }

        return result
    }

    /** مبلغ پرداختی هر نفر بابت هزینه‌های گروه (بدون احتساب تسویه‌ها). */
    fun calculatePaid(group: DongGroup): Map<String, Long> {
        val result = group.participants.associate { it.id to 0L }.toMutableMap()
        group.expenses.forEach { expense ->
            result[expense.payerId] = (result[expense.payerId] ?: 0L) + expense.amount
        }
        return result
    }

    /**
     * وضعیت کامل (پرداخت‌شده، سهم، مانده) برای هر شرکت‌کننده.
     * مانده ابتدا از روی هزینه‌ها محاسبه می‌شود (پرداختی منهای سهم)، سپس هر تسویه‌ی
     * ثبت‌شده روی آن اعمال می‌شود: مانده‌ی پرداخت‌کننده به‌اندازه‌ی مبلغ کم و مانده‌ی
     * گیرنده به همان اندازه زیاد می‌شود — دقیقاً مثل واریز نقدی بین دو نفر.
     */
    fun calculateStatuses(group: DongGroup): List<DongParticipantStatus> {
        val paidMap = calculatePaid(group)
        val shareMap = calculateShares(group)
        val balanceMap = group.participants.associate { p ->
            val paid = paidMap[p.id] ?: 0L
            val share = shareMap[p.id] ?: 0L
            p.id to (paid - share)
        }.toMutableMap()

        group.settlements.forEach { s ->
            balanceMap[s.fromId] = (balanceMap[s.fromId] ?: 0L) + s.amount
            balanceMap[s.toId] = (balanceMap[s.toId] ?: 0L) - s.amount
        }

        return group.participants.map { p ->
            DongParticipantStatus(
                participant = p,
                paid = paidMap[p.id] ?: 0L,
                share = shareMap[p.id] ?: 0L,
                balance = balanceMap[p.id] ?: 0L
            )
        }
    }

    /** آیا همه‌ی نفرات گروه تسویه هستند؟ */
    fun isFullySettled(group: DongGroup): Boolean =
        calculateStatuses(group).all { it.balance == 0L }
}
