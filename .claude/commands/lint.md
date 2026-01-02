---
allowed-tools: Bash(./gradlew:*), Bash(gradlew.bat:*), Bash(cmd /c:*), Read
description: Run ktlint and Android lint checks
argument-hint: check | format | all
---

# Lint and Code Analysis

Analyze code quality using ktlint and Android lint.

## Action
- Action: $1 (default: check)

## Commands

For **check** (default):
```bash
./gradlew lint --parallel
```

For **format** (auto-fix):
```bash
./gradlew ktlintFormat
```

For **all** (comprehensive):
```bash
./gradlew ktlintCheck lint --parallel
```

## Reports
- Android Lint: `app/build/reports/lint-results-debug.html`
- Ktlint: Console output

## After Lint
- Summarize issues found
- Categorize by severity
- Suggest fixes for critical issues
