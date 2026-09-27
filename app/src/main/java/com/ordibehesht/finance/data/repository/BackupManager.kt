package com.ordibehesht.finance.data.repository

import android.content.Context
import com.ordibehesht.finance.data.model.Account
import com.ordibehesht.finance.data.model.Debt
import com.ordibehesht.finance.data.model.DebtNotification
import com.ordibehesht.finance.data.model.DebtPayment
import com.ordibehesht.finance.data.model.DebtType
import com.ordibehesht.finance.data.model.ShoppingItem
import com.ordibehesht.finance.data.model.Transaction
import com.ordibehesht.finance.data.model.TransactionType
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupManager {

    // نسخه ۴: افزودن Transaction.time، Transaction.debtId و Debt.dueTime به بکاپ
    // نسخه ۵: افزودن Transaction.debtPaymentId (پیوند یکتا به رکورد پرداخت مرتبط)
    // نسخه ۶: افزودن Transaction.shoppingItemId، Transaction.transferGroupId و Account.isArchived
    // نسخه ۷: افزودن Debt.sourceTransactionTitle (عنوان تراکنش مبدا، جدا از note)
    // نسخه ۸: افزودن Debt.createdTime (ساعت ثبت، برای بازطراحی صفحه‌ی طلب و بدهی)
    // نسخه ۹: افزودن DebtPayment.time و DebtPayment.note (ساعت پرداخت + توضیحات اختیاری)
    // نسخه ۱۰: افزودن گروه‌های دونگ (dongGroups) — شامل نفرات، هزینه‌ها، تسویه‌ها و
    // وضعیت آرشیو هر گروه؛ ماژول دونگ از این نسخه به بعد ماندگار است
    // (بکاپ‌های نسخه‌های قدیمی‌تر همچنان با optString/isNull بدون خطا لود می‌شوند)
    private const val SCHEMA_VERSION = 10

    fun getBackupDir(context: Context): File {
        val dir = context.getExternalFilesDir("backups")
            ?: File(context.filesDir, "backups")

        if (!dir.exists()) {
            dir.mkdirs()
        }

        return dir
    }

    fun listBackupFiles(context: Context): List<File> {
        val dir = getBackupDir(context)

        return dir.listFiles { file ->
            file.isFile && file.name.endsWith(".json")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun buildBackupJson(): String {
        val root = JSONObject()

        root.put("version", SCHEMA_VERSION)

        val accountsArray = JSONArray()

        AccountRepository.accounts.value.forEach { account ->
            accountsArray.put(
                JSONObject().apply {
                    put("id", account.id)
                    put("name", account.name)
                    put("cardNumber", account.cardNumber)
                    put("startingBalance", account.startingBalance)
                    put("colorIndex", account.colorIndex)
                    put("startingBalanceDate", account.startingBalanceDate)
                    put("bankKey", account.bankKey ?: JSONObject.NULL)
                    put("isArchived", account.isArchived)
                }
            )
        }

        root.put("accounts", accountsArray)

        val transactionsArray = JSONArray()

        TransactionRepository.transactions.value.forEach { transaction ->
            transactionsArray.put(
                JSONObject().apply {
                    put("id", transaction.id)
                    put("title", transaction.title)
                    put("amount", transaction.amount)
                    put("type", transaction.type.name)
                    put("category", transaction.category)
                    put("date", transaction.date)
                    put("time", transaction.time)
                    put("note", transaction.note)
                    put(
                        "accountId",
                        transaction.accountId ?: JSONObject.NULL
                    )
                    put(
                        "debtId",
                        transaction.debtId ?: JSONObject.NULL
                    )
                    put(
                        "debtPaymentId",
                        transaction.debtPaymentId ?: JSONObject.NULL
                    )
                    put(
                        "shoppingItemId",
                        transaction.shoppingItemId ?: JSONObject.NULL
                    )
                    put(
                        "transferGroupId",
                        transaction.transferGroupId ?: JSONObject.NULL
                    )
                }
            )
        }

        root.put("transactions", transactionsArray)

        val itemsArray = JSONArray()

        ShoppingItemRepository.items.value.forEach { item ->
            itemsArray.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("isPurchased", item.isPurchased)
                    put("createdDate", item.createdDate)
                    put(
                        "addedToTransactions",
                        item.addedToTransactions
                    )
                }
            )
        }

        root.put("shoppingItems", itemsArray)

        val debtsArray = JSONArray()

        DebtRepository.debts.value.forEach { debt ->
            debtsArray.put(
                JSONObject().apply {
                    put("id", debt.id)
                    put("personName", debt.personName)
                    put("amount", debt.amount)
                    put("type", debt.type.name)
                    put("dueDate", debt.dueDate)
                    put("dueTime", debt.dueTime)
                    put("note", debt.note)
                    put("createdDate", debt.createdDate)
                    put("isSettled", debt.isSettled)
                    put("paidAmount", debt.paidAmount)
                    put("reminderEnabled", debt.reminderEnabled)
                    put("loanGroupId", debt.loanGroupId ?: JSONObject.NULL)
                    put("installmentNumber", debt.installmentNumber ?: JSONObject.NULL)
                    put("totalInstallments", debt.totalInstallments ?: JSONObject.NULL)
                    put("sourceTransactionTitle", debt.sourceTransactionTitle ?: JSONObject.NULL)
                    put("createdTime", debt.createdTime)
                }
            )
        }

        root.put("debts", debtsArray)

        val debtPaymentsArray = JSONArray()

        DebtPaymentRepository.payments.value.forEach { payment ->
            debtPaymentsArray.put(
                JSONObject().apply {
                    put("id", payment.id)
                    put("debtId", payment.debtId)
                    put("amount", payment.amount)
                    put("date", payment.date)
                    put(
                        "accountId",
                        payment.accountId ?: JSONObject.NULL
                    )
                    put("time", payment.time)
                    put("note", payment.note)
                }
            )
        }

        root.put("debtPayments", debtPaymentsArray)

        val debtNotificationsArray = JSONArray()

        com.ordibehesht.finance.data.repository.DebtNotificationRepository.notifications.value.forEach { notification ->
            debtNotificationsArray.put(
                JSONObject().apply {
                    put("id", notification.id)
                    put("debtId", notification.debtId)
                    put("title", notification.title)
                    put("body", notification.body)
                    put("date", notification.date)
                    put("time", notification.time)
                    put("isRead", notification.isRead)
                }
            )
        }

        root.put("debtNotifications", debtNotificationsArray)

        val dongGroupsArray = JSONArray()

        com.ordibehesht.finance.dong.DongRepository.groups.value.forEach { group ->
            dongGroupsArray.put(dongGroupToJson(group))
        }

        root.put("dongGroups", dongGroupsArray)

        return root.toString(2)
    }

    /** یک گروه دونگ کامل (با نفرات، هزینه‌ها و تسویه‌هایش) را به JSON تبدیل می‌کند —
     * هم buildBackupJson و هم buildBackupJsonFromDb از این تابع مشترک استفاده می‌کنند. */
    private fun dongGroupToJson(group: com.ordibehesht.finance.dong.DongGroup): JSONObject {
        return JSONObject().apply {
            put("id", group.id)
            put("title", group.title)
            put("isArchived", group.isArchived)
            put("createdDate", group.createdDate)

            val participantsArray = JSONArray()
            group.participants.forEach { p ->
                participantsArray.put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("manualShare", p.manualShare ?: JSONObject.NULL)
                    }
                )
            }
            put("participants", participantsArray)

            val expensesArray = JSONArray()
            group.expenses.forEach { e ->
                expensesArray.put(
                    JSONObject().apply {
                        put("id", e.id)
                        put("title", e.title)
                        put("amount", e.amount)
                        put("payerId", e.payerId)
                        put("colorIndex", e.colorIndex)
                    }
                )
            }
            put("expenses", expensesArray)

            val settlementsArray = JSONArray()
            group.settlements.forEach { s ->
                settlementsArray.put(
                    JSONObject().apply {
                        put("id", s.id)
                        put("fromId", s.fromId)
                        put("toId", s.toId)
                        put("amount", s.amount)
                        put("date", s.date)
                        put("time", s.time)
                    }
                )
            }
            put("settlements", settlementsArray)
        }
    }

    // نسخه‌ی امن برای اجرای پس‌زمینه (مثلاً از AlarmManager، بدون هیچ اکتیویتی بازی): برخلاف
    // buildBackupJson که از AccountRepository.accounts.value و مشابه آن (StateFlow.value)
    // می‌خواند — که مقداردهی‌شان async است و اگر همین الان پروسه‌ی اپ توسط سیستم صرفاً برای
    // اجرای این زنگ استارت شده باشد ممکن است هنوز خالی باشند — این نسخه مستقیماً از DAO با
    // Flow.first() یک بار کوئری می‌گیرد، دقیقاً همان الگویی که DebtReminderAlarmReceiver هم
    // برای خواندن اطلاعات بدهی از دیتابیس استفاده می‌کند
    suspend fun buildBackupJsonFromDb(context: Context): String {
        val db = com.ordibehesht.finance.data.local.AppDatabase.getInstance(context)
        val accounts = db.accountDao().getAllAccounts().first()
        val transactions = db.transactionDao().getAllTransactions().first()
        val items = db.shoppingItemDao().getAllItems().first()
        val debts = db.debtDao().getAllDebts().first()
        val payments = db.debtPaymentDao().getAllPayments().first()
        val notifications = db.debtNotificationDao().getAll().first()
        val dongGroupEntities = db.dongDao().getAllGroups().first()
        val dongParticipantEntities = db.dongDao().getAllParticipants().first()
        val dongExpenseEntities = db.dongDao().getAllExpenses().first()
        val dongSettlementEntities = db.dongDao().getAllSettlements().first()

        val root = JSONObject()
        root.put("version", SCHEMA_VERSION)

        val accountsArray = JSONArray()
        accounts.forEach { account ->
            accountsArray.put(
                JSONObject().apply {
                    put("id", account.id)
                    put("name", account.name)
                    put("cardNumber", account.cardNumber)
                    put("startingBalance", account.startingBalance)
                    put("colorIndex", account.colorIndex)
                    put("startingBalanceDate", account.startingBalanceDate)
                    put("bankKey", account.bankKey ?: JSONObject.NULL)
                    put("isArchived", account.isArchived)
                }
            )
        }
        root.put("accounts", accountsArray)

        val transactionsArray = JSONArray()
        transactions.forEach { transaction ->
            transactionsArray.put(
                JSONObject().apply {
                    put("id", transaction.id)
                    put("title", transaction.title)
                    put("amount", transaction.amount)
                    put("type", transaction.type.name)
                    put("category", transaction.category)
                    put("date", transaction.date)
                    put("time", transaction.time)
                    put("note", transaction.note)
                    put("accountId", transaction.accountId ?: JSONObject.NULL)
                    put("debtId", transaction.debtId ?: JSONObject.NULL)
                    put("debtPaymentId", transaction.debtPaymentId ?: JSONObject.NULL)
                    put("shoppingItemId", transaction.shoppingItemId ?: JSONObject.NULL)
                    put("transferGroupId", transaction.transferGroupId ?: JSONObject.NULL)
                }
            )
        }
        root.put("transactions", transactionsArray)

        val itemsArray = JSONArray()
        items.forEach { item ->
            itemsArray.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("isPurchased", item.isPurchased)
                    put("createdDate", item.createdDate)
                    put("addedToTransactions", item.addedToTransactions)
                }
            )
        }
        root.put("shoppingItems", itemsArray)

        val debtsArray = JSONArray()
        debts.forEach { debt ->
            debtsArray.put(
                JSONObject().apply {
                    put("id", debt.id)
                    put("personName", debt.personName)
                    put("amount", debt.amount)
                    put("type", debt.type.name)
                    put("dueDate", debt.dueDate)
                    put("dueTime", debt.dueTime)
                    put("note", debt.note)
                    put("createdDate", debt.createdDate)
                    put("isSettled", debt.isSettled)
                    put("paidAmount", debt.paidAmount)
                    put("reminderEnabled", debt.reminderEnabled)
                    put("loanGroupId", debt.loanGroupId ?: JSONObject.NULL)
                    put("installmentNumber", debt.installmentNumber ?: JSONObject.NULL)
                    put("totalInstallments", debt.totalInstallments ?: JSONObject.NULL)
                    put("sourceTransactionTitle", debt.sourceTransactionTitle ?: JSONObject.NULL)
                    put("createdTime", debt.createdTime)
                }
            )
        }
        root.put("debts", debtsArray)

        val debtPaymentsArray = JSONArray()
        payments.forEach { payment ->
            debtPaymentsArray.put(
                JSONObject().apply {
                    put("id", payment.id)
                    put("debtId", payment.debtId)
                    put("amount", payment.amount)
                    put("date", payment.date)
                    put("accountId", payment.accountId ?: JSONObject.NULL)
                    put("time", payment.time)
                    put("note", payment.note)
                }
            )
        }
        root.put("debtPayments", debtPaymentsArray)

        val debtNotificationsArray = JSONArray()
        notifications.forEach { notification ->
            debtNotificationsArray.put(
                JSONObject().apply {
                    put("id", notification.id)
                    put("debtId", notification.debtId)
                    put("title", notification.title)
                    put("body", notification.body)
                    put("date", notification.date)
                    put("time", notification.time)
                    put("isRead", notification.isRead)
                }
            )
        }
        root.put("debtNotifications", debtNotificationsArray)

        val dongGroupsArray = JSONArray()
        dongGroupEntities.forEach { g ->
            val group = com.ordibehesht.finance.dong.DongGroup(
                id = g.id,
                title = g.title,
                participants = dongParticipantEntities
                    .filter { it.groupId == g.id }
                    .map { com.ordibehesht.finance.dong.DongParticipant(id = it.id, name = it.name, manualShare = it.manualShare) },
                expenses = dongExpenseEntities
                    .filter { it.groupId == g.id }
                    .map { com.ordibehesht.finance.dong.DongExpense(id = it.id, title = it.title, amount = it.amount, payerId = it.payerId, colorIndex = it.colorIndex) },
                settlements = dongSettlementEntities
                    .filter { it.groupId == g.id }
                    .map { com.ordibehesht.finance.dong.DongSettlementRecord(id = it.id, fromId = it.fromId, toId = it.toId, amount = it.amount, date = it.date, time = it.time) },
                isArchived = g.isArchived,
                createdDate = g.createdDate
            )
            dongGroupsArray.put(dongGroupToJson(group))
        }
        root.put("dongGroups", dongGroupsArray)

        return root.toString(2)
    }

    suspend fun saveBackupToFileFromDb(context: Context, fileName: String): File {
        val file = File(getBackupDir(context), fileName)
        file.writeText(buildBackupJsonFromDb(context), Charsets.UTF_8)
        return file
    }

    // نگه‌داری حداکثر ۵ فایل پشتیبان خودکار؛ قدیمی‌ترین با آمدن فایل جدید حذف می‌شود.
    // فقط فایل‌های پشتیبان *خودکار* (پیشوند auto_) را می‌شمارد و پاک می‌کند — فایل‌هایی که
    // کاربر خودش دستی از تنظیمات گرفته (بدون این پیشوند) هرگز توسط این پاک‌سازی حذف نمی‌شوند
    fun pruneOldAutoBackups(context: Context, keepCount: Int = 5) {
        val autoBackups = getBackupDir(context)
            .listFiles { file -> file.isFile && file.name.startsWith("auto_") && file.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (autoBackups.size > keepCount) {
            autoBackups.drop(keepCount).forEach { it.delete() }
        }
    }

    fun saveBackupToFile(
        context: Context,
        fileName: String
    ): File {
        val file = File(getBackupDir(context), fileName)

        file.writeText(
            buildBackupJson(),
            Charsets.UTF_8
        )

        return file
    }

    suspend fun restoreFromFile(file: File) {
        restoreFromJson(
            file.readText(Charsets.UTF_8)
        )
    }

    suspend fun restoreFromJson(json: String) {
        val root = JSONObject(json)

        val accountsArray = root.optJSONArray("accounts") ?: JSONArray()

        val accounts = (0 until accountsArray.length()).map { index ->
            val item = accountsArray.getJSONObject(index)

            Account(
                id = item.getInt("id"),
                name = item.getString("name"),
                cardNumber = item.optString("cardNumber", ""),
                startingBalance = item.getLong("startingBalance"),
                colorIndex = item.optInt("colorIndex", 0),
                startingBalanceDate = item.optString(
                    "startingBalanceDate",
                    ""
                ),
                bankKey = if (item.isNull("bankKey")) null else item.optString("bankKey", null),
                isArchived = item.optBoolean("isArchived", false)
            )
        }

        AccountRepository.restoreAll(accounts)

        // توجه: Transaction.debtId هیچ @ForeignKey واقعی به جدول debts ندارد،
        // پس ترتیب ریستور (قبل یا بعد از Debt) مشکلی ایجاد نمی‌کند
        val transactionsArray =
            root.optJSONArray("transactions") ?: JSONArray()

        val transactions = (0 until transactionsArray.length()).map { index ->
            val item = transactionsArray.getJSONObject(index)

            Transaction(
                id = item.getInt("id"),
                title = item.getString("title"),
                amount = item.getLong("amount"),
                type = TransactionType.valueOf(
                    item.getString("type")
                ),
                category = item.getString("category"),
                date = item.getString("date"),
                time = item.optString("time", ""),
                note = item.optString("note", ""),
                accountId = if (item.isNull("accountId")) {
                    null
                } else {
                    item.getInt("accountId")
                },
                debtId = if (item.isNull("debtId")) {
                    null
                } else {
                    item.optInt("debtId")
                },
                debtPaymentId = if (item.isNull("debtPaymentId")) {
                    null
                } else {
                    item.optInt("debtPaymentId")
                },
                shoppingItemId = if (item.isNull("shoppingItemId")) {
                    null
                } else {
                    item.optInt("shoppingItemId")
                },
                transferGroupId = if (item.isNull("transferGroupId")) {
                    null
                } else {
                    item.optString("transferGroupId", null)
                }
            )
        }

        TransactionRepository.restoreAll(transactions)

        val itemsArray =
            root.optJSONArray("shoppingItems") ?: JSONArray()

        val items = (0 until itemsArray.length()).map { index ->
            val item = itemsArray.getJSONObject(index)

            ShoppingItem(
                id = item.getInt("id"),
                title = item.getString("title"),
                isPurchased = item.optBoolean(
                    "isPurchased",
                    false
                ),
                createdDate = item.getString("createdDate"),
                addedToTransactions = item.optBoolean(
                    "addedToTransactions",
                    false
                )
            )
        }

        ShoppingItemRepository.restoreAll(items)

        val debtsArray = root.optJSONArray("debts") ?: JSONArray()

        val debts = (0 until debtsArray.length()).map { index ->
            val item = debtsArray.getJSONObject(index)

            Debt(
                id = item.getInt("id"),
                personName = item.getString("personName"),
                amount = item.getLong("amount"),
                type = DebtType.valueOf(
                    item.getString("type")
                ),
                dueDate = item.optString("dueDate", ""),
                dueTime = item.optString("dueTime", ""),
                note = item.optString("note", ""),
                createdDate = item.getString("createdDate"),
                isSettled = item.optBoolean(
                    "isSettled",
                    false
                ),
                paidAmount = item.optLong("paidAmount", 0L),
                reminderEnabled = item.optBoolean("reminderEnabled", true),
                loanGroupId = if (item.isNull("loanGroupId")) null else item.optString("loanGroupId", null),
                installmentNumber = if (item.isNull("installmentNumber")) null else item.optInt("installmentNumber"),
                totalInstallments = if (item.isNull("totalInstallments")) null else item.optInt("totalInstallments"),
                // در بکاپ‌های قدیمی‌تر از این پچ، این کلید اصلاً وجود ندارد — isNull/optString با
                // fallback به null هر دو حالت (کلید غایب یا صریحاً JSONObject.NULL) را پوشش می‌دهد
                sourceTransactionTitle = if (item.isNull("sourceTransactionTitle")) null else item.optString("sourceTransactionTitle", null),
                // مشابه بالا: در بکاپ‌های قدیمی‌تر غایب است؛ optString با fallback رشته‌ی خالی
                // (نه null، چون createdTime یک String غیرقابل‌نال است) این حالت را هم پوشش می‌دهد
                createdTime = item.optString("createdTime", "")
            )
        }

        DebtRepository.restoreAll(debts)

        val debtPaymentsArray =
            root.optJSONArray("debtPayments") ?: JSONArray()

        val debtPayments = (0 until debtPaymentsArray.length()).map { index ->
            val item = debtPaymentsArray.getJSONObject(index)

            DebtPayment(
                id = item.getInt("id"),
                debtId = item.getInt("debtId"),
                amount = item.getLong("amount"),
                date = item.getString("date"),
                accountId = if (item.isNull("accountId")) {
                    null
                } else {
                    item.getInt("accountId")
                },
                // در بکاپ‌های قدیمی‌تر از این پچ این دو کلید وجود ندارند؛ optString با
                // fallback رشته‌ی خالی هر دو حالت (کلید غایب یا صریحاً NULL) را پوشش می‌دهد
                time = item.optString("time", ""),
                note = item.optString("note", "")
            )
        }

        DebtPaymentRepository.restoreAll(debtPayments)

        val debtNotificationsArray =
            root.optJSONArray("debtNotifications") ?: JSONArray()

        val debtNotifications = (0 until debtNotificationsArray.length()).map { index ->
            val item = debtNotificationsArray.getJSONObject(index)

            DebtNotification(
                id = item.getInt("id"),
                debtId = item.getInt("debtId"),
                title = item.getString("title"),
                body = item.getString("body"),
                date = item.getString("date"),
                time = item.getString("time"),
                isRead = item.optBoolean("isRead", false)
            )
        }

        com.ordibehesht.finance.data.repository.DebtNotificationRepository.restoreAll(debtNotifications)

        // بکاپ‌های قدیمی‌تر از این نسخه اصلاً کلید dongGroups را ندارند — optJSONArray در
        // آن حالت null برمی‌گرداند و JSONArray خالی یعنی «هیچ گروه دونگی بازیابی نشود»،
        // نه این‌که گروه‌های فعلی کاربر (اگر با نسخه‌ی جدیدتر ساخته شده باشند) پاک شوند؛
        // بنابراین وقتی کلید غایب است، restoreAll اصلاً صدا زده نمی‌شود.
        if (root.has("dongGroups")) {
            val dongGroupsArray = root.optJSONArray("dongGroups") ?: JSONArray()

            val dongGroups = (0 until dongGroupsArray.length()).map { index ->
                val item = dongGroupsArray.getJSONObject(index)

                val participantsArray = item.optJSONArray("participants") ?: JSONArray()
                val participants = (0 until participantsArray.length()).map { pIndex ->
                    val p = participantsArray.getJSONObject(pIndex)
                    com.ordibehesht.finance.dong.DongParticipant(
                        id = p.getString("id"),
                        name = p.getString("name"),
                        manualShare = if (p.isNull("manualShare")) null else p.optLong("manualShare")
                    )
                }

                val expensesArray = item.optJSONArray("expenses") ?: JSONArray()
                val expenses = (0 until expensesArray.length()).map { eIndex ->
                    val e = expensesArray.getJSONObject(eIndex)
                    com.ordibehesht.finance.dong.DongExpense(
                        id = e.getString("id"),
                        title = e.getString("title"),
                        amount = e.getLong("amount"),
                        payerId = e.getString("payerId"),
                        colorIndex = e.optInt("colorIndex", 0)
                    )
                }

                val settlementsArray = item.optJSONArray("settlements") ?: JSONArray()
                val settlements = (0 until settlementsArray.length()).map { sIndex ->
                    val s = settlementsArray.getJSONObject(sIndex)
                    com.ordibehesht.finance.dong.DongSettlementRecord(
                        id = s.getString("id"),
                        fromId = s.getString("fromId"),
                        toId = s.getString("toId"),
                        amount = s.getLong("amount"),
                        date = s.getString("date"),
                        time = s.getString("time")
                    )
                }

                com.ordibehesht.finance.dong.DongGroup(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    participants = participants,
                    expenses = expenses,
                    settlements = settlements,
                    isArchived = item.optBoolean("isArchived", false),
                    createdDate = item.optString("createdDate", "")
                )
            }

            com.ordibehesht.finance.dong.DongRepository.restoreAll(dongGroups)
        }
    }
}