package com.example.kanakku.core.error

import android.database.SQLException
import android.database.sqlite.SQLiteException
import android.util.Log
import com.example.kanakku.data.database.DatabaseBackupException
import com.example.kanakku.data.database.DatabaseInitializationException
import java.io.IOException

/**
 * Centralized error handling utility for consistent error processing across the application.
 *
 * This class provides:
 * - Mapping of technical exceptions to user-friendly messages
 * - Consistent error logging with appropriate log levels
 * - Error categorization for different handling strategies
 * - Structured error information for UI display
 *
 * Usage:
 * ```
 * try {
 *     // Some operation
 * } catch (e: Exception) {
 *     val error = ErrorHandler.handleError(e, "Loading transactions")
 *     _uiState.value = _uiState.value.copy(errorMessage = error.userMessage)
 * }
 * ```
 *
 * Thread-safe: All methods are stateless and safe to call from any thread.
 */
object ErrorHandler {

    private const val TAG = "ErrorHandler"

    /**
     * Error severity levels for determining appropriate user feedback and logging.
     */
    enum class ErrorSeverity {
        /** Minor issues that don't prevent app functionality */
        WARNING,

        /** Errors that prevent a specific operation but app can continue */
        ERROR,

        /** Critical errors that prevent core app functionality */
        CRITICAL
    }

    /**
     * Error category for grouping similar errors and applying consistent handling.
     */
    enum class ErrorCategory {
        /** Database-related errors (corruption, initialization, queries) */
        DATABASE,

        /** File I/O errors (read, write, backup, restore) */
        FILE_IO,

        /** Permission-related errors (SMS read, storage access) */
        PERMISSION,

        /** Data parsing errors (SMS parsing, invalid data) */
        PARSING,

        /** Network errors (should not occur in offline-first app) */
        NETWORK,

        /** Unknown or unexpected errors */
        UNKNOWN
    }

    /**
     * Structured error information for consistent error handling and display.
     *
     * @property userMessage User-friendly error message for UI display
     * @property technicalMessage Technical error details for logging
     * @property category Error category for handling strategy
     * @property severity Error severity level
     * @property throwable Original exception (if available)
     * @property isRecoverable Whether the error can be recovered from
     */
    data class ErrorInfo(
        val userMessage: String,
        val technicalMessage: String,
        val category: ErrorCategory,
        val severity: ErrorSeverity,
        val throwable: Throwable? = null,
        val isRecoverable: Boolean = true
    )

    /**
     * Handles an exception and returns structured error information.
     *
     * This is the main entry point for error handling. It:
     * 1. Categorizes the error
     * 2. Determines severity
     * 3. Generates user-friendly message
     * 4. Logs the error appropriately
     *
     * @param throwable The exception to handle
     * @param context Additional context about where the error occurred (e.g., "Loading transactions")
     * @return ErrorInfo containing structured error details
     */
    fun handleError(throwable: Throwable, context: String = ""): ErrorInfo {
        val errorInfo = categorizeError(throwable, context)
        logError(errorInfo, context)
        return errorInfo
    }

    /**
     * Handles an error with a custom user message.
     * Useful when you want to provide a specific message to the user.
     *
     * @param throwable The exception to handle
     * @param userMessage Custom user-friendly message
     * @param context Additional context about where the error occurred
     * @return ErrorInfo with the custom user message
     */
    fun handleErrorWithMessage(
        throwable: Throwable,
        userMessage: String,
        context: String = ""
    ): ErrorInfo {
        val errorInfo = categorizeError(throwable, context).copy(userMessage = userMessage)
        logError(errorInfo, context)
        return errorInfo
    }

