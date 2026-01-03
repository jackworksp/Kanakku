package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Jupiter.
 *
 * Jupiter is a digital neo-bank powered by Federal Bank that offers a mobile-first
 * banking experience. It provides UPI payments, debit cards, savings accounts, and
 * various digital banking services. Jupiter sends transaction alerts through multiple
 * sender IDs for different types of transactions.
 *
 * Common Jupiter sender IDs:
 * - JUPITER: Main sender ID for Jupiter transactions
 * - VM-JUPBK: Jupiter Bank transaction alerts (VM prefix)
 * - AD-JUPBK: Jupiter promotional/advertorial alerts
 * - JUPITERBK: Jupiter Bank full code
 * - JUPUPI: Jupiter UPI transactions
 * - JUPMB: Jupiter Mobile Banking
 * - JUPCARD: Jupiter Debit Card transactions
 * - VM-JUPTER: Jupiter VM prefix alternative
 * - AD-JUPTER: Jupiter advertorial alternative
 * - JUPITERPAY: Jupiter payment services
 *
 * SMS Format Examples:
 * - UPI Payment: "Rs.500 debited from your Jupiter A/c XX1234 via UPI to merchant@paytm on 03-01-26. Avl Bal: Rs.4500"
 * - Card Payment: "Rs.1200 spent on Jupiter Card XX1234 at Amazon on 03-01-26. Avl Bal: Rs.3300. Ref: JUP123456"
 * - UPI Credit: "Rs.2000 received in your Jupiter A/c XX1234 via UPI from john@upi on 03-01-26. Avl Bal: Rs.5300"
 * - Card Withdrawal: "Rs.3000 withdrawn from Jupiter Card XX1234 at ATM on 03-01-26. Avl Bal: Rs.2300"
 * - Account Credit: "Rs.5000 credited to your Jupiter A/c XX1234 on 03-01-26. Avl Bal: Rs.7300"
 * - Bill Payment: "Rs.850 debited from Jupiter A/c XX1234 for bill payment on 03-01-26. Ref: JUP789012"
 * - Auto-debit: "Rs.499 auto-debited from Jupiter A/c XX1234 for Netflix subscription on 03-01-26"
 */
object JupiterPatterns {

    /**
     * Returns the bank configuration for Jupiter.
     *
     * Includes all known sender IDs used by Jupiter for transaction alerts across
     * UPI payments, card transactions, and account activities.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Jupiter",
            displayName = "Jupiter",
            senderIds = listOf(
                "JUPITER",       // Main Jupiter sender ID
                "VM-JUPBK",      // Jupiter Bank transaction alerts (VM prefix)
                "AD-JUPBK",      // Jupiter promotional/advertorial alerts
                "JUPITERBK",     // Jupiter Bank full code
                "JUPUPI",        // Jupiter UPI transactions
                "JUPMB",         // Jupiter Mobile Banking
                "JUPCARD",       // Jupiter Debit Card transactions
                "VM-JUPTER",     // Jupiter VM prefix alternative
                "AD-JUPTER",     // Jupiter advertorial alternative
                "JUPITERPAY"     // Jupiter payment services
            )
        )
    }

    /**
     * Returns Jupiter-specific SMS parsing patterns.
     *
     * Jupiter follows standard Indian digital banking SMS formats:
     * - Amount: "Rs.XXX" or "Rs XXX" or "INR XXX"
     * - Balance: "Avl Bal: Rs.XXX" or "Balance: Rs.XXX"
     * - Reference: "Ref:", "Ref No:", "Transaction ID:"
     * - Merchant: "to [Merchant]", "at [Merchant]", "via UPI to [UPI ID]"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Jupiter differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Jupiter follows standard Indian digital banking SMS formats closely.
        // The generic patterns should handle most cases effectively, including:
        // - Standard "Rs.XXX" and "Rs XXX" amount formats
        // - Common balance formats (Avl Bal, Balance)
        // - Standard reference patterns (Ref, Ref No, UTR)
        // - UPI and card transaction formats
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
