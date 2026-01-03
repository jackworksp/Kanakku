package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Fi Money.
 *
 * Fi Money is a digital neo-bank powered by Federal Bank that offers a mobile-first
 * banking experience with intelligent features. It provides UPI payments, savings accounts,
 * debit cards, and automated money management tools. Fi sends transaction alerts through
 * multiple sender IDs for different types of transactions.
 *
 * Common Fi Money sender IDs:
 * - FIMONEY: Main sender ID for Fi Money transactions
 * - VM-FIBNK: Fi Bank transaction alerts (VM prefix)
 * - AD-FIBNK: Fi promotional/advertorial alerts
 * - FI: Short code for Fi Money
 * - FIUPI: Fi UPI transactions
 * - FIMB: Fi Mobile Banking
 * - FICARD: Fi Debit Card transactions
 * - VM-FIMONY: Fi Money VM prefix alternative
 * - AD-FIMONY: Fi advertorial alternative
 * - FIPAY: Fi payment services
 *
 * SMS Format Examples:
 * - UPI Payment: "Rs.750 debited from your Fi A/c XX5678 via UPI to merchant@paytm on 03-01-26. Avl Bal: Rs.5250"
 * - Card Payment: "Rs.1500 spent on Fi Card XX5678 at Flipkart on 03-01-26. Avl Bal: Rs.3750. Ref: FI123456"
 * - UPI Credit: "Rs.3000 received in your Fi A/c XX5678 via UPI from alice@upi on 03-01-26. Avl Bal: Rs.6750"
 * - Card Withdrawal: "Rs.2000 withdrawn from Fi Card XX5678 at ATM on 03-01-26. Avl Bal: Rs.4750"
 * - Account Credit: "Rs.10000 credited to your Fi A/c XX5678 on 03-01-26. Avl Bal: Rs.14750"
 * - Bill Payment: "Rs.1200 debited from Fi A/c XX5678 for bill payment on 03-01-26. Ref: FI789012"
 * - Auto-debit: "Rs.999 auto-debited from Fi A/c XX5678 for Spotify subscription on 03-01-26"
 */
object FiPatterns {

    /**
     * Returns the bank configuration for Fi Money.
     *
     * Includes all known sender IDs used by Fi Money for transaction alerts across
     * UPI payments, card transactions, and account activities.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Fi Money",
            displayName = "Fi",
            senderIds = listOf(
                "FIMONEY",       // Main Fi Money sender ID
                "VM-FIBNK",      // Fi Bank transaction alerts (VM prefix)
                "AD-FIBNK",      // Fi promotional/advertorial alerts
                "FI",            // Short code for Fi Money
                "FIUPI",         // Fi UPI transactions
                "FIMB",          // Fi Mobile Banking
                "FICARD",        // Fi Debit Card transactions
                "VM-FIMONY",     // Fi Money VM prefix alternative
                "AD-FIMONY",     // Fi advertorial alternative
                "FIPAY"          // Fi payment services
            )
        )
    }

    /**
     * Returns Fi Money-specific SMS parsing patterns.
     *
     * Fi Money follows standard Indian digital banking SMS formats:
     * - Amount: "Rs.XXX" or "Rs XXX" or "INR XXX"
     * - Balance: "Avl Bal: Rs.XXX" or "Balance: Rs.XXX"
     * - Reference: "Ref:", "Ref No:", "Transaction ID:"
     * - Merchant: "to [Merchant]", "at [Merchant]", "via UPI to [UPI ID]"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Fi Money differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Fi Money follows standard Indian digital banking SMS formats closely.
        // The generic patterns should handle most cases effectively, including:
        // - Standard "Rs.XXX" and "Rs XXX" amount formats
        // - Common balance formats (Avl Bal, Balance)
        // - Standard reference patterns (Ref, Ref No, UTR)
        // - UPI and card transaction formats
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
