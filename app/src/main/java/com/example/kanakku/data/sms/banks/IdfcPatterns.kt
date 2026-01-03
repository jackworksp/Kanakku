package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for IDFC First Bank.
 *
 * IDFC First Bank is a major private sector bank formed from the merger of IDFC Bank and
 * Capital First, and uses multiple sender IDs for transaction alerts across different services
 * (Internet Banking, Mobile Banking, Cards, UPI, etc.).
 *
 * Common IDFC First Bank sender IDs:
 * - VM-IDFCFB: Internet Banking and general transactions
 * - AD-IDFCFB: Advertorial and promotional banking alerts
 * - IDFCFB: Short code for general alerts
 * - IDFCBNK: Alternative banking alerts
 * - IDFC: Simple short code
 * - IDFCUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Your A/c XX1234 debited with Rs.500.00 on 03-Jan-26. Avl Bal: Rs.10000.00. Ref: 123456789012"
 * - Credit: "Your A/c XX1234 credited with Rs.1000.00 on 03-Jan-26. Info: NEFT-SALARY. Avl Bal: Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-Jan-26 via UPI to merchant@paytm. UPI Ref: 123456789012"
 * - Card: "Your IDFC FIRST Bank Credit Card XX1234 used for Rs.1500.00 at MERCHANT on 03-Jan-26"
 * - ATM: "Your A/c XX1234 debited with Rs.2000.00 on 03-Jan-26 at ATM. Avl Bal: Rs.8000.00"
 */
object IdfcPatterns {

    /**
     * Returns the bank configuration for IDFC First Bank.
     *
     * Includes all known sender IDs used by IDFC First Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "IDFC First Bank",
            displayName = "IDFC First",
            senderIds = listOf(
                "VM-IDFCFB",     // Internet Banking & General
                "AD-IDFCFB",     // Advertorial/Promotional
                "IDFCFB",        // Short code
                "IDFCBNK",       // Alternative banking code
                "IDFC",          // Simple short code
                "IDFCUPI",       // UPI transactions
                "IDFCMB",        // Mobile Banking
                "IDFCCC",        // Credit Card
                "IDFCATM",       // ATM transactions
                "VM-IDFCCC"      // Credit Card (alternate)
            )
        )
    }

    /**
     * Returns IDFC First Bank-specific SMS parsing patterns.
     *
     * IDFC First Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal: Rs.XXX" or "Available Bal: Rs.XXX"
     * - Reference: "Ref:", "UPI Ref:", "Ref No"
     * - Merchant: "at MERCHANT" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where IDFC First Bank differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // IDFC First Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