    /**
     * Categorizes an exception and generates appropriate error information.
     *
     * @param throwable The exception to categorize
     * @param context Additional context
     * @return ErrorInfo with appropriate category, severity, and messages
     */
    private fun categorizeError(throwable: Throwable, context: String): ErrorInfo {
        return when (throwable) {
            // Database Errors
            is DatabaseInitializationException -> ErrorInfo(
                userMessage = "Unable to initialize database. Please restart the app or reinstall if the problem persists.",
                technicalMessage = "DatabaseInitializationException: ${throwable.message}",
                category = ErrorCategory.DATABASE,
                severity = ErrorSeverity.CRITICAL,
                throwable = throwable,
                isRecoverable = false
            )

            is DatabaseBackupException -> ErrorInfo(
                userMessage = "Failed to backup data. Please check available storage space and try again.",
                technicalMessage = "DatabaseBackupException: ${throwable.message}",
                category = ErrorCategory.DATABASE,
                severity = ErrorSeverity.ERROR,
                throwable = throwable,
                isRecoverable = true
            )

            is SQLiteException -> ErrorInfo(
                userMessage = "Database error occurred. Your data is safe but this operation could not be completed.",
                technicalMessage = "SQLiteException: ${throwable.message}",
                category = ErrorCategory.DATABASE,
                severity = ErrorSeverity.ERROR,
                throwable = throwable,
                isRecoverable = true
            )

            is SQLException -> ErrorInfo(
                userMessage = "A database error occurred. Please try again.",
                technicalMessage = "SQLException: ${throwable.message}",
                category = ErrorCategory.DATABASE,
                severity = ErrorSeverity.ERROR,
                throwable = throwable,
                isRecoverable = true
            )

            // File I/O Errors
            is IOException -> ErrorInfo(
                userMessage = "File operation failed. Please check available storage space.",
                technicalMessage = "IOException: ${throwable.message}",
                category = ErrorCategory.FILE_IO,
                severity = ErrorSeverity.ERROR,
                throwable = throwable,
                isRecoverable = true
            )

            // Permission Errors
            is SecurityException -> ErrorInfo(
                userMessage = "Permission denied. Please grant the required permissions in Settings.",
                technicalMessage = "SecurityException: ${throwable.message}",
                category = ErrorCategory.PERMISSION,
                severity = ErrorSeverity.ERROR,
                throwable = throwable,
                isRecoverable = true
            )

            // Illegal State/Argument
            is IllegalStateException -> ErrorInfo(
                userMessage = "The app is in an unexpected state. Please restart the app.",
                technicalMessage = "IllegalStateException: ${throwable.message}",
                category = ErrorCategory.UNKNOWN,
                severity = ErrorSeverity.ERROR,
                throwable = throwable,
                isRecoverable = true
            )

            is IllegalArgumentException -> ErrorInfo(
                userMessage = "Invalid data detected. Please check your input and try again.",
                technicalMessage = "IllegalArgumentException: ${throwable.message}",
                category = ErrorCategory.PARSING,
                severity = ErrorSeverity.WARNING,
                throwable = throwable,
                isRecoverable = true
            )

            // Number Format (parsing errors)
            is NumberFormatException -> ErrorInfo(
                userMessage = "Invalid number format in data. Some information may not be displayed correctly.",
                technicalMessage = "NumberFormatException: ${throwable.message}",
                category = ErrorCategory.PARSING,
                severity = ErrorSeverity.WARNING,
                throwable = throwable,
                isRecoverable = true
            )

            // Null Pointer
            is NullPointerException -> ErrorInfo(
                userMessage = "An unexpected error occurred. Please restart the app.",
                technicalMessage = "NullPointerException: ${throwable.message}",
                category = ErrorCategory.UNKNOWN,
                severity = ErrorSeverity.ERROR,
                throwable = throwable,
                isRecoverable = true
            )

            // Generic/Unknown Errors
            else -> ErrorInfo(
                userMessage = "An unexpected error occurred. Please try again.",
                technicalMessage = "${throwable.javaClass.simpleName}: ${throwable.message}",
                category = ErrorCategory.UNKNOWN,
                severity = ErrorSeverity.ERROR,
                throwable = throwable,
                isRecoverable = true
            )
        }
    }

    /**
     * Logs error information at the appropriate log level.
     *
     * @param errorInfo The error information to log
     * @param context Additional context about where the error occurred
     */
    private fun logError(errorInfo: ErrorInfo, context: String) {
        val contextPrefix = if (context.isNotEmpty()) "[$context] " else ""
        val message = "$contextPrefix${errorInfo.technicalMessage}"

        when (errorInfo.severity) {
            ErrorSeverity.WARNING -> {
                Log.w(TAG, message, errorInfo.throwable)
            }
            ErrorSeverity.ERROR -> {
                Log.e(TAG, message, errorInfo.throwable)
            }
            ErrorSeverity.CRITICAL -> {
                Log.e(TAG, "CRITICAL: $message", errorInfo.throwable)
            }
        }
    }

