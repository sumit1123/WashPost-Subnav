/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.android.commons.util

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import android.view.accessibility.CaptioningManager
import android.webkit.WebSettings
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import com.wapo.android.commons.logger.BuildConfig
import com.wapo.android.commons.logger.R
import com.wapo.android.commons.util.Utils.isConnectedOrConnecting

/**
 * Singleton Utils class for [Context] related operations.
 * One time initialization with an application context is required.
 */
object AppContextUtils {
    var isInitialized = false
    var appContext: Context? = null
        private set
    var appName: String? = null
        private set
    lateinit var appApiUserAgent: String
        private set
    lateinit var appWebUserAgent: String
        private set

    /**
     * The application class can call this method in the app resume.
     */
    @Synchronized
    fun init(appContext: Context, appName: String) {
        if (isInitialized) return
        this.appContext = appContext
        this.appName = appName
        this.appApiUserAgent = buildUserAgent(false)
        this.appWebUserAgent = buildUserAgent(true)
        this.isInitialized = true
    }

    fun isTablet(): Boolean {
        checkContext()
        return DeviceUtils.isTablet(appContext)
    }

    fun isVideoCaptionsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val service: Any? = appContext?.getSystemService(Context.CAPTIONING_SERVICE)
            if (service is CaptioningManager) {
                return service.isEnabled
            }
        }
        return false
    }

    fun getSmallestScreenWidth(): Int {
        val w = appContext?.resources?.displayMetrics?.widthPixels ?: 0
        val h = appContext?.resources?.displayMetrics?.heightPixels ?: 0
        return if (w < h) w else h
    }

    fun getLargestScreenWidth(): Int {
        val w = appContext?.resources?.displayMetrics?.widthPixels ?: 0
        val h = appContext?.resources?.displayMetrics?.heightPixels ?: 0
        return if (w > h) w else h
    }

    fun getScreenHeight(): Int {
        return appContext?.resources?.displayMetrics?.heightPixels ?: 0
    }

    fun isConnectingOrConnected(): Boolean {
        return appContext?.let { isConnectedOrConnecting(it) } ?: false
    }

    fun getDeviceDpi(): Int {
        return appContext?.resources?.displayMetrics?.densityDpi ?: 0
    }

    fun getDeviceWidthPixels(): Int {
        return appContext?.resources?.displayMetrics?.widthPixels ?: 0
    }

    fun getDeviceDensity(): Float {
        return appContext?.resources?.displayMetrics?.density ?: 0f
    }

    fun getDeviceWidthInDp(): Float {
        return getDeviceDensity().takeIf { it > 0 }?.let { getDeviceWidthPixels() / it } ?: 0f
    }

    private fun getWebSettingsUserAgent(): String? {
        return try {
            WebSettings.getDefaultUserAgent(appContext)
        } catch (e: java.lang.Exception) {
            getSystemsHttpAgent()
        }
    }

    fun getSystemsHttpAgent(): String? {
        return System.getProperty("http.agent")
    }

    fun toastError(@StringRes msgStringResId: Int) {
        toastError(appContext?.getString(msgStringResId))
    }

    fun toastError(msg: String?) {
        msg ?: return
        if (!isConnectingOrConnected()) {
            showToast(appContext?.getString(R.string.audio_error_offline))
        } else {
            showToast(msg)
        }
    }

    fun showToast(@StringRes msgStringResId: Int) {
        showToast(appContext?.getString(msgStringResId))
    }

    fun showToast(msg: String?) {
        msg ?: return
        Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
    }

    fun areNotificationEnabled(): Boolean? {
        val context = appContext ?: return null
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun getUniqueDeviceId(): String {
        return DeviceUtils.getUniqueDeviceId(appContext)
    }

    fun isBetaBuild(): Boolean {
        return BuildConfig.BUILD_TYPE == "beta"
    }

    fun isDebugBuild(): Boolean {
        return BuildConfig.BUILD_TYPE == "debug"
    }

    fun isDebuggableBuild(): Boolean {
        return isDebugBuild() || isBetaBuild()
    }

    fun calculateArticleMargin(): Int {
        // calculations are matching iOS calculations for article body width (design wants to change this in the future)
        // take the width of the view, multiply it by .3125, subtract 160, and then use that or 16, whichever is bigger
        val screenWidthPx = Resources.getSystem().displayMetrics.widthPixels
        val density = appContext?.resources?.displayMetrics?.density
        if (density == null || density <= 0) {
            return 0
        }
        val screenWidthDp = screenWidthPx / density
        val marginDp = maxOf(screenWidthDp * 0.3125 - 160, 16.0)
        val margin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginDp.toFloat(),
            appContext?.resources?.displayMetrics
        ).toInt()

        return margin
    }

    private fun buildUserAgent(isWeb: Boolean): String {
        val pkgInfo = appContext?.let { it.packageManager?.getPackageInfo(it.packageName, 0) }
        return "${if (isWeb) getWebSettingsUserAgent() else getSystemsHttpAgent()} Classic/${pkgInfo?.versionName}#${pkgInfo?.versionCode} android/${Build.VERSION.SDK_INT} ${if (isTablet()) "tablet" else "phone"} $appName"
    }

    private fun checkContext() {
        appContext ?: throw IllegalStateException("appContext should be initialized first!")
    }
}