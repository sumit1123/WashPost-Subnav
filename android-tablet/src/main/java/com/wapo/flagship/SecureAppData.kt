// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship

import android.content.Context
import android.util.Base64
import com.getkeepsafe.relinker.ReLinker
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.android.commons.util.CryptoUtil
import com.wapo.android.commons.util.Logger

object SecureAppData {
    /**
     * Initialize JNI Library
     */
    fun loadLibrary(context: Context) {
        ReLinker
            .log { Logger.d(TAG, it) }
            .loadLibrary(context, "SecureLib")
    }

    /**
     * Required JNI functions
     */
    private external fun getCipherKey(): String

    private external fun getEncryptedData(): String

    val TAG = "SecureAppData"

    /**
     * Load App required data.
     */
    fun loadAppSecureData(provider: WapoSecDataProvider) {
        val decrypted = decryptSecureData(getEncryptedData())
        decrypted?.let {
            provider.loadWapoSecData(it)
        }
    }

    fun decryptSecureData(encryptedData: String): String? {
        val keyText = getCipherKey()
        val key = CryptoUtil.generateKey(keyText)
        val bytes = Base64.decode(encryptedData, Base64.DEFAULT)
        return CryptoUtil.decrypt(bytes, key)
    }
}
