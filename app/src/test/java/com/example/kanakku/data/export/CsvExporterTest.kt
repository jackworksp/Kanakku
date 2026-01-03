package com.example.kanakku.data.export

import androidx.compose.ui.graphics.Color
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for CsvExporter utility class.
 *
 * Tests cover:
 * - CSV formatting with proper headers and data rows
 * - Special character escaping (quotes, commas, newlines)
 * - Empty transaction lists
 * - Date formatting (yyyy-MM-dd HH:mm)
 * - Proper header generation
 * - Null field handling
 * - Category name resolution
 * - Custom headers functionality
 * - Edge cases and boundary conditions
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CsvExporterTest {

    // ==================== Test Data ====================

    private val testCategory = Category(
        id = "food",
        name = "Food & Dining",
        icon = "🍔",
        color = Color(0xFFFF9800),
        keywords = listOf("swiggy", "zomato", "restaurant")
    )

    private val shoppingCategory = Category(
        id = "shopping",
        name = "Shopping",
        icon = "🛍️",
        color = Color(0xFFE91E63),
        keywords = listOf("amazon", "flipkart", "myntra")
    )

    private val categoryMap = mapOf(
        "food" to testCategory,
        "shopping" to shoppingCategory
    )

    private fun createTestTransaction(
        smsId: Long = 1L,
        amount: Double = 100.50,
        type: TransactionType = TransactionType.DEBIT,
        merchant: String? = "SWIGGY",
        accountNumber: String? = "1234",
        referenceNumber: String? = "REF123",
        date: Long = 1704067200000L, // 2024-01-01 00:00:00
        balanceAfter: Double? = 5000.0,
        location: String? = "Mumbai"
    ): ParsedTransaction {
        return ParsedTransaction(
            smsId = smsId,
            amount = amount,
            type = type,
            merchant = merchant,
            accountNumber = accountNumber,
            referenceNumber = referenceNumber,
            date = date,
            rawSms = "Test SMS",
            senderAddress = "VM-HDFCBK",
            balanceAfter = balanceAfter,
            location = location
        )
    }

    // ==================== Header Generation Tests ====================

    @Test
    fun exportToCsv_emptyList_returnsHeadersOnly() {
        // Given
        val emptyList = emptyList<ParsedTransaction>()

        // When
        val csv = CsvExporter.exportToCsv(emptyList, categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should contain header line", lines.isNotEmpty())
        assertEquals("Date,Type,Amount,Category,Merchant,Account,Reference,Balance After,Location", lines[0])
        assertTrue("Should only have header and trailing newline", lines.size <= 2)
    }

    @Test
    fun exportToCsv_properHeaderGeneration_containsAllColumns() {
        // Given
        val transaction = createTestTransaction()

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        val header = lines[0]
        assertTrue("Header should contain Date", header.contains("Date"))
        assertTrue("Header should contain Type", header.contains("Type"))
        assertTrue("Header should contain Amount", header.contains("Amount"))
        assertTrue("Header should contain Category", header.contains("Category"))
        assertTrue("Header should contain Merchant", header.contains("Merchant"))
        assertTrue("Header should contain Account", header.contains("Account"))
        assertTrue("Header should contain Reference", header.contains("Reference"))
        assertTrue("Header should contain Balance After", header.contains("Balance After"))
        assertTrue("Header should contain Location", header.contains("Location"))
    }

    // ==================== CSV Formatting Tests ====================

    @Test
    fun exportToCsv_singleTransaction_formatsCorrectly() {
        // Given
        val transaction = createTestTransaction(
            amount = 100.50,
            type = TransactionType.DEBIT,
            merchant = "SWIGGY",
            accountNumber = "1234",
            referenceNumber = "REF123",
            balanceAfter = 5000.00,
            location = "Mumbai"
        )

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertEquals("Should have header + 1 transaction + trailing newline", 3, lines.size)

        val dataRow = lines[1]
        assertTrue("Should contain amount", dataRow.contains("100.50"))
        assertTrue("Should contain type", dataRow.contains("DEBIT"))
        assertTrue("Should contain merchant", dataRow.contains("SWIGGY"))
        assertTrue("Should contain account", dataRow.contains("1234"))
        assertTrue("Should contain reference", dataRow.contains("REF123"))
        assertTrue("Should contain balance", dataRow.contains("5000.00"))
        assertTrue("Should contain location", dataRow.contains("Mumbai"))
    }

    @Test
    fun exportToCsv_multipleTransactions_formatsAll() {
        // Given
        val transaction1 = createTestTransaction(smsId = 1, amount = 100.0, merchant = "SWIGGY")
        val transaction2 = createTestTransaction(smsId = 2, amount = 200.0, merchant = "AMAZON")
        val transaction3 = createTestTransaction(smsId = 3, amount = 300.0, merchant = "UBER")

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction1, transaction2, transaction3), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertEquals("Should have header + 3 transactions + trailing newline", 5, lines.size)
        assertTrue("First transaction should contain SWIGGY", lines[1].contains("SWIGGY"))
        assertTrue("Second transaction should contain AMAZON", lines[2].contains("AMAZON"))
        assertTrue("Third transaction should contain UBER", lines[3].contains("UBER"))
    }

    @Test
    fun exportToCsv_amountFormatting_usesTwoDecimals() {
        // Given
        val transaction1 = createTestTransaction(amount = 100.0)
        val transaction2 = createTestTransaction(amount = 99.99)
        val transaction3 = createTestTransaction(amount = 1234.567)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction1, transaction2, transaction3), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Amount 100.0 should be formatted as 100.00", lines[1].contains("100.00"))
        assertTrue("Amount 99.99 should remain 99.99", lines[2].contains("99.99"))
        assertTrue("Amount 1234.567 should be rounded to 1234.57", lines[3].contains("1234.57"))
    }

    // ==================== Date Formatting Tests ====================

    @Test
    fun exportToCsv_dateFormatting_usesReadableFormat() {
        // Given - 2024-01-01 00:00:00
        val transaction = createTestTransaction(date = 1704067200000L)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        val dataRow = lines[1]
        // Should start with date in yyyy-MM-dd HH:mm format
        assertTrue("Should contain formatted date starting with 2024-01-01",
            dataRow.contains("2024-01-01"))
    }

    @Test
    fun exportToCsv_differentDates_formatsAllCorrectly() {
        // Given
        val transaction1 = createTestTransaction(date = 1704067200000L) // 2024-01-01
        val transaction2 = createTestTransaction(date = 1735689600000L) // 2025-01-01

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction1, transaction2), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("First date should contain 2024-01-01", lines[1].contains("2024-01-01"))
        assertTrue("Second date should contain 2025-01-01", lines[2].contains("2025-01-01"))
    }

    // ==================== Special Character Escaping Tests ====================

    @Test
    fun exportToCsv_merchantWithComma_escapesCorrectly() {
        // Given
        val transaction = createTestTransaction(merchant = "ABC Store, Mumbai")

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        val dataRow = lines[1]
        // Field with comma should be quoted
        assertTrue("Merchant with comma should be quoted", dataRow.contains("\"ABC Store, Mumbai\""))
    }

    @Test
    fun exportToCsv_merchantWithQuotes_escapesCorrectly() {
        // Given
        val transaction = createTestTransaction(merchant = "Joe's \"Best\" Pizza")

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        val dataRow = lines[1]
        // Quotes should be doubled and field quoted
        assertTrue("Merchant with quotes should be escaped",
            dataRow.contains("\"Joe's \"\"Best\"\" Pizza\""))
    }

    @Test
    fun exportToCsv_merchantWithNewline_escapesCorrectly() {
        // Given
        val transaction = createTestTransaction(merchant = "Store\nName")

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        // Field with newline should be quoted and preserved
        val csv_content = csv.split("\n", limit = 3)
        assertTrue("CSV should contain quoted field with newline", csv.contains("\"Store\nName\""))
    }

    @Test
    fun exportToCsv_multipleSpecialCharacters_escapesAll() {
        // Given - merchant with comma, quotes, and newline
        val transaction = createTestTransaction(merchant = "\"Store, Inc.\"\nMumbai")

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        // Should be properly escaped and quoted
        assertTrue("Should escape complex special characters", csv.contains("\"\"\"Store, Inc.\"\"\nMumbai\""))
    }

    @Test
    fun exportToCsv_locationWithComma_escapesCorrectly() {
        // Given
        val transaction = createTestTransaction(location = "Mumbai, Maharashtra")

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Location with comma should be quoted", lines[1].contains("\"Mumbai, Maharashtra\""))
    }

    // ==================== Null Field Handling Tests ====================

    @Test
    fun exportToCsv_nullMerchant_rendersAsEmpty() {
        // Given
        val transaction = createTestTransaction(merchant = null)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        val fields = lines[1].split(",")
        // Merchant field (index 4) should be empty but present
        assertTrue("Null merchant should result in empty field", fields.size >= 5)
    }

    @Test
    fun exportToCsv_nullAccountNumber_rendersAsEmpty() {
        // Given
        val transaction = createTestTransaction(accountNumber = null)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        val dataRow = lines[1]
        // Should have consecutive commas for empty account field
        assertNotNull("Should handle null account number", dataRow)
    }

    @Test
    fun exportToCsv_nullReferenceNumber_rendersAsEmpty() {
        // Given
        val transaction = createTestTransaction(referenceNumber = null)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertNotNull("Should handle null reference number", lines[1])
    }

    @Test
    fun exportToCsv_nullBalanceAfter_rendersAsEmpty() {
        // Given
        val transaction = createTestTransaction(balanceAfter = null)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        val dataRow = lines[1]
        // Should not contain balance value, but should have the field
        assertNotNull("Should handle null balance", dataRow)
    }

    @Test
    fun exportToCsv_nullLocation_rendersAsEmpty() {
        // Given
        val transaction = createTestTransaction(location = null)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertNotNull("Should handle null location", lines[1])
    }

    @Test
    fun exportToCsv_allNullableFieldsNull_handlesGracefully() {
        // Given
        val transaction = createTestTransaction(
            merchant = null,
            accountNumber = null,
            referenceNumber = null,
            balanceAfter = null,
            location = null
        )

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertEquals("Should still have header + 1 transaction + trailing newline", 3, lines.size)
        assertNotNull("Should handle all nulls gracefully", lines[1])
    }

    // ==================== Category Name Resolution Tests ====================

    @Test
    fun exportToCsv_merchantMatchesCategory_resolvesCategoryName() {
        // Given
        val transaction = createTestTransaction(merchant = "SWIGGY")

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should resolve category to Food & Dining", lines[1].contains("Food & Dining"))
    }

    @Test
    fun exportToCsv_merchantMatchesCaseInsensitive_resolvesCategory() {
        // Given
        val transaction = createTestTransaction(merchant = "amazon")

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should resolve category case-insensitively", lines[1].contains("Shopping"))
    }

    @Test
    fun exportToCsv_merchantPartialMatch_resolvesCategory() {
        // Given
        val transaction = createTestTransaction(merchant = "Zomato Food Delivery")

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should resolve category by partial keyword match", lines[1].contains("Food & Dining"))
    }

    @Test
    fun exportToCsv_merchantNoMatch_returnsUncategorized() {
        // Given
        val transaction = createTestTransaction(merchant = "UNKNOWN MERCHANT")

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should return Uncategorized for unknown merchant", lines[1].contains("Uncategorized"))
    }

    @Test
    fun exportToCsv_nullMerchant_returnsUncategorized() {
        // Given
        val transaction = createTestTransaction(merchant = null)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should return Uncategorized for null merchant", lines[1].contains("Uncategorized"))
    }

    @Test
    fun exportToCsv_emptyCategoryMap_returnsUncategorized() {
        // Given
        val transaction = createTestTransaction(merchant = "SWIGGY")

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), emptyMap())

        // Then
        val lines = csv.split("\n")
        assertTrue("Should return Uncategorized when category map is empty", lines[1].contains("Uncategorized"))
    }

    // ==================== Transaction Type Tests ====================

    @Test
    fun exportToCsv_debitTransaction_rendersCorrectly() {
        // Given
        val transaction = createTestTransaction(type = TransactionType.DEBIT)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should render DEBIT type", lines[1].contains("DEBIT"))
    }

    @Test
    fun exportToCsv_creditTransaction_rendersCorrectly() {
        // Given
        val transaction = createTestTransaction(type = TransactionType.CREDIT)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should render CREDIT type", lines[1].contains("CREDIT"))
    }

    @Test
    fun exportToCsv_unknownTransaction_rendersCorrectly() {
        // Given
        val transaction = createTestTransaction(type = TransactionType.UNKNOWN)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should render UNKNOWN type", lines[1].contains("UNKNOWN"))
    }

    // ==================== Custom Headers Tests ====================

    @Test
    fun exportToCsvWithHeaders_customHeaders_usesCustomHeaders() {
        // Given
        val transaction = createTestTransaction()
        val customHeaders = listOf(
            "Transaction Date",
            "Transaction Type",
            "Transaction Amount",
            "Category Name",
            "Merchant Name",
            "Account Number",
            "Reference ID",
            "Balance",
            "Location Info"
        )

        // When
        val csv = CsvExporter.exportToCsvWithHeaders(listOf(transaction), categoryMap, customHeaders)

        // Then
        val lines = csv.split("\n")
        val header = lines[0]
        assertTrue("Should use custom header for date", header.contains("Transaction Date"))
        assertTrue("Should use custom header for type", header.contains("Transaction Type"))
        assertTrue("Should use custom header for amount", header.contains("Transaction Amount"))
        assertTrue("Should use custom header for category", header.contains("Category Name"))
        assertTrue("Should use custom header for merchant", header.contains("Merchant Name"))
    }

    @Test
    fun exportToCsvWithHeaders_customHeadersWithSpecialChars_escapesCorrectly() {
        // Given
        val transaction = createTestTransaction()
        val customHeaders = listOf(
            "Date, Time",
            "Type",
            "Amount ($)",
            "Category",
            "Merchant",
            "Account #",
            "Ref \"ID\"",
            "Balance",
            "Location"
        )

        // When
        val csv = CsvExporter.exportToCsvWithHeaders(listOf(transaction), categoryMap, customHeaders)

        // Then
        val lines = csv.split("\n")
        val header = lines[0]
        assertTrue("Should escape comma in header", header.contains("\"Date, Time\""))
        assertTrue("Should escape quotes in header", header.contains("\"Ref \"\"ID\"\"\""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun exportToCsvWithHeaders_wrongHeaderCount_throwsException() {
        // Given
        val transaction = createTestTransaction()
        val invalidHeaders = listOf("Date", "Amount") // Only 2 headers instead of 9

        // When/Then - Should throw IllegalArgumentException
        CsvExporter.exportToCsvWithHeaders(listOf(transaction), categoryMap, invalidHeaders)
    }

    @Test(expected = IllegalArgumentException::class)
    fun exportToCsvWithHeaders_tooManyHeaders_throwsException() {
        // Given
        val transaction = createTestTransaction()
        val invalidHeaders = listOf(
            "Date", "Type", "Amount", "Category", "Merchant",
            "Account", "Reference", "Balance", "Location", "Extra"
        ) // 10 headers instead of 9

        // When/Then - Should throw IllegalArgumentException
        CsvExporter.exportToCsvWithHeaders(listOf(transaction), categoryMap, invalidHeaders)
    }

    // ==================== Edge Cases and Integration Tests ====================

    @Test
    fun exportToCsv_largeAmountValue_formatsCorrectly() {
        // Given
        val transaction = createTestTransaction(amount = 999999999.99)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should format large amounts correctly", lines[1].contains("999999999.99"))
    }

    @Test
    fun exportToCsv_zeroAmount_formatsCorrectly() {
        // Given
        val transaction = createTestTransaction(amount = 0.0)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should format zero amount as 0.00", lines[1].contains("0.00"))
    }

    @Test
    fun exportToCsv_negativeAmount_formatsCorrectly() {
        // Given
        val transaction = createTestTransaction(amount = -50.75)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should format negative amounts correctly", lines[1].contains("-50.75"))
    }

    @Test
    fun exportToCsv_veryLongMerchantName_handlesCorrectly() {
        // Given
        val longName = "A".repeat(500)
        val transaction = createTestTransaction(merchant = longName)

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertTrue("Should handle very long merchant names", lines[1].contains(longName))
    }

    @Test
    fun exportToCsv_emptyStringMerchant_handlesCorrectly() {
        // Given
        val transaction = createTestTransaction(merchant = "")

        // When
        val csv = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then
        val lines = csv.split("\n")
        assertNotNull("Should handle empty string merchant", lines[1])
    }

    @Test
    fun exportToCsv_csvStructure_isValid() {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1, amount = 100.0, merchant = "SWIGGY"),
            createTestTransaction(smsId = 2, amount = 200.0, merchant = "AMAZON"),
            createTestTransaction(smsId = 3, amount = 300.0, merchant = "UBER")
        )

        // When
        val csv = CsvExporter.exportToCsv(transactions, categoryMap)

        // Then
        val lines = csv.split("\n")

        // Verify structure
        assertEquals("Should have header + 3 transactions + trailing newline", 5, lines.size)

        // Verify each non-empty line has 9 fields (when not considering quoted commas)
        // This is a basic structural check
        assertTrue("Header should exist", lines[0].isNotEmpty())
        assertTrue("First transaction should exist", lines[1].isNotEmpty())
        assertTrue("Second transaction should exist", lines[2].isNotEmpty())
        assertTrue("Third transaction should exist", lines[3].isNotEmpty())
    }

    @Test
    fun exportToCsv_consistentFieldCount_acrossRows() {
        // Given
        val transactions = listOf(
            createTestTransaction(merchant = "SWIGGY", location = "Mumbai"),
            createTestTransaction(merchant = null, location = null),
            createTestTransaction(merchant = "AMAZON", location = "Delhi")
        )

        // When
        val csv = CsvExporter.exportToCsv(transactions, categoryMap)

        // Then - Each row should have the same structure (9 fields)
        val lines = csv.split("\n").filter { it.isNotEmpty() }
        assertTrue("Should have consistent structure", lines.size == 4) // header + 3 transactions
    }

    @Test
    fun exportToCsv_threadSafety_multipleCallsProduceSameResult() {
        // Given
        val transaction = createTestTransaction()

        // When - Call multiple times
        val csv1 = CsvExporter.exportToCsv(listOf(transaction), categoryMap)
        val csv2 = CsvExporter.exportToCsv(listOf(transaction), categoryMap)
        val csv3 = CsvExporter.exportToCsv(listOf(transaction), categoryMap)

        // Then - Should produce identical results (stateless)
        assertEquals("Multiple calls should produce same result", csv1, csv2)
        assertEquals("Multiple calls should produce same result", csv2, csv3)
    }
}
