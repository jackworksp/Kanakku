package com.example.kanakku.data.sms

import android.content.Context
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.SmsMessage

/**
 * Data source for SMS-related operations.
 *
 * This class encapsulates all SMS reading and parsing operations, providing
 * a clean API for the repository layer. It wraps SmsReader and BankSmsParser
 * to abstract the complexity of SMS access and transaction parsing.
 *
 * Key responsibilities:
 * - Read SMS messages from device inbox
 * - Filter bank transaction SMS
 * - Parse SMS into structured transactions
 * - Handle deduplication of transactions
 * - Provide comprehensive error handling
 *
 * Error Handling Strategy:
 * - All operations wrapped with ErrorHandler for consistent error handling
 * - Methods return Result<T> for explicit error propagation
 * - Partial results returned on non-critical errors (e.g., some SMS fail to parse)
 * - Permission errors handled gracefully with empty results
 *
 * @param context Android context for accessing SMS content provider
 */
class SmsDataSource(context: Context) {

    private val smsReader = SmsReader(context)
    private val bankSmsParser = BankSmsParser()

    // ==================== SMS Reading Operations ====================

    /**
     * Reads all SMS messages from inbox for the specified number of days.
     *
     * This is a low-level operation that returns raw SMS messages without
     * filtering or parsing. Use readBankSms() for typical transaction sync.
     *
     * @param sinceDaysAgo Number of days to look back (default: 30)
     * @return Result containing list of all SMS messages or error information
     */
    suspend fun readAllSms(sinceDaysAgo: Int = 30): Result<List<SmsMessage>> {
        return ErrorHandler.runSuspendCatching("Read all SMS ($sinceDaysAgo days)") {
            smsReader.readInboxSms(sinceDaysAgo)
        }
    }

    /**
     * Reads SMS messages since a specific timestamp.
     *
     * Useful for incremental sync operations where you only want new messages.
     *
     * @param sinceTimestamp Unix timestamp in milliseconds (exclusive)
     * @return Result containing list of SMS messages after the timestamp or error information
     */
    suspend fun readSmsSince(sinceTimestamp: Long): Result<List<SmsMessage>> {
        return ErrorHandler.runSuspendCatching("Read SMS since timestamp $sinceTimestamp") {
            smsReader.readSmsSince(sinceTimestamp)
        }
    }

    // ==================== Bank SMS Filtering Operations ====================

    /**
     * Reads and filters bank transaction SMS from inbox.
     *
     * This method:
     * 1. Reads all SMS from the specified time period
     * 2. Filters to only bank transaction messages
     * 3. Returns filtered results
     *
     * @param sinceDaysAgo Number of days to look back (default: 30)
     * @return Result containing list of bank transaction SMS or error information
     */
    suspend fun readBankSms(sinceDaysAgo: Int = 30): Result<List<SmsMessage>> {
        return ErrorHandler.runSuspendCatching("Read bank SMS ($sinceDaysAgo days)") {
            val allSms = smsReader.readInboxSms(sinceDaysAgo)
            bankSmsParser.filterBankSms(allSms)
        }
    }

    /**
     * Reads and filters bank transaction SMS since a specific timestamp.
     *
     * Useful for incremental sync operations.
     *
     * @param sinceTimestamp Unix timestamp in milliseconds (exclusive)
     * @return Result containing list of bank transaction SMS or error information
     */
    suspend fun readBankSmsSince(sinceTimestamp: Long): Result<List<SmsMessage>> {
        return ErrorHandler.runSuspendCatching("Read bank SMS since timestamp $sinceTimestamp") {
            val allSms = smsReader.readSmsSince(sinceTimestamp)
            bankSmsParser.filterBankSms(allSms)
        }
    }

    // ==================== SMS Parsing Operations ====================

    /**
     * Parses a single SMS message into a transaction.
     *
     * @param sms The SMS message to parse
     * @return Result containing ParsedTransaction if successful, null if not a bank SMS, or error information
     */
    suspend fun parseSms(sms: SmsMessage): Result<ParsedTransaction?> {
        return ErrorHandler.runSuspendCatching("Parse SMS ${sms.id}") {
            bankSmsParser.parseSms(sms)
        }
    }

    /**
     * Parses multiple SMS messages into transactions.
     *
     * This method attempts to parse all messages and returns partial results
     * if some messages fail to parse.
     *
     * @param smsList List of SMS messages to parse
     * @return Result containing list of successfully parsed transactions or error information
     */
    suspend fun parseAllSms(smsList: List<SmsMessage>): Result<List<ParsedTransaction>> {
        return ErrorHandler.runSuspendCatching("Parse ${smsList.size} SMS messages") {
            bankSmsParser.parseAllBankSms(smsList)
        }
    }

