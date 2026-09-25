package com.ordibehesht.finance.dong

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * مخزن ماندگار (Room) برای گروه‌های دونگ.
 * از این نسخه به بعد گروه‌ها با بستن اپ از بین نمی‌روند — فقط با حذف صریح کاربر پاک
 * می‌شوند. چهار جدول مسطح (groups/participants/expenses/settlements) در DongDao با هم
 * combine می‌شوند تا لیست DongGroup کامل (با لیست‌های تودرتو) برای UI ساخته شود.
 */
object DongRepository {

    private lateinit var dao: DongDao
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _groups = MutableStateFlow<List<DongGroup>>(emptyList())
    val groups: StateFlow<List<DongGroup>> = _groups

    fun init(dao: DongDao) {
        this.dao = dao
        scope.launch {
            combine(
                dao.getAllGroups(),
                dao.getAllParticipants(),
                dao.getAllExpenses(),
                dao.getAllSettlements()
            ) { groupEntities, participantEntities, expenseEntities, settlementEntities ->
                groupEntities.map { g ->
                    DongGroup(
                        id = g.id,
                        title = g.title,
                        participants = participantEntities
                            .filter { it.groupId == g.id }
                            .map { DongParticipant(id = it.id, name = it.name, manualShare = it.manualShare) },
                        expenses = expenseEntities
                            .filter { it.groupId == g.id }
                            .map { DongExpense(id = it.id, title = it.title, amount = it.amount, payerId = it.payerId, colorIndex = it.colorIndex) },
                        settlements = settlementEntities
                            .filter { it.groupId == g.id }
                            .map { DongSettlementRecord(id = it.id, fromId = it.fromId, toId = it.toId, amount = it.amount, date = it.date, time = it.time) },
                        isArchived = g.isArchived,
                        createdDate = g.createdDate
                    )
                }
            }.collect { list ->
                _groups.value = list
            }
        }
    }

    fun groupById(id: String): DongGroup? = _groups.value.find { it.id == id }

    fun addGroup(title: String): DongGroup {
        val createdDate = com.ordibehesht.finance.ui.utils.PersianDateUtils.getCurrentPersianDate()
        val group = DongGroup(title = title, createdDate = createdDate)
        // Optimistic update: فوراً به state محلی اضافه می‌شود تا کاربر بدون تأخیر (حتی یک
        // فریم) به صفحه‌ی گروه هدایت شود؛ وقتی نوشتن در دیتابیس کامل شد، combine همین
        // آیتم را (با همان id) از DB هم برمی‌گرداند و چیزی عوض نمی‌شود
        _groups.value = _groups.value + group
        scope.launch {
            dao.insertGroup(DongGroupEntity(id = group.id, title = group.title, createdDate = group.createdDate))
        }
        return group
    }

    fun deleteGroup(groupId: String) {
        _groups.value = _groups.value.filterNot { it.id == groupId }
        scope.launch {
            dao.deleteParticipantsForGroup(groupId)
            dao.deleteExpensesForGroup(groupId)
            dao.deleteSettlementsForGroup(groupId)
            dao.deleteGroup(groupId)
        }
    }

    fun renameGroup(groupId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        val current = groupById(groupId) ?: return
        scope.launch {
            dao.updateGroup(
                DongGroupEntity(
                    id = groupId,
                    title = newTitle.trim(),
                    isArchived = current.isArchived,
                    createdDate = current.createdDate
                )
            )
        }
    }

    fun setGroupArchived(groupId: String, archived: Boolean) {
        updateLocalGroup(groupId) { it.copy(isArchived = archived) }
        scope.launch { dao.setGroupArchived(groupId, archived) }
    }

    fun addParticipant(groupId: String, name: String) {
        if (name.isBlank()) return
        val participant = DongParticipant(name = name.trim())
        updateLocalGroup(groupId) { it.copy(participants = it.participants + participant) }
        scope.launch {
            dao.insertParticipant(
                DongParticipantEntity(id = participant.id, groupId = groupId, name = participant.name, manualShare = null)
            )
        }
    }

