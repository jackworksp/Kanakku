package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Canara Bank.
 *
 * Canara Bank is one of India's leading public sector banks and uses multiple sender IDs
 * for transaction alerts across different services (Internet Banking, Mobile Banking, Cards, etc.).
 *
 * Common Canara Bank sender IDs:
 * - VM-CANBNK: Internet Banking and general transactions
 * - AD-CANARA: Advertorial and promotional banking alerts
 * - CANBNK: Short code for general alerts
 * - CANARA: Bank name short code
 * - CANARABANK: Full bank name code
 * - CANBNKUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Rs.500.00 debited from A/c XX1234 on 03-01-26. Avl Bal Rs.10000.00. Ref No 123456789012"
 * - Credit: "Rs.1000.00 credited to A/c XX1234 on 03-01-26. Info: NEFT-SALARY. Avl Bal Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-01-26 via UPI to merchant@paytm. Ref No 123456789012"
 * - Card: "Your Canara Bank Credit Card XX1234 has been used for Rs.1500.00 at MERCHANT on 03-01-26"
 */
object CanaraPatterns {

    /**
     * Returns the bank configuration for Canara Bank.
     *
     * Includes all known sender IDs used by Canara Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Canara Bank",
            displayName = "Canara",
            senderIds = listOf(
                "VM-CANBNK",     // Internet Banking & General
                "AD-CANARA",     // Advertorial/Promotional
                "CANBNK",        // Short code
                "CANARA",        // Bank name short code
                "CANARABANK",    // Full bank name code
                "CANBNKUPI",     // UPI transactions
                "CANBNKMB",      // Mobile Banking
                "CANBNKCC",      // Credit Card
                "CANBNKATM",     // ATM transactions
                "AD-CANBNK"      // Internet Banking promotional
            )
        )
    }

    /**
     * Returns Canara Bank-specific SMS parsing patterns.
     *
     * Canara Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal Rs.XXX" or "Available Bal Rs.XXX"
     * - Reference: "Ref No", "UPI Ref No", "UTR No"
     * - Merchant: "VPA merchant@upi" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Canara Bank differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Canara Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
