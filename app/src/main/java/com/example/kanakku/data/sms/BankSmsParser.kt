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

        // UPI-specific reference number patterns
        // Handles UPI-specific reference formats that differ from standard bank references:
        // - "UPI Ref: 123456789012" or "UPI Ref No: 987654321098"
        // - "UPI Txn ID: ABCD1234567890" or "UPI Transaction ID: XYZ123456"
        // - "Txn ID: PP123456789" (PhonePe, Paytm style)
        // - "Google Ref ID: 1234567890" (Google Pay)
        // - "PhonePe Txn ID: PP987654321"
        // - "Paytm Txn ID: PTM123456789"
        // - "UTR: 123456789012" (bank UPI)
        // Reference must contain at least one digit
        private val UPI_REFERENCE_PATTERN = Regex(
            """(?:UPI\s*(?:Ref(?:erence)?|Txn|Transaction)\s*(?:ID|No|Number)?|Google\s*Ref\s*ID|PhonePe\s*Txn\s*ID|Paytm\s*Txn\s*ID|Txn\s*(?:ID|No)|Transaction\s*(?:ID|No)|UTR)[:\s#]*([A-Z0-9]*\d[A-Z0-9]{5,21})""",
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

        // VPA (Virtual Payment Address) pattern for UPI IDs
        // Matches formats like: user@paytm, merchant@okaxis, name@ybl, person.name@upi, etc.
        // Common UPI handles: @paytm, @okaxis, @okicici, @okhdfcbank, @ybl, @ibl, @axl,
        // @axisbank, @sbi, @icici, @hdfc, @upi, @oksbi, @okhdfc, @hdfcbank, @sbibank, etc.
        private val VPA_PATTERN = Regex(
            """([a-zA-Z0-9][a-zA-Z0-9._-]{2,}@(?:paytm|okaxis|okicici|okhdfcbank|okhdfc|oksbi|ybl|ibl|axl|axisbank|axis|sbi|sbibank|icici|icicib(?:ank)?|hdfc|hdfcbank|upi|apl|indianbank|indbank|pnb|bob|unionbank|ubibank|canara|canarabank|cboi|cbin|barodapay|federal|rbl|idfc|idfcbank|kotak|kotakbank|indus|indusind|yes|yesbank|dbs|sc|hsbc|citi|citibank|jupiter|freecharge|mobikwik|airtel|olamoney|jio|postbank|equitas|dcu|cub))""",
            RegexOption.IGNORE_CASE
        )

        // VPA extraction with context - extract VPA along with context keywords
        // Matches patterns like: "to user@paytm", "from merchant@okaxis", "VPA: name@ybl", etc.
        private val VPA_WITH_CONTEXT_PATTERN = Regex(
            """(?:to|from|VPA:?|UPI\s*ID:?|paid\s*to|received\s*from|sent\s*to)\s+([a-zA-Z0-9][a-zA-Z0-9._-]{2,}@(?:paytm|okaxis|okicici|okhdfcbank|okhdfc|oksbi|ybl|ibl|axl|axisbank|axis|sbi|sbibank|icici|icicib(?:ank)?|hdfc|hdfcbank|upi|apl|indianbank|indbank|pnb|bob|unionbank|ubibank|canara|canarabank|cboi|cbin|barodapay|federal|rbl|idfc|idfcbank|kotak|kotakbank|indus|indusind|yes|yesbank|dbs|sc|hsbc|citi|citibank|jupiter|freecharge|mobikwik|airtel|olamoney|jio|postbank|equitas|dcu|cub))""",
            RegexOption.IGNORE_CASE
        )

        // UPI-specific merchant extraction patterns
        // Handles UPI-specific context keywords for P2P and P2M transactions
        // Matches patterns like:
        // - "to Merchant Name" (debit to merchant)
        // - "paid to XYZ" (payment to merchant)
        // - "sent to ABC" (money sent to person/merchant)
        // - "from Person Name" (credit from person/merchant)
        // - "received from DEF" (money received from person/merchant)
        // - "transferred to GHI" (transfer to person/merchant)
        // Captures name until common terminators (Ref, UPI, on, A/c, Rs, ₹, ., @, etc.)
        private val UPI_MERCHANT_PATTERN = Regex(
            """(?:(?:paid|sent|transferred)\s+to|to|from|received\s+from)\s+([A-Za-z0-9][A-Za-z0-9\s&.',-]{1,48}?)(?:\s+(?:on|Ref|UPI|A/?c|Rs\.?|₹|INR|@)|[.@]|\s*$)""",
            RegexOption.IGNORE_CASE
        )

        // Maximum length for merchant names
        private const val MAX_MERCHANT_LENGTH = 50

        // Common noise patterns to remove from merchant names
        private val MERCHANT_NOISE_PATTERN = Regex(
            """\s+(PVT\.?\s*LTD\.?|LTD\.?|LIMITED|INC\.?|CORP\.?|CO\.?)\s*$""",
            RegexOption.IGNORE_CASE
        )

        /**
         * Normalize merchant/payee name by cleaning and formatting.
         *
         * Performs the following normalizations:
         * 1. Trims leading/trailing whitespace
         * 2. Replaces multiple consecutive spaces with a single space
         * 3. Removes common business suffixes (PVT LTD, LIMITED, INC, etc.)
         * 4. Removes excessive special characters (multiple dots, dashes, etc.)
         * 5. Capitalizes each word (title case) for consistency
         * 6. Limits length to MAX_MERCHANT_LENGTH characters
         *
         * @param merchantName Raw merchant name extracted from SMS
         * @return Cleaned and normalized merchant name, or null if input is null/blank
         */
        private fun normalizeMerchantName(merchantName: String?): String? {
            if (merchantName.isNullOrBlank()) {
                return null
            }

            var normalized = merchantName.trim()

            // Replace multiple consecutive spaces with a single space
            normalized = normalized.replace(Regex("""\s+"""), " ")

            // Remove common business suffixes (e.g., "ABC Pvt Ltd" -> "ABC")
            normalized = normalized.replace(MERCHANT_NOISE_PATTERN, "")

            // Remove excessive consecutive special characters (e.g., "..." -> ".", "---" -> "-")
            normalized = normalized.replace(Regex("""\.{2,}"""), ".")
            normalized = normalized.replace(Regex("""-{2,}"""), "-")
            normalized = normalized.replace(Regex("""_{2,}"""), "_")

            // Remove trailing dots, commas, dashes, underscores
            normalized = normalized.replace(Regex("""[.,\-_]+$"""), "")

            // Remove leading dots, commas, dashes, underscores
            normalized = normalized.replace(Regex("""^[.,\-_]+"""), "")

            // Final trim to remove any remaining whitespace
            normalized = normalized.trim()

            // Return null if the result is empty after cleaning
            if (normalized.isBlank()) {
                return null
            }

            // Capitalize each word for consistency (e.g., "john doe" -> "John Doe")
            normalized = normalized.split(" ")
                .joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }

            // Limit length to maximum allowed
            return normalized.take(MAX_MERCHANT_LENGTH)
        }
    }

    /**
     * Extract VPA (Virtual Payment Address) from SMS message.
     *
     * Handles both sender and receiver VPAs by looking for context keywords:
     * - For debit: "to", "paid to", "sent to" (payee VPA)
     * - For credit: "from", "received from" (payer VPA)
     * - Direct VPA indicators: "VPA:", "UPI ID:"
     *
     * @param smsBody The SMS message body
     * @return VPA string (e.g., "merchant@paytm") or null if not found
     */
    fun extractVpa(smsBody: String): String? {
        // First try to extract VPA with context (more accurate)
        val contextMatch = VPA_WITH_CONTEXT_PATTERN.find(smsBody)
        if (contextMatch != null) {
            return contextMatch.groupValues.getOrNull(1)?.lowercase()
        }

        // Fallback: Extract any VPA from the message
        val vpaMatch = VPA_PATTERN.find(smsBody)
        return vpaMatch?.groupValues?.getOrNull(1)?.lowercase()
    }

    /**
     * Extract merchant name from VPA (Virtual Payment Address).
     *
     * Extracts a human-readable merchant name from a UPI VPA by taking
     * the username part (before @) and converting it to title case.
     *
     * Examples:
     * - "swiggy@axisbank" → "Swiggy"
     * - "amazon.pay@icici" → "Amazon Pay"
     * - "john.doe@paytm" → "John Doe"
     * - "phonepe_user123@ybl" → "Phonepe User123"
     *
     * @param vpa The VPA string (e.g., "merchant@handle")
     * @return Normalized merchant name extracted from VPA, or null if extraction fails
     */
    fun extractMerchantFromVpa(vpa: String?): String? {
        if (vpa.isNullOrBlank()) {
            return null
        }

        // Extract username part before @
        val username = vpa.substringBefore('@').trim()

        if (username.isBlank() || username.length < 3) {
            // Too short to be meaningful (e.g., "ab@paytm")
            return null
        }

        // Replace dots, underscores, and hyphens with spaces
        var merchantName = username
            .replace('.', ' ')
            .replace('_', ' ')
            .replace('-', ' ')

        // Remove numbers if the result would still be meaningful
        val withoutNumbers = merchantName.replace(Regex("""\d+"""), "").trim()
        if (withoutNumbers.length >= 3) {
            merchantName = withoutNumbers
        }

        // Normalize and return
        return normalizeMerchantName(merchantName)
    }

    /**
     * Extract UPI-specific reference number from SMS message.
     *
     * Prioritizes UPI-specific reference number formats before falling back to generic patterns.
     * UPI transactions often use different reference formats than standard bank transactions:
     *
     * UPI-specific formats (checked first):
     * - "UPI Ref: 123456789012" or "UPI Ref No: 987654321098"
     * - "UPI Txn ID: ABCD1234567890" or "UPI Transaction ID: XYZ123456"
     * - "Google Ref ID: 1234567890" (Google Pay)
     * - "PhonePe Txn ID: PP987654321" (PhonePe)
     * - "Paytm Txn ID: PTM123456789" (Paytm)
     * - "Txn ID: 123456789" (generic transaction ID)
     * - "UTR: 123456789012" (Unified Transaction Reference)
     *
     * Generic formats (fallback):
     * - "Ref: 123456789012"
     * - "RRN: 123456789012"
     *
     * @param smsBody The SMS message body
     * @return Reference number string or null if not found
     */
    fun extractUpiReference(smsBody: String): String? {
        // Try UPI-specific reference pattern first (more accurate for UPI)
        val upiRefMatch = UPI_REFERENCE_PATTERN.find(smsBody)
        if (upiRefMatch != null) {
            return upiRefMatch.groupValues.getOrNull(1)
        }

        // Fallback to generic reference pattern
        val genericRefMatch = REFERENCE_PATTERN.find(smsBody)
        return genericRefMatch?.groupValues?.getOrNull(1)
    }

    /**
     * Extract merchant/payee name from UPI transaction SMS.
     *
     * Uses UPI-specific patterns to extract merchant names from messages:
     * - "paid to Merchant Name" (P2M payment)
     * - "to XYZ" (transfer to merchant/person)
     * - "sent to ABC" (money sent to person/merchant)
     * - "from Person Name" (credit from person/merchant)
     * - "received from DEF" (money received)
     * - "transferred to GHI" (transfer to person)
     *
     * This method prioritizes UPI-specific context keywords over generic merchant patterns,
     * providing better accuracy for UPI transactions. If direct extraction fails, falls back
     * to extracting merchant name from VPA (e.g., "swiggy@axisbank" → "Swiggy").
     * The extracted merchant name is normalized (cleaned and formatted) before being returned.
     *
     * @param smsBody The SMS message body
     * @return Normalized merchant/payee name or null if not found
     */
    fun extractUpiMerchant(smsBody: String): String? {
        // Try UPI-specific merchant pattern first (most accurate for UPI)
        val upiMerchantMatch = UPI_MERCHANT_PATTERN.find(smsBody)
        if (upiMerchantMatch != null) {
            val rawMerchant = upiMerchantMatch.groupValues.getOrNull(1)
            val merchant = normalizeMerchantName(rawMerchant)
            if (!merchant.isNullOrBlank()) {
                return merchant
            }
        }

        // Fallback to generic merchant pattern
        val merchantMatch = MERCHANT_PATTERN.find(smsBody)
        val rawMerchant = merchantMatch?.groupValues?.getOrNull(1)
        val merchant = normalizeMerchantName(rawMerchant)
        if (!merchant.isNullOrBlank()) {
            return merchant
        }

        // Final fallback: Extract merchant from VPA if available
        val vpa = extractVpa(smsBody)
        return extractMerchantFromVpa(vpa)
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
     * Parse a UPI transaction SMS into a structured transaction.
     *
     * This method is specifically designed for UPI transactions and uses
     * UPI-specific extraction logic for better accuracy:
     * - Extracts UPI VPA (Virtual Payment Address) using extractVpa()
     * - Extracts merchant using extractUpiMerchant() with UPI-specific patterns
     * - Sets paymentMethod to "UPI"
     * - Handles both P2P and P2M UPI transactions
     *
     * @param sms The UPI transaction SMS message to parse
     * @return ParsedTransaction with UPI-specific fields populated, or null if parsing fails
     */
    fun parseUpiSms(sms: SmsMessage): ParsedTransaction? {
        if (!isUpiTransactionSms(sms)) {
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

        // Extract UPI VPA (Virtual Payment Address)
        val upiId = extractVpa(body)

        // Extract merchant/payee using UPI-specific patterns
        val merchant = extractUpiMerchant(body)

        // Extract reference number using UPI-specific extraction
        val referenceNumber = extractUpiReference(body)

        // Extract account number
        val accountMatch = ACCOUNT_PATTERN.find(body)
        val accountNumber = accountMatch?.groupValues?.get(1)

        // Extract balance after transaction
        val balanceMatch = BALANCE_PATTERN.find(body)
        val balanceAfter = balanceMatch?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()

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
            location = null,  // UPI transactions typically don't have location
            upiId = upiId,
            paymentMethod = "UPI"
        )
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
     * Parse a bank SMS into a structured transaction.
     *
     * This method intelligently routes SMS messages to the appropriate parser:
     * - UPI transaction messages are routed to parseUpiSms() for UPI-specific parsing
     * - Other bank transaction messages are parsed using generic bank SMS patterns
     *
     * UPI transactions are prioritized because they require specialized extraction logic
     * for VPA, merchant names, and payment method identification.
     */
    fun parseSms(sms: SmsMessage): ParsedTransaction? {
        // Priority 1: Check if this is a UPI transaction
        // UPI transactions need specialized parsing for VPA extraction and merchant identification
        if (isUpiTransactionSms(sms)) {
            return parseUpiSms(sms)
        }

        // Priority 2: Check if this is a generic bank transaction
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

        // Extract merchant (best effort) and normalize
        val merchantMatch = MERCHANT_PATTERN.find(body)
        val rawMerchant = merchantMatch?.groupValues?.get(1)
        val merchant = normalizeMerchantName(rawMerchant)

        // Extract balance after transaction
        val balanceMatch = BALANCE_PATTERN.find(body)
        val balanceAfter = balanceMatch?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()

        // Extract location (ATM/branch) and normalize
        val locationMatch = LOCATION_PATTERN.find(body)
        val rawLocation = locationMatch?.groupValues?.get(1)
        val location = normalizeMerchantName(rawLocation)

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
