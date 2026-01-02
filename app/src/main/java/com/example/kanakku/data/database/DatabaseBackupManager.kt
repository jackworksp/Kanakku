package com.example.kanakku.data.database

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages database backup and restore operations for data recovery scenarios.
 *
 * This class provides functionality to:
 * - Create timestamped backups of the database
 * - Restore database from backup files
 * - List available backups
 * - Clean up old backups
 *
 * Backups are stored in the app's private files directory under "backups/"
 * and include the main database file along with WAL and SHM files if present.
 *
 * Thread-safety: All methods are designed to be called from background threads.
 * Do NOT call from the main thread as file I/O operations are blocking.
 *
 * Usage:
 * ```
 * val backupManager = DatabaseBackupManager(context)
 *
 * // Create a backup
 * val backupFile = backupManager.createBackup()
 *
 * // Restore from backup
 * backupManager.restoreFromBackup(backupFile)
 *
 * // List all backups
 * val backups = backupManager.listBackups()
 * ```
 */
class DatabaseBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "DatabaseBackupManager"
        private const val DATABASE_NAME = "kanakku_database"
        private const val BACKUP_DIR = "backups"
        private const val BACKUP_EXTENSION = ".db"
        private const val DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss"
        private const val MAX_BACKUPS = 10 // Maximum number of backups to keep
    }

    private val backupDirectory: File by lazy {
        File(context.filesDir, BACKUP_DIR).apply {
            if (!exists()) {
                Log.d(TAG, "Creating backup directory: $absolutePath")
                mkdirs()
            }
        }
    }

    /**
     * Creates a backup of the current database.
     *
     * The backup includes:
     * - Main database file
     * - WAL (Write-Ahead Logging) file if present
     * - SHM (Shared Memory) file if present
     *
     * Backups are named with timestamp: "kanakku_backup_YYYY-MM-DD_HH-MM-SS.db"
     *
     * @return The backup file if successful, null if backup failed
     * @throws DatabaseBackupException if a critical error occurs during backup
     */
    fun createBackup(): File? {
        return try {
            Log.i(TAG, "Starting database backup...")

            // Close database connection before backup
            DatabaseProvider.getDatabase(context).close()

            val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
            val backupFileName = "kanakku_backup_$timestamp$BACKUP_EXTENSION"
            val backupFile = File(backupDirectory, backupFileName)

            // Get source database files
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) {
                Log.w(TAG, "Database file does not exist. Nothing to backup.")
                return null
            }

            // Copy main database file
            var filesCopied = 0
            if (copyFile(dbFile, backupFile)) {
                Log.d(TAG, "Backed up main database: ${dbFile.name} -> ${backupFile.name}")
                filesCopied++
            } else {
                Log.e(TAG, "Failed to backup main database file")
                backupFile.delete()
                return null
            }

            // Copy WAL file if exists
            val walFile = File(dbFile.parent, "$DATABASE_NAME-wal")
            if (walFile.exists()) {
                val walBackup = File(backupDirectory, "${backupFileName}-wal")
                if (copyFile(walFile, walBackup)) {
                    Log.d(TAG, "Backed up WAL file: ${walFile.name}")
                    filesCopied++
                }
            }

            // Copy SHM file if exists
            val shmFile = File(dbFile.parent, "$DATABASE_NAME-shm")
            if (shmFile.exists()) {
                val shmBackup = File(backupDirectory, "${backupFileName}-shm")
                if (copyFile(shmFile, shmBackup)) {
                    Log.d(TAG, "Backed up SHM file: ${shmFile.name}")
                    filesCopied++
                }
            }

            Log.i(TAG, "Database backup completed successfully. Backed up $filesCopied file(s) to: ${backupFile.name}")

            // Clean up old backups
            cleanupOldBackups()

            backupFile

        } catch (e: IOException) {
            Log.e(TAG, "IOException during database backup: ${e.message}", e)
            throw DatabaseBackupException("Failed to create database backup due to I/O error", e)

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during database backup: ${e.message}", e)
            throw DatabaseBackupException("Unexpected error during database backup", e)

        } finally {
            // Reinitialize database after backup
            try {
                DatabaseProvider.resetInstance()
                DatabaseProvider.getDatabase(context)
                Log.d(TAG, "Database reinitialized after backup")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reinitialize database after backup: ${e.message}", e)
            }
        }
    }

    /**
     * Restores the database from a backup file.
     *
     * This operation:
     * 1. Closes the current database connection
     * 2. Deletes the current database files
     * 3. Copies the backup files to the database location
     * 4. Reinitializes the database
     *
     * WARNING: This will replace all current data with the backup data.
     * Ensure you have confirmed user intent before calling this method.
     *
     * @param backupFile The backup file to restore from
     * @return True if restore was successful, false otherwise
     * @throws DatabaseBackupException if a critical error occurs during restore
     * @throws IllegalArgumentException if backup file doesn't exist or is invalid
     */
    fun restoreFromBackup(backupFile: File): Boolean {
        if (!backupFile.exists()) {
            throw IllegalArgumentException("Backup file does not exist: ${backupFile.absolutePath}")
        }

        if (!backupFile.name.startsWith("kanakku_backup_")) {
            throw IllegalArgumentException("Invalid backup file: ${backupFile.name}")
        }

        return try {
            Log.i(TAG, "Starting database restore from: ${backupFile.name}")

            // Close database connection before restore
            DatabaseProvider.getDatabase(context).close()
            DatabaseProvider.resetInstance()

            // Get destination database files
            val dbFile = context.getDatabasePath(DATABASE_NAME)

            // Delete existing database files
            deleteExistingDatabaseFiles()

            // Restore main database file
            var filesRestored = 0
            if (copyFile(backupFile, dbFile)) {
                Log.d(TAG, "Restored main database: ${backupFile.name} -> ${dbFile.name}")
                filesRestored++
            } else {
                Log.e(TAG, "Failed to restore main database file")
                return false
            }

            // Restore WAL file if exists in backup
            val backupWal = File(backupDirectory, "${backupFile.name}-wal")
            if (backupWal.exists()) {
                val walFile = File(dbFile.parent, "$DATABASE_NAME-wal")
                if (copyFile(backupWal, walFile)) {
                    Log.d(TAG, "Restored WAL file: ${backupWal.name}")
                    filesRestored++
                }
            }

            // Restore SHM file if exists in backup
            val backupShm = File(backupDirectory, "${backupFile.name}-shm")
            if (backupShm.exists()) {
                val shmFile = File(dbFile.parent, "$DATABASE_NAME-shm")
                if (copyFile(backupShm, shmFile)) {
                    Log.d(TAG, "Restored SHM file: ${backupShm.name}")
                    filesRestored++
                }
            }

            Log.i(TAG, "Database restore completed successfully. Restored $filesRestored file(s)")

            // Reinitialize database with restored data
            DatabaseProvider.getDatabase(context)
            Log.i(TAG, "Database reinitialized with restored data")

            true

        } catch (e: IOException) {
            Log.e(TAG, "IOException during database restore: ${e.message}", e)
            throw DatabaseBackupException("Failed to restore database due to I/O error", e)

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during database restore: ${e.message}", e)
            throw DatabaseBackupException("Unexpected error during database restore", e)

        } finally {
            // Ensure database is reinitialized even if restore partially failed
            try {
                DatabaseProvider.resetInstance()
                DatabaseProvider.getDatabase(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reinitialize database after restore: ${e.message}", e)
            }
        }
    }

    /**
     * Lists all available backup files, sorted by creation date (newest first).
     *
     * @return List of backup files, or empty list if no backups exist
     */
    fun listBackups(): List<File> {
        return try {
            backupDirectory.listFiles { file ->
                file.isFile && file.name.startsWith("kanakku_backup_") && file.name.endsWith(BACKUP_EXTENSION)
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

        } catch (e: Exception) {
            Log.e(TAG, "Error listing backups: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Deletes a specific backup file and its associated files (WAL, SHM).
     *
     * @param backupFile The backup file to delete
     * @return True if deletion was successful, false otherwise
     */
    fun deleteBackup(backupFile: File): Boolean {
        return try {
            Log.d(TAG, "Deleting backup: ${backupFile.name}")

            var filesDeleted = 0

            // Delete main backup file
            if (backupFile.exists() && backupFile.delete()) {
                filesDeleted++
            }

            // Delete associated WAL file
            val walFile = File(backupDirectory, "${backupFile.name}-wal")
            if (walFile.exists() && walFile.delete()) {
                filesDeleted++
            }

            // Delete associated SHM file
            val shmFile = File(backupDirectory, "${backupFile.name}-shm")
            if (shmFile.exists() && shmFile.delete()) {
                filesDeleted++
            }

            Log.i(TAG, "Deleted backup and associated files. Deleted $filesDeleted file(s)")
            filesDeleted > 0

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backup: ${e.message}", e)
            false
        }
    }

    /**
     * Gets the size of a backup in bytes, including associated files.
     *
     * @param backupFile The backup file to check
     * @return Total size in bytes
     */
    fun getBackupSize(backupFile: File): Long {
        var totalSize = 0L

        if (backupFile.exists()) {
            totalSize += backupFile.length()
        }

        val walFile = File(backupDirectory, "${backupFile.name}-wal")
        if (walFile.exists()) {
            totalSize += walFile.length()
        }

        val shmFile = File(backupDirectory, "${backupFile.name}-shm")
        if (shmFile.exists()) {
            totalSize += shmFile.length()
        }

        return totalSize
    }

    /**
     * Gets the total size of all backups in bytes.
     *
     * @return Total size of all backups in bytes
     */
    fun getTotalBackupSize(): Long {
        return listBackups().sumOf { getBackupSize(it) }
    }

    /**
     * Creates an automatic backup after significant changes to the database.
     *
     * This method should be called after:
     * - Large batch inserts (e.g., SMS sync with many transactions)
     * - Data migrations or schema updates
     * - User-initiated bulk operations
     * - Critical data modifications
     *
     * The method includes rate limiting to prevent excessive backups:
     * - Only creates backup if last backup was created more than 1 hour ago
     * - This prevents creating backups for every small change
     *
     * @param changeDescription Description of the change that triggered the backup (for logging)
     * @return The backup file if created, null if backup was skipped or failed
     */
    fun createAutomaticBackup(changeDescription: String): File? {
        return try {
            Log.i(TAG, "Automatic backup requested due to: $changeDescription")

            // Get last backup timestamp to implement rate limiting
            val lastBackup = listBackups().firstOrNull()
            if (lastBackup != null) {
                val timeSinceLastBackup = System.currentTimeMillis() - lastBackup.lastModified()
                val oneHourInMillis = 60 * 60 * 1000

                if (timeSinceLastBackup < oneHourInMillis) {
                    Log.d(TAG, "Skipping automatic backup - last backup was ${timeSinceLastBackup / 1000 / 60} minutes ago")
                    return null
                }
            }

            Log.i(TAG, "Creating automatic backup for: $changeDescription")
            createBackup()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create automatic backup: ${e.message}", e)
            // Don't throw - automatic backup failure shouldn't crash the app
            null
        }
    }

    /**
     * Cleans up old backups, keeping only the most recent MAX_BACKUPS backups.
     *
     * @return Number of backups deleted
     */
    private fun cleanupOldBackups(): Int {
        try {
            val backups = listBackups()
            if (backups.size <= MAX_BACKUPS) {
                Log.d(TAG, "No cleanup needed. Current backups: ${backups.size}, max: $MAX_BACKUPS")
                return 0
            }

            val backupsToDelete = backups.drop(MAX_BACKUPS)
            var deleted = 0

            backupsToDelete.forEach { backup ->
                if (deleteBackup(backup)) {
                    deleted++
                }
            }

            Log.i(TAG, "Cleaned up $deleted old backup(s). Remaining: ${backups.size - deleted}")
            return deleted

        } catch (e: Exception) {
            Log.e(TAG, "Error during backup cleanup: ${e.message}", e)
            return 0
        }
    }

    /**
     * Deletes all existing database files (main DB, WAL, SHM).
     * Used before restoring from backup.
     */
    private fun deleteExistingDatabaseFiles() {
        try {
            Log.d(TAG, "Deleting existing database files before restore...")

            val dbFile = context.getDatabasePath(DATABASE_NAME)
            var filesDeleted = 0

            if (dbFile.exists() && dbFile.delete()) {
                Log.d(TAG, "Deleted existing database: ${dbFile.name}")
                filesDeleted++
            }

            val walFile = File(dbFile.parent, "$DATABASE_NAME-wal")
            if (walFile.exists() && walFile.delete()) {
                Log.d(TAG, "Deleted existing WAL file: ${walFile.name}")
                filesDeleted++
            }

            val shmFile = File(dbFile.parent, "$DATABASE_NAME-shm")
            if (shmFile.exists() && shmFile.delete()) {
                Log.d(TAG, "Deleted existing SHM file: ${shmFile.name}")
                filesDeleted++
            }

            Log.i(TAG, "Deleted $filesDeleted existing database file(s)")

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting existing database files: ${e.message}", e)
            throw DatabaseBackupException("Failed to delete existing database files", e)
        }
    }

    /**
     * Copies a file from source to destination using buffered streams.
     *
     * @param source The source file
     * @param destination The destination file
     * @return True if copy was successful, false otherwise
     */
    private fun copyFile(source: File, destination: File): Boolean {
        return try {
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }
            true

        } catch (e: IOException) {
            Log.e(TAG, "Error copying file: ${source.name} -> ${destination.name}: ${e.message}", e)
            false
        }
    }
}

/**
 * Exception thrown when database backup or restore operations fail critically.
 *
 * This exception indicates a critical error during backup/restore operations.
 * It should be caught by the UI layer to inform the user about the issue.
 *
 * Common causes:
 * - Insufficient disk space
 * - File permission issues
 * - I/O errors during file operations
 * - Database file corruption
 *
 * Recovery steps:
 * 1. Inform user of the issue
 * 2. Suggest freeing up disk space
 * 3. Retry the operation
 * 4. Log details for debugging
 *
 * @param message User-friendly error message
 * @param cause The underlying cause of the backup/restore failure
 */
class DatabaseBackupException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
