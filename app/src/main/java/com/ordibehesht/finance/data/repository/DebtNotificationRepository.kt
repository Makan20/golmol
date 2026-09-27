package com.ordibehesht.finance.data.repository

import com.ordibehesht.finance.data.local.DebtNotificationDao
import com.ordibehesht.finance.data.model.DebtNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object DebtNotificationRepository {
    private lateinit var dao: DebtNotificationDao
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _notifications = MutableStateFlow<List<DebtNotification>>(emptyList())
    val notifications: StateFlow<List<DebtNotification>> = _notifications

    fun init(notificationDao: DebtNotificationDao) {
        dao = notificationDao
        scope.launch {
            dao.getAll().collect { list ->
                _notifications.value = list
            }
        }
    }

    fun add(notification: DebtNotification) {
        scope.launch { dao.insert(notification) }
    }

    fun markAsRead(notification: DebtNotification) {
        scope.launch { dao.update(notification.copy(isRead = true)) }
    }

    fun markAllAsRead() {
        scope.launch { dao.markAllAsRead() }
    }

    fun delete(notification: DebtNotification) {
        scope.launch { dao.delete(notification) }
    }

    /** حذف تمام اعلان‌های تاریخچه‌ی مربوط به یک بدهی/طلب مشخص (هنگام حذف خود بدهی) */
    fun deleteForDebt(debtId: Int) {
        scope.launch { dao.deleteForDebt(debtId) }
    }

    // باگ رفع‌شده: مشابه TransactionRepository.clearAll — قبلاً suspend نبود و caller
    // (AppResetManager) نمی‌توانست واقعاً منتظر تکمیل بماند.
    suspend fun clearAll() {
        dao.deleteAll()
    }

    suspend fun restoreAll(list: List<DebtNotification>) {
        dao.deleteAll()
        if (list.isNotEmpty()) dao.insertAll(list)
    }
}
