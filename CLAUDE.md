# Kanakku - Android Kotlin Project Memory

> Project memory for the Kanakku Android application. This file documents architecture, coding standards, build processes, and workflows.

## Project Overview

**Kanakku** is an Android application built with Kotlin, following modern Android development practices.

### Technology Stack
- Language: Kotlin
- Build System: Gradle (Kotlin DSL)
- Minimum API Level: 24
- Target API Level: 35
- Architecture Pattern: MVVM with Clean Architecture
- Testing: JUnit, Mockk, Espresso

### Directory Structure
```
Kanakku/
├── app/
│   ├── src/
│   │   ├── main/kotlin/
│   │   ├── test/           # Unit tests
│   │   └── androidTest/    # Instrumented tests
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── CLAUDE.md
```

## Architecture Patterns

### Clean Architecture Layers

**Data Layer** - Repositories, data sources, DTOs
**Domain Layer** - Use cases, business logic, entities
**Presentation Layer** - ViewModels, Activities, Fragments

### Recommended Patterns
- Repository Pattern for data abstraction
- Dependency Injection with Hilt
- ViewModels with Coroutines and StateFlow
- Separation of Concerns

## Offline-First Architecture

**Kanakku is a completely offline-first application with zero network dependencies.**

### Core Principles
- **100% Local Operation**: All data stored and processed on-device using Room database
- **No Network Calls**: Zero network libraries, no INTERNET permission in manifest
- **Privacy-Focused**: User data never leaves the device
- **Works Anywhere**: Full functionality in airplane mode or without connectivity

### Data Flow
```
SMS Messages (ContentProvider)
    ↓
SmsReader (Local parsing)
    ↓
TransactionRepository (Room database)
    ↓
MainViewModel (StateFlow)
    ↓
UI Layer (Jetpack Compose)
```

### Offline-First Components

#### 1. Local Data Storage
- **Room Database**: All transactions stored locally in SQLite
- **EncryptedSharedPreferences**: Secure storage for app settings and preferences
- **In-Memory Cache**: Performance optimization layer for frequently accessed data

#### 2. Database Robustness
- **Initialization Error Handling**: Automatic recovery from database corruption
- **Integrity Checks**: PRAGMA quick_check on startup to detect corruption early
- **Backup/Restore System**: Automatic backups after significant changes with rate limiting
- **WAL Mode**: Write-Ahead Logging for better concurrency and crash resistance

#### 3. Data Persistence Strategy
```kotlin
// AppPreferences - Encrypted settings storage
val prefs = AppPreferences.getInstance(context)
prefs.setDarkModeEnabled(true)  // AES256_GCM encrypted

// DatabaseBackupManager - Automatic backup
backupManager.createAutomaticBackup(database)  // Rate-limited to 1/hour

// TransactionRepository - In-memory caching
val transactions = repository.getAllTransactionsSnapshot()  // Cache hit
```

### UI Indicators
- **OfflineBadge**: Visible indicator showing "Local Data" mode
- **Last Sync Timestamp**: Shows when SMS data was last parsed
- **Privacy Dialog**: First-time onboarding explaining offline-first benefits

## Error Handling Patterns

**Centralized, user-friendly error handling across the entire app.**

### ErrorHandler Utility

The `ErrorHandler` class provides consistent error handling with proper categorization:

```kotlin
// Basic error handling
val error = ErrorHandler.handleError(
    exception = exception,
    context = "Save transaction"
)
println(error.userMessage)  // User-friendly message
println(error.technicalMessage)  // Developer details

// Functional error handling with Result type
val result = ErrorHandler.runSuspendCatching("Get transactions") {
    repository.getAllTransactions()
}
result.onSuccess { transactions -> /* use data */ }
result.onFailure { error -> /* show error.userMessage */ }
```

### Error Categories

