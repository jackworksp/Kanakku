package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for CRED.
 *
 * CRED is a members-only credit card bill payment platform that rewards users for paying
 * their credit card bills on time. While not a bank itself, CRED sends payment confirmation
 * SMS when users pay their credit card bills through the platform. These payment confirmations
 * represent debits from the user's linked bank account to their credit card.
 *
 * Common CRED sender IDs:
 * - CRED: Main sender ID for CRED payment confirmations
 * - VM-CRED: CRED payment alerts (VM prefix)
 * - AD-CRED: CRED promotional/advertorial alerts
 * - CREDPAY: CRED payment services
 * - CREDAPP: CRED app transaction alerts
 * - VM-CREDP: CRED VM prefix alternative
 * - AD-CREDP: CRED advertorial alternative
 * - CREDCLUB: CRED Club member benefits
 * - CREDMINT: CRED Mint payments
 * - CREDPMT: CRED payment transactions
 *
 * SMS Format Examples:
 * - Payment Confirmation: "Rs.15000 paid to HDFC Credit Card XX5678 via CRED on 03-01-26. Ref: CRED123456"
 * - Bill Payment: "Your ICICI Credit Card bill of Rs.22500 paid successfully through CRED on 03-01-26"
 * - Reward Points: "150 CRED coins earned for paying Rs.15000 bill on time. Total coins: 2500"
 * - Multiple Card Payment: "Rs.8000 paid to 2 credit cards via CRED on 03-01-26. Total: Rs.8000"
 * - Cashback Credit: "Rs.200 cashback credited for CRED payment of Rs.15000 on 03-01-26"
 * - Auto-pay Success: "Auto-pay of Rs.12500 to SBI Credit Card XX4567 via CRED on 03-01-26"
 * - Payment Scheduled: "Your CRED payment of Rs.18000 scheduled for 15-01-26 for Axis Credit Card"
 * - Payment Failed: "CRED payment of Rs.15000 to HDFC Card failed on 03-01-26. Please retry"
 */
object CredPatterns {

    /**
     * Returns the bank configuration for CRED.
     *
     * Includes all known sender IDs used by CRED for payment confirmation alerts across
     * credit card bill payments, reward notifications, and cashback credits.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "CRED",
            displayName = "CRED",
            senderIds = listOf(
                "CRED",          // Main CRED sender ID
                "VM-CRED",       // CRED payment alerts (VM prefix)
                "AD-CRED",       // CRED promotional/advertorial alerts
                "CREDPAY",       // CRED payment services
                "CREDAPP",       // CRED app transaction alerts
                "VM-CREDP",      // CRED VM prefix alternative
                "AD-CREDP",      // CRED advertorial alternative
                "CREDCLUB",      // CRED Club member benefits
                "CREDMINT",      // CRED Mint payments
                "CREDPMT"        // CRED payment transactions
            )
        )
    }

    /**
     * Returns CRED-specific SMS parsing patterns.
     *
     * CRED follows standard Indian payment SMS formats:
     * - Amount: "Rs.XXX" or "Rs XXX" or "INR XXX"
     * - Reference: "Ref:", "Ref No:", "Transaction ID:"
     * - Card Details: "to [Bank] Credit Card XXXX", "for [Bank] Card"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where CRED differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // CRED follows standard Indian payment SMS formats closely.
        // The generic patterns should handle most cases effectively, including:
        // - Standard "Rs.XXX" and "Rs XXX" amount formats
        // - Standard reference patterns (Ref, Ref No, Transaction ID)
        // - Payment confirmation formats
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
