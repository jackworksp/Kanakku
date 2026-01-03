package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Jio Payments Bank.
 *
 * Jio Payments Bank is operated by Reliance Jio in partnership with State Bank of India.
 * It provides basic savings accounts, digital payment services, and is integrated with
 * Jio's ecosystem of mobile and digital services including JioMoney wallet.
 *
 * Common Jio sender IDs:
 * - VM-JIOMNY: Jio Money/Bank transaction alerts (VM prefix)
 * - AD-JIOMNY: Jio Money promotional/advertorial alerts
 * - JIOMNY: Jio Money short code
 * - JIOPAY: Jio Payments general
 * - JioMoney: Jio Money alternative code
 * - JIOUPI: Jio UPI transactions
 * - JIOMB: Jio Mobile Banking
 * - JIOBK: Jio Bank short code
 *
 * SMS Format Examples:
 * - Bank Debit: "Rs.500.00 debited from your Jio Payments Bank A/c XX1234 on 03-01-26. Avl Bal: Rs.8000.00"
 * - Bank Credit: "Rs.2000.00 credited to your Jio Payments Bank A/c XX1234 on 03-01-26. Avl Bal: Rs.10000.00"
 * - UPI Payment: "Rs.350 sent via UPI from JioMoney to merchant@paytm on 03-01-26. Ref No: 123456789012"
 * - Wallet Payment: "Rs.199 paid from your JioMoney wallet to Merchant Name on 03-01-26. Wallet Bal: Rs.801"
 * - Mobile Recharge: "Rs.299 debited from Jio Bank A/c XX1234 for Jio recharge 9876543210 on 03-01-26"
 * - Bill Payment: "Rs.1200 debited from your Jio Payments Bank A/c XX1234 for bill payment on 03-01-26. Ref: JIO123456"
 * - ATM Withdrawal: "Rs.3000 withdrawn from Jio Bank A/c XX1234 at ATM on 03-01-26. Avl Bal: Rs.7000.00"
 */
object JioPatterns {

    /**
     * Returns the bank configuration for Jio Payments Bank.
     *
     * Includes all known sender IDs used by Jio for payment bank and wallet transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Jio Payments Bank",
            displayName = "Jio",
            senderIds = listOf(
                "VM-JIOMNY",     // Jio Money/Bank (VM prefix)
                "AD-JIOMNY",     // Jio Money advertorial
                "JIOMNY",        // Jio Money short code
                "JIOPAY",        // Jio Payments
                "JioMoney",      // Jio Money alternative
                "JIOUPI",        // Jio UPI transactions
                "JIOMB",         // Jio Mobile Banking
                "VM-JIOPAY",     // Jio Pay VM prefix
                "AD-JIOPAY",     // Jio Pay advertorial
                "JIOBK"          // Jio Bank short code
            )
        )
    }

    /**
     * Returns Jio-specific SMS parsing patterns.
     *
     * Jio Payments Bank follows standard Indian banking SMS formats:
     * - Amount: "Rs.XXX" or "Rs XXX" or "INR XXX"
     * - Balance: "Avl Bal: Rs.XXX" or "Balance: Rs.XXX" or "Wallet Bal: Rs.XXX"
     * - Reference: "Ref No:", "Ref:", "Transaction ID:"
     * - Merchant: "to [Merchant]", "at [Merchant]", "for [Service]"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Jio differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Jio Payments Bank follows standard Indian banking and wallet SMS formats closely.
        // The generic patterns should handle most cases effectively, including:
        // - Standard "Rs.XXX" and "Rs XXX" amount formats
        // - Common balance formats (Avl Bal, Balance, Wallet Bal)
        // - Standard reference patterns (Ref No, Ref, UTR)
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
