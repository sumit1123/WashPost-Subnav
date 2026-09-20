/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    fun toDateLong(dateString: String?, dateFormat: SimpleDateFormat = getDefaultDateFormat()) : Long {
        if (dateString == null) return 0
        return try {
            dateFormat.parse(dateString)?.time ?: 0
        } catch (t: Throwable) {
            0
        }
    }

    private fun getDefaultDateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
    }

}