| Category | Examples | User Message |
|----------|----------|-------------|
| DATABASE | SQLiteException, DatabaseInitializationException | "Database error. Please restart the app." |
| FILE_IO | IOException, FileNotFoundException | "File access error. Check storage permissions." |
| PERMISSION | SecurityException | "Permission denied. Grant necessary permissions in Settings." |
| PARSING | NumberFormatException, IllegalArgumentException | "Data format error. Some data may not load correctly." |
| NETWORK | N/A (no network code) | N/A |
| UNKNOWN | Generic exceptions | "An unexpected error occurred." |

### Error Severity Levels

- **WARNING**: Non-critical issues, log only
- **ERROR**: User-visible errors with recovery options
- **CRITICAL**: Severe errors requiring immediate attention

### Repository Pattern with Result Type

All repository methods return `Result<T>` for explicit error handling:

```kotlin
// TransactionRepository - all operations return Result
suspend fun saveTransaction(transaction: Transaction): Result<Long> =
    ErrorHandler.runSuspendCatching("Save transaction") {
        transactionDao.insert(transaction.toEntity())
    }

// ViewModel usage
viewModelScope.launch {
    repository.saveTransaction(transaction)
        .onSuccess { id ->
            _uiState.value = _uiState.value.copy(
                errorMessage = null
            )
        }
        .onFailure { error ->
            val errorInfo = error.toErrorInfo()
            _uiState.value = _uiState.value.copy(
                errorMessage = errorInfo.userMessage
            )
        }
}
```

### Graceful Degradation Strategy

1. **Partial Results**: Return valid partial data when some operations fail
2. **Cache Fallback**: Use cached data when database operations fail
3. **Empty States**: Return empty collections instead of null
4. **User Feedback**: Show clear error messages with recovery guidance

```kotlin
// SmsReader - returns partial results on error
fun readAllTransactionSms(): List<SmsMessage> {
    val messages = mutableListOf<SmsMessage>()
    try {
        cursor?.use { c ->
            while (c.moveToNext()) {
                try {
                    messages.add(extractSmsFromCursor(c))
                } catch (e: Exception) {
                    // Log error but continue processing
                    ErrorHandler.handleError(e, "Extract SMS row")
                }
            }
        }
    } catch (e: SecurityException) {
        // Return partial results collected before error
        ErrorHandler.handleError(e, "Read SMS")
    }
    return messages  // Returns partial results, not empty
}
```

### Database Error Recovery

```kotlin
// DatabaseProvider - automatic corruption recovery
private fun handleDatabaseCorruption(context: Context) {
    try {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val walFile = File(dbFile.parent, "$DATABASE_NAME-wal")
        val shmFile = File(dbFile.parent, "$DATABASE_NAME-shm")

        // Delete corrupted files
        dbFile.delete()
        walFile.delete()
        shmFile.delete()

        // Retry initialization
        buildDatabase(context)
    } catch (e: Exception) {
        throw DatabaseInitializationException(
            "Failed to recover from database corruption",
            e
        )
    }
}
```

## Build Commands

```bash
# Clean build
./gradlew clean

# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Lint analysis
./gradlew lint

# Install debug to device
./gradlew installDebug
```

## Testing Approach

### Unit Tests (JUnit + Mockk)
- Location: `app/src/test/kotlin/`
- Mock external dependencies
- Test ViewModels and business logic

### Instrumented Tests (Espresso)
- Location: `app/src/androidTest/kotlin/`
- Test UI interactions
- Use FragmentScenario for fragments

### Coverage Target
- Critical paths: 80%+
- New code: 70%+

### Offline-First Testing Strategy

#### 1. Error Handling Tests
- **ErrorHandlerTest**: 50+ tests covering all exception types and error categories
- **Repository Tests**: Error scenario validation with Result type
- **Edge Cases**: Boundary values, empty states, concurrent operations

```kotlin
// Example: Testing error handling in repository
@Test
fun `saveTransaction returns error on database failure`() = runTest {
    database.close()  // Simulate database error

    val result = repository.saveTransaction(testTransaction)

    assertTrue(result.isFailure)
    val error = result.exceptionOrNull()?.toErrorInfo()
    assertEquals(ErrorCategory.DATABASE, error?.category)
}
```

