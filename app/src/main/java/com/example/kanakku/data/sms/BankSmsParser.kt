package com.example.kanakku.data.sms

import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.SmsMessage
import com.example.kanakku.data.model.TransactionType

class BankSmsParser {

    companion object {
        // Common bank sender ID patterns (VM-HDFCBK, AD-SBIBNK, etc.)
        private val BANK_SENDER_PATTERN = Regex(
            """^[A-Z]{2}-[A-Z]+BK|^[A-Z]{2}-[A-Z]+BANK|^[A-Z]{2}-[A-Z]*CARD|^[A-Z]{2}-[A-Z]+FIN""",
            RegexOption.IGNORE_CASE
        )

        // UPI app sender ID patterns
        // Matches Google Pay, PhonePe, Paytm, Amazon Pay, BHIM, and bank UPI apps
        private val UPI_SENDER_PATTERN = Regex(
            """(?:^[A-Z]{2}-)?(?:GOOGL(?:E)?PAY|GPAY|G-?PAY|PHONEPE|PHONPE|PAYTM|PYTMPA|AMAZONP|AZNPAY|AMAZON-?PAY|BHIM|NPCI(?:UPI)?|UPIAPP|IMOBILE|IMOBL|YONO(?:SBI)?|SBIYONO|SBIPAY|SBIUPI|HDFCUPI|HDFCPAY|ICICIM(?:OBILE)?|ICICIPAY|ICICIBK|AXISBK|AXISBNK|AXISPAY|IDFCPAY|IDFC|KOTAKUPI|KOTAKBK|INDUSIND|YESBANK|YESBK|PNB(?:PAY|UPI)?|BOBUPI|BOB(?:MOBILE)?|UNION(?:UPI|PAY)?|CANARA(?:UPI)?|CBIN)""",
            RegexOption.IGNORE_CASE
        )

        // Amount patterns: Rs.500, Rs 500, INR 500, Rs.5,00,000.00, ₹500
        private val AMOUNT_PATTERN = Regex(
            """(?:Rs\.?|₹|INR)\s*([\d,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )

        // Balance pattern: Bal Rs.3305.19, Bal:Rs 500, Available Bal Rs.1000
        private val BALANCE_PATTERN = Regex(
            """(?:Bal|Balance|Avl\.?\s*Bal|Available\s*Bal)[:\s]*(?:Rs\.?|₹|INR)\s*([\d,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )

        // Debit keywords (includes UPI-specific terms)
        private val DEBIT_KEYWORDS = Regex(
            """(debited|debit|spent|paid|purchase|withdrawn|sent|payment|transferred|UPI[\s-](?:txn|transaction|payment|transfer|sent|paid|debit|debited))""",
            RegexOption.IGNORE_CASE
        )

        // Credit keywords (includes UPI-specific terms)
        private val CREDIT_KEYWORDS = Regex(
            """(credited|credit|received|deposited|refund|cashback|reversed|UPI[\s-](?:txn|transaction|credit|credited|received|refund))""",
            RegexOption.IGNORE_CASE
        )

        // Reference number patterns (UTR, REF, TXN, RRN)
        // Reference must contain at least one digit to avoid matching words like "Reversed"
        private val REFERENCE_PATTERN = Regex(
            """(?:Ref\.?|UTR|TXN|RRN|UPI|Ref\s*No\.?|Reference)[:\s#]*([A-Z0-9]*\d[A-Z0-9]{5,21})""",
            RegexOption.IGNORE_CASE
        )

        // Account/Card number pattern (XX1234, A/c XX1234, Acct ending 1234, Card x3255, Card 3255)
        private val ACCOUNT_PATTERN = Regex(
            """(?:A/?c|Acct?|Account|Card|ending)[:\s]*(?:no\.?)?[:\s]*[Xx*]*(\d{4,})""",
            RegexOption.IGNORE_CASE
        )

        // Merchant/Payee pattern (at MERCHANT, to MERCHANT, Info: MERCHANT)
        private val MERCHANT_PATTERN = Regex(
            """(?:at|to|from|Info:?|VPA:?|@)\s*([A-Za-z0-9@._\s-]+?)(?:\s+on|\s+Ref|\s+UPI|\.|\s*$)""",
            RegexOption.IGNORE_CASE
        )

        // Location pattern for ATM withdrawals: "At LOCATION On" or "At LOCATION."
        private val LOCATION_PATTERN = Regex(
            """[Aa]t\s+([A-Za-z0-9\s]+?(?:BR|ATM|BRANCH)?)\s+[Oo]n\s+\d{4}""",
            RegexOption.IGNORE_CASE
        )

        // OTP pattern to filter out OTP messages
        private val OTP_PATTERN = Regex(
            """OTP|One\s*Time\s*Password|verification\s*code|CVV""",
            RegexOption.IGNORE_CASE
        )
    }

    /**
     * Check if SMS is a UPI transaction message with better accuracy
     * than generic bank SMS detection.
     *
     * UPI transactions are identified by:
     * 1. Sender matches UPI app patterns (GPay, PhonePe, Paytm, etc.)
     * 2. Body contains UPI-specific keywords
     * 3. Body contains amount
     * 4. Body contains transaction keywords (debit/credit)
     *
     * @param sms The SMS message to check
     * @return true if this is a UPI transaction SMS
     */
    fun isUpiTransactionSms(sms: SmsMessage): Boolean {
        val body = sms.body
        val sender = sms.address

        // Filter out OTP messages
        if (OTP_PATTERN.containsMatchIn(body)) {
            return false
        }

        // Check if sender matches UPI app pattern
        val hasUpiSender = UPI_SENDER_PATTERN.containsMatchIn(sender)

        // Check if body contains UPI keyword (case-insensitive)
        val hasUpiKeyword = body.contains("UPI", ignoreCase = true)

        // Must have either UPI sender OR UPI keyword in body
        // (Some banks send UPI transactions from generic bank sender IDs)
        if (!hasUpiSender && !hasUpiKeyword) {
            return false
        }

        // Must contain amount pattern
        if (!AMOUNT_PATTERN.containsMatchIn(body)) {
            return false
        }

        // Must contain debit or credit keyword
        val hasDebitKeyword = DEBIT_KEYWORDS.containsMatchIn(body)
        val hasCreditKeyword = CREDIT_KEYWORDS.containsMatchIn(body)

        if (!hasDebitKeyword && !hasCreditKeyword) {
            return false
        }

        return true
    }

    /**
     * Check if SMS is likely a bank transaction message
     */
    fun isBankTransactionSms(sms: SmsMessage): Boolean {
        val body = sms.body

        // Filter out OTP messages
        if (OTP_PATTERN.containsMatchIn(body)) {
            return false
        }

        // Must contain amount pattern
        if (!AMOUNT_PATTERN.containsMatchIn(body)) {
            return false
        }

        // Must contain debit or credit keyword
        val hasDebitKeyword = DEBIT_KEYWORDS.containsMatchIn(body)
        val hasCreditKeyword = CREDIT_KEYWORDS.containsMatchIn(body)

        if (!hasDebitKeyword && !hasCreditKeyword) {
            return false
        }

        // Optional: Check if sender looks like a bank
        // Some banks use short codes, so we're lenient here
        return true
    }

    /**
     * Filter SMS list to only bank transaction messages
     */
    fun filterBankSms(smsList: List<SmsMessage>): List<SmsMessage> {
        return smsList.filter { isBankTransactionSms(it) }
    }

    /**
     * Parse a bank SMS into a structured transaction
     */
    fun parseSms(sms: SmsMessage): ParsedTransaction? {
        if (!isBankTransactionSms(sms)) {
            return null
        }

        val body = sms.body

        // Extract amount
        val amountMatch = AMOUNT_PATTERN.find(body)
        val amount = amountMatch?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull() ?: return null

        // Determine transaction type
        val type = when {
            DEBIT_KEYWORDS.containsMatchIn(body) -> TransactionType.DEBIT
            CREDIT_KEYWORDS.containsMatchIn(body) -> TransactionType.CREDIT
            else -> TransactionType.UNKNOWN
        }

        // Extract reference number
        val referenceMatch = REFERENCE_PATTERN.find(body)
        val referenceNumber = referenceMatch?.groupValues?.get(1)

        // Extract account number
        val accountMatch = ACCOUNT_PATTERN.find(body)
        val accountNumber = accountMatch?.groupValues?.get(1)

        // Extract merchant (best effort)
        val merchantMatch = MERCHANT_PATTERN.find(body)
        val merchant = merchantMatch?.groupValues?.get(1)?.trim()?.take(50)

        // Extract balance after transaction
        val balanceMatch = BALANCE_PATTERN.find(body)
        val balanceAfter = balanceMatch?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()

        // Extract location (ATM/branch)
        val locationMatch = LOCATION_PATTERN.find(body)
        val location = locationMatch?.groupValues?.get(1)?.trim()?.take(50)

        return ParsedTransaction(
            smsId = sms.id,
            amount = amount,
            type = type,
            merchant = merchant,
            accountNumber = accountNumber,
            referenceNumber = referenceNumber,
            date = sms.date,
            rawSms = sms.body,
            senderAddress = sms.address,
            balanceAfter = balanceAfter,
            location = location
        )
    }

    /**
     * Parse all bank SMS from a list
     */
    fun parseAllBankSms(smsList: List<SmsMessage>): List<ParsedTransaction> {
        return smsList.mapNotNull { parseSms(it) }
    }

    /**
     * Parse all bank SMS and remove duplicates.
     * Duplicates are identified by:
     * 1. Same reference number (if available)
     * 2. Same amount + same type + same account + within 5 minutes
     */
    fun parseAndDeduplicate(smsList: List<SmsMessage>): List<ParsedTransaction> {
        val allTransactions = parseAllBankSms(smsList)
        return removeDuplicates(allTransactions)
    }

    /**
     * Remove duplicate transactions.
     * Banks often send multiple SMS for the same transaction.
     */
    fun removeDuplicates(transactions: List<ParsedTransaction>): List<ParsedTransaction> {
        if (transactions.isEmpty()) return transactions

        // Sort by date (oldest first) to keep the first occurrence
        val sorted = transactions.sortedBy { it.date }
        val result = mutableListOf<ParsedTransaction>()
        val seenRefNumbers = mutableSetOf<String>()

        for (txn in sorted) {
            // Check 1: If has reference number, use it as unique identifier
            if (!txn.referenceNumber.isNullOrBlank()) {
                if (txn.referenceNumber in seenRefNumbers) {
                    // Duplicate by reference number, skip
                    continue
                }
                seenRefNumbers.add(txn.referenceNumber)
                result.add(txn)
                continue
            }

            // Check 2: No reference number - check for near-duplicates
            // Same amount + same type + same account + within 1 minute
            val isDuplicate = result.any { existing ->
                isSimilarTransaction(existing, txn)
            }

            if (!isDuplicate) {
                result.add(txn)
            }
        }

        // Return sorted by date descending (newest first) for display
        return result.sortedByDescending { it.date }
    }

    /**
     * Check if two transactions are likely duplicates.
     *
     * Real duplicates occur when banks send the same SMS multiple times for
     * the same transaction - these arrive within SECONDS of each other.
     *
     * Legitimate different transactions (even with same amount/balance) will
     * have timestamps at least 1 minute apart.
     */
    private fun isSimilarTransaction(a: ParsedTransaction, b: ParsedTransaction): Boolean {
        // Must have same amount
        if (a.amount != b.amount) return false

        // Must have same type
        if (a.type != b.type) return false

        // CRITICAL: Use tight time window (1 minute) for duplicate detection.
        // Real duplicates from banks arrive within seconds, not minutes.
        // Transactions 1+ minute apart are separate transactions, even if
        // they have the same amount/balance (e.g., after a reversal).
        val timeDiffSeconds = kotlin.math.abs(a.date - b.date) / 1000
        if (timeDiffSeconds > 60) return false

        // If both have account numbers, they must match
        if (!a.accountNumber.isNullOrBlank() && !b.accountNumber.isNullOrBlank()) {
            if (a.accountNumber != b.accountNumber) return false
        }

        // If both have balance info and balances are different,
        // these are DEFINITELY separate transactions, not duplicates!
        if (a.balanceAfter != null && b.balanceAfter != null) {
            if (a.balanceAfter != b.balanceAfter) return false
        }

        // Likely a duplicate (same amount, type, account, balance, within 1 minute)
        return true
    }
}
