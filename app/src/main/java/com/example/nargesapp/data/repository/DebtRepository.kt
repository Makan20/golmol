package com.example.nargesapp.data.repository

import android.content.Context
import com.example.nargesapp.data.local.DebtDao
import com.example.nargesapp.data.model.Debt
import com.example.nargesapp.data.model.DebtType
import com.example.nargesapp.notification.DebtReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object DebtRepository {
    private lateinit var dao: DebtDao
    private var appContext: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts

    fun init(debtDao: DebtDao, context: Context? = null) {
        dao = debtDao
        appContext = context?.applicationContext
        scope.launch {
            dao.getAllDebts().collect { list ->
                _debts.value = list
            }
        }
    }

    fun addDebt(debt: Debt) {
        scope.launch {
            val newId = dao.insert(debt)
            appContext?.let { ctx ->
                DebtReminderScheduler.scheduleFor(ctx, debt.copy(id = newId.toInt()))
            }
        }
    }

    /**
     * ثبت یک وام قسطی: تمام اقساط را با مبلغ و سررسید محاسبه‌شده به‌صورت یکجا می‌سازد.
     * مبلغ هر قسط مساوی (گرد به پایین) است، به‌جز قسط آخر که باقی‌مانده‌ی دقیق را می‌گیرد
     * تا مجموع اقساط همیشه دقیقاً برابر مبلغ کل وام باشد.
     */
    fun addLoan(
        personName: String,
        totalAmount: Long,
        type: DebtType,
        installmentCount: Int,
        firstDueDate: String,
        installmentDayOfMonth: Int,
        note: String,
        createdDate: String,
        reminderEnabled: Boolean,
        dueTime: String = ""
    ) {
        if (installmentCount < 2) {
            addDebt(
                Debt(
                    personName = personName,
                    amount = totalAmount,
                    type = type,
                    dueDate = firstDueDate,
                    dueTime = dueTime,
                    note = note,
                    createdDate = createdDate,
                    reminderEnabled = reminderEnabled
                )
            )
            return
        }

        val loanGroupId = java.util.UUID.randomUUID().toString()
        val baseInstallmentAmount = totalAmount / installmentCount
        val lastInstallmentAmount = totalAmount - (baseInstallmentAmount * (installmentCount - 1))

        val installments = (1..installmentCount).map { index ->
            val amount = if (index == installmentCount) lastInstallmentAmount else baseInstallmentAmount
            val dueDate = if (index == 1) {
                firstDueDate
            } else {
                com.example.nargesapp.ui.utils.PersianDateUtils.addMonthsToJalaliDate(
                    firstDueDate,
                    index - 1,
                    installmentDayOfMonth
                ) ?: firstDueDate
            }

            Debt(
                personName = personName,
                amount = amount,
                type = type,
                dueDate = dueDate,
                dueTime = dueTime,
                note = note,
                createdDate = createdDate,
                reminderEnabled = reminderEnabled,
                loanGroupId = loanGroupId,
                installmentNumber = index,
                totalInstallments = installmentCount
            )
        }

        scope.launch {
            val ids = installments.map { dao.insert(it) }
            appContext?.let { ctx ->
                installments.forEachIndexed { i, debt ->
                    DebtReminderScheduler.scheduleFor(ctx, debt.copy(id = ids[i].toInt()))
                }
            }
        }
    }

    fun updateDebt(debt: Debt) {
        scope.launch {
            dao.update(debt)
            appContext?.let { ctx -> DebtReminderScheduler.scheduleFor(ctx, debt) }
        }
    }

    fun deleteDebt(debt: Debt) {
        scope.launch {
            dao.delete(debt)
            appContext?.let { ctx -> DebtReminderScheduler.cancelFor(ctx, debt.id) }
        }
    }

    fun getDebtById(id: Int): Debt? = _debts.value.find { it.id == id }

    /** تمام اقساط یک وام (بر اساس شناسه‌ی گروه)، مرتب‌شده بر اساس شماره‌ی قسط */
    fun getLoanInstallments(loanGroupId: String): List<Debt> = _debts.value
        .filter { it.loanGroupId == loanGroupId }
        .sortedBy { it.installmentNumber ?: 0 }

    /**
     * آیا این قسط قابل تسویه است؟ یعنی آیا تمام اقساط قبل از آن (شماره‌ی کوچک‌تر) تسویه شده‌اند؟
     * برای بدهی/طلب معمولی (بدون loanGroupId) همیشه true است.
     */
    fun canSettleInstallment(debt: Debt): Boolean {
        val groupId = debt.loanGroupId ?: return true
        val currentNumber = debt.installmentNumber ?: return true
        return getLoanInstallments(groupId)
            .filter { (it.installmentNumber ?: 0) < currentNumber }
            .all { it.isSettled }
    }

    fun getOpenReceivables(): Long = _debts.value
        .filter { it.type == DebtType.RECEIVABLE && !it.isSettled }
        .sumOf { it.amount }

    fun getOpenPayables(): Long = _debts.value
        .filter { it.type == DebtType.PAYABLE && !it.isSettled }
        .sumOf { it.amount }

    fun clearAll() {
        scope.launch { dao.deleteAll() }
    }

    suspend fun restoreAll(list: List<Debt>) {
        dao.deleteAll()
        if (list.isNotEmpty()) dao.insertAll(list)
        appContext?.let { ctx ->
            list.forEach { debt -> DebtReminderScheduler.scheduleFor(ctx, debt) }
        }
    }
}
