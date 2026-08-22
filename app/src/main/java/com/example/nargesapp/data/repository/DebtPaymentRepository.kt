package com.example.nargesapp.data.repository

import com.example.nargesapp.data.local.DebtPaymentDao
import com.example.nargesapp.data.model.DebtPayment
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

    fun updatePayment(payment: DebtPayment) {
        scope.launch { dao.update(payment) }
    }

    fun deletePayment(payment: DebtPayment) {
        scope.launch { dao.delete(payment) }
    }

    fun deletePaymentsForDebt(debtId: Int) {
        scope.launch { dao.deleteForDebt(debtId) }
    }

    fun getPaymentsFor(debtId: Int): List<DebtPayment> =
        _payments.value.filter { it.debtId == debtId }.sortedByDescending { it.id }

    fun clearAll() {
        scope.launch { dao.deleteAll() }
    }

    suspend fun restoreAll(list: List<DebtPayment>) {
        dao.deleteAll()
        if (list.isNotEmpty()) dao.insertAll(list)
    }
}
