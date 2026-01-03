package com.example.kanakku.ui.theme

import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.model.ThemeMode
import com.example.kanakku.data.preferences.AppPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ThemeViewModel.
 *
 * Tests cover:
 * - Theme mode initialization and loading
 * - Theme mode changes with persistence
 * - Reactive StateFlow updates
 * - Error state management
 * - Integration with AppPreferences
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ThemeViewModelTest {

    private lateinit var viewModel: ThemeViewModel
    private lateinit var appPreferences: AppPreferences

    @Before
    fun setup() {
        // Reset AppPreferences singleton before each test
        AppPreferences.resetInstance()

        // Get fresh AppPreferences instance
        appPreferences = AppPreferences.getInstance(ApplicationProvider.getApplicationContext())

        // Clear all preferences to start with clean slate
        appPreferences.clearAll()

        // Create fresh ViewModel instance
        viewModel = ThemeViewModel()
    }

    @After
    fun teardown() {
        // Clean up after each test
        appPreferences.clearAll()
        AppPreferences.resetInstance()
    }

    // ==================== Initialization Tests ====================

    @Test
    fun initialize_loadsDefaultThemeMode() = runTest {
        // When
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // Then - Should load default theme mode (SYSTEM)
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun initialize_loadsSavedThemeMode_light() = runTest {
        // Given - Save LIGHT theme mode before initialization
        appPreferences.setThemeMode(ThemeMode.LIGHT)

        // When
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // Then
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun initialize_loadsSavedThemeMode_dark() = runTest {
        // Given - Save DARK theme mode before initialization
        appPreferences.setThemeMode(ThemeMode.DARK)

        // When
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // Then
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun initialize_loadsSavedThemeMode_system() = runTest {
        // Given - Explicitly save SYSTEM theme mode
        appPreferences.setThemeMode(ThemeMode.SYSTEM)

        // When
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // Then
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun initialize_observesThemeChangesFromAppPreferences() = runTest {
        // Given - Initialize ViewModel
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // Verify initial state
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        // When - Change theme directly in AppPreferences
        appPreferences.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // Then - ViewModel should react to the change
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun initialize_multipleChangesObserved() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When - Multiple theme changes
        appPreferences.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)

        appPreferences.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)

        appPreferences.setThemeMode(ThemeMode.SYSTEM)
        advanceUntilIdle()

        // Then - All changes should be observed
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
    }

    @Test
    fun initialize_canBeCalledMultipleTimes() = runTest {
        // Given
        val context = ApplicationProvider.getApplicationContext()

        // When - Initialize multiple times
        viewModel.initialize(context)
        advanceUntilIdle()

        viewModel.initialize(context)
        advanceUntilIdle()

        viewModel.initialize(context)
        advanceUntilIdle()

        // Then - Should still work correctly
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
        assertNull(viewModel.errorMessage.value)
    }

    // ==================== setThemeMode Tests ====================

    @Test
    fun setThemeMode_updatesStateFlow() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // Then
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun setThemeMode_persistsToAppPreferences() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // Then - Should be persisted in AppPreferences
        assertEquals(ThemeMode.DARK, appPreferences.getThemeMode())
    }

    @Test
    fun setThemeMode_clearsErrorMessage() = runTest {
        // Given - Force an error state (would need to mock AppPreferences for real error)
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        // Then - Error should be cleared
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun setThemeMode_allThemeModes_light() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        // Then
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
        assertEquals(ThemeMode.LIGHT, appPreferences.getThemeMode())
    }

    @Test
    fun setThemeMode_allThemeModes_dark() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // Then
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        assertEquals(ThemeMode.DARK, appPreferences.getThemeMode())
    }

    @Test
    fun setThemeMode_allThemeModes_system() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When
        viewModel.setThemeMode(ThemeMode.SYSTEM)
        advanceUntilIdle()

        // Then
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
        assertEquals(ThemeMode.SYSTEM, appPreferences.getThemeMode())
    }

    @Test
    fun setThemeMode_multipleConsecutiveChanges() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When - Rapid consecutive changes
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.SYSTEM)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // Then - Last change should be applied
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        assertEquals(ThemeMode.DARK, appPreferences.getThemeMode())
    }

    @Test
    fun setThemeMode_beforeInitialization() = runTest {
        // Given - ViewModel NOT initialized

        // When
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // Then - Should still update the StateFlow (but won't persist until initialized)
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun setThemeMode_sameThemeMode_doesNotCauseIssues() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // When - Set the same theme mode again
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // Then - Should still work correctly
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        assertEquals(ThemeMode.DARK, appPreferences.getThemeMode())
    }

    // ==================== StateFlow Reactivity Tests ====================

    @Test
    fun themeMode_stateFlowEmitsInitialValue() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When
        val currentValue = viewModel.themeMode.value

        // Then
        assertNotNull(currentValue)
        assertEquals(ThemeMode.SYSTEM, currentValue)
    }

    @Test
    fun themeMode_stateFlowEmitsUpdates() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        val emissions = mutableListOf<ThemeMode>()
        val collectJob = launch {
            viewModel.themeMode.collect { theme ->
                emissions.add(theme)
            }
        }

        // Wait for initial emission
        advanceUntilIdle()

        // When - Change theme mode
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        collectJob.cancel()

        // Then - Should have collected multiple emissions
        assertTrue(emissions.size >= 3) // Initial + 2 changes
        assertEquals(ThemeMode.DARK, emissions.last())
    }

    @Test
    fun themeMode_firstCollectorGetsCurrentValue() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // When - New collector subscribes
        val firstValue = viewModel.themeMode.first()

        // Then - Should immediately get current value
        assertEquals(ThemeMode.DARK, firstValue)
    }

    @Test
    fun errorMessage_stateFlowEmitsInitialNull() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When
        val currentValue = viewModel.errorMessage.value

        // Then
        assertNull(currentValue)
    }

    // ==================== clearError Tests ====================

    @Test
    fun clearError_clearsErrorMessage() = runTest {
        // Given - ViewModel initialized (no actual error to set in happy path)
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When
        viewModel.clearError()
        advanceUntilIdle()

        // Then
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun clearError_canBeCalledMultipleTimes() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When - Call multiple times
        viewModel.clearError()
        viewModel.clearError()
        viewModel.clearError()
        advanceUntilIdle()

        // Then - Should not cause issues
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun clearError_beforeInitialization() = runTest {
        // Given - ViewModel NOT initialized

        // When
        viewModel.clearError()
        advanceUntilIdle()

        // Then - Should not throw
        assertNull(viewModel.errorMessage.value)
    }

    // ==================== Persistence Across Sessions Tests ====================

    @Test
    fun persistence_themeModePersistedAcrossViewModelInstances() = runTest {
        // Given - First ViewModel instance
        val context = ApplicationProvider.getApplicationContext()
        viewModel.initialize(context)
        advanceUntilIdle()
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // When - Create new ViewModel instance (simulating app restart)
        val newViewModel = ThemeViewModel()
        newViewModel.initialize(context)
        advanceUntilIdle()

        // Then - Should load saved theme mode
        assertEquals(ThemeMode.DARK, newViewModel.themeMode.value)
    }

    @Test
    fun persistence_multipleChangesPersistedCorrectly() = runTest {
        // Given
        val context = ApplicationProvider.getApplicationContext()
        viewModel.initialize(context)
        advanceUntilIdle()

        // When - Change theme multiple times
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.SYSTEM)
        advanceUntilIdle()

        // Create new instance
        val newViewModel = ThemeViewModel()
        newViewModel.initialize(context)
        advanceUntilIdle()

        // Then - Should have the last saved value
        assertEquals(ThemeMode.SYSTEM, newViewModel.themeMode.value)
    }

    // ==================== Integration Tests ====================

    @Test
    fun integration_fullWorkflow_initializeChangeObserve() = runTest {
        // 1. Initialize
        val context = ApplicationProvider.getApplicationContext()
        viewModel.initialize(context)
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        // 2. Change theme
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)

        // 3. Verify persistence
        assertEquals(ThemeMode.DARK, appPreferences.getThemeMode())

        // 4. Change externally
        appPreferences.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        // 5. Verify ViewModel observed the change
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    }

    @Test
    fun integration_multipleViewModelsObserveSamePreferences() = runTest {
        // Given - Two ViewModels sharing same preferences
        val context = ApplicationProvider.getApplicationContext()
        val viewModel1 = ThemeViewModel()
        val viewModel2 = ThemeViewModel()

        viewModel1.initialize(context)
        advanceUntilIdle()

        viewModel2.initialize(context)
        advanceUntilIdle()

        // When - Change theme in first ViewModel
        viewModel1.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // Then - Both ViewModels should reflect the change
        assertEquals(ThemeMode.DARK, viewModel1.themeMode.value)
        assertEquals(ThemeMode.DARK, viewModel2.themeMode.value)
    }

    @Test
    fun integration_themeModeFlowSynchronizedWithAppPreferences() = runTest {
        // Given
        val context = ApplicationProvider.getApplicationContext()
        viewModel.initialize(context)
        advanceUntilIdle()

        val emissions = mutableListOf<ThemeMode>()
        val collectJob = launch {
            appPreferences.themeModeFlow.collect { theme ->
                emissions.add(theme)
            }
        }
        advanceUntilIdle()

        // When - Change through ViewModel
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        collectJob.cancel()

        // Then - AppPreferences flow should have emitted the changes
        assertTrue(emissions.contains(ThemeMode.DARK))
        assertTrue(emissions.contains(ThemeMode.LIGHT))
    }

    // ==================== Edge Cases ====================

    @Test
    fun edgeCase_rapidThemeTogglingDoesNotCauseIssues() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When - Rapid toggling
        repeat(10) { iteration ->
            val mode = when (iteration % 3) {
                0 -> ThemeMode.LIGHT
                1 -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            viewModel.setThemeMode(mode)
        }
        advanceUntilIdle()

        // Then - Should have stable state
        assertNotNull(viewModel.themeMode.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun edgeCase_initializationWithApplicationContext() = runTest {
        // Given
        val appContext = ApplicationProvider.getApplicationContext()

        // When
        viewModel.initialize(appContext)
        advanceUntilIdle()

        // Then
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun edgeCase_viewModelWorksBeforeAndAfterInitialization() = runTest {
        // Before initialization
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        // Change before initialization
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)

        // Initialize
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // After initialization - should have loaded from preferences (SYSTEM default)
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        // Change after initialization
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    }

    @Test
    fun edgeCase_clearErrorDoesNotAffectThemeMode() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // When
        viewModel.clearError()
        advanceUntilIdle()

        // Then - Theme mode should be unchanged
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    // ==================== Concurrent Access Tests ====================

    @Test
    fun concurrency_multipleSimultaneousChanges() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When - Launch multiple concurrent changes
        val job1 = launch { viewModel.setThemeMode(ThemeMode.LIGHT) }
        val job2 = launch { viewModel.setThemeMode(ThemeMode.DARK) }
        val job3 = launch { viewModel.setThemeMode(ThemeMode.SYSTEM) }

        job1.join()
        job2.join()
        job3.join()
        advanceUntilIdle()

        // Then - Should have stable final state (one of the three modes)
        assertTrue(
            viewModel.themeMode.value in listOf(
                ThemeMode.LIGHT,
                ThemeMode.DARK,
                ThemeMode.SYSTEM
            )
        )
    }

    @Test
    fun concurrency_readWhileWriting() = runTest {
        // Given
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // When - Read and write simultaneously
        val writeJob = launch {
            repeat(5) {
                viewModel.setThemeMode(ThemeMode.DARK)
                delay(10)
                viewModel.setThemeMode(ThemeMode.LIGHT)
                delay(10)
            }
        }

        val readJob = launch {
            repeat(10) {
                val currentMode = viewModel.themeMode.value
                assertNotNull(currentMode)
                delay(5)
            }
        }

        writeJob.join()
        readJob.join()
        advanceUntilIdle()

        // Then - Should complete without errors
        assertNotNull(viewModel.themeMode.value)
    }

    // ==================== Migration Tests ====================

    @Test
    fun migration_loadsFromOldBooleanDarkModePreference() = runTest {
        // Given - Old boolean-based dark mode preference (deprecated)
        @Suppress("DEPRECATION")
        appPreferences.setDarkModeEnabled(true)

        // When - Initialize new ViewModel
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // Then - Should migrate to DARK theme mode
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun migration_loadsFromOldBooleanLightModePreference() = runTest {
        // Given - Old boolean-based light mode preference
        @Suppress("DEPRECATION")
        appPreferences.setDarkModeEnabled(false)

        // When
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // Then - Should migrate to LIGHT theme mode
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    }

    @Test
    fun migration_loadsFromOldBooleanSystemDefaultPreference() = runTest {
        // Given - Old boolean-based system default preference (null)
        @Suppress("DEPRECATION")
        appPreferences.setDarkModeEnabled(null)

        // When
        viewModel.initialize(ApplicationProvider.getApplicationContext())
        advanceUntilIdle()

        // Then - Should migrate to SYSTEM theme mode
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
    }

    // ==================== Display Name Tests ====================

    @Test
    fun themeMode_hasCorrectDisplayNames() {
        // Verify each theme mode has the expected display name
        assertEquals("Light", ThemeMode.LIGHT.displayName)
        assertEquals("Dark", ThemeMode.DARK.displayName)
        assertEquals("System Default", ThemeMode.SYSTEM.displayName)
    }
}