    fun removeParticipant(groupId: String, participantId: String) {
        updateLocalGroup(groupId) { g ->
            g.copy(
                participants = g.participants.filterNot { it.id == participantId },
                expenses = g.expenses.filterNot { it.payerId == participantId },
                settlements = g.settlements.filterNot { it.fromId == participantId || it.toId == participantId }
            )
        }
        scope.launch {
            // هزینه‌ها و تسویه‌های مرتبط با این فرد هم حذف می‌شوند تا داده ناسازگار نماند
            dao.deleteExpensesByPayer(participantId)
            dao.deleteSettlementsInvolving(participantId)
            dao.deleteParticipant(participantId)
        }
    }

    /** تنظیم سهم دستی یک نفر؛ مقدار null یعنی بازگشت به تقسیم خودکار/مساوی. */
    fun setManualShare(groupId: String, participantId: String, manualShare: Long?) {
        val group = groupById(groupId) ?: return
        val participant = group.participants.find { it.id == participantId } ?: return
        updateLocalGroup(groupId) { g ->
            g.copy(participants = g.participants.map { if (it.id == participantId) it.copy(manualShare = manualShare) else it })
        }
        scope.launch {
            dao.updateParticipant(
                DongParticipantEntity(id = participant.id, groupId = groupId, name = participant.name, manualShare = manualShare)
            )
        }
    }

    /**
     * ثبت یک تسویه‌ی واقعی از fromId به toId — مثلاً «گندم به سارا ۱۰۰,۰۰۰ تومان پرداخت کرد».
     * تاریخ و ساعت به‌صورت خودکار از زمان فعلی سیستم گرفته می‌شود.
     * قاعده‌ی «فقط بدهکار به طلبکار» و مبلغ معتبر اینجا اجرا می‌شود، نه در UI، تا این
     * محدودیت هرجا این تابع صدا زده شود رعایت شود.
     */
    fun addSettlement(groupId: String, fromId: String, toId: String, amount: Long): Boolean {
        if (amount <= 0 || fromId.isBlank() || toId.isBlank() || fromId == toId) return false
        val group = groupById(groupId) ?: return false
        val statuses = DongCalculator.calculateStatuses(group)
        val fromStatus = statuses.find { it.participant.id == fromId } ?: return false
        val toStatus = statuses.find { it.participant.id == toId } ?: return false
        // فقط از بدهکار به طلبکار مجاز است
        if (fromStatus.balance >= 0 || toStatus.balance <= 0) return false
        // سقف مبلغ: نه بیشتر از بدهی فرستنده، نه بیشتر از طلب گیرنده — وگرنه یک طلب/بدهی
        // برعکس و جدید بین همین دو نفر ایجاد می‌شود
        val maxAllowed = minOf(-fromStatus.balance, toStatus.balance)
        if (amount > maxAllowed) return false

        val date = com.ordibehesht.finance.ui.utils.PersianDateUtils.getCurrentPersianDate()
        val time = com.ordibehesht.finance.ui.utils.PersianDateUtils.getCurrentTime()
        val record = DongSettlementRecord(fromId = fromId, toId = toId, amount = amount, date = date, time = time)

        updateLocalGroup(groupId) { it.copy(settlements = it.settlements + record) }
        scope.launch {
            dao.insertSettlement(
                DongSettlementEntity(
                    id = record.id,
                    groupId = groupId,
                    fromId = record.fromId,
                    toId = record.toId,
                    amount = record.amount,
                    date = record.date,
                    time = record.time
                )
            )
        }
        return true
    }

    fun deleteSettlement(groupId: String, settlementId: String) {
        updateLocalGroup(groupId) { it.copy(settlements = it.settlements.filterNot { s -> s.id == settlementId }) }
        scope.launch { dao.deleteSettlement(settlementId) }
    }

    fun addExpense(groupId: String, title: String, amount: Long, payerId: String) {
        if (title.isBlank() || amount <= 0 || payerId.isBlank()) return
        val expense = DongExpense(title = title.trim(), amount = amount, payerId = payerId)
        updateLocalGroup(groupId) { it.copy(expenses = it.expenses + expense) }
        scope.launch {
            dao.insertExpense(
                DongExpenseEntity(
                    id = expense.id,
                    groupId = groupId,
                    title = expense.title,
                    amount = expense.amount,
                    payerId = expense.payerId,
                    colorIndex = expense.colorIndex
                )
            )
        }
    }

