package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Federal Bank.
 *
 * Federal Bank is a major private sector bank headquartered in Kerala, India,
 * and uses multiple sender IDs for transaction alerts across different services
 * (Internet Banking, Mobile Banking, Cards, UPI, etc.).
 *
 * Common Federal Bank sender IDs:
 * - VM-FEDBNK: Internet Banking and general transactions
 * - AD-FEDBNK: Advertorial and promotional banking alerts
 * - FEDBNK: Short code for general alerts
 * - FEDERALBK: Alternative banking alerts
 * - FEDERAL: Simple short code
 * - FEDUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Your A/c XX1234 debited with Rs.500.00 on 03-Jan-26. Avl Bal: Rs.10000.00. Ref: 123456789012"
 * - Credit: "Your A/c XX1234 credited with Rs.1000.00 on 03-Jan-26. Info: NEFT-SALARY. Avl Bal: Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-Jan-26 via UPI to merchant@paytm. UPI Ref: 123456789012"
 * - Card: "Your Federal Bank Credit Card XX1234 used for Rs.1500.00 at MERCHANT on 03-Jan-26"
 * - ATM: "Your A/c XX1234 debited with Rs.2000.00 on 03-Jan-26 at ATM. Avl Bal: Rs.8000.00"
 */
object FederalPatterns {

    /**
     * Returns the bank configuration for Federal Bank.
     *
     * Includes all known sender IDs used by Federal Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Federal Bank",
            displayName = "Federal Bank",
            senderIds = listOf(
                "VM-FEDBNK",     // Internet Banking & General
                "AD-FEDBNK",     // Advertorial/Promotional
                "FEDBNK",        // Short code
                "FEDERALBK",     // Alternative banking code
                "FEDERAL",       // Simple short code
                "FEDUPI",        // UPI transactions
                "FEDMB",         // Mobile Banking
                "FEDCC",         // Credit Card
                "FEDATM",        // ATM transactions
                "VM-FEDCC"       // Credit Card (alternate)
            )
        )
    }

    /**
     * Returns Federal Bank-specific SMS parsing patterns.
     *
     * Federal Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal: Rs.XXX" or "Available Bal: Rs.XXX"
     * - Reference: "Ref:", "UPI Ref:", "Ref No"
     * - Merchant: "at MERCHANT" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Federal Bank differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Federal Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
