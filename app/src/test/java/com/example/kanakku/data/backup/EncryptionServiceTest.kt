package com.example.kanakku.data.backup

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for EncryptionService
 *
 * Tests encryption/decryption roundtrip, password validation, and error handling
 */
class EncryptionServiceTest {

    private lateinit var encryptionService: EncryptionService

    @Before
    fun setup() {
        encryptionService = EncryptionService()
    }

    // ========== Encryption/Decryption Roundtrip Tests ==========

    @Test
    fun encrypt_decrypt_roundtrip_simpleString() {
        // Given
        val originalData = "Hello, World!".toByteArray()
        val password = "SecurePassword123"

        // When
        val encrypted = encryptionService.encrypt(originalData, password)
        val decrypted = encryptionService.decrypt(encrypted, password)

        // Then
        assertArrayEquals(originalData, decrypted)
    }

    @Test
    fun encrypt_decrypt_roundtrip_emptyData() {
        // Given
        val originalData = ByteArray(0)
        val password = "SecurePassword123"

        // When
        val encrypted = encryptionService.encrypt(originalData, password)
        val decrypted = encryptionService.decrypt(encrypted, password)

        // Then
        assertArrayEquals(originalData, decrypted)
    }

    @Test
    fun encrypt_decrypt_roundtrip_largeData() {
        // Given
        val originalData = ByteArray(10000) { it.toByte() }
        val password = "SecurePassword123"

        // When
        val encrypted = encryptionService.encrypt(originalData, password)
        val decrypted = encryptionService.decrypt(encrypted, password)

        // Then
        assertArrayEquals(originalData, decrypted)
    }

    @Test
    fun encrypt_decrypt_roundtrip_jsonData() {
        // Given
        val jsonData = """{"name":"John","amount":1000.50,"date":"2024-01-01"}"""
        val originalData = jsonData.toByteArray()
        val password = "MyBackupPassword2024!"

        // When
        val encrypted = encryptionService.encrypt(originalData, password)
        val decrypted = encryptionService.decrypt(encrypted, password)

        // Then
        assertArrayEquals(originalData, decrypted)
        assertEquals(jsonData, String(decrypted))
    }

    @Test
    fun encrypt_decrypt_roundtrip_specialCharacters() {
        // Given
        val originalData = "Special chars: !@#$%^&*()_+-=[]{}|;:',.<>?/~`".toByteArray()
        val password = "Password!@#123"

        // When
        val encrypted = encryptionService.encrypt(originalData, password)
        val decrypted = encryptionService.decrypt(encrypted, password)

        // Then
        assertArrayEquals(originalData, decrypted)
    }

