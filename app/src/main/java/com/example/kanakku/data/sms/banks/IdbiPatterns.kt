package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for IDBI Bank.
 *
 * IDBI Bank (Industrial Development Bank of India) is a major public sector bank
 * and uses multiple sender IDs for transaction alerts across different services
 * (Internet Banking, Mobile Banking, Cards, etc.).
 *
 * Common IDBI Bank sender IDs:
 * - VM-IDBIBK: Internet Banking and general transactions
 * - AD-IDBIBK: Advertorial and promotional banking alerts
 * - IDBIBK: Short code for general alerts
 * - IDBIBNK: Alternative banking alerts
 * - IDBI: Simple short code
 * - IDBIUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Rs.500.00 debited from A/c XX1234 on 03-01-26. Avl Bal Rs.10000.00. Ref No 123456789012"
 * - Credit: "Rs.1000.00 credited to A/c XX1234 on 03-01-26. Info: NEFT-SALARY. Avl Bal Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-01-26 via UPI to merchant@paytm. UPI Ref No 123456789012"
 * - Card: "Your IDBI Bank Credit Card XX1234 has been used for Rs.1500.00 at MERCHANT on 03-01-26"
 */
object IdbiPatterns {

    /**
     * Returns the bank configuration for IDBI Bank.
     *
     * Includes all known sender IDs used by IDBI Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "IDBI Bank",
            displayName = "IDBI",
            senderIds = listOf(
                "VM-IDBIBK",     // Internet Banking & General
                "AD-IDBIBK",     // Advertorial/Promotional
                "IDBIBK",        // Short code
                "IDBIBNK",       // Alternative banking code
                "IDBI",          // Simple short code
                "IDBIUPI",       // UPI transactions
                "IDBIMB",        // Mobile Banking
                "IDBICC",        // Credit Card
                "IDBIATM",       // ATM transactions
                "VM-IDBICC"      // Credit Card (alternate)
            )
        )
    }

    /**
     * Returns IDBI Bank-specific SMS parsing patterns.
     *
     * IDBI Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal Rs.XXX" or "Available Bal Rs.XXX"
     * - Reference: "Ref No", "UPI Ref No", "UTR No"
     * - Merchant: "VPA merchant@upi" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where IDBI Bank differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // IDBI Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
