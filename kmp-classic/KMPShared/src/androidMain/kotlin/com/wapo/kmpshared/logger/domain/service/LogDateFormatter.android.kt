package com.wapo.kmpshared.logger.domain.service

import com.wapo.kmpshared.util.KMPDate
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal actual class LogDateFormatter actual constructor() {
    private val formatter =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).apply {
            isLenient = false
        }

    actual fun format(date: KMPDate): String {
        formatter.timeZone = TimeZone.getDefault()
        return formatter.format(date)
    }
}
