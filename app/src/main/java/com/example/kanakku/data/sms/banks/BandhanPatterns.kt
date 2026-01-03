package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Bandhan Bank.
 *
 * Bandhan Bank is a major private sector bank headquartered in Kolkata, West Bengal, India,
 * and uses multiple sender IDs for transaction alerts across different services
 * (Internet Banking, Mobile Banking, Cards, UPI, etc.).
 *
 * Common Bandhan Bank sender IDs:
 * - VM-BANDHN: Internet Banking and general transactions
 * - AD-BANDHN: Advertorial and promotional banking alerts
 * - BANDHN: Short code for general alerts
 * - BANDHAN: Alternative banking alerts
 * - BANDHANBK: Bandhan Bank alerts
 * - BANDUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Your A/c XX1234 debited with Rs.500.00 on 03-Jan-26. Avl Bal: Rs.10000.00. Ref: 123456789012"
 * - Credit: "Your A/c XX1234 credited with Rs.1000.00 on 03-Jan-26. Info: NEFT-SALARY. Avl Bal: Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-Jan-26 via UPI to merchant@paytm. UPI Ref: 123456789012"
 * - Card: "Your Bandhan Bank Credit Card XX1234 used for Rs.1500.00 at MERCHANT on 03-Jan-26"
 * - ATM: "Your A/c XX1234 debited with Rs.2000.00 on 03-Jan-26 at ATM. Avl Bal: Rs.8000.00"
 */
object BandhanPatterns {

    /**
     * Returns the bank configuration for Bandhan Bank.
     *
     * Includes all known sender IDs used by Bandhan Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Bandhan Bank",
            displayName = "Bandhan Bank",
            senderIds = listOf(
                "VM-BANDHN",     // Internet Banking & General
                "AD-BANDHN",     // Advertorial/Promotional
                "BANDHN",        // Short code
                "BANDHAN",       // Alternative banking code
                "BANDHANBK",     // Bandhan Bank alerts
                "BANDUPI",       // UPI transactions
                "BANDMB",        // Mobile Banking
                "BANDCC",        // Credit Card
                "BANDATM",       // ATM transactions
                "VM-BANDCC"      // Credit Card (alternate)
            )
        )
    }

    /**
     * Returns Bandhan Bank-specific SMS parsing patterns.
     *
     * Bandhan Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal: Rs.XXX" or "Available Bal: Rs.XXX"
     * - Reference: "Ref:", "UPI Ref:", "Ref No"
     * - Merchant: "at MERCHANT" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Bandhan Bank differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Bandhan Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
