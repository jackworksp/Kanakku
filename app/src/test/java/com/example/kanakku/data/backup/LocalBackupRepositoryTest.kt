package com.example.kanakku.data.backup

import com.example.kanakku.data.model.BackupData
import com.example.kanakku.data.model.BackupMetadata
import com.example.kanakku.data.model.CategoryOverride
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.SerializableTransaction
import com.example.kanakku.data.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Unit tests for LocalBackupRepository
 *
 * Tests backup creation, file handling, and restore operations
 */
class LocalBackupRepositoryTest {

    private lateinit var encryptionService: EncryptionService
    private lateinit var repository: LocalBackupRepository

    @Before
    fun setup() {
        encryptionService = EncryptionService()
        repository = LocalBackupRepository(encryptionService)
    }

    // ========== Test Data Helpers ==========

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

    // ========== CreateBackup Tests ==========

    @Test
    fun createBackup_success_writesEncryptedData() = runTest {
        // Given
        val backupData = createSampleBackupData()
        val password = "SecurePassword123"
        val outputStream = ByteArrayOutputStream()

        // When
        val result = repository.createBackup(backupData, password, outputStream)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(backupData.metadata, result.getOrNull())
        assertTrue(outputStream.size() > 0)
    }

    @Test
    fun createBackup_withValidData_canBeRestored() = runTest {
        // Given
        val backupData = createSampleBackupData()
        val password = "SecurePassword123"
        val outputStream = ByteArrayOutputStream()

        // When - create backup
        val createResult = repository.createBackup(backupData, password, outputStream)

        // Then - verify can be restored
        assertTrue(createResult.isSuccess)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val restoreResult = repository.restoreBackup(inputStream, password)
        assertTrue(restoreResult.isSuccess)
        assertEquals(backupData, restoreResult.getOrNull())
    }

