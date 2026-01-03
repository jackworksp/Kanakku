package com.example.kanakku.ui.backup

import android.content.Context
import com.example.kanakku.data.backup.BackupException
import com.example.kanakku.data.backup.BackupRepository
import com.example.kanakku.data.backup.BackupSerializer
import com.example.kanakku.data.backup.EncryptionService
import com.example.kanakku.data.backup.InvalidBackupException
import com.example.kanakku.data.backup.RestoreException
import com.example.kanakku.data.category.CategoryManager
import com.example.kanakku.data.model.BackupData
import com.example.kanakku.data.model.BackupMetadata
import com.example.kanakku.data.model.CategoryOverride
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.SerializableTransaction
import com.example.kanakku.data.model.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Unit tests for BackupViewModel
 *
 * Tests all ViewModel states, backup/restore flows, and error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private lateinit var viewModel: BackupViewModel
    private lateinit var mockContext: Context
    private lateinit var mockCategoryManager: CategoryManager
    private lateinit var mockLocalRepository: BackupRepository
    private lateinit var mockDriveRepository: BackupRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        mockCategoryManager = mockk(relaxed = true)
        mockLocalRepository = mockk(relaxed = true)
        mockDriveRepository = mockk(relaxed = true)

        viewModel = BackupViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== Test Data Helpers ==========

    private fun createSampleTransactions(count: Int = 3): List<ParsedTransaction> {
        return (1..count).map { i ->
            ParsedTransaction(
                smsId = i.toLong(),
                amount = 100.0 * i,
                type = if (i % 2 == 0) TransactionType.CREDIT else TransactionType.DEBIT,
                merchant = "Merchant $i",
                accountNumber = "XXXX1234",
                referenceNumber = "REF$i",
                date = System.currentTimeMillis() - (i * 86400000L),
                rawSms = "SMS content $i",
                senderAddress = "BANK-$i",
                balanceAfter = 5000.0 + (i * 100.0),
                location = "Location $i"
            )
        }
    }

    private fun createSampleBackupData(
        transactionCount: Int = 3,
        categoryOverrideCount: Int = 2
    ): BackupData {
        val transactions = (1..transactionCount).map { i ->
            SerializableTransaction(
                smsId = i.toLong(),
                amount = 100.0 * i,
                type = if (i % 2 == 0) "CREDIT" else "DEBIT",
                merchant = "Merchant $i",
                accountNumber = "XXXX1234",
                referenceNumber = "REF$i",
                date = System.currentTimeMillis() - (i * 86400000L),
                rawSms = "SMS content $i",
                senderAddress = "BANK-$i",
                balanceAfter = 5000.0 + (i * 100.0),
                location = "Location $i"
            )
        }

        val categoryOverrides = (1..categoryOverrideCount).map { i ->
            CategoryOverride(
                smsId = i.toLong(),
                categoryId = "category_$i"
            )
        }

        val metadata = BackupMetadata(
            version = 1,
            timestamp = System.currentTimeMillis(),
            deviceName = "Test Device",
            appVersion = "1.0.0",
            transactionCount = transactionCount,
            categoryOverrideCount = categoryOverrideCount
        )

        return BackupData(
            metadata = metadata,
            transactions = transactions,
            categoryOverrides = categoryOverrides,
            customCategories = emptyList()
        )
    }

    private fun createSampleMetadata(): BackupMetadata {
        return BackupMetadata(
            version = 1,
            timestamp = System.currentTimeMillis(),
            deviceName = "Test Device",
            appVersion = "1.0.0",
            transactionCount = 5,
            categoryOverrideCount = 3
        )
    }

    // ========== Initial State Tests ==========

    @Test
    fun initialState_hasCorrectDefaults() {
        // Given - ViewModel created in setup()

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertEquals(OperationType.IDLE, state.operationType)
        assertEquals("", state.progress)
        assertEquals(BackupType.LOCAL, state.backupType)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertNull(state.passwordError)
        assertNull(state.successMessage)
        assertNull(state.errorMessage)
        assertNull(state.lastBackupMetadata)
        assertTrue(state.availableBackups.isEmpty())
        assertFalse(state.isDriveSignedIn)
    }

    // ========== Password Management Tests ==========

    @Test
    fun updatePassword_updatesStateAndClearsError() {
        // Given
        viewModel.uiState.value.copy(passwordError = "Previous error")

        // When
        viewModel.updatePassword("NewPassword123")

        // Then
        val state = viewModel.uiState.value
        assertEquals("NewPassword123", state.password)
        assertNull(state.passwordError)
    }

    @Test
    fun updateConfirmPassword_updatesStateAndClearsError() {
        // Given
        viewModel.uiState.value.copy(passwordError = "Previous error")

        // When
        viewModel.updateConfirmPassword("ConfirmPassword123")

        // Then
        val state = viewModel.uiState.value
        assertEquals("ConfirmPassword123", state.confirmPassword)
        assertNull(state.passwordError)
    }

    @Test
    fun selectBackupType_updatesBackupType() {
        // Given - Default is LOCAL
        assertEquals(BackupType.LOCAL, viewModel.uiState.value.backupType)

        // When
        viewModel.selectBackupType(BackupType.GOOGLE_DRIVE)

        // Then
        assertEquals(BackupType.GOOGLE_DRIVE, viewModel.uiState.value.backupType)
    }

    // ========== Password Validation Tests ==========

    @Test
    fun createBackup_withShortPassword_setsPasswordError() = runTest {
        // Given
        viewModel.updatePassword("short")
        viewModel.updateConfirmPassword("short")
        val transactions = createSampleTransactions()
        val categoryOverrides = mapOf(1L to "category_1")
        val outputStream = ByteArrayOutputStream()

        // Use reflection to set repository since initialize() requires Context
        setMockLocalRepository()

        // When
        viewModel.createBackup(transactions, categoryOverrides, outputStream)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.passwordError)
        assertTrue(state.passwordError!!.contains("at least 8 characters") ||
                   state.passwordError!!.contains("too short"))
        assertFalse(state.isLoading)
        assertEquals(OperationType.IDLE, state.operationType)
    }

    @Test
    fun createBackup_withMismatchedPasswords_setsPasswordError() = runTest {
        // Given
        viewModel.updatePassword("Password123")
        viewModel.updateConfirmPassword("DifferentPassword123")
        val transactions = createSampleTransactions()
        val categoryOverrides = mapOf(1L to "category_1")
        val outputStream = ByteArrayOutputStream()

        setMockLocalRepository()

        // When
        viewModel.createBackup(transactions, categoryOverrides, outputStream)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("Passwords do not match", state.passwordError)
        assertFalse(state.isLoading)
    }

    // ========== Create Backup Tests ==========

    @Test
    fun createBackup_success_updatesStateCorrectly() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        viewModel.updateConfirmPassword(password)
        val transactions = createSampleTransactions()
        val categoryOverrides = mapOf(1L to "category_1", 2L to "category_2")
        val outputStream = ByteArrayOutputStream()
        val expectedMetadata = createSampleMetadata()

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.createBackup(any(), eq(password), eq(outputStream))
        } returns Result.success(expectedMetadata)

        // When
        viewModel.createBackup(transactions, categoryOverrides, outputStream)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(OperationType.IDLE, state.operationType)
        assertEquals("", state.progress)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("successfully"))
        assertTrue(state.successMessage!!.contains("${expectedMetadata.transactionCount}"))
        assertNull(state.errorMessage)
        assertEquals(expectedMetadata, state.lastBackupMetadata)

        coVerify { mockLocalRepository.createBackup(any(), eq(password), eq(outputStream)) }
    }

    @Test
    fun createBackup_withRepositoryError_setsErrorMessage() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        viewModel.updateConfirmPassword(password)
        val transactions = createSampleTransactions()
        val categoryOverrides = mapOf(1L to "category_1")
        val outputStream = ByteArrayOutputStream()

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.createBackup(any(), any(), any())
        } returns Result.failure(BackupException("Storage full"))

        // When
        viewModel.createBackup(transactions, categoryOverrides, outputStream)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(OperationType.IDLE, state.operationType)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Backup failed"))
        assertTrue(state.errorMessage!!.contains("Storage full"))
        assertNull(state.successMessage)
    }

    @Test
    fun createBackup_withoutRepository_setsErrorMessage() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        viewModel.updateConfirmPassword(password)
        val transactions = createSampleTransactions()
        val categoryOverrides = mapOf(1L to "category_1")
        val outputStream = ByteArrayOutputStream()

        // Don't set repository - it will be null

        // When
        viewModel.createBackup(transactions, categoryOverrides, outputStream)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(OperationType.IDLE, state.operationType)
        assertEquals("Backup repository not initialized", state.errorMessage)
    }

    @Test
    fun createBackup_showsProgressStates() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        viewModel.updateConfirmPassword(password)
        val transactions = createSampleTransactions()
        val categoryOverrides = mapOf(1L to "category_1")
        val outputStream = ByteArrayOutputStream()
        val expectedMetadata = createSampleMetadata()

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.createBackup(any(), any(), any())
        } returns Result.success(expectedMetadata)

        // When
        viewModel.createBackup(transactions, categoryOverrides, outputStream)

        // Then - Check loading state before coroutine completes
        var state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertEquals(OperationType.CREATING_BACKUP, state.operationType)

        advanceUntilIdle()

        // Then - Check final state
        state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(OperationType.IDLE, state.operationType)
    }

    // ========== Restore Backup Tests ==========

    @Test
    fun restoreBackup_success_callsCallbackWithData() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        val backupData = createSampleBackupData()
        val inputStream = ByteArrayInputStream(byteArrayOf())
        var callbackCalled = false
        var restoredTransactions: List<ParsedTransaction>? = null
        var restoredOverrides: Map<Long, String>? = null

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.restoreBackup(eq(inputStream), eq(password))
        } returns Result.success(backupData)

        // When
        viewModel.restoreBackup(inputStream) { transactions, overrides ->
            callbackCalled = true
            restoredTransactions = transactions
            restoredOverrides = overrides
        }
        advanceUntilIdle()

        // Then
        assertTrue(callbackCalled)
        assertNotNull(restoredTransactions)
        assertNotNull(restoredOverrides)
        assertEquals(backupData.transactions.size, restoredTransactions!!.size)
        assertEquals(backupData.categoryOverrides.size, restoredOverrides!!.size)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(OperationType.IDLE, state.operationType)
        assertEquals("", state.password)
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("restored successfully"))
        assertNull(state.errorMessage)
    }

    @Test
    fun restoreBackup_withInvalidBackupException_setsSpecificError() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        val inputStream = ByteArrayInputStream(byteArrayOf())

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.restoreBackup(any(), any())
        } returns Result.failure(InvalidBackupException("Corrupted file"))

        // When
        viewModel.restoreBackup(inputStream) { _, _ -> }
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Invalid backup file"))
        assertTrue(state.errorMessage!!.contains("Corrupted file"))
    }

    @Test
    fun restoreBackup_withRestoreException_setsSpecificError() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        val inputStream = ByteArrayInputStream(byteArrayOf())

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.restoreBackup(any(), any())
        } returns Result.failure(RestoreException("Wrong password"))

        // When
        viewModel.restoreBackup(inputStream) { _, _ -> }
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Restore failed"))
        assertTrue(state.errorMessage!!.contains("Wrong password"))
    }

    @Test
    fun restoreBackup_withInvalidPassword_setsPasswordError() = runTest {
        // Given
        viewModel.updatePassword("short")
        val inputStream = ByteArrayInputStream(byteArrayOf())

        setMockLocalRepository()

        // When
        viewModel.restoreBackup(inputStream) { _, _ -> }
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.passwordError)
        assertFalse(state.isLoading)
    }

    @Test
    fun restoreBackup_showsProgressStates() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        val backupData = createSampleBackupData()
        val inputStream = ByteArrayInputStream(byteArrayOf())

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.restoreBackup(any(), any())
        } returns Result.success(backupData)

        // When
        viewModel.restoreBackup(inputStream) { _, _ -> }

        // Then - Check loading state
        var state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertEquals(OperationType.RESTORING_BACKUP, state.operationType)

        advanceUntilIdle()

        // Then - Check final state
        state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(OperationType.IDLE, state.operationType)
    }

    // ========== Validate Backup Password Tests ==========

    @Test
    fun validateBackupPassword_success_withCorrectPassword() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        val inputStream = ByteArrayInputStream(byteArrayOf())

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.validateBackupPassword(eq(inputStream), eq(password))
        } returns Result.success(true)

        // When
        viewModel.validateBackupPassword(inputStream)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(OperationType.IDLE, state.operationType)
        assertEquals("Password is correct", state.successMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun validateBackupPassword_success_withIncorrectPassword() = runTest {
        // Given
        val password = "WrongPassword123"
        viewModel.updatePassword(password)
        val inputStream = ByteArrayInputStream(byteArrayOf())

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.validateBackupPassword(any(), any())
        } returns Result.success(false)

        // When
        viewModel.validateBackupPassword(inputStream)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Incorrect password", state.errorMessage)
        assertNull(state.successMessage)
    }

    @Test
    fun validateBackupPassword_withError_setsErrorMessage() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        val inputStream = ByteArrayInputStream(byteArrayOf())

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.validateBackupPassword(any(), any())
        } returns Result.failure(Exception("File read error"))

        // When
        viewModel.validateBackupPassword(inputStream)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Validation failed"))
    }

    // ========== Get Backup Metadata Tests ==========

    @Test
    fun getBackupMetadata_success_updatesState() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        val inputStream = ByteArrayInputStream(byteArrayOf())
        val expectedMetadata = createSampleMetadata()

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.getBackupMetadata(eq(inputStream), eq(password))
        } returns Result.success(expectedMetadata)

        // When
        viewModel.getBackupMetadata(inputStream)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.progress)
        assertEquals(expectedMetadata, state.lastBackupMetadata)
        assertNull(state.errorMessage)
    }

    @Test
    fun getBackupMetadata_withError_setsErrorMessage() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        val inputStream = ByteArrayInputStream(byteArrayOf())

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.getBackupMetadata(any(), any())
        } returns Result.failure(Exception("Cannot read metadata"))

        // When
        viewModel.getBackupMetadata(inputStream)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Failed to read backup info"))
    }

    // ========== Load Available Backups Tests ==========

    @Test
    fun loadAvailableBackups_success_updatesBackupsList() = runTest {
        // Given
        val backups = listOf(createSampleMetadata(), createSampleMetadata())

        setMockDriveRepository()
        coEvery { mockDriveRepository.listBackups() } returns Result.success(backups)

        // When
        viewModel.loadAvailableBackups()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(OperationType.IDLE, state.operationType)
        assertEquals(backups, state.availableBackups)
        assertNull(state.errorMessage)
    }

    @Test
    fun loadAvailableBackups_withoutDriveRepository_setsError() = runTest {
        // Given - No drive repository set

        // When
        viewModel.loadAvailableBackups()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Google Drive not initialized", state.errorMessage)
    }

    @Test
    fun loadAvailableBackups_withError_setsErrorMessage() = runTest {
        // Given
        setMockDriveRepository()
        coEvery { mockDriveRepository.listBackups() } returns Result.failure(Exception("Network error"))

        // When
        viewModel.loadAvailableBackups()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Failed to load backups"))
    }

    // ========== Delete Backup Tests ==========

    @Test
    fun deleteBackup_success_reloadsBackups() = runTest {
        // Given
        val backupId = "backup_123"
        val updatedBackups = listOf(createSampleMetadata())

        setMockDriveRepository()
        coEvery { mockDriveRepository.deleteBackup(backupId) } returns Result.success(Unit)
        coEvery { mockDriveRepository.listBackups() } returns Result.success(updatedBackups)

        // When
        viewModel.deleteBackup(backupId)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("Backup deleted successfully", state.successMessage)
        assertEquals(updatedBackups, state.availableBackups)

        coVerify { mockDriveRepository.deleteBackup(backupId) }
        coVerify { mockDriveRepository.listBackups() }
    }

    @Test
    fun deleteBackup_withError_setsErrorMessage() = runTest {
        // Given
        val backupId = "backup_123"

        setMockDriveRepository()
        coEvery {
            mockDriveRepository.deleteBackup(backupId)
        } returns Result.failure(Exception("Permission denied"))

        // When
        viewModel.deleteBackup(backupId)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Failed to delete backup"))
    }

    @Test
    fun deleteBackup_showsProgressStates() = runTest {
        // Given
        val backupId = "backup_123"

        setMockDriveRepository()
        coEvery { mockDriveRepository.deleteBackup(any()) } returns Result.success(Unit)
        coEvery { mockDriveRepository.listBackups() } returns Result.success(emptyList())

        // When
        viewModel.deleteBackup(backupId)

        // Then - Check loading state
        var state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertEquals(OperationType.DELETING_BACKUP, state.operationType)

        advanceUntilIdle()

        // Then - Check final state
        state = viewModel.uiState.value
        assertFalse(state.isLoading)
    }

    // ========== Message Management Tests ==========

    @Test
    fun clearMessages_removesSuccessAndErrorMessages() {
        // Given
        viewModel.uiState.value = viewModel.uiState.value.copy(
            successMessage = "Success!",
            errorMessage = "Error!"
        )

        // When
        viewModel.clearMessages()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.successMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun resetState_clearsAllExceptDriveStatus() {
        // Given
        viewModel.uiState.value = viewModel.uiState.value.copy(
            isLoading = true,
            operationType = OperationType.CREATING_BACKUP,
            progress = "Creating...",
            password = "password",
            confirmPassword = "password",
            passwordError = "error",
            successMessage = "success",
            errorMessage = "error",
            isDriveSignedIn = true,
            lastBackupMetadata = createSampleMetadata()
        )

        // When
        viewModel.resetState()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(OperationType.IDLE, state.operationType)
        assertEquals("", state.progress)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertNull(state.passwordError)
        assertNull(state.successMessage)
        assertNull(state.errorMessage)
        assertTrue(state.isDriveSignedIn) // Should be preserved
    }

    // ========== Backup Type Selection Tests ==========

    @Test
    fun createBackup_withLocalType_usesLocalRepository() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        viewModel.updateConfirmPassword(password)
        viewModel.selectBackupType(BackupType.LOCAL)
        val transactions = createSampleTransactions()
        val categoryOverrides = mapOf(1L to "category_1")
        val outputStream = ByteArrayOutputStream()

        setMockLocalRepository()
        coEvery { mockLocalRepository.createBackup(any(), any(), any()) } returns
            Result.success(createSampleMetadata())

        // When
        viewModel.createBackup(transactions, categoryOverrides, outputStream)
        advanceUntilIdle()

        // Then
        coVerify { mockLocalRepository.createBackup(any(), any(), any()) }
    }

    // ========== Edge Cases and Error Handling Tests ==========

    @Test
    fun createBackup_withEmptyTransactions_stillCreatesBackup() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        viewModel.updateConfirmPassword(password)
        val transactions = emptyList<ParsedTransaction>()
        val categoryOverrides = emptyMap<Long, String>()
        val outputStream = ByteArrayOutputStream()
        val metadata = createSampleMetadata().copy(transactionCount = 0)

        setMockLocalRepository()
        coEvery { mockLocalRepository.createBackup(any(), any(), any()) } returns
            Result.success(metadata)

        // When
        viewModel.createBackup(transactions, categoryOverrides, outputStream)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("0 transactions"))
    }

    @Test
    fun restoreBackup_withUnexpectedException_handlesGracefully() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        val inputStream = ByteArrayInputStream(byteArrayOf())

        setMockLocalRepository()
        coEvery {
            mockLocalRepository.restoreBackup(any(), any())
        } throws RuntimeException("Unexpected error")

        // When
        viewModel.restoreBackup(inputStream) { _, _ -> }
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(OperationType.IDLE, state.operationType)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Unexpected error"))
    }

    @Test
    fun validateBackupPassword_withoutRepository_setsError() = runTest {
        // Given
        viewModel.updatePassword("SecurePassword123")
        val inputStream = ByteArrayInputStream(byteArrayOf())

        // Don't set repository

        // When
        viewModel.validateBackupPassword(inputStream)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("Backup repository not initialized", state.errorMessage)
    }

    @Test
    fun multipleOperations_handleStateCorrectly() = runTest {
        // Given
        val password = "SecurePassword123"
        viewModel.updatePassword(password)
        viewModel.updateConfirmPassword(password)

        setMockLocalRepository()
        coEvery { mockLocalRepository.createBackup(any(), any(), any()) } returns
            Result.success(createSampleMetadata())

        // When - First operation
        viewModel.createBackup(
            createSampleTransactions(),
            mapOf(1L to "cat1"),
            ByteArrayOutputStream()
        )
        advanceUntilIdle()

        // Clear messages
        viewModel.clearMessages()

        // When - Second operation
        viewModel.updatePassword(password)
        viewModel.updateConfirmPassword(password)
        viewModel.createBackup(
            createSampleTransactions(),
            mapOf(2L to "cat2"),
            ByteArrayOutputStream()
        )
        advanceUntilIdle()

        // Then - Both operations should succeed
        val state = viewModel.uiState.value
        assertNotNull(state.successMessage)
        assertFalse(state.isLoading)
    }

    // ========== Helper Methods ==========

    private fun setMockLocalRepository() {
        // Use reflection to set the private repository field
        val field = BackupViewModel::class.java.getDeclaredField("localRepository")
        field.isAccessible = true
        field.set(viewModel, mockLocalRepository)
    }

    private fun setMockDriveRepository() {
        // Use reflection to set the private repository field
        val field = BackupViewModel::class.java.getDeclaredField("driveRepository")
        field.isAccessible = true
        field.set(viewModel, mockDriveRepository)
    }
}
