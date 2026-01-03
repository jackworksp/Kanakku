package com.example.kanakku.data.sms.model

/**
 * Configuration for a specific bank's SMS patterns.
 *
 * Each bank may send transaction SMS from multiple sender IDs with varying formats.
 * This class encapsulates the basic identification and display information for a bank.
 *
 * Examples:
 * - HDFC Bank uses sender IDs: VM-HDFCBK, AD-HDFCBK
 * - SBI uses sender IDs: VM-SBIINB, AD-SBIBNK, SBI, SBIINB
 *
 * @property bankName Full name of the bank (e.g., "HDFC Bank", "State Bank of India")
 * @property displayName Short display name for UI (e.g., "HDFC", "SBI")
 * @property senderIds List of sender ID patterns this bank uses for transaction SMS
 */
data class BankConfig(
    val bankName: String,
    val displayName: String,
    val senderIds: List<String>
)
