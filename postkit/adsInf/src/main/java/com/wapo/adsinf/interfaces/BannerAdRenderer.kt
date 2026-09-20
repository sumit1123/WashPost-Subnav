package com.wapo.adsinf.interfaces

import com.wapo.adsinf.BannerAdView
import com.wapo.adsinf.models.AdRequest
import com.wapo.adsinf.models.AdLoadSession

abstract class BannerAdRenderer {
    private val requestStartTimes: MutableMap<String, Long> = mutableMapOf()

    abstract fun load(
        parent: BannerAdView,
        adRequest: AdRequest,
        adLoadSession: AdLoadSession,
        callback: AdLoadCallback,
    )

    open fun release(parent: BannerAdView) {
        requestStartTimes.clear()
    }

    fun startReqTimer(id: String) {
        requestStartTimes[id] = System.currentTimeMillis()
    }

    fun endReqTimer(id: String): Long? {
        val startTime = requestStartTimes[id] ?: return null
        val responseTime = System.currentTimeMillis() - startTime
        requestStartTimes.remove(id)
        return responseTime
    }
}