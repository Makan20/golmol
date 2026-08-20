package com.example.nargesapp.data.repository

import com.example.nargesapp.data.local.ShoppingItemDao
import com.example.nargesapp.data.model.ShoppingItem
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

    fun updateItem(item: ShoppingItem) {
        scope.launch { dao.update(item) }
    }

    fun deleteItem(item: ShoppingItem) {
        scope.launch { dao.delete(item) }
    }

    fun clearAll() {
        scope.launch { dao.deleteAll() }
    }

    suspend fun restoreAll(list: List<ShoppingItem>) {
        dao.deleteAll()
        if (list.isNotEmpty()) dao.insertAll(list)
    }
}