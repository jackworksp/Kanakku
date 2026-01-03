package com.example.kanakku.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisting user-reported undetected bank SMS messages.
 * Stores SMS messages that users report as not being automatically detected
 * as transactions, enabling pattern improvement and debugging.
 */
@Entity(
    tableName = "unreported_sms",
    indices = [
        Index(value = ["reportedAt"]),
        Index(value = ["senderAddress"]),
        Index(value = ["status"])
    ]
)
data class UnreportedSmsEntity(
    /**
     * Unique identifier for the SMS message.
     * Uses the SMS system ID from the device.
     */
    @PrimaryKey
    val smsId: Long,

    /**
     * Sender address of the SMS (e.g., "VM-HDFCBK", "AD-SBIBNK").
     * Indexed for quick lookup by bank sender.
     */
    val senderAddress: String,

    /**
     * Full content of the SMS message.
     * Stored for pattern analysis and debugging.
     */
    val smsBody: String,

    /**
     * Original SMS timestamp in milliseconds since epoch.
     * Represents when the SMS was received on the device.
     */
    val smsDate: Long,

    /**
     * Timestamp when the user reported this SMS in milliseconds since epoch.
     * Indexed for chronological queries.
     */
    val reportedAt: Long,

    /**
     * Processing status of the reported SMS.
     * Possible values: "pending", "reviewed", "processed", "dismissed"
     */
    val status: String = "pending",

    /**
     * Optional user notes or comments about why this SMS should have been detected.
     * Can provide context for pattern improvement.
     */
    val userNotes: String? = null
)
