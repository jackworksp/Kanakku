package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Niyo.
 *
 * Niyo is a digital banking platform that partners with banks like Equitas Small Finance Bank
 * and SBM Bank to offer digital banking services. It provides salary accounts (Niyo Equitas),
 * international travel cards (Niyo Global), and savings accounts (Niyo Bharat). Niyo sends
 * transaction alerts through multiple sender IDs for different types of transactions.
 *
 * Common Niyo sender IDs:
 * - NIYO: Main sender ID for Niyo transactions
 * - VM-NIYO: Niyo transaction alerts (VM prefix)
 * - AD-NIYO: Niyo promotional/advertorial alerts
 * - NIYOBNK: Niyo Bank full code
 * - NIYOUPI: Niyo UPI transactions
 * - NIYOMB: Niyo Mobile Banking
 * - NIYOCARD: Niyo Debit/Travel Card transactions
 * - NIYOPAY: Niyo payment services
 * - VM-NIYOGL: Niyo Global travel card (VM prefix)
 * - NIYOEQ: Niyo Equitas salary account
 *
 * SMS Format Examples:
 * - UPI Payment: "Rs.600 debited from your Niyo A/c XX9876 via UPI to merchant@paytm on 03-01-26. Avl Bal: Rs.4400"
 * - Card Payment: "Rs.2500 spent on Niyo Card XX9876 at Swiggy on 03-01-26. Avl Bal: Rs.1900. Ref: NYO123456"
 * - UPI Credit: "Rs.25000 received in your Niyo A/c XX9876 via UPI from employer@upi on 03-01-26. Avl Bal: Rs.26900"
 * - Card Withdrawal: "Rs.5000 withdrawn from Niyo Card XX9876 at ATM on 03-01-26. Avl Bal: Rs.21900"
 * - Account Credit: "Rs.30000 credited to your Niyo A/c XX9876 on 03-01-26. Avl Bal: Rs.51900. Salary credited"
 * - Bill Payment: "Rs.1500 debited from Niyo A/c XX9876 for bill payment on 03-01-26. Ref: NYO789012"
 * - Auto-debit: "Rs.299 auto-debited from Niyo A/c XX9876 for subscription on 03-01-26"
 * - Travel Card: "USD 50 spent on Niyo Global Card XX9876 at merchant abroad on 03-01-26. Ref: NYO456789"
 */
object NiyoPatterns {

    /**
     * Returns the bank configuration for Niyo.
     *
     * Includes all known sender IDs used by Niyo for transaction alerts across
     * UPI payments, card transactions, account activities, and travel card usage.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Niyo",
            displayName = "Niyo",
            senderIds = listOf(
                "NIYO",          // Main Niyo sender ID
                "VM-NIYO",       // Niyo transaction alerts (VM prefix)
                "AD-NIYO",       // Niyo promotional/advertorial alerts
                "NIYOBNK",       // Niyo Bank full code
                "NIYOUPI",       // Niyo UPI transactions
                "NIYOMB",        // Niyo Mobile Banking
                "NIYOCARD",      // Niyo Debit/Travel Card transactions
                "NIYOPAY",       // Niyo payment services
                "VM-NIYOGL",     // Niyo Global travel card (VM prefix)
                "NIYOEQ"         // Niyo Equitas salary account
            )
        )
    }

    /**
     * Returns Niyo-specific SMS parsing patterns.
     *
     * Niyo follows standard Indian digital banking SMS formats:
     * - Amount: "Rs.XXX" or "Rs XXX" or "INR XXX" or "USD XXX" (for travel card)
     * - Balance: "Avl Bal: Rs.XXX" or "Balance: Rs.XXX"
     * - Reference: "Ref:", "Ref No:", "Transaction ID:"
     * - Merchant: "to [Merchant]", "at [Merchant]", "via UPI to [UPI ID]"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Niyo differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Niyo follows standard Indian digital banking SMS formats closely.
        // The generic patterns should handle most cases effectively, including:
        // - Standard "Rs.XXX" and "Rs XXX" amount formats
        // - Common balance formats (Avl Bal, Balance)
        // - Standard reference patterns (Ref, Ref No, UTR)
        // - UPI and card transaction formats
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
