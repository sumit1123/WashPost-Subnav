package com.wapo.flagship.util

import com.wapo.android.commons.util.DeviceUtils

/**
 * This is a helper object to track generated [jUcid] per session.
 * Generation happens just before the first usage of it which could be pv events from section front or article.
 */
object JUcidTracker {
    val jUcid: String by lazy {
        DeviceUtils.generateJUcid()
    }
}
