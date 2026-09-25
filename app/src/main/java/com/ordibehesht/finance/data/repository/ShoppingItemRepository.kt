package com.ordibehesht.finance.data.repository

import com.ordibehesht.finance.data.local.ShoppingItemDao
import com.ordibehesht.finance.data.model.ShoppingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object ShoppingItemRepository {
    private lateinit var dao: ShoppingItemDao
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _items = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val items: StateFlow<List<ShoppingItem>> = _items

    fun init(shoppingItemDao: ShoppingItemDao) {
        dao = shoppingItemDao
        scope.launch {
            dao.getAllItems().collect { list ->
                _items.value = list
            }
        }
    }

    fun addItem(item: ShoppingItem) {
        scope.launch { dao.insert(item) }
    }

    // نسخه‌ی suspend که id جدید تولیدشده را برمی‌گرداند — لازم برای بازگردانی آیتمی که قبلاً
    // به تراکنش تبدیل شده بود (addedToTransactions=true)، تا شناسه‌ی جدید بتواند به تراکنش
    // مرتبطش پیوند بخورد و ارتباط بین آن دو بعد از Undo قطع نشود
    suspend fun addItemAndGetId(item: ShoppingItem): Int = dao.insert(item).toInt()

    fun updateItem(item: ShoppingItem) {
        scope.launch { dao.update(item) }
    }

    fun deleteItem(item: ShoppingItem) {
        scope.launch { dao.delete(item) }
    }

    // باگ رفع‌شده: مشابه TransactionRepository.clearAll — قبلاً suspend نبود و caller
    // (AppResetManager) نمی‌توانست واقعاً منتظر تکمیل بماند.
    suspend fun clearAll() {
        dao.deleteAll()
    }

    suspend fun restoreAll(list: List<ShoppingItem>) {
        dao.deleteAll()
        if (list.isNotEmpty()) dao.insertAll(list)
    }
}