    @Test
    fun encrypt_decrypt_roundtrip_unicodeData() {
        // Given
        val originalData = "Unicode: 你好世界 🚀 नमस्ते".toByteArray(Charsets.UTF_8)
        val password = "SecurePassword123"

        // When
        val encrypted = encryptionService.encrypt(originalData, password)
        val decrypted = encryptionService.decrypt(encrypted, password)

        // Then
        assertArrayEquals(originalData, decrypted)
        assertEquals("Unicode: 你好世界 🚀 नमस्ते", String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun encrypt_producesUniqueResults() {
        // Given
        val data = "Same data".toByteArray()
        val password = "SamePassword"

        // When - encrypt same data twice
        val encrypted1 = encryptionService.encrypt(data, password)
        val encrypted2 = encryptionService.encrypt(data, password)

        // Then - should produce different encrypted results (different salt/IV)
        assertFalse(encrypted1.salt.contentEquals(encrypted2.salt))
        assertFalse(encrypted1.iv.contentEquals(encrypted2.iv))
        assertFalse(encrypted1.ciphertext.contentEquals(encrypted2.ciphertext))
    }

    // ========== Password Validation Tests ==========

    @Test
    fun validatePassword_validPassword_returnsNull() {
        // Given
        val validPasswords = listOf(
            "Password123",
            "12345678",
            "VeryLongPasswordWithManyCharacters",
            "Pass!@#$123"
        )

        // When & Then
        validPasswords.forEach { password ->
            assertNull(
                "Password '$password' should be valid",
                encryptionService.validatePassword(password)
            )
        }
    }

    @Test
    fun validatePassword_tooShort_returnsError() {
        // Given
        val shortPasswords = listOf("", "a", "1234567")

        // When & Then
        shortPasswords.forEach { password ->
            val result = encryptionService.validatePassword(password)
            assertNotNull("Password '$password' should be invalid", result)
            assertTrue(
                "Error should mention length requirement",
                result?.contains("8 characters") == true || result?.contains("empty") == true
            )
        }
    }

    @Test
    fun validatePassword_exactlyEightCharacters_isValid() {
        // Given
        val password = "12345678"

        // When
        val result = encryptionService.validatePassword(password)

        // Then
        assertNull(result)
    }

    @Test
    fun validatePassword_blankPassword_returnsError() {
        // Given
        val blankPasswords = listOf("        ", "\t\t\t\t\t\t\t\t", "\n\n\n\n\n\n\n\n")

        // When & Then
        blankPasswords.forEach { password ->
            val result = encryptionService.validatePassword(password)
            assertNotNull("Blank password should be invalid", result)
            assertTrue(
                "Error should mention empty/blank requirement",
                result?.contains("empty") == true || result?.contains("blank") == true
            )
        }
    }

    @Test
    fun validatePassword_emptyString_returnsError() {
        // Given
        val password = ""

        // When
        val result = encryptionService.validatePassword(password)

        // Then
        assertNotNull(result)
        assertTrue(
            "Error should mention length or empty",
            result?.contains("8 characters") == true || result?.contains("empty") == true
        )
    }

    // ========== Error Handling Tests ==========

    @Test(expected = DecryptionException::class)
    fun decrypt_withWrongPassword_throwsException() {
        // Given
        val data = "Secret data".toByteArray()
        val correctPassword = "CorrectPassword"
        val wrongPassword = "WrongPassword"
        val encrypted = encryptionService.encrypt(data, correctPassword)

        // When & Then - should throw DecryptionException
        encryptionService.decrypt(encrypted, wrongPassword)
    }

    @Test
    fun decrypt_withWrongPassword_throwsDecryptionException() {
        // Given
        val data = "Secret data".toByteArray()
        val correctPassword = "CorrectPassword"
        val wrongPassword = "WrongPassword"
        val encrypted = encryptionService.encrypt(data, correctPassword)

        // When
        try {
            encryptionService.decrypt(encrypted, wrongPassword)
            fail("Should have thrown DecryptionException")
        } catch (e: DecryptionException) {
            // Then
            assertTrue(
                "Exception message should mention wrong password or corrupted data",
                e.message?.contains("wrong password") == true ||
                e.message?.contains("corrupted data") == true
            )
        }
    }

    @Test(expected = DecryptionException::class)
    fun decrypt_withCorruptedCiphertext_throwsException() {
        // Given
        val data = "Secret data".toByteArray()
        val password = "CorrectPassword"
        val encrypted = encryptionService.encrypt(data, password)

        // Corrupt the ciphertext
        val corruptedCiphertext = encrypted.ciphertext.clone()
        corruptedCiphertext[0] = (corruptedCiphertext[0] + 1).toByte()
        val corrupted = EncryptionService.EncryptedData(
            encrypted.salt,
            encrypted.iv,
            corruptedCiphertext
        )

        // When & Then - should throw DecryptionException
        encryptionService.decrypt(corrupted, password)
    }

    @Test(expected = DecryptionException::class)
    fun decrypt_withCorruptedIV_throwsException() {
        // Given
        val data = "Secret data".toByteArray()
        val password = "CorrectPassword"
        val encrypted = encryptionService.encrypt(data, password)

        // Corrupt the IV
        val corruptedIV = encrypted.iv.clone()
        corruptedIV[0] = (corruptedIV[0] + 1).toByte()
        val corrupted = EncryptionService.EncryptedData(
            encrypted.salt,
            corruptedIV,
            encrypted.ciphertext
        )

        // When & Then - should throw DecryptionException
        encryptionService.decrypt(corrupted, password)
    }

    @Test(expected = DecryptionException::class)
    fun decrypt_withCorruptedSalt_throwsException() {
        // Given
        val data = "Secret data".toByteArray()
        val password = "CorrectPassword"
        val encrypted = encryptionService.encrypt(data, password)

        // Corrupt the salt (changes derived key)
        val corruptedSalt = encrypted.salt.clone()
        corruptedSalt[0] = (corruptedSalt[0] + 1).toByte()
        val corrupted = EncryptionService.EncryptedData(
            corruptedSalt,
            encrypted.iv,
            encrypted.ciphertext
        )

        // When & Then - should throw DecryptionException
        encryptionService.decrypt(corrupted, password)
    }

    // ========== Serialization Tests ==========

    @Test
    fun encryptedData_serializeDeserialize_roundtrip() {
        // Given
        val data = "Test data".toByteArray()
        val password = "TestPassword"
        val encrypted = encryptionService.encrypt(data, password)

        // When
        val serialized = encrypted.toByteArray()
        val deserialized = EncryptionService.EncryptedData.fromByteArray(serialized)

        // Then
        assertEquals(encrypted, deserialized)
        assertArrayEquals(encrypted.salt, deserialized.salt)
        assertArrayEquals(encrypted.iv, deserialized.iv)
        assertArrayEquals(encrypted.ciphertext, deserialized.ciphertext)
    }

    @Test
    fun encryptedData_serializeDeserialize_canDecrypt() {
        // Given
        val originalData = "Test data for serialization".toByteArray()
        val password = "TestPassword"
        val encrypted = encryptionService.encrypt(originalData, password)

        // When - serialize, deserialize, then decrypt
        val serialized = encrypted.toByteArray()
        val deserialized = EncryptionService.EncryptedData.fromByteArray(serialized)
        val decrypted = encryptionService.decrypt(deserialized, password)

        // Then
        assertArrayEquals(originalData, decrypted)
    }

    @Test
    fun encryptedData_serialization_preservesDataIntegrity() {
        // Given
        val data = ByteArray(1000) { it.toByte() }
        val password = "TestPassword"
        val encrypted = encryptionService.encrypt(data, password)

        // When
        val serialized = encrypted.toByteArray()
        val deserialized = EncryptionService.EncryptedData.fromByteArray(serialized)
        val decrypted = encryptionService.decrypt(deserialized, password)

        // Then
        assertArrayEquals(data, decrypted)
    }

    // ========== Secure Password Generation Tests ==========

    @Test
    fun generateSecurePassword_hasDefaultLength() {
        // When
        val password = encryptionService.generateSecurePassword()

        // Then
        assertEquals(16, password.length)
    }

    @Test
    fun generateSecurePassword_customLength() {
        // Given
        val lengths = listOf(8, 12, 20, 32)

        // When & Then
        lengths.forEach { length ->
            val password = encryptionService.generateSecurePassword(length)
            assertEquals(length, password.length)
        }
    }

    @Test
    fun generateSecurePassword_isUnique() {
        // When
        val password1 = encryptionService.generateSecurePassword()
        val password2 = encryptionService.generateSecurePassword()

        // Then
        assertNotEquals(password1, password2)
    }

    @Test
    fun generateSecurePassword_passesValidation() {
        // When
        val password = encryptionService.generateSecurePassword()

        // Then
        assertNull(encryptionService.validatePassword(password))
    }

    @Test
    fun generateSecurePassword_containsVariedCharacters() {
        // When
        val password = encryptionService.generateSecurePassword(100)

        // Then - should contain mix of uppercase, lowercase, numbers, and special chars
        assertTrue("Should contain uppercase", password.any { it.isUpperCase() })
        assertTrue("Should contain lowercase", password.any { it.isLowerCase() })
        assertTrue("Should contain digits", password.any { it.isDigit() })
        assertTrue("Should contain special chars", password.any { !it.isLetterOrDigit() })
    }

    // ========== Edge Cases and Integration Tests ==========

    @Test
    fun encrypt_decrypt_multiplePasswords_independent() {
        // Given
        val data = "Shared data".toByteArray()
        val password1 = "Password1"
        val password2 = "Password2"

        // When
        val encrypted1 = encryptionService.encrypt(data, password1)
        val encrypted2 = encryptionService.encrypt(data, password2)

        // Then - each can be decrypted with correct password
        assertArrayEquals(data, encryptionService.decrypt(encrypted1, password1))
        assertArrayEquals(data, encryptionService.decrypt(encrypted2, password2))

        // And - cannot be decrypted with wrong password
        try {
            encryptionService.decrypt(encrypted1, password2)
            fail("Should not decrypt with wrong password")
        } catch (e: DecryptionException) {
            // Expected
        }
    }

    @Test
    fun encryptedData_equals_sameContent() {
        // Given
        val salt = ByteArray(32) { 1 }
        val iv = ByteArray(12) { 2 }
        val ciphertext = ByteArray(100) { 3 }

        val data1 = EncryptionService.EncryptedData(salt, iv, ciphertext)
        val data2 = EncryptionService.EncryptedData(
            salt.clone(),
            iv.clone(),
            ciphertext.clone()
        )

        // When & Then
        assertEquals(data1, data2)
        assertEquals(data1.hashCode(), data2.hashCode())
    }

    @Test
    fun encryptedData_notEquals_differentContent() {
        // Given
        val salt1 = ByteArray(32) { 1 }
        val salt2 = ByteArray(32) { 2 }
        val iv = ByteArray(12) { 2 }
        val ciphertext = ByteArray(100) { 3 }

        val data1 = EncryptionService.EncryptedData(salt1, iv, ciphertext)
        val data2 = EncryptionService.EncryptedData(salt2, iv, ciphertext)

        // When & Then
        assertNotEquals(data1, data2)
    }

    @Test
    fun encrypt_decrypt_veryLongPassword() {
        // Given
        val data = "Test data".toByteArray()
        val longPassword = "A".repeat(1000)

        // When
        val encrypted = encryptionService.encrypt(data, longPassword)
        val decrypted = encryptionService.decrypt(encrypted, longPassword)

        // Then
        assertArrayEquals(data, decrypted)
    }

    @Test
    fun encrypt_decrypt_passwordWithSpecialCharacters() {
        // Given
        val data = "Test data".toByteArray()
        val specialPassword = "P@ssw0rd!@#$%^&*()_+-=[]{}|;:',.<>?/~`"

        // When
        val encrypted = encryptionService.encrypt(data, specialPassword)
        val decrypted = encryptionService.decrypt(encrypted, specialPassword)

        // Then
        assertArrayEquals(data, decrypted)
    }
}