    @Test
    fun createBackup_missingOutputStream_returnsFailure() = runTest {
        // Given
        val backupData = createSampleBackupData()
        val password = "SecurePassword123"

        // When
        val result = repository.createBackup(backupData, password, null)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is BackupException)
        assertTrue(
            exception?.message?.contains("Output stream is required") == true
        )
    }

    @Test
    fun createBackup_invalidPassword_returnsFailure() = runTest {
        // Given
        val backupData = createSampleBackupData()
        val weakPassword = "1234567"
        val outputStream = ByteArrayOutputStream()

        // When
        val result = repository.createBackup(backupData, weakPassword, outputStream)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is BackupException)
        assertTrue(
            exception?.message?.contains("8 characters") == true ||
            exception?.message?.contains("empty") == true
        )
    }

    @Test
    fun createBackup_emptyPassword_returnsFailure() = runTest {
        // Given
        val backupData = createSampleBackupData()
        val emptyPassword = ""
        val outputStream = ByteArrayOutputStream()

        // When
        val result = repository.createBackup(backupData, emptyPassword, outputStream)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is BackupException)
    }

    @Test
    fun createBackup_largeData_success() = runTest {
        // Given - backup with many transactions
        val backupData = createSampleBackupData(
            transactionCount = 1000,
            categoryOverrideCount = 500
        )
        val password = "SecurePassword123"
        val outputStream = ByteArrayOutputStream()

        // When
        val result = repository.createBackup(backupData, password, outputStream)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(outputStream.size() > 0)
    }

    @Test
    fun createBackup_emptyTransactions_success() = runTest {
        // Given - backup with no transactions
        val backupData = createSampleBackupData(
            transactionCount = 0,
            categoryOverrideCount = 0
        )
        val password = "SecurePassword123"
        val outputStream = ByteArrayOutputStream()

        // When
        val result = repository.createBackup(backupData, password, outputStream)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.transactionCount)
    }

    // ========== RestoreBackup Tests ==========

    @Test
    fun restoreBackup_success_returnsBackupData() = runTest {
        // Given
        val originalData = createSampleBackupData()
        val password = "SecurePassword123"
        val outputStream = ByteArrayOutputStream()
        repository.createBackup(originalData, password, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        // When
        val result = repository.restoreBackup(inputStream, password)

        // Then
        assertTrue(result.isSuccess)
        val restoredData = result.getOrNull()!!
        assertEquals(originalData.metadata, restoredData.metadata)
        assertEquals(originalData.transactions.size, restoredData.transactions.size)
        assertEquals(originalData.categoryOverrides.size, restoredData.categoryOverrides.size)
    }

    @Test
    fun restoreBackup_wrongPassword_returnsFailure() = runTest {
        // Given
        val originalData = createSampleBackupData()
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword123"
        val outputStream = ByteArrayOutputStream()
        repository.createBackup(originalData, correctPassword, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        // When
        val result = repository.restoreBackup(inputStream, wrongPassword)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is RestoreException)
        assertTrue(
            exception?.message?.contains("decrypt") == true ||
            exception?.message?.contains("password") == true
        )
    }

    @Test
    fun restoreBackup_invalidPassword_returnsFailure() = runTest {
        // Given
        val weakPassword = "weak"
        val inputStream = ByteArrayInputStream(ByteArray(100))

        // When
        val result = repository.restoreBackup(inputStream, weakPassword)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is RestoreException)
    }

    @Test
    fun restoreBackup_corruptedData_returnsFailure() = runTest {
        // Given - corrupted backup file
        val password = "SecurePassword123"
        val corruptedData = ByteArray(100) { it.toByte() }
        val inputStream = ByteArrayInputStream(corruptedData)

        // When
        val result = repository.restoreBackup(inputStream, password)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(
            exception is InvalidBackupException || exception is RestoreException
        )
    }

    @Test
    fun restoreBackup_emptyFile_returnsFailure() = runTest {
        // Given
        val password = "SecurePassword123"
        val emptyStream = ByteArrayInputStream(ByteArray(0))

        // When
        val result = repository.restoreBackup(emptyStream, password)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun restoreBackup_invalidJsonStructure_returnsFailure() = runTest {
        // Given - valid encryption but invalid JSON
        val password = "SecurePassword123"
        val invalidJson = "This is not JSON".toByteArray()
        val encrypted = encryptionService.encrypt(invalidJson, password)
        val inputStream = ByteArrayInputStream(encrypted.toByteArray())

        // When
        val result = repository.restoreBackup(inputStream, password)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is InvalidBackupException)
    }

    @Test
    fun restoreBackup_preservesAllFields() = runTest {
        // Given
        val originalData = createSampleBackupData(5, 3)
        val password = "SecurePassword123"
        val outputStream = ByteArrayOutputStream()
        repository.createBackup(originalData, password, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        // When
        val result = repository.restoreBackup(inputStream, password)

        // Then
        assertTrue(result.isSuccess)
        val restored = result.getOrNull()!!

        // Verify metadata
        assertEquals(originalData.metadata.version, restored.metadata.version)
        assertEquals(originalData.metadata.deviceName, restored.metadata.deviceName)
        assertEquals(originalData.metadata.appVersion, restored.metadata.appVersion)

        // Verify transactions
        assertEquals(originalData.transactions.size, restored.transactions.size)
        originalData.transactions.forEachIndexed { index, original ->
            val restoredTx = restored.transactions[index]
            assertEquals(original.smsId, restoredTx.smsId)
            assertEquals(original.amount, restoredTx.amount, 0.001)
            assertEquals(original.type, restoredTx.type)
            assertEquals(original.merchant, restoredTx.merchant)
        }

        // Verify category overrides
        assertEquals(originalData.categoryOverrides.size, restored.categoryOverrides.size)
        originalData.categoryOverrides.forEachIndexed { index, original ->
            val restoredOverride = restored.categoryOverrides[index]
            assertEquals(original.smsId, restoredOverride.smsId)
            assertEquals(original.categoryId, restoredOverride.categoryId)
        }
    }

    // ========== ValidateBackupPassword Tests ==========

    @Test
    fun validateBackupPassword_correctPassword_returnsTrue() = runTest {
        // Given
        val backupData = createSampleBackupData()
        val password = "SecurePassword123"
        val outputStream = ByteArrayOutputStream()
        repository.createBackup(backupData, password, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        // When
        val result = repository.validateBackupPassword(inputStream, password)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun validateBackupPassword_wrongPassword_returnsFalse() = runTest {
        // Given
        val backupData = createSampleBackupData()
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword123"
        val outputStream = ByteArrayOutputStream()
        repository.createBackup(backupData, correctPassword, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        // When
        val result = repository.validateBackupPassword(inputStream, wrongPassword)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun validateBackupPassword_invalidPasswordFormat_returnsFalse() = runTest {
        // Given
        val weakPassword = "weak"
        val inputStream = ByteArrayInputStream(ByteArray(100))

        // When
        val result = repository.validateBackupPassword(inputStream, weakPassword)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun validateBackupPassword_corruptedFile_returnsFalse() = runTest {
        // Given
        val password = "SecurePassword123"
        val corruptedData = ByteArray(100) { it.toByte() }
        val inputStream = ByteArrayInputStream(corruptedData)

        // When
        val result = repository.validateBackupPassword(inputStream, password)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun validateBackupPassword_emptyFile_returnsFalse() = runTest {
        // Given
        val password = "SecurePassword123"
        val emptyStream = ByteArrayInputStream(ByteArray(0))

        // When
        val result = repository.validateBackupPassword(emptyStream, password)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    // ========== GetBackupMetadata Tests ==========

    @Test
    fun getBackupMetadata_success_returnsMetadata() = runTest {
        // Given
        val backupData = createSampleBackupData()
        val password = "SecurePassword123"
        val outputStream = ByteArrayOutputStream()
        repository.createBackup(backupData, password, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        // When
        val result = repository.getBackupMetadata(inputStream, password)

        // Then
        assertTrue(result.isSuccess)
        val metadata = result.getOrNull()!!
        assertEquals(backupData.metadata.version, metadata.version)
        assertEquals(backupData.metadata.deviceName, metadata.deviceName)
        assertEquals(backupData.metadata.appVersion, metadata.appVersion)
        assertEquals(backupData.metadata.transactionCount, metadata.transactionCount)
        assertEquals(backupData.metadata.categoryOverrideCount, metadata.categoryOverrideCount)
    }

    @Test
    fun getBackupMetadata_wrongPassword_returnsFailure() = runTest {
        // Given
        val backupData = createSampleBackupData()
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword123"
        val outputStream = ByteArrayOutputStream()
        repository.createBackup(backupData, correctPassword, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        // When
        val result = repository.getBackupMetadata(inputStream, wrongPassword)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is InvalidBackupException)
    }

    @Test
    fun getBackupMetadata_invalidPassword_returnsFailure() = runTest {
        // Given
        val weakPassword = "weak"
        val inputStream = ByteArrayInputStream(ByteArray(100))

        // When
        val result = repository.getBackupMetadata(inputStream, weakPassword)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is InvalidBackupException)
    }

    @Test
    fun getBackupMetadata_corruptedFile_returnsFailure() = runTest {
        // Given
        val password = "SecurePassword123"
        val corruptedData = ByteArray(100) { it.toByte() }
        val inputStream = ByteArrayInputStream(corruptedData)

        // When
        val result = repository.getBackupMetadata(inputStream, password)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is InvalidBackupException)
    }

    // ========== ListBackups Tests ==========

    @Test
    fun listBackups_returnsEmptyList() = runTest {
        // When
        val result = repository.listBackups()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    // ========== DeleteBackup Tests ==========

    @Test
    fun deleteBackup_returnsUnsupportedOperation() = runTest {
        // When
        val result = repository.deleteBackup("any-backup-id")

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is UnsupportedOperationException)
        assertTrue(
            exception?.message?.contains("not supported") == true ||
            exception?.message?.contains("system file manager") == true
        )
    }

    // ========== Integration Tests ==========

    @Test
    fun fullBackupRestoreCycle_success() = runTest {
        // Given
        val originalData = createSampleBackupData(10, 5)
        val password = "MyBackupPassword2024!"
        val backupFile = ByteArrayOutputStream()

        // When - create backup
        val createResult = repository.createBackup(originalData, password, backupFile)
        assertTrue(createResult.isSuccess)

        // And - validate password
        val validateStream = ByteArrayInputStream(backupFile.toByteArray())
        val validateResult = repository.validateBackupPassword(validateStream, password)
        assertTrue(validateResult.isSuccess)
        assertTrue(validateResult.getOrNull() == true)

        // And - get metadata
        val metadataStream = ByteArrayInputStream(backupFile.toByteArray())
        val metadataResult = repository.getBackupMetadata(metadataStream, password)
        assertTrue(metadataResult.isSuccess)
        assertEquals(originalData.metadata.transactionCount, metadataResult.getOrNull()?.transactionCount)

        // And - restore backup
        val restoreStream = ByteArrayInputStream(backupFile.toByteArray())
        val restoreResult = repository.restoreBackup(restoreStream, password)
        assertTrue(restoreResult.isSuccess)

        // Then - verify restored data matches original
        val restoredData = restoreResult.getOrNull()!!
        assertEquals(originalData.metadata, restoredData.metadata)
        assertEquals(originalData.transactions, restoredData.transactions)
        assertEquals(originalData.categoryOverrides, restoredData.categoryOverrides)
    }

    @Test
    fun multipleBackups_withDifferentPasswords_independent() = runTest {
        // Given
        val data1 = createSampleBackupData(5, 2)
        val data2 = createSampleBackupData(7, 3)
        val password1 = "Password1"
        val password2 = "Password2"

        // When - create two separate backups
        val backup1 = ByteArrayOutputStream()
        val backup2 = ByteArrayOutputStream()
        repository.createBackup(data1, password1, backup1)
        repository.createBackup(data2, password2, backup2)

        // Then - each can be restored with correct password
        val restore1 = repository.restoreBackup(
            ByteArrayInputStream(backup1.toByteArray()),
            password1
        )
        val restore2 = repository.restoreBackup(
            ByteArrayInputStream(backup2.toByteArray()),
            password2
        )

        assertTrue(restore1.isSuccess)
        assertTrue(restore2.isSuccess)
        assertEquals(5, restore1.getOrNull()?.transactions?.size)
        assertEquals(7, restore2.getOrNull()?.transactions?.size)

        // And - cannot restore with wrong password
        val wrongRestore1 = repository.restoreBackup(
            ByteArrayInputStream(backup1.toByteArray()),
            password2
        )
        assertTrue(wrongRestore1.isFailure)
    }

    @Test
    fun backup_withSpecialCharactersInData_preservesData() = runTest {
        // Given - data with special characters
        val specialTransaction = SerializableTransaction(
            smsId = 1L,
            amount = 1234.56,
            type = "DEBIT",
            merchant = "Café & Restaurant™ <Special> \"Quotes\"",
            accountNumber = "XXXX-9999",
            referenceNumber = "REF@#$%123",
            date = System.currentTimeMillis(),
            rawSms = "SMS with unicode: 你好世界 🚀 नमस्ते",
            senderAddress = "BANK-NAME",
            balanceAfter = 5000.0,
            location = "Bangalore, KA, IN"
        )

        val backupData = BackupData(
            metadata = BackupMetadata(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceName = "Test Device",
                appVersion = "1.0.0",
                transactionCount = 1,
                categoryOverrideCount = 0
            ),
            transactions = listOf(specialTransaction),
            categoryOverrides = emptyList(),
            customCategories = emptyList()
        )

        val password = "SecurePassword123"
        val outputStream = ByteArrayOutputStream()

        // When
        repository.createBackup(backupData, password, outputStream)
        val restored = repository.restoreBackup(
            ByteArrayInputStream(outputStream.toByteArray()),
            password
        )

        // Then
        assertTrue(restored.isSuccess)
        val restoredTx = restored.getOrNull()?.transactions?.first()!!
        assertEquals(specialTransaction.merchant, restoredTx.merchant)
        assertEquals(specialTransaction.rawSms, restoredTx.rawSms)
        assertEquals(specialTransaction.referenceNumber, restoredTx.referenceNumber)
    }

    @Test
    fun backup_withZeroAmount_preservesValue() = runTest {
        // Given - transaction with zero amount
        val zeroTransaction = SerializableTransaction(
            smsId = 1L,
            amount = 0.0,
            type = "DEBIT",
            merchant = "Test Merchant",
            accountNumber = "XXXX1234",
            referenceNumber = "REF123",
            date = System.currentTimeMillis(),
            rawSms = "SMS content",
            senderAddress = "BANK",
            balanceAfter = 1000.0,
            location = null
        )

        val backupData = BackupData(
            metadata = BackupMetadata(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceName = "Test Device",
                appVersion = "1.0.0",
                transactionCount = 1,
                categoryOverrideCount = 0
            ),
            transactions = listOf(zeroTransaction),
            categoryOverrides = emptyList(),
            customCategories = emptyList()
        )

        val password = "SecurePassword123"
        val outputStream = ByteArrayOutputStream()

        // When
        repository.createBackup(backupData, password, outputStream)
        val restored = repository.restoreBackup(
            ByteArrayInputStream(outputStream.toByteArray()),
            password
        )

        // Then
        assertTrue(restored.isSuccess)
        val restoredTx = restored.getOrNull()?.transactions?.first()!!
        assertEquals(0.0, restoredTx.amount, 0.001)
    }

    @Test
    fun backup_withNullFields_preservesNulls() = runTest {
        // Given - transaction with null optional fields
        val transactionWithNulls = SerializableTransaction(
            smsId = 1L,
            amount = 100.0,
            type = "DEBIT",
            merchant = null,
            accountNumber = null,
            referenceNumber = null,
            date = System.currentTimeMillis(),
            rawSms = "SMS content",
            senderAddress = "BANK",
            balanceAfter = null,
            location = null
        )

        val backupData = BackupData(
            metadata = BackupMetadata(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceName = null,
                appVersion = "1.0.0",
                transactionCount = 1,
                categoryOverrideCount = 0
            ),
            transactions = listOf(transactionWithNulls),
            categoryOverrides = emptyList(),
            customCategories = emptyList()
        )

        val password = "SecurePassword123"
        val outputStream = ByteArrayOutputStream()

        // When
        repository.createBackup(backupData, password, outputStream)
        val restored = repository.restoreBackup(
            ByteArrayInputStream(outputStream.toByteArray()),
            password
        )

        // Then
        assertTrue(restored.isSuccess)
        val restoredData = restored.getOrNull()!!
        assertNull(restoredData.metadata.deviceName)
        val restoredTx = restoredData.transactions.first()
        assertNull(restoredTx.merchant)
        assertNull(restoredTx.accountNumber)
        assertNull(restoredTx.balanceAfter)
        assertNull(restoredTx.location)
    }
}
