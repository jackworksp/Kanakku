package com.example.kanakku.data.sms

import com.example.kanakku.data.sms.model.BankConfig
import com.example.kanakku.data.sms.model.BankPatternSet

/**
 * Complete bank configuration including identification and parsing patterns.
 *
 * This class pairs a bank's identification information (BankConfig) with its
 * specific SMS parsing patterns (BankPatternSet) to provide a complete configuration.
 *
 * @property config Bank identification and sender IDs
 * @property patterns Optional bank-specific regex patterns for parsing SMS
 */
data class BankRegistryEntry(
    val config: BankConfig,
    val patterns: BankPatternSet? = null
)

/**
 * Registry interface for managing bank configurations and pattern lookup.
 *
 * The BankRegistry acts as a central repository for all supported bank configurations,
 * enabling efficient lookup of bank-specific patterns based on SMS sender IDs.
 *
 * This abstraction allows for different implementations (e.g., static, dynamic, remote)
 * while providing a consistent interface to the parser.
 */
interface BankRegistry {

    /**
     * Registers a bank with its configuration and optional custom patterns.
     *
     * @param config Bank configuration (name, display name, sender IDs)
     * @param patterns Optional bank-specific regex patterns. If null, generic patterns will be used.
     */
    fun registerBank(config: BankConfig, patterns: BankPatternSet? = null)

    /**
     * Looks up a bank configuration by sender ID.
     *
     * This method checks if the given sender ID matches any registered bank's sender ID patterns.
     * The matching is case-insensitive to handle variations in sender ID formats.
     *
     * @param senderId The SMS sender ID/address to look up (e.g., "VM-HDFCBK", "SBIINB")
     * @return BankRegistryEntry if a matching bank is found, null otherwise
     */
    fun findBankBySender(senderId: String): BankRegistryEntry?

    /**
     * Gets all registered banks.
     *
     * @return List of all registered bank entries
     */
    fun getAllBanks(): List<BankRegistryEntry>

    /**
     * Gets the total number of registered banks.
     *
     * @return Count of registered banks
     */
    fun getBankCount(): Int
}

/**
 * Default implementation of BankRegistry.
 *
 * This implementation uses in-memory storage with optimized lookup by sender ID.
 * Sender IDs are normalized to uppercase for case-insensitive matching.
 *
 * Thread-safety: This implementation is NOT thread-safe. Banks should be registered
 * during initialization before concurrent access begins.
 */
class BankRegistryImpl : BankRegistry {

    // List of all registered bank entries
    private val banks = mutableListOf<BankRegistryEntry>()

    // Map for fast lookup: sender ID (uppercase) -> BankRegistryEntry
    // This enables O(1) lookup instead of O(n) linear search
    private val senderIdMap = mutableMapOf<String, BankRegistryEntry>()

    override fun registerBank(config: BankConfig, patterns: BankPatternSet?) {
        val entry = BankRegistryEntry(config, patterns)
        banks.add(entry)

        // Register each sender ID for fast lookup
        config.senderIds.forEach { senderId ->
            // Normalize to uppercase for case-insensitive matching
            senderIdMap[senderId.uppercase()] = entry
        }
    }

    override fun findBankBySender(senderId: String): BankRegistryEntry? {
        // Normalize to uppercase for case-insensitive matching
        return senderIdMap[senderId.uppercase()]
    }

    override fun getAllBanks(): List<BankRegistryEntry> {
        return banks.toList() // Return defensive copy
    }

    override fun getBankCount(): Int {
        return banks.size
    }
}
