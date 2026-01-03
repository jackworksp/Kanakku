package com.example.kanakku.data.events

import com.example.kanakku.core.error.ErrorHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event manager for real-time transaction updates.
 *
 * This singleton class provides a reactive event system using Kotlin Flow to notify
 * the UI when new transactions are detected and saved in the background.
 *
 * Architecture:
 * - Uses SharedFlow for hot stream that can have multiple subscribers
 * - Events are emitted from background services (SmsProcessingService)
 * - Events are collected in ViewModels (MainViewModel) to trigger UI updates
 * - Offline-first: All events are local, no network required
 * - Thread-safe: SharedFlow handles concurrent access automatically
 *
 * Event Flow:
 * 1. Background SMS arrives -> SmsBroadcastReceiver intercepts
 * 2. SmsProcessingService parses and saves transaction
 * 3. Service calls emitNewTransactionEvent() with transaction details
 * 4. MainViewModel collects events from transactionEvents flow
 * 5. ViewModel refreshes UI with latest data
 *
 * Benefits:
 * - Decouples background processing from UI updates
 * - Works even when app is in background/foreground
 * - No polling or manual refresh needed
 * - Memory efficient (events are not buffered indefinitely)
 * - Multiple UI components can observe the same event stream
 *
 * Usage in Service:
 * ```kotlin
 * // After saving transaction
 * TransactionEventManager.emitNewTransactionEvent(
 *     smsId = transaction.smsId,
 *     amount = transaction.amount,
 *     type = transaction.type
 * )
 * ```
 *
 * Usage in ViewModel:
 * ```kotlin
 * init {
 *     viewModelScope.launch {
 *         TransactionEventManager.transactionEvents.collect { event ->
 *             // Handle new transaction event
 *             loadSmsData(context)
 *         }
 *     }
 * }
 * ```
 */
object TransactionEventManager {

    /**
     * Shared flow for broadcasting transaction events.
     *
     * Configuration:
     * - replay = 0: Don't replay events to new subscribers (events are time-sensitive)
     * - extraBufferCapacity = 10: Buffer up to 10 events if collectors are slow
     * - onBufferOverflow = DROP_OLDEST: Drop oldest events if buffer is full
     *
     * This ensures:
     * - Events are delivered to active subscribers immediately
     * - Slow collectors don't block event emission
     * - Memory usage is bounded (max 10 events buffered)
     */
    private val _transactionEvents = MutableSharedFlow<TransactionEvent>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    /**
     * Public read-only flow for observing transaction events.
     * UI components should collect from this flow to receive real-time updates.
     */
    val transactionEvents: SharedFlow<TransactionEvent> = _transactionEvents.asSharedFlow()

    /**
     * Emits a new transaction event to all active subscribers.
     *
     * This method is called by background services after successfully saving
     * a new transaction to the database.
     *
     * The event emission is non-blocking - if no collectors are active, the event
     * is buffered (up to extraBufferCapacity). If buffer is full, oldest events
     * are dropped to prevent memory issues.
     *
     * Error Handling:
     * - If event emission fails (very rare), error is logged but not thrown
     * - Service continues normally even if event emission fails
     * - UI will still show updated data on next manual refresh
     *
     * @param smsId The unique SMS ID of the new transaction
     * @param amount The transaction amount
     * @param type The transaction type (DEBIT/CREDIT/UNKNOWN)
     */
    suspend fun emitNewTransactionEvent(
        smsId: Long,
        amount: Double,
        type: com.example.kanakku.data.model.TransactionType
    ) {
        try {
            val event = TransactionEvent.NewTransaction(
                smsId = smsId,
                amount = amount,
                type = type,
                timestamp = System.currentTimeMillis()
            )

            _transactionEvents.emit(event)

            ErrorHandler.logInfo(
                "Transaction event emitted: smsId=$smsId, amount=$amount, type=$type",
                "TransactionEventManager"
            )
        } catch (e: Exception) {
            // Log error but don't throw - event emission is not critical
            // UI will still update on next manual refresh
            ErrorHandler.handleError(e, "TransactionEventManager.emitNewTransactionEvent")
        }
    }

    /**
     * Emits a transaction deleted event to all active subscribers.
     *
     * This method can be called when a transaction is deleted from the database.
     * Currently not used in the app, but provided for future extensibility.
     *
     * @param smsId The SMS ID of the deleted transaction
     */
    suspend fun emitTransactionDeletedEvent(smsId: Long) {
        try {
            val event = TransactionEvent.TransactionDeleted(
                smsId = smsId,
                timestamp = System.currentTimeMillis()
            )

            _transactionEvents.emit(event)

            ErrorHandler.logInfo(
                "Transaction deleted event emitted: smsId=$smsId",
                "TransactionEventManager"
            )
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "TransactionEventManager.emitTransactionDeletedEvent")
        }
    }
}

/**
 * Sealed class representing different types of transaction events.
 *
 * This allows for type-safe event handling and easy extensibility.
 * Future event types can be added as new sealed class implementations.
 */
sealed class TransactionEvent {
    /**
     * Event emitted when a new transaction is saved to the database.
     *
     * @property smsId The unique SMS ID of the transaction
     * @property amount The transaction amount
     * @property type The transaction type (DEBIT/CREDIT/UNKNOWN)
     * @property timestamp The timestamp when the event was emitted (not transaction date)
     */
    data class NewTransaction(
        val smsId: Long,
        val amount: Double,
        val type: com.example.kanakku.data.model.TransactionType,
        val timestamp: Long
    ) : TransactionEvent()

    /**
     * Event emitted when a transaction is deleted from the database.
     *
     * @property smsId The SMS ID of the deleted transaction
     * @property timestamp The timestamp when the event was emitted
     */
    data class TransactionDeleted(
        val smsId: Long,
        val timestamp: Long
    ) : TransactionEvent()
}
