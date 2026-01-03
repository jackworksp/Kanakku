package com.example.kanakku.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kanakku.data.backup.BackupSerializer
import com.example.kanakku.data.backup.EncryptionService
import com.example.kanakku.data.backup.LocalBackupRepository
import com.example.kanakku.data.model.BackupData
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Integration tests for backup/restore functionality
 *
 * Tests the full backup/restore roundtrip with encryption:
 * 1. Create sample data (transactions, category overrides)
 * 2. Serialize data with BackupSerializer
 * 3. Encrypt and write backup with LocalBackupRepository
 * 4. Decrypt and restore backup
 * 5. Verify restored data matches original data
 *
 * These tests verify that all components work together correctly:
 * - BackupSerializer (serialization)
 * - EncryptionService (encryption/decryption)
 * - LocalBackupRepository (backup/restore flow)
 */
@RunWith(AndroidJUnit4::class)
class BackupIntegrationTest {

    private lateinit var encryptionService: EncryptionService
    private lateinit var backupSerializer: BackupSerializer
    private lateinit var localBackupRepository: LocalBackupRepository

    @Before
    fun setup() {
        encryptionService = EncryptionService()
        backupSerializer = BackupSerializer()
        localBackupRepository = LocalBackupRepository(encryptionService)
    }

    // ========== Test Data Helpers ==========

    /**
     * Create sample transactions for testing
     */
    private fun createSampleTransactions(count: Int = 5): List<ParsedTransaction> {
        return (1..count).map { i ->
            ParsedTransaction(
                smsId = i.toLong(),
                amount = 100.0 * i,
                type = if (i % 2 == 0) TransactionType.CREDIT else TransactionType.DEBIT,
                merchant = "Merchant $i",
                accountNumber = "XXXX${1234 + i}",
                referenceNumber = "REF$i",
                date = System.currentTimeMillis() - (i * 86400000L),
                rawSms = "Test SMS content for transaction $i",
                senderAddress = "BANK-TEST-$i",
                balanceAfter = 5000.0 + (i * 100.0),
                location = if (i % 3 == 0) "Location $i" else null
            )
        }
    }

    /**
     * Create sample category overrides for testing
     */
    private fun createSampleCategoryOverrides(count: Int = 3): Map<Long, String> {
        return (1L..count.toLong()).associateWith { "category_$it" }
    }

    // ========== Full Backup/Restore Roundtrip Tests ==========

    @Test
    fun fullBackupRestoreRoundtrip_withBasicData_succeeds() = runTest {
        // Given
        val password = "SecurePassword123"
        val transactions = createSampleTransactions(5)
        val categoryOverrides = createSampleCategoryOverrides(3)

        val originalBackupData = backupSerializer.createBackupData(
            transactions = transactions,
            categoryOverrides = categoryOverrides
        )

        val outputStream = ByteArrayOutputStream()

        // When - Create backup
        val createResult = localBackupRepository.createBackup(
            data = originalBackupData,
            password = password,
            outputStream = outputStream
        )

        // Then - Verify backup creation succeeded
        assertTrue(createResult.isSuccess)
        val metadata = createResult.getOrNull()
        assertNotNull(metadata)
        assertEquals(5, metadata?.transactionCount)
        assertEquals(3, metadata?.categoryOverrideCount)

        // When - Restore backup
        val backupBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(backupBytes)

        val restoreResult = localBackupRepository.restoreBackup(
            inputStream = inputStream,
            password = password
        )

        // Then - Verify restore succeeded
        assertTrue(restoreResult.isSuccess)
        val restoredData = restoreResult.getOrNull()
        assertNotNull(restoredData)

        // Verify metadata
        assertEquals(originalBackupData.metadata.version, restoredData?.metadata?.version)
        assertEquals(originalBackupData.metadata.transactionCount, restoredData?.metadata?.transactionCount)
        assertEquals(originalBackupData.metadata.categoryOverrideCount, restoredData?.metadata?.categoryOverrideCount)
        assertEquals(originalBackupData.metadata.deviceName, restoredData?.metadata?.deviceName)

        // Verify transactions
        assertEquals(originalBackupData.transactions.size, restoredData?.transactions?.size)
        originalBackupData.transactions.forEachIndexed { index, original ->
            val restored = restoredData?.transactions?.get(index)
            assertEquals(original.smsId, restored?.smsId)
            assertEquals(original.amount, restored?.amount, 0.001)
            assertEquals(original.type, restored?.type)
            assertEquals(original.merchant, restored?.merchant)
            assertEquals(original.accountNumber, restored?.accountNumber)
            assertEquals(original.referenceNumber, restored?.referenceNumber)
            assertEquals(original.date, restored?.date)
            assertEquals(original.rawSms, restored?.rawSms)
            assertEquals(original.senderAddress, restored?.senderAddress)
            assertEquals(original.balanceAfter, restored?.balanceAfter)
            assertEquals(original.location, restored?.location)
        }

        // Verify category overrides
        assertEquals(originalBackupData.categoryOverrides.size, restoredData?.categoryOverrides?.size)
        originalBackupData.categoryOverrides.forEachIndexed { index, original ->
            val restored = restoredData?.categoryOverrides?.get(index)
            assertEquals(original.smsId, restored?.smsId)
            assertEquals(original.categoryId, restored?.categoryId)
        }
    }

