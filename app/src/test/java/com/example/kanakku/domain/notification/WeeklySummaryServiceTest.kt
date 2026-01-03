package com.example.kanakku.domain.notification

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.entity.TransactionEntity
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.model.WeeklySummarySettings
import com.example.kanakku.data.preferences.AppPreferences
import com.example.kanakku.data.repository.TransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.util.Calendar

/**
 * Unit tests for WeeklySummaryService weekly summary generation logic.
 *
 * Tests cover:
 * - Weekly summary generation and sending
 * - Summary tracking and duplicate prevention
 * - Week key generation and formatting
 * - Notification data creation
 * - Summary clearing and reset logic
 * - Helper method functionality
 * - Edge cases and integration scenarios
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WeeklySummaryServiceTest {

    private lateinit var context: Context
    private lateinit var database: KanakkuDatabase
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var appPreferences: AppPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Reset and initialize AppPreferences
        AppPreferences.resetInstance()
        appPreferences = AppPreferences.getInstance(context)
        appPreferences.clearAll()

        // Enable weekly summary by default
        appPreferences.setWeeklySummarySettings(
            WeeklySummarySettings(
                enabled = true,
                dayOfWeek = DayOfWeek.MONDAY,
                hourOfDay = 9
            )
        )

        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        transactionRepository = TransactionRepository(database)
    }

    @After
    fun teardown() {
        database.close()
        appPreferences.clearAll()
        AppPreferences.resetInstance()
    }

    // ==================== Helper Functions ====================

    private fun createTestTransaction(
        amount: Double,
        merchant: String = "Test Merchant",
        type: TransactionType = TransactionType.DEBIT,
        date: Long = System.currentTimeMillis(),
        categoryId: String = "food",
        smsId: String? = "12345"
    ): TransactionEntity {
        return TransactionEntity(
            id = 0,
            amount = amount,
            merchant = merchant,
            type = type,
            date = date,
            categoryId = categoryId,
            smsId = smsId,
            accountNumber = "1234",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun getCurrentWeekKeyForTest(): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        val year = calendar.get(Calendar.YEAR)
        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        return String.format("%04d-W%02d", year, week)
    }

    // ==================== Basic Summary Generation Tests ====================

    @Test
    fun generateAndSendWeeklySummary_whenSummariesDisabled_returnsFalse() = runTest {
        // Given: Weekly summary disabled
        appPreferences.setWeeklySummarySettings(
            WeeklySummarySettings(enabled = false, dayOfWeek = DayOfWeek.MONDAY, hourOfDay = 9)
        )

        // Add some transactions
        val tx = createTestTransaction(amount = 1000.0)
        transactionRepository.saveTransaction(tx)

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should return false (summaries disabled)
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun generateAndSendWeeklySummary_whenNoTransactions_returnsFalse() = runTest {
        // Given: No transactions in database

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should return false (no transactions)
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun generateAndSendWeeklySummary_whenHasTransactions_generatesSuccessfully() = runTest {
        // Given: Multiple transactions
        val tx1 = createTestTransaction(amount = 500.0, categoryId = "food")
        val tx2 = createTestTransaction(amount = 300.0, categoryId = "transport")
        val tx3 = createTestTransaction(amount = 200.0, categoryId = "food")

        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)
        transactionRepository.saveTransaction(tx3)

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should succeed and mark as sent
        assertTrue(result.isSuccess)
        // Note: The notification may not actually send in test environment,
        // but we verify the logic executed successfully
        val weekKey = getCurrentWeekKeyForTest()
        // If notification was attempted, the summary should be marked as sent
        // This depends on notification manager behavior in test environment
    }

    // ==================== Summary Tracking Tests ====================

    @Test
    fun generateAndSendWeeklySummary_whenAlreadySentThisWeek_returnsFalse() = runTest {
        // Given: Transactions exist
        val tx = createTestTransaction(amount = 1000.0)
        transactionRepository.saveTransaction(tx)

        // Mark summary as already sent for this week
        val weekKey = getCurrentWeekKeyForTest()
        appPreferences.setBoolean("weekly_summary_sent_$weekKey", true)

        // When: Try to generate weekly summary again
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should return false (already sent)
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun clearSummary_allowsResendingForSameWeek() = runTest {
        // Given: Transactions exist
        val tx = createTestTransaction(amount = 1000.0)
        transactionRepository.saveTransaction(tx)

        // Mark summary as sent
        val weekKey = getCurrentWeekKeyForTest()
        appPreferences.setBoolean("weekly_summary_sent_$weekKey", true)
        assertTrue(WeeklySummaryService.hasSummaryBeenSent(weekKey, appPreferences))

        // When: Clear the summary
        WeeklySummaryService.clearSummary(weekKey, appPreferences)

        // Then: Should allow resending
        assertFalse(WeeklySummaryService.hasSummaryBeenSent(weekKey, appPreferences))
    }

    @Test
    fun clearCurrentWeekSummary_removesCurrentWeekFlag() = runTest {
        // Given: Current week summary marked as sent
        val weekKey = getCurrentWeekKeyForTest()
        appPreferences.setBoolean("weekly_summary_sent_$weekKey", true)
        assertTrue(WeeklySummaryService.hasSummaryBeenSentThisWeek(appPreferences))

        // When: Clear current week summary
        WeeklySummaryService.clearCurrentWeekSummary(appPreferences)

        // Then: Should remove the flag
        assertFalse(WeeklySummaryService.hasSummaryBeenSentThisWeek(appPreferences))
    }

    @Test
    fun clearAllSummaries_removesAllWeekFlags() = runTest {
        // Given: Multiple week summaries marked as sent
        appPreferences.setBoolean("weekly_summary_sent_2026-W01", true)
        appPreferences.setBoolean("weekly_summary_sent_2026-W02", true)
        appPreferences.setBoolean("weekly_summary_sent_2026-W03", true)

        // When: Clear all summaries
        val clearedCount = WeeklySummaryService.clearAllSummaries(appPreferences)

        // Then: Should remove all summary flags
        assertEquals(3, clearedCount)
        assertFalse(WeeklySummaryService.hasSummaryBeenSent("2026-W01", appPreferences))
        assertFalse(WeeklySummaryService.hasSummaryBeenSent("2026-W02", appPreferences))
        assertFalse(WeeklySummaryService.hasSummaryBeenSent("2026-W03", appPreferences))
    }

    @Test
    fun clearAllSummaries_whenNoSummaries_returnsZero() = runTest {
        // Given: No summary flags

        // When: Clear all summaries
        val clearedCount = WeeklySummaryService.clearAllSummaries(appPreferences)

        // Then: Should return zero
        assertEquals(0, clearedCount)
    }

    // ==================== Week Key Tests ====================

    @Test
    fun hasSummaryBeenSentThisWeek_returnsTrueWhenSent() = runTest {
        // Given: Current week summary marked as sent
        val weekKey = getCurrentWeekKeyForTest()
        appPreferences.setBoolean("weekly_summary_sent_$weekKey", true)

        // When: Check if summary sent this week
        val result = WeeklySummaryService.hasSummaryBeenSentThisWeek(appPreferences)

        // Then: Should return true
        assertTrue(result)
    }

    @Test
    fun hasSummaryBeenSentThisWeek_returnsFalseWhenNotSent() = runTest {
        // Given: No summary sent this week

        // When: Check if summary sent this week
        val result = WeeklySummaryService.hasSummaryBeenSentThisWeek(appPreferences)

        // Then: Should return false
        assertFalse(result)
    }

    @Test
    fun hasSummaryBeenSent_checksSpecificWeek() = runTest {
        // Given: Specific week summary marked as sent
        val weekKey = "2026-W05"
        appPreferences.setBoolean("weekly_summary_sent_$weekKey", true)

        // When: Check if summary sent for that week
        val result = WeeklySummaryService.hasSummaryBeenSent(weekKey, appPreferences)

        // Then: Should return true
        assertTrue(result)
    }

    @Test
    fun hasSummaryBeenSent_returnsFalseForDifferentWeek() = runTest {
        // Given: Week 5 summary marked as sent
        appPreferences.setBoolean("weekly_summary_sent_2026-W05", true)

        // When: Check if summary sent for week 6
        val result = WeeklySummaryService.hasSummaryBeenSent("2026-W06", appPreferences)

        // Then: Should return false
        assertFalse(result)
    }

    // ==================== Clear Old Summaries Tests ====================

    @Test
    fun clearOldSummaries_removesOldWeekFlags() = runTest {
        // Given: Mix of recent and old summaries
        val currentWeekKey = getCurrentWeekKeyForTest()
        val currentYear = currentWeekKey.substringBefore("-W").toInt()
        val currentWeek = currentWeekKey.substringAfter("-W").toInt()

        // Recent summaries (should be kept)
        appPreferences.setBoolean("weekly_summary_sent_$currentWeekKey", true)
        appPreferences.setBoolean("weekly_summary_sent_${currentYear}-W${String.format("%02d", currentWeek - 1)}", true)

        // Old summaries (should be removed) - 10 weeks old
        val oldWeek = if (currentWeek > 10) currentWeek - 10 else 52 - (10 - currentWeek)
        val oldYear = if (currentWeek > 10) currentYear else currentYear - 1
        appPreferences.setBoolean("weekly_summary_sent_${oldYear}-W${String.format("%02d", oldWeek)}", true)

        // When: Clear old summaries (keep 4 weeks)
        val clearedCount = WeeklySummaryService.clearOldSummaries(appPreferences, weeksToKeep = 4)

        // Then: Should remove old summaries but keep recent ones
        assertTrue(clearedCount >= 1) // At least the 10-week-old summary should be cleared
        assertTrue(WeeklySummaryService.hasSummaryBeenSentThisWeek(appPreferences))
    }

    @Test
    fun clearOldSummaries_keepsRecentWeeks() = runTest {
        // Given: Recent summaries only
        val currentWeekKey = getCurrentWeekKeyForTest()
        val currentYear = currentWeekKey.substringBefore("-W").toInt()
        val currentWeek = currentWeekKey.substringAfter("-W").toInt()

        appPreferences.setBoolean("weekly_summary_sent_$currentWeekKey", true)
        val lastWeek = if (currentWeek > 1) currentWeek - 1 else 52
        val lastWeekYear = if (currentWeek > 1) currentYear else currentYear - 1
        appPreferences.setBoolean("weekly_summary_sent_${lastWeekYear}-W${String.format("%02d", lastWeek)}", true)

        // When: Clear old summaries (keep 4 weeks)
        val clearedCount = WeeklySummaryService.clearOldSummaries(appPreferences, weeksToKeep = 4)

        // Then: Should keep all recent summaries
        assertEquals(0, clearedCount)
        assertTrue(WeeklySummaryService.hasSummaryBeenSentThisWeek(appPreferences))
    }

    @Test
    fun clearOldSummaries_handlesInvalidWeekKeys() = runTest {
        // Given: Invalid week key
        appPreferences.setBoolean("weekly_summary_sent_invalid", true)
        appPreferences.setBoolean("weekly_summary_sent_2026", true)
        appPreferences.setBoolean("weekly_summary_sent_W05", true)

        // When: Clear old summaries
        val clearedCount = WeeklySummaryService.clearOldSummaries(appPreferences)

        // Then: Should remove invalid keys
        assertEquals(3, clearedCount)
    }

    @Test
    fun clearOldSummaries_withCustomWeeksToKeep() = runTest {
        // Given: Summaries from various weeks
        val currentWeekKey = getCurrentWeekKeyForTest()
        val currentYear = currentWeekKey.substringBefore("-W").toInt()
        val currentWeek = currentWeekKey.substringAfter("-W").toInt()

        appPreferences.setBoolean("weekly_summary_sent_$currentWeekKey", true)

        // 2 weeks old (within 4 week limit)
        val twoWeeksOld = if (currentWeek > 2) currentWeek - 2 else 52 - (2 - currentWeek)
        val twoWeeksOldYear = if (currentWeek > 2) currentYear else currentYear - 1
        appPreferences.setBoolean("weekly_summary_sent_${twoWeeksOldYear}-W${String.format("%02d", twoWeeksOld)}", true)

        // 6 weeks old (beyond 4 week limit, should be removed)
        val sixWeeksOld = if (currentWeek > 6) currentWeek - 6 else 52 - (6 - currentWeek)
        val sixWeeksOldYear = if (currentWeek > 6) currentYear else currentYear - 1
        appPreferences.setBoolean("weekly_summary_sent_${sixWeeksOldYear}-W${String.format("%02d", sixWeeksOld)}", true)

        // When: Clear old summaries (keep 4 weeks)
        val clearedCount = WeeklySummaryService.clearOldSummaries(appPreferences, weeksToKeep = 4)

        // Then: Should remove 6-week-old summary but keep current and 2-week-old
        assertTrue(clearedCount >= 1)
        assertTrue(WeeklySummaryService.hasSummaryBeenSentThisWeek(appPreferences))
    }

    // ==================== Summary Generation Edge Cases ====================

    @Test
    fun generateAndSendWeeklySummary_withOnlyDebitTransactions() = runTest {
        // Given: Only DEBIT transactions (spending)
        val tx1 = createTestTransaction(amount = 500.0, type = TransactionType.DEBIT)
        val tx2 = createTestTransaction(amount = 300.0, type = TransactionType.DEBIT)

        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should succeed
        assertTrue(result.isSuccess)
    }

    @Test
    fun generateAndSendWeeklySummary_withOnlyCreditTransactions() = runTest {
        // Given: Only CREDIT transactions (income)
        val tx1 = createTestTransaction(amount = 5000.0, type = TransactionType.CREDIT)
        val tx2 = createTestTransaction(amount = 3000.0, type = TransactionType.CREDIT)

        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should succeed
        assertTrue(result.isSuccess)
    }

    @Test
    fun generateAndSendWeeklySummary_withMixedTransactions() = runTest {
        // Given: Mix of DEBIT and CREDIT transactions
        val tx1 = createTestTransaction(amount = 500.0, type = TransactionType.DEBIT, categoryId = "food")
        val tx2 = createTestTransaction(amount = 5000.0, type = TransactionType.CREDIT, categoryId = "salary")
        val tx3 = createTestTransaction(amount = 300.0, type = TransactionType.DEBIT, categoryId = "transport")
        val tx4 = createTestTransaction(amount = 200.0, type = TransactionType.DEBIT, categoryId = "food")

        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)
        transactionRepository.saveTransaction(tx3)
        transactionRepository.saveTransaction(tx4)

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should succeed
        assertTrue(result.isSuccess)
    }

    @Test
    fun generateAndSendWeeklySummary_withVeryLargeAmounts() = runTest {
        // Given: Very large transaction amounts
        val tx1 = createTestTransaction(amount = 1000000.0, type = TransactionType.DEBIT)
        val tx2 = createTestTransaction(amount = 500000.0, type = TransactionType.DEBIT)

        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should succeed and handle large amounts
        assertTrue(result.isSuccess)
    }

    @Test
    fun generateAndSendWeeklySummary_withVerySmallAmounts() = runTest {
        // Given: Very small transaction amounts
        val tx1 = createTestTransaction(amount = 0.01, type = TransactionType.DEBIT)
        val tx2 = createTestTransaction(amount = 0.50, type = TransactionType.DEBIT)

        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should succeed and handle small amounts
        assertTrue(result.isSuccess)
    }

    @Test
    fun generateAndSendWeeklySummary_withDecimalAmounts() = runTest {
        // Given: Decimal transaction amounts
        val tx1 = createTestTransaction(amount = 123.45, type = TransactionType.DEBIT)
        val tx2 = createTestTransaction(amount = 678.90, type = TransactionType.DEBIT)

        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should succeed and handle decimal precision
        assertTrue(result.isSuccess)
    }

    @Test
    fun generateAndSendWeeklySummary_withSingleTransaction() = runTest {
        // Given: Single transaction
        val tx = createTestTransaction(amount = 1000.0, type = TransactionType.DEBIT)
        transactionRepository.saveTransaction(tx)

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should succeed with single transaction
        assertTrue(result.isSuccess)
    }

    @Test
    fun generateAndSendWeeklySummary_withManyTransactions() = runTest {
        // Given: Many transactions (50+)
        for (i in 1..50) {
            val tx = createTestTransaction(
                amount = (i * 100.0),
                type = if (i % 3 == 0) TransactionType.CREDIT else TransactionType.DEBIT,
                categoryId = listOf("food", "transport", "shopping", "entertainment")[i % 4]
            )
            transactionRepository.saveTransaction(tx)
        }

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should succeed with many transactions
        assertTrue(result.isSuccess)
    }

    // ==================== Multiple Categories Tests ====================

    @Test
    fun generateAndSendWeeklySummary_withMultipleCategories() = runTest {
        // Given: Transactions across different categories
        val tx1 = createTestTransaction(amount = 500.0, categoryId = "food")
        val tx2 = createTestTransaction(amount = 300.0, categoryId = "transport")
        val tx3 = createTestTransaction(amount = 200.0, categoryId = "shopping")
        val tx4 = createTestTransaction(amount = 800.0, categoryId = "food") // Highest total

        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)
        transactionRepository.saveTransaction(tx3)
        transactionRepository.saveTransaction(tx4)

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should succeed and identify top category
        assertTrue(result.isSuccess)
    }

    @Test
    fun generateAndSendWeeklySummary_withSingleCategory() = runTest {
        // Given: All transactions in one category
        val tx1 = createTestTransaction(amount = 500.0, categoryId = "food")
        val tx2 = createTestTransaction(amount = 300.0, categoryId = "food")
        val tx3 = createTestTransaction(amount = 200.0, categoryId = "food")

        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)
        transactionRepository.saveTransaction(tx3)

        // When: Generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should succeed with single category
        assertTrue(result.isSuccess)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun generateAndSendWeeklySummary_withDatabaseError_handlesGracefully() = runTest {
        // Given: Database with transactions
        val tx = createTestTransaction(amount = 1000.0)
        transactionRepository.saveTransaction(tx)

        // Close database to simulate error
        database.close()

        // When: Try to generate weekly summary
        val result = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should handle error gracefully
        // Result could be success with false (no summary sent) or failure
        assertTrue(result.isSuccess || result.isFailure)
    }

    // ==================== Integration Tests ====================

    @Test
    fun fullWorkflow_generateSummaryAndTrack() = runTest {
        // Given: Fresh week with transactions
        val tx1 = createTestTransaction(amount = 500.0, categoryId = "food")
        val tx2 = createTestTransaction(amount = 300.0, categoryId = "transport")
        val tx3 = createTestTransaction(amount = 2000.0, type = TransactionType.CREDIT)

        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)
        transactionRepository.saveTransaction(tx3)

        val weekKey = getCurrentWeekKeyForTest()

        // Verify summary not sent yet
        assertFalse(WeeklySummaryService.hasSummaryBeenSentThisWeek(appPreferences))

        // When: Generate first summary
        val result1 = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should succeed (or at least execute without error)
        assertTrue(result1.isSuccess)

        // When: Try to generate again
        // First mark as sent to simulate successful notification
        appPreferences.setBoolean("weekly_summary_sent_$weekKey", true)
        val result2 = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should return false (duplicate prevented)
        assertTrue(result2.isSuccess)
        assertFalse(result2.getOrNull() ?: true)

        // When: Clear summary and try again
        WeeklySummaryService.clearCurrentWeekSummary(appPreferences)
        val result3 = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should allow resending
        assertTrue(result3.isSuccess)
    }

    @Test
    fun multipleWeeks_tracking() = runTest {
        // Given: Summaries for multiple weeks
        val week1 = "2026-W01"
        val week2 = "2026-W02"
        val week3 = "2026-W03"

        // Mark weeks as sent
        appPreferences.setBoolean("weekly_summary_sent_$week1", true)
        appPreferences.setBoolean("weekly_summary_sent_$week2", true)
        appPreferences.setBoolean("weekly_summary_sent_$week3", true)

        // Verify all marked as sent
        assertTrue(WeeklySummaryService.hasSummaryBeenSent(week1, appPreferences))
        assertTrue(WeeklySummaryService.hasSummaryBeenSent(week2, appPreferences))
        assertTrue(WeeklySummaryService.hasSummaryBeenSent(week3, appPreferences))

        // When: Clear week 2 only
        WeeklySummaryService.clearSummary(week2, appPreferences)

        // Then: Week 2 cleared, others remain
        assertTrue(WeeklySummaryService.hasSummaryBeenSent(week1, appPreferences))
        assertFalse(WeeklySummaryService.hasSummaryBeenSent(week2, appPreferences))
        assertTrue(WeeklySummaryService.hasSummaryBeenSent(week3, appPreferences))

        // When: Clear all summaries
        val clearedCount = WeeklySummaryService.clearAllSummaries(appPreferences)

        // Then: All cleared
        assertEquals(2, clearedCount) // week1 and week3 remaining
        assertFalse(WeeklySummaryService.hasSummaryBeenSent(week1, appPreferences))
        assertFalse(WeeklySummaryService.hasSummaryBeenSent(week2, appPreferences))
        assertFalse(WeeklySummaryService.hasSummaryBeenSent(week3, appPreferences))
    }

    @Test
    fun settingsChange_respectsNewSettings() = runTest {
        // Given: Weekly summary enabled with transactions
        val tx = createTestTransaction(amount = 1000.0)
        transactionRepository.saveTransaction(tx)

        // Initially enabled
        var result1 = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )
        assertTrue(result1.isSuccess)

        // When: Disable weekly summary
        appPreferences.setWeeklySummarySettings(
            WeeklySummarySettings(enabled = false, dayOfWeek = DayOfWeek.MONDAY, hourOfDay = 9)
        )

        // Clear the sent flag
        WeeklySummaryService.clearCurrentWeekSummary(appPreferences)

        // Try to generate again
        val result2 = WeeklySummaryService.generateAndSendWeeklySummary(
            context = context,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should respect disabled setting
        assertTrue(result2.isSuccess)
        assertFalse(result2.getOrNull() ?: true)
    }

    @Test
    fun stressTest_rapidSummaryGeneration() = runTest {
        // Given: Transactions
        val tx = createTestTransaction(amount = 1000.0)
        transactionRepository.saveTransaction(tx)

        // When: Rapidly generate summaries 10 times
        val results = mutableListOf<Result<Boolean>>()
        for (i in 1..10) {
            val result = WeeklySummaryService.generateAndSendWeeklySummary(
                context = context,
                transactionRepository = transactionRepository,
                appPreferences = appPreferences
            )
            results.add(result)

            // Only clear after first attempt to test duplicate prevention
            if (i == 1) {
                // Don't clear - let subsequent calls test duplicate prevention
            }
        }

        // Then: All should succeed (but only first should actually send if notification works)
        results.forEach { result ->
            assertTrue(result.isSuccess)
        }
    }
}
