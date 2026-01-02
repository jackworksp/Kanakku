---
name: kotlin-review
description: Reviews Kotlin and Android code for best practices, performance, and maintainability. Use when reviewing Kotlin code, Android fragments, activities, or checking code quality. Evaluates architecture patterns, memory leaks, lifecycle handling, and Kotlin idioms.
allowed-tools: Read, Grep, Glob
---

# Kotlin & Android Code Review

## Instructions

When reviewing Kotlin or Android code, evaluate:

### 1. Kotlin Idioms & Practices
- Use of data classes, sealed classes, and extension functions
- Null safety with nullable types and smart casts
- Scope functions (let, apply, run, with, also) usage
- Proper use of val vs var
- Lambda and higher-order function patterns

### 2. Android-Specific Patterns
- Activity and Fragment lifecycle handling
- Proper context usage (Activity vs Application context)
- View binding vs findViewById
- Memory leak prevention
- Threading and coroutine usage
- Resource cleanup in onDestroy/onPause

### 3. Performance & Architecture
- RecyclerView optimization (ViewHolder pattern, DiffUtil)
- Network request batching and caching
- Database query efficiency
- Memory profiling considerations
- ANR prevention

### 4. Testing & Maintainability
- Testability of code structure
- Dependency injection opportunities
- Logging and error handling
- Code duplication
- Documentation for complex logic

## Review Format

Provide feedback organized as:

1. **Strengths** - What's done well
2. **Issues** - Critical problems (crashes, memory leaks)
3. **Improvements** - Best practice suggestions
4. **Suggestions** - Performance enhancements

Severity levels:
- Critical: Can crash or cause data loss
- High: Memory leak or performance issue
- Medium: Best practice violation
- Low: Code style improvement
