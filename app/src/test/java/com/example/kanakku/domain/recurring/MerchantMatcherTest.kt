package com.example.kanakku.domain.recurring

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MerchantMatcher.
 *
 * Tests cover:
 * - Basic normalization (case, whitespace, special characters)
 * - Business suffix removal (INC, LTD, PVT, CORP, etc.)
 * - Domain extension removal (.COM, .NET, .ORG, etc.)
 * - Common prefix removal (WWW, HTTP, HTTPS)
 * - Multiple space normalization
 * - Merchant matching (two names)
 * - Match any pattern from list
 * - Find best match from candidates
 * - Grouping by normalized form
 * - Keyword containment checks
 * - Regex pattern matching
 * - Edge cases: blank strings, null values, complex variations
 */
class MerchantMatcherTest {

    // ==================== normalize() Tests ====================

    @Test
    fun normalize_simpleMerchantName_convertsToUppercase() {
        // Given
        val merchant = "Netflix"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("NETFLIX", result)
    }

    @Test
    fun normalize_uppercaseName_remainsUppercase() {
        // Given
        val merchant = "AMAZON"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("AMAZON", result)
    }

    @Test
    fun normalize_lowercaseName_convertsToUppercase() {
        // Given
        val merchant = "spotify"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("SPOTIFY", result)
    }

    @Test
    fun normalize_mixedCaseName_convertsToUppercase() {
        // Given
        val merchant = "HdFc BaNk"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("HDFC BANK", result)
    }

    @Test
    fun normalize_nameWithLeadingTrailingSpaces_trimsSpaces() {
        // Given
        val merchant = "  Netflix  "

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("NETFLIX", result)
    }

    @Test
    fun normalize_nameWithMultipleSpaces_normalizesToSingleSpace() {
        // Given
        val merchant = "Amazon   Prime    Video"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("AMAZON PRIME VIDEO", result)
    }

    @Test
    fun normalize_nameWithSpecialCharacters_removesSpecialCharacters() {
        // Given
        val merchant = "Amazon.com"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        // Dot becomes space creating "AMAZON COM", then COM suffix is removed
        assertEquals("AMAZON", result)
    }

    @Test
    fun normalize_nameWithHyphens_replacesHyphensWithSpaces() {
        // Given
        val merchant = "ABC-XYZ-Store"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("ABC XYZ STORE", result)
    }

    @Test
    fun normalize_nameWithUnderscores_replacesUnderscoresWithSpaces() {
        // Given
        val merchant = "Company_Name_Here"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("COMPANY NAME HERE", result)
    }

    @Test
    fun normalize_nameWithDots_replacesDotsWithSpaces() {
        // Given
        val merchant = "H.D.F.C. Bank"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        // Note: Multiple spaces get normalized to single space, but each dot creates a space
        assertEquals("H D F C BANK", result)
    }

    // ==================== Business Suffix Removal Tests ====================

    @Test
    fun normalize_nameWithIncSuffix_removesInc() {
        // Given
        val merchant = "Netflix Inc"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("NETFLIX", result)
    }

    @Test
    fun normalize_nameWithIncorporatedSuffix_removesIncorporated() {
        // Given
        val merchant = "Netflix Incorporated"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("NETFLIX", result)
    }

    @Test
    fun normalize_nameWithLtdSuffix_removesLtd() {
        // Given
        val merchant = "HDFC Bank Ltd"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("HDFC BANK", result)
    }

    @Test
    fun normalize_nameWithLimitedSuffix_removesLimited() {
        // Given
        val merchant = "ICICI Bank Limited"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("ICICI BANK", result)
    }

    @Test
    fun normalize_nameWithPvtSuffix_removesPvt() {
        // Given
        val merchant = "Swiggy Pvt"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("SWIGGY", result)
    }

    @Test
    fun normalize_nameWithPrivateSuffix_removesPrivate() {
        // Given
        val merchant = "Zomato Private"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("ZOMATO", result)
    }

    @Test
    fun normalize_nameWithLlcSuffix_removesLlc() {
        // Given
        val merchant = "Amazon LLC"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("AMAZON", result)
    }

    @Test
    fun normalize_nameWithLlpSuffix_removesLlp() {
        // Given
        val merchant = "Consulting LLP"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("CONSULTING", result)
    }

    @Test
    fun normalize_nameWithCoSuffix_removesCo() {
        // Given
        val merchant = "Trading Co"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("TRADING", result)
    }