    /**
     * Parses SMS messages and removes duplicates.
     *
     * Duplicates are identified by:
     * 1. Same reference number (if available)
     * 2. Same amount + type + account + within 1 minute
     *
     * @param smsList List of SMS messages to parse and deduplicate
     * @return Result containing deduplicated list of transactions or error information
     */
    suspend fun parseAndDeduplicate(smsList: List<SmsMessage>): Result<List<ParsedTransaction>> {
        return ErrorHandler.runSuspendCatching("Parse and deduplicate ${smsList.size} SMS") {
            bankSmsParser.parseAndDeduplicate(smsList)
        }
    }

    /**
     * Removes duplicate transactions from a list.
     *
     * Useful when you already have parsed transactions and need to deduplicate.
     *
     * @param transactions List of transactions to deduplicate
     * @return Result containing deduplicated list of transactions or error information
     */
    suspend fun removeDuplicates(transactions: List<ParsedTransaction>): Result<List<ParsedTransaction>> {
        return ErrorHandler.runSuspendCatching("Remove duplicates from ${transactions.size} transactions") {
            bankSmsParser.removeDuplicates(transactions)
        }
    }

    // ==================== High-Level Convenience Methods ====================

    /**
     * Reads, filters, parses, and deduplicates bank SMS in one operation.
     *
     * This is the primary method for syncing transactions from SMS.
     * It orchestrates the full pipeline:
     * 1. Read SMS from inbox
     * 2. Filter to bank transaction SMS
     * 3. Parse into structured transactions
     * 4. Remove duplicates
     *
     * @param sinceDaysAgo Number of days to look back (default: 30)
     * @return Result containing deduplicated list of transactions or error information
     */
    suspend fun readAndParseTransactions(sinceDaysAgo: Int = 30): Result<List<ParsedTransaction>> {
        return ErrorHandler.runSuspendCatching("Read and parse transactions ($sinceDaysAgo days)") {
            ErrorHandler.logDebug("Starting SMS read and parse operation", "SmsDataSource")

            // Read all SMS
            val allSms = smsReader.readInboxSms(sinceDaysAgo)
            ErrorHandler.logDebug("Read ${allSms.size} SMS messages", "SmsDataSource")

            // Filter to bank SMS
            val bankSms = bankSmsParser.filterBankSms(allSms)
            ErrorHandler.logDebug("Filtered to ${bankSms.size} bank SMS", "SmsDataSource")

            // Parse and deduplicate
            val transactions = bankSmsParser.parseAndDeduplicate(bankSms)
            ErrorHandler.logDebug("Parsed ${transactions.size} unique transactions", "SmsDataSource")

            transactions
        }
    }

    /**
     * Reads, filters, parses, and deduplicates bank SMS since a timestamp.
     *
     * Incremental version of readAndParseTransactions() for efficient syncing.
     *
     * @param sinceTimestamp Unix timestamp in milliseconds (exclusive)
     * @return Result containing deduplicated list of new transactions or error information
     */
    suspend fun readAndParseTransactionsSince(sinceTimestamp: Long): Result<List<ParsedTransaction>> {
        return ErrorHandler.runSuspendCatching("Read and parse transactions since $sinceTimestamp") {
            ErrorHandler.logDebug("Starting incremental SMS read and parse operation", "SmsDataSource")

            // Read SMS since timestamp
            val allSms = smsReader.readSmsSince(sinceTimestamp)
            ErrorHandler.logDebug("Read ${allSms.size} SMS messages since timestamp", "SmsDataSource")

            // Filter to bank SMS
            val bankSms = bankSmsParser.filterBankSms(allSms)
            ErrorHandler.logDebug("Filtered to ${bankSms.size} bank SMS", "SmsDataSource")

            // Parse and deduplicate
            val transactions = bankSmsParser.parseAndDeduplicate(bankSms)
            ErrorHandler.logDebug("Parsed ${transactions.size} unique new transactions", "SmsDataSource")

            transactions
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Checks if an SMS message is a bank transaction.
     *
     * @param sms The SMS message to check
     * @return Result containing true if bank transaction SMS, false otherwise, or error information
     */
    suspend fun isBankTransactionSms(sms: SmsMessage): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Check if SMS ${sms.id} is bank transaction") {
            bankSmsParser.isBankTransactionSms(sms)
        }
    }
}
