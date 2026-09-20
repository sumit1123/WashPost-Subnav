package com.wapo.android.commons.util

import androidx.core.net.toUri

/**
 * Returns the URL with everything after the path removed, i.e. query params and fragment
 */
fun String.getUrlWithoutParameters(): String =
    this
        .toUri()
        .buildUpon()
        .clearQuery()
        .fragment("")
        .build()
        .toString()