package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for OneCard.
 *
 * OneCard is a digital metal credit card service powered by Federal Bank and SBM Bank (India).
 * It offers a mobile-first credit card experience with lifetime free benefits, high credit limits,
 * instant rewards, and a comprehensive mobile app. OneCard sends transaction alerts through
 * multiple sender IDs for different types of transactions and promotional messages.
 *
 * Common OneCard sender IDs:
 * - ONECARD: Main sender ID for OneCard transactions
 * - VM-ONECD: OneCard transaction alerts (VM prefix)
 * - AD-ONECD: OneCard promotional/advertorial alerts
 * - ONECRD: OneCard short form for card transactions
 * - ONECARDCC: OneCard Credit Card specific transactions
 * - ONECARDPAY: OneCard payment services
 * - VM-ONECRD: OneCard VM prefix alternative
 * - AD-ONECRD: OneCard advertorial alternative
 * - ONECARDUPI: OneCard UPI-linked transactions
 * - ONECARDMB: OneCard Mobile app transactions
 *
 * SMS Format Examples:
 * - Card Purchase: "Rs.1500 spent on OneCard XX5678 at Swiggy on 03-01-26. Available limit: Rs.98500"
 * - Online Transaction: "Rs.3200 debited from OneCard XX5678 for online txn at Amazon on 03-01-26. Ref: OC123456"
 * - International Transaction: "USD 50 (Rs.4150) spent on OneCard XX5678 at APPLE.COM on 03-01-26"
 * - Payment Received: "Rs.8000 payment received for OneCard XX5678 on 03-01-26. Outstanding: Rs.15000"
 * - EMI Conversion: "Rs.25000 transaction converted to EMI of 6 months. EMI: Rs.4333/month"
 * - Reward Points: "150 reward points earned on OneCard XX5678. Total Points: 2500"
 * - Credit Limit Increase: "Your OneCard credit limit increased to Rs.150000 on 03-01-26"
 * - Bill Generated: "Your OneCard bill of Rs.22500 is generated. Due: 20-01-26. Min due: Rs.2250"
 * - Auto-debit Success: "Rs.22500 auto-debited for OneCard XX5678 on 03-01-26. Thank you!"
 * - Card Activation: "Your OneCard XX5678 is now active. Start enjoying lifetime free benefits!"
 */
object OneCardPatterns {

    /**
     * Returns the bank configuration for OneCard.
     *
     * Includes all known sender IDs used by OneCard for transaction alerts across
     * credit card transactions, UPI payments, EMI conversions, and payment confirmations.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "OneCard",
            displayName = "OneCard",
            senderIds = listOf(
                "ONECARD",       // Main OneCard sender ID
                "VM-ONECD",      // OneCard transaction alerts (VM prefix)
                "AD-ONECD",      // OneCard promotional/advertorial alerts
                "ONECRD",        // OneCard short form for card transactions
                "ONECARDCC",     // OneCard Credit Card specific transactions
                "ONECARDPAY",    // OneCard payment services
                "VM-ONECRD",     // OneCard VM prefix alternative
                "AD-ONECRD",     // OneCard advertorial alternative
                "ONECARDUPI",    // OneCard UPI-linked transactions
                "ONECARDMB"      // OneCard Mobile app transactions
            )
        )
    }

    /**
     * Returns OneCard-specific SMS parsing patterns.
     *
     * OneCard follows standard Indian credit card SMS formats:
     * - Amount: "Rs.XXX" or "Rs XXX" or "INR XXX" or "USD XX (Rs.XXX)"
     * - Balance: "Available limit: Rs.XXX" or "Avl limit: Rs.XXX"
     * - Reference: "Ref:", "Ref No:", "Transaction ID:"
     * - Merchant: "at [Merchant]", "for online txn at [Merchant]"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where OneCard differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // OneCard follows standard Indian credit card SMS formats closely.
        // The generic patterns should handle most cases effectively, including:
        // - Standard "Rs.XXX" and "Rs XXX" amount formats
        // - Common balance/credit limit formats (Available limit, Avl limit)
        // - Standard reference patterns (Ref, Ref No, Transaction ID)
        // - Card transaction and UPI payment formats
        // - International transactions with currency conversion
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
