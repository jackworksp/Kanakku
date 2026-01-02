package com.example.kanakku.ui.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanakku.data.backup.BackupException
import com.example.kanakku.data.backup.BackupRepository
import com.example.kanakku.data.backup.BackupSerializer
import com.example.kanakku.data.backup.DriveBackupRepository
import com.example.kanakku.data.backup.EncryptionService
import com.example.kanakku.data.backup.GoogleDriveService
import com.example.kanakku.data.backup.InvalidBackupException
import com.example.kanakku.data.backup.LocalBackupRepository
import com.example.kanakku.data.backup.RestoreException
import com.example.kanakku.data.category.CategoryManager
import com.example.kanakku.data.model.BackupData
import com.example.kanakku.data.model.BackupMetadata
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

/**
 * UI state for backup/restore operations
 */
data class BackupUiState(
    val isLoading: Boolean = false,
    val operationType: OperationType = OperationType.IDLE,
    val progress: String = "",
    val backupType: BackupType = BackupType.LOCAL,
    val password: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val lastBackupMetadata: BackupMetadata? = null,
    val availableBackups: List<BackupMetadata> = emptyList(),
    val isDriveSignedIn: Boolean = false
)

/**
 * Type of backup operation
 */
enum class OperationType {
    IDLE,
    CREATING_BACKUP,
    RESTORING_BACKUP,
    VALIDATING_PASSWORD,
    LOADING_BACKUPS,
    DELETING_BACKUP
}

/**
 * Backup storage destination
 */
enum class BackupType {
    LOCAL,      // Device storage via SAF
    GOOGLE_DRIVE // Google Drive app folder
}

/**
 * ViewModel for managing backup and restore operations.
 *
 * Responsibilities:
 * - Manage backup/restore state and progress
 * - Handle password input and validation
 * - Coordinate between repositories and UI
 * - Support both local and Google Drive backups
 * - Track operation status and provide user feedback
 */
class BackupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val encryptionService = EncryptionService()
    private val serializer = BackupSerializer()

    private var localRepository: BackupRepository? = null
    private var driveRepository: BackupRepository? = null
    private var googleDriveService: GoogleDriveService? = null
    private var categoryManager: CategoryManager? = null

    /**
     * Initialize repositories with required dependencies.
     * Call this from the UI after ViewModel creation.
     */
    fun initialize(
        context: Context,
        categoryManager: CategoryManager
    ) {
        this.categoryManager = categoryManager
        this.localRepository = LocalBackupRepository(encryptionService, serializer)

        // Initialize Google Drive service
        googleDriveService = GoogleDriveService(context).apply {
            initializeSignInClient()
        }

        // Check if already signed in
        val signedInAccount = googleDriveService?.getSignedInAccount()
        _uiState.value = _uiState.value.copy(isDriveSignedIn = signedInAccount != null)

        if (signedInAccount != null) {
            viewModelScope.launch {
                try {
                    googleDriveService?.handleSignInResult(signedInAccount)
                    driveRepository = DriveBackupRepository(
                        googleDriveService!!,
                        encryptionService,
                        serializer
                    )
                } catch (e: Exception) {
                    // Silent failure - user will need to sign in again
                    _uiState.value = _uiState.value.copy(isDriveSignedIn = false)
                }
            }
        }
    }

    /**
     * Update password input
     */
    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null
        )
    }

    /**
     * Update confirm password input
     */
    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = confirmPassword,
            passwordError = null
        )
    }

    /**
     * Update backup type selection
     */
    fun selectBackupType(type: BackupType) {
        _uiState.value = _uiState.value.copy(backupType = type)
    }

    /**
     * Validate password meets security requirements
     */
    private fun validatePassword(password: String): String? {
        return try {
            encryptionService.validatePassword(password)
            null
        } catch (e: IllegalArgumentException) {
            e.message
        }
    }

    /**
     * Validate password and confirm password match
     */
    private fun validatePasswordsMatch(): String? {
        val state = _uiState.value
        if (state.password != state.confirmPassword) {
            return "Passwords do not match"
        }
        return validatePassword(state.password)
    }

    /**
     * Create a backup with current app data.
     *
     * @param transactions List of transactions to backup
     * @param categoryOverrides Map of category overrides
     * @param outputStream Output stream for local backup (null for Drive)
     */
    fun createBackup(
        transactions: List<ParsedTransaction>,
        categoryOverrides: Map<Long, String>,
        outputStream: OutputStream? = null
    ) {
        viewModelScope.launch {
            try {
                // Validate password
                val passwordError = validatePasswordsMatch()
                if (passwordError != null) {
                    _uiState.value = _uiState.value.copy(passwordError = passwordError)
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    operationType = OperationType.CREATING_BACKUP,
                    progress = "Preparing backup data...",
                    errorMessage = null,
                    successMessage = null
                )

                val repository = getCurrentRepository() ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationType = OperationType.IDLE,
                        errorMessage = "Backup repository not initialized"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(progress = "Serializing data...")
                val backupData = serializer.serializeToBackupData(
                    transactions = transactions,
                    categoryOverrides = categoryOverrides
                )

                _uiState.value = _uiState.value.copy(progress = "Encrypting backup...")
                val result = repository.createBackup(
                    data = backupData,
                    password = _uiState.value.password,
                    outputStream = outputStream
                )

                result.fold(
                    onSuccess = { metadata ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            operationType = OperationType.IDLE,
                            progress = "",
                            password = "",
                            confirmPassword = "",
                            successMessage = "Backup created successfully (${metadata.transactionCount} transactions)",
                            lastBackupMetadata = metadata
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            operationType = OperationType.IDLE,
                            progress = "",
                            errorMessage = "Backup failed: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationType = OperationType.IDLE,
                    progress = "",
                    errorMessage = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    /**
     * Restore data from a backup file.
     *
     * @param inputStream Input stream to read backup from
     * @param onRestoreComplete Callback with restored data (transactions, category overrides)
     */
    fun restoreBackup(
        inputStream: InputStream,
        onRestoreComplete: (List<ParsedTransaction>, Map<Long, String>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Validate password
                val passwordError = validatePassword(_uiState.value.password)
                if (passwordError != null) {
                    _uiState.value = _uiState.value.copy(passwordError = passwordError)
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    operationType = OperationType.RESTORING_BACKUP,
                    progress = "Reading backup file...",
                    errorMessage = null,
                    successMessage = null
                )

                val repository = getCurrentRepository() ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationType = OperationType.IDLE,
                        errorMessage = "Backup repository not initialized"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(progress = "Decrypting backup...")
                val result = repository.restoreBackup(
                    inputStream = inputStream,
                    password = _uiState.value.password
                )

                result.fold(
                    onSuccess = { backupData ->
                        _uiState.value = _uiState.value.copy(progress = "Restoring data...")

                        // Convert serializable data back to app format
                        val transactions = serializer.deserializeTransactions(backupData.transactions)
                        val categoryOverrides = serializer.deserializeCategoryOverrides(backupData.categoryOverrides)

                        // Call completion callback
                        onRestoreComplete(transactions, categoryOverrides)

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            operationType = OperationType.IDLE,
                            progress = "",
                            password = "",
                            successMessage = "Backup restored successfully (${backupData.metadata.transactionCount} transactions)",
                            lastBackupMetadata = backupData.metadata
                        )
                    },
                    onFailure = { error ->
                        val errorMsg = when (error) {
                            is InvalidBackupException -> "Invalid backup file: ${error.message}"
                            is RestoreException -> "Restore failed: ${error.message}"
                            else -> "Restore failed: ${error.message}"
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            operationType = OperationType.IDLE,
                            progress = "",
                            errorMessage = errorMsg
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationType = OperationType.IDLE,
                    progress = "",
                    errorMessage = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    /**
     * Validate if a password can decrypt a backup file without fully restoring it.
     */
    fun validateBackupPassword(inputStream: InputStream) {
        viewModelScope.launch {
            try {
                val passwordError = validatePassword(_uiState.value.password)
                if (passwordError != null) {
                    _uiState.value = _uiState.value.copy(passwordError = passwordError)
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    operationType = OperationType.VALIDATING_PASSWORD,
                    progress = "Validating password...",
                    errorMessage = null,
                    successMessage = null
                )

                val repository = getCurrentRepository() ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationType = OperationType.IDLE,
                        errorMessage = "Backup repository not initialized"
                    )
                    return@launch
                }

                val result = repository.validateBackupPassword(
                    inputStream = inputStream,
                    password = _uiState.value.password
                )

                result.fold(
                    onSuccess = { isValid ->
                        if (isValid) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                operationType = OperationType.IDLE,
                                progress = "",
                                successMessage = "Password is correct"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                operationType = OperationType.IDLE,
                                progress = "",
                                errorMessage = "Incorrect password"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            operationType = OperationType.IDLE,
                            progress = "",
                            errorMessage = "Validation failed: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationType = OperationType.IDLE,
                    progress = "",
                    errorMessage = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    /**
     * Get metadata from a backup file without full restore.
     */
    fun getBackupMetadata(inputStream: InputStream) {
        viewModelScope.launch {
            try {
                val passwordError = validatePassword(_uiState.value.password)
                if (passwordError != null) {
                    _uiState.value = _uiState.value.copy(passwordError = passwordError)
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    progress = "Reading backup info...",
                    errorMessage = null
                )

                val repository = getCurrentRepository() ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Backup repository not initialized"
                    )
                    return@launch
                }

                val result = repository.getBackupMetadata(
                    inputStream = inputStream,
                    password = _uiState.value.password
                )

                result.fold(
                    onSuccess = { metadata ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            progress = "",
                            lastBackupMetadata = metadata
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            progress = "",
                            errorMessage = "Failed to read backup info: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    progress = "",
                    errorMessage = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    /**
     * Load list of available backups (Drive only).
     */
    fun loadAvailableBackups() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    operationType = OperationType.LOADING_BACKUPS,
                    progress = "Loading backups...",
                    errorMessage = null
                )

                val repository = driveRepository ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationType = OperationType.IDLE,
                        errorMessage = "Google Drive not initialized"
                    )
                    return@launch
                }

                val result = repository.listBackups()
                result.fold(
                    onSuccess = { backups ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            operationType = OperationType.IDLE,
                            progress = "",
                            availableBackups = backups
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            operationType = OperationType.IDLE,
                            progress = "",
                            errorMessage = "Failed to load backups: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationType = OperationType.IDLE,
                    progress = "",
                    errorMessage = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    /**
     * Delete a backup (Drive only).
     */
    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    operationType = OperationType.DELETING_BACKUP,
                    progress = "Deleting backup...",
                    errorMessage = null
                )

                val repository = driveRepository ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationType = OperationType.IDLE,
                        errorMessage = "Google Drive not initialized"
                    )
                    return@launch
                }

                val result = repository.deleteBackup(backupId)
                result.fold(
                    onSuccess = {
                        // Reload backups list
                        loadAvailableBackups()
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Backup deleted successfully"
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            operationType = OperationType.IDLE,
                            progress = "",
                            errorMessage = "Failed to delete backup: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationType = OperationType.IDLE,
                    progress = "",
                    errorMessage = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    /**
     * Handle Google Sign-In result
     */
    fun handleDriveSignIn(context: Context) {
        viewModelScope.launch {
            try {
                val account = googleDriveService?.getSignedInAccount()
                if (account != null) {
                    googleDriveService?.handleSignInResult(account)
                    driveRepository = DriveBackupRepository(
                        googleDriveService!!,
                        encryptionService,
                        serializer
                    )
                    _uiState.value = _uiState.value.copy(
                        isDriveSignedIn = true,
                        successMessage = "Signed in to Google Drive"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDriveSignedIn = false,
                    errorMessage = "Failed to sign in: ${e.message}"
                )
            }
        }
    }

    /**
     * Sign out from Google Drive
     */
    fun signOutFromDrive() {
        viewModelScope.launch {
            try {
                googleDriveService?.signOut()
                driveRepository = null
                _uiState.value = _uiState.value.copy(
                    isDriveSignedIn = false,
                    successMessage = "Signed out from Google Drive"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to sign out: ${e.message}"
                )
            }
        }
    }

    /**
     * Get Google Sign-In intent for launching
     */
    fun getGoogleSignInIntent() = googleDriveService?.getSignInIntent()

    /**
     * Clear messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    /**
     * Clear all state (useful for resetting after operations)
     */
    fun resetState() {
        _uiState.value = BackupUiState(
            isDriveSignedIn = _uiState.value.isDriveSignedIn
        )
    }

    /**
     * Get the appropriate repository based on current backup type
     */
    private fun getCurrentRepository(): BackupRepository? {
        return when (_uiState.value.backupType) {
            BackupType.LOCAL -> localRepository
            BackupType.GOOGLE_DRIVE -> driveRepository
        }
    }
}
