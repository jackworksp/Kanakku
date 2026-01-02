package com.example.kanakku.data.backup

import com.example.kanakku.data.model.BackupData
import com.example.kanakku.data.model.BackupMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Implementation of BackupRepository for Google Drive storage.
 *
 * This repository handles backup/restore operations where backups are stored in
 * the user's Google Drive app folder. Files are encrypted using EncryptionService
 * and stored as JSON with AES-256-GCM encryption.
 *
 * Key features:
 * - Backups stored in Google Drive appDataFolder (private to the app)
 * - Encrypted backup files with .kbak extension
 * - JSON serialization for data portability
 * - Password-protected backups
 * - List and delete operations supported (unlike local storage)
 *
 * Note: Requires Google Sign-In with Drive scope. Use GoogleDriveService
 * to handle authentication before using this repository.
 */
class DriveBackupRepository(
    private val driveService: GoogleDriveService,
    private val encryptionService: EncryptionService
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Create an encrypted backup and upload it to Google Drive.
     *
     * Process:
     * 1. Serialize BackupData to JSON
     * 2. Encrypt JSON bytes with user password
     * 3. Upload encrypted data to Drive app folder
     *
     * @param data The backup data to save
     * @param password Password for encrypting the backup
     * @param outputStream Not used for Drive storage (optional)
     * @return Result with BackupMetadata on success
     * @throws BackupException if backup creation fails
     * @throws DriveAuthException if not signed in to Drive
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

            // Check if Drive service is ready
            if (!driveService.isDriveServiceReady()) {
                return@withContext Result.failure(
                    BackupException("Not signed in to Google Drive. Sign in first.")
                )
            }

            // Serialize data to JSON
            val jsonString = json.encodeToString(data)
            val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

            // Encrypt the JSON data
            val encryptedData = encryptionService.encrypt(jsonBytes, password)

            // Generate filename with timestamp
            val fileName = "backup_${data.metadata.timestamp}${GoogleDriveService.BACKUP_FILE_EXTENSION}"

            // Upload to Drive
            try {
                driveService.uploadFile(fileName, encryptedData.toByteArray())
            } catch (e: DriveException) {
                return@withContext Result.failure(
                    BackupException("Failed to upload backup to Drive", e)
                )
            }

            Result.success(data.metadata)
        } catch (e: EncryptionException) {
            Result.failure(BackupException("Failed to encrypt backup", e))
        } catch (e: Exception) {
            when (e) {
                is BackupException -> Result.failure(e)
                else -> Result.failure(BackupException("Failed to create backup", e))
            }
        }
    }

    /**
     * Restore data from an encrypted backup file.
     *
     * For Drive storage, the input stream should contain the encrypted backup data
     * downloaded from Drive. Alternatively, use restoreBackupById() to restore
     * directly from a Drive file ID.
     *
     * Process:
     * 1. Read encrypted data from input stream
     * 2. Decrypt data with user password
     * 3. Deserialize JSON to BackupData
     *
     * @param inputStream Input stream containing encrypted backup data
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
     * Restore data directly from a Drive file by ID.
     *
     * This is a convenience method for Drive storage that downloads and restores
     * in a single operation.
     *
     * @param fileId Drive file ID to restore from
     * @param password Password for decrypting the backup
     * @return Result with BackupData on success
     * @throws RestoreException if restore fails
     * @throws DriveAuthException if not signed in to Drive
     */
    suspend fun restoreBackupById(
        fileId: String,
        password: String
    ): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            // Check if Drive service is ready
            if (!driveService.isDriveServiceReady()) {
                return@withContext Result.failure(
                    RestoreException("Not signed in to Google Drive. Sign in first.")
                )
            }

            // Download file from Drive
            val encryptedBytes = try {
                driveService.downloadFile(fileId)
            } catch (e: DriveException) {
                return@withContext Result.failure(
                    RestoreException("Failed to download backup from Drive", e)
                )
            }

            // Restore from downloaded data
            val inputStream = ByteArrayInputStream(encryptedBytes)
            restoreBackup(inputStream, password)
        } catch (e: Exception) {
            when (e) {
                is RestoreException -> Result.failure(e)
                else -> Result.failure(RestoreException("Failed to restore backup", e))
            }
        }
    }

    /**
     * List all available backups in Google Drive app folder.
     *
     * @return Result with list of BackupMetadata for all backups
     * @throws DriveException if listing fails
     * @throws DriveAuthException if not signed in to Drive
     */
    override suspend fun listBackups(): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        try {
            // Check if Drive service is ready
            if (!driveService.isDriveServiceReady()) {
                return@withContext Result.failure(
                    DriveAuthException("Not signed in to Google Drive. Sign in first.")
                )
            }

            // List backup files from Drive
            val files = try {
                driveService.listBackupFiles()
            } catch (e: DriveException) {
                return@withContext Result.failure(e)
            }

            // Note: Without password, we can't decrypt metadata from files
            // This returns basic info from Drive file metadata
            // For full metadata, use getBackupMetadata() with password
            val metadataList = files.map { file ->
                BackupMetadata(
                    version = 1, // Default version
                    timestamp = file.modifiedTime,
                    deviceName = null,
                    appVersion = "unknown",
                    transactionCount = 0,
                    categoryOverrideCount = 0
                )
            }

            Result.success(metadataList)
        } catch (e: Exception) {
            when (e) {
                is DriveAuthException, is DriveException -> Result.failure(e)
                else -> Result.failure(DriveException("Failed to list backups", e))
            }
        }
    }

    /**
     * Get list of Drive file info for all backups.
     * This returns file metadata from Drive without decrypting backup content.
     *
     * @return Result with list of DriveFileInfo
     * @throws DriveException if listing fails
     * @throws DriveAuthException if not signed in to Drive
     */
    suspend fun listBackupFiles(): Result<List<DriveFileInfo>> = withContext(Dispatchers.IO) {
        try {
            // Check if Drive service is ready
            if (!driveService.isDriveServiceReady()) {
                return@withContext Result.failure(
                    DriveAuthException("Not signed in to Google Drive. Sign in first.")
                )
            }

            // List backup files from Drive
            val files = driveService.listBackupFiles()
            Result.success(files)
        } catch (e: DriveException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(DriveException("Failed to list backup files", e))
        }
    }

    /**
     * Delete a specific backup from Google Drive.
     *
     * @param backupId Drive file ID of the backup to delete
     * @return Result indicating success or failure
     * @throws DriveException if deletion fails
     * @throws DriveAuthException if not signed in to Drive
     */
    override suspend fun deleteBackup(backupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check if Drive service is ready
            if (!driveService.isDriveServiceReady()) {
                return@withContext Result.failure(
                    DriveAuthException("Not signed in to Google Drive. Sign in first.")
                )
            }

            // Delete file from Drive
            driveService.deleteFile(backupId)
            Result.success(Unit)
        } catch (e: DriveException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(DriveException("Failed to delete backup", e))
        }
    }

    /**
     * Validate that a password can decrypt a backup without fully restoring it.
     *
     * Performs a quick validation by attempting to decrypt the data and checking
     * if it produces valid JSON structure.
     *
     * @param inputStream Input stream containing encrypted backup data
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
     * Validate password for a Drive backup by file ID.
     *
     * @param fileId Drive file ID to validate password against
     * @param password Password to validate
     * @return Result with true if password is correct, false otherwise
     */
    suspend fun validateBackupPasswordById(
        fileId: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Check if Drive service is ready
            if (!driveService.isDriveServiceReady()) {
                return@withContext Result.failure(
                    DriveAuthException("Not signed in to Google Drive. Sign in first.")
                )
            }

            // Download file from Drive
            val encryptedBytes = try {
                driveService.downloadFile(fileId)
            } catch (e: DriveException) {
                return@withContext Result.failure(e)
            }

            // Validate using downloaded data
            val inputStream = ByteArrayInputStream(encryptedBytes)
            validateBackupPassword(inputStream, password)
        } catch (e: Exception) {
            when (e) {
                is DriveAuthException, is DriveException -> Result.failure(e)
                else -> Result.failure(DriveException("Failed to validate password", e))
            }
        }
    }

    /**
     * Get metadata from a backup file without decrypting the full content.
     *
     * This reads and decrypts the backup to extract only the metadata portion.
     * Note: Due to encryption, the entire file must be decrypted, but we only
     * parse the metadata field from the JSON.
     *
     * @param inputStream Input stream containing encrypted backup data
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

    /**
     * Get metadata from a Drive backup by file ID.
     *
     * @param fileId Drive file ID to get metadata from
     * @param password Password to decrypt metadata
     * @return Result with BackupMetadata on success
     */
    suspend fun getBackupMetadataById(
        fileId: String,
        password: String
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            // Check if Drive service is ready
            if (!driveService.isDriveServiceReady()) {
                return@withContext Result.failure(
                    InvalidBackupException("Not signed in to Google Drive. Sign in first.")
                )
            }

            // Download file from Drive
            val encryptedBytes = try {
                driveService.downloadFile(fileId)
            } catch (e: DriveException) {
                return@withContext Result.failure(
                    InvalidBackupException("Failed to download backup from Drive", e)
                )
            }

            // Get metadata from downloaded data
            val inputStream = ByteArrayInputStream(encryptedBytes)
            getBackupMetadata(inputStream, password)
        } catch (e: Exception) {
            when (e) {
                is InvalidBackupException -> Result.failure(e)
                else -> Result.failure(InvalidBackupException("Failed to get backup metadata", e))
            }
        }
    }
}
