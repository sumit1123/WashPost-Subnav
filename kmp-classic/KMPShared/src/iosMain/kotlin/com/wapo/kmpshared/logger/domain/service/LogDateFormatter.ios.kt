package com.wapo.kmpshared.logger.domain.service

import com.wapo.kmpshared.util.KMPDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale

internal actual class LogDateFormatter actual constructor() {
    private val formatter =
        NSDateFormatter().apply {
            locale = NSLocale(localeIdentifier = "en_US_POSIX")
            dateFormat = "yyyy-MM-dd HH:mm:ss.SSS Z"
        }

    actual fun format(date: KMPDate): String = formatter.stringFromDate(date)
}
