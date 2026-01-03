package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Axis Bank.
 *
 * Axis Bank is one of India's largest private sector banks and uses multiple sender IDs
 * for transaction alerts across different services (Internet Banking, Mobile Banking, Cards, etc.).
 *
 * Common Axis Bank sender IDs:
 * - VM-AXISBK: Internet Banking and general transactions
 * - AD-AXISBK: Advertorial and promotional banking alerts
 * - AXISBK: Short code for general alerts
 * - AxisBank: Alternative general banking alerts
 * - AXISBNK: Alternate short code
 * - AXISCRD: Credit card transactions
 *
 * SMS Format Examples:
 * - Debit: "Rs 500.00 debited from A/c XX1234 on 03-Jan-26. Avl Bal: Rs 10000.00. UPI Ref No 123456789012"
 * - Credit: "Rs 1000.00 credited to A/c XX1234 on 03-Jan-26. Info: NEFT-SALARY. Avl Bal: Rs 11000.00"
 * - UPI: "Rs 250 debited from A/c XX1234 on 03-Jan-26 to VPA merchant@paytm (UPI). Ref No 123456789012"
 * - Card: "Your Axis Bank Credit Card XX1234 has been used for Rs 1500.00 at AMAZON on 03-Jan-26"
 */
object AxisPatterns {

    /**
     * Returns the bank configuration for Axis Bank.
     *
     * Includes all known sender IDs used by Axis Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Axis Bank",
            displayName = "Axis",
            senderIds = listOf(
                "VM-AXISBK",     // Internet Banking & General
                "AD-AXISBK",     // Advertorial/Promotional
                "AXISBK",        // Short code
                "AxisBank",      // Alternative general code
                "AXISBNK",       // Alternate short code
                "AXISCRD",       // Credit Card
                "AXISUPI",       // UPI transactions
                "VM-AXISCB",     // Credit Card (alternate)
                "AD-AXISCB",     // Credit Card promotional
                "AXISMB"         // Mobile Banking
            )
        )
    }

    /**
     * Returns Axis Bank-specific SMS parsing patterns.
     *
     * Axis Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal: Rs XXX" or "Available Balance Rs XXX"
     * - Reference: "UPI Ref No", "Ref No", "UTR No"
     * - Merchant: "VPA merchant@upi" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Axis differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Axis Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
