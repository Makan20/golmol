package com.ordibehesht.finance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ordibehesht.finance.data.model.Account
import com.ordibehesht.finance.data.model.Debt
import com.ordibehesht.finance.data.model.DebtNotification
import com.ordibehesht.finance.data.model.DebtPayment
import com.ordibehesht.finance.data.model.ShoppingItem
import com.ordibehesht.finance.data.model.Transaction
import com.ordibehesht.finance.dong.DongDao
import com.ordibehesht.finance.dong.DongExpenseEntity
import com.ordibehesht.finance.dong.DongGroupEntity
import com.ordibehesht.finance.dong.DongParticipantEntity
import com.ordibehesht.finance.dong.DongSettlementEntity

@Database(
    entities = [Transaction::class, Account::class, ShoppingItem::class, Debt::class, DebtPayment::class, DebtNotification::class, DongGroupEntity::class, DongParticipantEntity::class, DongExpenseEntity::class, DongSettlementEntity::class],
    version = 23,
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
    abstract fun dongDao(): DongDao

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
                    .addMigrations(MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN bankKey TEXT DEFAULT NULL")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN debtPaymentId INTEGER DEFAULT NULL")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN transferGroupId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE accounts ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN shoppingItemId INTEGER DEFAULT NULL")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE debts ADD COLUMN sourceTransactionTitle TEXT DEFAULT NULL")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // برای بازطراحی صفحه‌ی طلب و بدهی: نمایش «ساعت ثبت» زیر مبلغ هر کارت. رکوردهای قبلی
        // این فیلد را نداشتند، پس مقدار پیش‌فرض رشته‌ی خالی است (و در UI نمایش داده نمی‌شود)
        db.execSQL("ALTER TABLE debts ADD COLUMN createdTime TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // برای دیالوگ پرداخت: فیلد اختیاری توضیحات (مثلاً شماره تراکنش) + ساعت ثبت پرداخت،
        // هر دو در تاریخچه‌ی پرداخت‌های DebtDetailScreen نمایش داده می‌شوند
        db.execSQL("ALTER TABLE debt_payments ADD COLUMN time TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE debt_payments ADD COLUMN note TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ماژول دونگ (تقسیم هزینه‌ی گروهی): قبلاً کاملاً درون‌حافظه‌ای و موقتی بود،
        // از این نسخه به بعد ماندگار است و فقط با حذف صریح کاربر پاک می‌شود
        db.execSQL("CREATE TABLE IF NOT EXISTS dong_groups (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS dong_participants (id TEXT NOT NULL PRIMARY KEY, groupId TEXT NOT NULL, name TEXT NOT NULL, manualShare INTEGER)")
        db.execSQL("CREATE TABLE IF NOT EXISTS dong_expenses (id TEXT NOT NULL PRIMARY KEY, groupId TEXT NOT NULL, title TEXT NOT NULL, amount INTEGER NOT NULL, payerId TEXT NOT NULL, colorIndex INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS dong_settlements (id TEXT NOT NULL PRIMARY KEY, groupId TEXT NOT NULL, fromId TEXT NOT NULL, toId TEXT NOT NULL, amount INTEGER NOT NULL, date TEXT NOT NULL, time TEXT NOT NULL)")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // امکان آرشیو کردن گروه دونگ (بدون حذف داده) — سویپ روی هر ردیف در لیست گروه‌ها
        db.execSQL("ALTER TABLE dong_groups ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // تاریخ ثبت گروه دونگ — برای نمایش «تاریخ ثبت گروه» در لیست گروه‌ها
        db.execSQL("ALTER TABLE dong_groups ADD COLUMN createdDate TEXT NOT NULL DEFAULT ''")
    }
}