package com.example.nargesapp.data.repository

import android.content.Context
import com.example.nargesapp.data.model.Account
import com.example.nargesapp.data.model.Debt
import com.example.nargesapp.data.model.DebtNotification
import com.example.nargesapp.data.model.DebtPayment
import com.example.nargesapp.data.model.DebtType
import com.example.nargesapp.data.model.ShoppingItem
import com.example.nargesapp.data.model.Transaction
import com.example.nargesapp.data.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupManager {

    private const val SCHEMA_VERSION = 3

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
                    put("note", transaction.note)
                    put(
                        "accountId",
                        transaction.accountId ?: JSONObject.NULL
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
                    put("note", debt.note)
                    put("createdDate", debt.createdDate)
                    put("isSettled", debt.isSettled)
                    put("paidAmount", debt.paidAmount)
                    put("reminderEnabled", debt.reminderEnabled)
                    put("loanGroupId", debt.loanGroupId ?: JSONObject.NULL)
                    put("installmentNumber", debt.installmentNumber ?: JSONObject.NULL)
                    put("totalInstallments", debt.totalInstallments ?: JSONObject.NULL)
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
                }
            )
        }

        root.put("debtPayments", debtPaymentsArray)

        val debtNotificationsArray = JSONArray()

        com.example.nargesapp.data.repository.DebtNotificationRepository.notifications.value.forEach { notification ->
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

        return root.toString(2)
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
                )
            )
        }

        AccountRepository.restoreAll(accounts)

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
                note = item.optString("note", ""),
                accountId = if (item.isNull("accountId")) {
                    null
                } else {
                    item.getInt("accountId")
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
                totalInstallments = if (item.isNull("totalInstallments")) null else item.optInt("totalInstallments")
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
                }
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

        com.example.nargesapp.data.repository.DebtNotificationRepository.restoreAll(debtNotifications)
    }
}