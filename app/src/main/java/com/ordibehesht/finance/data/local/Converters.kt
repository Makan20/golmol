package com.ordibehesht.finance.data.local

import androidx.room.TypeConverter
import com.ordibehesht.finance.data.model.DebtType
import com.ordibehesht.finance.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromDebtType(value: DebtType): String = value.name

    @TypeConverter
    fun toDebtType(value: String): DebtType = DebtType.valueOf(value)
}