    @Test
    fun fullBackupRestoreRoundtrip_withEmptyData_succeeds() = runTest {
        // Given
        val password = "SecurePassword123"
        val transactions = emptyList<ParsedTransaction>()
        val categoryOverrides = emptyMap<Long, String>()

        val originalBackupData = backupSerializer.createBackupData(
            transactions = transactions,
            categoryOverrides = categoryOverrides
        )

        val outputStream = ByteArrayOutputStream()

        // When - Create backup
        val createResult = localBackupRepository.createBackup(
            data = originalBackupData,
            password = password,
            outputStream = outputStream
        )

        // Then - Verify backup creation succeeded
        assertTrue(createResult.isSuccess)

        // When - Restore backup
        val backupBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(backupBytes)

        val restoreResult = localBackupRepository.restoreBackup(
            inputStream = inputStream,
            password = password
        )

        // Then - Verify restore succeeded
        assertTrue(restoreResult.isSuccess)
        val restoredData = restoreResult.getOrNull()
        assertNotNull(restoredData)
        assertEquals(0, restoredData?.transactions?.size)
        assertEquals(0, restoredData?.categoryOverrides?.size)
    }

    @Test
    fun fullBackupRestoreRoundtrip_withLargeDataset_succeeds() = runTest {
        // Given
        val password = "SecurePassword123"
        val transactions = createSampleTransactions(100)
        val categoryOverrides = createSampleCategoryOverrides(50)

        val originalBackupData = backupSerializer.createBackupData(
            transactions = transactions,
            categoryOverrides = categoryOverrides
        )

        val outputStream = ByteArrayOutputStream()

        // When - Create backup
        val createResult = localBackupRepository.createBackup(
            data = originalBackupData,
            password = password,
            outputStream = outputStream
        )

        // Then - Verify backup creation succeeded
        assertTrue(createResult.isSuccess)
        assertEquals(100, createResult.getOrNull()?.transactionCount)
        assertEquals(50, createResult.getOrNull()?.categoryOverrideCount)

        // When - Restore backup
        val backupBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(backupBytes)

        val restoreResult = localBackupRepository.restoreBackup(
            inputStream = inputStream,
            password = password
        )

        // Then - Verify restore succeeded
        assertTrue(restoreResult.isSuccess)
        val restoredData = restoreResult.getOrNull()
        assertEquals(100, restoredData?.transactions?.size)
        assertEquals(50, restoredData?.categoryOverrides?.size)
    }

