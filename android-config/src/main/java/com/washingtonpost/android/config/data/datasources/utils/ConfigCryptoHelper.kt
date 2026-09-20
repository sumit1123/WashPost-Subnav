package com.washingtonpost.android.config.data.datasources.utils

import android.content.res.Resources
import com.wapo.android.commons.util.EncryptionUtils
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.config.R

class ConfigCryptoHelper(
    resources: Resources,
) {
    private val encryptionKey = resources.getString(R.string.configEncryptKey)
    private val encryptionKeyAsBytes = EncryptionUtils.hexStringToByteArray(encryptionKey)

    fun decryptValue(value: String): String {
        return try {
            val valueAsBytes = EncryptionUtils.hexStringToByteArray(value)
            val decryptedBytes = EncryptionUtils.decrypt(encryptionKeyAsBytes, valueAsBytes)
            String(decryptedBytes)
        } catch (e: Exception) {
            Logger.e(TAG, "error decrypting value")
            value
        }
    }

    fun encryptValue(value: String): String {
        val valueAsBytes = EncryptionUtils.hexStringToByteArray(value)
        val encryptedAsBytes = EncryptionUtils.encrypt(encryptionKeyAsBytes, valueAsBytes)
        return String(encryptedAsBytes)
    }

    companion object {
        private const val TAG = "ConfigCryptoHelper"
    }
}
