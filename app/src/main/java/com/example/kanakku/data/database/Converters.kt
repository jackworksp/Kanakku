package com.example.kanakku.data.database

import androidx.room.TypeConverter
import com.example.kanakku.data.model.RecurringFrequency
import com.example.kanakku.data.model.RecurringType
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

    /**
     * Converts RecurringFrequency enum to String for database storage.
     * @param frequency The RecurringFrequency enum value
     * @return String representation of the recurring frequency
     */
    @TypeConverter
    fun fromRecurringFrequency(frequency: RecurringFrequency): String {
        return frequency.name
    }

    /**
     * Converts String to RecurringFrequency enum from database.
     * @param value The stored string value
     * @return RecurringFrequency enum, defaults to MONTHLY if invalid
     */
    @TypeConverter
    fun toRecurringFrequency(value: String): RecurringFrequency {
        return try {
            RecurringFrequency.valueOf(value)
        } catch (e: IllegalArgumentException) {
            RecurringFrequency.MONTHLY
        }
    }

    /**
     * Converts RecurringType enum to String for database storage.
     * @param type The RecurringType enum value
     * @return String representation of the recurring type
     */
    @TypeConverter
    fun fromRecurringType(type: RecurringType): String {
        return type.name
    }

    /**
     * Converts String to RecurringType enum from database.
     * @param value The stored string value
     * @return RecurringType enum, defaults to OTHER if invalid
     */
    @TypeConverter
    fun toRecurringType(value: String): RecurringType {
        return try {
            RecurringType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            RecurringType.OTHER
        }
    }

    /**
     * Converts List<Long> to comma-separated String for database storage.
     * @param list The list of Long values
     * @return Comma-separated string representation
     */
    @TypeConverter
    fun fromLongList(list: List<Long>): String {
        return list.joinToString(",")
    }

    /**
     * Converts comma-separated String to List<Long> from database.
     * @param value The stored comma-separated string
     * @return List of Long values, empty list if invalid or empty
     */
    @TypeConverter
    fun toLongList(value: String): List<Long> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            try {
                value.split(",").map { it.toLong() }
            } catch (e: NumberFormatException) {
                emptyList()
            }
        }
    }
}