    @Test
    fun fullBackupRestoreRoundtrip_withSpecialCharactersInData_succeeds() = runTest {
        // Given
        val password = "P@ssw0rd!#$%"
        val specialCharacterTransaction = ParsedTransaction(
            smsId = 1L,
            amount = 123.45,
            type = TransactionType.DEBIT,
            merchant = "Merchant with special chars: @#$%^&*()",
            accountNumber = "XXXX1234",
            referenceNumber = "REF-!@#$%",
            date = System.currentTimeMillis(),
            rawSms = "SMS with unicode: \u20B9123.45 debited from A/c XXXX1234",
            senderAddress = "BANK-\u20B9",
            balanceAfter = 5000.0,
            location = "Location with émojis 😀🎉"
        )

        val originalBackupData = backupSerializer.createBackupData(
            transactions = listOf(specialCharacterTransaction),
            categoryOverrides = mapOf(1L to "special_category_😀")
        )

        val outputStream = ByteArrayOutputStream()

        // When - Create and restore backup
        localBackupRepository.createBackup(
            data = originalBackupData,
            password = password,
            outputStream = outputStream
        )

        val backupBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(backupBytes)

        val restoreResult = localBackupRepository.restoreBackup(
            inputStream = inputStream,
            password = password
        )

        // Then - Verify special characters are preserved
        assertTrue(restoreResult.isSuccess)
        val restoredData = restoreResult.getOrNull()
        val restoredTransaction = restoredData?.transactions?.first()
        assertEquals("Merchant with special chars: @#$%^&*()", restoredTransaction?.merchant)
        assertEquals("REF-!@#$%", restoredTransaction?.referenceNumber)
        assertEquals("SMS with unicode: \u20B9123.45 debited from A/c XXXX1234", restoredTransaction?.rawSms)
        assertEquals("BANK-\u20B9", restoredTransaction?.senderAddress)
        assertEquals("Location with émojis 😀🎉", restoredTransaction?.location)
        assertEquals("special_category_😀", restoredData?.categoryOverrides?.first()?.categoryId)
    }

    @Test
    fun fullBackupRestoreRoundtrip_withDifferentPasswords_fails() = runTest {
        // Given
        val createPassword = "Password123"
        val wrongPassword = "WrongPassword456"
        val transactions = createSampleTransactions(3)
        val categoryOverrides = createSampleCategoryOverrides(2)

        val backupData = backupSerializer.createBackupData(
            transactions = transactions,
            categoryOverrides = categoryOverrides
        )

        val outputStream = ByteArrayOutputStream()

        // When - Create backup with one password
        localBackupRepository.createBackup(
            data = backupData,
            password = createPassword,
            outputStream = outputStream
        )

        // Then - Restore with different password should fail
        val backupBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(backupBytes)

        val restoreResult = localBackupRepository.restoreBackup(
            inputStream = inputStream,
            password = wrongPassword
        )

        assertTrue(restoreResult.isFailure)
    }

    @Test
    fun fullBackupRestoreRoundtrip_withMultipleIterations_succeeds() = runTest {
        // Given
        val password = "SecurePassword123"
        val transactions = createSampleTransactions(3)
        val categoryOverrides = createSampleCategoryOverrides(2)

        var currentBackupData = backupSerializer.createBackupData(
            transactions = transactions,
            categoryOverrides = categoryOverrides
        )

        // When - Perform backup/restore multiple times
        repeat(3) { iteration ->
            val outputStream = ByteArrayOutputStream()

            localBackupRepository.createBackup(
                data = currentBackupData,
                password = password,
                outputStream = outputStream
            )

            val backupBytes = outputStream.toByteArray()
            val inputStream = ByteArrayInputStream(backupBytes)

            val restoreResult = localBackupRepository.restoreBackup(
                inputStream = inputStream,
                password = password
            )

            // Then - Each iteration should succeed
            assertTrue("Iteration $iteration failed", restoreResult.isSuccess)
            currentBackupData = restoreResult.getOrThrow()
        }

        // Verify final data integrity
        assertEquals(3, currentBackupData.transactions.size)
        assertEquals(2, currentBackupData.categoryOverrides.size)
    }

    // ========== Encryption Integrity Tests ==========

    @Test
    fun encryption_withSameData_producesUniqueBackups() = runTest {
        // Given
        val password = "SecurePassword123"
        val transactions = createSampleTransactions(3)
        val categoryOverrides = createSampleCategoryOverrides(2)

        val backupData = backupSerializer.createBackupData(
            transactions = transactions,
            categoryOverrides = categoryOverrides
        )

        // When - Create two backups with same data and password
        val outputStream1 = ByteArrayOutputStream()
        localBackupRepository.createBackup(
            data = backupData,
            password = password,
            outputStream = outputStream1
        )

        val outputStream2 = ByteArrayOutputStream()
        localBackupRepository.createBackup(
            data = backupData,
            password = password,
            outputStream = outputStream2
        )

        // Then - Encrypted backups should be different (due to random salt/IV)
        val backup1Bytes = outputStream1.toByteArray()
        val backup2Bytes = outputStream2.toByteArray()
        assertFalse("Backups should be different due to random salt/IV",
            backup1Bytes.contentEquals(backup2Bytes))

        // But both should restore to same data
        val restored1 = localBackupRepository.restoreBackup(
            inputStream = ByteArrayInputStream(backup1Bytes),
            password = password
        ).getOrThrow()

        val restored2 = localBackupRepository.restoreBackup(
            inputStream = ByteArrayInputStream(backup2Bytes),
            password = password
        ).getOrThrow()

        assertEquals(restored1.transactions.size, restored2.transactions.size)
        assertEquals(restored1.categoryOverrides.size, restored2.categoryOverrides.size)
    }

