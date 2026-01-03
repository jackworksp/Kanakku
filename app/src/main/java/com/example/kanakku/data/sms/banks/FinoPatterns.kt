package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Fino Payments Bank.
 *
 * Fino Payments Bank is a payment bank that provides basic savings and deposit products,
 * remittance services, money transfer, and digital payment services. It focuses on
 * financial inclusion and serves customers primarily through its extensive network of
 * banking points and digital channels.
 *
 * Common Fino sender IDs:
 * - VM-FINOPB: Fino Payments Bank transaction alerts (VM prefix)
 * - AD-FINOPB: Fino Payments Bank promotional/advertorial alerts
 * - FINOPB: Fino Payments Bank short code
 * - FINO: Fino general alerts
 * - FINOBANK: Fino Bank full code
 * - FINOUPI: Fino UPI transactions
 * - FINOMB: Fino Mobile Banking
 * - VM-FINO: Fino VM prefix alternative
 * - AD-FINO: Fino advertorial alternative
 * - FINOPAY: Fino payment services
 *
 * SMS Format Examples:
 * - Bank Debit: "Rs.500.00 debited from your Fino Payments Bank A/c XX1234 on 03-01-26. Avl Bal: Rs.3500.00"
 * - Bank Credit: "Rs.1000.00 credited to your Fino Payments Bank A/c XX1234 on 03-01-26. Avl Bal: Rs.4500.00"
 * - UPI Payment: "Rs.200 sent via UPI from Fino Bank to merchant@paytm on 03-01-26. Ref No: 123456789012"
 * - Money Transfer: "Rs.1500 transferred from your Fino A/c XX1234 to A/c YY5678 on 03-01-26. Ref: FINO123456"
 * - Bill Payment: "Rs.750 debited from your Fino A/c XX1234 for bill payment on 03-01-26. Avl Bal: Rs.3750.00"
 * - Cash Withdrawal: "Rs.2000 withdrawn from Fino A/c XX1234 at BC Point on 03-01-26. Avl Bal: Rs.1750.00"
 * - Remittance: "Rs.3000 received in your Fino A/c XX1234 on 03-01-26. Sender: John Doe. Ref: FINO789012"
 */
object FinoPatterns {

    /**
     * Returns the bank configuration for Fino Payments Bank.
     *
     * Includes all known sender IDs used by Fino for payment bank transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Fino Payments Bank",
            displayName = "Fino",
            senderIds = listOf(
                "VM-FINOPB",     // Fino Payments Bank transaction alerts (VM prefix)
                "AD-FINOPB",     // Fino Payments Bank advertorial
                "FINOPB",        // Fino Payments Bank short code
                "FINO",          // Fino general alerts
                "FINOBANK",      // Fino Bank full code
                "FINOUPI",       // Fino UPI transactions
                "FINOMB",        // Fino Mobile Banking
                "VM-FINO",       // Fino VM prefix alternative
                "AD-FINO",       // Fino advertorial alternative
                "FINOPAY"        // Fino payment services
            )
        )
    }

    /**
     * Returns Fino-specific SMS parsing patterns.
     *
     * Fino Payments Bank follows standard Indian banking SMS formats:
     * - Amount: "Rs.XXX" or "Rs XXX" or "INR XXX"
     * - Balance: "Avl Bal: Rs.XXX" or "Balance: Rs.XXX"
     * - Reference: "Ref No:", "Ref:", "Transaction ID:"
     * - Merchant: "to [Merchant]", "at [Merchant]", "for [Service]"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Fino differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Fino Payments Bank follows standard Indian banking SMS formats closely.
        // The generic patterns should handle most cases effectively, including:
        // - Standard "Rs.XXX" and "Rs XXX" amount formats
        // - Common balance formats (Avl Bal, Balance)
        // - Standard reference patterns (Ref No, Ref, UTR)
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
