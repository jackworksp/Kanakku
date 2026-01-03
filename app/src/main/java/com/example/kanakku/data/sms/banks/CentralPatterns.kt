package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Central Bank of India.
 *
 * Central Bank of India is a major nationalized bank and one of the oldest commercial banks
 * in India. The bank uses multiple sender IDs for transaction alerts across different services
 * (Internet Banking, Mobile Banking, Cards, UPI, etc.).
 *
 * Common Central Bank sender IDs:
 * - VM-CNTBNK: Internet Banking and general transactions
 * - AD-CNTBNK: Advertorial and promotional banking alerts
 * - CNTBNK: Short code for general banking alerts
 * - CENBNK: Central Bank short code
 * - CBINDIA: Central Bank India alerts
 * - CBIUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Your A/c XX1234 debited with Rs.500.00 on 03-Jan-26. Avl Bal: Rs.10000.00. Ref: 123456789012"
 * - Credit: "Your A/c XX1234 credited with Rs.1000.00 on 03-Jan-26. Info: NEFT-SALARY. Avl Bal: Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-Jan-26 via UPI to merchant@paytm. UPI Ref: 123456789012"
 * - Card: "Your Central Bank Credit Card XX1234 used for Rs.1500.00 at MERCHANT on 03-Jan-26"
 * - ATM: "Your A/c XX1234 debited with Rs.2000.00 on 03-Jan-26 at ATM. Avl Bal: Rs.8000.00"
 */
object CentralPatterns {

    /**
     * Returns the bank configuration for Central Bank of India.
     *
     * Includes all known sender IDs used by Central Bank of India for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Central Bank of India",
            displayName = "Central Bank",
            senderIds = listOf(
                "VM-CNTBNK",     // Internet Banking & General
                "AD-CNTBNK",     // Advertorial/Promotional
                "CNTBNK",        // Short code
                "CENBNK",        // Central Bank short code
                "CBINDIA",       // Central Bank India
                "CBIUPI",        // UPI transactions
                "CBIMB",         // Mobile Banking
                "CBICC",         // Credit Card
                "CBIATM",        // ATM transactions
                "VM-CBICC"       // Credit Card (alternate)
            )
        )
    }

    /**
     * Returns Central Bank of India-specific SMS parsing patterns.
     *
     * Central Bank of India generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal: Rs.XXX" or "Available Bal: Rs.XXX"
     * - Reference: "Ref:", "UPI Ref:", "Ref No"
     * - Merchant: "at MERCHANT" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Central Bank of India differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Central Bank of India follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