    fun deleteExpense(groupId: String, expenseId: String) {
        // اگر با حذف این هزینه، هیچ هزینه‌ای برای گروه باقی نماند، طبق تصمیم کاربر
        // تسویه‌های ثبت‌شده‌ی این گروه هم پاک می‌شوند تا محاسبات واقعاً از صفر شروع شود —
        // وگرنه مانده‌ی نفرات با تسویه‌های قدیمی (بدون هیچ هزینه‌ای در پس‌زمینه) ناهمخوان می‌ماند
        val remainingExpenseCount = groupById(groupId)?.expenses?.count { it.id != expenseId } ?: -1
        val shouldClearSettlements = remainingExpenseCount == 0

        updateLocalGroup(groupId) { g ->
            g.copy(
                expenses = g.expenses.filterNot { e -> e.id == expenseId },
                settlements = if (shouldClearSettlements) emptyList() else g.settlements
            )
        }
        scope.launch {
            dao.deleteExpense(expenseId)
            if (shouldClearSettlements) {
                dao.deleteSettlementsForGroup(groupId)
            }
        }
    }

    /** آپدیت خوش‌بینانه‌ی state محلی، بدون منتظر ماندن برای نوشتن در دیتابیس. */
    private fun updateLocalGroup(groupId: String, transform: (DongGroup) -> DongGroup) {
        _groups.value = _groups.value.map { if (it.id == groupId) transform(it) else it }
    }

    /** پاک کردن کامل همه‌ی گروه‌های دونگ (فعال و آرشیوشده) — برای «پاک کردن کل دیتا». */
    // باگ رفع‌شده: مشابه TransactionRepository.clearAll — بخش دیتابیسی قبلاً suspend نبود
    // (scope.launch جدا) و caller (AppResetManager) نمی‌توانست واقعاً منتظر تکمیل بماند.
    // آپدیت _groups.value همچنان همزمان (synchronous) است، همان‌طور که قبلاً هم بود.
    suspend fun clearAll() {
        _groups.value = emptyList()
        dao.deleteAllParticipants()
        dao.deleteAllExpenses()
        dao.deleteAllSettlements()
        dao.deleteAllGroups()
    }

    /**
     * جایگزینی کامل داده‌های دونگ با یک لیست دیگر — برای بازیابی از پشتیبان.
     * suspend است تا BackupManager بتواند قبل از ادامه‌ی بازیابی بقیه‌ی بخش‌ها صبر کند،
     * دقیقاً هم‌الگو با AccountRepository.restoreAll/TransactionRepository.restoreAll.
     */
    suspend fun restoreAll(groups: List<DongGroup>) {
        dao.deleteAllParticipants()
        dao.deleteAllExpenses()
        dao.deleteAllSettlements()
        dao.deleteAllGroups()

        groups.forEach { g ->
            dao.insertGroup(DongGroupEntity(id = g.id, title = g.title, isArchived = g.isArchived, createdDate = g.createdDate))
            g.participants.forEach { p ->
                dao.insertParticipant(DongParticipantEntity(id = p.id, groupId = g.id, name = p.name, manualShare = p.manualShare))
            }
            g.expenses.forEach { e ->
                dao.insertExpense(DongExpenseEntity(id = e.id, groupId = g.id, title = e.title, amount = e.amount, payerId = e.payerId, colorIndex = e.colorIndex))
            }
            g.settlements.forEach { s ->
                dao.insertSettlement(DongSettlementEntity(id = s.id, groupId = g.id, fromId = s.fromId, toId = s.toId, amount = s.amount, date = s.date, time = s.time))
            }
        }
        // combine در init با تأخیر (async) خودش را با DB هماهنگ می‌کند، ولی برای این‌که
        // بلافاصله بعد از restoreAll (بدون منتظر ماندن برای رسیدن Flow) هم UI مقدار درست
        // را ببیند، state محلی را همین‌جا هم مستقیماً به‌روزرسانی می‌کنیم
        _groups.value = groups
    }
}
