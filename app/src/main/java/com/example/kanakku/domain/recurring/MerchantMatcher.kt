package com.example.kanakku.domain.recurring

/**
 * Utility for normalizing and matching merchant names from transaction data.
 *
 * This matcher handles variations in merchant names such as:
 * - Case differences: "Netflix" vs "NETFLIX"
 * - Business suffixes: "Netflix Inc" vs "Netflix"
 * - Special characters: "AMAZON.COM" vs "Amazon"
 * - Whitespace variations: "Spotify  Premium" vs "Spotify Premium"
 *
 * The normalization process is designed to group transactions from the same
 * merchant even when the exact string representation varies across different SMS messages.
 */
object MerchantMatcher {

    /**
     * Common business suffixes and keywords to remove during normalization.
     * These are typically legal entity identifiers that don't help with merchant identification.
     */
    private val BUSINESS_SUFFIXES = setOf(
        "INC", "INCORPORATED",
        "LTD", "LIMITED",
        "PVT", "PRIVATE",
        "LLC", "LLP",
        "CO", "COMPANY", "CORP", "CORPORATION",
        "PLC", "GMBH", "SA", "AG"
    )

    /**
     * Common prefixes that can be removed for better matching.
     */
    private val COMMON_PREFIXES = setOf(
        "WWW", "HTTP", "HTTPS"
    )

    /**
     * Common domain extensions to remove.
     */
    private val DOMAIN_EXTENSIONS = setOf(
        "COM", "NET", "ORG", "IN", "CO", "IO"
    )

    /**
     * Normalizes a merchant name for consistent matching.
     *
     * The normalization process:
     * 1. Converts to uppercase for case-insensitive matching
     * 2. Trims leading and trailing whitespace
     * 3. Removes special characters (keeps only alphanumeric and spaces)
     * 4. Removes common business suffixes (INC, LTD, PVT, etc.)
     * 5. Removes common prefixes (WWW, HTTP, etc.)
     * 6. Removes domain extensions (.COM, .NET, etc.)
     * 7. Normalizes multiple spaces to single space
     * 8. Trims again after all transformations
     *
     * Examples:
     * - "Netflix" → "NETFLIX"
     * - "NETFLIX Inc" → "NETFLIX"
     * - "amazon.com" → "AMAZON"
     * - "www.spotify.com" → "SPOTIFY"
     * - "HDFC Bank Ltd." → "HDFC BANK"
     * - "Swiggy  India  Pvt" → "SWIGGY INDIA"
     *
     * @param merchant Raw merchant name from transaction
     * @return Normalized merchant pattern, or empty string if input is blank
     */
    fun normalize(merchant: String): String {
        if (merchant.isBlank()) {
            return ""
        }

        var normalized = merchant
            .uppercase()
            .trim()
            // Remove special characters, keeping only alphanumeric and spaces
            .replace(Regex("[^A-Z0-9\\s]"), " ")
            .trim()

        // Remove common prefixes
        COMMON_PREFIXES.forEach { prefix ->
            normalized = normalized.replace(Regex("^$prefix\\s+"), "")
        }

        // Remove domain extensions (standalone or at the end)
        DOMAIN_EXTENSIONS.forEach { ext ->
            normalized = normalized.replace(Regex("\\s+$ext\\s*$"), "")
            normalized = normalized.replace(Regex("\\s+$ext\\s+"), " ")
        }

        // Remove business suffixes (at the end of string)
        BUSINESS_SUFFIXES.forEach { suffix ->
            normalized = normalized.replace(Regex("\\s+$suffix\\s*$"), "")
        }

        // Normalize multiple spaces to single space
        normalized = normalized.replace(Regex("\\s+"), " ").trim()

        return normalized
    }

