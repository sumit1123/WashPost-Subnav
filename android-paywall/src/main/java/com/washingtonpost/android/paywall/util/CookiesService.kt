package com.washingtonpost.android.paywall.util

import android.content.Context
import android.os.Build
import com.wapo.android.commons.util.Logger
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.WebSettings
import com.wapo.android.commons.logger.BuildConfig
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import com.washingtonpost.android.paywall.newdata.model.WpUser
import java.text.SimpleDateFormat
import java.util.*
/**
 * Class to manage WebView cookies.
 *
 * url:
 *      url of the site (ex: https://washingtonpost.com).
 *      Note: targetSdkVersion >= R(30) has restrictions on secure cookies. Either use the 'https:' scheme
 *      for the url or omit the 'secure' directive in the cookie values.
 *
 * domain:
 *      domain of the site (ex: washingtonpost.com).
 *      Note: domain should not start with any schema.
 */
class CookiesService(private val url: String, private val domain: String, context: Context) {
    private val TAG = CookiesService::class.java.simpleName
    private val cookieManager: CookieManager
    private val cookieExpiresPattern = "EEE, dd-MMM-yyyy HH:mm:ss z"
    private val path = "/"
    private val version = 1
    private val maxAge : Long = 63072000
    private val meterExpire = {
        getDate(System.currentTimeMillis() + (24 * 60 * 60 * 1000))
    }
    private val expires = {
        getDate(System.currentTimeMillis() + (maxAge * 1000))
    }
    private val currentDate = {
        SimpleDateFormat("yyyyMMdd").run {
            timeZone = TimeZone.getTimeZone("GMT")
            format(System.currentTimeMillis())
        }.toString()
    }

    var wasMeterCookieSet = false

    private val secure = "secure"
    private val httponly = "httponly"

    private var isSameSiteEnabled = false

    private fun getDate(date: Long): String {
        val date = Date(date)
        return SimpleDateFormat(cookieExpiresPattern, Locale.getDefault()).run {
            timeZone = TimeZone.getTimeZone("GMT")
            format(date)
        }
    }

