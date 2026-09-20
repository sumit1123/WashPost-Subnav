/*
 * Copyright (C) 2026 . The Washington Post. All rights reserved.
 */
package com.wapo.android.commons.domain

import android.content.Intent
import java.io.InputStream

interface UtilsRepo {
    fun isAmazonBuild(): Boolean
    fun inputStreamToString(inputStream: InputStream): String
    fun getAppVersionCode(): Int
    fun getAppVersionName(): String
    fun ellipsize(text: String, max: Int): String
    fun share(
        url: String,
        articleTitle: String,
        title: String?,
        subjectMaxLength: Int,
        shareReceiver: Intent?,
        sectionName: String
    )
    fun getSharedUrl(url: String?): String
    val isAmazonDevice: Boolean
    fun isConnectedOrConnecting(): Boolean
    fun removeCurrencySignFromPrice(formattedPrice: String?): String
    fun <T> coalesce(vararg items: T): T?
    fun isFreshInstall(): Boolean
    fun isProductFlavorAmazon(): Boolean
    fun getManufacturerValue(): String
    fun getOsVersion(): Int
    fun isChromebook(): Boolean
}
