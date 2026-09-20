/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.android.commons.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.security.*
import java.util.*
import java.util.Base64
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.cert.CertificateException


object CryptoUtil {

    private const val KEYGEN_ALGORITHM = "AES"
    private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"

    @Throws(Exception::class)
    fun encrypt(data: String, secretKey: SecretKey?): ByteArray? {
        try {
            // encoding format needs thought
            val clearTextbytes = data.toByteArray(charset("UTF-8"))
            val cipherInstance = Cipher.getInstance(CIPHER_ALGORITHM)
            cipherInstance.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedText = cipherInstance.doFinal(clearTextbytes)
            val iv = cipherInstance.iv
            val message = ByteArray(12 + clearTextbytes.size + 16)
            System.arraycopy(iv, 0, message, 0, 12)
            System.arraycopy(encryptedText, 0, message, 12, encryptedText.size)
            return message
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return null
    } // encrypt.


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @Throws(Exception::class)
    fun decrypt(encryptedText: ByteArray, secretKey: SecretKey?): String? {
        try {
            val cipherInstance = Cipher.getInstance(CIPHER_ALGORITHM)
            val params = GCMParameterSpec(128, encryptedText, 0, 12)
            cipherInstance.init(Cipher.DECRYPT_MODE, secretKey, params)
            val decryptedText =
                cipherInstance.doFinal(encryptedText, 12, encryptedText.size - 12)
            return String(decryptedText, Charsets.UTF_8)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return "something went wrong with decrypt"
    }

    fun encryptPreKitKat(iv:String, data: String, secretKey: SecretKey?): ByteArray {
        try {
            val cipher = Cipher.getInstance( "AES/CBC/PKCS5PADDING")
            val byteArray = iv.toByteArray().copyOfRange(0, 16)
            val ivParameterSpec = IvParameterSpec(byteArray)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey,ivParameterSpec)
            return cipher.doFinal(data.toByteArray())
        } catch (e: BadPaddingException) {
            throw RuntimeException(e)
        } catch (e: IllegalBlockSizeException) {
            throw RuntimeException(e)
        } catch (e: KeyStoreException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        }
    }

    fun decryptPreKitKat(iv:String ,encryptedText: ByteArray, secretKey: SecretKey?): String? {
        try {
            val cipher = Cipher.getInstance( "AES/CBC/PKCS5PADDING")
            val byteArray = iv.toByteArray().copyOfRange(0, 16)
            val ivParameterSpec = IvParameterSpec(byteArray)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
            val decryptedText = cipher.doFinal(encryptedText)
            return String(decryptedText, Charsets.UTF_8)

        } catch (e: BadPaddingException) {
            throw RuntimeException(e)
        } catch (e: IllegalBlockSizeException) {
            throw RuntimeException(e)
        } catch (e: KeyStoreException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        }
    }

    fun generateKey(key: String): SecretKey {
        return SecretKeySpec(key.toByteArray(Charsets.UTF_8), KEYGEN_ALGORITHM)
    }

}
