package com.example.nargesapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.nargesapp.data.model.Account
import com.example.nargesapp.data.model.Debt
import com.example.nargesapp.data.model.DebtNotification
import com.example.nargesapp.data.model.DebtPayment
import com.example.nargesapp.data.model.ShoppingItem
import com.example.nargesapp.data.model.Transaction

@Database(
    entities = [Transaction::class, Account::class, ShoppingItem::class, Debt::class, DebtPayment::class, DebtNotification::class],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun debtDao(): DebtDao
    abstract fun debtPaymentDao(): DebtPaymentDao
    abstract fun debtNotificationDao(): DebtNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "narges_app.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}