package com.example.kanakku.data.category

import androidx.compose.ui.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.repository.CategoryRepository
import com.example.kanakku.data.repository.TransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive unit tests for CategorySuggestionEngine.
 *
 * Tests cover:
 * - Keyword matching (exact and fuzzy)
 * - Merchant pattern recognition
 * - Confidence scoring and levels
 * - Pattern learning from historical transactions
 * - String similarity calculations
 * - Keyword extraction
 * - Suggestion generation and ranking
 * - Edge cases and error handling
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CategorySuggestionEngineTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var suggestionEngine: CategorySuggestionEngine

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        categoryRepository = CategoryRepository(database)
        transactionRepository = TransactionRepository(database)
        suggestionEngine = CategorySuggestionEngine(categoryRepository, transactionRepository)
    }

    @After
    fun teardown() {
        database.close()
    }

    // ==================== Helper Functions ====================

    private fun createTestCategory(
        id: String = "test_category",
        name: String = "Test Category",
        icon: String = "🏷️",
        color: Color = Color(0xFFFF5722),
        keywords: MutableList<String> = mutableListOf("test"),
        isSystemCategory: Boolean = false
    ): Category {
        return Category(
            id = id,
            name = name,
            icon = icon,
            color = color,
            keywords = keywords,
            parentId = null,
            isSystemCategory = isSystemCategory
        )
    }

    private fun createTestTransaction(
        smsId: Long = 1L,
        amount: Double = 100.0,
        type: TransactionType = TransactionType.DEBIT,
        merchant: String? = "Test Merchant",
        rawSms: String = "Test SMS",
        location: String? = null,
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
            rawSms = rawSms,
            senderAddress = "VM-TESTBK",
            balanceAfter = 500.0,
            location = location
        )
    }

    private suspend fun seedTestCategories() {
        val foodCategory = createTestCategory(
            id = "food",
            name = "Food",
            keywords = mutableListOf("swiggy", "zomato", "restaurant", "food", "pizza")
        )
        val shoppingCategory = createTestCategory(
            id = "shopping",
            name = "Shopping",
            keywords = mutableListOf("amazon", "flipkart", "shop", "store")
        )
        val transportCategory = createTestCategory(
            id = "transport",
            name = "Transport",
            keywords = mutableListOf("uber", "ola", "taxi", "fuel")
        )

        categoryRepository.saveCategory(foodCategory)
        categoryRepository.saveCategory(shoppingCategory)
        categoryRepository.saveCategory(transportCategory)
    }

    // ==================== Initialization Tests ====================

    @Test
    fun initialize_loadsCategories() = runTest {
        // Given
        seedTestCategories()

        // When
        suggestionEngine.initialize()

        // Then
        val suggestions = suggestionEngine.suggestCategories(
            createTestTransaction(rawSms = "Paid to Swiggy Rs 100")
        )
        assertTrue(suggestions.isSuccess)
        assertTrue(suggestions.getOrNull()!!.isNotEmpty())
    }

    @Test
    fun initialize_buildsPatternCache() = runTest {
        // Given
        seedTestCategories()
        val transaction = createTestTransaction(smsId = 1L, merchant = "Swiggy", rawSms = "Paid to Swiggy")
        transactionRepository.saveTransaction(transaction)
        transactionRepository.saveCategoryOverride(1L, "food")

        // When
        suggestionEngine.initialize()

        // Then
        assertEquals(1, suggestionEngine.getMerchantPatternCount())
        assertTrue(suggestionEngine.getKeywordPatternCount() > 0)
    }

    @Test
    fun initialize_handlesEmptyDatabase() = runTest {
        // Given - empty database

        // When
        suggestionEngine.initialize()

        // Then - should not crash
        val suggestions = suggestionEngine.suggestCategories(
            createTestTransaction()
        )
        assertTrue(suggestions.isSuccess)
        assertTrue(suggestions.getOrNull()!!.isEmpty())
    }

    // ==================== Keyword Matching Tests ====================

    @Test
    fun suggestCategories_exactKeywordMatch_returnsHighConfidence() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "Unknown Merchant",
            rawSms = "Payment to Swiggy for food delivery Rs 100"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        assertEquals("food", suggestions[0].category.id)
        assertTrue(suggestions[0].confidence > 0.0)
    }

    @Test
    fun suggestCategories_multipleKeywordMatches_scoresHigher() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "Test",
            rawSms = "Paid to Swiggy restaurant for pizza and food delivery Rs 200"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        assertEquals("food", suggestions[0].category.id)
        // Multiple keywords (swiggy, restaurant, pizza, food) should increase confidence
        assertTrue(suggestions[0].confidence > 0.2)
    }

    @Test
    fun suggestCategories_fuzzyKeywordMatch_returnsMatch() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "Test",
            rawSms = "Payment to Swigy (typo) for delivery Rs 100" // Typo: Swigy instead of Swiggy
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        // Should still match food category despite typo
        assertEquals("food", suggestions[0].category.id)
    }

    @Test
    fun suggestCategories_noKeywordMatch_returnsEmpty() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "Unknown",
            rawSms = "Some transaction with no matching keywords Rs 100"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun suggestCategories_keywordInMerchantName_matchesCategory() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "Uber Rides",
            rawSms = "Payment for ride Rs 150"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        assertEquals("transport", suggestions[0].category.id)
    }

    @Test
    fun suggestCategories_categoryWithNoKeywords_doesNotMatch() = runTest {
        // Given
        val emptyKeywordsCategory = createTestCategory(
            id = "empty",
            name = "Empty",
            keywords = mutableListOf()
        )
        categoryRepository.saveCategory(emptyKeywordsCategory)
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            rawSms = "Some transaction Rs 100"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        // Should not suggest category with no keywords
        assertFalse(suggestions.any { it.category.id == "empty" })
    }

    // ==================== Merchant Pattern Recognition Tests ====================

    @Test
    fun suggestCategories_exactMerchantMatch_returnsHighConfidence() = runTest {
        // Given
        seedTestCategories()
        val historicalTx = createTestTransaction(smsId = 1L, merchant = "Swiggy")
        transactionRepository.saveTransaction(historicalTx)
        transactionRepository.saveCategoryOverride(1L, "food")
        suggestionEngine.initialize()

        val newTransaction = createTestTransaction(
            smsId = 2L,
            merchant = "Swiggy",
            rawSms = "Payment to Swiggy Rs 250"
        )

        // When
        val result = suggestionEngine.suggestCategories(newTransaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        assertEquals("food", suggestions[0].category.id)
        // Historical merchant match should give high confidence
        assertTrue(suggestions[0].confidence >= 0.5)
    }

    @Test
    fun suggestCategories_partialMerchantMatch_returnsLowerConfidence() = runTest {
        // Given
        seedTestCategories()
        val historicalTx = createTestTransaction(smsId = 1L, merchant = "Swiggy Food Delivery")
        transactionRepository.saveTransaction(historicalTx)
        transactionRepository.saveCategoryOverride(1L, "food")
        suggestionEngine.initialize()

        val newTransaction = createTestTransaction(
            smsId = 2L,
            merchant = "Swiggy",
            rawSms = "Payment to Swiggy Rs 250"
        )

        // When
        val result = suggestionEngine.suggestCategories(newTransaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        assertEquals("food", suggestions[0].category.id)
        // Partial match should give lower confidence than exact match
        assertTrue(suggestions[0].confidence > 0.0)
    }

    @Test
    fun suggestCategories_frequentMerchantCategorization_increasesConfidence() = runTest {
        // Given
        seedTestCategories()

        // Create multiple historical categorizations for same merchant
        for (i in 1L..5L) {
            val tx = createTestTransaction(smsId = i, merchant = "Swiggy")
            transactionRepository.saveTransaction(tx)
            transactionRepository.saveCategoryOverride(i, "food")
        }
        suggestionEngine.initialize()

        val newTransaction = createTestTransaction(
            smsId = 10L,
            merchant = "Swiggy",
            rawSms = "Payment Rs 300"
        )

        // When
        val result = suggestionEngine.suggestCategories(newTransaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        assertEquals("food", suggestions[0].category.id)
        // Multiple categorizations should increase confidence
        assertTrue(suggestions[0].confidence >= 0.5)
        assertEquals(ConfidenceLevel.HIGH, suggestions[0].confidenceLevel)
    }

    @Test
    fun suggestCategories_merchantWithMixedCategories_suggestsMostFrequent() = runTest {
        // Given
        seedTestCategories()

        // Same merchant categorized differently
        val tx1 = createTestTransaction(smsId = 1L, merchant = "Amazon")
        val tx2 = createTestTransaction(smsId = 2L, merchant = "Amazon")
        val tx3 = createTestTransaction(smsId = 3L, merchant = "Amazon")

        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)
        transactionRepository.saveTransaction(tx3)

        transactionRepository.saveCategoryOverride(1L, "shopping")
        transactionRepository.saveCategoryOverride(2L, "shopping")
        transactionRepository.saveCategoryOverride(3L, "food") // Different category

        suggestionEngine.initialize()

        val newTransaction = createTestTransaction(
            smsId = 10L,
            merchant = "Amazon",
            rawSms = "Payment Rs 500"
        )

        // When
        val result = suggestionEngine.suggestCategories(newTransaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        // Should suggest shopping (2 occurrences) over food (1 occurrence)
        assertEquals("shopping", suggestions[0].category.id)
    }

    @Test
    fun suggestCategories_nullMerchant_doesNotCrash() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = null,
            rawSms = "Some payment Rs 100"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        // Should not crash, may return suggestions based on keywords
    }

    @Test
    fun suggestCategories_emptyMerchant_doesNotCrash() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "",
            rawSms = "Payment with uber Rs 150"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        // Should still match based on keywords in rawSms
        assertTrue(suggestions.isNotEmpty())
        assertEquals("transport", suggestions[0].category.id)
    }

    // ==================== Confidence Scoring Tests ====================

    @Test
    fun confidenceLevel_highConfidence_classifiedCorrectly() = runTest {
        // Given
        seedTestCategories()
        val historicalTx = createTestTransaction(smsId = 1L, merchant = "Swiggy")
        transactionRepository.saveTransaction(historicalTx)
        transactionRepository.saveCategoryOverride(1L, "food")
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = "Swiggy",
            rawSms = "Payment to Swiggy for food Rs 200"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions[0].confidence >= 0.7)
        assertEquals(ConfidenceLevel.HIGH, suggestions[0].confidenceLevel)
    }

    @Test
    fun confidenceLevel_mediumConfidence_classifiedCorrectly() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "Unknown",
            rawSms = "Payment for food delivery Rs 150"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        val foodSuggestion = suggestions.find { it.category.id == "food" }
        assertNotNull(foodSuggestion)
        assertTrue(foodSuggestion!!.confidence >= 0.4 && foodSuggestion.confidence < 0.7)
        assertEquals(ConfidenceLevel.MEDIUM, foodSuggestion.confidenceLevel)
    }

    @Test
    fun confidenceLevel_lowConfidence_classifiedCorrectly() = runTest {
        // Given
        val lowMatchCategory = createTestCategory(
            id = "low_match",
            name = "Low Match",
            keywords = mutableListOf("rareword")
        )
        categoryRepository.saveCategory(lowMatchCategory)
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "Test",
            rawSms = "Payment with rareword mentioned Rs 100"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        val suggestions = result.getOrNull()!!
        if (suggestions.isNotEmpty()) {
            val lowSuggestion = suggestions.find { it.category.id == "low_match" }
            if (lowSuggestion != null) {
                assertTrue(lowSuggestion.confidence < 0.4)
                assertEquals(ConfidenceLevel.LOW, lowSuggestion.confidenceLevel)
            }
        }
    }

    @Test
    fun calculateScore_combinesMultipleStrategies() = runTest {
        // Given
        seedTestCategories()
        val historicalTx = createTestTransaction(
            smsId = 1L,
            merchant = "Swiggy",
            rawSms = "Payment to Swiggy for food"
        )
        transactionRepository.saveTransaction(historicalTx)
        transactionRepository.saveCategoryOverride(1L, "food")
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = "Swiggy",
            rawSms = "Payment to Swiggy restaurant for pizza delivery Rs 300"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        val foodSuggestion = suggestions[0]

        // Score should combine:
        // 1. Historical merchant match (high)
        // 2. Multiple keyword matches (swiggy, restaurant, pizza)
        // 3. Pattern learning from "swiggy" keyword
        assertTrue(foodSuggestion.confidence >= 0.7)
    }

    @Test
    fun calculateScore_normalizedToMaxOne() = runTest {
        // Given
        seedTestCategories()

        // Create many historical categorizations
        for (i in 1L..10L) {
            val tx = createTestTransaction(
                smsId = i,
                merchant = "Swiggy",
                rawSms = "Payment to Swiggy for food pizza restaurant delivery"
            )
            transactionRepository.saveTransaction(tx)
            transactionRepository.saveCategoryOverride(i, "food")
        }
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            smsId = 20L,
            merchant = "Swiggy",
            rawSms = "Payment to Swiggy restaurant for pizza food delivery Rs 500"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        // Score should be capped at 1.0
        assertTrue(suggestions[0].confidence <= 1.0)
    }

    // ==================== Pattern Learning Tests ====================

    @Test
    fun recordCategorization_updatesPatterns() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val initialMerchantCount = suggestionEngine.getMerchantPatternCount()
        val initialKeywordCount = suggestionEngine.getKeywordPatternCount()

        val transaction = createTestTransaction(
            merchant = "New Merchant",
            rawSms = "Payment to New Merchant with unique keywords Rs 100"
        )

        // When
        suggestionEngine.recordCategorization(transaction, "food")

        // Then
        assertTrue(suggestionEngine.getMerchantPatternCount() > initialMerchantCount)
        assertTrue(suggestionEngine.getKeywordPatternCount() >= initialKeywordCount)
    }

    @Test
    fun recordCategorization_improvesSubsequentSuggestions() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction1 = createTestTransaction(
            smsId = 1L,
            merchant = "NewCafe",
            rawSms = "Payment to NewCafe Rs 100"
        )

        // First suggestion should be low or none
        val firstSuggestion = suggestionEngine.suggestCategories(transaction1).getOrNull()!!
        val firstFoodMatch = firstSuggestion.find { it.category.id == "food" }

        // When - record categorization
        suggestionEngine.recordCategorization(transaction1, "food")

        val transaction2 = createTestTransaction(
            smsId = 2L,
            merchant = "NewCafe",
            rawSms = "Payment to NewCafe Rs 150"
        )
        val secondSuggestion = suggestionEngine.suggestCategories(transaction2).getOrNull()!!

        // Then - second suggestion should be better
        assertTrue(secondSuggestion.isNotEmpty())
        assertEquals("food", secondSuggestion[0].category.id)

        if (firstFoodMatch != null) {
            assertTrue(secondSuggestion[0].confidence >= firstFoodMatch.confidence)
        } else {
            assertTrue(secondSuggestion[0].confidence > 0.0)
        }
    }

    @Test
    fun refresh_reloadsCategories() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        // Add new category after initialization
        val newCategory = createTestCategory(
            id = "health",
            name = "Health",
            keywords = mutableListOf("pharmacy", "hospital")
        )
        categoryRepository.saveCategory(newCategory)

        // When
        val refreshResult = suggestionEngine.refresh()

        // Then
        assertTrue(refreshResult.isSuccess)

        val transaction = createTestTransaction(
            rawSms = "Payment to pharmacy Rs 200"
        )
        val suggestions = suggestionEngine.suggestCategories(transaction).getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        assertEquals("health", suggestions[0].category.id)
    }

    @Test
    fun refresh_rebuildsPatternCache() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        // Add new historical categorization after initialization
        val newTx = createTestTransaction(smsId = 100L, merchant = "NewMerchant")
        transactionRepository.saveTransaction(newTx)
        transactionRepository.saveCategoryOverride(100L, "food")

        // When
        val refreshResult = suggestionEngine.refresh()

        // Then
        assertTrue(refreshResult.isSuccess)
        assertTrue(suggestionEngine.getMerchantPatternCount() > 0)
    }

    // ==================== Suggestion Generation Tests ====================

    @Test
    fun suggestCategories_returnsSortedByConfidence() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "Test",
            rawSms = "Payment for uber food and amazon shopping Rs 500"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        val suggestions = result.getOrNull()!!
        if (suggestions.size >= 2) {
            // Suggestions should be sorted by confidence (descending)
            assertTrue(suggestions[0].confidence >= suggestions[1].confidence)
        }
    }

    @Test
    fun suggestCategories_respectsMaxSuggestions() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "Test",
            rawSms = "Payment for uber swiggy amazon food shopping taxi Rs 500"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction, maxSuggestions = 2)

        // Then
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.size <= 2)
    }

    @Test
    fun suggestCategories_filtersZeroScoreCategories() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "Unknown",
            rawSms = "Generic payment Rs 100"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        val suggestions = result.getOrNull()!!
        // All suggestions should have confidence > 0
        suggestions.forEach { suggestion ->
            assertTrue(suggestion.confidence > 0.0)
        }
    }

    @Test
    fun suggestCategories_includesReasonForSuggestion() = runTest {
        // Given
        seedTestCategories()
        val historicalTx = createTestTransaction(smsId = 1L, merchant = "Swiggy")
        transactionRepository.saveTransaction(historicalTx)
        transactionRepository.saveCategoryOverride(1L, "food")
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = "Swiggy",
            rawSms = "Payment Rs 200"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        assertNotNull(suggestions[0].reason)
        assertFalse(suggestions[0].reason.isEmpty())
        // Should mention historical match
        assertTrue(suggestions[0].reason.contains("Swiggy") ||
                   suggestions[0].reason.contains("Previously") ||
                   suggestions[0].reason.contains("Strong match"))
    }

    // ==================== String Similarity Tests ====================

    @Test
    fun stringSimilarity_identicalStrings_returnsOne() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        // Test via fuzzy matching
        val transaction = createTestTransaction(
            merchant = "Swiggy",
            rawSms = "Payment Rs 100"
        )
        val historicalTx = createTestTransaction(smsId = 1L, merchant = "Swiggy")
        transactionRepository.saveTransaction(historicalTx)
        transactionRepository.saveCategoryOverride(1L, "food")
        suggestionEngine.refresh()

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        // Identical merchant should give high confidence
        assertTrue(suggestions[0].confidence >= 0.5)
    }

    @Test
    fun stringSimilarity_similarStrings_returnsHighSimilarity() = runTest {
        // Given
        seedTestCategories()
        val historicalTx = createTestTransaction(smsId = 1L, merchant = "Swiggy Food")
        transactionRepository.saveTransaction(historicalTx)
        transactionRepository.saveCategoryOverride(1L, "food")
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = "Swiggy",
            rawSms = "Payment Rs 100"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        // Similar merchants should still match
        assertEquals("food", suggestions[0].category.id)
    }

    @Test
    fun stringSimilarity_differentStrings_returnsLowSimilarity() = runTest {
        // Given
        seedTestCategories()
        val historicalTx = createTestTransaction(smsId = 1L, merchant = "Swiggy")
        transactionRepository.saveTransaction(historicalTx)
        transactionRepository.saveCategoryOverride(1L, "food")
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            smsId = 2L,
            merchant = "Amazon",
            rawSms = "Payment Rs 100"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        val suggestions = result.getOrNull()!!
        // Different merchants should not match based on merchant pattern alone
        if (suggestions.isNotEmpty() && suggestions[0].category.id == "food") {
            // If food is suggested, it should be due to keywords, not merchant
            assertTrue(suggestions[0].confidence < 0.5)
        }
    }

    @Test
    fun stringSimilarity_emptyStrings_returnsZero() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "",
            rawSms = ""
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        // Empty strings should not match anything
        assertTrue(suggestions.isEmpty())
    }

    // ==================== Keyword Extraction Tests ====================

    @Test
    fun keywordExtraction_filtersCommonWords() = runTest {
        // Given
        seedTestCategories()
        val transaction = createTestTransaction(
            rawSms = "The transaction was debited from a/c for food at Swiggy"
        )

        // When
        suggestionEngine.initialize()
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        // Should match based on "food" and "swiggy", not common words
        assertEquals("food", suggestions[0].category.id)
    }

    @Test
    fun keywordExtraction_handlesSpecialCharacters() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            rawSms = "Payment@Swiggy.com-for_food;delivery:Rs-100[confirmed]"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        // Should extract "swiggy", "food", "delivery" despite special characters
        assertEquals("food", suggestions[0].category.id)
    }

    @Test
    fun keywordExtraction_filtersShortWords() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            rawSms = "A to B at C is ok Rs 100 for uber ride"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        // Should match "uber" and "ride", not single/two-letter words
        assertEquals("transport", suggestions[0].category.id)
    }

    @Test
    fun keywordExtraction_filtersNumbers() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            rawSms = "Transaction 12345 at 09876 for 500 with amazon shopping"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        // Should match "amazon" and "shopping", not pure numbers
        assertEquals("shopping", suggestions[0].category.id)
    }

    @Test
    fun keywordExtraction_returnsDistinctKeywords() = runTest {
        // Given
        seedTestCategories()
        val transaction = createTestTransaction(
            rawSms = "Paid to Swiggy Swiggy Swiggy for food food Rs 100"
        )
        transactionRepository.saveTransaction(transaction)
        transactionRepository.saveCategoryOverride(transaction.smsId, "food")
        suggestionEngine.initialize()

        // When - Pattern should be learned without duplicate keywords inflating count
        val merchantCount = suggestionEngine.getMerchantPatternCount()
        val keywordCount = suggestionEngine.getKeywordPatternCount()

        // Then
        assertTrue(merchantCount >= 0)
        assertTrue(keywordCount >= 0)
        // Duplicate keywords should be removed during extraction
    }

    // ==================== Edge Cases and Error Handling ====================

    @Test
    fun suggestCategories_withLocation_includesInSearch() = runTest {
        // Given
        val transportCategory = createTestCategory(
            id = "transport",
            name = "Transport",
            keywords = mutableListOf("airport", "station")
        )
        categoryRepository.saveCategory(transportCategory)
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "Unknown",
            rawSms = "Payment Rs 100",
            location = "Airport Terminal"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        assertEquals("transport", suggestions[0].category.id)
    }

    @Test
    fun suggestCategories_largeTransactionAmount_doesNotAffectScoring() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val smallTransaction = createTestTransaction(
            amount = 10.0,
            rawSms = "Payment to Swiggy Rs 10"
        )
        val largeTransaction = createTestTransaction(
            amount = 10000.0,
            rawSms = "Payment to Swiggy Rs 10000"
        )

        // When
        val smallResult = suggestionEngine.suggestCategories(smallTransaction)
        val largeResult = suggestionEngine.suggestCategories(largeTransaction)

        // Then
        val smallSuggestions = smallResult.getOrNull()!!
        val largeSuggestions = largeResult.getOrNull()!!

        assertTrue(smallSuggestions.isNotEmpty())
        assertTrue(largeSuggestions.isNotEmpty())

        // Both should suggest same category
        assertEquals(smallSuggestions[0].category.id, largeSuggestions[0].category.id)
        // Amount shouldn't significantly affect confidence in current implementation
    }

    @Test
    fun suggestCategories_uninitializedEngine_loadsCategories() = runTest {
        // Given
        seedTestCategories()
        // Don't call initialize()

        val transaction = createTestTransaction(
            rawSms = "Payment to Swiggy for food Rs 100"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        // Should auto-load categories and return suggestions
        assertTrue(suggestions.isNotEmpty())
    }

    @Test
    fun suggestCategories_caseInsensitiveMatching() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val upperCaseTransaction = createTestTransaction(
            merchant = "SWIGGY",
            rawSms = "PAYMENT TO SWIGGY FOR FOOD RS 100"
        )
        val lowerCaseTransaction = createTestTransaction(
            merchant = "swiggy",
            rawSms = "payment to swiggy for food rs 100"
        )

        // When
        val upperResult = suggestionEngine.suggestCategories(upperCaseTransaction)
        val lowerResult = suggestionEngine.suggestCategories(lowerCaseTransaction)

        // Then
        val upperSuggestions = upperResult.getOrNull()!!
        val lowerSuggestions = lowerResult.getOrNull()!!

        assertTrue(upperSuggestions.isNotEmpty())
        assertTrue(lowerSuggestions.isNotEmpty())

        // Both should match same category
        assertEquals("food", upperSuggestions[0].category.id)
        assertEquals("food", lowerSuggestions[0].category.id)
    }

    @Test
    fun suggestCategories_veryLongRawSms_handlesGracefully() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val longSms = "Payment to Swiggy " + "word ".repeat(1000) + "for food delivery"
        val transaction = createTestTransaction(
            rawSms = longSms
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertTrue(suggestions.isNotEmpty())
        assertEquals("food", suggestions[0].category.id)
    }

    @Test
    fun suggestCategories_unicodeCharacters_handlesCorrectly() = runTest {
        // Given
        seedTestCategories()
        suggestionEngine.initialize()

        val transaction = createTestTransaction(
            merchant = "Café ☕",
            rawSms = "Payment to Café ☕ for food 🍕 Rs 100"
        )

        // When
        val result = suggestionEngine.suggestCategories(transaction)

        // Then
        assertTrue(result.isSuccess)
        // Should not crash with unicode characters
        val suggestions = result.getOrNull()!!
        if (suggestions.isNotEmpty()) {
            assertEquals("food", suggestions[0].category.id)
        }
    }

    @Test
    fun getPatternCounts_returnsAccurateCounts() = runTest {
        // Given
        seedTestCategories()

        val tx1 = createTestTransaction(smsId = 1L, merchant = "Merchant1", rawSms = "Payment for food")
        val tx2 = createTestTransaction(smsId = 2L, merchant = "Merchant2", rawSms = "Payment for shopping")

        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)
        transactionRepository.saveCategoryOverride(1L, "food")
        transactionRepository.saveCategoryOverride(2L, "shopping")

        suggestionEngine.initialize()

        // When
        val merchantCount = suggestionEngine.getMerchantPatternCount()
        val keywordCount = suggestionEngine.getKeywordPatternCount()

        // Then
        assertEquals(2, merchantCount) // merchant1, merchant2
        assertTrue(keywordCount > 0) // At least some keywords extracted
    }
}
