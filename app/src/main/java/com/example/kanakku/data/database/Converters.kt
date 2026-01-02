package com.example.kanakku.data.database

import androidx.room.TypeConverter
import com.example.kanakku.data.model.TransactionType

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
}
