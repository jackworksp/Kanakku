# Git Workflow Rules

## Branch Naming
- Feature: `feature/feature-name`
- Bugfix: `bugfix/bug-name`
- Hotfix: `hotfix/fix-name`
- Release: `release/v1.0.0`

## Commit Message Format
```
[TYPE] Brief description (50 chars max)

Optional longer description (72 chars per line)
```

### Types
- `FEATURE` - New feature
- `BUG` - Bug fix
- `DOCS` - Documentation
- `STYLE` - Code style/formatting
- `REFACTOR` - Code refactoring
- `PERF` - Performance improvement
- `TEST` - Tests
- `CHORE` - Build/tooling

## Pre-Commit Checklist
1. Run `./gradlew ktlintFormat`
2. Run `./gradlew test`
3. Run `./gradlew lint`
4. Verify no secrets in staged files

## PR Guidelines
- Clear title describing the change
- Link to related issue if applicable
- Include testing steps
- Request review from team member
