package com.example.kanakku.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing sync metadata in key-value format.
 * Used to track synchronization state like last processed SMS timestamp.
 *
 * Common keys:
 * - "lastSyncTimestamp": Timestamp of the last SMS sync operation
 * - "lastProcessedSmsId": ID of the last processed SMS message
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
