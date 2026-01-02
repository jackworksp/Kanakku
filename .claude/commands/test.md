---
allowed-tools: Bash(./gradlew:*), Bash(gradlew.bat:*), Bash(cmd /c:*), Bash(dir:*), Read
description: Run unit tests with optional coverage report
argument-hint: unit | instrumented | coverage
---

# Run Android Tests

Execute tests and optionally generate coverage report.

## Test Type
- Test scope: $1 (default: unit)

## Commands

For **unit** tests:
```bash
./gradlew testDebugUnitTest --parallel
```

For **instrumented** tests:
```bash
./gradlew connectedAndroidTest
```

For **coverage** report:
```bash
./gradlew testDebugUnitTestCoverage
```

## After Tests
- Report pass/fail count
- Show failed test details
- Provide coverage percentage if available
- Suggest fixes for failures