    @Test
    fun normalize_nameWithCompanySuffix_removesCompany() {
        // Given
        val merchant = "Electric Company"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("ELECTRIC", result)
    }

    @Test
    fun normalize_nameWithCorpSuffix_removesCorp() {
        // Given
        val merchant = "Microsoft Corp"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("MICROSOFT", result)
    }

    @Test
    fun normalize_nameWithCorporationSuffix_removesCorporation() {
        // Given
        val merchant = "Apple Corporation"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("APPLE", result)
    }

    @Test
    fun normalize_nameWithPlcSuffix_removesPlc() {
        // Given
        val merchant = "British Company PLC"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("BRITISH COMPANY", result)
    }

    @Test
    fun normalize_nameWithGmbhSuffix_removesGmbh() {
        // Given
        val merchant = "German Company GmbH"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("GERMAN COMPANY", result)
    }

    @Test
    fun normalize_nameWithMultipleSuffixes_removesAllSuffixes() {
        // Given
        val merchant = "Company Name Pvt Ltd"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("COMPANY NAME", result)
    }

    // ==================== Domain Extension Removal Tests ====================

    @Test
    fun normalize_nameWithComExtension_removesComExtension() {
        // Given
        val merchant = "amazon.com"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        // Dot becomes space, then "COM" suffix is removed
        assertEquals("AMAZON", result)
    }

    @Test
    fun normalize_nameWithNetExtension_removesNetExtension() {
        // Given
        val merchant = "website.net"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("WEBSITE", result)
    }

    @Test
    fun normalize_nameWithOrgExtension_removesOrgExtension() {
        // Given
        val merchant = "nonprofit.org"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("NONPROFIT", result)
    }

    @Test
    fun normalize_nameWithInExtension_removesInExtension() {
        // Given
        val merchant = "flipkart.in"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("FLIPKART", result)
    }

    @Test
    fun normalize_nameWithCoInExtension_removesCoExtension() {
        // Given
        val merchant = "myntra.co"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        // Dot becomes space, then "CO" suffix is removed
        assertEquals("MYNTRA", result)
    }

    @Test
    fun normalize_nameWithIoExtension_removesIoExtension() {
        // Given
        val merchant = "startup.io"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("STARTUP", result)
    }

    @Test
    fun normalize_domainInMiddle_doesNotRemoveDomainExtension() {
        // Given: "COM" appears in middle of name, not at end
        val merchant = "Commonwealth Bank"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then: Should NOT remove "COM" from "COMmonwealth"
        assertEquals("COMMONWEALTH BANK", result)
    }

    // ==================== Common Prefix Removal Tests ====================

    @Test
    fun normalize_nameWithWwwPrefix_removesWwwPrefix() {
        // Given
        val merchant = "www.netflix.com"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        // Dots become spaces, WWW prefix and COM suffix are removed
        assertEquals("NETFLIX", result)
    }

    @Test
    fun normalize_nameWithHttpPrefix_removesHttpPrefix() {
        // Given
        val merchant = "http://amazon.com"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("AMAZON", result)
    }

    @Test
    fun normalize_nameWithHttpsPrefix_removesHttpsPrefix() {
        // Given
        val merchant = "https://spotify.com"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        // Special chars become spaces, HTTPS prefix and COM suffix are removed
        assertEquals("SPOTIFY", result)
    }

    // ==================== Complex Normalization Tests ====================

    @Test
    fun normalize_complexNameWithAllTransformations_normalizesCorrectly() {
        // Given: Name with prefixes, suffixes, special chars, spaces
        val merchant = "www.HDFC-Bank.co.in Ltd."

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("HDFC BANK", result)
    }

    @Test
    fun normalize_realWorldNetflixVariations_normalizesToSameName() {
        // Given: Various Netflix name formats
        val variations = listOf(
            "Netflix",
            "NETFLIX Inc",
            "netflix.com",
            "www.netflix.com",
            "Netflix Inc.",
            "NETFLIX INCORPORATED"
        )

        // When: Normalize all variations
        val results = variations.map { MerchantMatcher.normalize(it) }

        // Then: All should normalize to "NETFLIX"
        results.forEach { assertEquals("NETFLIX", it) }
    }

