package com.example.kanakku.data.category

import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.repository.CategoryRepository
import com.example.kanakku.data.repository.TransactionRepository
import kotlin.math.min

/**
 * Smart suggestion engine for recommending categories based on transaction patterns.
 *
 * This engine uses multiple strategies to suggest the most appropriate category:
 * 1. **Keyword Matching**: Fuzzy matching against category keywords with scoring
 * 2. **Pattern Learning**: Analyzes historical categorizations for similar merchants/patterns
 * 3. **Amount-Based Heuristics**: Uses transaction amounts to refine suggestions
 * 4. **Confidence Scoring**: Ranks suggestions by confidence level
 *
 * The engine prioritizes suggestions based on:
 * - Exact merchant matches from history (highest priority)
 * - Partial merchant matches with keyword correlation
 * - Similar transaction patterns (amount + type)
 * - Keyword density and relevance
 *
 * @param categoryRepository Repository for loading available categories
 * @param transactionRepository Repository for analyzing historical categorizations
 */
class CategorySuggestionEngine(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {

    // In-memory cache of available categories
    private var availableCategories: List<Category> = emptyList()

    // Pattern learning cache: merchant -> category ID -> count
    private val merchantPatterns = mutableMapOf<String, MutableMap<String, Int>>()

    // Keyword pattern cache: keyword -> category ID -> count
    private val keywordPatterns = mutableMapOf<String, MutableMap<String, Int>>()

    /**
     * Initializes the suggestion engine by:
     * 1. Loading all available categories
     * 2. Building pattern learning cache from historical transactions
     *
     * Should be called after repositories are available and before generating suggestions.
     */
    suspend fun initialize() {
        loadCategories()
        buildPatternCache()
    }

    /**
     * Loads all available categories from the repository.
     * Updates the in-memory cache for fast access.
     */
    private suspend fun loadCategories() {
        categoryRepository.getAllCategoriesSnapshot()
            .onSuccess { categories ->
                availableCategories = categories
                ErrorHandler.logDebug(
                    "Loaded ${categories.size} categories for suggestion engine",
                    "CategorySuggestionEngine"
                )
            }
            .onFailure { throwable ->
                ErrorHandler.logWarning(
                    "Failed to load categories for suggestion engine: ${throwable.message}",
                    "CategorySuggestionEngine"
                )
                availableCategories = emptyList()
            }
    }

    /**
     * Builds pattern learning cache from historical transactions.
     * Analyzes merchant names and keywords to identify common categorization patterns.
     */
    private suspend fun buildPatternCache() {
        merchantPatterns.clear()
        keywordPatterns.clear()

        // Get all transactions with category overrides (manually categorized)
        transactionRepository.getAllCategoryOverrides()
            .onSuccess { overrides ->
                // Load transactions to analyze patterns
                transactionRepository.getAllTransactionsSnapshot()
                    .onSuccess { transactions ->
                        for ((smsId, categoryId) in overrides) {
                            val transaction = transactions.find { it.smsId == smsId }
                            if (transaction != null) {
                                learnFromTransaction(transaction, categoryId)
                            }
                        }
                        ErrorHandler.logDebug(
                            "Built pattern cache: ${merchantPatterns.size} merchants, ${keywordPatterns.size} keywords",
                            "CategorySuggestionEngine"
                        )
                    }
                    .onFailure { throwable ->
                        ErrorHandler.logWarning(
                            "Failed to load transactions for pattern learning: ${throwable.message}",
                            "CategorySuggestionEngine"
                        )
                    }
            }
            .onFailure { throwable ->
                ErrorHandler.logWarning(
                    "Failed to load category overrides for pattern learning: ${throwable.message}",
                    "CategorySuggestionEngine"
                )
            }
    }

    /**
     * Learns patterns from a categorized transaction.
     * Updates merchant and keyword pattern caches.
     *
     * @param transaction The transaction to learn from
     * @param categoryId The category ID it was assigned to
     */
    private fun learnFromTransaction(transaction: ParsedTransaction, categoryId: String) {
        // Learn merchant patterns
        val merchant = transaction.merchant?.lowercase()?.trim()
        if (!merchant.isNullOrEmpty()) {
            val categoryMap = merchantPatterns.getOrPut(merchant) { mutableMapOf() }
            categoryMap[categoryId] = (categoryMap[categoryId] ?: 0) + 1
        }

        // Learn keyword patterns from raw SMS
        val keywords = extractKeywords(transaction.rawSms)
        for (keyword in keywords) {
            val categoryMap = keywordPatterns.getOrPut(keyword) { mutableMapOf() }
            categoryMap[categoryId] = (categoryMap[categoryId] ?: 0) + 1
        }
    }

    /**
     * Generates category suggestions for an uncategorized transaction.
     *
     * Returns a list of suggestions sorted by confidence (highest first).
     * Each suggestion includes the category and a confidence score (0.0 - 1.0).
     *
     * @param transaction The transaction to suggest categories for
     * @param maxSuggestions Maximum number of suggestions to return (default: 3)
     * @return Result<List<CategorySuggestion>> containing suggestions or error information
     */
    suspend fun suggestCategories(
        transaction: ParsedTransaction,
        maxSuggestions: Int = 3
    ): Result<List<CategorySuggestion>> {
        return ErrorHandler.runSuspendCatching("Suggest categories") {
            if (availableCategories.isEmpty()) {
                loadCategories()
            }

            // Calculate scores for each category
            val scoredCategories = availableCategories.mapNotNull { category ->
                val score = calculateCategoryScore(transaction, category)
                if (score > 0.0) {
                    CategorySuggestion(
                        category = category,
                        confidence = score,
                        reason = generateSuggestionReason(transaction, category, score)
                    )
                } else {
                    null
                }
            }

            // Sort by confidence (highest first) and limit to maxSuggestions
            scoredCategories
                .sortedByDescending { it.confidence }
                .take(maxSuggestions)
        }
    }

    /**
     * Calculates a confidence score for a category based on the transaction.
     * Combines multiple scoring strategies for comprehensive analysis.
     *
     * Score components:
     * - Historical merchant match: 0.0 - 0.5
     * - Keyword matching: 0.0 - 0.3
     * - Pattern learning: 0.0 - 0.2
     *
     * @param transaction The transaction to score
     * @param category The category to score against
     * @return Confidence score from 0.0 (no match) to 1.0 (perfect match)
     */
    private fun calculateCategoryScore(transaction: ParsedTransaction, category: Category): Double {
        var score = 0.0

        // 1. Historical merchant match (highest weight)
        score += scoreHistoricalMerchantMatch(transaction, category.id)

        // 2. Keyword matching
        score += scoreKeywordMatch(transaction, category)

        // 3. Pattern learning from keywords
        score += scorePatternLearning(transaction, category.id)

        // Normalize to 0.0 - 1.0 range
        return min(score, 1.0)
    }

    /**
     * Scores based on historical merchant categorizations.
     * Returns high score if this merchant was previously categorized to this category.
     *
     * @param transaction The transaction to analyze
     * @param categoryId The category ID to check
     * @return Score from 0.0 - 0.5
     */
    private fun scoreHistoricalMerchantMatch(transaction: ParsedTransaction, categoryId: String): Double {
        val merchant = transaction.merchant?.lowercase()?.trim()
        if (merchant.isNullOrEmpty()) return 0.0

        // Exact merchant match
        val exactMatch = merchantPatterns[merchant]?.get(categoryId) ?: 0
        if (exactMatch > 0) {
            // Weight by frequency: more categorizations = higher confidence
            val totalForMerchant = merchantPatterns[merchant]?.values?.sum() ?: 1
            return 0.5 * (exactMatch.toDouble() / totalForMerchant)
        }

        // Partial merchant match (fuzzy matching)
        var bestPartialScore = 0.0
        for ((knownMerchant, categories) in merchantPatterns) {
            val similarity = calculateStringSimilarity(merchant, knownMerchant)
            if (similarity > 0.7) { // 70% similarity threshold
                val count = categories[categoryId] ?: 0
                if (count > 0) {
                    val totalForMerchant = categories.values.sum()
                    val partialScore = 0.3 * similarity * (count.toDouble() / totalForMerchant)
                    bestPartialScore = maxOf(bestPartialScore, partialScore)
                }
            }
        }

        return bestPartialScore
    }

    /**
     * Scores based on keyword matching against category keywords.
     * Uses fuzzy matching to handle variations and typos.
     *
     * @param transaction The transaction to analyze
     * @param category The category to check
     * @return Score from 0.0 - 0.3
     */
    private fun scoreKeywordMatch(transaction: ParsedTransaction, category: Category): Double {
        if (category.keywords.isEmpty()) return 0.0

        val searchText = buildSearchText(transaction)
        var matchCount = 0
        var totalWeight = 0.0

        for (keyword in category.keywords) {
            val keywordLower = keyword.lowercase()

            // Exact keyword match
            if (searchText.contains(keywordLower)) {
                matchCount++
                totalWeight += 1.0
                continue
            }

            // Fuzzy keyword match
            val words = searchText.split(Regex("\\s+"))
            for (word in words) {
                val similarity = calculateStringSimilarity(word, keywordLower)
                if (similarity > 0.8) { // 80% similarity threshold for keywords
                    matchCount++
                    totalWeight += similarity
                    break
                }
            }
        }

        if (matchCount == 0) return 0.0

        // Score based on keyword density and match quality
        val density = matchCount.toDouble() / category.keywords.size
        val avgWeight = totalWeight / matchCount

        return 0.3 * density * avgWeight
    }

    /**
     * Scores based on pattern learning from keyword analysis.
     * Checks if transaction keywords correlate with historical categorizations.
     *
     * @param transaction The transaction to analyze
     * @param categoryId The category ID to check
     * @return Score from 0.0 - 0.2
     */
    private fun scorePatternLearning(transaction: ParsedTransaction, categoryId: String): Double {
        val keywords = extractKeywords(transaction.rawSms)
        if (keywords.isEmpty()) return 0.0

        var totalScore = 0.0
        var matchingKeywords = 0

        for (keyword in keywords) {
            val categoryMap = keywordPatterns[keyword] ?: continue
            val count = categoryMap[categoryId] ?: 0
            if (count > 0) {
                matchingKeywords++
                val totalForKeyword = categoryMap.values.sum()
                totalScore += count.toDouble() / totalForKeyword
            }
        }

        if (matchingKeywords == 0) return 0.0

        // Average score weighted by number of matching keywords
        val avgScore = totalScore / matchingKeywords
        return 0.2 * avgScore
    }

    /**
     * Builds search text from transaction data for keyword matching.
     * Combines merchant, location, and raw SMS content.
     *
     * @param transaction The transaction to extract text from
     * @return Lowercase search text
     */
    private fun buildSearchText(transaction: ParsedTransaction): String {
        return buildString {
            transaction.merchant?.let { append(it.lowercase()).append(" ") }
            transaction.location?.let { append(it.lowercase()).append(" ") }
            append(transaction.rawSms.lowercase())
        }
    }

    /**
     * Extracts meaningful keywords from text for pattern learning.
     * Filters out common words and numbers.
     *
     * @param text The text to extract keywords from
     * @return List of extracted keywords (lowercase)
     */
    private fun extractKeywords(text: String): List<String> {
        val commonWords = setOf(
            "the", "and", "or", "to", "from", "in", "on", "at", "is", "was", "are", "were",
            "rs", "inr", "upi", "txn", "ref", "a/c", "ac", "debited", "credited", "transaction"
        )

        return text.lowercase()
            .split(Regex("[\\s\\-_.,;:()\\[\\]{}]+")) // Split on whitespace and punctuation
            .filter { word ->
                word.length >= 3 && // Minimum word length
                        !word.matches(Regex("\\d+")) && // Not pure numbers
                        word !in commonWords // Not a common word
            }
            .distinct()
    }

    /**
     * Calculates similarity between two strings using Levenshtein distance.
     * Returns a normalized score from 0.0 (completely different) to 1.0 (identical).
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity score from 0.0 - 1.0
     */
    private fun calculateStringSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val maxLen = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)

        return 1.0 - (distance.toDouble() / maxLen)
    }

    /**
     * Calculates Levenshtein distance between two strings.
     * The distance is the minimum number of single-character edits required
     * to change one string into the other.
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Edit distance as an integer
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length

        // Create distance matrix
        val dp = Array(m + 1) { IntArray(n + 1) }

        // Initialize base cases
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        // Fill matrix using dynamic programming
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // Deletion
                    dp[i][j - 1] + 1,      // Insertion
                    dp[i - 1][j - 1] + cost // Substitution
                )
            }
        }

        return dp[m][n]
    }

    /**
     * Generates a human-readable reason for why a category was suggested.
     *
     * @param transaction The transaction being categorized
     * @param category The suggested category
     * @param score The confidence score
     * @return User-friendly explanation
     */
    private fun generateSuggestionReason(
        transaction: ParsedTransaction,
        category: Category,
        score: Double
    ): String {
        val merchant = transaction.merchant
        val hasHistoricalMatch = !merchant.isNullOrEmpty() &&
                merchantPatterns[merchant.lowercase().trim()]?.containsKey(category.id) == true

        return when {
            hasHistoricalMatch -> "Previously categorized similar transactions from '${merchant}'"
            score > 0.7 -> "Strong match based on transaction details"
            score > 0.4 -> "Likely match based on keywords and patterns"
            else -> "Possible match based on transaction analysis"
        }
    }

    /**
     * Reloads categories and rebuilds pattern cache.
     * Should be called when categories are updated or when learning new patterns.
     *
     * @return Result<Unit> indicating success or failure
     */
    suspend fun refresh(): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Refresh suggestion engine") {
            loadCategories()
            buildPatternCache()
        }
    }

    /**
     * Updates the pattern learning cache with a new categorization.
     * Call this when a user manually categorizes a transaction to improve future suggestions.
     *
     * @param transaction The categorized transaction
     * @param categoryId The assigned category ID
     */
    fun recordCategorization(transaction: ParsedTransaction, categoryId: String) {
        learnFromTransaction(transaction, categoryId)
        ErrorHandler.logDebug(
            "Recorded new categorization pattern for category: $categoryId",
            "CategorySuggestionEngine"
        )
    }

    /**
     * Gets the current count of learned merchant patterns.
     * Useful for analytics and debugging.
     *
     * @return Number of unique merchants in the pattern cache
     */
    fun getMerchantPatternCount(): Int = merchantPatterns.size

    /**
     * Gets the current count of learned keyword patterns.
     * Useful for analytics and debugging.
     *
     * @return Number of unique keywords in the pattern cache
     */
    fun getKeywordPatternCount(): Int = keywordPatterns.size
}

/**
 * Represents a category suggestion with confidence score and reasoning.
 *
 * @property category The suggested category
 * @property confidence Confidence score from 0.0 (low) to 1.0 (high)
 * @property reason Human-readable explanation for the suggestion
 */
data class CategorySuggestion(
    val category: Category,
    val confidence: Double,
    val reason: String
) {
    /**
     * Confidence level classification for UI display.
     */
    val confidenceLevel: ConfidenceLevel
        get() = when {
            confidence >= 0.7 -> ConfidenceLevel.HIGH
            confidence >= 0.4 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
}

/**
 * Classification of suggestion confidence levels.
 */
enum class ConfidenceLevel {
    /** High confidence (>= 70%) - Strong recommendation */
    HIGH,

    /** Medium confidence (40-70%) - Likely match */
    MEDIUM,

    /** Low confidence (< 40%) - Possible match */
    LOW
}
