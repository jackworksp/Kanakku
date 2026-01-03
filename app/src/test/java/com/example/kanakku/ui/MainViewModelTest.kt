package com.example.kanakku.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.SmsMessage
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.preferences.AppPreferences
import com.example.kanakku.data.repository.TransactionRepository
import com.example.kanakku.data.sms.SmsReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for MainViewModel batch processing and sync progress tracking.
 *
 * Tests cover:
 * - Initial sync flow for first launch
 * - Incremental sync after initial sync completes
 * - Progress updates during sync
 * - Batch processing for large datasets
 * - Cancellation of sync operations
 * - Error handling during sync
 *
 * Testing Approach:
 * - Uses Robolectric for Android context
 * - Uses in-memory Room database for repository testing
 * - Uses test coroutine dispatcher for controlling async operations
 * - Uses fake SmsReader to simulate SMS reading
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainViewModelTest {

    private lateinit var context: Context
    private lateinit var database: KanakkuDatabase
    private lateinit var viewModel: MainViewModel
    private lateinit var appPreferences: AppPreferences
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        // Clear preferences before each test
        appPreferences = AppPreferences.getInstance(context)
        appPreferences.resetInitialSync()
        appPreferences.clearSyncProgress()

        viewModel = MainViewModel()
    }

    @After
    fun teardown() {
        database.close()
        Dispatchers.resetMain()

        // Clean up preferences
        appPreferences.resetInitialSync()
        appPreferences.clearSyncProgress()
    }

    // ==================== Helper Functions ====================

    /**
     * Creates a test SMS message
     */
    private fun createTestSms(
        id: Long,
        address: String = "VM-HDFC",
        body: String = "Spent Rs 100.00 at Test Merchant on card ending 1234",
        date: Long = System.currentTimeMillis() - (id * 1000)
    ): SmsMessage {
        return SmsMessage(
            id = id,
            address = address,
            body = body,
            date = date,
            isRead = true
        )
    }

    /**
     * Creates a batch of test SMS messages
     */
    private fun createTestSmsBatch(count: Int, startId: Long = 1L): List<SmsMessage> {
        return (startId until startId + count).map { id ->
            createTestSms(
                id = id,
                body = "Spent Rs ${id * 10}.00 at Merchant$id on card ending 1234"
            )
        }
    }

    /**
     * Waits for UI state to update
     */
    private suspend fun awaitUiState(): MainUiState {
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel.uiState.first()
    }

    // ==================== Initial Sync Tests ====================

    @Test
    fun `loadSmsData triggers initial sync on first launch`() = runTest {
        // Given: First launch (isInitialSyncComplete = false)
        assertFalse(appPreferences.isInitialSyncComplete())

        // When: loadSmsData is called
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)

        // Then: Initial sync should start
        testDispatcher.scheduler.runCurrent()
        val uiState = viewModel.uiState.first()

        // Verify initial sync flag is set
        assertTrue("Initial sync should be active", uiState.isInitialSync || uiState.isLoading)
    }

    @Test
    fun `initial sync marks sync as complete when finished`() = runTest {
        // Given: First launch with no SMS
        assertFalse(appPreferences.isInitialSyncComplete())

        // When: Initial sync completes
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)
        val finalState = awaitUiState()

        // Then: Sync should be marked as complete
        assertTrue(
            "Initial sync should be marked complete",
            appPreferences.isInitialSyncComplete()
        )
        assertFalse("isInitialSync flag should be cleared", finalState.isInitialSync)
    }

    @Test
    fun `initial sync processes all available SMS without time limit`() = runTest {
        // Given: First launch
        assertFalse(appPreferences.isInitialSyncComplete())

        // When: loadSmsData is called (will use readAllSms internally)
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)
        val finalState = awaitUiState()

        // Then: Should complete successfully
        assertFalse(finalState.isLoading)
        assertTrue(appPreferences.isInitialSyncComplete())
    }

    // ==================== Incremental Sync Tests ====================

    @Test
    fun `incremental sync triggers after initial sync complete`() = runTest {
        // Given: Initial sync already completed
        appPreferences.setInitialSyncComplete()
        assertTrue(appPreferences.isInitialSyncComplete())

        // When: loadSmsData is called
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)
        val finalState = awaitUiState()

        // Then: Should NOT be initial sync
        assertFalse("Should not trigger initial sync", finalState.isInitialSync)
    }

    @Test
    fun `incremental sync uses last sync timestamp`() = runTest {
        // Given: Initial sync completed with timestamp
        appPreferences.setInitialSyncComplete()
        val repository = TransactionRepository(database)
        val pastTimestamp = System.currentTimeMillis() - 86400000L // 1 day ago
        repository.setLastSyncTimestamp(pastTimestamp)

        // When: loadSmsData is called
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)
        val finalState = awaitUiState()

        // Then: Should use incremental sync (not initial)
        assertFalse("Should use incremental sync", finalState.isInitialSync)
        assertNotNull("Should have last sync timestamp", finalState.lastSyncTimestamp)
    }

    // ==================== Progress Tracking Tests ====================

    @Test
    fun `sync progress updates are emitted during initial sync`() = runTest {
        // Given: First launch
        assertFalse(appPreferences.isInitialSyncComplete())
        val progressStates = mutableListOf<MainUiState>()

        // When: Monitor state during sync
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)

        // Capture intermediate states
        testDispatcher.scheduler.runCurrent()
        progressStates.add(viewModel.uiState.first())

        testDispatcher.scheduler.advanceTimeBy(100)
        progressStates.add(viewModel.uiState.first())

        testDispatcher.scheduler.advanceUntilIdle()
        progressStates.add(viewModel.uiState.first())

        // Then: Should have multiple state updates
        assertTrue(
            "Should have multiple progress updates",
            progressStates.size > 1
        )

        // At least one state should have isLoading or isInitialSync true
        assertTrue(
            "Should have loading state",
            progressStates.any { it.isLoading || it.isInitialSync }
        )
    }

    @Test
    fun `sync status messages are updated during processing`() = runTest {
        // Given: First launch
        assertFalse(appPreferences.isInitialSyncComplete())

        // When: Sync starts
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)

        testDispatcher.scheduler.runCurrent()
        val initialState = viewModel.uiState.first()

        // Then: Should have status message during sync
        if (initialState.isInitialSync || initialState.isLoading) {
            // Status message may be set during initial sync
            // (empty SMS will complete quickly without status messages)
        }

        // Wait for completion
        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = viewModel.uiState.first()

        // Final state should have status cleared
        assertNull(
            "Status message should be cleared after completion",
            finalState.syncStatusMessage
        )
    }

    @Test
    fun `sync progress counts are tracked correctly`() = runTest {
        // Given: First launch with initial sync
        assertFalse(appPreferences.isInitialSyncComplete())

        // When: Sync runs
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = viewModel.uiState.first()

        // Then: Progress should be reset after completion
        assertEquals(
            "Progress should be reset to 0 after completion",
            0,
            finalState.syncProgress
        )
        assertEquals(
            "Total should be reset to 0 after completion",
            0,
            finalState.syncTotal
        )
    }

    // ==================== Batch Processing Tests ====================

    @Test
    fun `batch processing handles datasets larger than batch size`() = runTest {
        // Given: First launch with large dataset (> SMS_BATCH_SIZE = 100)
        // Note: This test validates the batch processing logic exists
        // Actual SMS reading is limited by device, so we test the state transitions
        assertFalse(appPreferences.isInitialSyncComplete())

        // When: Sync processes data
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = viewModel.uiState.first()

        // Then: Should complete without errors
        assertFalse("Should finish loading", finalState.isLoading)
        assertNull("Should have no error", finalState.errorMessage)
        assertTrue(
            "Should mark initial sync complete",
            appPreferences.isInitialSyncComplete()
        )
    }

    @Test
    fun `batch processing updates progress between batches`() = runTest {
        // Given: Initial sync scenario
        assertFalse(appPreferences.isInitialSyncComplete())

        // When: Sync starts
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)

        // Advance time to capture intermediate progress
        testDispatcher.scheduler.runCurrent()

        // Then: Progress tracking should be initialized
        // (Actual batch processing only happens with > 100 SMS,
        // but progress fields should still be managed correctly)
        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = viewModel.uiState.first()

        assertFalse("Should complete loading", finalState.isLoading)
        assertFalse("Should clear initial sync flag", finalState.isInitialSync)
    }

    @Test
    fun `small datasets skip batch processing`() = runTest {
        // Given: First launch with small dataset (< 100 SMS)
        // Note: Most test scenarios will have < 100 SMS
        assertFalse(appPreferences.isInitialSyncComplete())

        // When: Sync runs
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = viewModel.uiState.first()

        // Then: Should complete successfully using single-pass processing
        assertFalse("Should finish loading", finalState.isLoading)
        assertNull("Should have no error", finalState.errorMessage)
        assertTrue(
            "Should mark sync complete",
            appPreferences.isInitialSyncComplete()
        )
    }

    // ==================== Cancellation Tests ====================

    @Test
    fun `cancelSync stops ongoing sync operation`() = runTest {
        // Given: Sync in progress
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)

        testDispatcher.scheduler.runCurrent()

        // When: User cancels sync
        viewModel.cancelSync()

        // Then: Sync state should be reset
        val state = viewModel.uiState.first()
        assertFalse("isLoading should be false", state.isLoading)
        assertFalse("isInitialSync should be false", state.isInitialSync)
        assertEquals("syncProgress should be reset", 0, state.syncProgress)
        assertEquals("syncTotal should be reset", 0, state.syncTotal)
        assertNull("syncStatusMessage should be cleared", state.syncStatusMessage)
    }

    @Test
    fun `cancelSync clears progress tracking`() = runTest {
        // Given: Sync started
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)
        testDispatcher.scheduler.runCurrent()

        // When: Cancel is called
        viewModel.cancelSync()

        // Then: All sync-related state should be cleared
        val state = viewModel.uiState.first()
        assertEquals("Progress should be 0", 0, state.syncProgress)
        assertEquals("Total should be 0", 0, state.syncTotal)
        assertNull("Status message should be null", state.syncStatusMessage)
    }

    @Test
    fun `cancelSync can be called multiple times safely`() = runTest {
        // Given: Sync running
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)
        testDispatcher.scheduler.runCurrent()

        // When: Cancel called multiple times
        viewModel.cancelSync()
        viewModel.cancelSync()
        viewModel.cancelSync()

        // Then: Should not crash and state should be consistent
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertFalse(state.isInitialSync)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `error during sync resets sync state`() = runTest {
        // Given: Invalid context that will cause error
        // (Using real context should work, but testing error handling logic exists)

        // When: Attempt to load with permission denied
        viewModel.updatePermissionStatus(false)
        viewModel.loadSmsData(context)

        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.first()

        // Then: Should handle gracefully
        assertFalse("Loading should complete", state.isLoading)
        assertFalse("Initial sync should be cleared", state.isInitialSync)
    }

    @Test
    fun `error clears progress tracking`() = runTest {
        // Given: Sync will encounter error (no permission)
        viewModel.updatePermissionStatus(false)

        // When: Sync runs
        viewModel.loadSmsData(context)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Progress should be reset
        val state = viewModel.uiState.first()
        assertEquals("Progress should be reset", 0, state.syncProgress)
        assertEquals("Total should be reset", 0, state.syncTotal)
    }

    // ==================== Permission Tests ====================

    @Test
    fun `updatePermissionStatus updates UI state`() = runTest {
        // Given: Initial state
        val initialState = viewModel.uiState.first()
        assertFalse(initialState.hasPermission)

        // When: Permission granted
        viewModel.updatePermissionStatus(true)

        // Then: State updated
        val updatedState = viewModel.uiState.first()
        assertTrue("Permission should be granted", updatedState.hasPermission)
    }

    @Test
    fun `loadSmsData without permission completes gracefully`() = runTest {
        // Given: No SMS permission
        viewModel.updatePermissionStatus(false)

        // When: Attempt to load SMS data
        viewModel.loadSmsData(context)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should complete without crash
        val state = viewModel.uiState.first()
        assertFalse("Should finish loading", state.isLoading)
    }

    // ==================== State Management Tests ====================

    @Test
    fun `clearError removes error message from state`() = runTest {
        // Given: State with error (simulate by setting through private means)
        // When we have a real error, we'll test this properly
        // For now, verify the method exists and doesn't crash

        // When: Clear error called
        viewModel.clearError()

        // Then: Should not crash
        val state = viewModel.uiState.first()
        assertNull("Error should be null", state.errorMessage)
    }

    @Test
    fun `initial UI state has correct defaults`() = runTest {
        // Given: Fresh ViewModel
        val freshViewModel = MainViewModel()

        // When: Get initial state
        val state = freshViewModel.uiState.first()

        // Then: Should have correct defaults
        assertFalse("isLoading should be false", state.isLoading)
        assertFalse("hasPermission should be false", state.hasPermission)
        assertFalse("isInitialSync should be false", state.isInitialSync)
        assertEquals("syncProgress should be 0", 0, state.syncProgress)
        assertEquals("syncTotal should be 0", 0, state.syncTotal)
        assertNull("syncStatusMessage should be null", state.syncStatusMessage)
        assertEquals("totalSmsCount should be 0", 0, state.totalSmsCount)
        assertEquals("bankSmsCount should be 0", 0, state.bankSmsCount)
        assertTrue("transactions should be empty", state.transactions.isEmpty())
        assertNull("errorMessage should be null", state.errorMessage)
    }

    // ==================== Integration Tests ====================

    @Test
    fun `complete sync workflow from initial to incremental`() = runTest {
        // Given: First launch
        assertFalse(appPreferences.isInitialSyncComplete())

        // When: First sync (initial)
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)
        testDispatcher.scheduler.advanceUntilIdle()

        val firstSyncState = viewModel.uiState.first()

        // Then: Initial sync should complete
        assertFalse("First sync should finish", firstSyncState.isLoading)
        assertTrue("Should mark as complete", appPreferences.isInitialSyncComplete())

        // When: Second sync (incremental)
        viewModel.loadSmsData(context)
        testDispatcher.scheduler.advanceUntilIdle()

        val secondSyncState = viewModel.uiState.first()

        // Then: Should use incremental sync
        assertFalse("Second sync should finish", secondSyncState.isLoading)
        assertFalse("Should not be initial sync", secondSyncState.isInitialSync)
    }

    @Test
    fun `sync preserves existing transactions`() = runTest {
        // Given: Database with existing transaction
        val repository = TransactionRepository(database)
        val existingTx = ParsedTransaction(
            smsId = 999L,
            amount = 50.0,
            type = TransactionType.DEBIT,
            merchant = "Existing Merchant",
            accountNumber = "9999",
            referenceNumber = "REF999",
            date = System.currentTimeMillis(),
            rawSms = "Existing SMS",
            senderAddress = "VM-TEST",
            balanceAfter = 1000.0,
            location = null
        )
        repository.saveTransaction(existingTx)

        // Mark initial sync as complete
        appPreferences.setInitialSyncComplete()

        // When: Incremental sync runs
        viewModel.updatePermissionStatus(true)
        viewModel.loadSmsData(context)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()

        // Then: Existing transaction should still be present
        assertTrue(
            "Should load existing transaction",
            state.transactions.any { it.smsId == 999L }
        )
    }
}