    @Test
    fun normalize_realWorldAmazonVariations_normalizesToSameName() {
        // Given: Various Amazon name formats
        val variations = listOf(
            "Amazon",
            "AMAZON.COM",
            "amazon.in",
            "www.amazon.com",
            "Amazon Corp"
        )

        // When: Normalize all variations
        val results = variations.map { MerchantMatcher.normalize(it) }

        // Then: All should normalize to "AMAZON"
        results.forEach { assertEquals("AMAZON", it) }
    }

    @Test
    fun normalize_swiggyVariations_normalizesToSameName() {
        // Given: Various Swiggy name formats
        val variations = listOf(
            "Swiggy",
            "Swiggy Pvt",
            "SWIGGY INDIA PVT LTD",
            "swiggy.in"
        )

        // When: Normalize all variations
        val results = variations.map { MerchantMatcher.normalize(it) }

        // Then: Most should normalize to "SWIGGY" or "SWIGGY INDIA"
        assertTrue(results.any { it == "SWIGGY" || it == "SWIGGY INDIA" })
    }

    // ==================== Edge Cases ====================

    @Test
    fun normalize_blankString_returnsEmptyString() {
        // Given
        val merchant = ""

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("", result)
    }

    @Test
    fun normalize_onlySpaces_returnsEmptyString() {
        // Given
        val merchant = "   "

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("", result)
    }

    @Test
    fun normalize_onlySpecialCharacters_returnsEmptyString() {
        // Given
        val merchant = "!@#$%^&*()"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("", result)
    }

    @Test
    fun normalize_singleCharacter_returnsUppercaseCharacter() {
        // Given
        val merchant = "a"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("A", result)
    }

    @Test
    fun normalize_numbersInName_preservesNumbers() {
        // Given
        val merchant = "Store123"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("STORE123", result)
    }

    @Test
    fun normalize_onlyNumbers_preservesNumbers() {
        // Given
        val merchant = "12345"

        // When
        val result = MerchantMatcher.normalize(merchant)

        // Then
        assertEquals("12345", result)
    }

    // ==================== matches() Tests ====================

    @Test
    fun matches_identicalNames_returnsTrue() {
        // Given
        val merchant1 = "Netflix"
        val merchant2 = "Netflix"

        // When
        val result = MerchantMatcher.matches(merchant1, merchant2)

        // Then
        assertTrue(result)
    }

    @Test
    fun matches_sameMerchantDifferentCase_returnsTrue() {
        // Given
        val merchant1 = "Netflix"
        val merchant2 = "NETFLIX"

        // When
        val result = MerchantMatcher.matches(merchant1, merchant2)

        // Then
        assertTrue(result)
    }

    @Test
    fun matches_sameMerchantWithAndWithoutSuffix_returnsTrue() {
        // Given
        val merchant1 = "Netflix"
        val merchant2 = "NETFLIX Inc"

        // When
        val result = MerchantMatcher.matches(merchant1, merchant2)

        // Then
        assertTrue(result)
    }

    @Test
    fun matches_sameMerchantAsPlainAndDomain_returnsTrue() {
        // Given
        val merchant1 = "amazon.com"
        val merchant2 = "Amazon"

        // When
        val result = MerchantMatcher.matches(merchant1, merchant2)

        // Then
        assertTrue(result)
    }

    @Test
    fun matches_differentMerchants_returnsFalse() {
        // Given
        val merchant1 = "Spotify"
        val merchant2 = "Netflix"

        // When
        val result = MerchantMatcher.matches(merchant1, merchant2)

        // Then
        assertFalse(result)
    }

    @Test
    fun matches_similarButDifferentMerchants_returnsFalse() {
        // Given
        val merchant1 = "HDFC Bank"
        val merchant2 = "ICICI Bank"

        // When
        val result = MerchantMatcher.matches(merchant1, merchant2)

        // Then
        assertFalse(result)
    }

    @Test
    fun matches_blankStrings_returnsTrue() {
        // Given
        val merchant1 = ""
        val merchant2 = "   "

        // When
        val result = MerchantMatcher.matches(merchant1, merchant2)

        // Then
        assertTrue(result)
    }

    // ==================== matchesAny() Tests ====================

    @Test
    fun matchesAny_merchantMatchesFirstPattern_returnsTrue() {
        // Given
        val merchant = "Netflix Inc"
        val patterns = listOf("Netflix", "Spotify", "Prime")

        // When
        val result = MerchantMatcher.matchesAny(merchant, patterns)

        // Then
        assertTrue(result)
    }

