package com.example.kanakku.data.backup

import android.os.Build
import com.example.kanakku.BuildConfig
import com.example.kanakku.data.model.BackupData
import com.example.kanakku.data.model.BackupMetadata
import com.example.kanakku.data.model.CategoryOverride
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.SerializableCategory
import com.example.kanakku.data.model.SerializableTransaction

/**
 * Service responsible for serializing app data into BackupData format.
 *
 * This class handles the conversion of runtime app data (transactions, category overrides)
 * into a serializable format suitable for backup. It creates appropriate metadata and
 * packages all data into a BackupData object.
 *
 * Key responsibilities:
 * - Convert ParsedTransaction list to SerializableTransaction list
 * - Convert category override map to CategoryOverride list
 * - Generate backup metadata with version and statistics
 * - Package all data into BackupData for encryption and storage
 *
 * Usage:
 * ```
 * val serializer = BackupSerializer()
 * val backupData = serializer.createBackupData(
 *     transactions = transactionList,
 *     categoryOverrides = categoryOverrideMap
 * )
 * ```
 */
class BackupSerializer {

    companion object {
        /**
         * Current backup format version.
         * Increment this when making breaking changes to BackupData structure.
         * This enables migration logic when restoring from older backup versions.
         */
        const val BACKUP_VERSION = 1
    }

    /**
     * Create a BackupData object from application data.
     *
     * This method collects all app data that needs to be backed up, converts it to
     * serializable format, generates metadata, and packages everything into a BackupData
     * object ready for encryption and storage.
     *
     * @param transactions List of all parsed transactions to backup
     * @param categoryOverrides Map of manual category overrides (smsId -> categoryId)
     * @param customCategories List of custom user-created categories (future use, defaults to empty)
     * @param deviceName Optional device name for backup metadata (defaults to Android device model)
     * @return BackupData object containing all app data and metadata
     */
    fun createBackupData(
        transactions: List<ParsedTransaction>,
        categoryOverrides: Map<Long, String>,
        customCategories: List<SerializableCategory> = emptyList(),
        deviceName: String? = null
    ): BackupData {
        // Convert transactions to serializable format
        val serializableTransactions = transactions.map { transaction ->
            SerializableTransaction.from(transaction)
        }

        // Convert category overrides map to list
        val categoryOverrideList = categoryOverrides.map { (smsId, categoryId) ->
            CategoryOverride(
                smsId = smsId,
                categoryId = categoryId
            )
        }

        // Generate metadata
        val metadata = createMetadata(
            transactionCount = serializableTransactions.size,
            categoryOverrideCount = categoryOverrideList.size,
            deviceName = deviceName ?: getDefaultDeviceName()
        )

        // Package all data
        return BackupData(
            metadata = metadata,
            transactions = serializableTransactions,
            categoryOverrides = categoryOverrideList,
            customCategories = customCategories
        )
    }

    /**
     * Create backup metadata with version info and statistics.
     *
     * @param transactionCount Number of transactions in the backup
     * @param categoryOverrideCount Number of manual category overrides
     * @param deviceName Device name for reference
     * @return BackupMetadata object with current timestamp and version info
     */
    private fun createMetadata(
        transactionCount: Int,
        categoryOverrideCount: Int,
        deviceName: String
    ): BackupMetadata {
        return BackupMetadata(
            version = BACKUP_VERSION,
            timestamp = System.currentTimeMillis(),
            deviceName = deviceName,
            appVersion = BuildConfig.VERSION_NAME,
            transactionCount = transactionCount,
            categoryOverrideCount = categoryOverrideCount
        )
    }

    /**
     * Get default device name from Android system properties.
     * Falls back to "Unknown Device" if device info is unavailable.
     */
    private fun getDefaultDeviceName(): String {
        return try {
            val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: ""
            val model = Build.MODEL ?: ""
            if (manufacturer.isNotBlank() && model.isNotBlank()) {
                "$manufacturer $model"
            } else {
                "Unknown Device"
            }
        } catch (e: Exception) {
            "Unknown Device"
        }
    }

    /**
     * Validate backup data before serialization.
     * Checks for common issues that might cause backup to fail.
     *
     * @param data BackupData to validate
     * @return null if valid, error message if invalid
     */
    fun validateBackupData(data: BackupData): String? {
        return when {
            data.metadata.version <= 0 -> "Invalid backup version"
            data.metadata.timestamp <= 0 -> "Invalid backup timestamp"
            data.metadata.transactionCount != data.transactions.size ->
                "Transaction count mismatch: expected ${data.metadata.transactionCount}, got ${data.transactions.size}"
            data.metadata.categoryOverrideCount != data.categoryOverrides.size ->
                "Category override count mismatch: expected ${data.metadata.categoryOverrideCount}, got ${data.categoryOverrides.size}"
            data.metadata.appVersion.isBlank() -> "App version cannot be empty"
            else -> null
        }
    }

    /**
     * Extract statistics from backup data for preview/summary purposes.
     *
     * @param data BackupData to analyze
     * @return BackupStatistics with summary information
     */
    fun getBackupStatistics(data: BackupData): BackupStatistics {
        val debitCount = data.transactions.count { it.type == "DEBIT" }
        val creditCount = data.transactions.count { it.type == "CREDIT" }
        val totalAmount = data.transactions
            .filter { it.type == "DEBIT" }
            .sumOf { it.amount }

        return BackupStatistics(
            totalTransactions = data.transactions.size,
            debitTransactions = debitCount,
            creditTransactions = creditCount,
            totalDebitAmount = totalAmount,
            categoryOverrides = data.categoryOverrides.size,
            customCategories = data.customCategories.size,
            backupDate = data.metadata.timestamp,
            backupVersion = data.metadata.version
        )
    }
}

/**
 * Statistics extracted from backup data for display or validation purposes
 */
data class BackupStatistics(
    val totalTransactions: Int,
    val debitTransactions: Int,
    val creditTransactions: Int,
    val totalDebitAmount: Double,
    val categoryOverrides: Int,
    val customCategories: Int,
    val backupDate: Long,
    val backupVersion: Int
)
