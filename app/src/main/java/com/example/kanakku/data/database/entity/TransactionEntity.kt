package com.example.kanakku.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.kanakku.data.model.TransactionType

/**
 * Room entity for persisting parsed transactions.
 * Mirrors ParsedTransaction domain model for database storage.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val smsId: Long,
    val amount: Double,
    val type: TransactionType,
    val merchant: String?,
    val accountNumber: String?,
    val referenceNumber: String?,
    val date: Long,
    val rawSms: String,
    val senderAddress: String,
    val balanceAfter: Double?,
    val location: String?,
    val upiId: String?,         // UPI VPA (Virtual Payment Address) like user@paytm
    val paymentMethod: String?  // Payment method (e.g., "UPI", "Card", "Net Banking")
)
