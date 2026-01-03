package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for ICICI Bank.
 *
 * ICICI Bank is one of India's largest private sector banks and uses multiple sender IDs
 * for transaction alerts across different services (Internet Banking, Mobile Banking, Cards, etc.).
 *
 * Common ICICI sender IDs:
 * - VM-ICICIB: Internet Banking and general transactions
 * - BZ-ICICIB: Business banking alerts
 * - ICICIB: Short code for general alerts
 * - iMobile: ICICI Mobile Banking app
 * - ICICIC: ICICI Credit Card transactions
 * - ICICICC: Credit card (alternate)
 *
 * SMS Format Examples:
 * - Debit: "Rs 500.00 debited from A/c XX1234 on 03-Jan-26. Avl Bal: Rs 10000.00. UPI Ref No 123456789012"
 * - Credit: "Rs 1000.00 credited to A/c XX1234 on 03-Jan-26. Info: NEFT-SALARY. Avl Bal: Rs 11000.00"
 * - UPI: "Rs 250 debited from A/c XX1234 on 03-Jan-26 for VPA merchant@paytm (UPI). Ref No 123456789012"
 * - Card: "Your ICICI Bank Credit Card XX1234 used for Rs 1500.00 at AMAZON on 03-Jan-26. Avl Credit Limit Rs 50000.00"
 */
object IciciPatterns {

    /**
     * Returns the bank configuration for ICICI Bank.
     *
     * Includes all known sender IDs used by ICICI Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "ICICI Bank",
            displayName = "ICICI",
            senderIds = listOf(
                "VM-ICICIB",     // Internet Banking & General
                "BZ-ICICIB",     // Business Banking
                "ICICIB",        // Short code
                "iMobile",       // Mobile Banking App
                "ICICIC",        // Credit Card
                "ICICICC",       // Credit Card (alternate)
                "ICICIPB",       // Pockets (Wallet)
                "ICIUPI",        // UPI transactions
                "AD-ICICIB",     // Advertorial/Promotional
                "VM-ICIPRU"      // ICICI Prudential (linked services)
            )
        )
    }

    /**
     * Returns ICICI Bank-specific SMS parsing patterns.
     *
     * ICICI Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal: Rs XXX" or "Available Balance Rs XXX"
     * - Reference: "UPI Ref No", "Ref No", "UTR No"
     * - Merchant: "VPA merchant@upi" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where ICICI differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // ICICI Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
