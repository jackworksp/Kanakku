package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Bank of Baroda (BoB).
 *
 * Bank of Baroda is one of India's leading public sector banks and uses multiple sender IDs
 * for transaction alerts across different services (Internet Banking, Mobile Banking, Cards, etc.).
 *
 * Common Bank of Baroda sender IDs:
 * - AD-BOBANK: Advertorial and promotional banking alerts
 * - VM-BOBANK: Internet Banking and general transactions
 * - BOBANK: Short code for general alerts
 * - BOBBNK: Alternative short code
 * - BOB: Simple short code
 * - BOBUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Rs.500.00 debited from A/c XX1234 on 03-01-26. Avl Bal Rs.10000.00. UPI Ref No 123456789012"
 * - Credit: "Rs.1000.00 credited to A/c XX1234 on 03-01-26. Info: NEFT-SALARY. Avl Bal Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-01-26 to VPA merchant@paytm (UPI). Ref No 123456789012"
 * - Card: "Your BoB Credit Card XX1234 has been used for Rs.1500.00 at MERCHANT on 03-01-26"
 */
object BobPatterns {

    /**
     * Returns the bank configuration for Bank of Baroda.
     *
     * Includes all known sender IDs used by Bank of Baroda for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Bank of Baroda",
            displayName = "BoB",
            senderIds = listOf(
                "AD-BOBANK",     // Advertorial/Promotional
                "VM-BOBANK",     // Internet Banking & General
                "BOBANK",        // Short code
                "BOBBNK",        // Alternative short code
                "BOB",           // Simple short code
                "BOBUPI",        // UPI transactions
                "BOBMB",         // Mobile Banking
                "BOBCC",         // Credit Card
                "BOBATM",        // ATM transactions
                "AD-BOBCRD"      // Credit Card promotional
            )
        )
    }

    /**
     * Returns Bank of Baroda-specific SMS parsing patterns.
     *
     * Bank of Baroda generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal Rs.XXX" or "Available Bal Rs.XXX"
     * - Reference: "UPI Ref No", "Ref No", "UTR No"
     * - Merchant: "VPA merchant@upi" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where BoB differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Bank of Baroda follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