    @Test
    fun encryption_corruptedData_failsToRestore() = runTest {
        // Given
        val password = "SecurePassword123"
        val transactions = createSampleTransactions(3)
        val categoryOverrides = createSampleCategoryOverrides(2)

        val backupData = backupSerializer.createBackupData(
            transactions = transactions,
            categoryOverrides = categoryOverrides
        )

        val outputStream = ByteArrayOutputStream()
        localBackupRepository.createBackup(
            data = backupData,
            password = password,
            outputStream = outputStream
        )

        // When - Corrupt the encrypted data
        val backupBytes = outputStream.toByteArray()
        if (backupBytes.size > 50) {
            backupBytes[25] = (backupBytes[25].toInt() xor 0xFF).toByte()
            backupBytes[50] = (backupBytes[50].toInt() xor 0xFF).toByte()
        }

        val inputStream = ByteArrayInputStream(backupBytes)

        // Then - Restore should fail
        val restoreResult = localBackupRepository.restoreBackup(
            inputStream = inputStream,
            password = password
        )

        assertTrue(restoreResult.isFailure)
    }

    // ========== Password Validation Tests ==========

    @Test
    fun passwordValidation_correctPassword_succeeds() = runTest {
        // Given
        val password = "SecurePassword123"
        val transactions = createSampleTransactions(2)
        val categoryOverrides = createSampleCategoryOverrides(1)

        val backupData = backupSerializer.createBackupData(
            transactions = transactions,
            categoryOverrides = categoryOverrides
        )

        val outputStream = ByteArrayOutputStream()
        localBackupRepository.createBackup(
            data = backupData,
            password = password,
            outputStream = outputStream
        )

        // When - Validate password
        val backupBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(backupBytes)

        val validationResult = localBackupRepository.validateBackupPassword(
            inputStream = inputStream,
            password = password
        )

        // Then
        assertTrue(validationResult.isSuccess)
        assertTrue(validationResult.getOrDefault(false))
    }

    @Test
    fun passwordValidation_wrongPassword_fails() = runTest {
        // Given
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword456"
        val transactions = createSampleTransactions(2)
        val categoryOverrides = createSampleCategoryOverrides(1)

        val backupData = backupSerializer.createBackupData(
            transactions = transactions,
            categoryOverrides = categoryOverrides
        )

        val outputStream = ByteArrayOutputStream()
        localBackupRepository.createBackup(
            data = backupData,
            password = correctPassword,
            outputStream = outputStream
        )

        // When - Validate with wrong password
        val backupBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(backupBytes)

        val validationResult = localBackupRepository.validateBackupPassword(
            inputStream = inputStream,
            password = wrongPassword
        )

        // Then
        assertTrue(validationResult.isSuccess)
        assertFalse(validationResult.getOrDefault(true))
    }

    // ========== Metadata Tests ==========

    @Test
    fun getBackupMetadata_withValidBackup_returnsCorrectMetadata() = runTest {
        // Given
        val password = "SecurePassword123"
        val transactions = createSampleTransactions(7)
        val categoryOverrides = createSampleCategoryOverrides(4)

        val backupData = backupSerializer.createBackupData(
            transactions = transactions,
            categoryOverrides = categoryOverrides,
            deviceName = "Test Device"
        )

        val outputStream = ByteArrayOutputStream()
        localBackupRepository.createBackup(
            data = backupData,
            password = password,
            outputStream = outputStream
        )

        // When - Get metadata
        val backupBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(backupBytes)

        val metadataResult = localBackupRepository.getBackupMetadata(
            inputStream = inputStream,
            password = password
        )

        // Then
        assertTrue(metadataResult.isSuccess)
        val metadata = metadataResult.getOrNull()
        assertNotNull(metadata)
        assertEquals(7, metadata?.transactionCount)
        assertEquals(4, metadata?.categoryOverrideCount)
        assertEquals("Test Device", metadata?.deviceName)
        assertEquals(BackupSerializer.BACKUP_VERSION, metadata?.version)
    }

