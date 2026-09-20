/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.utils

import java.net.URI
import java.net.URISyntaxException

object UrlUtils {

    @Throws(URISyntaxException::class)
    fun getUrlWithoutParameters(url: String): String {
        val uri = URI(url)
        return URI(
            uri.scheme,
            uri.authority,
            uri.path,
            null  // Ignore the query part of the input url
        ).toString()
    }

}