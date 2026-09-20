@file:JvmName("ContentUtils")

package com.wapo.flagship.common

import android.content.Context
import android.net.Uri
import androidx.annotation.NonNull
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.wapo.adsinf.utils.AdsUtil
import com.wapo.android.commons.extensions.urlEncoded
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor.Companion.headers
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.Utils
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.grid.model.HomepageStory
import com.wapo.flagship.features.grid.model.Video
import com.wapo.flagship.features.sections.model.*
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.model.ArticleMeta
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.wrappers.CrashWrapper
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.features.ccpa.appendCCPAQueryParameterToUrl
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.Proxy
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri
import com.wapo.android.commons.util.getHourOfDay
import com.wapo.flagship.features.wpvideos.fragments.WatchVideoFragment

// Paths Test: android-tablet/src/test/java/com/wapo/flagship/content/SectionUrlsText.kt
const val SECTION_URL_TO_SECTION_PATH_REGEX = "^[^\\/]+\\/\\/[^\\/]+\\/([^\\/]+\\/?){1,2}$"
const val AD_PARAM_SIZE = "sz"
const val AD_PARAM_IU = "iu"
const val AD_PARAM_DESCRIPTION_URL = "description_url"
const val AD_PARAM_CORRELATOR = "correlator"
const val AD_PARAM_HOUR_OF_DAY = "hour"
const val AD_APP_URL_KEY = "app_url"

fun removeQueryParams(
    uri: Uri?,
    params: Array<String>?,
): Uri? {
    uri ?: return null
    params ?: return uri
    val queryParameterNames = uri.queryParameterNames
    val builder = uri.buildUpon().clearQuery()
    queryParameterNames.forEach { queryParam ->
        if (queryParam !in params) {
            builder.appendQueryParameter(queryParam, uri.getQueryParameter(queryParam))
        }
    }
    return builder.build()
}

fun getUrlAndAnchorRefPair(
    @NonNull link: String,
): Pair<String, String?> {
    val urlParts = link.split(("#"))
    return if (urlParts.size == 2) {
        Pair(urlParts[0], urlParts[1])
    } else {
        Pair(link, null)
    }
}

/**
 * Returns true if a given url is www.washingtonpost.com main page (no path)
 */
fun isWaPoHomepage(
    url: String,
    context: Context,
): Boolean =
    try {
        val uri = Uri.parse(url)
        (
            uri.host == context.getString(R.string.wp_domain_root) ||
                uri.host ==
                context.getString(
                    R.string.wp_domain_www,
                )
        ) &&
            uri.pathSegments.isEmpty()
    } catch (t: Throwable) {
        false
    }

/**
 * process given ad tag url for Section Fronts
 * 1. update description_url
 * 2. append targeting values
 * 3. append CCPA value
 */
fun getAdTagUrl(
    video: Video,
    story: HomepageStory,
    targetingContent: TargetingContent?
): String? {
    val articleUrl = story.link?.url ?: ""
    var adTagUrl = video.adConfig?.adSetUrl ?: return null
    val originalUri = adTagUrl.toUri()
    val builder = originalUri.buildUpon().clearQuery()
    builder?.appendQueryParameter(AD_PARAM_HOUR_OF_DAY, getHourOfDay().toString())
    originalUri.queryParameterNames.forEach { key ->
        when (key) {
            AD_PARAM_SIZE -> {
                builder.appendQueryParameter(AD_PARAM_SIZE, "640x480")
            }

            AD_PARAM_IU -> {
                val iuValue = if (AppContextUtils.isTablet()) {
                    FlagshipApplication.getInstance()
                        .getString(com.wapo.adsinf.R.string.ad_key_tab_root_path_carousal)
                } else {
                    FlagshipApplication.getInstance()
                        .getString(com.wapo.adsinf.R.string.ad_key_mob_root_path_carousal)
                }
                builder.appendQueryParameter(AD_PARAM_IU, iuValue)
            }

            AD_PARAM_DESCRIPTION_URL -> {
                builder.appendQueryParameter(AD_PARAM_DESCRIPTION_URL, articleUrl.urlEncoded)
            }

            AD_PARAM_CORRELATOR -> {
                builder.appendQueryParameter(
                    AD_PARAM_CORRELATOR,
                    System.currentTimeMillis().toString()
                )
            }

            else -> {
                builder.appendQueryParameter(key, originalUri.getQueryParameter(key))
            }
        }
    }
    adTagUrl = builder.build().toString()
    adTagUrl = addTargetingValuesToAdTagUrl(
        adTagUrl,
        getSectionsAdTargetingValues(video.adConfig?.primarySectionId, targetingContent, articleUrl)
    ) ?: adTagUrl
    return appendCCPAQueryParameterToUrl(adTagUrl)
}

