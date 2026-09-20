package com.wapo.flagship.features.articles2.luf

import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

internal fun Long.toLUFDateFormat(): String {
    val cal: Calendar = Calendar.getInstance()
    val tz: TimeZone = cal.timeZone
    val sdf = SimpleDateFormat("h:mm aa", Locale.getDefault())
    val symbols = DateFormatSymbols(Locale.getDefault())
    symbols.amPmStrings = arrayOf("a.m.", "p.m.")
    sdf.dateFormatSymbols = symbols
    sdf.timeZone = tz
    return sdf.format(Date(this))
}
