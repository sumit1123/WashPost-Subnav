package com.wapo.flagship.features.articles2.utils

import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.android.commons.util.CryptoUtil
import com.wapo.android.commons.util.Logger
import javax.crypto.spec.SecretKeySpec

/**
 * Helper class for decrypting AES-256-GCM encrypted article responses from the proxy API.
 */
object ArticleDecryptionHelper {
    private const val TAG = "ArticleDecryptionHelper"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val GCM_TAG_LENGTH = 128 // 16 bytes

    /**
     * Decrypts an encrypted response from the proxy API.
     *
     * @param iv The initialization vector in hex string format
     * @param authTag The authentication tag in hex string format
     * @param encrypted The encrypted data in hex string format
     * @return The decrypted JSON string, or null if decryption fails
     */
    fun decrypt(iv: String, authTag: String, encrypted: String): String? {
        return try {

            val encryptionKey = WapoSecDataProvider.articleEncryptionKey
            if (encryptionKey.isEmpty()) {
                Logger.e(TAG, "Article encryption key is not available")
                return null
            }
            // Convert hex strings to byte arrays
            val ivBytes = hexStringToByteArray(iv)
            val authTagBytes = hexStringToByteArray(authTag)
            val encryptedBytes = hexStringToByteArray(encrypted)
            val keyBytes = hexStringToByteArray(encryptionKey)

            // Create the secret key from raw bytes (not from UTF-8 of the hex string)
            val secretKey = SecretKeySpec(keyBytes, KEY_ALGORITHM)

            // Build message in the format CryptoUtil expects:
            // IV (first 12 bytes) || ciphertext+tag
            val message = ByteArray(ivBytes.size + encryptedBytes.size + authTagBytes.size)
            System.arraycopy(ivBytes, 0, message, 0, ivBytes.size)
            System.arraycopy(encryptedBytes, 0, message, ivBytes.size, encryptedBytes.size)
            System.arraycopy(
                authTagBytes,
                0,
                message,
                ivBytes.size + encryptedBytes.size,
                authTagBytes.size
            )

            val decrypted = CryptoUtil.decrypt(message, secretKey)

            // Normalize CryptoUtil's error string back to null + logging
            if (decrypted == null || decrypted == "something went wrong with decrypt") {
                Logger.e(TAG, "Failed to decrypt article response")
                null
            } else {
                decrypted
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to decrypt article response", e)
            null
        }
    }


    /**
     * Converts a hex string to a byte array.
     */
    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}

