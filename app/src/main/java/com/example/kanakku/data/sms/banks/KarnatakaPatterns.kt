package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Karnataka Bank.
 *
 * Karnataka Bank is a major private sector bank headquartered in Mangalore, Karnataka, India,
 * and uses multiple sender IDs for transaction alerts across different services
 * (Internet Banking, Mobile Banking, Cards, UPI, etc.).
 *
 * Common Karnataka Bank sender IDs:
 * - VM-KTKBNK: Internet Banking and general transactions
 * - AD-KTKBNK: Advertorial and promotional banking alerts
 * - KTKBNK: Short code for general alerts
 * - KARBNK: Alternative banking alerts
 * - KTKBANK: Karnataka Bank alerts
 * - KTKUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Your A/c XX1234 debited with Rs.500.00 on 03-Jan-26. Avl Bal: Rs.10000.00. Ref: 123456789012"
 * - Credit: "Your A/c XX1234 credited with Rs.1000.00 on 03-Jan-26. Info: NEFT-SALARY. Avl Bal: Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-Jan-26 via UPI to merchant@paytm. UPI Ref: 123456789012"
 * - Card: "Your Karnataka Bank Credit Card XX1234 used for Rs.1500.00 at MERCHANT on 03-Jan-26"
 * - ATM: "Your A/c XX1234 debited with Rs.2000.00 on 03-Jan-26 at ATM. Avl Bal: Rs.8000.00"
 */
object KarnatakaPatterns {

    /**
     * Returns the bank configuration for Karnataka Bank.
     *
     * Includes all known sender IDs used by Karnataka Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Karnataka Bank",
            displayName = "Karnataka Bank",
            senderIds = listOf(
                "VM-KTKBNK",     // Internet Banking & General
                "AD-KTKBNK",     // Advertorial/Promotional
                "KTKBNK",        // Short code
                "KARBNK",        // Alternative banking code
                "KTKBANK",       // Karnataka Bank alerts
                "KTKUPI",        // UPI transactions
                "KTKMB",         // Mobile Banking
                "KTKCC",         // Credit Card
                "KTKATM",        // ATM transactions
                "VM-KTKCC"       // Credit Card (alternate)
            )
        )
    }

    /**
     * Returns Karnataka Bank-specific SMS parsing patterns.
     *
     * Karnataka Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal: Rs.XXX" or "Available Bal: Rs.XXX"
     * - Reference: "Ref:", "UPI Ref:", "Ref No"
     * - Merchant: "at MERCHANT" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Karnataka Bank differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Karnataka Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