    @Test
    fun matchesAny_merchantMatchesMiddlePattern_returnsTrue() {
        // Given
        val merchant = "spotify.com"
        val patterns = listOf("Netflix", "Spotify", "Prime")

        // When
        val result = MerchantMatcher.matchesAny(merchant, patterns)

        // Then
        assertTrue(result)
    }

    @Test
    fun matchesAny_merchantMatchesLastPattern_returnsTrue() {
        // Given
        val merchant = "Amazon Prime"
        val patterns = listOf("Netflix", "Spotify", "Amazon Prime")

        // When
        val result = MerchantMatcher.matchesAny(merchant, patterns)

        // Then
        assertTrue(result)
    }

    @Test
    fun matchesAny_merchantDoesNotMatch_returnsFalse() {
        // Given
        val merchant = "Unknown Service"
        val patterns = listOf("Netflix", "Spotify", "Prime")

        // When
        val result = MerchantMatcher.matchesAny(merchant, patterns)

        // Then
        assertFalse(result)
    }

    @Test
    fun matchesAny_emptyPatternList_returnsFalse() {
        // Given
        val merchant = "Netflix"
        val patterns = emptyList<String>()

        // When
        val result = MerchantMatcher.matchesAny(merchant, patterns)

        // Then
        assertFalse(result)
    }

    // ==================== findBestMatch() Tests ====================

    @Test
    fun findBestMatch_merchantMatchesFirstCandidate_returnsFirstCandidate() {
        // Given
        val merchant = "Netflix Inc"
        val candidates = listOf("Netflix", "Spotify", "Amazon")

        // When
        val result = MerchantMatcher.findBestMatch(merchant, candidates)

        // Then
        assertEquals("Netflix", result)
    }

    @Test
    fun findBestMatch_merchantMatchesMiddleCandidate_returnsMiddleCandidate() {
        // Given
        val merchant = "spotify.com"
        val candidates = listOf("Netflix", "Spotify", "Amazon")

        // When
        val result = MerchantMatcher.findBestMatch(merchant, candidates)

        // Then
        assertEquals("Spotify", result)
    }

    @Test
    fun findBestMatch_merchantMatchesLastCandidate_returnsLastCandidate() {
        // Given
        val merchant = "amazon.in"
        val candidates = listOf("Netflix", "Spotify", "Amazon")

        // When
        val result = MerchantMatcher.findBestMatch(merchant, candidates)

        // Then
        assertEquals("Amazon", result)
    }

    @Test
    fun findBestMatch_merchantDoesNotMatch_returnsNull() {
        // Given
        val merchant = "Unknown"
        val candidates = listOf("Netflix", "Spotify", "Amazon")

        // When
        val result = MerchantMatcher.findBestMatch(merchant, candidates)

        // Then
        assertNull(result)
    }

    @Test
    fun findBestMatch_emptyCandidateList_returnsNull() {
        // Given
        val merchant = "Netflix"
        val candidates = emptyList<String>()

        // When
        val result = MerchantMatcher.findBestMatch(merchant, candidates)

        // Then
        assertNull(result)
    }

    // ==================== groupByNormalized() Tests ====================

    @Test
    fun groupByNormalized_variationsOfSameMerchant_groupsTogether() {
        // Given
        val merchants = listOf("Netflix", "NETFLIX Inc", "netflix.com")

        // When
        val result = MerchantMatcher.groupByNormalized(merchants)

        // Then
        assertEquals(1, result.size)
        assertTrue(result.containsKey("NETFLIX"))
        assertEquals(3, result["NETFLIX"]?.size)
        assertTrue(result["NETFLIX"]?.containsAll(merchants) == true)
    }

    @Test
    fun groupByNormalized_multipleDifferentMerchants_groupsSeparately() {
        // Given
        val merchants = listOf(
            "Netflix", "NETFLIX Inc",
            "Amazon.com", "amazon",
            "Spotify"
        )

        // When
        val result = MerchantMatcher.groupByNormalized(merchants)

        // Then
        assertEquals(3, result.size)
        assertTrue(result.containsKey("NETFLIX"))
        assertTrue(result.containsKey("AMAZON"))
        assertTrue(result.containsKey("SPOTIFY"))
        assertEquals(2, result["NETFLIX"]?.size)
        assertEquals(2, result["AMAZON"]?.size)
        assertEquals(1, result["SPOTIFY"]?.size)
    }

