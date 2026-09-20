package com.wapo.android.commons.util

import org.junit.Test
import java.util.Base64

class CryptoUtilTest {
    val KEY = "68566D59713374367639792442264529"

    val jsonData =
       ""

    @Test
    fun test_encrypt_json() {
        val key = CryptoUtil.generateKey(KEY)
        val encrypted = CryptoUtil.encrypt(jsonData, key)
        val text = Base64.getEncoder().encodeToString(encrypted)
        println(text)
        val bytes = Base64.getDecoder().decode(text)

        val decrypted = CryptoUtil.decrypt(bytes, key)
        print(decrypted)
    }
}
