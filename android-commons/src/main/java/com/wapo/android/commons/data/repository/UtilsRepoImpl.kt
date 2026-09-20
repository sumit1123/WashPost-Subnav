/*
 * Copyright (C) 2026 . The Washington Post. All rights reserved.
 */
package com.wapo.android.commons.data.repository

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.text.TextUtils
import com.wapo.android.commons.domain.BuildConfigProvider
import com.wapo.android.commons.domain.BuildProviderRepo
import com.wapo.android.commons.domain.UtilsRepo
import com.wapo.android.commons.logger.R
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.Scanner
import java.util.regex.Pattern
import javax.inject.Inject

class UtilsRepoImpl @Inject constructor(
    private val context: Context,
    private val buildProviderRepo: BuildProviderRepo,
    private val buildConfigProvider: BuildConfigProvider
): UtilsRepo {
    private val NON_THIN = "[^iIl1\\.,']"
    private val VERSION_NAME_UNKNOWN = "Unknown"
    private val KindleRegEx = Pattern.compile("(Kindle Fire|KF[A-Z]{2,})")

    override fun isAmazonBuild(): Boolean {
        return "Amazon" == buildProviderRepo.getManufacturer()
    }

    override fun inputStreamToString(inputStream: InputStream): String {
        val stream =
            if (BufferedInputStream::class.java.isInstance(inputStream)) inputStream else BufferedInputStream(
                inputStream
            )
        val scanner = Scanner(stream).useDelimiter("\\A")
        return if (scanner.hasNext()) scanner.next() else ""
    }

    override fun getAppVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            -1
        }
    }

    override fun getAppVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: VERSION_NAME_UNKNOWN
        } catch (e: PackageManager.NameNotFoundException) {
            VERSION_NAME_UNKNOWN
        }
    }

    private fun textWidth(str: String): Int {
        return str.length - str.replace(NON_THIN.toRegex(), "").length / 2
    }

    override fun ellipsize(text: String, max: Int): String {
        if (textWidth(text) <= max) return text
        var end = text.lastIndexOf(' ', max - 3)
        if (end == -1) return text.substring(0, max - 3) + "..."
        var newEnd = end
        do {
            end = newEnd
            newEnd = text.indexOf(' ', end + 1)
            if (newEnd == -1) newEnd = text.length
        } while (textWidth(text.substring(0, newEnd) + "...") < max)
        return text.substring(0, end) + "..."
    }

    @SuppressLint("NewApi")
    override fun share(
        url: String,
        articleTitle: String,
        title: String?,
        subjectMaxLength: Int,
        shareReceiver: Intent?,
        sectionName: String
    ) {
        var url = url
        val share = Intent(Intent.ACTION_SEND)
        share.type = "text/plain"
        var subject = ""
        if (!TextUtils.isEmpty(sectionName)) {
            subject = if (!TextUtils.isEmpty(articleTitle)) {
                "$sectionName | $articleTitle"
            } else {
                sectionName
            }
        } else if (!TextUtils.isEmpty(articleTitle)) {
            subject = articleTitle
        }
        if (subjectMaxLength > 0) {
            url = ellipsize(url, subjectMaxLength)
        }
        share.putExtra(
            Intent.EXTRA_SUBJECT,
            String.format(context.resources.getString(R.string.share_email_subject), subject)
        )
        share.putExtra(Intent.EXTRA_TEXT, url)
        val openInChooser: Intent
        openInChooser =
            if (buildProviderRepo.getVersionSdkInt() >= buildProviderRepo.getVersionCodesLollipopMR1() && shareReceiver != null) {
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    shareReceiver,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                Intent.createChooser(share, title, pendingIntent.intentSender)
            } else {
                Intent.createChooser(share, title)
            }
        context.startActivity(openInChooser)
    }

    override fun getSharedUrl(url: String?): String {
        val pattern = Pattern.compile("https?://.*", Pattern.CASE_INSENSITIVE)
        if (url != null && !url.isEmpty() && pattern.matcher(url).matches()) {
            return url
        }
        val sb = StringBuilder()
        val shareBaseUrl = "http://www.washingtonpost.com"
        sb.append(shareBaseUrl)
        if (url != null && !url.startsWith("/") && !shareBaseUrl.endsWith("/")) {
            sb.append("/")
        }
        sb.append(url)
        return sb.toString()
    }

    override val isAmazonDevice: Boolean
        get() = "SD4930UR".equals(
            buildProviderRepo.getModel(),
            ignoreCase = true
        ) || "Amazon" == buildProviderRepo.getManufacturer() && KindleRegEx.matcher(
            buildProviderRepo.getModel()
        ).matches()

    override fun isConnectedOrConnecting(): Boolean {
        return try {
            val cm =
                context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ni = cm.activeNetworkInfo
            ni != null && ni.isConnectedOrConnecting
        } catch (e: Exception) {
            false
        }
    }

    override fun removeCurrencySignFromPrice(formattedPrice: String?): String {
        var result = "0.0"
        if (formattedPrice != null && !formattedPrice.isEmpty()) {
            val price = formattedPrice.replace("[^\\d.-]".toRegex(), "")
            if (!price.isEmpty()) {
                result = price
            }
        }
        return result
    }

    override fun <T> coalesce(vararg items: T): T? {
        for (i in items) if (i != null) return i
        return null
    }

    override fun isFreshInstall(): Boolean {
        return try {
            val firstInstallTime: Long = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
            val lastUpdateTime: Long = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
            firstInstallTime == lastUpdateTime
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            true
        }
    }

    override fun isProductFlavorAmazon(): Boolean {
        return buildConfigProvider.getStoreType() == "amazon"
    }

    override fun getManufacturerValue(): String {
        return buildProviderRepo.getManufacturer()
    }

    override fun getOsVersion(): Int {
        return buildProviderRepo.getVersionSdkInt()
    }

    override fun isChromebook(): Boolean {
        return context.packageManager.hasSystemFeature("org.chromium.arc") ||
                context.packageManager.hasSystemFeature("org.chromium.arc.device_management")
    }
}
