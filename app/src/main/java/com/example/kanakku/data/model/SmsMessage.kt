package com.example.kanakku.data.model

data class SmsMessage(
    val id: Long,
    val address: String,      // Sender (e.g., "VM-HDFCBK", "AD-SBIBNK")
    val body: String,         // SMS content
    val date: Long,           // Timestamp in milliseconds
    val isRead: Boolean
)

data class ParsedTransaction(
    val smsId: Long,
    val amount: Double,
    val type: TransactionType,
    val merchant: String?,        // Best-effort extraction
    val accountNumber: String?,   // Last 4 digits usually
    val referenceNumber: String?, // UTR/REF/TXN number
    val date: Long,               // Transaction timestamp
    val rawSms: String,           // Original SMS for reference
    val senderAddress: String,    // Bank sender ID
    val balanceAfter: Double? = null,  // Balance after transaction (if available in SMS)
    val location: String? = null,      // ATM/merchant location (if available)
    val upiId: String? = null,         // UPI VPA (Virtual Payment Address) like user@paytm
    val paymentMethod: String? = null  // Payment method (e.g., "UPI", "Card", "Net Banking")
)

enum class TransactionType {
    DEBIT,
    CREDIT,
    UNKNOWN
}
