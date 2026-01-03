package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Punjab National Bank (PNB).
 *
 * Punjab National Bank is one of India's leading public sector banks and uses multiple sender IDs
 * for transaction alerts across different services (Internet Banking, Mobile Banking, Cards, etc.).
 *
 * Common PNB sender IDs:
 * - VM-PNBSMS: Internet Banking and general transactions
 * - AD-PNBANK: Advertorial and promotional banking alerts
 * - PNBSMS: Short code for general alerts
 * - PNBANK: Alternative short code
 * - PNB: Simple short code
 * - PNBUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Rs.500.00 debited from A/c XX1234 on 03-01-26. Avl Bal Rs.10000.00. Ref No 123456789012"
 * - Credit: "Rs.1000.00 credited to A/c XX1234 on 03-01-26. Info: NEFT-SALARY. Avl Bal Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-01-26 via UPI to merchant@paytm. Ref No 123456789012"
 * - Card: "Your PNB Credit Card XX1234 has been used for Rs.1500.00 at MERCHANT on 03-01-26"
 */
object PnbPatterns {

    /**
     * Returns the bank configuration for Punjab National Bank.
     *
     * Includes all known sender IDs used by PNB for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Punjab National Bank",
            displayName = "PNB",
            senderIds = listOf(
                "VM-PNBSMS",     // Internet Banking & General
                "AD-PNBANK",     // Advertorial/Promotional
                "PNBSMS",        // Short code
                "PNBANK",        // Alternative short code
                "PNB",           // Simple short code
                "PNBUPI",        // UPI transactions
                "PNBMB",         // Mobile Banking
                "PNBCC",         // Credit Card
                "PNBATM",        // ATM transactions
                "AD-PNBCRD"      // Credit Card promotional
            )
        )
    }

    /**
     * Returns PNB-specific SMS parsing patterns.
     *
     * PNB generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal Rs.XXX" or "Available Bal Rs.XXX"
     * - Reference: "Ref No", "UPI Ref No", "UTR No"
     * - Merchant: "VPA merchant@upi" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where PNB differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Punjab National Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
