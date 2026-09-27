package com.ordibehesht.finance.data.repository

import com.ordibehesht.finance.data.local.AccountDao
import com.ordibehesht.finance.data.local.TransactionDao
import com.ordibehesht.finance.data.model.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object AccountRepository {
    private lateinit var accountDao: AccountDao
    private lateinit var transactionDao: TransactionDao
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    fun init(accountDao: AccountDao, transactionDao: TransactionDao) {
        this.accountDao = accountDao
        this.transactionDao = transactionDao
        scope.launch {
            accountDao.getAllAccounts().collect { list ->
                _accounts.value = list
            }
        }
    }

    fun addAccount(account: Account) {
        scope.launch { accountDao.insert(account) }
    }

    fun updateAccount(account: Account) {
        scope.launch { accountDao.update(account) }
    }

    fun deleteAccount(account: Account) {
        scope.launch {
            transactionDao.clearAccountReferences(account.id)
            DebtPaymentRepository.clearAccountReferences(account.id)
            accountDao.delete(account)
        }
    }

    // باگ رفع‌شده: مشابه TransactionRepository.clearAll — قبلاً suspend نبود و caller
    // (AppResetManager) نمی‌توانست واقعاً منتظر تکمیل بماند.
    suspend fun clearAll() {
        accountDao.deleteAll()
    }

    // Suspend so BackupManager can await this before restoring transactions,
    // since transactions may reference these account ids.
    suspend fun restoreAll(list: List<Account>) {
        accountDao.deleteAll()
        if (list.isNotEmpty()) accountDao.insertAll(list)
    }
}