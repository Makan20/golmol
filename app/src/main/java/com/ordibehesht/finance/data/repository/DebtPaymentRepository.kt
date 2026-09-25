package com.ordibehesht.finance.data.repository

import com.ordibehesht.finance.data.local.DebtPaymentDao
import com.ordibehesht.finance.data.model.DebtPayment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object DebtPaymentRepository {

    private lateinit var dao: DebtPaymentDao
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _payments = MutableStateFlow<List<DebtPayment>>(emptyList())
    val payments: StateFlow<List<DebtPayment>> = _payments

    fun init(debtPaymentDao: DebtPaymentDao) {
        dao = debtPaymentDao
        scope.launch {
            dao.getAllPayments().collect { list ->
                _payments.value = list
            }
        }
    }

    fun addPayment(payment: DebtPayment) {
        scope.launch { dao.insert(payment) }
    }

    /** نسخه‌ی suspend که id واقعی رکورد درج‌شده را برمی‌گرداند —
     * برای پیوند دقیق و یکتا با تراکنش خودکار ساخته‌شده (Transaction.debtPaymentId)،
     * به‌جای match کردن بر اساس amount+date که با دو پرداخت هم‌مبلغ/هم‌تاریخ خطا می‌دهد */
    suspend fun addPaymentAndGetId(payment: DebtPayment): Int =
        dao.insert(payment).toInt()

    fun updatePayment(payment: DebtPayment) {
        scope.launch { dao.update(payment) }
    }

    fun deletePayment(payment: DebtPayment) {
        scope.launch { dao.delete(payment) }
    }

    fun deletePaymentsForDebt(debtId: Int) {
        scope.launch { dao.deleteForDebt(debtId) }
    }

    /** هنگام حذف یک حساب/کارت، ارجاع پرداخت‌های بدهی به آن حساب پاک می‌شود
     * (هماهنگ با TransactionRepository.clearAccountReferences) */
    fun clearAccountReferences(accountId: Int) {
        scope.launch { dao.clearAccountReferences(accountId) }
    }

    fun getPaymentsFor(debtId: Int): List<DebtPayment> =
        _payments.value.filter { it.debtId == debtId }.sortedByDescending { it.id }

    // باگ رفع‌شده: مشابه TransactionRepository.clearAll — قبلاً suspend نبود و caller
    // (AppResetManager) نمی‌توانست واقعاً منتظر تکمیل بماند.
    suspend fun clearAll() {
        dao.deleteAll()
    }

    suspend fun restoreAll(list: List<DebtPayment>) {
        dao.deleteAll()
        if (list.isNotEmpty()) dao.insertAll(list)
    }
}
