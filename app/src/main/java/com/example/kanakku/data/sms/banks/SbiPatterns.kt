package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for State Bank of India (SBI).
 *
 * SBI is India's largest public sector bank and uses multiple sender IDs
 * for transaction alerts across different services (Internet Banking, Mobile Banking, Cards, etc.).
 *
 * Common SBI sender IDs:
 * - VM-SBIINB: Internet Banking transactions
 * - AD-SBIBNK: General banking alerts
 * - SBI: Short code for general alerts
 * - SBIINB: Internet Banking alerts
 * - SBIPSG: Payment gateway transactions
 * - VM-SBICard: Credit card transactions
 *
 * SMS Format Examples:
 * - Debit: "Rs.500.00 debited from A/c XX1234 on 03-01-26. Avl Bal Rs.10000.00. UPI Ref No 123456789012"
 * - Credit: "Rs.1000.00 credited to A/c XX1234 on 03-01-26. Avl Bal Rs.11000.00. Info: NEFT CREDIT"
 * - UPI: "Rs.250 sent from SBI A/c XX1234 to VPA merchant@upi on 03-01-26. UPI Ref No 123456789012"
 */
object SbiPatterns {

    /**
     * Returns the bank configuration for SBI.
     *
     * Includes all known sender IDs used by SBI for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "State Bank of India",
            displayName = "SBI",
            senderIds = listOf(
                "VM-SBIINB",     // Internet Banking
                "AD-SBIBNK",     // General Banking
                "SBI",           // Short code
                "SBIINB",        // Internet Banking (alternate)
                "SBIPSG",        // Payment Gateway
                "VM-SBICard",    // Credit Card
                "AD-SBicrd",     // Credit Card (alternate)
                "SBIUPI",        // UPI transactions
                "SBMSMS",        // Mobile Banking
                "VM-SBIATM"      // ATM transactions
            )
        )
    }

    /**
     * Returns SBI-specific SMS parsing patterns.
     *
     * SBI generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal Rs.XXX" or "Available Bal Rs.XXX"
     * - Reference: "UPI Ref No", "Ref No", "UTR No"
     * - Merchant: "VPA merchant@upi" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where SBI differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // SBI follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
