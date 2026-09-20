package com.wapo.android.commons.util

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.net.URL

/**
 * Helper that parses the given url and has operations related to [java.net.URL] and
 * [android.net.Uri]
 */

@Parcelize
class URLParser(
    val rawLink: String?,
    var isExternalOrigin: Boolean = false,
    var isPushOriginated: Boolean = false,
    var isWidgetOriginated: Boolean = false,
    var referrer: String? = null
) : Parcelable {
    constructor(link: String) : this(link, false, false, false, null)

    @IgnoredOnParcel
    val url: URL? = try {
        URL(getHttpsLink(rawLink))
    } catch (e: Exception) {
        null
    }

    @IgnoredOnParcel
    val uri: Uri? = try {
        Uri.parse(getHttpsLink(rawLink))
    } catch (e: Exception) {
        null
    }

    @IgnoredOnParcel
    val urlString: String? = url?.toString()

    private fun getHttpsLink(link: String?): String? {
        link ?: return null
        return when (link.substring(0, link.indexOf(":"))) {
            WASHPOST_SCHEME -> "${HTTPS_SCHEME}${link.substring(WASHPOST_SCHEME.length)}"
            AIRSHIP_SCHEME -> "${HTTPS_SCHEME}${link.substring(AIRSHIP_SCHEME.length)}"
            WP_ANDROID_SCHEME -> "${HTTPS_SCHEME}${link.substring(WP_ANDROID_SCHEME.length)}"
            HTTP_SCHEME -> "${HTTPS_SCHEME}${link.substring(HTTP_SCHEME.length)}"
            else -> link
        }
    }

    fun isWashPostScheme(): Boolean = rawLink?.startsWith(WASHPOST_SCHEME) == true

    fun isAirshipScheme(): Boolean = rawLink?.startsWith(AIRSHIP_SCHEME) == true

    fun isValid(): Boolean = url != null

    fun getScheme(): String = url?.protocol ?: ""

    fun getAuthority(): String = url?.authority ?: ""

    fun getDomain(): String = url?.host ?: ""

    fun getPort(): Int = url?.port ?: -1

    fun getPath(): String = url?.path ?: ""

    fun getParameters(): String = url?.query ?: ""

    fun getParametersBundle(): Bundle? {
        val currentUri = uri ?: return null
        val paramNames = currentUri.queryParameterNames
        if (paramNames.isEmpty()) return null
        val bundle = Bundle()

        for (name in paramNames) {
            if (name.isBlank()) continue
            val value = currentUri.getQueryParameter(name)
            if (value != null) {
                bundle.putString(name, value)
            }
        }
        return if (bundle.isEmpty) null else bundle
    }

    fun getPathAndParameters(): String = url?.file ?: ""

    fun getAnchor(): String = url?.ref ?: ""

    fun getQueryParameterNames(): Set<String> = uri?.queryParameterNames ?: emptySet()

    fun getQueryParameter(name: String): String? = uri?.getQueryParameter(name)

    fun lastPathSegment(): String? = uri?.lastPathSegment

    override fun toString(): String {
        return "URLParser\n" +
                "link=$rawLink\n" +
                "url=$url\n" +
                "isValid=${isValid()}\n" +
                "scheme=${getScheme()}\n" +
                "authority=${getAuthority()}\n" +
                "domain=${getDomain()}\n" +
                "port=${getPort()}\n" +
                "path=${getPath()}\n" +
                "parameters=${getParameters()}\n" +
                "pathAndParameters=${getPathAndParameters()}\n" +
                "anchor=${getAnchor()}\n" +
                "isWashPostLink=${isWashPostScheme()}"
    }

    fun copy(
        rawLink: String? = this.rawLink,
        isExternalOrigin: Boolean = this.isExternalOrigin,
        isPushOriginated: Boolean = this.isPushOriginated,
        isWidgetOriginated: Boolean = this.isWidgetOriginated,
        referrer: String? = this.referrer
    ): URLParser {
        return URLParser(
            rawLink,
            isExternalOrigin,
            isPushOriginated,
            isWidgetOriginated,
            referrer
        )
    }

    companion object {
        private const val WASHPOST_SCHEME = "washpost"
        private const val AIRSHIP_SCHEME = "airship"
        private const val WP_ANDROID_SCHEME = "wp-android"
        private const val HTTP_SCHEME = "http"
        private const val HTTPS_SCHEME = "https"
    }
}

fun getCanonicalUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val after = url.split("washingtonpost.com", limit = 2).getOrNull(1) ?: return null
    val path = after.split("?", limit = 2)[0]

    return path
}