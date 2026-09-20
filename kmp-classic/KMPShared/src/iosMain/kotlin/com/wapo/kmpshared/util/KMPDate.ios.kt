package com.wapo.kmpshared.util

import platform.Foundation.NSDate
import platform.Foundation.NSISO8601DateFormatWithFractionalSeconds
import platform.Foundation.NSISO8601DateFormatWithInternetDateTime
import platform.Foundation.NSISO8601DateFormatter

actual typealias KMPDate = NSDate

private val iso8601Formatter =
    NSISO8601DateFormatter().apply {
        formatOptions = NSISO8601DateFormatWithInternetDateTime or NSISO8601DateFormatWithFractionalSeconds
    }

private val iso8601SansMSFormatter =
    NSISO8601DateFormatter().apply {
        formatOptions = NSISO8601DateFormatWithInternetDateTime
    }

actual fun createKMPDateFromISO8601(isoString: String): KMPDate? =
    iso8601Formatter.dateFromString(isoString)
        ?: iso8601SansMSFormatter.dateFromString(isoString)
