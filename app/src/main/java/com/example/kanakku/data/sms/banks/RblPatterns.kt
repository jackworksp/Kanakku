package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for RBL Bank (Ratnakar Bank Limited).
 *
 * RBL Bank is a major private sector scheduled commercial bank that provides comprehensive
 * banking services across India. The bank uses multiple sender IDs for transaction alerts
 * across different services (Internet Banking, Mobile Banking, Cards, UPI, etc.).
 *
 * Common RBL Bank sender IDs:
 * - VM-RBLBNK: Internet Banking and general transactions
 * - AD-RBLBNK: Advertorial and promotional banking alerts
 * - RBLBNK: Short code for general banking alerts
 * - RBLBANK: Alternative banking alerts
 * - RBL: Simple short code
 * - RBLUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Your A/c XX1234 debited with Rs.500.00 on 03-Jan-26. Avl Bal: Rs.10000.00. Ref: 123456789012"
 * - Credit: "Your A/c XX1234 credited with Rs.1000.00 on 03-Jan-26. Info: NEFT-SALARY. Avl Bal: Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-Jan-26 via UPI to merchant@paytm. UPI Ref: 123456789012"
 * - Card: "Your RBL Bank Credit Card XX1234 used for Rs.1500.00 at MERCHANT on 03-Jan-26"
 * - ATM: "Your A/c XX1234 debited with Rs.2000.00 on 03-Jan-26 at ATM. Avl Bal: Rs.8000.00"
 */
object RblPatterns {

    /**
     * Returns the bank configuration for RBL Bank.
     *
     * Includes all known sender IDs used by RBL Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "RBL Bank",
            displayName = "RBL Bank",
            senderIds = listOf(
                "VM-RBLBNK",     // Internet Banking & General
                "AD-RBLBNK",     // Advertorial/Promotional
                "RBLBNK",        // Short code
                "RBLBANK",       // Alternative banking code
                "RBL",           // Simple short code
                "RBLUPI",        // UPI transactions
                "RBLMB",         // Mobile Banking
                "RBLCC",         // Credit Card
                "RBLATM",        // ATM transactions
                "VM-RBLCC"       // Credit Card (alternate)
            )
        )
    }

    /**
     * Returns RBL Bank-specific SMS parsing patterns.
     *
     * RBL Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal: Rs.XXX" or "Available Bal: Rs.XXX"
     * - Reference: "Ref:", "UPI Ref:", "Ref No"
     * - Merchant: "at MERCHANT" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where RBL Bank differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // RBL Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