/**
 * process given ad tag url for Articles
 * 1. update description_url
 * 2. append targeting(analytics provider's, user's, contextual) values
 * 3. append CCPA value
 */
fun getAdTagUrl(
    video: com.wapo.flagship.features.articles2.models.deserialized.video.Video,
    articleModel: Article2?,
    targetingContent: com.wapo.flagship.features.articles2.ads.targeting.TargetingContent?
): String? {
    var adTagUrl = video.adconfig?.adSetConfig?.adSetUrls?.apps
    val originalUri = adTagUrl?.toUri()
    val builder = originalUri?.buildUpon()?.clearQuery()
    builder?.appendQueryParameter(AD_PARAM_HOUR_OF_DAY, getHourOfDay().toString())

    originalUri?.queryParameterNames?.forEach { key ->
        when (key) {
            AD_PARAM_SIZE -> {
                builder?.appendQueryParameter(AD_PARAM_SIZE, "640x480")
            }

            AD_PARAM_IU -> {
                val iuValue = if (AppContextUtils.isTablet()) {
                    FlagshipApplication.getInstance()
                        .getString(com.wapo.adsinf.R.string.ad_key_tab_root_path_article)
                } else {
                    FlagshipApplication.getInstance()
                        .getString(com.wapo.adsinf.R.string.ad_key_mob_root_path_article)
                }
                builder?.appendQueryParameter(AD_PARAM_IU, iuValue)
            }

            AD_PARAM_DESCRIPTION_URL -> {
                builder?.appendQueryParameter(AD_PARAM_DESCRIPTION_URL, video.contenturl.urlEncoded)
            }

            AD_PARAM_CORRELATOR -> {
                builder?.appendQueryParameter(
                    AD_PARAM_CORRELATOR,
                    System.currentTimeMillis().toString()
                )
            }

            else -> {
                builder?.appendQueryParameter(key, originalUri.getQueryParameter(key))
            }
        }
    }
    adTagUrl = builder?.build()?.toString()
    adTagUrl = addTargetingValuesToAdTagUrl(
        adTagUrl,
        getArticlesAdTargetingValues(
            articleModel,
            video.adconfig?.primarySectionId,
            targetingContent,
            video.contenturl.toString()
        )
    )
        ?: adTagUrl
    return appendCCPAQueryParameterToUrl(adTagUrl)
}

/**
 * process given ad tag url for Watch Vertical Video
 * Setting the SZ to be 640x480|480x640 for Vertical Video Sizes required from Ads Team to support multiple sizes
 * setting iu to the vertical Video Ad unit ID
 */
