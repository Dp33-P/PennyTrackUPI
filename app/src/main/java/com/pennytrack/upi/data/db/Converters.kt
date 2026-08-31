package com.pennytrack.upi.data.db

import androidx.room.TypeConverter
import com.pennytrack.upi.data.model.TransactionKind
import com.pennytrack.upi.data.model.TransactionSource
import com.pennytrack.upi.data.model.TransactionType

class Converters {
    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionKind(value: String): TransactionKind = TransactionKind.valueOf(value)

    @TypeConverter
    fun fromTransactionKind(value: TransactionKind): String = value.name

    @TypeConverter
    fun toTransactionSource(value: String): TransactionSource = TransactionSource.valueOf(value)

    @TypeConverter
    fun fromTransactionSource(value: TransactionSource): String = value.name
}
