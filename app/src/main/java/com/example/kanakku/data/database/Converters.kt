package com.example.kanakku.data.database

import androidx.room.TypeConverter
import com.example.kanakku.data.model.BudgetPeriod
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.model.TransactionSource

/**
 * Type converters for Room database to handle complex types.
 * These converters allow Room to persist enums and other non-primitive types.
 */
class Converters {

    /**
     * Converts TransactionType enum to String for database storage.
     * @param type The TransactionType enum value
     * @return String representation of the transaction type
     */
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }

    /**
     * Converts String to TransactionType enum from database.
     * @param value The stored string value
     * @return TransactionType enum, defaults to UNKNOWN if invalid
     */
    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            TransactionType.UNKNOWN
        }
    }

    /**
     * Converts TransactionSource enum to String for database storage.
     * @param source The TransactionSource enum value
     * @return String representation of the transaction source
     */
    @TypeConverter
    fun fromTransactionSource(source: TransactionSource): String {
        return source.name
    }

    /**
     * Converts String to TransactionSource enum from database.
     * @param value The stored string value
     * @return TransactionSource enum, defaults to SMS if invalid
     */
    @TypeConverter
    fun toTransactionSource(value: String): TransactionSource {
        return try {
            TransactionSource.valueOf(value)
        } catch (e: IllegalArgumentException) {
            TransactionSource.SMS
        }
    }
}
