package com.example.nargesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nargesapp.data.model.Transaction
import com.example.nargesapp.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TransactionViewModel : ViewModel() {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions
    val balance = MutableStateFlow(0L)
    val totalIncome = MutableStateFlow(0L)
    val totalExpense = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            TransactionRepository.transactions.collectLatest { list ->
                _transactions.value = list.reversed()
                balance.value = TransactionRepository.getBalance()
                totalIncome.value = TransactionRepository.getTotalIncome()
                totalExpense.value = TransactionRepository.getTotalExpense()
            }
        }
    }

    fun addTransaction(transaction: Transaction) {
        TransactionRepository.addTransaction(transaction)
    }

    fun deleteTransaction(transaction: Transaction) {
        TransactionRepository.deleteTransaction(transaction)
    }

    fun getTransactionById(id: Int): Transaction? {
        return TransactionRepository.getTransactionById(id)
    }

    fun updateTransaction(transaction: Transaction) {
        TransactionRepository.updateTransaction(transaction)
    }
}
