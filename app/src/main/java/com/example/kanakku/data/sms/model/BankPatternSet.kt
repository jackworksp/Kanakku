package com.example.kanakku.data.sms.model

/**
 * Bank-specific regex patterns for parsing SMS transaction messages.
 *
 * Different banks use varying formats for amounts, balances, merchant names, and reference numbers.
 * This class allows defining custom patterns for each bank to improve parsing accuracy.
 *
 * All patterns are optional - if null, the parser will fall back to generic patterns.
 * Only define custom patterns when a bank uses a format that differs from the standard.
 *
 * Examples of when custom patterns are needed:
 * - A bank uses "Amt:" instead of "Rs." for amounts
 * - Balance format is "Avl.Bal" instead of "Bal"
 * - Reference numbers have a specific prefix like "HDFC" or "SBI"
 * - Merchant names are in a specific format or position
 *
 * @property amountPattern Custom regex for extracting transaction amounts.
 *                         Should have a capturing group for the numeric value.
 *                         Example: `Regex("Amt:\\s*([\\d,]+(?:\\.\\d{1,2})?)")`
 *
 * @property balancePattern Custom regex for extracting account balance after transaction.
 *                          Should have a capturing group for the numeric value.
 *                          Example: `Regex("Avl\\.Bal\\s*Rs\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)")`
 *
 * @property merchantPattern Custom regex for extracting merchant/payee name.
 *                           Should have a capturing group for the merchant name.
 *                           Example: `Regex("at\\s+([A-Za-z0-9\\s]+?)\\s+on")`
 *
 * @property referencePattern Custom regex for extracting transaction reference/UPI/UTR number.
 *                            Should have a capturing group for the reference value.
 *                            Example: `Regex("Ref\\.No:\\s*([A-Z0-9]+)")`
 */
data class BankPatternSet(
    val amountPattern: Regex? = null,
    val balancePattern: Regex? = null,
    val merchantPattern: Regex? = null,
    val referencePattern: Regex? = null
)
