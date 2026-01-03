package com.example.kanakku.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.kanakku.data.model.RecurringFrequency
import com.example.kanakku.data.model.RecurringType

/**
 * Room entity for persisting recurring transaction patterns.
 * Stores detected patterns for subscriptions, EMIs, salaries, rent, and other recurring payments.
 */
@Entity(
    tableName = "recurring_transactions",
    indices = [
        Index(value = ["merchantPattern"]),
        Index(value = ["nextExpected"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey
    val id: String,
    val merchantPattern: String,
    val amount: Double,
    val frequency: RecurringFrequency,
    val averageInterval: Int,
    val lastOccurrence: Long,
    val nextExpected: Long,
    val transactionIds: String,              // Comma-separated list of SMS IDs
    val isUserConfirmed: Boolean,
    val type: RecurringType
)
