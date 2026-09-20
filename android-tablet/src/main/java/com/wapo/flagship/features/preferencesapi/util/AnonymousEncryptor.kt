package com.wapo.flagship.features.preferencesapi.util

import android.util.Base64
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.config.domain.manager.ConfigManager
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object AnonymousEncryptor {
    private const val TAG = "AnonymousEncryptor"
    private const val ALGORITHM_HMAC = "HmacSHA256"

    fun tryEncrypt(message: String): String? {
        return try {
            encrypt(message)
        } catch (e: Exception) {
            Logger.e(TAG, "Error encrypting message: message=$message, error=${e.message}", e)
            null
        }
    }

    @Throws(
        IllegalStateException::class,
        NoSuchAlgorithmException::class,
        InvalidKeyException::class
    )
    fun encrypt(message: String): String {
        val publicKey = getPublicKey().ifEmpty {
            throw IllegalStateException("Encryption public key not found")
        }
        return getHmacStr(message, publicKey)
    }

    private fun getPublicKey(): String {
        val isStage =
            ConfigManager.getInstance().config.preferencesApiConfig.preferenceBaseUrl.contains("stage")
        return if (AppContextUtils.isDebuggableBuild() && isStage) {
            WapoSecDataProvider.preferencesPublicKeyStage
        } else {
            WapoSecDataProvider.preferencesPublicKeyProd
        }
    }

    private fun getHmacStr(data: String, publicKey: String): String {
        val secretKeySpec = SecretKeySpec(publicKey.toByteArray(), ALGORITHM_HMAC)
        val mac = Mac.getInstance(ALGORITHM_HMAC)
        mac.init(secretKeySpec)
        return Base64.encodeToString(mac.doFinal(data.toByteArray()), Base64.NO_WRAP)
    }
}