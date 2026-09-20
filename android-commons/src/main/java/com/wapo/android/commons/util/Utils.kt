/*
 * Copyright (C) 2015 . The Washington Post. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wapo.android.commons.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.text.TextUtils
import com.wapo.android.commons.logger.R
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.Scanner
import java.util.regex.Pattern

/**
 * Created by muppallav on 2/12/15.
 */
@Deprecated("Use UtilsRepo instead", ReplaceWith("UtilsRepo", "com.wapo.android.commons.domain.UtilsRepo"))
object Utils {
    private const val NON_THIN = "[^iIl1\\.,']"
    private const val VERSION_NAME_UNKNOWN = "Unknown"
    private val KindleRegEx = Pattern.compile("(Kindle Fire|KF[A-Z]{2,})")

    fun isAmazonBuild(): Boolean {
        return "Amazon" == Build.MANUFACTURER
    }

    @JvmStatic
    fun inputStreamToString(inputStream: InputStream): String {
        val stream =
            if (BufferedInputStream::class.java.isInstance(inputStream)) inputStream else BufferedInputStream(
                inputStream
            )
        val scanner = Scanner(stream).useDelimiter("\\A")
        return if (scanner.hasNext()) scanner.next() else ""
    }

    @JvmStatic
    fun getAppVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            -1
        }
    }

    @JvmStatic
    fun getAppVersionName(context: Context): String {
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

    fun ellipsize(text: String, max: Int): String {
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

    fun share(
        context: Context,
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && shareReceiver != null) {
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

    fun getSharedUrl(url: String?): String {
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

    val isAmazonDevice: Boolean
        get() = "SD4930UR".equals(
            Build.MODEL,
            ignoreCase = true
        ) || "Amazon" == Build.MANUFACTURER && KindleRegEx.matcher(
            Build.MODEL
        ).matches()

    @JvmStatic
    fun isConnectedOrConnecting(ctx: Context): Boolean {
        return try {
            val cm =
                ctx.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ni = cm.activeNetworkInfo
            ni != null && ni.isConnectedOrConnecting
        } catch (e: Exception) {
            false
        }
    }

    @JvmStatic
    fun removeCurrencySignFromPrice(formattedPrice: String?): String {
        var result = "0.0"
        if (formattedPrice != null && !formattedPrice.isEmpty()) {
            val price = formattedPrice.replace("[^\\d.-]".toRegex(), "")
            if (!price.isEmpty()) {
                result = price
            }
        }
        return result
    }

    fun <T> coalesce(vararg items: T): T? {
        for (i in items) if (i != null) return i
        return null
    }

    fun isFreshInstall(context: Context): Boolean {
        return try {
            val firstInstallTime: Long = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
            val lastUpdateTime: Long = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
            firstInstallTime == lastUpdateTime
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            true
        }
    }

    @JvmStatic
    fun getActivity(context: Context?): android.app.Activity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
