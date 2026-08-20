package com.example.nargesapp.data.repository

import com.example.nargesapp.data.local.TransactionDao
import com.example.nargesapp.data.model.Transaction
import com.example.nargesapp.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object TransactionRepository {
    private lateinit var dao: TransactionDao
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    fun init(transactionDao: TransactionDao) {
        dao = transactionDao
        scope.launch {
            dao.getAllTransactions().collect { list ->
                _transactions.value = list
            }
        }
    }

    fun addTransaction(transaction: Transaction) {
        scope.launch { dao.insert(transaction) }
    }

    fun deleteTransaction(transaction: Transaction) {
        scope.launch { dao.delete(transaction) }
    }

    fun updateTransaction(transaction: Transaction) {
        scope.launch { dao.update(transaction) }
    }

    fun getTransactionById(id: Int): Transaction? {
        return _transactions.value.find { it.id == id }
    }

    fun getTotalIncome(): Long =
        _transactions.value.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    fun getTotalExpense(): Long =
        _transactions.value.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    fun getBalance(): Long = getTotalIncome() - getTotalExpense()

    fun getIncomeByDate(): Map<String, Long> = _transactions.value
        .filter { it.type == TransactionType.INCOME }
        .groupBy { it.date }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    fun getExpenseByDate(): Map<String, Long> = _transactions.value
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.date }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    fun clearAll() {
        scope.launch { dao.deleteAll() }
    }

    // Used only by BackupManager during restore. Suspend (not scope.launch)
    // so the caller can await completion before restoring the next table —
    // this preserves the order accounts -> transactions -> shopping items,
    // which matters because transactions reference accountId.
    suspend fun restoreAll(list: List<Transaction>) {
        dao.deleteAll()
        if (list.isNotEmpty()) dao.insertAll(list)
    }
}