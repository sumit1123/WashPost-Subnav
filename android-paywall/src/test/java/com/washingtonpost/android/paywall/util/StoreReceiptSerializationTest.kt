package com.washingtonpost.android.paywall.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.junit.Assert
import org.junit.Test

class StoreReceiptSerializationTest {

    val receipt = "{\n" +
            "    \"receiptId\": \"H5vb90ZK3ZfwrxcZpwllP_nvwOxouG61p0cY15LjJuI=:3:11\",\n" +
            "    \"productId\": \"M1-R\",\n" +
            "    \"itemType\": \"SUBSCRIPTION\",\n" +
            "    \"purchaseDate\": \"Sat Jun 17 15:59:09 EDT 2017\"\n" +
            "}"

    data class StoreReceipt(
        val receiptId: String?,
        val productId: String?,
    )

    data class StoreReceiptObfuscated(
        val r: String?,
        val p: String?,
    )

    data class StoreReceiptObfuscatedWithSerializeSupport(
        @SerializedName("receiptId") val r: String?,
        @SerializedName("productId") val s: String?,
    )

    @Test
    fun testGSonStoreReceiptConversion() {
        // Convert json to the StoreReceipt class
        val type = object : TypeToken<StoreReceipt?>() {}.type
        val receipt = Gson().fromJson<StoreReceipt>(receipt, type)
        // Fail if productId is null
        Assert.assertTrue(receipt.productId != null)
        // Convert class to json to make sure
        val receiptJson = Gson().toJson(receipt)
        println("StoreReceipt Json=$receiptJson")
        println(">>>>> StoreReceipt Passed.")
    }

    @Test
    fun testGSonStoreReceiptObfuscatedConversion() {
        // Convert json to the StoreReceipt class
        val type = object : TypeToken<StoreReceiptObfuscated?>() {}.type
        val receipt = Gson().fromJson<StoreReceiptObfuscated>(receipt, type)
        // Fail if productId is null
        Assert.assertTrue(receipt.p == null)
        // Convert class to json to make sure
        val receiptJson = Gson().toJson(receipt)
        println("StoreReceiptObfuscated Json=$receiptJson")
        println(">>>>> StoreReceiptObfuscated Passed.")
    }

    @Test
    fun testGSonStoreReceiptObfuscatedWithSerializeSupportConversion() {
        // Convert json to the StoreReceipt class
        val type = object : TypeToken<StoreReceiptObfuscatedWithSerializeSupport?>() {}.type
        val receipt = Gson().fromJson<StoreReceiptObfuscatedWithSerializeSupport>(receipt, type)
        // Fail if productId is null
        Assert.assertTrue(receipt.s != null)
        // Convert class to json to make sure
        val receiptJson = Gson().toJson(receipt)
        println("StoreReceiptObfuscatedWithSerializeSupport Json=$receiptJson")
        println(">>>>> StoreReceiptObfuscatedWithSerializeSupport Passed.")
    }
}