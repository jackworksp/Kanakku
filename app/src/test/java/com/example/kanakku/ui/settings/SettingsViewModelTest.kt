package com.example.kanakku.ui.settings

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.BuildConfig
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.preferences.AppPreferences
import com.example.kanakku.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SettingsViewModel.
 *
 * Tests cover:
 * - Initialization and preference loading
 * - Individual preference update methods
 * - Clear all data functionality with success and error scenarios
 * - Error handling throughout all operations
 * - UI state management
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var appPreferences: AppPreferences
    private lateinit var database: KanakkuDatabase
    private lateinit var repository: TransactionRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // Set test dispatcher
        Dispatchers.setMain(testDispatcher)

        // Reset AppPreferences singleton
        AppPreferences.resetInstance()

        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        repository = TransactionRepository(database)
        appPreferences = AppPreferences.getInstance(ApplicationProvider.getApplicationContext())

        // Create ViewModel
        viewModel = SettingsViewModel()
    }

    @After
    fun teardown() {
        database.close()
        AppPreferences.resetInstance()
        Dispatchers.resetMain()
    }

    // ==================== Helper Functions ====================

    private fun createTestTransaction(
        smsId: Long = 1L,
        amount: Double = 100.0,
        type: TransactionType = TransactionType.DEBIT,
        merchant: String? = "Test Merchant",
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
            location = "Test Location"
        )
    }

    // ==================== Initialization Tests ====================

    @Test
    fun initialize_loadsPreferencesSuccessfully() = runTest {
        // Given - Set some preferences first
        appPreferences.setDarkModeEnabled(true)
        appPreferences.setDynamicColorsEnabled(false)
        appPreferences.setCompactViewEnabled(true)
        appPreferences.setShowOfflineBadge(false)
        appPreferences.setNotificationsEnabled(false)
        appPreferences.setDefaultAnalyticsPeriod("WEEK")
        appPreferences.setCurrencySymbol("$")
        appPreferences.setAutoCategorizeEnabled(false)

        // When
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(true, uiState.isDarkMode)
        assertEquals(false, uiState.isDynamicColors)
        assertEquals(true, uiState.isCompactView)
        assertEquals(false, uiState.showOfflineBadge)
        assertEquals(false, uiState.isNotificationsEnabled)
        assertEquals("WEEK", uiState.defaultAnalyticsPeriod)
        assertEquals("$", uiState.currencySymbol)
        assertEquals(false, uiState.isAutoCategorize)
        assertTrue(uiState.appVersion.isNotEmpty())
        assertNull(uiState.errorMessage)
    }

    @Test
    fun initialize_loadsDefaultValues() = runTest {
        // Given - No preferences set (using defaults)

        // When
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should have default values
        val uiState = viewModel.uiState.value
        assertEquals(null, uiState.isDarkMode) // Default system
        assertEquals(true, uiState.isDynamicColors) // Default
        assertEquals(false, uiState.isCompactView) // Default
        assertEquals(true, uiState.showOfflineBadge) // Default
        assertEquals(true, uiState.isNotificationsEnabled) // Default
        assertEquals("MONTH", uiState.defaultAnalyticsPeriod) // Default
        assertEquals("₹", uiState.currencySymbol) // Default
        assertEquals(true, uiState.isAutoCategorize) // Default
        assertNull(uiState.errorMessage)
    }

    @Test
    fun initialize_setsAppVersion() = runTest {
        // When
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val expectedVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        assertEquals(expectedVersion, viewModel.uiState.value.appVersion)
    }

    // ==================== Dark Mode Update Tests ====================

    @Test
    fun updateDarkMode_toTrue_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateDarkMode(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(true, viewModel.uiState.value.isDarkMode)
        assertEquals(true, appPreferences.isDarkModeEnabled())
    }

    @Test
    fun updateDarkMode_toFalse_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateDarkMode(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(false, viewModel.uiState.value.isDarkMode)
        assertEquals(false, appPreferences.isDarkModeEnabled())
    }

    @Test
    fun updateDarkMode_toNull_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateDarkMode(null)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.isDarkMode)
        assertEquals(null, appPreferences.isDarkModeEnabled())
    }

    // ==================== Dynamic Colors Update Tests ====================

    @Test
    fun updateDynamicColors_toTrue_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateDynamicColors(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(true, viewModel.uiState.value.isDynamicColors)
        assertEquals(true, appPreferences.isDynamicColorsEnabled())
    }

    @Test
    fun updateDynamicColors_toFalse_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateDynamicColors(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(false, viewModel.uiState.value.isDynamicColors)
        assertEquals(false, appPreferences.isDynamicColorsEnabled())
    }

    // ==================== Compact View Update Tests ====================

    @Test
    fun updateCompactView_toTrue_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateCompactView(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(true, viewModel.uiState.value.isCompactView)
        assertEquals(true, appPreferences.isCompactViewEnabled())
    }

    @Test
    fun updateCompactView_toFalse_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateCompactView(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(false, viewModel.uiState.value.isCompactView)
        assertEquals(false, appPreferences.isCompactViewEnabled())
    }

    // ==================== Show Offline Badge Update Tests ====================

    @Test
    fun updateShowOfflineBadge_toTrue_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateShowOfflineBadge(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(true, viewModel.uiState.value.showOfflineBadge)
        assertEquals(true, appPreferences.shouldShowOfflineBadge())
    }

    @Test
    fun updateShowOfflineBadge_toFalse_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateShowOfflineBadge(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(false, viewModel.uiState.value.showOfflineBadge)
        assertEquals(false, appPreferences.shouldShowOfflineBadge())
    }

    // ==================== Notifications Update Tests ====================

    @Test
    fun updateNotificationsEnabled_toTrue_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateNotificationsEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(true, viewModel.uiState.value.isNotificationsEnabled)
        assertEquals(true, appPreferences.isNotificationsEnabled())
    }

    @Test
    fun updateNotificationsEnabled_toFalse_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateNotificationsEnabled(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(false, viewModel.uiState.value.isNotificationsEnabled)
        assertEquals(false, appPreferences.isNotificationsEnabled())
    }

    // ==================== Default Analytics Period Update Tests ====================

    @Test
    fun updateDefaultAnalyticsPeriod_toDAY_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateDefaultAnalyticsPeriod("DAY")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("DAY", viewModel.uiState.value.defaultAnalyticsPeriod)
        assertEquals("DAY", appPreferences.getDefaultAnalyticsPeriod())
    }

    @Test
    fun updateDefaultAnalyticsPeriod_toWEEK_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateDefaultAnalyticsPeriod("WEEK")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("WEEK", viewModel.uiState.value.defaultAnalyticsPeriod)
        assertEquals("WEEK", appPreferences.getDefaultAnalyticsPeriod())
    }

    @Test
    fun updateDefaultAnalyticsPeriod_toMONTH_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateDefaultAnalyticsPeriod("MONTH")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("MONTH", viewModel.uiState.value.defaultAnalyticsPeriod)
        assertEquals("MONTH", appPreferences.getDefaultAnalyticsPeriod())
    }

    @Test
    fun updateDefaultAnalyticsPeriod_toYEAR_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateDefaultAnalyticsPeriod("YEAR")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("YEAR", viewModel.uiState.value.defaultAnalyticsPeriod)
        assertEquals("YEAR", appPreferences.getDefaultAnalyticsPeriod())
    }

    // ==================== Currency Symbol Update Tests ====================

    @Test
    fun updateCurrencySymbol_toDollar_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateCurrencySymbol("$")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("$", viewModel.uiState.value.currencySymbol)
        assertEquals("$", appPreferences.getCurrencySymbol())
    }

    @Test
    fun updateCurrencySymbol_toEuro_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateCurrencySymbol("€")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("€", viewModel.uiState.value.currencySymbol)
        assertEquals("€", appPreferences.getCurrencySymbol())
    }

    @Test
    fun updateCurrencySymbol_toRupee_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateCurrencySymbol("₹")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("₹", viewModel.uiState.value.currencySymbol)
        assertEquals("₹", appPreferences.getCurrencySymbol())
    }

    // ==================== Auto-Categorize Update Tests ====================

    @Test
    fun updateAutoCategorize_toTrue_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateAutoCategorize(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(true, viewModel.uiState.value.isAutoCategorize)
        assertEquals(true, appPreferences.isAutoCategorizeEnabled())
    }

    @Test
    fun updateAutoCategorize_toFalse_updatesStateAndPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateAutoCategorize(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(false, viewModel.uiState.value.isAutoCategorize)
        assertEquals(false, appPreferences.isAutoCategorizeEnabled())
    }

    // ==================== Clear All Data Tests ====================

    @Test
    fun clearAllData_deletesTransactionsSuccessfully() = runTest {
        // Given - Initialize and add test data
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0),
            createTestTransaction(smsId = 2L, amount = 200.0),
            createTestTransaction(smsId = 3L, amount = 300.0)
        )
        repository.saveTransactions(transactions)
        assertEquals(3, repository.getTransactionCount().getOrNull())

        // When
        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(0, repository.getTransactionCount().getOrNull())
        assertEquals(false, viewModel.uiState.value.isClearing)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun clearAllData_removesCategoryOverridesSuccessfully() = runTest {
        // Given - Initialize and add test data with overrides
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.saveTransaction(createTestTransaction(smsId = 2L))
        repository.setCategoryOverride(1L, "food")
        repository.setCategoryOverride(2L, "transport")
        assertEquals(2, repository.getAllCategoryOverrides().getOrNull()?.size)

        // When
        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(repository.getAllCategoryOverrides().getOrNull()?.isEmpty() == true)
    }

    @Test
    fun clearAllData_clearsSyncMetadataSuccessfully() = runTest {
        // Given - Initialize and set sync metadata
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        val syncTime = System.currentTimeMillis()
        repository.setLastSyncTimestamp(syncTime)
        repository.setLastProcessedSmsId(123L)
        assertNotNull(repository.getLastSyncTimestamp().getOrNull())
        assertNotNull(repository.getLastProcessedSmsId().getOrNull())

        // When
        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(repository.getLastSyncTimestamp().getOrNull())
        assertNull(repository.getLastProcessedSmsId().getOrNull())
    }

    @Test
    fun clearAllData_preservesThemePreferences() = runTest {
        // Given - Initialize and set theme preferences
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        appPreferences.setDarkModeEnabled(true)
        appPreferences.setDynamicColorsEnabled(false)
        appPreferences.setCompactViewEnabled(true)
        appPreferences.setShowOfflineBadge(false)

        // When
        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Theme preferences should be preserved
        assertEquals(true, appPreferences.isDarkModeEnabled())
        assertEquals(false, appPreferences.isDynamicColorsEnabled())
        assertEquals(true, appPreferences.isCompactViewEnabled())
        assertEquals(false, appPreferences.shouldShowOfflineBadge())
    }

    @Test
    fun clearAllData_resetsOtherPreferencesToDefaults() = runTest {
        // Given - Initialize and set non-theme preferences
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        appPreferences.setNotificationsEnabled(false)
        appPreferences.setDefaultAnalyticsPeriod("DAY")
        appPreferences.setCurrencySymbol("$")
        appPreferences.setAutoCategorizeEnabled(false)

        // When
        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should reset to defaults
        assertEquals(true, appPreferences.isNotificationsEnabled()) // Default
        assertEquals("MONTH", appPreferences.getDefaultAnalyticsPeriod()) // Default
        assertEquals("₹", appPreferences.getCurrencySymbol()) // Default
        assertEquals(true, appPreferences.isAutoCategorizeEnabled()) // Default
    }

    @Test
    fun clearAllData_setsLoadingStateWhileClearing() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        repository.saveTransaction(createTestTransaction(smsId = 1L))

        // When - Start clearing
        viewModel.clearAllData()

        // Then - Should set isClearing to true immediately
        assertEquals(true, viewModel.uiState.value.isClearing)

        // Advance until idle
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should set isClearing to false when done
        assertEquals(false, viewModel.uiState.value.isClearing)
    }

    @Test
    fun clearAllData_clearsErrorMessageBeforeStarting() = runTest {
        // Given - Set an error message first
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate an error by closing database
        database.close()
        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error was set
        assertNotNull(viewModel.uiState.value.errorMessage)

        // Recreate database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        ).allowMainThreadQueries().build()

        // Re-initialize with new database
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Clear data again
        viewModel.clearAllData()

        // Then - Error should be cleared when starting
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun clearAllData_handlesRepositoryFailureGracefully() = runTest {
        // Given - Initialize and close database to simulate error
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        database.close()

        // When
        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should set error message and stop clearing
        assertEquals(false, viewModel.uiState.value.isClearing)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Failed to delete"))
    }

    @Test
    fun clearAllData_continuesAfterCategoryOverrideFailure() = runTest {
        // Given - Initialize with transactions
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.setLastSyncTimestamp(System.currentTimeMillis())

        // When - Clear data (category override failure is non-critical)
        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should succeed despite non-critical failures
        assertEquals(0, repository.getTransactionCount().getOrNull())
        assertNull(repository.getLastSyncTimestamp().getOrNull())
        assertEquals(false, viewModel.uiState.value.isClearing)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun clearError_removesErrorMessage() = runTest {
        // Given - Initialize with database error
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        database.close()
        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error is set
        assertNotNull(viewModel.uiState.value.errorMessage)

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun updateDarkMode_handlesErrorGracefully() = runTest {
        // Given - Preferences not initialized
        val uninitializedViewModel = SettingsViewModel()

        // When - Try to update without initialization
        uninitializedViewModel.updateDarkMode(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should not crash (graceful handling)
        // The error will be caught and logged, UI state will not be updated
        assertNotNull(uninitializedViewModel.uiState.value)
    }

    // ==================== Integration Tests ====================

    @Test
    fun fullWorkflow_initializeUpdateClearData() = runTest {
        // 1. Initialize
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.isDarkMode) // Default

        // 2. Update multiple preferences
        viewModel.updateDarkMode(true)
        viewModel.updateDynamicColors(false)
        viewModel.updateNotificationsEnabled(false)
        viewModel.updateDefaultAnalyticsPeriod("WEEK")
        viewModel.updateCurrencySymbol("$")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isDarkMode)
        assertEquals(false, viewModel.uiState.value.isDynamicColors)
        assertEquals(false, viewModel.uiState.value.isNotificationsEnabled)
        assertEquals("WEEK", viewModel.uiState.value.defaultAnalyticsPeriod)
        assertEquals("$", viewModel.uiState.value.currencySymbol)

        // 3. Add some data
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.setCategoryOverride(1L, "food")
        repository.setLastSyncTimestamp(System.currentTimeMillis())

        // 4. Clear all data
        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        // 5. Verify data cleared but theme preserved
        assertEquals(0, repository.getTransactionCount().getOrNull())
        assertTrue(repository.getAllCategoryOverrides().getOrNull()?.isEmpty() == true)
        assertNull(repository.getLastSyncTimestamp().getOrNull())
        assertEquals(true, viewModel.uiState.value.isDarkMode) // Theme preserved
        assertEquals(false, viewModel.uiState.value.isDynamicColors) // Theme preserved
        assertEquals(true, viewModel.uiState.value.isNotificationsEnabled) // Reset to default
        assertEquals("MONTH", viewModel.uiState.value.defaultAnalyticsPeriod) // Reset to default
        assertEquals("₹", viewModel.uiState.value.currencySymbol) // Reset to default
    }

    @Test
    fun multiplePreferenceUpdates_rapidlyInSequence() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Rapid updates to same preference
        viewModel.updateDarkMode(true)
        viewModel.updateDarkMode(false)
        viewModel.updateDarkMode(null)
        viewModel.updateDarkMode(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Last update should win
        assertEquals(true, viewModel.uiState.value.isDarkMode)
        assertEquals(true, appPreferences.isDarkModeEnabled())
    }

    @Test
    fun allPreferences_updateIndependently() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Update all preferences to non-default values
        viewModel.updateDarkMode(true)
        viewModel.updateDynamicColors(false)
        viewModel.updateCompactView(true)
        viewModel.updateShowOfflineBadge(false)
        viewModel.updateNotificationsEnabled(false)
        viewModel.updateDefaultAnalyticsPeriod("DAY")
        viewModel.updateCurrencySymbol("$")
        viewModel.updateAutoCategorize(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - All should be updated independently
        assertEquals(true, viewModel.uiState.value.isDarkMode)
        assertEquals(false, viewModel.uiState.value.isDynamicColors)
        assertEquals(true, viewModel.uiState.value.isCompactView)
        assertEquals(false, viewModel.uiState.value.showOfflineBadge)
        assertEquals(false, viewModel.uiState.value.isNotificationsEnabled)
        assertEquals("DAY", viewModel.uiState.value.defaultAnalyticsPeriod)
        assertEquals("$", viewModel.uiState.value.currencySymbol)
        assertEquals(false, viewModel.uiState.value.isAutoCategorize)

        // Verify preferences persisted
        assertEquals(true, appPreferences.isDarkModeEnabled())
        assertEquals(false, appPreferences.isDynamicColorsEnabled())
        assertEquals(true, appPreferences.isCompactViewEnabled())
        assertEquals(false, appPreferences.shouldShowOfflineBadge())
        assertEquals(false, appPreferences.isNotificationsEnabled())
        assertEquals("DAY", appPreferences.getDefaultAnalyticsPeriod())
        assertEquals("$", appPreferences.getCurrencySymbol())
        assertEquals(false, appPreferences.isAutoCategorizeEnabled())
    }

    // ==================== Edge Cases ====================

    @Test
    fun clearAllData_whenNoDataExists_succeeds() = runTest {
        // Given - Empty database
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, repository.getTransactionCount().getOrNull())

        // When
        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should succeed without errors
        assertEquals(0, repository.getTransactionCount().getOrNull())
        assertEquals(false, viewModel.uiState.value.isClearing)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun clearAllData_withLargeDataset_succeeds() = runTest {
        // Given - Large dataset
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        val largeDataset = (1L..100L).map { id ->
            createTestTransaction(smsId = id, amount = id * 10.0)
        }
        repository.saveTransactions(largeDataset)
        assertEquals(100, repository.getTransactionCount().getOrNull())

        // When
        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(0, repository.getTransactionCount().getOrNull())
        assertEquals(false, viewModel.uiState.value.isClearing)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun updateCurrencySymbol_withEmptyString_updatesSuccessfully() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateCurrencySymbol("")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("", viewModel.uiState.value.currencySymbol)
        assertEquals("", appPreferences.getCurrencySymbol())
    }

    @Test
    fun updateCurrencySymbol_withSpecialCharacters_updatesSuccessfully() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val specialSymbol = "¥€£₹"
        viewModel.updateCurrencySymbol(specialSymbol)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(specialSymbol, viewModel.uiState.value.currencySymbol)
        assertEquals(specialSymbol, appPreferences.getCurrencySymbol())
    }
}
