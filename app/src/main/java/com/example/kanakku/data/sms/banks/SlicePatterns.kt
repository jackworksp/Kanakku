package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Slice.
 *
 * Slice is a digital credit card service that offers a UPI-linked credit line and Visa credit card
 * to young Indians. It provides credit card payments, EMI options, rewards, and cashback features.
 * Slice is powered by various partner banks and sends transaction alerts through multiple sender IDs
 * for different types of transactions.
 *
 * Common Slice sender IDs:
 * - SLICE: Main sender ID for Slice transactions
 * - VM-SLICE: Slice transaction alerts (VM prefix)
 * - AD-SLICE: Slice promotional/advertorial alerts
 * - SLICECC: Slice Credit Card transactions
 * - SLICEPAY: Slice payment services
 * - SLICECARD: Slice Card transaction alerts
 * - VM-SLICEC: Slice VM prefix alternative
 * - AD-SLICEC: Slice advertorial alternative
 * - SLICEUPI: Slice UPI-linked credit transactions
 * - SLICEMB: Slice Mobile app transactions
 *
 * SMS Format Examples:
 * - Card Purchase: "Rs.1200 spent on your Slice Card XX4567 at Amazon on 03-01-26. Avl Credit: Rs.48800"
 * - Online Transaction: "Rs.2500 debited from Slice Card XX4567 for online transaction at Flipkart on 03-01-26. Ref: SLC123456"
 * - UPI Transaction: "Rs.800 spent via Slice UPI at merchant@paytm on 03-01-26. Avl Credit: Rs.49200"
 * - Payment Received: "Rs.5000 payment received for Slice Card XX4567 on 03-01-26. Total Due: Rs.12000"
 * - EMI Conversion: "Rs.15000 transaction converted to EMI of 3 months. EMI amount: Rs.5166/month"
 * - Cashback Credit: "Rs.120 cashback credited to your Slice Card XX4567 on 03-01-26. Avl Credit: Rs.49320"
 * - Reward Points: "100 reward points earned on Slice Card XX4567. Total Points: 1500"
 * - Credit Limit Update: "Your Slice credit limit has been increased to Rs.75000 on 03-01-26"
 * - Bill Generated: "Your Slice bill of Rs.18500 is generated. Due date: 15-01-26. Min due: Rs.1850"
 * - Auto-debit Success: "Rs.18500 auto-debited for Slice Card XX4567 payment on 03-01-26. Thank you!"
 */
object SlicePatterns {

    /**
     * Returns the bank configuration for Slice.
     *
     * Includes all known sender IDs used by Slice for transaction alerts across
     * credit card transactions, UPI payments, EMI conversions, and payment confirmations.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Slice",
            displayName = "Slice",
            senderIds = listOf(
                "SLICE",         // Main Slice sender ID
                "VM-SLICE",      // Slice transaction alerts (VM prefix)
                "AD-SLICE",      // Slice promotional/advertorial alerts
                "SLICECC",       // Slice Credit Card transactions
                "SLICEPAY",      // Slice payment services
                "SLICECARD",     // Slice Card transaction alerts
                "VM-SLICEC",     // Slice VM prefix alternative
                "AD-SLICEC",     // Slice advertorial alternative
                "SLICEUPI",      // Slice UPI-linked credit transactions
                "SLICEMB"        // Slice Mobile app transactions
            )
        )
    }

    /**
     * Returns Slice-specific SMS parsing patterns.
     *
     * Slice follows standard Indian credit card SMS formats:
     * - Amount: "Rs.XXX" or "Rs XXX" or "INR XXX"
     * - Balance: "Avl Credit: Rs.XXX" or "Available Credit: Rs.XXX"
     * - Reference: "Ref:", "Ref No:", "Transaction ID:"
     * - Merchant: "at [Merchant]", "for online transaction at [Merchant]"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Slice differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Slice follows standard Indian credit card SMS formats closely.
        // The generic patterns should handle most cases effectively, including:
        // - Standard "Rs.XXX" and "Rs XXX" amount formats
        // - Common balance/credit limit formats (Avl Credit, Available Credit)
        // - Standard reference patterns (Ref, Ref No, Transaction ID)
        // - Card transaction and UPI payment formats
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
