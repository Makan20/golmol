package com.ordibehesht.finance.data.repository

import com.ordibehesht.finance.data.local.TransactionDao
import com.ordibehesht.finance.data.model.Transaction
import com.ordibehesht.finance.data.model.TransactionType
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

    // اگر این تراکنش نتیجه‌ی تبدیل یک آیتم لیست خرید بوده (shoppingItemId != null)، حذفش
    // یعنی آن آیتم دیگر واقعاً «ثبت‌شده» نیست — بدون این، آیتم برای همیشه با نشان «ثبت شد»
    // و چک‌باکس غیرفعال قفل می‌ماند، بدون هیچ راهی برای اصلاح جز حذف و ساخت دوباره‌ی خودش
    fun deleteTransaction(transaction: Transaction) {
        scope.launch {
            dao.delete(transaction)
            val shoppingItemId = transaction.shoppingItemId
            if (shoppingItemId != null) {
                val item = ShoppingItemRepository.items.value.find { it.id == shoppingItemId }
                if (item != null && item.addedToTransactions) {
                    ShoppingItemRepository.updateItem(item.copy(addedToTransactions = false))
                }
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        scope.launch { dao.update(transaction) }
    }

    // وقتی آیتم لیست خریدی که قبلاً تبدیل به تراکنش شده بود حذف و سپس Undo می‌شود، رکورد
    // بازگردانده‌شده id تازه‌ای از Room می‌گیرد (چون id قدیمی دیگر در جدول وجود ندارد)، پس
    // shoppingItemId تراکنش قدیمی باید به همین id جدید اصلاح شود، وگرنه پیوندشان برای همیشه
    // قطع می‌ماند — بدون کرش، فقط دیگر با هم sync نیستند
    fun relinkShoppingItem(oldItemId: Int, newItemId: Int) {
        scope.launch {
            val linked = _transactions.value.find { it.shoppingItemId == oldItemId }
            if (linked != null) {
                dao.update(linked.copy(shoppingItemId = newItemId))
            }
        }
    }

    fun getTransactionById(id: Int): Transaction? {
        return _transactions.value.find { it.id == id }
    }

    // مجموع درآمد/هزینه شامل تراکنش‌های خودکارِ تسویه‌ی بدهی/طلب هم می‌شود —
    // هماهنگ با totalIncome/totalExpense در HomeScreen و ReportsScreen.
    // تراکنش‌های انتقال بین کارت‌ها (transferGroupId != null) حساب نمی‌شوند چون جابه‌جایی
    // داخلی پول‌اند، نه درآمد/هزینه‌ی واقعی
    fun getTotalIncome(): Long =
        _transactions.value.filter { it.type == TransactionType.INCOME && it.transferGroupId == null }.sumOf { it.amount }

    fun getTotalExpense(): Long =
        _transactions.value.filter { it.type == TransactionType.EXPENSE && it.transferGroupId == null }.sumOf { it.amount }

    fun getBalance(): Long = getTotalIncome() - getTotalExpense()

    // باگ رفع‌شده: قبلاً این تابع suspend نبود (fun clearAll() { scope.launch { ... } })، پس
    // caller (AppResetManager) نمی‌توانست واقعاً منتظر تکمیل پاک‌سازی بماند — فقط یک coroutine
    // async روی scope داخلی این آبجکت راه می‌انداخت و بلافاصله برمی‌گشت. حالا suspend است و
    // مستقیم (بدون scope.launch جدا) دیتابیس را پاک می‌کند، تا caller بتواند با await واقعی
    // مطمئن شود پاک‌سازی قبل از ادامه‌ی کار تمام شده — دقیقاً همان الگویی که restoreAll در
    // AccountRepository برای همین منظور استفاده می‌کند.
    suspend fun clearAll() {
        dao.deleteAll()
    }

    fun deleteByDebtId(debtId: Int) {
        scope.launch { dao.deleteByDebtId(debtId) }
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