---
name: test-generation
description: Generates unit tests and Espresso integration tests for Kotlin/Android code. Use when you need to write tests, improve test coverage, or create UI tests. Supports JUnit, Mockk, and Espresso frameworks.
allowed-tools: Read, Grep, Glob, Bash(./gradlew:*), Bash(gradlew.bat:*)
---

# Test Generation for Kotlin/Android

## Instructions

Generate tests following Android testing best practices.

### 1. Unit Tests (JUnit + Mockk)

```kotlin
@RunWith(MockKJUnitRunner::class)
class MyViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var repository: MyRepository

    private lateinit var viewModel: MyViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel = MyViewModel(repository)
    }

    @Test
    fun `should load data successfully`() {
        // Arrange
        coEvery { repository.getData() } returns flowOf(testData)

        // Act
        viewModel.loadData()

        // Assert
        assertEquals(expected, viewModel.state.value)
    }
}
```

### 2. Espresso UI Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class MyActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testButtonClick() {
        onView(withId(R.id.myButton))
            .perform(click())

        onView(withText("Expected"))
            .check(matches(isDisplayed()))
    }
}
```

### 3. Test Structure
- Use AAA pattern: Arrange, Act, Assert
- One assertion per test
- Descriptive test names with backticks
- Mock external dependencies
- Test both success and error cases

### 4. Running Tests

```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # UI tests
./gradlew testDebugUnitTestCoverage  # With coverage
```

## Coverage Targets
- ViewModels: 90%+
- Repositories: 80%+
- Utilities: 70%+
