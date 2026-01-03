package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Yes Bank.
 *
 * Yes Bank is a major private sector bank in India and uses multiple sender IDs
 * for transaction alerts across different services (Internet Banking, Mobile Banking,
 * Cards, UPI, etc.).
 *
 * Common Yes Bank sender IDs:
 * - VM-YESBNK: Internet Banking and general transactions
 * - AD-YESBNK: Advertorial and promotional banking alerts
 * - YESBNK: Short code for general alerts
 * - YESBANK: Alternative banking alerts
 * - YES: Simple short code
 * - YESUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Your A/c XX1234 debited with Rs.500.00 on 03-Jan-26. Avl Bal: Rs.10000.00. Txn Ref: 123456789012"
 * - Credit: "Your A/c XX1234 credited with Rs.1000.00 on 03-Jan-26. Info: NEFT-SALARY. Avl Bal: Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-Jan-26 via UPI to merchant@paytm. UPI Ref: 123456789012"
 * - Card: "Your Yes Bank Credit Card XX1234 used for Rs.1500.00 at MERCHANT on 03-Jan-26"
 * - ATM: "Your A/c XX1234 debited with Rs.2000.00 on 03-Jan-26 at ATM. Avl Bal: Rs.8000.00"
 */
object YesPatterns {

    /**
     * Returns the bank configuration for Yes Bank.
     *
     * Includes all known sender IDs used by Yes Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Yes Bank",
            displayName = "Yes Bank",
            senderIds = listOf(
                "VM-YESBNK",     // Internet Banking & General
                "AD-YESBNK",     // Advertorial/Promotional
                "YESBNK",        // Short code
                "YESBANK",       // Alternative banking code
                "YES",           // Simple short code
                "YESUPI",        // UPI transactions
                "YESMB",         // Mobile Banking
                "YESCC",         // Credit Card
                "YESATM",        // ATM transactions
                "VM-YESCC"       // Credit Card (alternate)
            )
        )
    }

    /**
     * Returns Yes Bank-specific SMS parsing patterns.
     *
     * Yes Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal: Rs.XXX" or "Available Bal: Rs.XXX"
     * - Reference: "Txn Ref:", "UPI Ref:", "Ref No"
     * - Merchant: "at MERCHANT" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Yes Bank differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Yes Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
