@file:JvmName("URLHelper")

package com.wapo.flagship.features.articles2.utils

import androidx.core.net.toUri
import com.wapo.android.commons.extensions.toUri
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.util.tracking.Measurement

// used for analytics tracking mainly to attribute pvs to click through from section front
const val ITID_QUERY_PARAM_KEY = "itid"
const val UTM_SOURCE_QUERY_PARAM_KEY = "utm_source"
const val UTM_MEDIUM_QUERY_PARAM_KEY = "utm_medium"
const val UTM_CAMPAIGN_QUERY_PARAM_KEY = "utm_campaign"
const val NEXT_URL_QUERY_PARAM_KEY = "next_url"

/**
 * Returns the URL with everything after the path removed, i.e. query params and fragment
 * Trailing slash must be preserved to avoid 415 error
 */
fun getUrlWithoutParameters(url: String): String =
    url
        .toUri()
        .buildUpon()
        .clearQuery()
        .fragment("")
        .build()
        .toString()

fun appendQueryParams(url: String, queryParams: Map<String, String>): String {
    val newUri = url
        .toUri()
        .buildUpon()
        .apply {
            queryParams.forEach { key, value ->
                appendQueryParameter(key, value)
            }
        }
        .build()
    return newUri.toString()
}

/**
 * Appends UTM params to pass tracking info to webview.
 * Used currently for Games and Search.
 */
fun appendTrackingParams(
    link: String,
    itId: String?,
    tabName: String?,
): String {
    val linkUri = link.toUri()
    val linkUriBuilder = linkUri.buildUpon()

    if (DeepLinksProcessor.isExternalUrl(URLParser(link))) {
        if (linkUri.queryParameterNames.contains(NEXT_URL_QUERY_PARAM_KEY)) {
            linkUriBuilder.clearQuery()
            linkUri.queryParameterNames
                .filter { it != NEXT_URL_QUERY_PARAM_KEY }
                .forEach { param ->
                    linkUri.getQueryParameter(param)?.let { value ->
                        linkUriBuilder.appendQueryParameter(param, value)
                    }
                }
        }
        linkUriBuilder.appendQueryParameter(NEXT_URL_QUERY_PARAM_KEY, DeepLinksProcessor.WASHPOST_SCHEMA)
    }

    // Check if the URL already has the itid.
    if (!linkUri.queryParameterNames.contains("itid")) {
        itId?.let {
            linkUriBuilder.appendQueryParameter("itid", it)
        }
    }

    // Check if the URL already has the utm params.
    if (tabName != null &&
        !linkUri.queryParameterNames.contains("utm_source") &&
        !linkUri.queryParameterNames.contains("utm_medium") &&
        !linkUri.queryParameterNames.contains("utm_campaign")
    ) {
        linkUriBuilder
            .appendQueryParameter("utm_source", "webview")
            .appendQueryParameter("utm_medium", "referral_$tabName")
            .appendQueryParameter("utm_campaign", formatTestGroup(Measurement.getABTestGroup()))
    }

    return linkUriBuilder.build().toString()
}

fun appendTrackingITIDParams(link: String, itId: String?): String {
    val linkUri = link.toUri()
    val linkUriBuilder = linkUri.buildUpon()

    // Check if the URL already has the itId.
    return if (!linkUri.queryParameterNames.contains(ITID_QUERY_PARAM_KEY)) {
        itId?.let {
            linkUriBuilder.appendQueryParameter(ITID_QUERY_PARAM_KEY, it).build().toString()
        } ?: link
    } else link
}

/**
 * Adds tracking params from current url to new url.
 * Does not overwrite if param is already present in new url.
 */
fun preserveTrackingParams(
    originalUrl: String?,
    incomingUrl: String?,
): String? {
    incomingUrl ?: return null
    val currentUri = originalUrl.toUri()
    currentUri ?: return incomingUrl
    val incomingUri = incomingUrl.toUri()
    val builder = incomingUri.buildUpon()
    val trackingParams =
        listOf(
            ITID_QUERY_PARAM_KEY,
            UTM_SOURCE_QUERY_PARAM_KEY,
            UTM_MEDIUM_QUERY_PARAM_KEY,
            UTM_CAMPAIGN_QUERY_PARAM_KEY,
        )
    trackingParams.forEach { param ->
        if (!currentUri.getQueryParameter(param).isNullOrEmpty() &&
            incomingUri
                .getQueryParameter(
                    param,
                ).isNullOrEmpty()
        ) {
            builder.appendQueryParameter(param, currentUri.getQueryParameter(param))
        }
    }
    return builder.build().toString()
}

/**
 * Replaces illegal '|' char for Test Group in query param
 */
private fun formatTestGroup(string: String): String = string.replace('|', ':')

fun getBaseUrl(url: String): String? {
    return try {
        url
            .toUri()
            .buildUpon()
            .clearQuery()
            .path("")
            .fragment("")
            .build()
            .toString()
    } catch (e: Exception) {
        null
    }
}
