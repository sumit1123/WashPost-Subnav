package com.wapo.adsinf.interfaces

import android.view.View
import com.wapo.adsinf.models.AdConfig

interface AdUiEventsListener {
    fun showLoading()
    fun hideLoading()
    fun showOfflineAd(config: AdConfig)
    fun renderAdView(view: View)
    fun resizeAdSlot()
}