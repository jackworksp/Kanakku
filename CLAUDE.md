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
- Always use HTTPS for network calls
- Validate all user inputs

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
