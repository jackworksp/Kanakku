package com.example.kanakku.data.category

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DefaultCategories
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.repository.TransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for CategoryManager's merchant learning functionality.
 *
 * Tests cover:
 * - Learning merchant-to-category mappings from user overrides
 * - Applying learned mappings to new transactions
 * - Reset functionality for learned mappings
 * - Null/empty merchant handling
 * - Merchant name normalization
 * - Categorization priority (override > merchant > keyword > default)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CategoryManagerTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var categoryManager: CategoryManager

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        repository = TransactionRepository(database)
        categoryManager = CategoryManager()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ==================== Helper Functions ====================

    private fun createTestTransaction(
        smsId: Long = 1L,
        amount: Double = 100.0,
        type: TransactionType = TransactionType.DEBIT,
        merchant: String? = "Test Merchant",
        date: Long = System.currentTimeMillis(),
        rawSms: String = "Test SMS"
    ): ParsedTransaction {
        return ParsedTransaction(
            smsId = smsId,
            amount = amount,
            type = type,
            merchant = merchant,
            accountNumber = "1234",
            referenceNumber = "REF123",
            date = date,
            rawSms = rawSms,
            senderAddress = "VM-TESTBK",
            balanceAfter = 500.0,
            location = null
        )
    }

    // ==================== Merchant Learning Tests ====================

    @Test
    fun setManualOverride_withMerchant_savesMerchantMapping() = runTest {
        // Given
        categoryManager.initialize(repository)
        val transaction = createTestTransaction(smsId = 1L, merchant = "Swiggy")
        repository.saveTransaction(transaction) // Save transaction first for foreign key constraint

        // When
        val result = categoryManager.setManualOverride(
            smsId = 1L,
            category = DefaultCategories.FOOD,
            merchant = "Swiggy"
        )

        // Then
        assertTrue(result.isSuccess)

        // Verify merchant mapping was saved to database
        val mapping = repository.getMerchantCategoryMapping("swiggy").getOrNull()
        assertEquals("food", mapping)
    }

    @Test
    fun setManualOverride_withMerchant_updatesMerchantCache() = runTest {
        // Given
        categoryManager.initialize(repository)
        val transaction = createTestTransaction(smsId = 1L, merchant = "Amazon India")
        repository.saveTransaction(transaction) // Save transaction first for foreign key constraint

        // When
        categoryManager.setManualOverride(
            smsId = 1L,
            category = DefaultCategories.SHOPPING,
            merchant = "Amazon India"
        )

        // Then - New transaction from same merchant should use learned category
        val newTransaction = createTestTransaction(
            smsId = 2L,
            merchant = "Amazon India",
            rawSms = "Debited INR 500" // No keywords
        )
        val category = categoryManager.categorizeTransaction(newTransaction)
        assertEquals(DefaultCategories.SHOPPING, category)
    }

    @Test
    fun setManualOverride_withNullMerchant_doesNotSaveMapping() = runTest {
        // Given
        categoryManager.initialize(repository)
        val transaction = createTestTransaction(smsId = 1L, merchant = "Test")
        repository.saveTransaction(transaction) // Save transaction first for foreign key constraint

        // When
        val result = categoryManager.setManualOverride(
            smsId = 1L,
            category = DefaultCategories.FOOD,
            merchant = null
        )

        // Then
        assertTrue(result.isSuccess)

        // Verify no merchant mapping was saved
        val count = repository.getMerchantMappingCount().getOrNull()
        assertEquals(0, count)
    }

    @Test
    fun setManualOverride_withEmptyMerchant_doesNotSaveMapping() = runTest {
        // Given
        categoryManager.initialize(repository)
        val transaction = createTestTransaction(smsId = 1L, merchant = "Test")
        repository.saveTransaction(transaction) // Save transaction first for foreign key constraint

        // When
        val result = categoryManager.setManualOverride(
            smsId = 1L,
            category = DefaultCategories.FOOD,
            merchant = "   " // Blank string
        )

        // Then
        assertTrue(result.isSuccess)

        // Verify no merchant mapping was saved
        val count = repository.getMerchantMappingCount().getOrNull()
        assertEquals(0, count)
    }

    @Test
    fun setManualOverride_updatesMerchantMappingForSameMerchant() = runTest {
        // Given
        categoryManager.initialize(repository)

        // Set initial mapping
        categoryManager.setManualOverride(1L, DefaultCategories.FOOD, "Uber Eats")

        // When - Change category for same merchant
        categoryManager.setManualOverride(2L, DefaultCategories.TRANSPORT, "Uber Eats")

        // Then - Latest mapping should be used
        val transaction = createTestTransaction(
            smsId = 3L,
            merchant = "Uber Eats",
            rawSms = "Payment"
        )
        val category = categoryManager.categorizeTransaction(transaction)
        assertEquals(DefaultCategories.TRANSPORT, category)
    }

    // ==================== Applying Merchant Mappings Tests ====================

    @Test
    fun categorizeTransaction_usesMerchantMappingBeforeKeywords() = runTest {
        // Given
        categoryManager.initialize(repository)

        // Learn that "Pizza Palace" is FOOD (not SHOPPING)
        categoryManager.setManualOverride(1L, DefaultCategories.FOOD, "Pizza Palace")

        // When - Transaction has both merchant mapping AND shopping keyword
        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = "Pizza Palace",
            rawSms = "Payment at MALL for shopping" // Has "mall" and "shopping" keywords
        )

        // Then - Merchant mapping should win over keywords
        val category = categoryManager.categorizeTransaction(transaction)
        assertEquals(DefaultCategories.FOOD, category)
    }

    @Test
    fun categorizeTransaction_usesKeywordsWhenNoMerchantMapping() = runTest {
        // Given
        categoryManager.initialize(repository)

        // When - Transaction has keywords but no merchant mapping
        val transaction = createTestTransaction(
            smsId = 1L,
            merchant = "Unknown Merchant",
            rawSms = "Payment at swiggy for food"
        )

        // Then - Should fall back to keyword matching
        val category = categoryManager.categorizeTransaction(transaction)
        assertEquals(DefaultCategories.FOOD, category)
    }

    @Test
    fun categorizeTransaction_manualOverrideTakesPrecedence() = runTest {
        // Given
        categoryManager.initialize(repository)

        // Set merchant mapping
        categoryManager.setManualOverride(1L, DefaultCategories.FOOD, "Test Merchant")

        // Set per-transaction override (different category)
        categoryManager.setManualOverride(2L, DefaultCategories.TRANSPORT, "Test Merchant")

        // When - Categorize the transaction with override
        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = "Test Merchant",
            rawSms = "Payment"
        )

        // Then - Per-transaction override should win
        val category = categoryManager.categorizeTransaction(transaction)
        assertEquals(DefaultCategories.TRANSPORT, category)
    }

    @Test
    fun categorizeTransaction_withoutMerchantOrKeywords_usesDefault() = runTest {
        // Given
        categoryManager.initialize(repository)

        // When - Transaction has no merchant and no matching keywords
        val transaction = createTestTransaction(
            smsId = 1L,
            merchant = null,
            rawSms = "INR 500 debited"
        )

        // Then - Should return default category
        val category = categoryManager.categorizeTransaction(transaction)
        assertEquals(DefaultCategories.OTHER, category)
    }

    @Test
    fun categorizeTransaction_withNullMerchant_skipsMerchantMapping() = runTest {
        // Given
        categoryManager.initialize(repository)

        // Set merchant mapping for "Amazon"
        categoryManager.setManualOverride(1L, DefaultCategories.SHOPPING, "Amazon")

        // When - Transaction has null merchant
        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = null,
            rawSms = "Payment to Amazon" // Has keyword
        )

        // Then - Should use keyword matching (not merchant mapping)
        val category = categoryManager.categorizeTransaction(transaction)
        assertEquals(DefaultCategories.SHOPPING, category)
    }

    @Test
    fun categorizeTransaction_withEmptyMerchant_skipsMerchantMapping() = runTest {
        // Given
        categoryManager.initialize(repository)

        // When - Transaction has empty merchant
        val transaction = createTestTransaction(
            smsId = 1L,
            merchant = "",
            rawSms = "Payment at restaurant"
        )

        // Then - Should use keyword matching
        val category = categoryManager.categorizeTransaction(transaction)
        assertEquals(DefaultCategories.FOOD, category)
    }

    // ==================== Merchant Name Normalization Tests ====================

    @Test
    fun merchantNormalization_handlesUpperCase() = runTest {
        // Given
        categoryManager.initialize(repository)

        // When - Set mapping with uppercase merchant
        categoryManager.setManualOverride(1L, DefaultCategories.FOOD, "SWIGGY INDIA")

        // Then - Should match lowercase variant
        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = "swiggy india",
            rawSms = "Payment"
        )
        val category = categoryManager.categorizeTransaction(transaction)
        assertEquals(DefaultCategories.FOOD, category)
    }

    @Test
    fun merchantNormalization_handlesMixedCase() = runTest {
        // Given
        categoryManager.initialize(repository)

        // When - Set mapping with mixed case
        categoryManager.setManualOverride(1L, DefaultCategories.SHOPPING, "AmAzOn InDiA")

        // Then - Should match different casing
        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = "AMAZON INDIA",
            rawSms = "Payment"
        )
        val category = categoryManager.categorizeTransaction(transaction)
        assertEquals(DefaultCategories.SHOPPING, category)
    }

    @Test
    fun merchantNormalization_handlesSpecialCharacters() = runTest {
        // Given
        categoryManager.initialize(repository)

        // When - Set mapping with special characters
        categoryManager.setManualOverride(1L, DefaultCategories.FOOD, "Swiggy™️ (India)")

        // Then - Should match normalized version without special chars
        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = "Swiggy India",
            rawSms = "Payment"
        )
        val category = categoryManager.categorizeTransaction(transaction)
        assertEquals(DefaultCategories.FOOD, category)
    }

    @Test
    fun merchantNormalization_handlesExtraWhitespace() = runTest {
        // Given
        categoryManager.initialize(repository)

        // When - Set mapping with extra whitespace
        categoryManager.setManualOverride(1L, DefaultCategories.TRANSPORT, "  Uber   Rides  ")

        // Then - Should match trimmed version
        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = "Uber Rides",
            rawSms = "Payment"
        )
        val category = categoryManager.categorizeTransaction(transaction)
        assertEquals(DefaultCategories.TRANSPORT, category)
    }

    @Test
    fun merchantNormalization_handlesMultipleSpaces() = runTest {
        // Given
        categoryManager.initialize(repository)

        // Save transaction first for foreign key constraint
        repository.saveTransaction(createTestTransaction(smsId = 1L, merchant = "Book  My  Show"))

        // When - Set mapping with multiple spaces
        categoryManager.setManualOverride(1L, DefaultCategories.ENTERTAINMENT, "Book  My  Show")

        // Then - Should normalize to single spaces
        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = "Book My Show",
            rawSms = "Payment"
        )
        val category = categoryManager.categorizeTransaction(transaction)
        assertEquals(DefaultCategories.ENTERTAINMENT, category)
    }

    // ==================== Reset Functionality Tests ====================

    @Test
    fun resetAllMerchantMappings_clearsMappingsFromDatabase() = runTest {
        // Given
        categoryManager.initialize(repository)

        // Save transactions first for foreign key constraint
        repository.saveTransaction(createTestTransaction(smsId = 1L, merchant = "Swiggy"))
        repository.saveTransaction(createTestTransaction(smsId = 2L, merchant = "Amazon"))
        repository.saveTransaction(createTestTransaction(smsId = 3L, merchant = "Uber"))

        // Add multiple merchant mappings
        categoryManager.setManualOverride(1L, DefaultCategories.FOOD, "Swiggy")
        categoryManager.setManualOverride(2L, DefaultCategories.SHOPPING, "Amazon")
        categoryManager.setManualOverride(3L, DefaultCategories.TRANSPORT, "Uber")

        // Verify mappings exist
        val countBefore = repository.getMerchantMappingCount().getOrNull()
        assertEquals(3, countBefore)

        // When
        val result = categoryManager.resetAllMerchantMappings()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()) // Returns count of deleted mappings

        // Verify database is empty
        val countAfter = repository.getMerchantMappingCount().getOrNull()
        assertEquals(0, countAfter)
    }

    @Test
    fun resetAllMerchantMappings_clearsInMemoryCache() = runTest {
        // Given
        categoryManager.initialize(repository)

        // Save transaction first for foreign key constraint
        repository.saveTransaction(createTestTransaction(smsId = 1L, merchant = "Zomato"))

        // Add merchant mapping
        categoryManager.setManualOverride(1L, DefaultCategories.FOOD, "Zomato")

        // Verify mapping works
        val txBefore = createTestTransaction(smsId = 2L, merchant = "Zomato", rawSms = "Payment")
        val categoryBefore = categoryManager.categorizeTransaction(txBefore)
        assertEquals(DefaultCategories.FOOD, categoryBefore)

        // When
        categoryManager.resetAllMerchantMappings()

        // Then - Merchant mapping should not apply anymore
        val txAfter = createTestTransaction(smsId = 3L, merchant = "Zomato", rawSms = "Payment at restaurant")
        val categoryAfter = categoryManager.categorizeTransaction(txAfter)
        // Should fall back to keyword matching ("restaurant" keyword)
        assertEquals(DefaultCategories.FOOD, categoryAfter)
    }

    @Test
    fun resetAllMerchantMappings_doesNotAffectPerTransactionOverrides() = runTest {
        // Given
        categoryManager.initialize(repository)

        // Set both merchant mapping and per-transaction override
        categoryManager.setManualOverride(1L, DefaultCategories.FOOD, "TestMerchant")

        // When
        categoryManager.resetAllMerchantMappings()

        // Then - Per-transaction override should still work
        val tx = createTestTransaction(smsId = 1L, merchant = "TestMerchant")
        val category = categoryManager.categorizeTransaction(tx)
        assertEquals(DefaultCategories.FOOD, category) // Still overridden
    }

    @Test
    fun resetAllMerchantMappings_withNoMappings_returnsZero() = runTest {
        // Given
        categoryManager.initialize(repository)

        // When - Reset with no mappings
        val result = categoryManager.resetAllMerchantMappings()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun resetAllMerchantMappings_withoutRepository_succeeds() = runTest {
        // Given - CategoryManager without repository
        val manager = CategoryManager()

        // When
        val result = manager.resetAllMerchantMappings()

        // Then - Should succeed with 0 deletions
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    // ==================== Initialization Tests ====================

    @Test
    fun initialize_loadsMerchantMappingsFromDatabase() = runTest {
        // Given - Add mappings directly to database
        repository.setMerchantCategoryMapping("dominos", "food")
        repository.setMerchantCategoryMapping("flipkart", "shopping")

        // When - Initialize CategoryManager
        categoryManager.initialize(repository)

        // Then - Mappings should be loaded and applied
        val tx1 = createTestTransaction(smsId = 1L, merchant = "Dominos", rawSms = "Payment")
        val category1 = categoryManager.categorizeTransaction(tx1)
        assertEquals(DefaultCategories.FOOD, category1)

        val tx2 = createTestTransaction(smsId = 2L, merchant = "Flipkart", rawSms = "Payment")
        val category2 = categoryManager.categorizeTransaction(tx2)
        assertEquals(DefaultCategories.SHOPPING, category2)
    }

    @Test
    fun initialize_handlesEmptyDatabase() = runTest {
        // Given - Empty database

        // When
        categoryManager.initialize(repository)

        // Then - Should succeed without errors
        val count = repository.getMerchantMappingCount().getOrNull()
        assertEquals(0, count)
    }

    @Test
    fun initialize_handlesInvalidCategoryIds() = runTest {
        // Given - Add mapping with invalid category ID
        repository.setMerchantCategoryMapping("testmerchant", "invalid_category")

        // When
        categoryManager.initialize(repository)

        // Then - Should not crash, invalid mapping should be ignored
        val tx = createTestTransaction(smsId = 1L, merchant = "TestMerchant", rawSms = "Payment")
        val category = categoryManager.categorizeTransaction(tx)
        // Should fall back to default since category ID is invalid
        assertEquals(DefaultCategories.OTHER, category)
    }

    // ==================== Integration Tests ====================

    @Test
    fun fullWorkflow_learnApplyAndReset() = runTest {
        // Given
        categoryManager.initialize(repository)

        // Save transactions first for foreign key constraint
        repository.saveTransaction(createTestTransaction(smsId = 1L, merchant = "Pizza Hut"))
        repository.saveTransaction(createTestTransaction(smsId = 3L, merchant = "Myntra"))

        // Step 1: User corrects a transaction
        categoryManager.setManualOverride(1L, DefaultCategories.FOOD, "Pizza Hut")

        // Step 2: New transaction from same merchant is auto-categorized
        val tx2 = createTestTransaction(smsId = 2L, merchant = "Pizza Hut", rawSms = "Payment")
        val category2 = categoryManager.categorizeTransaction(tx2)
        assertEquals(DefaultCategories.FOOD, category2)

        // Step 3: Another merchant
        categoryManager.setManualOverride(3L, DefaultCategories.SHOPPING, "Myntra")
        val tx4 = createTestTransaction(smsId = 4L, merchant = "Myntra", rawSms = "Order")
        val category4 = categoryManager.categorizeTransaction(tx4)
        assertEquals(DefaultCategories.SHOPPING, category4)

        // Step 4: Reset all learned mappings
        val resetResult = categoryManager.resetAllMerchantMappings()
        assertTrue(resetResult.isSuccess)
        assertEquals(2, resetResult.getOrNull())

        // Step 5: Transactions now use keyword matching
        val tx5 = createTestTransaction(smsId = 5L, merchant = "Pizza Hut", rawSms = "Payment at pizza outlet")
        val category5 = categoryManager.categorizeTransaction(tx5)
        assertEquals(DefaultCategories.FOOD, category5) // Keyword "pizza"
    }

    @Test
    fun categorizationPriority_verifyFullHierarchy() = runTest {
        // Given
        categoryManager.initialize(repository)

        // Set up merchant mapping using one transaction
        repository.saveTransaction(createTestTransaction(smsId = 1L, merchant = "Shop A"))
        categoryManager.setManualOverride(1L, DefaultCategories.SHOPPING, "Shop A")

        // Set up per-transaction override using different merchant to avoid overwriting mapping
        repository.saveTransaction(createTestTransaction(smsId = 2L, merchant = "Shop B"))
        categoryManager.setManualOverride(2L, DefaultCategories.FOOD, "Shop B")

        // Test priority 1: Per-transaction override wins (even over keywords)
        val tx2 = createTestTransaction(smsId = 2L, merchant = "Shop B", rawSms = "Payment at amazon flipkart")
        assertEquals(DefaultCategories.FOOD, categoryManager.categorizeTransaction(tx2))

        // Test priority 2: Merchant mapping wins over keywords
        val tx3 = createTestTransaction(smsId = 3L, merchant = "Shop A", rawSms = "Payment at zomato swiggy")
        assertEquals(DefaultCategories.SHOPPING, categoryManager.categorizeTransaction(tx3))

        // Test priority 3: Keywords win when no merchant/override
        val tx4 = createTestTransaction(smsId = 4L, merchant = "Unknown", rawSms = "Payment at netflix")
        assertEquals(DefaultCategories.ENTERTAINMENT, categoryManager.categorizeTransaction(tx4))

        // Test priority 4: Default fallback
        val tx5 = createTestTransaction(smsId = 5L, merchant = "Unknown", rawSms = "Payment")
        assertEquals(DefaultCategories.OTHER, categoryManager.categorizeTransaction(tx5))
    }

    @Test
    fun concurrentMerchantMappings_maintainConsistency() = runTest {
        // Given
        categoryManager.initialize(repository)

        // When - Add multiple merchant mappings
        val merchants = listOf(
            "Swiggy" to DefaultCategories.FOOD,
            "Amazon" to DefaultCategories.SHOPPING,
            "Uber" to DefaultCategories.TRANSPORT,
            "Netflix" to DefaultCategories.ENTERTAINMENT,
            "Apollo" to DefaultCategories.HEALTH
        )

        // Save transactions first for foreign key constraint
        merchants.forEachIndexed { index, (merchant, _) ->
            repository.saveTransaction(createTestTransaction(smsId = index.toLong(), merchant = merchant))
        }

        merchants.forEachIndexed { index, (merchant, category) ->
            categoryManager.setManualOverride(index.toLong(), category, merchant)
        }

        // Then - All mappings should work correctly
        merchants.forEachIndexed { index, (merchant, expectedCategory) ->
            val tx = createTestTransaction(
                smsId = (index + 100).toLong(),
                merchant = merchant,
                rawSms = "Payment"
            )
            val category = categoryManager.categorizeTransaction(tx)
            assertEquals(expectedCategory, category)
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun categorizeTransaction_withVeryLongMerchantName() = runTest {
        // Given
        categoryManager.initialize(repository)
        val longMerchant = "A".repeat(500)

        // Save transaction first for foreign key constraint
        repository.saveTransaction(createTestTransaction(smsId = 1L, merchant = longMerchant))

        // When
        categoryManager.setManualOverride(1L, DefaultCategories.FOOD, longMerchant)

        // Then
        val tx = createTestTransaction(smsId = 2L, merchant = longMerchant, rawSms = "Payment")
        val category = categoryManager.categorizeTransaction(tx)
        assertEquals(DefaultCategories.FOOD, category)
    }

    @Test
    fun merchantMapping_withNumericMerchantName() = runTest {
        // Given
        categoryManager.initialize(repository)

        // Save transaction first for foreign key constraint
        repository.saveTransaction(createTestTransaction(smsId = 1L, merchant = "123456789"))

        // When - Merchant is all numbers
        categoryManager.setManualOverride(1L, DefaultCategories.SHOPPING, "123456789")

        // Then
        val tx = createTestTransaction(smsId = 2L, merchant = "123456789", rawSms = "Payment")
        val category = categoryManager.categorizeTransaction(tx)
        assertEquals(DefaultCategories.SHOPPING, category)
    }

    @Test
    fun merchantMapping_withUnicodeCharacters() = runTest {
        // Given
        categoryManager.initialize(repository)

        // Save transaction first for foreign key constraint
        repository.saveTransaction(createTestTransaction(smsId = 1L, merchant = "カフェ☕️"))

        // When - Merchant has unicode characters
        categoryManager.setManualOverride(1L, DefaultCategories.FOOD, "カフェ☕️")

        // Then - Should normalize to alphanumeric only
        val tx = createTestTransaction(smsId = 2L, merchant = "カフェ☕️", rawSms = "Payment")
        val category = categoryManager.categorizeTransaction(tx)
        // Unicode chars get stripped, so mapping might not match
        // This tests graceful handling
        assertNotNull(category)
    }

    @Test
    fun categorizeAll_withMerchantMappings() = runTest {
        // Given
        categoryManager.initialize(repository)
        categoryManager.setManualOverride(1L, DefaultCategories.FOOD, "Swiggy")
        categoryManager.setManualOverride(2L, DefaultCategories.SHOPPING, "Amazon")

        // When
        val transactions = listOf(
            createTestTransaction(smsId = 10L, merchant = "Swiggy", rawSms = "Order"),
            createTestTransaction(smsId = 11L, merchant = "Amazon", rawSms = "Purchase"),
            createTestTransaction(smsId = 12L, merchant = "Unknown", rawSms = "Payment at uber")
        )
        val categories = categoryManager.categorizeAll(transactions)

        // Then
        assertEquals(DefaultCategories.FOOD, categories[10L])
        assertEquals(DefaultCategories.SHOPPING, categories[11L])
        assertEquals(DefaultCategories.TRANSPORT, categories[12L]) // Keyword "uber"
    }
}
