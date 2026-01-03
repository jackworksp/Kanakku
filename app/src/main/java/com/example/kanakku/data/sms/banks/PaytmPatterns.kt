package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Paytm Payments Bank.
 *
 * Paytm Payments Bank is a mobile-first payment bank that operates both a banking service
 * and the popular Paytm wallet. It uses multiple sender IDs for transaction alerts across
 * bank transactions, wallet transactions, UPI payments, and merchant transactions.
 *
 * Common Paytm sender IDs:
 * - VM-PAYTMB: Paytm Payments Bank transactions
 * - AD-PYTMWL: Paytm Wallet transactions and promotional alerts
 * - PAYTMB: Short code for bank transactions
 * - PAYTM: General Paytm alerts (wallet/bank)
 * - PaytmB: Alternative bank code
 * - PYTMWL: Paytm Wallet short code
 *
 * SMS Format Examples:
 * - Bank Debit: "Rs.500.00 debited from Paytm Payments Bank A/c XX1234 on 03-01-26. Avl Bal: Rs.10000.00"
 * - Bank Credit: "Rs.1000.00 credited to your Paytm Payments Bank A/c XX1234 on 03-01-26. Avl Bal: Rs.11000.00"
 * - Wallet Add Money: "Rs.500 added to your Paytm Wallet on 03-01-26. Balance: Rs.1500"
 * - Wallet Payment: "Rs.250 paid from your Paytm Wallet to Merchant Name on 03-01-26. Wallet Bal: Rs.1250"
 * - UPI Payment: "Rs.300 sent via UPI from Paytm to merchant@paytm on 03-01-26. Ref No: 123456789012"
 * - Merchant Payment: "You paid Rs.199 to Amazon via Paytm on 03-01-26. Txn ID: T123456789"
 */
object PaytmPatterns {

    /**
     * Returns the bank configuration for Paytm Payments Bank.
     *
     * Includes all known sender IDs used by Paytm for both bank and wallet transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Paytm Payments Bank",
            displayName = "Paytm",
            senderIds = listOf(
                "VM-PAYTMB",     // Paytm Payments Bank
                "AD-PYTMWL",     // Paytm Wallet advertorial
                "PAYTMB",        // Bank short code
                "PAYTM",         // General Paytm
                "PaytmB",        // Bank alternative
                "Paytm",         // Alternative general code
                "PYTMWL",        // Wallet short code
                "VM-PAYTM",      // Paytm general (VM prefix)
                "AD-PAYTMB",     // Bank advertorial
                "PAYTMUPI"       // UPI transactions
            )
        )
    }

    /**
     * Returns Paytm-specific SMS parsing patterns.
     *
     * Paytm has some unique formats due to its dual nature as both a bank and wallet:
     * - Amount: "Rs.XXX" or "Rs XXX" or "INR XXX"
     * - Balance: "Avl Bal: Rs.XXX" (bank) or "Wallet Bal: Rs.XXX" or "Balance: Rs.XXX" (wallet)
     * - Reference: "Ref No:", "Txn ID:", "Transaction ID:"
     * - Merchant: "to [Merchant Name]", "at [Merchant Name]", "paid to [Merchant]"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Paytm differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Paytm follows standard Indian banking and wallet SMS formats closely.
        // The generic patterns should handle most cases effectively, including:
        // - Standard "Rs.XXX" and "Rs XXX" amount formats
        // - Common balance formats (Avl Bal, Balance, Wallet Bal)
        // - Standard reference patterns (Ref No, Txn ID, UTR)
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
