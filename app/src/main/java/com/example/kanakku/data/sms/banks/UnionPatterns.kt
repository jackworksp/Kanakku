package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Union Bank of India.
 *
 * Union Bank of India is one of India's leading public sector banks and uses multiple sender IDs
 * for transaction alerts across different services (Internet Banking, Mobile Banking, Cards, etc.).
 *
 * Common Union Bank sender IDs:
 * - VM-UBIONL: Internet Banking and general transactions
 * - AD-UBIONL: Advertorial and promotional banking alerts
 * - UBIONL: Short code for general alerts
 * - UNIONBK: Bank name short code
 * - UNIONBNK: Alternative bank code
 * - UBIUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Rs.500.00 debited from A/c XX1234 on 03-01-26. Avl Bal Rs.10000.00. Ref No 123456789012"
 * - Credit: "Rs.1000.00 credited to A/c XX1234 on 03-01-26. Info: NEFT-SALARY. Avl Bal Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-01-26 via UPI to merchant@paytm. Ref No 123456789012"
 * - Card: "Your Union Bank Credit Card XX1234 has been used for Rs.1500.00 at MERCHANT on 03-01-26"
 */
object UnionPatterns {

    /**
     * Returns the bank configuration for Union Bank of India.
     *
     * Includes all known sender IDs used by Union Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Union Bank of India",
            displayName = "Union Bank",
            senderIds = listOf(
                "VM-UBIONL",     // Internet Banking & General
                "AD-UBIONL",     // Advertorial/Promotional
                "UBIONL",        // Short code
                "UNIONBK",       // Bank name short code
                "UNIONBNK",      // Alternative bank code
                "UBIUPI",        // UPI transactions
                "UBIMB",         // Mobile Banking
                "UBICC",         // Credit Card
                "UBIATM",        // ATM transactions
                "AD-UNIONB"      // Internet Banking promotional
            )
        )
    }

    /**
     * Returns Union Bank-specific SMS parsing patterns.
     *
     * Union Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal Rs.XXX" or "Available Bal Rs.XXX"
     * - Reference: "Ref No", "UPI Ref No", "UTR No"
     * - Merchant: "VPA merchant@upi" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Union Bank differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Union Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