#### 2. Database Robustness Tests
- **Corruption Recovery**: Verify automatic recovery from corrupted database
- **Integrity Checks**: Test PRAGMA quick_check detection
- **Backup/Restore**: Validate backup creation and restoration
- **Concurrent Access**: Stress tests with multiple rapid operations

```kotlin
// Example: Testing partial results on error
@Test
fun `getAllTransactions returns partial results on error`() = runTest {
    repository.saveTransactions(listOf(tx1, tx2, tx3))
    database.close()  // Fail during read

    val result = repository.getAllTransactions()

    // Should return cached data, not fail
    assertTrue(result.isSuccess)
}
```

#### 3. Offline Testing Checklist
Manual verification in airplane mode (see `.auto-claude/specs/019-offline-first-architecture/OFFLINE_TESTING_CHECKLIST.md`):

- [ ] App launches without network
- [ ] SMS reading works offline
- [ ] All screens accessible
- [ ] Data persists after app restart
- [ ] No network error messages
- [ ] UI indicators show offline status
- [ ] Database operations work correctly
- [ ] Category management functions properly

#### 4. Testing Best Practices
- **Always test error paths**: Every operation should have error scenario tests
- **Use Result type**: Validate both success and failure cases
- **Test partial results**: Ensure graceful degradation works
- **Verify user messages**: Check ErrorHandler produces user-friendly messages
- **Simulate failures**: Close database, revoke permissions, corrupt data
- **Cache validation**: Test cache hits, misses, and invalidation

```kotlin
// Example: Comprehensive test with error handling
@Test
fun `loadTransactions handles all scenarios`() = runTest {
    // Success case
    val result1 = repository.getAllTransactions()
    assertTrue(result1.isSuccess)

    // Error case
    database.close()
    val result2 = repository.getAllTransactions()
    assertTrue(result2.isFailure)

    // Verify error message
    val errorInfo = result2.exceptionOrNull()?.toErrorInfo()
    assertNotNull(errorInfo?.userMessage)
    assertEquals(ErrorSeverity.ERROR, errorInfo?.severity)
}
```

## Code Style Guidelines

### Kotlin Standards
- Use `val` by default, `var` when necessary
- Prefer expression bodies for simple functions
- Use sealed classes for state management
- Data classes for DTOs and entities

### Naming Conventions
- Classes: PascalCase (`UserViewModel`)
- Functions/Variables: camelCase (`getUserData`)
- Constants: UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)

### Best Practices
- Avoid nullable types when possible
- Use scope functions appropriately
- Keep functions small and focused
- Document complex logic with comments

## Security Practices

- Never hardcode API keys or secrets
- Use BuildConfig for environment values
- Encrypt sensitive data with EncryptedSharedPreferences
- Validate all user inputs
- **Offline-First Security**: No INTERNET permission = no network attack surface
- **Local Data Encryption**: Use AppPreferences for encrypted settings (AES256_GCM)
- **Privacy by Design**: All user data stays on device, never transmitted

## Common Workflows

### Feature Development
1. Create feature branch: `git checkout -b feature/name`
2. Implement following architecture patterns
3. Write tests
4. Run `./gradlew test lint`
5. Create PR

### Bug Fixing
1. Create branch: `git checkout -b bugfix/name`
2. Write failing test
3. Fix bug
4. Verify tests pass
5. Create PR

### Release
1. Update version in build.gradle.kts
2. Run full test suite
3. Build release: `./gradlew bundleRelease`
4. Tag release: `git tag v1.0.0`

## Git Workflow

- Main branches: `main`, `develop`
- Feature: `feature/feature-name`
- Bugfix: `bugfix/bug-name`
- Commit format: `[TYPE] Brief description`
  - Types: FEATURE, BUG, DOCS, REFACTOR, TEST

## Useful ADB Commands

```bash
adb devices              # List devices
adb logcat               # View logs
adb install app.apk      # Install APK
adb shell pm clear pkg   # Clear app data
```

## Tips for Claude Code

- Use `/build` to build the app
- Use `/test` to run tests
- Use `/lint` for code quality checks
- Use `/deploy` to install on device
- Use `/review` for code reviews
