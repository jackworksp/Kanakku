package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for HDFC Bank.
 *
 * HDFC Bank is one of India's largest private sector banks and uses multiple sender IDs
 * for transaction alerts across different services (Internet Banking, Mobile Banking, Cards, etc.).
 *
 * Common HDFC sender IDs:
 * - VM-HDFCBK: Internet Banking and general transactions
 * - AD-HDFCBK: Advertorial and promotional banking alerts
 * - HDFCBK: Short code for general alerts
 * - HDFCBank: Alternative general banking alerts
 * - HDFCCC: Credit card transactions
 * - HDFC: Simple short code
 *
 * SMS Format Examples:
 * - Debit: "Rs 500.00 debited from A/c **1234 on 03-01-26. Avl Bal: Rs 10000.00. UPI Ref No 123456789012"
 * - Credit: "Rs 1000.00 credited to A/c **1234 on 03-01-26. Info: NEFT-SALARY. Avl Bal: Rs 11000.00"
 * - UPI: "Rs 250 debited from A/c **1234 on 03-01-26 to VPA merchant@paytm (UPI). Info: Payment to Merchant"
 * - Card: "Your HDFC Bank Credit Card XX1234 has been used for Rs 1500.00 at AMAZON on 03-01-26"
 */
object HdfcPatterns {

    /**
     * Returns the bank configuration for HDFC Bank.
     *
     * Includes all known sender IDs used by HDFC Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "HDFC Bank",
            displayName = "HDFC",
            senderIds = listOf(
                "VM-HDFCBK",     // Internet Banking & General
                "AD-HDFCBK",     // Advertorial/Promotional
                "HDFCBK",        // Short code
                "HDFCBank",      // Alternative general code
                "HDFCCC",        // Credit Card
                "HDFC",          // Simple short code
                "HDFCUPI",       // UPI transactions
                "VM-HDFCCC",     // Credit Card (alternate)
                "AD-HDFCCC",     // Credit Card promotional
                "HDFCMB"         // Mobile Banking
            )
        )
    }

    /**
     * Returns HDFC Bank-specific SMS parsing patterns.
     *
     * HDFC Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal: Rs XXX" or "Available Balance Rs XXX"
     * - Reference: "UPI Ref No", "Ref No", "UTR No"
     * - Merchant: "VPA merchant@upi" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where HDFC differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // HDFC Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
