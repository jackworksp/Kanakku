package com.example.kanakku.data.backup

import com.example.kanakku.data.model.BackupData
import com.example.kanakku.data.model.BackupMetadata
import java.io.InputStream
import java.io.OutputStream

/**
 * Repository interface for backup and restore operations.
 *
 * Implementations handle different storage destinations (local device, Google Drive)
 * while providing a unified API for backup operations. All operations use encryption
 * via EncryptionService for data security.
 *
 * Implementations:
 * - LocalBackupRepository: Stores backups on device storage using SAF
 * - DriveBackupRepository: Stores backups in Google Drive app folder
 */
interface BackupRepository {

    /**
     * Create a backup of the provided data and save it to storage.
     * The backup will be encrypted using the provided password.
     *
     * @param data The backup data to save
     * @param password Password for encrypting the backup
     * @param outputStream Output stream to write the encrypted backup (for local storage)
     * @return Result with BackupMetadata on success, error on failure
     */
    suspend fun createBackup(
        data: BackupData,
        password: String,
        outputStream: OutputStream? = null
    ): Result<BackupMetadata>

    /**
     * Restore data from a backup file.
     * The backup will be decrypted using the provided password.
     *
     * @param inputStream Input stream to read the encrypted backup from
     * @param password Password for decrypting the backup
     * @return Result with BackupData on success, error on failure
     */
    suspend fun restoreBackup(
        inputStream: InputStream,
        password: String
    ): Result<BackupData>

    /**
     * List all available backups.
     *
     * For local storage: Returns backups the user has access to
     * For Google Drive: Returns backups in the app folder
     *
     * @return Result with list of BackupMetadata, empty list if none found
     */
    suspend fun listBackups(): Result<List<BackupMetadata>>

    /**
     * Delete a specific backup.
     *
     * @param backupId Identifier for the backup to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteBackup(backupId: String): Result<Unit>

    /**
     * Validate that a password can decrypt a backup without fully restoring it.
     * Useful for password verification before attempting full restore.
     *
     * @param inputStream Input stream to read the encrypted backup from
     * @param password Password to validate
     * @return Result with true if password is correct, false otherwise
     */
    suspend fun validateBackupPassword(
        inputStream: InputStream,
        password: String
    ): Result<Boolean>

    /**
     * Get metadata from a backup file without decrypting the full content.
     * This reads only the metadata portion for display purposes.
     *
     * @param inputStream Input stream to read the backup from
     * @param password Password to decrypt metadata
     * @return Result with BackupMetadata on success
     */
    suspend fun getBackupMetadata(
        inputStream: InputStream,
        password: String
    ): Result<BackupMetadata>
}

/**
 * Exception thrown when backup operation fails
 */
class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when restore operation fails
 */
class RestoreException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when backup file is invalid or corrupted
 */
class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