fun getAdTagUrl(
    video: com.wapo.flagship.features.posttv.model.Video,
    sourceScreen: String?
): String? {
    val originalUrl = video.adTagUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val originalUri = originalUrl.toUri()
    val builder = originalUri.buildUpon().clearQuery()

    originalUri.queryParameterNames.forEach { key ->
        when (key) {
            AD_PARAM_SIZE -> {
                builder.appendQueryParameter(AD_PARAM_SIZE, "640x480|480x640")
            }

            AD_PARAM_IU -> {
                if (sourceScreen?.trim()
                        .equals(WatchVideoFragment.WP_VIDEO_BUNDLE_NAME.trim(), ignoreCase = true)
                ) {
                    val iuValue = if (AppContextUtils.isTablet()) {
                        FlagshipApplication.getInstance()
                            .getString(com.wapo.adsinf.R.string.ad_key_tab_root_path_watch)
                    } else {
                        FlagshipApplication.getInstance()
                            .getString(com.wapo.adsinf.R.string.ad_key_mob_root_path_watch)
                    }
                    builder.appendQueryParameter(AD_PARAM_IU, iuValue)
                } else {
                    val iuValue = if (AppContextUtils.isTablet()) {
                        FlagshipApplication.getInstance()
                            .getString(com.wapo.adsinf.R.string.ad_key_tab_root_path_carousal)
                    } else {
                        FlagshipApplication.getInstance()
                            .getString(com.wapo.adsinf.R.string.ad_key_mob_root_path_carousal)
                    }
                    builder.appendQueryParameter(AD_PARAM_IU, iuValue)
                }
            }

            AD_PARAM_DESCRIPTION_URL -> {
                builder.appendQueryParameter(AD_PARAM_DESCRIPTION_URL, video.contentUrl.urlEncoded)
            }

            AD_PARAM_CORRELATOR -> {
                builder.appendQueryParameter(
                    AD_PARAM_CORRELATOR,
                    System.currentTimeMillis().toString()
                )
            }

            else -> {
                builder.appendQueryParameter(key, originalUri.getQueryParameter(key))
            }
        }
    }
    return builder.build().toString()
}

/**
 * Build and return a new adTagUrl with ad targeting query parameters from the given map.
 */
fun addTargetingValuesToAdTagUrl(
    adTagUrl: String?,
    map: Map<String, List<String>>,
): String? {
    adTagUrl ?: return null
    return try {
        val customParamsKey = "cust_params"
        // Construct query params string(format: key1=value1$key2=value21,value22) from map entries.
        val paramsBuilder = StringBuilder()
        for (key in map.keys) {
            map[key]?.let { value ->
                if (paramsBuilder.length > 1) paramsBuilder.append("&")
                paramsBuilder.append("$key=${value.joinToString(separator = ",") { it }}")
            }
        }
        Logger.i(
            "ContentUtils",
            "Targeting AdTagUrl, paramsBuilder=$paramsBuilder, adTagUrl=$adTagUrl",
        )
        if (paramsBuilder.isNotEmpty()) {
            val uri = Uri.parse(adTagUrl)
            // There is no remove query param method. So reconstructing uri with all query
            // parameters excluding cust_params
            val newUriBuilder = uri.buildUpon().clearQuery()
            for (param in uri.queryParameterNames) {
                if (param != customParamsKey) {
                    newUriBuilder.appendQueryParameter(param, uri.getQueryParameter(param))
                }
            }
            // Prepare cust_params. If there is already a query parameter with the same key, then
            // append paramsBuilder to that, otherwise starts with an empty value.
            val customParams = StringBuilder(uri.getQueryParameter(customParamsKey) ?: "")
            if (customParams.length > 1) {
                customParams.append("&")
            }
            customParams.append(paramsBuilder)
            // Append cust_params to newUri
            newUriBuilder.appendQueryParameter(customParamsKey, customParams.toString())
            // Build newUri
            val newUri = newUriBuilder.build().toString()
            Logger.i("ContentUtils", "Targeting AdTagUrl, finalUrl=$newUri")
            newUri
        } else {
            // return original adTagUrl when map (paramsBuilder) is empty.
            adTagUrl
        }
    } catch (t: Throwable) {
        t.printStackTrace()
        // return original adTagUrl for any exception
        adTagUrl
    }
}