    /**
     * Creates a generic error for display when no exception is available.
     * Useful for custom error scenarios.
     *
     * @param message The error message for both user and technical logs
     * @param category Error category
     * @param severity Error severity
     * @return ErrorInfo with the provided details
     */
    fun createError(
        message: String,
        category: ErrorCategory = ErrorCategory.UNKNOWN,
        severity: ErrorSeverity = ErrorSeverity.ERROR
    ): ErrorInfo {
        val errorInfo = ErrorInfo(
            userMessage = message,
            technicalMessage = message,
            category = category,
            severity = severity,
            throwable = null,
            isRecoverable = true
        )
        logError(errorInfo, "Custom Error")
        return errorInfo
    }

    /**
     * Logs an info message for successful operations or important state changes.
     * Not an error, but useful for tracking app behavior.
     *
     * @param message The message to log
     * @param context Additional context
     */
    fun logInfo(message: String, context: String = "") {
        val contextPrefix = if (context.isNotEmpty()) "[$context] " else ""
        Log.i(TAG, "$contextPrefix$message")
    }

    /**
     * Logs a debug message for development and troubleshooting.
     *
     * @param message The message to log
     * @param context Additional context
     */
    fun logDebug(message: String, context: String = "") {
        val contextPrefix = if (context.isNotEmpty()) "[$context] " else ""
        Log.d(TAG, "$contextPrefix$message")
    }

    /**
     * Logs a warning message for non-critical issues.
     *
     * @param message The message to log
     * @param context Additional context
     */
    fun logWarning(message: String, context: String = "") {
        val contextPrefix = if (context.isNotEmpty()) "[$context] " else ""
        Log.w(TAG, "$contextPrefix$message")
    }

    /**
     * Wraps a potentially failing operation with error handling.
     * Returns Result type for functional error handling.
     *
     * Usage:
     * ```
     * val result = ErrorHandler.runCatching("Save transaction") {
     *     repository.saveTransaction(transaction)
     * }
     * result.onSuccess { ... }.onFailure { error -> ... }
     * ```
     *
     * @param context Description of the operation being performed
     * @param block The operation to execute
     * @return Result<T> containing either the success value or ErrorInfo
     */
    inline fun <T> runCatching(
        context: String = "",
        block: () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            val errorInfo = handleError(e, context)
            Result.failure(ErrorHandlerException(errorInfo))
        }
    }

    /**
     * Wraps a suspend function with error handling.
     * Useful for coroutine-based operations in repositories and ViewModels.
     *
     * Usage:
     * ```
     * val result = ErrorHandler.runSuspendCatching("Load data") {
     *     repository.getAllTransactions()
     * }
     * ```
     *
     * @param context Description of the operation being performed
     * @param block The suspend operation to execute
     * @return Result<T> containing either the success value or ErrorInfo
     */
    suspend inline fun <T> runSuspendCatching(
        context: String = "",
        crossinline block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            val errorInfo = handleError(e, context)
            Result.failure(ErrorHandlerException(errorInfo))
        }
    }
}

/**
 * Exception that wraps ErrorInfo for use with Result type.
 * This allows ErrorInfo to be propagated through Result.failure().
 *
 * @property errorInfo The structured error information
 */
class ErrorHandlerException(
    val errorInfo: ErrorHandler.ErrorInfo
) : Exception(errorInfo.technicalMessage, errorInfo.throwable)

/**
 * Extension function to extract ErrorInfo from Result.
 *
 * Usage:
 * ```
 * result.onFailure { throwable ->
 *     val errorInfo = throwable.toErrorInfo()
 *     showError(errorInfo.userMessage)
 * }
 * ```
 */
fun Throwable.toErrorInfo(): ErrorHandler.ErrorInfo {
    return when (this) {
        is ErrorHandlerException -> this.errorInfo
        else -> ErrorHandler.handleError(this)
    }
}
