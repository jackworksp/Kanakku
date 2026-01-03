package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Indian Bank.
 *
 * Indian Bank is a major nationalized bank in India and was merged with Allahabad Bank in 2020,
 * making it the seventh-largest public sector bank. The bank uses multiple sender IDs for
 * transaction alerts across different services (Internet Banking, Mobile Banking, Cards, UPI, etc.).
 *
 * Common Indian Bank sender IDs:
 * - VM-INBBNK: Internet Banking and general transactions
 * - AD-INBBNK: Advertorial and promotional banking alerts
 * - INBBNK: Short code for general banking alerts
 * - INDIANBK: Indian Bank alerts
 * - INDBNK: Short code variant
 * - INBUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Your A/c XX1234 debited with Rs.500.00 on 03-Jan-26. Avl Bal: Rs.10000.00. Ref: 123456789012"
 * - Credit: "Your A/c XX1234 credited with Rs.1000.00 on 03-Jan-26. Info: NEFT-SALARY. Avl Bal: Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-Jan-26 via UPI to merchant@paytm. UPI Ref: 123456789012"
 * - Card: "Your Indian Bank Credit Card XX1234 used for Rs.1500.00 at MERCHANT on 03-Jan-26"
 * - ATM: "Your A/c XX1234 debited with Rs.2000.00 on 03-Jan-26 at ATM. Avl Bal: Rs.8000.00"
 */
object IndianBankPatterns {

    /**
     * Returns the bank configuration for Indian Bank.
     *
     * Includes all known sender IDs used by Indian Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Indian Bank",
            displayName = "Indian Bank",
            senderIds = listOf(
                "VM-INBBNK",     // Internet Banking & General
                "AD-INBBNK",     // Advertorial/Promotional
                "INBBNK",        // Short code
                "INDIANBK",      // Indian Bank
                "INDBNK",        // Short code variant
                "INBUPI",        // UPI transactions
                "INBMB",         // Mobile Banking
                "INBCC",         // Credit Card
                "INBATM",        // ATM transactions
                "VM-INBCC"       // Credit Card (alternate)
            )
        )
    }

    /**
     * Returns Indian Bank-specific SMS parsing patterns.
     *
     * Indian Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal: Rs.XXX" or "Available Bal: Rs.XXX"
     * - Reference: "Ref:", "UPI Ref:", "Ref No"
     * - Merchant: "at MERCHANT" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Indian Bank differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Indian Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