/**
 * Prepare targeting values for section front ads.
 * Merge analytics providers targeting values if there is more than one.
 * Permutive is the only analytics provider that provides targeting values for now.
 */
fun getSectionsAdTargetingValues(primarySectionId: String?, targetingContent: TargetingContent?, contentUrl: String?): Map<String, List<String>> {
    val analyticsSegments = getAnalyticsProvidersTargetingValues()
    val primarySectionIdValues = getPrimarySectionIdValues(primarySectionId)
    var targetingValues = mergeMaps(analyticsSegments, primarySectionIdValues)
    val adContentSegments = getAdContentTargetingValues(targetingContent?.adCall)
    targetingValues = mergeMaps(targetingValues, adContentSegments)
    targetingValues = mergeMaps(targetingValues, mapOf(AD_APP_URL_KEY to listOf(contentUrl.toString())))
    Logger.i("ContentUtils", "Section Ad targetingValues=$targetingValues")
    return targetingValues
}

/**
 * Prepare targeting values for Article ads.
 * Merge article's targeting.adCall and analytics providers targeting values when no targetingContent.adCall data.
 * Otherwise merge targetingContent.adCall and analytics providers targeting values.
 * Permutive is the only analytics provider that provides targeting values for now.
 */
fun getArticlesAdTargetingValues(articleModel: Article2?,
                                 primarySectionId: String?,
                                 targetingContent: com.wapo.flagship.features.articles2.ads.targeting.TargetingContent?,
                                 contentUrl: String?): Map<String, List<String>> {
    val contextualSegments = if (targetingContent?.adCall == null) getContextualTargetingValues(articleModel) else mutableMapOf()
    val analyticsSegments = getAnalyticsProvidersTargetingValues()
    var targetingValues = mergeMaps(contextualSegments, analyticsSegments)
    val primarySectionIdValues = getPrimarySectionIdValues(primarySectionId)
    targetingValues = mergeMaps(targetingValues, primarySectionIdValues)
    val adContentSegments = getAdContentTargetingValues(targetingContent?.adCall)
    targetingValues = mergeMaps(targetingValues, adContentSegments)
    targetingValues = mergeMaps(targetingValues, mapOf(AD_APP_URL_KEY to listOf(contentUrl.toString())))
    Logger.i("ContentUtils", "Article Ad targetingValues=$targetingValues")
    return targetingValues
}

fun getPrimarySectionIdValues(primarySectionId: String?): Map<String, List<String>> {
    val primarySectionIdValues = AdsUtil.getPrimarySectionIdValues(primarySectionId)
    val primarySectionIdValueMap = mutableMapOf<String, List<String>>()
    primarySectionIdValues?.forEach {
        primarySectionIdValueMap[it.first] = listOf(it.second)
    }

    return primarySectionIdValueMap
}

fun mergeMaps(
    first: Map<String, List<String>>,
    second: Map<String, List<String>>,
): Map<String, List<String>> {
    val result: Map<String, List<String>> =
        (first.keys + second.keys)
            .associateWith {
                val set = mutableSetOf<String>()
                first[it]?.let { it1 -> set.addAll(it1) }
                second[it]?.let { it1 -> set.addAll(it1) }
                set.toList()
            }
    return result
}

/**
 * Returns a map with article's targeting.adCall segments
 */
fun getContextualTargetingValues(articleModel: Article2?): Map<String, List<String>> {
    // Process targeting.adCall data
    val targetingMap = mutableMapOf<String, List<String>>()
    // Add targeting.adcall key values to adMap
    if (articleModel?.targeting?.adCall is List<*>) {
        articleModel.targeting.adCall.forEach {
            if (it is Map<*, *>) {
                val key = it["key"]?.toString()
                val values = it["values"] as? List<*>
                if (!key.isNullOrEmpty() && !values.isNullOrEmpty()) {
                    targetingMap[key] = values.mapNotNull { value -> value.toString() }
                }
            }
        }
    }
    return targetingMap
}

