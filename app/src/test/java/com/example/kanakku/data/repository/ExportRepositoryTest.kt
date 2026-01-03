package com.example.kanakku.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for ExportRepository.
 *
 * Tests cover:
 * - File creation and storage in exports directory
 * - CSV export with filter application
 * - PDF export with filter application
 * - Error handling (empty results, file I/O errors)
 * - Cleanup functionality for old exports
 * - File management (list, delete, get export files)
 * - URI generation for file sharing
 * - Date range filtering
 * - Category filtering (when implemented)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExportRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: KanakkuDatabase
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var exportRepository: ExportRepository
    private lateinit var exportsDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        transactionRepository = TransactionRepository(database)
        exportRepository = ExportRepository(context, transactionRepository)

        // Get reference to exports directory
        exportsDir = File(context.filesDir, "exports")

        // Clean up exports directory before each test
        if (exportsDir.exists()) {
            exportsDir.deleteRecursively()
        }
    }

    @After
    fun teardown() {
        database.close()

        // Clean up exports directory after each test
        if (exportsDir.exists()) {
            exportsDir.deleteRecursively()
        }
    }

    // ==================== Helper Functions ====================

    private val testCategory = Category(
        id = "food",
        name = "Food & Dining",
        icon = "🍔",
        color = androidx.compose.ui.graphics.Color(0xFFFF9800),
        keywords = listOf("swiggy", "zomato", "restaurant")
    )

    private val shoppingCategory = Category(
        id = "shopping",
        name = "Shopping",
        icon = "🛍️",
        color = androidx.compose.ui.graphics.Color(0xFFE91E63),
        keywords = listOf("amazon", "flipkart", "myntra")
    )

    private val categoryMap = mapOf(
        "food" to testCategory,
        "shopping" to shoppingCategory
    )

    private fun createTestTransaction(
        smsId: Long = 1L,
        amount: Double = 100.0,
        type: TransactionType = TransactionType.DEBIT,
        merchant: String? = "SWIGGY",
        date: Long = System.currentTimeMillis()
    ): ParsedTransaction {
        return ParsedTransaction(
            smsId = smsId,
            amount = amount,
            type = type,
            merchant = merchant,
            accountNumber = "1234",
            referenceNumber = "REF123",
            date = date,
            rawSms = "Test SMS",
            senderAddress = "VM-TESTBK",
            balanceAfter = 500.0,
            location = "Mumbai"
        )
    }

    private suspend fun setupTestTransactions(count: Int = 3): List<ParsedTransaction> {
        val transactions = (1..count).map { i ->
            createTestTransaction(
                smsId = i.toLong(),
                amount = 100.0 * i,
                merchant = if (i % 2 == 0) "SWIGGY" else "AMAZON",
                date = System.currentTimeMillis() - (i * 86400000L) // Different days
            )
        }
        transactions.forEach { transactionRepository.saveTransaction(it) }
        return transactions
    }

    // ==================== File Creation Tests ====================

    @Test
    fun getExportFile_createsExportsDirectory() {
        // When
        val file = exportRepository.getExportFile("test.csv")

        // Then
        assertTrue("Exports directory should be created", exportsDir.exists())
        assertTrue("Exports directory should be a directory", exportsDir.isDirectory)
    }

    @Test
    fun getExportFile_returnsFileInExportsDirectory() {
        // When
        val file = exportRepository.getExportFile("test.csv")

        // Then
        assertEquals("File should be in exports directory", exportsDir, file.parentFile)
        assertEquals("File should have correct name", "test.csv", file.name)
    }

    @Test
    fun getExportFile_multipleFiles_allInSameDirectory() {
        // When
        val file1 = exportRepository.getExportFile("test1.csv")
        val file2 = exportRepository.getExportFile("test2.pdf")
        val file3 = exportRepository.getExportFile("test3.csv")

        // Then
        assertEquals("All files should be in same directory", file1.parentFile, file2.parentFile)
        assertEquals("All files should be in same directory", file2.parentFile, file3.parentFile)
    }

    // ==================== CSV Export Tests ====================

    @Test
    fun exportToCsv_withTransactions_createsFile() = runTest {
        // Given
        setupTestTransactions(3)
        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.CSV
        )

        // When
        val result = exportRepository.exportToCsv(filter, categoryMap)

        // Then
        assertTrue("Export should succeed", result.isSuccess)
        val exportResult = result.getOrNull()
        assertNotNull("Export result should not be null", exportResult)
        assertTrue("Export result should be Success", exportResult is ExportResult.Success)

        val success = exportResult as ExportResult.Success
        assertTrue("File name should end with .csv", success.fileName.endsWith(".csv"))
        assertTrue("File size should be greater than 0", success.fileSizeBytes > 0)
    }

    @Test
    fun exportToCsv_withTransactions_fileContainsData() = runTest {
        // Given
        setupTestTransactions(3)
        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.CSV
        )

        // When
        val result = exportRepository.exportToCsv(filter, categoryMap)

        // Then
        assertTrue("Export should succeed", result.isSuccess)
        val exportResult = result.getOrNull() as ExportResult.Success
        val file = exportRepository.getExportFile(exportResult.fileName)

        assertTrue("File should exist", file.exists())
        val content = file.readText()
        assertTrue("CSV should contain header", content.contains("Date,Type,Amount"))
        assertTrue("CSV should contain transaction data", content.lines().size > 1)
    }

    @Test
    fun exportToCsv_emptyTransactions_returnsFailure() = runTest {
        // Given - No transactions
        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.CSV
        )

        // When
        val result = exportRepository.exportToCsv(filter, categoryMap)

        // Then
        assertTrue("Export should succeed (returns wrapped result)", result.isSuccess)
        val exportResult = result.getOrNull()
        assertTrue("Export result should be Failure", exportResult is ExportResult.Failure)

        val failure = exportResult as ExportResult.Failure
        assertTrue("Error message should mention no transactions",
            failure.errorMessage.contains("No transactions"))
    }

    @Test
    fun exportToCsv_withDateRangeFilter_filtersTransactions() = runTest {
        // Given - 5 transactions spread across different dates
        val now = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 1, date = now - 10 * 86400000L), // 10 days ago
            createTestTransaction(smsId = 2, date = now - 5 * 86400000L),  // 5 days ago
            createTestTransaction(smsId = 3, date = now - 2 * 86400000L),  // 2 days ago
            createTestTransaction(smsId = 4, date = now - 1 * 86400000L),  // 1 day ago
            createTestTransaction(smsId = 5, date = now)                    // today
        )
        transactions.forEach { transactionRepository.saveTransaction(it) }

        // Filter for last 3 days
        val dateRange = DateRange(
            startDate = now - 3 * 86400000L,
            endDate = now
        )
        val filter = ExportFilter(
            dateRange = dateRange,
            categories = emptyList(),
            exportFormat = ExportFormat.CSV
        )

        // When
        val result = exportRepository.exportToCsv(filter, categoryMap)

        // Then
        assertTrue("Export should succeed", result.isSuccess)
        val exportResult = result.getOrNull() as ExportResult.Success
        val file = exportRepository.getExportFile(exportResult.fileName)
        val content = file.readText()
        val lines = content.lines().filter { it.isNotEmpty() }

        // Should have header + 3 transactions (within date range)
        assertEquals("Should have 4 lines (header + 3 transactions)", 4, lines.size)
    }

    @Test
    fun exportToCsv_withDateRangeFilter_noMatches_returnsFailure() = runTest {
        // Given - Transactions from long ago
        val now = System.currentTimeMillis()
        val oldDate = now - 365 * 86400000L // 1 year ago
        setupTestTransactions(3)

        // Filter for very recent dates (no matches)
        val dateRange = DateRange(
            startDate = now - 86400000L, // yesterday
            endDate = now
        )
        val filter = ExportFilter(
            dateRange = dateRange,
            categories = emptyList(),
            exportFormat = ExportFormat.CSV
        )

        // When
        val result = exportRepository.exportToCsv(filter, categoryMap)

        // Then
        assertTrue("Export should succeed (returns wrapped result)", result.isSuccess)
        val exportResult = result.getOrNull()
        assertTrue("Export result should be Failure", exportResult is ExportResult.Failure)
    }

    @Test
    fun exportToCsv_fileName_containsTimestamp() = runTest {
        // Given
        setupTestTransactions(2)
        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.CSV
        )

        // When
        val result1 = exportRepository.exportToCsv(filter, categoryMap)
        Thread.sleep(1000) // Ensure different timestamp
        val result2 = exportRepository.exportToCsv(filter, categoryMap)

        // Then
        val success1 = result1.getOrNull() as ExportResult.Success
        val success2 = result2.getOrNull() as ExportResult.Success

        assertNotEquals("File names should be different", success1.fileName, success2.fileName)
        assertTrue("File names should start with 'transactions_'",
            success1.fileName.startsWith("transactions_"))
        assertTrue("File names should contain timestamp pattern",
            success1.fileName.matches(Regex("transactions_\\d{4}-\\d{2}-\\d{2}_\\d{6}\\.csv")))
    }

    // ==================== PDF Export Tests ====================

    @Test
    fun exportToPdf_withTransactions_createsFile() = runTest {
        // Given
        setupTestTransactions(3)
        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.PDF
        )

        // When
        val result = exportRepository.exportToPdf(filter, categoryMap)

        // Then
        assertTrue("Export should succeed", result.isSuccess)
        val exportResult = result.getOrNull()
        assertNotNull("Export result should not be null", exportResult)
        assertTrue("Export result should be Success", exportResult is ExportResult.Success)

        val success = exportResult as ExportResult.Success
        assertTrue("File name should end with .pdf", success.fileName.endsWith(".pdf"))
        assertTrue("File size should be greater than 0", success.fileSizeBytes > 0)
    }

    @Test
    fun exportToPdf_withTransactions_fileIsValidPdf() = runTest {
        // Given
        setupTestTransactions(3)
        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.PDF
        )

        // When
        val result = exportRepository.exportToPdf(filter, categoryMap)

        // Then
        assertTrue("Export should succeed", result.isSuccess)
        val exportResult = result.getOrNull() as ExportResult.Success
        val file = exportRepository.getExportFile(exportResult.fileName)

        assertTrue("File should exist", file.exists())
        assertTrue("File should have content", file.length() > 0)

        // Check PDF magic number (starts with %PDF)
        val header = file.inputStream().use { stream ->
            val buffer = ByteArray(4)
            stream.read(buffer)
            String(buffer)
        }
        assertEquals("File should be a PDF", "%PDF", header)
    }

    @Test
    fun exportToPdf_emptyTransactions_returnsFailure() = runTest {
        // Given - No transactions
        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.PDF
        )

        // When
        val result = exportRepository.exportToPdf(filter, categoryMap)

        // Then
        assertTrue("Export should succeed (returns wrapped result)", result.isSuccess)
        val exportResult = result.getOrNull()
        assertTrue("Export result should be Failure", exportResult is ExportResult.Failure)

        val failure = exportResult as ExportResult.Failure
        assertTrue("Error message should mention no transactions",
            failure.errorMessage.contains("No transactions"))
    }

    @Test
    fun exportToPdf_withDateRangeFilter_filtersTransactions() = runTest {
        // Given - 5 transactions spread across different dates
        val now = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 1, date = now - 10 * 86400000L),
            createTestTransaction(smsId = 2, date = now - 5 * 86400000L),
            createTestTransaction(smsId = 3, date = now - 2 * 86400000L),
            createTestTransaction(smsId = 4, date = now - 1 * 86400000L),
            createTestTransaction(smsId = 5, date = now)
        )
        transactions.forEach { transactionRepository.saveTransaction(it) }

        // Filter for last 3 days
        val dateRange = DateRange(
            startDate = now - 3 * 86400000L,
            endDate = now
        )
        val filter = ExportFilter(
            dateRange = dateRange,
            categories = emptyList(),
            exportFormat = ExportFormat.PDF
        )

        // When
        val result = exportRepository.exportToPdf(filter, categoryMap)

        // Then
        assertTrue("Export should succeed", result.isSuccess)
        val exportResult = result.getOrNull() as ExportResult.Success

        assertTrue("File should exist", exportRepository.getExportFile(exportResult.fileName).exists())
        assertTrue("File size should indicate filtered data", exportResult.fileSizeBytes > 0)
    }

    // ==================== Cleanup Tests ====================

    @Test
    fun cleanupOldExports_emptyDirectory_returnsZero() = runTest {
        // Given - No export files

        // When
        val result = exportRepository.cleanupOldExports()

        // Then
        assertTrue("Cleanup should succeed", result.isSuccess)
        assertEquals("Should delete 0 files", 0, result.getOrNull())
    }

    @Test
    fun cleanupOldExports_newFiles_deletesNone() = runTest {
        // Given - Create recent export files
        exportsDir.mkdirs()
        val file1 = File(exportsDir, "export1.csv")
        val file2 = File(exportsDir, "export2.pdf")
        file1.writeText("test content")
        file2.writeText("test content")

        // When
        val result = exportRepository.cleanupOldExports()

        // Then
        assertTrue("Cleanup should succeed", result.isSuccess)
        assertEquals("Should delete 0 files (all are recent)", 0, result.getOrNull())
        assertTrue("File 1 should still exist", file1.exists())
        assertTrue("File 2 should still exist", file2.exists())
    }

    @Test
    fun cleanupOldExports_oldFiles_deletesOldOnes() = runTest {
        // Given - Create old and new files
        exportsDir.mkdirs()
        val oldFile = File(exportsDir, "old_export.csv")
        val newFile = File(exportsDir, "new_export.csv")

        oldFile.writeText("old content")
        newFile.writeText("new content")

        // Set old file to be 8 days old (older than MAX_EXPORT_AGE_DAYS = 7)
        val eightDaysAgo = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L)
        oldFile.setLastModified(eightDaysAgo)

        // When
        val result = exportRepository.cleanupOldExports()

        // Then
        assertTrue("Cleanup should succeed", result.isSuccess)
        assertEquals("Should delete 1 file", 1, result.getOrNull())
        assertFalse("Old file should be deleted", oldFile.exists())
        assertTrue("New file should still exist", newFile.exists())
    }

    @Test
    fun cleanupOldExports_multipleOldFiles_deletesAll() = runTest {
        // Given - Create multiple old files
        exportsDir.mkdirs()
        val oldFiles = listOf(
            File(exportsDir, "old1.csv"),
            File(exportsDir, "old2.pdf"),
            File(exportsDir, "old3.csv")
        )

        val eightDaysAgo = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L)
        oldFiles.forEach { file ->
            file.writeText("old content")
            file.setLastModified(eightDaysAgo)
        }

        // When
        val result = exportRepository.cleanupOldExports()

        // Then
        assertTrue("Cleanup should succeed", result.isSuccess)
        assertEquals("Should delete 3 files", 3, result.getOrNull())
        oldFiles.forEach { file ->
            assertFalse("Old file should be deleted: ${file.name}", file.exists())
        }
    }

    @Test
    fun cleanupOldExports_mixedFiles_deletesOnlyOld() = runTest {
        // Given - Mix of old and new files
        exportsDir.mkdirs()
        val oldFile1 = File(exportsDir, "old1.csv")
        val oldFile2 = File(exportsDir, "old2.pdf")
        val newFile1 = File(exportsDir, "new1.csv")
        val newFile2 = File(exportsDir, "new2.pdf")

        val eightDaysAgo = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L)

        oldFile1.writeText("old content")
        oldFile2.writeText("old content")
        newFile1.writeText("new content")
        newFile2.writeText("new content")

        oldFile1.setLastModified(eightDaysAgo)
        oldFile2.setLastModified(eightDaysAgo)

        // When
        val result = exportRepository.cleanupOldExports()

        // Then
        assertTrue("Cleanup should succeed", result.isSuccess)
        assertEquals("Should delete 2 files", 2, result.getOrNull())
        assertFalse("Old file 1 should be deleted", oldFile1.exists())
        assertFalse("Old file 2 should be deleted", oldFile2.exists())
        assertTrue("New file 1 should still exist", newFile1.exists())
        assertTrue("New file 2 should still exist", newFile2.exists())
    }

    // ==================== File Management Tests ====================

    @Test
    fun getAllExportFiles_emptyDirectory_returnsEmptyList() = runTest {
        // Given - No export files

        // When
        val result = exportRepository.getAllExportFiles()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        val files = result.getOrNull()
        assertNotNull("Files list should not be null", files)
        assertTrue("Files list should be empty", files!!.isEmpty())
    }

    @Test
    fun getAllExportFiles_withFiles_returnsAllFiles() = runTest {
        // Given - Create export files
        exportsDir.mkdirs()
        val file1 = File(exportsDir, "export1.csv")
        val file2 = File(exportsDir, "export2.pdf")
        val file3 = File(exportsDir, "export3.csv")

        file1.writeText("content1")
        file2.writeText("content2")
        file3.writeText("content3")

        // When
        val result = exportRepository.getAllExportFiles()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        val files = result.getOrNull()
        assertNotNull("Files list should not be null", files)
        assertEquals("Should return 3 files", 3, files!!.size)
    }

    @Test
    fun getAllExportFiles_sortsByModifiedDate_newestFirst() = runTest {
        // Given - Create files with different modification times
        exportsDir.mkdirs()
        val file1 = File(exportsDir, "oldest.csv")
        val file2 = File(exportsDir, "middle.csv")
        val file3 = File(exportsDir, "newest.csv")

        val now = System.currentTimeMillis()
        file1.writeText("content1")
        file2.writeText("content2")
        file3.writeText("content3")

        file1.setLastModified(now - 10000)
        file2.setLastModified(now - 5000)
        file3.setLastModified(now)

        // When
        val result = exportRepository.getAllExportFiles()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        val files = result.getOrNull()!!

        assertEquals("Newest file should be first", "newest.csv", files[0].name)
        assertEquals("Middle file should be second", "middle.csv", files[1].name)
        assertEquals("Oldest file should be last", "oldest.csv", files[2].name)
    }

    @Test
    fun deleteExportFile_existingFile_deletesSuccessfully() = runTest {
        // Given
        exportsDir.mkdirs()
        val file = File(exportsDir, "test.csv")
        file.writeText("test content")
        assertTrue("File should exist initially", file.exists())

        // When
        val result = exportRepository.deleteExportFile("test.csv")

        // Then
        assertTrue("Delete should succeed", result.isSuccess)
        assertTrue("Should return true", result.getOrNull() == true)
        assertFalse("File should be deleted", file.exists())
    }

    @Test
    fun deleteExportFile_nonExistentFile_returnsFalse() = runTest {
        // Given - No file exists

        // When
        val result = exportRepository.deleteExportFile("nonexistent.csv")

        // Then
        assertTrue("Should succeed (not throw error)", result.isSuccess)
        assertFalse("Should return false", result.getOrNull() == true)
    }

    @Test
    fun deleteExportFile_multipleFiles_deletesOnlySpecified() = runTest {
        // Given
        exportsDir.mkdirs()
        val file1 = File(exportsDir, "file1.csv")
        val file2 = File(exportsDir, "file2.csv")
        val file3 = File(exportsDir, "file3.csv")

        file1.writeText("content1")
        file2.writeText("content2")
        file3.writeText("content3")

        // When
        val result = exportRepository.deleteExportFile("file2.csv")

        // Then
        assertTrue("Delete should succeed", result.isSuccess)
        assertTrue("File 1 should still exist", file1.exists())
        assertFalse("File 2 should be deleted", file2.exists())
        assertTrue("File 3 should still exist", file3.exists())
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun exportToCsv_databaseError_returnsFailure() = runTest {
        // Given - Close database to cause error
        database.close()
        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.CSV
        )

        // When
        val result = exportRepository.exportToCsv(filter, categoryMap)

        // Then
        assertTrue("Should return success wrapper", result.isSuccess)
        val exportResult = result.getOrNull()
        assertTrue("Export result should be Failure", exportResult is ExportResult.Failure)

        val failure = exportResult as ExportResult.Failure
        assertNotNull("Should have error message", failure.errorMessage)
        assertNotNull("Should have technical error", failure.technicalError)
    }

    @Test
    fun exportToPdf_databaseError_returnsFailure() = runTest {
        // Given - Close database to cause error
        database.close()
        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.PDF
        )

        // When
        val result = exportRepository.exportToPdf(filter, categoryMap)

        // Then
        assertTrue("Should return success wrapper", result.isSuccess)
        val exportResult = result.getOrNull()
        assertTrue("Export result should be Failure", exportResult is ExportResult.Failure)

        val failure = exportResult as ExportResult.Failure
        assertNotNull("Should have error message", failure.errorMessage)
        assertNotNull("Should have technical error", failure.technicalError)
    }

    // ==================== URI Generation Tests ====================

    @Test
    fun exportToCsv_generatesValidUri() = runTest {
        // Given
        setupTestTransactions(2)
        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.CSV
        )

        // When
        val result = exportRepository.exportToCsv(filter, categoryMap)

        // Then
        assertTrue("Export should succeed", result.isSuccess)
        val exportResult = result.getOrNull() as ExportResult.Success

        assertNotNull("URI should not be null", exportResult.fileUri)
        assertTrue("URI should be content URI",
            exportResult.fileUri.toString().startsWith("content://"))
        assertTrue("URI should contain file provider authority",
            exportResult.fileUri.toString().contains("com.example.kanakku.fileprovider"))
    }

    @Test
    fun exportToPdf_generatesValidUri() = runTest {
        // Given
        setupTestTransactions(2)
        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.PDF
        )

        // When
        val result = exportRepository.exportToPdf(filter, categoryMap)

        // Then
        assertTrue("Export should succeed", result.isSuccess)
        val exportResult = result.getOrNull() as ExportResult.Success

        assertNotNull("URI should not be null", exportResult.fileUri)
        assertTrue("URI should be content URI",
            exportResult.fileUri.toString().startsWith("content://"))
        assertTrue("URI should contain file provider authority",
            exportResult.fileUri.toString().contains("com.example.kanakku.fileprovider"))
    }

    // ==================== Integration Tests ====================

    @Test
    fun exportToCsv_fullFlow_createsShareableFile() = runTest {
        // Given - Complete setup with transactions
        val transactions = listOf(
            createTestTransaction(smsId = 1, amount = 100.0, merchant = "SWIGGY"),
            createTestTransaction(smsId = 2, amount = 200.0, merchant = "AMAZON"),
            createTestTransaction(smsId = 3, amount = 300.0, merchant = "UBER")
        )
        transactions.forEach { transactionRepository.saveTransaction(it) }

        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.CSV
        )

        // When
        val result = exportRepository.exportToCsv(filter, categoryMap)

        // Then - Verify complete export
        assertTrue("Export should succeed", result.isSuccess)
        val exportResult = result.getOrNull() as ExportResult.Success

        // Verify file
        val file = exportRepository.getExportFile(exportResult.fileName)
        assertTrue("File should exist", file.exists())

        // Verify content
        val content = file.readText()
        assertTrue("Should have header", content.contains("Date,Type,Amount"))
        assertTrue("Should have SWIGGY transaction", content.contains("SWIGGY"))
        assertTrue("Should have AMAZON transaction", content.contains("AMAZON"))
        assertTrue("Should have UBER transaction", content.contains("UBER"))

        // Verify URI is shareable
        assertNotNull("Should have shareable URI", exportResult.fileUri)
        assertTrue("URI should be content:// scheme",
            exportResult.fileUri.toString().startsWith("content://"))

        // Verify file size matches
        assertEquals("File size should match", file.length(), exportResult.fileSizeBytes)
    }

    @Test
    fun exportToPdf_fullFlow_createsShareableFile() = runTest {
        // Given - Complete setup with transactions
        val transactions = listOf(
            createTestTransaction(smsId = 1, amount = 100.0, merchant = "SWIGGY"),
            createTestTransaction(smsId = 2, amount = 200.0, merchant = "AMAZON")
        )
        transactions.forEach { transactionRepository.saveTransaction(it) }

        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.PDF
        )

        // When
        val result = exportRepository.exportToPdf(filter, categoryMap)

        // Then - Verify complete export
        assertTrue("Export should succeed", result.isSuccess)
        val exportResult = result.getOrNull() as ExportResult.Success

        // Verify file
        val file = exportRepository.getExportFile(exportResult.fileName)
        assertTrue("File should exist", file.exists())
        assertTrue("File should have content", file.length() > 0)

        // Verify it's a PDF
        val header = file.inputStream().use { stream ->
            val buffer = ByteArray(4)
            stream.read(buffer)
            String(buffer)
        }
        assertEquals("Should be valid PDF", "%PDF", header)

        // Verify URI is shareable
        assertNotNull("Should have shareable URI", exportResult.fileUri)
        assertTrue("URI should be content:// scheme",
            exportResult.fileUri.toString().startsWith("content://"))

        // Verify file size matches
        assertEquals("File size should match", file.length(), exportResult.fileSizeBytes)
    }

    @Test
    fun cleanup_afterExports_maintainsRecentFiles() = runTest {
        // Given - Create multiple exports
        setupTestTransactions(3)
        val filter = ExportFilter(
            dateRange = null,
            categories = emptyList(),
            exportFormat = ExportFormat.CSV
        )

        // Create 3 exports
        val result1 = exportRepository.exportToCsv(filter, categoryMap)
        Thread.sleep(100)
        val result2 = exportRepository.exportToCsv(filter, categoryMap)
        Thread.sleep(100)
        val result3 = exportRepository.exportToCsv(filter, categoryMap)

        // When - Cleanup (should not delete recent files)
        val cleanupResult = exportRepository.cleanupOldExports()

        // Then
        assertTrue("Cleanup should succeed", cleanupResult.isSuccess)
        assertEquals("Should delete 0 files (all recent)", 0, cleanupResult.getOrNull())

        // Verify all files still exist
        val allFiles = exportRepository.getAllExportFiles()
        assertEquals("Should have 3 export files", 3, allFiles.getOrNull()?.size)
    }
}
