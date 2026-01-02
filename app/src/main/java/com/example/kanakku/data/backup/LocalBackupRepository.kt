package com.example.kanakku.data.backup

import com.example.kanakku.data.model.BackupData
import com.example.kanakku.data.model.BackupMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * Implementation of BackupRepository for local device storage using Storage Access Framework (SAF).
 *
 * This repository handles backup/restore operations where the user selects file locations
 * via the system file picker. Files are encrypted using EncryptionService and stored as
 * JSON with AES-256-GCM encryption.
 *
 * Key features:
 * - User chooses backup location via SAF file picker
 * - Encrypted backup files with .kbak extension
 * - JSON serialization for data portability
 * - Password-protected backups
 *
 * Note: listBackups() and deleteBackup() are not implemented for local storage
 * since SAF doesn't provide persistent access to user-chosen locations.
 * Users manage backup files through the system file manager.
 */
class LocalBackupRepository(
    private val encryptionService: EncryptionService
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Create an encrypted backup and write it to the provided output stream.
     *
     * Process:
     * 1. Serialize BackupData to JSON
     * 2. Encrypt JSON bytes with user password
     * 3. Write encrypted data to output stream
     *
     * @param data The backup data to save
     * @param password Password for encrypting the backup
     * @param outputStream Output stream from SAF document (required for local storage)
     * @return Result with BackupMetadata on success
     * @throws BackupException if backup creation fails
     */
    override suspend fun createBackup(
        data: BackupData,
        password: String,
        outputStream: OutputStream?
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            // Validate password
            encryptionService.validatePassword(password)?.let { error ->
                return@withContext Result.failure(BackupException(error))
            }

            // Require output stream for local storage
            if (outputStream == null) {
                return@withContext Result.failure(
                    BackupException("Output stream is required for local backup")
                )
            }

            // Serialize data to JSON
            val jsonString = json.encodeToString(data)
            val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

            // Encrypt the JSON data
            val encryptedData = encryptionService.encrypt(jsonBytes, password)

            // Write encrypted data to output stream
            outputStream.use { stream ->
                stream.write(encryptedData.toByteArray())
                stream.flush()
            }

            Result.success(data.metadata)
        } catch (e: EncryptionException) {
            Result.failure(BackupException("Failed to encrypt backup", e))
        } catch (e: Exception) {
            Result.failure(BackupException("Failed to create backup", e))
        }
    }

    /**
     * Restore data from an encrypted backup file.
     *
     * Process:
     * 1. Read encrypted data from input stream
     * 2. Decrypt data with user password
     * 3. Deserialize JSON to BackupData
     *
     * @param inputStream Input stream from SAF document
     * @param password Password for decrypting the backup
     * @return Result with BackupData on success
     * @throws RestoreException if restore fails
     * @throws InvalidBackupException if backup file is corrupted
     */
    override suspend fun restoreBackup(
        inputStream: InputStream,
        password: String
    ): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            // Validate password
            encryptionService.validatePassword(password)?.let { error ->
                return@withContext Result.failure(RestoreException(error))
            }

            // Read encrypted data from input stream
            val encryptedBytes = inputStream.use { it.readBytes() }

            // Deserialize encrypted data structure
            val encryptedData = try {
                EncryptionService.EncryptedData.fromByteArray(encryptedBytes)
            } catch (e: Exception) {
                return@withContext Result.failure(
                    InvalidBackupException("Invalid backup file format", e)
                )
            }

            // Decrypt the data
            val decryptedBytes = try {
                encryptionService.decrypt(encryptedData, password)
            } catch (e: DecryptionException) {
                return@withContext Result.failure(
                    RestoreException("Failed to decrypt backup - incorrect password or corrupted file", e)
                )
            }

            // Deserialize JSON to BackupData
            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            val backupData = try {
                json.decodeFromString<BackupData>(jsonString)
            } catch (e: Exception) {
                return@withContext Result.failure(
                    InvalidBackupException("Invalid backup data format", e)
                )
            }

            Result.success(backupData)
        } catch (e: Exception) {
            when (e) {
                is RestoreException, is InvalidBackupException -> Result.failure(e)
                else -> Result.failure(RestoreException("Failed to restore backup", e))
            }
        }
    }

    /**
     * List all available backups.
     *
     * Not implemented for local storage as SAF doesn't provide persistent access
     * to user-chosen locations. Users manage backup files through system file manager.
     *
     * @return Result with empty list (not supported for local storage)
     */
    override suspend fun listBackups(): Result<List<BackupMetadata>> {
        return Result.success(emptyList())
    }

    /**
     * Delete a specific backup.
     *
     * Not implemented for local storage as SAF doesn't provide persistent access
     * to user-chosen locations. Users delete files through system file manager.
     *
     * @param backupId Identifier for the backup to delete
     * @return Result indicating operation not supported
     */
    override suspend fun deleteBackup(backupId: String): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException(
                "Delete operation not supported for local storage. " +
                "Use system file manager to delete backup files."
            )
        )
    }

    /**
     * Validate that a password can decrypt a backup without fully restoring it.
     *
     * Performs a quick validation by attempting to decrypt the data and checking
     * if it produces valid JSON structure.
     *
     * @param inputStream Input stream to read the encrypted backup from
     * @param password Password to validate
     * @return Result with true if password is correct, false otherwise
     */
    override suspend fun validateBackupPassword(
        inputStream: InputStream,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Validate password format
            encryptionService.validatePassword(password)?.let {
                return@withContext Result.success(false)
            }

            // Read encrypted data
            val encryptedBytes = inputStream.use { it.readBytes() }

            // Try to deserialize and decrypt
            val encryptedData = try {
                EncryptionService.EncryptedData.fromByteArray(encryptedBytes)
            } catch (e: Exception) {
                return@withContext Result.success(false)
            }

            // Attempt decryption
            val isValid = try {
                encryptionService.decrypt(encryptedData, password)
                true
            } catch (e: DecryptionException) {
                false
            }

            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get metadata from a backup file without decrypting the full content.
     *
     * This reads and decrypts the backup to extract only the metadata portion.
     * Note: Due to encryption, the entire file must be decrypted, but we only
     * parse the metadata field from the JSON.
     *
     * @param inputStream Input stream to read the backup from
     * @param password Password to decrypt metadata
     * @return Result with BackupMetadata on success
     */
    override suspend fun getBackupMetadata(
        inputStream: InputStream,
        password: String
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            // Validate password
            encryptionService.validatePassword(password)?.let { error ->
                return@withContext Result.failure(InvalidBackupException(error))
            }

            // Read encrypted data
            val encryptedBytes = inputStream.use { it.readBytes() }

            // Deserialize encrypted data structure
            val encryptedData = try {
                EncryptionService.EncryptedData.fromByteArray(encryptedBytes)
            } catch (e: Exception) {
                return@withContext Result.failure(
                    InvalidBackupException("Invalid backup file format", e)
                )
            }

            // Decrypt the data
            val decryptedBytes = try {
                encryptionService.decrypt(encryptedData, password)
            } catch (e: DecryptionException) {
                return@withContext Result.failure(
                    InvalidBackupException("Failed to decrypt backup - incorrect password", e)
                )
            }

            // Parse JSON to extract metadata only
            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            val backupData = try {
                json.decodeFromString<BackupData>(jsonString)
            } catch (e: Exception) {
                return@withContext Result.failure(
                    InvalidBackupException("Invalid backup data format", e)
                )
            }

            Result.success(backupData.metadata)
        } catch (e: Exception) {
            when (e) {
                is InvalidBackupException -> Result.failure(e)
                else -> Result.failure(InvalidBackupException("Failed to read backup metadata", e))
            }
        }
    }
}
