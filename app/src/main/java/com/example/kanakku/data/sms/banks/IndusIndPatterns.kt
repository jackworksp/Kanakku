package com.example.kanakku.data.sms.banks

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * SMS patterns and configuration for IndusInd Bank.
 *
 * IndusInd Bank is a major private sector bank in India and uses multiple sender IDs
 * for transaction alerts across different services (Internet Banking, Mobile Banking, Cards, Loans, etc.).
 *
 * Common IndusInd Bank sender IDs:
 * - VM-ILOANS: Loan-related transactions and alerts
 * - AD-INDBNK: General banking alerts and promotions
 * - INDBNK: Short code for general alerts
 * - IndusInd: Bank name code
 * - INDUSIND: Alternative bank code
 * - INDUSUPI: UPI transactions
 *
 * SMS Format Examples:
 * - Debit: "Rs.500.00 debited from A/c XX1234 on 03-01-26. Avl Bal Rs.10000.00. Ref No 123456789012"
 * - Credit: "Rs.1000.00 credited to A/c XX1234 on 03-01-26. Info: NEFT-SALARY. Avl Bal Rs.11000.00"
 * - UPI: "Rs.250 debited from A/c XX1234 on 03-01-26 via UPI to merchant@paytm. Ref No 123456789012"
 * - Card: "Your IndusInd Bank Credit Card XX1234 has been used for Rs.1500.00 at MERCHANT on 03-01-26"
 * - Loan: "Your loan A/c XX1234 has been debited by Rs.5000.00 on 03-01-26. EMI payment successful"
 */
object IndusIndPatterns {

    /**
     * Returns the bank configuration for IndusInd Bank.
     *
     * Includes all known sender IDs used by IndusInd Bank for transaction alerts.
     */
    fun getBankConfig(): BankConfig {
        return BankConfig(
            bankName = "IndusInd Bank",
            displayName = "IndusInd",
            senderIds = listOf(
                "VM-ILOANS",     // Loans
                "AD-INDBNK",     // General Banking & Promotions
                "INDBNK",        // Short code
                "IndusInd",      // Bank name code
                "INDUSIND",      // Alternative bank code
                "INDUSUPI",      // UPI transactions
                "INDUSMB",       // Mobile Banking
                "INDUSCC",       // Credit Card
                "INDUSATM",      // ATM transactions
                "VM-INDBNK"      // Internet Banking
            )
        )
    }

    /**
     * Returns IndusInd Bank-specific SMS parsing patterns.
     *
     * IndusInd Bank generally follows standard formats, but has some specific patterns:
     * - Amount: "Rs.XXX.XX" or "INR XXX.XX"
     * - Balance: "Avl Bal Rs.XXX" or "Available Bal Rs.XXX"
     * - Reference: "Ref No", "UPI Ref No", "UTR No"
     * - Merchant: "VPA merchant@upi" or "Info: MERCHANT"
     *
     * Note: Most patterns are handled well by generic patterns, so we only
     * define custom patterns where IndusInd Bank differs significantly.
     */
    fun getBankPatternSet(): BankPatternSet? {
        // IndusInd Bank follows standard Indian banking SMS format closely.
        // Generic patterns should handle most cases effectively.
        // Return null to use generic patterns unless specific edge cases are discovered.
        return null
    }
}