/**
 * Returns a map with adCall segments
 */
fun getAdContentTargetingValues(adCall: Any?): Map<String, List<String>> {
    // Process adCall data
    val targetingMap = mutableMapOf<String, List<String>>()
    // Add adcall key values to adMap
    if (adCall is Map<*, *>) {
        adCall.forEach { entry ->
            val key = entry.key?.toString()
            val values = entry.value as? List<*>
            if (!key.isNullOrEmpty() && !values.isNullOrEmpty()) {
                targetingMap[key] = values.mapNotNull { value -> value.toString() }
            }
        }
    }
    return targetingMap
}

/**
 * Returns a map that has key values from the analytics providers targeting values.
 */
fun getAnalyticsProvidersTargetingValues(): Map<String, List<String>> {
    val targetingMap = hashMapOf<String, List<String>>()
    AdManagerAdRequest
        .Builder()
        .run {
            Measurement.addCustomTargeting(this)
            build()
        }.run {
            for (key in customTargeting.keySet()) {
                customTargeting.get(key)?.toString()?.let { value ->
                    targetingMap[key] = value.split(",")
                }
            }
        }
    return targetingMap
}

fun getArticleIndex(
    articleMetaList: List<ArticleMeta>,
    articleUrl: String,
): Int {
    articleMetaList?.forEachIndexed { index, articleMeta ->
        if (articleMeta.id?.equals(articleUrl) == true) {
            return index
        }
    }
    return -1
}

fun getMenuSectionFromUrl(
    url: String?,
    intentHelper: IntentHelper,
    menuSections: List<MenuSection>?,
): MenuSection? {
    if (url != null && menuSections != null) {
        val urlParser = URLParser(url)
        if (!DeepLinksProcessor.isSectionPath(urlParser)) {
            return null
        }
        val sectionId = DeepLinksProcessor.getSectionId(urlParser)
        return intentHelper.findMenuSection(sectionId, menuSections)
    }
    return null
}

fun getMenuSections(): List<MenuSection>? =
    FlagshipApplication
        .getInstance()
        .contentManager.allMenuSections
        .toBlocking()
        .firstOrDefault(null)

private fun getDefaultDateFormat(): SimpleDateFormat =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

/**
 * [maxRetryAttempts] - Maximum number of times we want to try finding the redirect for the shortened URL
 * e.g. when user clicks on an article on twitter app/site, the "https://wapo.st" resolves into another shortened url "trib.al"
 * In this case we need to resolve/expand the subsequent url (https://tirb.al) as well.
 */
fun expandShortUrl(
    shortenedUrl: String?,
    context: Context,
    maxRetryAttempts: Int = 1,
): String? {
    try {
        val url = URL(shortenedUrl)
        // open connection
        val httpURLConnection = url.openConnection(Proxy.NO_PROXY) as HttpURLConnection
        for (header in headers) {
            httpURLConnection.setRequestProperty(header.key, header.value)
        }

        // stop following browser redirect
        httpURLConnection.instanceFollowRedirects = false

        // extract location header containing the actual destination URL
        val expandedURL = httpURLConnection.getHeaderField("Location")
        httpURLConnection.disconnect()
        when {
            /**
             *  This will be the first attempt if this is the first call to [expandShortUrl] function on the recursion stack.
             *  This also means we stop if we find an internal url and return it.
             */
            Utils.isWapoURL(expandedURL, context) -> {
                return expandedURL
            }
            /**
             * If max attempt has not reached, try one more time and decrement the [maxRetryAttempts]
             */
            maxRetryAttempts > 0 -> {
                return expandShortUrl(expandedURL, context, maxRetryAttempts - 1)
            }
            /**
             * Else report the URL as invalid.
             */
            else -> {
                CrashWrapper.sendException(
                    java.lang.Exception("Resolved to Invalid Wapo Url $expandedURL"),
                )
            }
        }
    } catch (e: MalformedURLException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}
