package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Kotak Mahindra Bank.
 *
 * Kotak Mahindra Bank is one of India's leading private sector banks and uses multiple sender IDs
 * for transaction alerts across different services (Internet Banking, Mobile Banking, Cards, etc.).
 *
 * Common Kotak Mahindra Bank sender IDs:
 * - VK-KOTAKB: Internet Banking and general transactions
 * - AD-KOTAKB: Advertorial and promotional banking alerts
 * - KOTAKB: Short code for general alerts
 * - Kotak: Alternative general banking alerts
 * - KOTAK: Simple short code
 * - KOTAKCC: Credit card transactions
 *
 * SMS Format Examples:
 * - Debit: "Rs 500.00 debited from A/c XX1234 on 03-01-26. Avl Bal: Rs 10000.00. UPI Ref No 123456789012"
 * - Credit: "Rs 1000.00 credited to A/c XX1234 on 03-01-26. Info: NEFT-SALARY. Avl Bal: Rs 11000.00"
 * - UPI: "Rs 250 debited from A/c XX1234 on 03-01-26 to VPA merchant@paytm (UPI). Ref No 123456789012"
 * - Card: "Your Kotak Bank Credit Card XX1234 has been used for Rs 1500.00 at AMAZON on 03-01-26"
 */
object KotakPatterns {

    /**
     * Returns the bank configuration for Kotak Mahindra Bank.
     *
     * Includes all known sender IDs used by Kotak Mahindra Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Kotak Mahindra Bank",
            displayName = "Kotak",
            senderIds = listOf(
                "VK-KOTAKB",     // Internet Banking & General
                "AD-KOTAKB",     // Advertorial/Promotional
                "KOTAKB",        // Short code
                "Kotak",         // Alternative general code
                "KOTAK",         // Simple short code
                "KOTAKCC",       // Credit Card
                "KOTAKUPI",      // UPI transactions
                "VK-KOTAKC",     // Credit Card (alternate)
                "AD-KOTAKC",     // Credit Card promotional
                "KOTAKMB"        // Mobile Banking
            )
        )
    }

    /**
     * Returns Kotak Mahindra Bank-specific SMS parsing patterns.
     *
     * Kotak Mahindra Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal: Rs XXX" or "Available Balance Rs XXX"
     * - Reference: "UPI Ref No", "Ref No", "UTR No"
     * - Merchant: "VPA merchant@upi" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Kotak differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Kotak Mahindra Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