    init {
        require(url.isNotEmpty()) { "url is empty." }
        require(domain.isNotEmpty()) { "domain is empty." }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(context)
        }
        cookieManager = CookieManager.getInstance()
    }

    private fun setCookie(value: String) {
        log("setCookie -> $url, $value")
        if (value == null) {
            return
        }
        cookieManager.setCookie(url, value)
    }

    fun clearCookies() {
        log("clearCookies")
        val cookies = cookieManager.getCookie(url)
        if (cookies.isNullOrEmpty()) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null)
        } else {
            cookieManager.removeAllCookie()
        }
    }

    /**
     * Remove session cookies from webview
     */
    fun clearSessionCookies() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeSessionCookies(null)
        } else {
            cookieManager.removeSessionCookie()
        }
    }

    fun printCookies() {
        // Note: Not reliable and getCookie method is returning null or missing cookies sometimes.
        // But WebView is working fine.
        val sb = StringBuilder("Cookies: ");
        val cookies = cookieManager.getCookie(url)
        if (!cookies.isNullOrEmpty()) {
            sb.append(cookies + "\n")
            cookies.split(";")?.forEach {
                sb.append(it + "\n")
            }
        }
        log(sb.toString())
    }

    private fun log(msg: String) {
        if (BuildConfig.DEBUG) {
            Logger.d(TAG, msg)
        }
    }

    fun prepareCookieManager(user: WpUser?, context: Context, isSameSiteEnabled: Boolean) {
        this.isSameSiteEnabled = isSameSiteEnabled
        setWebViewCookie(context)
        setFeatureJwtCookie(context)
        if (user != null) {
            val sameSiteString = if (isSameSiteEnabled) getSameSite(context) else ""
            val cookieSubSequence = "path=$path; domain=$domain; max-age=$maxAge; expires=${expires()}; $sameSiteString $secure"
            setCookie(value = "wapo_login_id=${user.uuid}; $cookieSubSequence")
            setCookie(value = "sec_wapo_login_id=${user.uuid}; $cookieSubSequence; $httponly")
            setCookie(value = "wapo_secure_login_id=${user.secureLoginID}; Version=$version; $cookieSubSequence")
            setCookie(value = "sec_wapo_secure_login_id=${user.secureLoginID}; Version=$version; $cookieSubSequence; $httponly")
            setActMgmtCookie(context)
            setRctCookie(context)
            printCookies()
        }
    }

    /**
     * Set this cookie so that webviews know the subscription status of a user. Without this, user may end up being
     * redirected to the WPAA when opening Archive Page or Article in a webview.
     */
    private fun setActMgmtCookie(context: Context) {
        if(PaywallService.getInstance() == null) {
            return
        }
        PaywallService.getConnector()?.subAcctMgmt?.let {
            val sameSiteString = if (isSameSiteEnabled) getSameSite(context) else ""
            val cookieSubSequence = "path=$path; domain=$domain; max-age=$maxAge; expires=${expires()}; $sameSiteString $secure"
            setCookie("wapo_actmgmt=$it; $cookieSubSequence")
        }
    }

    /**
     * This cookie allows a user to remain in a particular allocation for the RCT test.
     */
    private fun setRctCookie(context: Context) {
        if(PaywallService.getInstance() == null) {
            return
        }
        PaywallPrefHelper.getPrefTetroRctCookie()?.let {
            val sameSiteString = if (isSameSiteEnabled) getSameSite(context) else ""
            val cookieSubSequence = "path=$path; domain=$domain; max-age=$maxAge; expires=${expires()}; $sameSiteString $secure"
            setCookie("wp_ak_v_mab=$it; $cookieSubSequence")
        }
    }

    /**
     * This cookie (wp_wv) is set in webviews so Site can identify this article as a webview article from Apps.
     * (t_android-classic) - used by site to determine platform webview is running on.
     */
    fun setWebViewCookie(context: Context) {
        val sameSiteString = if (isSameSiteEnabled) getSameSite(context) else ""
        val cookieSubSequence = "path=$path; domain=$domain; max-age=$maxAge; expires=${expires()}; $sameSiteString $secure"
        setCookie(value = "wp_wv=1|${currentDate()}|t_android-classic; $cookieSubSequence")
    }

    /**
     * Sets the feature JWT as a cookie (wp_f_token) so webviews can pass feature entitlement info
     * to Akamai via cookie instead of the x-set-wp-f-token header.
     */
    fun setFeatureJwtCookie(context: Context) {
        val jwt = PaywallService.getInstance()?.getResolvedFeatureJwt()
        val sameSiteString = if (isSameSiteEnabled) getSameSite(context) else ""
        if (jwt.isNullOrEmpty()) {
            val expireCookieSubSequence = "path=$path; domain=$domain; max-age=0; $sameSiteString $secure"
            setCookie(value = "wp_f_token=; $expireCookieSubSequence")
            return
        }
        val cookieSubSequence = "path=$path; domain=$domain; max-age=$maxAge; expires=${expires()}; $sameSiteString $secure"
        setCookie(value = "wp_f_token=$jwt; $cookieSubSequence")
    }

    fun setMeterHitCookie(context: Context) {
        cookieManager.removeExpiredCookiesCompat()
        val sameSiteString = if (isSameSiteEnabled) getSameSite(context) else ""
        val cookieSubSequence = "path=$path; domain=$domain; max-age=$maxAge; expires=${meterExpire()}; $sameSiteString $secure"
        setCookie(value = "wp_appmc=1|${currentDate()}; $cookieSubSequence")
        wasMeterCookieSet = true

        Logger.d("CookieService", "wp_appmc cookie has been set")
    }

    /**
     * These are session cookies set for Anonymous Subs (IAP / Not signed in)
     */
    fun setAnonymousUserCookie(context: Context) {
        // TODO Set the cookie once pwapi_token is defined and getting it from the backend (as part of the verify call).
    }

    companion object {

        private const val SAME_SITE_MAJOR_VERSION = 67
        private const val SAME_SITE_NONE = "SameSite=None;"

        @JvmStatic
        fun getSameSite(context: Context): String {
            return when {
                isChromiumBased(context) && isChromiumVersionCompatible(context, SAME_SITE_MAJOR_VERSION) -> SAME_SITE_NONE
                else -> ""
            }
        }

        @JvmStatic
        fun isChromiumBased(context: Context) : Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return false
            }

            val userAgent = WebSettings.getDefaultUserAgent(context)
            val regex = Regex("Chrom(e|ium)")
            val isChromium = userAgent.contains(regex)
            Logger.d("Chromium", "userAgent : $userAgent")
            Logger.d("Chromium", "isChromium : $isChromium")
            return isChromium
        }

        @JvmStatic
        fun isChromiumVersionCompatible(context: Context, major:Int) : Boolean {
            // https://www.chromium.org/updates/same-site/incompatible-clients
            // https://www.chromium.org/updates/same-site
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return false
            }
            val userAgent = WebSettings.getDefaultUserAgent(context)
            val regex = Regex("(?<=Chrom[^ \\/]\\/)(\\d+)")
            val versionResult = regex.find(userAgent)
            var isCompatible = false
            versionResult?.value?.apply {
                val version = this.toIntOrNull()
                isCompatible = version != null && version >= major
            }
            Logger.d("Chromium", "isCompatible : $isCompatible")
            return isCompatible
        }

        fun CookieManager.removeExpiredCookiesCompat() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                this.removeExpiredCookie()
            }
        }
    }
}
