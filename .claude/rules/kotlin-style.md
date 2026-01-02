# Kotlin Style Rules

## General
- Use `val` by default, `var` only when necessary
- Prefer expression bodies for simple functions
- Maximum line length: 120 characters
- Use 4 spaces for indentation

## Naming
- Classes/Objects: PascalCase (`UserViewModel`)
- Functions/Properties: camelCase (`getUserData`)
- Constants: UPPER_SNAKE_CASE (`MAX_RETRY`)
- Backing properties: prefix with underscore (`_state`)

## Null Safety
- Avoid nullable types when possible
- Use `?.` safe call operator
- Use `?:` elvis operator for defaults
- Avoid `!!` except in tests

## Scope Functions
- `let` - null checks, transform values
- `apply` - object configuration
- `run` - computation with receiver
- `also` - additional actions
- `with` - multiple operations on object

## Collections
- Prefer immutable collections
- Use `listOf`, `mapOf`, `setOf`
- Use sequence for large collections with multiple operations

## Coroutines
- Use structured concurrency
- Cancel coroutines in onCleared/onDestroy
- Use appropriate dispatchers (IO, Main, Default)
- Handle exceptions with try-catch or CoroutineExceptionHandler