    @Test
    fun getBackupMetadata_withWrongPassword_fails() = runTest {
        // Given
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword456"
        val transactions = createSampleTransactions(3)

        val backupData = backupSerializer.createBackupData(
            transactions = transactions,
            categoryOverrides = emptyMap()
        )

        val outputStream = ByteArrayOutputStream()
        localBackupRepository.createBackup(
            data = backupData,
            password = correctPassword,
            outputStream = outputStream
        )

        // When - Get metadata with wrong password
        val backupBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(backupBytes)

        val metadataResult = localBackupRepository.getBackupMetadata(
            inputStream = inputStream,
            password = wrongPassword
        )

        // Then
        assertTrue(metadataResult.isFailure)
    }

    // ========== Data Integrity Tests ==========

    @Test
    fun dataIntegrity_nullableFieldsPreserved_succeeds() = runTest {
        // Given
        val password = "SecurePassword123"
        val transactionWithNulls = ParsedTransaction(
            smsId = 1L,
            amount = 100.0,
            type = TransactionType.DEBIT,
            merchant = null,
            accountNumber = null,
            referenceNumber = null,
            date = System.currentTimeMillis(),
            rawSms = "Test SMS",
            senderAddress = "BANK",
            balanceAfter = null,
            location = null
        )

        val backupData = backupSerializer.createBackupData(
            transactions = listOf(transactionWithNulls),
            categoryOverrides = emptyMap()
        )

        val outputStream = ByteArrayOutputStream()

        // When - Create and restore backup
        localBackupRepository.createBackup(
            data = backupData,
            password = password,
            outputStream = outputStream
        )

        val backupBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(backupBytes)

        val restoreResult = localBackupRepository.restoreBackup(
            inputStream = inputStream,
            password = password
        )

        // Then - Null values should be preserved
        assertTrue(restoreResult.isSuccess)
        val restoredTransaction = restoreResult.getOrNull()?.transactions?.first()
        assertNull(restoredTransaction?.merchant)
        assertNull(restoredTransaction?.accountNumber)
        assertNull(restoredTransaction?.referenceNumber)
        assertNull(restoredTransaction?.balanceAfter)
        assertNull(restoredTransaction?.location)
    }

    @Test
    fun dataIntegrity_preciseAmountValues_preserved() = runTest {
        // Given
        val password = "SecurePassword123"
        val preciseAmounts = listOf(0.01, 123.45, 999999.99, 0.001, 123456.789)
        val transactions = preciseAmounts.mapIndexed { index, amount ->
            ParsedTransaction(
                smsId = index.toLong() + 1,
                amount = amount,
                type = TransactionType.DEBIT,
                merchant = "Merchant $index",
                accountNumber = "XXXX1234",
                referenceNumber = "REF$index",
                date = System.currentTimeMillis(),
                rawSms = "Test SMS $index",
                senderAddress = "BANK",
                balanceAfter = amount * 10,
                location = null
            )
        }

        val backupData = backupSerializer.createBackupData(
            transactions = transactions,
            categoryOverrides = emptyMap()
        )

        val outputStream = ByteArrayOutputStream()

        // When - Create and restore backup
        localBackupRepository.createBackup(
            data = backupData,
            password = password,
            outputStream = outputStream
        )

        val backupBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(backupBytes)

        val restoreResult = localBackupRepository.restoreBackup(
            inputStream = inputStream,
            password = password
        )

        // Then - Precise amounts should be preserved
        assertTrue(restoreResult.isSuccess)
        val restoredTransactions = restoreResult.getOrNull()?.transactions
        preciseAmounts.forEachIndexed { index, expectedAmount ->
            assertEquals(expectedAmount, restoredTransactions?.get(index)?.amount ?: 0.0, 0.001)
        }
    }
}