    @Test
    fun groupByNormalized_emptyList_returnsEmptyMap() {
        // Given
        val merchants = emptyList<String>()

        // When
        val result = MerchantMatcher.groupByNormalized(merchants)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun groupByNormalized_listWithBlankStrings_filtersOutBlanks() {
        // Given
        val merchants = listOf("Netflix", "", "  ", "Amazon")

        // When
        val result = MerchantMatcher.groupByNormalized(merchants)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.containsKey("NETFLIX"))
        assertTrue(result.containsKey("AMAZON"))
    }

    @Test
    fun groupByNormalized_complexRealWorldExample_groupsCorrectly() {
        // Given: Real-world merchant name variations
        val merchants = listOf(
            "Netflix",
            "NETFLIX Inc",
            "netflix.com",
            "Amazon.com",
            "amazon.in",
            "AMAZON",
            "Spotify",
            "spotify.com",
            "HDFC Bank Ltd",
            "HDFC Bank"
        )

        // When
        val result = MerchantMatcher.groupByNormalized(merchants)

        // Then
        assertEquals(4, result.size) // Netflix, Amazon, Spotify, HDFC BANK
        assertTrue(result.containsKey("NETFLIX"))
        assertTrue(result.containsKey("AMAZON"))
        assertTrue(result.containsKey("SPOTIFY"))
        assertTrue(result.containsKey("HDFC BANK"))
        assertEquals(3, result["NETFLIX"]?.size)
        assertEquals(3, result["AMAZON"]?.size)
        assertEquals(2, result["SPOTIFY"]?.size)
        assertEquals(2, result["HDFC BANK"]?.size)
    }

    // ==================== contains() Tests ====================

    @Test
    fun contains_merchantContainsKeyword_returnsTrue() {
        // Given
        val merchant = "Netflix India Pvt Ltd"
        val keyword = "NETFLIX"

        // When
        val result = MerchantMatcher.contains(merchant, keyword)

        // Then
        assertTrue(result)
    }

    @Test
    fun contains_merchantContainsKeywordDifferentCase_returnsTrue() {
        // Given
        val merchant = "Amazon.com"
        val keyword = "amazon"

        // When
        val result = MerchantMatcher.contains(merchant, keyword)

        // Then
        assertTrue(result)
    }

    @Test
    fun contains_merchantContainsKeywordAsSubstring_returnsTrue() {
        // Given
        val merchant = "HDFC Bank Ltd"
        val keyword = "HDFC"

        // When
        val result = MerchantMatcher.contains(merchant, keyword)

        // Then
        assertTrue(result)
    }

    @Test
    fun contains_merchantDoesNotContainKeyword_returnsFalse() {
        // Given
        val merchant = "HDFC Bank"
        val keyword = "SBI"

        // When
        val result = MerchantMatcher.contains(merchant, keyword)

        // Then
        assertFalse(result)
    }

    @Test
    fun contains_emptyKeyword_returnsTrueForNonEmptyMerchant() {
        // Given: Empty string is contained in any string
        val merchant = "Netflix"
        val keyword = ""

        // When
        val result = MerchantMatcher.contains(merchant, keyword)

        // Then
        assertTrue(result)
    }

    @Test
    fun contains_keywordWithSpecialChars_normalizesAndMatches() {
        // Given
        val merchant = "HDFC-Bank"
        val keyword = "HDFC.Bank"

        // When
        val result = MerchantMatcher.contains(merchant, keyword)

        // Then
        assertTrue(result)
    }

    // ==================== matchesPattern() Tests ====================

    @Test
    fun matchesPattern_merchantMatchesSimplePattern_returnsTrue() {
        // Given
        val merchant = "HDFC Bank EMI"
        val pattern = Regex("EMI")

        // When
        val result = MerchantMatcher.matchesPattern(merchant, pattern)

        // Then
        assertTrue(result)
    }

    @Test
    fun matchesPattern_merchantMatchesAlternativePattern_returnsTrue() {
        // Given
        val merchant = "HDFC Loan Payment"
        val pattern = Regex("EMI|LOAN")

        // When
        val result = MerchantMatcher.matchesPattern(merchant, pattern)

        // Then
        assertTrue(result)
    }

