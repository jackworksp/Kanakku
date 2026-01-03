package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for India Post Payments Bank (IPPB).
 *
 * India Post Payments Bank is a public sector payment bank under the Department of Posts,
 * Ministry of Communications. It provides basic savings and deposit products, remittance
 * services, and digital payment services through India's vast postal network.
 *
 * Common IPPB sender IDs:
 * - VM-IPPBSM: India Post Payments Bank transaction alerts (VM prefix)
 * - AD-IPPBSM: IPPB promotional/advertorial alerts
 * - IPPBSM: IPPB short code
 * - IPPB: India Post Payments Bank general
 * - POSTBK: Post Bank alternative code
 * - IPPBUPI: IPPB UPI transactions
 * - IPPBMB: IPPB Mobile Banking
 * - INDIAPOST: India Post general alerts
 * - POSTBANK: Post Bank full code
 * - VM-IPPB: IPPB VM prefix alternative
 *
 * SMS Format Examples:
 * - Bank Debit: "Rs.500.00 debited from your IPPB A/c XX1234 on 03-01-26. Avl Bal: Rs.7500.00"
 * - Bank Credit: "Rs.1500.00 credited to your IPPB A/c XX1234 on 03-01-26. Avl Bal: Rs.9000.00"
 * - UPI Payment: "Rs.300 sent via UPI from IPPB to merchant@paytm on 03-01-26. Ref No: 123456789012"
 * - Remittance: "Rs.2000 received in your IPPB A/c XX1234 on 03-01-26. Sender: John Doe. Ref: IP123456"
 * - Bill Payment: "Rs.800 debited from your IPPB A/c XX1234 for bill payment on 03-01-26. Ref: IPPB123456"
 * - ATM Withdrawal: "Rs.3000 withdrawn from IPPB A/c XX1234 at ATM on 03-01-26. Avl Bal: Rs.6000.00"
 * - Door Banking: "Rs.1000 deposited to your IPPB A/c XX1234 via Door Banking on 03-01-26. Avl Bal: Rs.10000.00"
 */
object IppbPatterns {

    /**
     * Returns the bank configuration for India Post Payments Bank.
     *
     * Includes all known sender IDs used by IPPB for payment bank transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "India Post Payments Bank",
            displayName = "IPPB",
            senderIds = listOf(
                "VM-IPPBSM",     // IPPB transaction alerts (VM prefix)
                "AD-IPPBSM",     // IPPB advertorial
                "IPPBSM",        // IPPB short code
                "IPPB",          // IPPB general alerts
                "POSTBK",        // Post Bank alternative
                "IPPBUPI",       // IPPB UPI transactions
                "IPPBMB",        // IPPB Mobile Banking
                "INDIAPOST",     // India Post general
                "POSTBANK",      // Post Bank full code
                "VM-IPPB"        // IPPB VM prefix alternative
            )
        )
    }

    /**
     * Returns IPPB-specific SMS parsing patterns.
     *
     * India Post Payments Bank follows standard Indian banking SMS formats:
     * - Amount: "Rs.XXX" or "Rs XXX" or "INR XXX"
     * - Balance: "Avl Bal: Rs.XXX" or "Balance: Rs.XXX"
     * - Reference: "Ref No:", "Ref:", "Transaction ID:"
     * - Merchant: "to [Merchant]", "at [Merchant]", "for [Service]"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where IPPB differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // India Post Payments Bank follows standard Indian banking SMS formats closely.
        // The generic patterns should handle most cases effectively, including:
        // - Standard "Rs.XXX" and "Rs XXX" amount formats
        // - Common balance formats (Avl Bal, Balance)
        // - Standard reference patterns (Ref No, Ref, UTR)
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