    /**
     * Checks if two merchant names match after normalization.
     *
     * This is a convenience method that normalizes both names and compares them.
     *
     * Examples:
     * ```
     * matches("Netflix", "NETFLIX Inc")  → true
     * matches("amazon.com", "Amazon")    → true
     * matches("Spotify", "Netflix")      → false
     * ```
     *
     * @param merchant1 First merchant name
     * @param merchant2 Second merchant name
     * @return True if the normalized names match, false otherwise
     */
    fun matches(merchant1: String, merchant2: String): Boolean {
        return normalize(merchant1) == normalize(merchant2)
    }

    /**
     * Checks if a merchant name matches any of the provided patterns after normalization.
     *
     * Useful for checking if a merchant belongs to a set of known merchants or patterns.
     *
     * Examples:
     * ```
     * matchesAny("Netflix Inc", listOf("Spotify", "Netflix", "Prime"))  → true
     * matchesAny("unknown", listOf("Spotify", "Netflix"))               → false
     * ```
     *
     * @param merchant Merchant name to check
     * @param patterns List of merchant patterns to match against
     * @return True if merchant matches any of the patterns
     */
    fun matchesAny(merchant: String, patterns: List<String>): Boolean {
        val normalizedMerchant = normalize(merchant)
        return patterns.any { normalize(it) == normalizedMerchant }
    }

    /**
     * Finds the best matching merchant pattern from a list of candidates.
     *
     * Returns the first matching pattern if found, or null if no matches exist.
     * The comparison is done after normalizing both the merchant and all candidates.
     *
     * @param merchant Merchant name to match
     * @param candidates List of candidate patterns to match against
     * @return First matching candidate pattern, or null if no match found
     */
    fun findBestMatch(merchant: String, candidates: List<String>): String? {
        val normalizedMerchant = normalize(merchant)
        return candidates.firstOrNull { normalize(it) == normalizedMerchant }
    }

    /**
     * Groups a list of merchant names by their normalized form.
     *
     * Useful for grouping transactions by merchant despite name variations.
     *
     * Example:
     * ```
     * val merchants = listOf("Netflix", "NETFLIX Inc", "Amazon.com", "amazon")
     * groupByNormalized(merchants)
     * // Returns: {
     * //   "NETFLIX" -> ["Netflix", "NETFLIX Inc"],
     * //   "AMAZON" -> ["Amazon.com", "amazon"]
     * // }
     * ```
     *
     * @param merchants List of merchant names to group
     * @return Map of normalized pattern to list of original merchant names
     */
    fun groupByNormalized(merchants: List<String>): Map<String, List<String>> {
        return merchants
            .filter { it.isNotBlank() }
            .groupBy { normalize(it) }
    }

    /**
     * Checks if a merchant name contains a specific keyword after normalization.
     *
     * Useful for categorization or pattern matching based on keywords.
     *
     * Examples:
     * ```
     * contains("Netflix India Pvt Ltd", "NETFLIX")  → true
     * contains("Amazon.com", "AMAZON")              → true
     * contains("HDFC Bank", "SBI")                  → false
     * ```
     *
     * @param merchant Merchant name to check
     * @param keyword Keyword to search for
     * @return True if normalized merchant contains the normalized keyword
     */
    fun contains(merchant: String, keyword: String): Boolean {
        return normalize(merchant).contains(normalize(keyword))
    }

    /**
     * Checks if a merchant name matches any of the provided regex patterns after normalization.
     *
     * Useful for advanced pattern matching like detecting EMI providers or utility companies.
     *
     * Examples:
     * ```
     * matchesPattern("HDFC Bank EMI", Regex("EMI|LOAN"))     → true
     * matchesPattern("Electricity Bill", Regex("ELECTRIC"))  → true
     * ```
     *
     * @param merchant Merchant name to check
     * @param pattern Regex pattern to match against normalized merchant name
     * @return True if pattern matches the normalized merchant name
     */
    fun matchesPattern(merchant: String, pattern: Regex): Boolean {
        return normalize(merchant).contains(pattern)
    }
}