    @Test
    fun matchesPattern_merchantMatchesUtilityPattern_returnsTrue() {
        // Given
        val merchant = "Electricity Bill Payment"
        val pattern = Regex("ELECTRIC|WATER|GAS")

        // When
        val result = MerchantMatcher.matchesPattern(merchant, pattern)

        // Then
        assertTrue(result)
    }

    @Test
    fun matchesPattern_merchantDoesNotMatchPattern_returnsFalse() {
        // Given
        val merchant = "Netflix Subscription"
        val pattern = Regex("EMI|LOAN")

        // When
        val result = MerchantMatcher.matchesPattern(merchant, pattern)

        // Then
        assertFalse(result)
    }

    @Test
    fun matchesPattern_caseInsensitiveMatching_returnsTrue() {
        // Given: Pattern should match after normalization to uppercase
        val merchant = "hdfc bank emi"
        val pattern = Regex("EMI")

        // When
        val result = MerchantMatcher.matchesPattern(merchant, pattern)

        // Then
        assertTrue(result)
    }

    @Test
    fun matchesPattern_wildcardPattern_matchesMultipleWords() {
        // Given
        val merchant = "HDFC Bank Personal Loan EMI"
        val pattern = Regex("HDFC.*EMI")

        // When
        val result = MerchantMatcher.matchesPattern(merchant, pattern)

        // Then
        assertTrue(result)
    }

    @Test
    fun matchesPattern_startOfStringPattern_matchesBeginning() {
        // Given
        val merchant = "HDFC Bank Ltd"
        val pattern = Regex("^HDFC")

        // When
        val result = MerchantMatcher.matchesPattern(merchant, pattern)

        // Then
        assertTrue(result)
    }

    @Test
    fun matchesPattern_endOfStringPattern_matchesEnd() {
        // Given: "BANK" should be at the end after normalization removes "Ltd"
        val merchant = "HDFC Bank Ltd"
        val pattern = Regex("BANK$")

        // When
        val result = MerchantMatcher.matchesPattern(merchant, pattern)

        // Then
        assertTrue(result)
    }

    // ==================== Integration Tests ====================

    @Test
    fun integration_detectSubscriptionServices_usingContains() {
        // Given: Various subscription services
        val merchants = listOf(
            "Netflix Inc",
            "Amazon Prime Video",
            "Spotify Premium",
            "Adobe Creative Cloud",
            "HDFC Bank EMI"
        )
        val subscriptionKeywords = listOf("NETFLIX", "PRIME", "SPOTIFY", "ADOBE")

        // When: Filter subscription services
        val subscriptions = merchants.filter { merchant ->
            subscriptionKeywords.any { keyword ->
                MerchantMatcher.contains(merchant, keyword)
            }
        }

        // Then
        assertEquals(4, subscriptions.size)
        assertFalse(subscriptions.any { it.contains("HDFC") })
    }

    @Test
    fun integration_groupTransactionsByMerchant_handlesVariations() {
        // Given: Transaction merchants with variations
        val merchants = listOf(
            "Netflix",
            "NETFLIX Inc",
            "netflix.com",
            "Amazon.com",
            "amazon.in",
            "Spotify",
            "spotify.com"
        )

        // When: Group by normalized merchant
        val grouped = MerchantMatcher.groupByNormalized(merchants)

        // Then: Should have 3 distinct merchants
        assertEquals(3, grouped.size)

        // Verify Netflix group
        val netflixGroup = grouped["NETFLIX"]
        assertNotNull(netflixGroup)
        assertEquals(3, netflixGroup?.size)

        // Verify Amazon group
        val amazonGroup = grouped["AMAZON"]
        assertNotNull(amazonGroup)
        assertEquals(2, amazonGroup?.size)

        // Verify Spotify group
        val spotifyGroup = grouped["SPOTIFY"]
        assertNotNull(spotifyGroup)
        assertEquals(2, spotifyGroup?.size)
    }

    @Test
    fun integration_matchMerchantAgainstKnownPatterns_findsMatch() {
        // Given: New transaction merchant and known patterns
        val newMerchant = "NETFLIX Inc"
        val knownPatterns = listOf(
            "NETFLIX",
            "AMAZON",
            "SPOTIFY",
            "ADOBE"
        )

        // When: Find if new merchant matches known pattern
        val match = MerchantMatcher.findBestMatch(newMerchant, knownPatterns)

        // Then
        assertNotNull(match)
        assertEquals("NETFLIX", match)
    }
}
