package com.example.kanakku.data.backup

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Service for encrypting and decrypting backup files using AES-256-GCM
 * with password-based key derivation (PBKDF2) and Android Keystore.
 *
 * Encryption process:
 * 1. User password is used to derive a 256-bit key using PBKDF2
 * 2. Data is encrypted with AES-256-GCM using the derived key
 * 3. Salt, IV, and encrypted data are packaged together
 *
 * Security features:
 * - AES-256-GCM for authenticated encryption
 * - PBKDF2 with 100,000 iterations for key derivation
 * - Random salt and IV for each encryption
 * - Android Keystore for secure key storage (optional)
 */
class EncryptionService {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val PBKDF2_ITERATIONS = 100_000
        private const val PBKDF2_KEY_LENGTH = 256
        private const val SALT_LENGTH = 32
    }

    /**
     * Result of encryption operation containing all necessary data for decryption
     */
    data class EncryptedData(
        val salt: ByteArray,
        val iv: ByteArray,
        val ciphertext: ByteArray
    ) {
        /**
         * Serialize to byte array for storage
         * Format: [salt_length][salt][iv_length][iv][ciphertext]
         */
        fun toByteArray(): ByteArray {
            val buffer = ByteBuffer.allocate(
                4 + salt.size + 4 + iv.size + ciphertext.size
            )
            buffer.putInt(salt.size)
            buffer.put(salt)
            buffer.putInt(iv.size)
            buffer.put(iv)
            buffer.put(ciphertext)
            return buffer.array()
        }

        companion object {
            /**
             * Deserialize from byte array
             */
            fun fromByteArray(data: ByteArray): EncryptedData {
                val buffer = ByteBuffer.wrap(data)
                val saltLength = buffer.int
                val salt = ByteArray(saltLength)
                buffer.get(salt)
                val ivLength = buffer.int
                val iv = ByteArray(ivLength)
                buffer.get(iv)
                val ciphertext = ByteArray(buffer.remaining())
                buffer.get(ciphertext)
                return EncryptedData(salt, iv, ciphertext)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptedData

            if (!salt.contentEquals(other.salt)) return false
            if (!iv.contentEquals(other.iv)) return false
            if (!ciphertext.contentEquals(other.ciphertext)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = salt.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            result = 31 * result + ciphertext.contentHashCode()
            return result
        }
    }

    /**
     * Encrypt data with the given password
     *
     * @param data The data to encrypt
     * @param password The password to use for encryption
     * @return EncryptedData containing salt, IV, and ciphertext
     * @throws EncryptionException if encryption fails
     */
    fun encrypt(data: ByteArray, password: String): EncryptedData {
        try {
            // Generate random salt for PBKDF2
            val salt = ByteArray(SALT_LENGTH)
            SecureRandom().nextBytes(salt)

            // Derive key from password using PBKDF2
            val key = deriveKeyFromPassword(password, salt)

            // Generate random IV for GCM
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)

            // Encrypt data
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
            val ciphertext = cipher.doFinal(data)

            return EncryptedData(salt, iv, ciphertext)
        } catch (e: Exception) {
            throw EncryptionException("Encryption failed", e)
        }
    }

    /**
     * Decrypt data with the given password
     *
     * @param encryptedData The encrypted data containing salt, IV, and ciphertext
     * @param password The password to use for decryption
     * @return The decrypted data
     * @throws DecryptionException if decryption fails (wrong password or corrupted data)
     */
    fun decrypt(encryptedData: EncryptedData, password: String): ByteArray {
        try {
            // Derive key from password using the stored salt
            val key = deriveKeyFromPassword(password, encryptedData.salt)

            // Decrypt data
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
            return cipher.doFinal(encryptedData.ciphertext)
        } catch (e: Exception) {
            throw DecryptionException("Decryption failed - wrong password or corrupted data", e)
        }
    }

    /**
     * Derive a 256-bit AES key from password using PBKDF2
     */
    private fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            PBKDF2_KEY_LENGTH
        )
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, KEY_ALGORITHM)
    }

    /**
     * Validate password strength
     * @return null if valid, error message if invalid
     */
    fun validatePassword(password: String): String? {
        return when {
            password.length < 8 -> "Password must be at least 8 characters"
            password.isBlank() -> "Password cannot be empty"
            else -> null
        }
    }

    /**
     * Generate a secure random password (for testing or fallback)
     */
    fun generateSecurePassword(length: Int = 16): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val random = SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}

/**
 * Exception thrown when encryption fails
 */
class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when decryption fails
 */
class DecryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
