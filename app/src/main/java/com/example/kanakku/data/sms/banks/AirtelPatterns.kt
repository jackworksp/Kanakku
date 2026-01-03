package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for Airtel Payments Bank.
 *
 * Airtel Payments Bank is a payment bank operated by Bharti Airtel in partnership with
 * Kotak Mahindra Bank. It provides basic savings and deposit products along with digital
 * payment services integrated with Airtel's mobile network services.
 *
 * Common Airtel sender IDs:
 * - VM-AIRTEL: Airtel general transaction alerts
 * - AD-AIRTPB: Airtel Payments Bank promotional/advertorial alerts
 * - AIRTPB: Airtel Payments Bank short code
 * - AIRTEL: General Airtel alerts (bank/mobile)
 * - AIRTELB: Airtel Bank alternative code
 * - AIRTELPB: Airtel Payments Bank full code
 *
 * SMS Format Examples:
 * - Bank Debit: "Rs.500.00 debited from your Airtel Payments Bank A/c XX1234 on 03-01-26. Avl Bal: Rs.5000.00"
 * - Bank Credit: "Rs.1000.00 credited to your Airtel Payments Bank A/c XX1234 on 03-01-26. Avl Bal: Rs.6000.00"
 * - UPI Payment: "Rs.250 sent via UPI from Airtel Bank to merchant@paytm on 03-01-26. Ref No: 123456789012"
 * - Mobile Recharge: "Rs.199 debited from Airtel Bank A/c XX1234 for mobile recharge 9876543210 on 03-01-26"
 * - Bill Payment: "Rs.1500 debited from your Airtel Payments Bank A/c XX1234 for bill payment on 03-01-26. Ref: AB123456"
 * - ATM Withdrawal: "Rs.2000 withdrawn from Airtel Bank A/c XX1234 at ATM on 03-01-26. Avl Bal: Rs.4000.00"
 */
object AirtelPatterns {

    /**
     * Returns the bank configuration for Airtel Payments Bank.
     *
     * Includes all known sender IDs used by Airtel for payment bank transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "Airtel Payments Bank",
            displayName = "Airtel",
            senderIds = listOf(
                "VM-AIRTEL",     // Airtel general (VM prefix)
                "AD-AIRTPB",     // Airtel Payments Bank advertorial
                "AIRTPB",        // Airtel Payments Bank short code
                "AIRTEL",        // General Airtel alerts
                "AIRTELB",       // Airtel Bank alternative
                "AIRTELPB",      // Airtel Payments Bank full
                "AIRTELUPI",     // Airtel UPI transactions
                "AIRTELMB",      // Airtel Mobile Banking
                "VM-AIRTPB",     // Airtel Payments Bank VM prefix
                "AD-AIRTEL"      // Airtel advertorial
            )
        )
    }

    /**
     * Returns Airtel-specific SMS parsing patterns.
     *
     * Airtel Payments Bank follows standard Indian banking SMS formats:
     * - Amount: "Rs.XXX" or "Rs XXX" or "INR XXX"
     * - Balance: "Avl Bal: Rs.XXX" or "Balance: Rs.XXX"
     * - Reference: "Ref No:", "Ref:", "Transaction ID:"
     * - Merchant: "to [Merchant]", "at [Merchant]", "for [Service]"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where Airtel differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // Airtel Payments Bank follows standard Indian banking SMS formats closely.
        // The generic patterns should handle most cases effectively, including:
        // - Standard "Rs.XXX" and "Rs XXX" amount formats
        // - Common balance formats (Avl Bal, Balance)
        // - Standard reference patterns (Ref No, Ref, UTR)
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
